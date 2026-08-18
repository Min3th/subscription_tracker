# Dev API proxy and TLS

The dev API uses `api.dev.subtrak.xyz`. Nginx accepts public HTTP/HTTPS traffic
and proxies it only to Spring Boot on `127.0.0.1:8080`.

## GitHub environment variables

Add these non-secret variables to the GitHub `development` environment:

| Variable | Initial value |
| --- | --- |
| `API_DOMAIN` | `api.dev.subtrak.xyz` |
| `ENABLE_TLS` | `false` |
| `LETSENCRYPT_EMAIL` | account email used for certificate expiry notices |

Keep TLS disabled for the first proxy deployment. The workflow configures port
80, preserves the ACME challenge path, and verifies Nginx can reach the backend.

## DNS cutover

At the authoritative DNS provider for `subtrak.xyz`, create an `A` record:

| Type | Host | Value | TTL |
| --- | --- | --- | --- |
| `A` | `api.dev` | dev Terraform output `application_public_ip` | automatic or 5 minutes |

Do not change root-domain records, production API records, or inbound-email MX
records. Wait until public DNS resolves `api.dev.subtrak.xyz` to the dev Elastic
IP and plain HTTP reaches Nginx.

## Enable TLS

Set `ENABLE_TLS=true` in the GitHub `development` environment and manually run
the dev backend workflow from the `dev` branch. The Nginx configurator obtains a
certificate using Certbot's webroot HTTP-01 flow, keeps the challenge path on
port 80, redirects other HTTP requests to HTTPS, and installs a deploy hook that
reloads Nginx after renewal.

Verify externally:

```bash
curl --fail --silent --show-error \
  https://api.dev.subtrak.xyz/v3/api-docs \
  >/dev/null
```

Do not enable TLS before DNS resolves to the dev instance. Repeated issuance
attempts against incorrect DNS can consume certificate-authority rate limits.
