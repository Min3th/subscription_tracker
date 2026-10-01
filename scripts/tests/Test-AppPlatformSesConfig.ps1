$ErrorActionPreference = 'Stop'
$repository = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$exporter = Join-Path $repository 'scripts/Export-AppPlatformSesConfig.ps1'
$testRoot = Join-Path $repository ('.local/ses-config-tests/' + [Guid]::NewGuid().ToString('N'))
$global:appPlatformSesTestCallerAccount = '123456789012'
$global:appPlatformSesTestOutputs = @{
    SesRegion = 'ap-south-1'
    SesDomainIdentity = 'example.com'
    SesConfigurationSet = 'existing-events'
    SesInboundBucket = 'existing-inbound-bucket'
    InboundMxRecordName = 'inbound.example.com'
    SesInboundQueueUrl = 'https://sqs.ap-south-1.amazonaws.com/123456789012/inbound'
    SesEventQueueUrl = 'https://sqs.ap-south-1.amazonaws.com/123456789012/events'
}

# Shadow the AWS CLI: any unexpected API invocation fails instead of contacting AWS.
function aws {
    $global:LASTEXITCODE = 0
    if ($args[0] -eq 'sts' -and $args[1] -eq 'get-caller-identity') {
        return (@{ Account = $global:appPlatformSesTestCallerAccount } | ConvertTo-Json)
    }
    if ($args[0] -eq 'cloudformation' -and $args[1] -eq 'describe-stacks') {
        return (@{ Stacks = @(@{
            StackStatus = 'UPDATE_COMPLETE'
            Parameters = @(@{ ParameterKey = 'AwsAccountId'; ParameterValue = '123456789012' })
            Outputs = @($global:appPlatformSesTestOutputs.GetEnumerator() | ForEach-Object {
                @{ OutputKey = $_.Key; OutputValue = $_.Value }
            })
        }) } | ConvertTo-Json -Depth 8)
    }
    throw 'Unexpected AWS operation in exporter.'
}
function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}
function Assert-Rejected {
    param([string]$Name, [string]$Sender = 'noreply@example.com')
    $destination = Join-Path $testRoot $Name
    $rejected = $false
    try { & $exporter -SenderEmail $Sender -OutputDirectory $destination } catch { $rejected = $true }
    Assert-True $rejected "Unsafe configuration was accepted: $Name"
    Assert-True (-not (Test-Path $destination)) "Unsafe export wrote files: $Name"
}

& $exporter -SenderEmail 'noreply@example.com' -OutputDirectory (Join-Path $testRoot 'valid')
$parameters = Get-Content (Join-Path $testRoot 'valid/parameters.json') -Raw | ConvertFrom-Json
Assert-True ($parameters.Count -eq 8) 'Missing template parameters.'
Assert-True (($parameters | Where-Object ParameterKey -eq 'EventQueueName').ParameterValue -eq 'events') 'Incorrect queue extraction.'
$runtime = Get-Content (Join-Path $testRoot 'valid/runtime.env.example') -Raw
Assert-True ($runtime -match '(?m)^SES_CONSUMERS_ENABLED=false\r?$') 'Consumers must default to disabled.'
Assert-True ($runtime -match '(?m)^INBOUND_EMAIL_DOMAIN=inbound.example.com\r?$') 'Incorrect stack output for inbound domain.'
Assert-True ($runtime -notmatch '(?m)^AWS_(ACCESS_KEY_ID|SECRET_ACCESS_KEY|SESSION_TOKEN)=') 'Exporter must not emit credentials.'

Assert-Rejected 'wrong-sender' 'noreply@another.example'
$global:appPlatformSesTestCallerAccount = '999999999999'
Assert-Rejected 'wrong-account'
$global:appPlatformSesTestCallerAccount = '123456789012'
$global:appPlatformSesTestOutputs.SesEventQueueUrl = 'https://sqs.ap-south-1.amazonaws.com/999999999999/events'
Assert-Rejected 'wrong-queue-account'
$global:appPlatformSesTestOutputs.SesEventQueueUrl = 'https://sqs.eu-west-1.amazonaws.com/123456789012/events'
Assert-Rejected 'wrong-queue-region'
$global:appPlatformSesTestOutputs.SesEventQueueUrl = 'https://sqs.ap-south-1.amazonaws.com/123456789012/events'
$global:appPlatformSesTestOutputs.Remove('SesConfigurationSet')
Assert-Rejected 'missing-output'

$template = Get-Content (Join-Path $repository 'infrastructure/app-platform-ses-access.json') -Raw | ConvertFrom-Json
Assert-True (@($template.Resources.PSObject.Properties).Count -eq 1) 'Access stack must own only its runtime identity.'
Assert-True ($template.Resources.RuntimeUser.Type -eq 'AWS::IAM::User') 'Unexpected resource type.'
Assert-True (-not $template.Resources.RuntimeUser.Properties.LoginProfile) 'Runtime user must not have console access.'
$policy = $template.Resources.RuntimeUser.Properties.Policies[0].PolicyDocument
$allowed = @('ses:SendEmail', 'sqs:ReceiveMessage', 'sqs:DeleteMessage', 'sqs:ChangeMessageVisibility', 'sqs:GetQueueAttributes', 's3:GetObject')
foreach ($statement in $policy.Statement) {
    foreach ($action in @($statement.Action)) { Assert-True ($action -in $allowed) "Unexpected runtime permission: $action" }
    Assert-True (@($statement.Resource) -notcontains '*') 'Unscoped resource permission.'
}
Assert-True ($policy.Statement[0].Condition.StringEquals.'ses:FromAddress'.Ref -eq 'SenderEmail') 'Missing sender restriction.'
Write-Host 'PASS: export, account/region/domain rejection, disabled consumers, and IAM scope checks (no AWS calls).'
