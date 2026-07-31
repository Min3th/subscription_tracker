[CmdletBinding()]
param(
    [string]$InboundDomain = "inbound.subtrak.xyz",
    [string]$ExpectedExchange = "inbound-smtp.ap-south-1.amazonaws.com",
    [ValidateRange(0, 65535)]
    [int]$ExpectedPreference = 10
)

$ErrorActionPreference = "Stop"

try {
    $records = @(
        Resolve-DnsName -Name $InboundDomain -Type MX -DnsOnly |
            Where-Object Type -eq "MX"
    )
} catch {
    Write-Host "[FAIL] $InboundDomain has no publicly resolvable MX record." `
        -ForegroundColor Red
    exit 1
}

if ($records.Count -eq 0) {
    Write-Host "[FAIL] $InboundDomain has no publicly resolvable MX record." `
        -ForegroundColor Red
    exit 1
}

$normalizedExpected = $ExpectedExchange.TrimEnd(".").ToLowerInvariant()
$matching = @(
    $records | Where-Object {
        $_.Preference -eq $ExpectedPreference -and
        $_.NameExchange.TrimEnd(".").ToLowerInvariant() -eq $normalizedExpected
    }
)
$unexpected = @(
    $records | Where-Object {
        $_.Preference -ne $ExpectedPreference -or
        $_.NameExchange.TrimEnd(".").ToLowerInvariant() -ne $normalizedExpected
    }
)

foreach ($record in $records) {
    Write-Host ("MX priority={0} exchange={1} ttl={2}" -f
        $record.Preference,
        $record.NameExchange.TrimEnd("."),
        $record.TTL)
}

if ($matching.Count -ne 1) {
    Write-Host "[FAIL] Expected exactly one matching SES MX record." `
        -ForegroundColor Red
    exit 1
}
if ($unexpected.Count -gt 0) {
    Write-Host "[FAIL] Unexpected MX records remain on the inbound-only hostname." `
        -ForegroundColor Red
    exit 1
}

Write-Host "[PASS] $InboundDomain routes exclusively to the expected SES receiving endpoint." `
    -ForegroundColor Green
exit 0
