[CmdletBinding()]
param(
    [switch]$RequireRemoved
)

$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
Push-Location $repositoryRoot

try {
    $matches = @(git grep -I -n -i sendgrid 2>$null)
    if ($LASTEXITCODE -notin @(0, 1)) {
        throw "git grep failed with exit code $LASTEXITCODE"
    }

    $entries = foreach ($match in $matches) {
        if ($match -match '^(?<path>.*?):(?<line>\d+):(?<text>.*)$') {
            [pscustomobject]@{
                Path = $Matches.path
                Line = [int]$Matches.line
                Text = $Matches.text.Trim()
            }
        }
    }

    $runtimePrefixes = @(
        ".github/workflows/",
        "scripts/",
        "subscription-service/pom.xml",
        "subscription-service/src/main/",
        "subscription-service/src/test/"
    )
    $runtimeEntries = @($entries | Where-Object {
        $path = $_.Path.Replace("\", "/")
        $path -ne "scripts/Get-SendGridRemovalInventory.ps1" -and
        ($runtimePrefixes | Where-Object { $path.StartsWith($_) })
    })

    $groups = [ordered]@{
        "Dependency and application code" = @($entries | Where-Object {
            $_.Path -eq "subscription-service/pom.xml" -or
            $_.Path.Replace("\", "/").StartsWith("subscription-service/src/main/")
        })
        "Tests" = @($entries | Where-Object {
            $_.Path.Replace("\", "/").StartsWith("subscription-service/src/test/")
        })
        "Deployment and operations" = @($entries | Where-Object {
            $path = $_.Path.Replace("\", "/")
            ($path.StartsWith(".github/workflows/") -or
            $path.StartsWith("scripts/") -or
            $path.StartsWith("infrastructure/")) -and
            $path -ne "scripts/Get-SendGridRemovalInventory.ps1"
        })
        "Documentation and policy" = @($entries | Where-Object {
            $path = $_.Path.Replace("\", "/")
            $path -eq "README.md" -or $path -eq "guidelines.md" -or
            $path.StartsWith("docs/")
        })
    }

    Write-Output "SENDGRID_TRACKED_MATCHES=$($entries.Count)"
    Write-Output "SENDGRID_RUNTIME_MATCHES=$($runtimeEntries.Count)"

    foreach ($group in $groups.GetEnumerator()) {
        $files = @($group.Value | Select-Object -ExpandProperty Path -Unique | Sort-Object)
        Write-Output ""
        Write-Output "[$($group.Key)] files=$($files.Count)"
        foreach ($file in $files) {
            Write-Output " - $file"
        }
    }

    Write-Output ""
    if ($runtimeEntries.Count -eq 0) {
        Write-Output "SENDGRID_REMOVAL_STATUS=REMOVED_FROM_RUNTIME"
        exit 0
    }

    Write-Output "SENDGRID_REMOVAL_STATUS=ROLLBACK_COMPONENTS_PRESENT"
    Write-Output "Inventory only: no files, configuration, endpoints, or secrets were changed."
    Write-Output "See docs/sendgrid-removal-checklist.md before removing these components."

    if ($RequireRemoved) {
        exit 1
    }
}
finally {
    Pop-Location
}
