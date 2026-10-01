[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$SenderEmail,
    [string]$SenderName = 'SubTrak',
    [string]$StackName = 'subtrak-production-email',
    [string]$Region = 'ap-south-1',
    [string]$Profile,
    [string]$RuntimeUserName = 'subtrak-production-app-platform-email',
    [string]$OutputDirectory = (Join-Path $PSScriptRoot '../.local/app-platform-ses')
)

$ErrorActionPreference = 'Stop'
function Read-AwsJson {
    param([string[]]$AwsArguments)
    $cliArguments = @($AwsArguments) + @('--region', $Region, '--output', 'json', '--no-cli-pager')
    if ($Profile) { $cliArguments += @('--profile', $Profile) }
    $result = & aws @cliArguments
    if ($LASTEXITCODE -ne 0) { throw 'AWS discovery failed. Use an operator profile with STS and CloudFormation read access.' }
    return ($result -join "`n") | ConvertFrom-Json
}
function Require-Value {
    param([string]$Value, [string]$Pattern, [string]$Name)
    if ([string]::IsNullOrWhiteSpace($Value) -or $Value -cnotmatch $Pattern) {
        throw "Missing or invalid $Name. No configuration was exported."
    }
}

Require-Value $Region '^[a-z]{2}(-gov)?-[a-z]+-[0-9]+$' 'SES region'
Require-Value $RuntimeUserName '^[A-Za-z0-9+=,.@_-]{1,64}$' 'runtime user name'
Require-Value $SenderEmail '^[A-Za-z0-9._%+-]+@[a-z0-9.-]+$' 'sender address'
Require-Value $SenderName '^[A-Za-z0-9 ._-]+$' 'sender display name'
$identity = Read-AwsJson @('sts', 'get-caller-identity')
$response = Read-AwsJson @('cloudformation', 'describe-stacks', '--stack-name', $StackName)
$stack = $response.Stacks | Select-Object -First 1
if ($stack.StackStatus -notin @('CREATE_COMPLETE', 'UPDATE_COMPLETE')) { throw 'Email stack is not ready.' }
$account = ($stack.Parameters | Where-Object ParameterKey -eq 'AwsAccountId').ParameterValue
if ($account -ne $identity.Account) { throw 'Caller account does not match the existing SES stack.' }
$outputs = @{}
foreach ($output in $stack.Outputs) { $outputs[$output.OutputKey] = $output.OutputValue }
if ($outputs.SesRegion -ne $Region) { throw 'SES region does not match the existing stack.' }
Require-Value $outputs.SesDomainIdentity '^[a-z0-9][a-z0-9.-]+$' 'sending domain'
if (($SenderEmail -split '@')[1] -ne $outputs.SesDomainIdentity) { throw 'Sender must belong to the verified stack domain.' }
Require-Value $outputs.SesConfigurationSet '^[A-Za-z0-9_-]+$' 'configuration set'
Require-Value $outputs.SesInboundBucket '^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$' 'inbound bucket'
Require-Value $outputs.InboundMxRecordName '^[a-z0-9][a-z0-9.-]+$' 'inbound domain'
$queueNames = @{}
foreach ($key in @('SesInboundQueueUrl', 'SesEventQueueUrl')) {
    $queue = [Uri]$outputs[$key]
    $parts = $queue.AbsolutePath.Trim('/').Split('/')
    if ($queue.Scheme -ne 'https' -or $queue.Host -ne "sqs.$Region.amazonaws.com" -or
        $parts.Count -ne 2 -or $parts[0] -ne $account -or $queue.Query -or $queue.Fragment -or $queue.UserInfo) {
        throw "Invalid account, region, or URL in $key."
    }
    Require-Value $parts[1] '^[A-Za-z0-9_-]+(\.fifo)?$' 'queue name'
    $queueNames[$key] = $parts[1]
}
$values = [ordered]@{
    RuntimeUserName = $RuntimeUserName
    SesRegion = $Region
    SendingDomain = $outputs.SesDomainIdentity
    SenderEmail = $SenderEmail
    ConfigurationSetName = $outputs.SesConfigurationSet
    InboundQueueName = $queueNames.SesInboundQueueUrl
    EventQueueName = $queueNames.SesEventQueueUrl
    InboundBucketName = $outputs.SesInboundBucket
}
$parameters = @($values.GetEnumerator() | ForEach-Object {
    @{ ParameterKey = $_.Key; ParameterValue = $_.Value }
})
$runtime = @(
    '# Non-secret App Platform runtime configuration. Review before applying.',
    '# Consumers remain disabled until the old production workers are stopped.',
    'EMAIL_OUTBOUND_PROVIDER=ses',
    'SES_CONSUMERS_ENABLED=false',
    'SENDGRID_INBOUND_ENABLED=false',
    'AWS_EC2_METADATA_DISABLED=true',
    "AWS_REGION=$Region", "SES_REGION=$Region",
    "SES_FROM_EMAIL=$SenderEmail", "SES_FROM_NAME=$SenderName",
    "SES_CONFIGURATION_SET=$($outputs.SesConfigurationSet)",
    "SES_INBOUND_QUEUE_URL=$($outputs.SesInboundQueueUrl)",
    "SES_EVENT_QUEUE_URL=$($outputs.SesEventQueueUrl)",
    "SES_INBOUND_BUCKET=$($outputs.SesInboundBucket)",
    "INBOUND_EMAIL_DOMAIN=$($outputs.InboundMxRecordName)",
    '# Add AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY as encrypted RUN_TIME values in App Platform.',
    '# Preserve the existing INBOUND_EMAIL_TOKEN_ENCRYPTION_KEY separately.'
)
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$encoding = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Join-Path $OutputDirectory 'parameters.json'), ($parameters | ConvertTo-Json -Depth 5), $encoding)
[System.IO.File]::WriteAllLines((Join-Path $OutputDirectory 'runtime.env.example'), $runtime, $encoding)
Write-Host "Exported non-secret configuration to $OutputDirectory. No AWS resources or credentials were changed."
