[CmdletBinding()]
param(
    [string]$StackName = "subtrak-production-email",
    [string]$Region = "ap-south-1",
    [string]$FromEmail = "noreply@subtrak.xyz",
    [ValidateRange(5, 60)]
    [int]$WaitSeconds = 45
)

$ErrorActionPreference = "Stop"

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

    $text = ($result |
        ForEach-Object { $_.ToString() } |
        Where-Object {
            -not [string]::IsNullOrWhiteSpace($_) -and
            $_ -ne "System.Management.Automation.RemoteException"
        }) -join [Environment]::NewLine
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

function Get-OutputValue {
    param(
        [Parameter(Mandatory)]$Stack,
        [Parameter(Mandatory)][string]$Key
    )

    return ($Stack.Outputs |
        Where-Object OutputKey -eq $Key |
        Select-Object -First 1 -ExpandProperty OutputValue)
}

function Get-VisibleMessageCount {
    param([Parameter(Mandatory)][string]$QueueUrl)

    $attributes = Invoke-AwsJson @(
        "sqs", "get-queue-attributes",
        "--queue-url", $QueueUrl,
        "--attribute-names", "ApproximateNumberOfMessages"
    )
    return [int64]$attributes.Attributes.ApproximateNumberOfMessages
}

try {
    $stackResponse = Invoke-AwsJson @(
        "cloudformation", "describe-stacks", "--stack-name", $StackName
    )
    $stack = $stackResponse.Stacks | Select-Object -First 1
    $configurationSet = Get-OutputValue $stack "SesConfigurationSet"
    $eventQueueUrl = Get-OutputValue $stack "SesEventQueueUrl"
    $beforeCount = Get-VisibleMessageCount $eventQueueUrl

    $testId = [DateTimeOffset]::UtcNow.ToString("yyyyMMddHHmmss")
    $scenarios = @(
        @{ Name = "delivery"; Recipient = "success+$testId@simulator.amazonses.com" },
        @{ Name = "bounce"; Recipient = "bounce+$testId@simulator.amazonses.com" },
        @{ Name = "complaint"; Recipient = "complaint+$testId@simulator.amazonses.com" }
    )

    foreach ($scenario in $scenarios) {
        $response = Invoke-AwsJson @(
            "sesv2", "send-email",
            "--from-email-address", $FromEmail,
            "--destination", "ToAddresses=$($scenario.Recipient)",
            "--content",
            "Simple={Subject={Data=SubTrak SES acceptance $($scenario.Name)},Body={Text={Data=SubTrak controlled mailbox simulator acceptance test.}}}",
            "--configuration-set-name", $configurationSet
        )
        Write-Host "[PASS] $($scenario.Name) send accepted - message ID $($response.MessageId)" `
            -ForegroundColor Green
    }

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($WaitSeconds)
    $afterCount = $beforeCount
    do {
        Start-Sleep -Seconds 5
        $afterCount = Get-VisibleMessageCount $eventQueueUrl
    } while ($afterCount -lt ($beforeCount + $scenarios.Count) -and
        [DateTimeOffset]::UtcNow -lt $deadline)

    $increase = $afterCount - $beforeCount
    if ($increase -lt $scenarios.Count) {
        Write-Host "[FAIL] Event queue increased by $increase; expected at least $($scenarios.Count)." `
            -ForegroundColor Red
        exit 1
    }

    Write-Host "[PASS] Event plumbing - visible queue count increased from $beforeCount to $afterCount." `
        -ForegroundColor Green
    Write-Host "Simulator payloads were not read or consumed." -ForegroundColor Green
    exit 0
} catch {
    Write-Host "[FAIL] SES outbound acceptance failed - $($_.Exception.Message)" `
        -ForegroundColor Red
    exit 1
}
