[CmdletBinding()]
param(
    [string]$StackName = "subtrak-production-email",
    [string]$SesRegion = "ap-south-1",
    [string]$ApplicationRegion = "eu-north-1",
    [string]$InstanceName = "subscription-tracker",
    [string]$ServiceName = "subscription-tracker",
    [string]$InboundDomain = "inbound.subtrak.xyz"
)

$ErrorActionPreference = "Stop"
$script:Healthy = $true

function Write-Section {
    param([Parameter(Mandatory)][string]$Name)

    Write-Host ""
    Write-Host "=== $Name ===" -ForegroundColor Cyan
}

function Invoke-StatusScript {
    param(
        [Parameter(Mandatory)][string]$Path,
        [string[]]$Arguments = @()
    )

    $powerShell = (Get-Process -Id $PID).Path
    $output = & $powerShell -NoProfile -ExecutionPolicy Bypass -File $Path @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    $output | ForEach-Object { Write-Host $_ }
    if ($exitCode -ne 0) {
        $script:Healthy = $false
    }
}

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

Write-Section "Public inbound DNS"
Invoke-StatusScript `
    -Path (Join-Path $PSScriptRoot "Test-SesDnsCutover.ps1") `
    -Arguments @("-InboundDomain", $InboundDomain)

Write-Section "SES infrastructure and queues"
Invoke-StatusScript `
    -Path (Join-Path $PSScriptRoot "Test-SesReadiness.ps1") `
    -Arguments @(
        "-StackName", $StackName,
        "-Region", $SesRegion,
        "-RequireCutoverReady"
    )

Write-Section "Production application runtime"
try {
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

    $commands = @(
        "set -e",
        "echo INSTANCE_ID=$instanceId",
        "echo SERVICE_ACTIVE=`$(systemctl is-active '$ServiceName')",
        "PID=`$(systemctl show '$ServiceName' --property=MainPID --value)",
        "echo HTTP_STATUS=`$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 3 http://127.0.0.1:8080/v3/api-docs || true)",
        "for NAME in EMAIL_OUTBOUND_PROVIDER SES_CONSUMERS_ENABLED SENDGRID_INBOUND_ENABLED; do VALUE=`$(sudo grep -z -o `"^`$NAME=[^`$]*`" /proc/`$PID/environ | tr -d '\0' | cut -d= -f2-); if [ -n `"`$VALUE`" ]; then echo `"`$NAME=`$VALUE`"; else echo `"`$NAME=UNSET`"; fi; done",
        "if jar tf /home/ec2-user/subscription-service.jar | grep -Fqx 'BOOT-INF/classes/com/track/subscription_service/notification/service/SesEventQueueWorker.class'; then echo SES_EVENT_WORKER_PRESENT=true; else echo SES_EVENT_WORKER_PRESENT=false; fi",
        "if jar tf /home/ec2-user/subscription-service.jar | grep -Fqx 'BOOT-INF/classes/com/track/subscription_service/inboundemail/service/SesInboundQueueWorker.class'; then echo SES_INBOUND_WORKER_PRESENT=true; else echo SES_INBOUND_WORKER_PRESENT=false; fi"
    )

    $parameterFile = [IO.Path]::GetTempFileName()
    try {
        [IO.File]::WriteAllText(
            $parameterFile,
            (@{ commands = $commands } | ConvertTo-Json -Depth 3),
            [Text.UTF8Encoding]::new($false)
        )
        $commandId = Invoke-Aws @(
            "ssm", "send-command",
            "--region", $ApplicationRegion,
            "--instance-ids", $instanceId,
            "--document-name", "AWS-RunShellScript",
            "--comment", "Report SubTrak SES migration status",
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
    } finally {
        Remove-Item -LiteralPath $parameterFile -Force -ErrorAction SilentlyContinue
    }

    if ($result.Status -ne "Success") {
        throw "Runtime inspection failed with SSM status $($result.Status)."
    }

    $runtime = @{}
    foreach ($line in ($result.Output -split "\r?\n")) {
        if ($line -match "^([^=]+)=(.*)$") {
            $runtime[$matches[1]] = $matches[2]
        }
    }
    foreach ($name in @(
            "INSTANCE_ID",
            "SERVICE_ACTIVE",
            "HTTP_STATUS",
            "EMAIL_OUTBOUND_PROVIDER",
            "SES_CONSUMERS_ENABLED",
            "SENDGRID_INBOUND_ENABLED",
            "SES_EVENT_WORKER_PRESENT",
            "SES_INBOUND_WORKER_PRESENT")) {
        Write-Host "$name=$($runtime[$name])"
    }

    $runtimeHealthy =
        $runtime.SERVICE_ACTIVE -eq "active" -and
        $runtime.HTTP_STATUS -match "^[1-5]\d\d$" -and
        $runtime.HTTP_STATUS -ne "000" -and
        $runtime.EMAIL_OUTBOUND_PROVIDER -eq "ses" -and
        $runtime.SES_CONSUMERS_ENABLED -eq "true" -and
        $runtime.SES_EVENT_WORKER_PRESENT -eq "true" -and
        $runtime.SES_INBOUND_WORKER_PRESENT -eq "true"
    if ($runtimeHealthy) {
        Write-Host "[PASS] Production runtime is SES-active and responsive." `
            -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Production runtime does not match the expected SES state." `
            -ForegroundColor Red
        $script:Healthy = $false
    }
} catch {
    Write-Host "[FAIL] Runtime status could not be determined - $($_.Exception.Message)" `
        -ForegroundColor Red
    $script:Healthy = $false
}

Write-Host ""
if ($script:Healthy) {
    Write-Host "MIGRATION_STATUS=READY" -ForegroundColor Green
    exit 0
}
Write-Host "MIGRATION_STATUS=NOT_READY" -ForegroundColor Red
exit 1
