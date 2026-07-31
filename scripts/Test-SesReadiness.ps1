[CmdletBinding()]
param(
    [string]$StackName = "subtrak-production-email",
    [string]$Region = "ap-south-1",
    [switch]$RequireCutoverReady
)

$ErrorActionPreference = "Stop"
$script:Failures = 0

function Invoke-AwsJson {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = & aws @Arguments --region $Region --output json 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $lines = $result |
        ForEach-Object { $_.ToString() } |
        Where-Object {
            -not [string]::IsNullOrWhiteSpace($_) -and
            $_ -ne "System.Management.Automation.RemoteException"
        }
    $text = $lines -join [Environment]::NewLine
    if ($exitCode -ne 0) {
        if ([string]::IsNullOrWhiteSpace($text)) {
            $text = "AWS CLI exited with code $exitCode."
        }
        throw [System.InvalidOperationException]::new($text)
    }
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }
    return $text | ConvertFrom-Json
}

function Write-Check {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][bool]$Passed,
        [Parameter(Mandatory)][string]$Detail,
        [switch]$Required
    )

    if ($Passed) {
        Write-Host "[PASS] $Name - $Detail" -ForegroundColor Green
        return
    }

    if ($Required) {
        $script:Failures++
        Write-Host "[FAIL] $Name - $Detail" -ForegroundColor Red
    } else {
        Write-Host "[WARN] $Name - $Detail" -ForegroundColor Yellow
    }
}

function Get-OutputValue {
    param(
        [Parameter(Mandatory)]$Stack,
        [Parameter(Mandatory)][string]$Key
    )

    return ($Stack.Outputs | Where-Object OutputKey -eq $Key |
        Select-Object -First 1 -ExpandProperty OutputValue)
}

try {
    $identity = Invoke-AwsJson @("sts", "get-caller-identity")
    Write-Check "AWS credentials" $true "authenticated to account $($identity.Account)" -Required

    $stackResponse = Invoke-AwsJson @(
        "cloudformation", "describe-stacks", "--stack-name", $StackName
    )
    $stack = $stackResponse.Stacks | Select-Object -First 1
    $stackReady = $stack.StackStatus -in @("CREATE_COMPLETE", "UPDATE_COMPLETE")
    Write-Check "CloudFormation stack" $stackReady "$StackName is $($stack.StackStatus)" -Required

    $stackAccount = $stack.Parameters |
        Where-Object ParameterKey -eq "AwsAccountId" |
        Select-Object -First 1 -ExpandProperty ParameterValue
    Write-Check "AWS account" ($identity.Account -eq $stackAccount) `
        "caller account matches stack parameter" -Required

    $sesRegion = Get-OutputValue $stack "SesRegion"
    Write-Check "SES region" ($sesRegion -eq $Region) `
        "stack output is $sesRegion" -Required

    $domainIdentity = Get-OutputValue $stack "SesDomainIdentity"
    $emailIdentity = Invoke-AwsJson @(
        "sesv2", "get-email-identity", "--email-identity", $domainIdentity
    )
    Write-Check "SES identity" ($emailIdentity.VerifiedForSendingStatus -eq $true) `
        "$domainIdentity verified-for-sending=$($emailIdentity.VerifiedForSendingStatus)" -Required
    Write-Check "SES DKIM" ($emailIdentity.DkimAttributes.Status -eq "SUCCESS") `
        "DKIM status is $($emailIdentity.DkimAttributes.Status)" -Required

    $account = Invoke-AwsJson @("sesv2", "get-account")
    Write-Check "SES production access" ($account.ProductionAccessEnabled -eq $true) `
        "production-access=$($account.ProductionAccessEnabled)" `
        -Required:$RequireCutoverReady
    Write-Check "SES sending" ($account.SendingEnabled -eq $true) `
        "account sending-enabled=$($account.SendingEnabled)" `
        -Required:$RequireCutoverReady

    $configurationSet = Get-OutputValue $stack "SesConfigurationSet"
    $null = Invoke-AwsJson @(
        "sesv2", "get-configuration-set",
        "--configuration-set-name", $configurationSet
    )
    Write-Check "SES configuration set" $true "$configurationSet exists" -Required

    $expectedRuleSet = "subtrak-$(
        ($stack.Parameters |
            Where-Object ParameterKey -eq "EnvironmentName" |
            Select-Object -First 1 -ExpandProperty ParameterValue)
    )-inbound"
    $activeRules = Invoke-AwsJson @("ses", "describe-active-receipt-rule-set")
    $activeRuleSet = $activeRules.Metadata.Name
    Write-Check "Active receipt rule set" ($activeRuleSet -eq $expectedRuleSet) `
        "expected $expectedRuleSet; active=$activeRuleSet" `
        -Required:$RequireCutoverReady

    $bucket = Get-OutputValue $stack "SesInboundBucket"
    $publicAccess = Invoke-AwsJson @(
        "s3api", "get-public-access-block", "--bucket", $bucket
    )
    $block = $publicAccess.PublicAccessBlockConfiguration
    $allPublicAccessBlocked = $block.BlockPublicAcls -and
        $block.IgnorePublicAcls -and
        $block.BlockPublicPolicy -and
        $block.RestrictPublicBuckets
    Write-Check "S3 public access block" $allPublicAccessBlocked `
        "all four public-access controls are enabled" -Required

    $encryption = Invoke-AwsJson @(
        "s3api", "get-bucket-encryption", "--bucket", $bucket
    )
    $encryptionAlgorithm =
        $encryption.ServerSideEncryptionConfiguration.Rules[0].ApplyServerSideEncryptionByDefault.SSEAlgorithm
    Write-Check "S3 encryption" ($encryptionAlgorithm -in @("AES256", "aws:kms")) `
        "default encryption is $encryptionAlgorithm" -Required

    $lifecycle = Invoke-AwsJson @(
        "s3api", "get-bucket-lifecycle-configuration", "--bucket", $bucket
    )
    $enabledLifecycle = @($lifecycle.Rules | Where-Object Status -eq "Enabled").Count -gt 0
    Write-Check "S3 lifecycle" $enabledLifecycle `
        "at least one lifecycle rule is enabled" -Required

    $queueChecks = @(
        @{ Name = "Inbound"; Output = "SesInboundQueueUrl"; IsDlq = $false },
        @{ Name = "Event"; Output = "SesEventQueueUrl"; IsDlq = $false },
        @{ Name = "Inbound DLQ"; Output = "InboundDeadLetterQueueUrl"; IsDlq = $true },
        @{ Name = "Event DLQ"; Output = "OutboundEventDeadLetterQueueUrl"; IsDlq = $true }
    )
    foreach ($queueCheck in $queueChecks) {
        $queueUrl = Get-OutputValue $stack $queueCheck.Output
        $attributes = Invoke-AwsJson @(
            "sqs", "get-queue-attributes",
            "--queue-url", $queueUrl,
            "--attribute-names", "ApproximateNumberOfMessages",
            "ApproximateNumberOfMessagesNotVisible"
        )
        $visible = $attributes.Attributes.ApproximateNumberOfMessages
        $inFlight = $attributes.Attributes.ApproximateNumberOfMessagesNotVisible
        Write-Check "$($queueCheck.Name) queue access" $true `
            "readable; visible=$visible, in-flight=$inFlight" -Required

        $messageCount = [int64]$visible + [int64]$inFlight
        if ($queueCheck.IsDlq) {
            Write-Check "$($queueCheck.Name) empty" ($messageCount -eq 0) `
                "message count is $messageCount" `
                -Required:$RequireCutoverReady
        } elseif ($messageCount -gt 0) {
            Write-Check "$($queueCheck.Name) backlog" $false `
                "$messageCount message(s) will be processed when consumers are enabled"
        }
    }

    Write-Host ""
    if ($script:Failures -gt 0) {
        Write-Host "SES readiness failed with $script:Failures required check(s)." -ForegroundColor Red
        exit 1
    }
    Write-Host "SES readiness checks passed." -ForegroundColor Green
    exit 0
} catch {
    $failureMessage = $_.Exception.Message
    if ([string]::IsNullOrWhiteSpace($failureMessage)) {
        $failureMessage = $_.ToString()
    }
    Write-Host "[FAIL] AWS readiness check could not complete - $failureMessage" `
        -ForegroundColor Red
    exit 1
}
