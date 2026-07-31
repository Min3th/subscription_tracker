[CmdletBinding()]
param(
    [string]$StackName = "subtrak-production-email",
    [string]$SesRegion = "ap-south-1",
    [string]$ApplicationRegion = "eu-north-1",
    [string]$InstanceName = "subscription-tracker",
    [string]$ServiceName = "subscription-tracker",
    [string]$FromEmail = "noreply@subtrak.xyz",
    [string]$FromName = "SubTrak",
    [string]$InboundDomain = "inbound.subtrak.xyz",
    [switch]$EnableSesOutbound,
    [switch]$EnableConsumers,
    [switch]$Apply
)

$ErrorActionPreference = "Stop"

function Invoke-Aws {
    param(
        [Parameter(Mandatory)][string[]]$Arguments,
        [switch]$Json
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $result = & aws @Arguments 2>&1
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
        throw [System.InvalidOperationException]::new($text)
    }
    if ($Json -and -not [string]::IsNullOrWhiteSpace($text)) {
        return $text | ConvertFrom-Json
    }
    return $text
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

foreach ($value in @($StackName, $SesRegion, $ApplicationRegion, $InstanceName,
        $ServiceName, $FromEmail, $FromName, $InboundDomain)) {
    if ($value -match "[`r`n]") {
        throw "Configuration values must not contain line breaks."
    }
}

$stackResponse = Invoke-Aws @(
    "cloudformation", "describe-stacks",
    "--stack-name", $StackName,
    "--region", $SesRegion,
    "--output", "json"
) -Json
$stack = $stackResponse.Stacks | Select-Object -First 1

$instances = Invoke-Aws @(
    "ec2", "describe-instances",
    "--region", $ApplicationRegion,
    "--filters",
    "Name=instance-state-name,Values=running",
    "Name=tag:Name,Values=$InstanceName",
    "--query", "Reservations[].Instances[].InstanceId",
    "--output", "json"
) -Json
if (@($instances).Count -ne 1) {
    throw "Expected exactly one running EC2 instance named '$InstanceName' in $ApplicationRegion."
}
$instanceId = @($instances)[0]

$settings = [ordered]@{
    EMAIL_OUTBOUND_PROVIDER = $(if ($EnableSesOutbound) { "ses" } else { "sendgrid" })
    SES_REGION = Get-OutputValue $stack "SesRegion"
    SES_CONFIGURATION_SET = Get-OutputValue $stack "SesConfigurationSet"
    SES_INBOUND_QUEUE_URL = Get-OutputValue $stack "SesInboundQueueUrl"
    SES_EVENT_QUEUE_URL = Get-OutputValue $stack "SesEventQueueUrl"
    SES_INBOUND_BUCKET = Get-OutputValue $stack "SesInboundBucket"
    SES_FROM_EMAIL = $FromEmail
    SES_FROM_NAME = $FromName
    SES_CONSUMERS_ENABLED = $EnableConsumers.ToString().ToLowerInvariant()
    SENDGRID_INBOUND_ENABLED = "true"
    INBOUND_EMAIL_DOMAIN = $InboundDomain
}

Write-Host "Target instance: $instanceId ($ApplicationRegion)"
foreach ($entry in $settings.GetEnumerator()) {
    $display = if ($entry.Key -match "URL|BUCKET") { "<from CloudFormation>" } else { $entry.Value }
    Write-Host "$($entry.Key)=$display"
}
if (-not $Apply) {
    Write-Host "Dry run only. Re-run with -Apply to install and restart the service."
    exit 0
}

$dropIn = @("[Service]") + @(
    $settings.GetEnumerator() |
        ForEach-Object {
            $escaped = $_.Value.Replace("\", "\\").Replace('"', '\"')
            "Environment=`"$($_.Key)=$escaped`""
        }
)
$encodedDropIn = [Convert]::ToBase64String(
    [Text.Encoding]::UTF8.GetBytes(($dropIn -join "`n") + "`n")
)
$dropInPath = "/etc/systemd/system/$ServiceName.service.d/20-ses.conf"
$remoteCommands = @(
    "set -e",
    "DROPIN='$dropInPath'",
    "BACKUP=`"`$DROPIN.previous`"",
    "sudo install -d -m 0755 `"`$(dirname `"`$DROPIN`")`"",
    "if sudo test -f `"`$DROPIN`"; then sudo cp `"`$DROPIN`" `"`$BACKUP`"; HAD_BACKUP=true; else HAD_BACKUP=false; fi",
    "rollback() { if [ `"`$HAD_BACKUP`" = true ]; then sudo mv `"`$BACKUP`" `"`$DROPIN`"; else sudo rm -f `"`$DROPIN`"; fi; sudo systemctl daemon-reload; sudo systemctl restart '$ServiceName'; }",
    "trap rollback ERR",
    "echo '$encodedDropIn' | base64 -d | sudo tee `"`$DROPIN.new`" >/dev/null",
    "sudo install -m 0644 `"`$DROPIN.new`" `"`$DROPIN`"",
    "sudo rm -f `"`$DROPIN.new`"",
    "sudo systemctl daemon-reload",
    "sudo systemctl restart '$ServiceName'",
    "sudo systemctl is-active --quiet '$ServiceName'",
    "trap - ERR",
    "sudo rm -f `"`$BACKUP`"",
    "echo SERVICE_ACTIVE=`$(systemctl is-active '$ServiceName')",
    "echo SES_RUNTIME_CONFIGURED=true"
)

$parameterFile = [IO.Path]::GetTempFileName()
try {
    [IO.File]::WriteAllText(
        $parameterFile,
        (@{ commands = $remoteCommands } | ConvertTo-Json -Depth 3),
        [Text.UTF8Encoding]::new($false)
    )
    $commandId = Invoke-Aws @(
        "ssm", "send-command",
        "--region", $ApplicationRegion,
        "--instance-ids", $instanceId,
        "--document-name", "AWS-RunShellScript",
        "--comment", "Configure SubTrak SES runtime",
        "--parameters", "file://$parameterFile",
        "--query", "Command.CommandId",
        "--output", "text"
    )
    $null = Invoke-Aws @(
        "ssm", "wait", "command-executed",
        "--region", $ApplicationRegion,
        "--command-id", $commandId,
        "--instance-id", $instanceId
    )
    $result = Invoke-Aws @(
        "ssm", "get-command-invocation",
        "--region", $ApplicationRegion,
        "--command-id", $commandId,
        "--instance-id", $instanceId,
        "--query", "{Status:Status,Output:StandardOutputContent,Error:StandardErrorContent}",
        "--output", "json"
    ) -Json
    $result | ConvertTo-Json
    if ($result.Status -ne "Success") {
        exit 1
    }
} finally {
    Remove-Item -LiteralPath $parameterFile -Force -ErrorAction SilentlyContinue
}
