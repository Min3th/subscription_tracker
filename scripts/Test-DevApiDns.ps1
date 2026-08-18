[CmdletBinding()]
param(
    [Parameter(Mandatory = $false)]
    [ValidatePattern('^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+$')]
    [string]$ApiDomain = 'api.dev.subtrak.xyz',

    [Parameter(Mandatory = $true)]
    [ValidateScript({
        $parsed = $null
        [System.Net.IPAddress]::TryParse($_, [ref]$parsed) -and
            $parsed.AddressFamily -eq [System.Net.Sockets.AddressFamily]::InterNetwork
    })]
    [string]$ExpectedIpv4Address,

    [Parameter(Mandatory = $false)]
    [ValidateRange(1, 60)]
    [int]$HttpTimeoutSeconds = 10
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

try {
    $resolvedAddresses = @(
        Resolve-DnsName -Name $ApiDomain -Type A -DnsOnly -ErrorAction Stop |
            Where-Object { $_.Type -eq 'A' } |
            Select-Object -ExpandProperty IPAddress -Unique
    )
}
catch {
    throw "DNS lookup failed for $ApiDomain. The A record may not have propagated yet."
}

if ($resolvedAddresses.Count -eq 0) {
    throw "No public A record was returned for $ApiDomain."
}

$unexpectedAddresses = @($resolvedAddresses | Where-Object { $_ -ne $ExpectedIpv4Address })
if ($unexpectedAddresses.Count -gt 0 -or $resolvedAddresses -notcontains $ExpectedIpv4Address) {
    throw "DNS is not ready. Expected only $ExpectedIpv4Address but resolved: $($resolvedAddresses -join ', ')."
}

$endpoint = "http://$ApiDomain/v3/api-docs"
try {
    $response = Invoke-WebRequest `
        -Uri $endpoint `
        -Method Get `
        -TimeoutSec $HttpTimeoutSeconds `
        -MaximumRedirection 0 `
        -UseBasicParsing
}
catch {
    throw "DNS resolves correctly, but the Nginx HTTP endpoint is not ready at $endpoint. $($_.Exception.Message)"
}

if ($response.StatusCode -ne 200) {
    throw "DNS resolves correctly, but $endpoint returned HTTP $($response.StatusCode)."
}

Write-Host "Dev API DNS readiness passed."
Write-Host "Domain: $ApiDomain"
Write-Host "IPv4 address: $ExpectedIpv4Address"
Write-Host "HTTP endpoint: $endpoint"
Write-Host 'TLS may now be enabled through the GitHub development environment.'
