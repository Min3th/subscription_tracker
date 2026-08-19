# Dev frontend deployment

The development frontend is `https://dev.subtrak.xyz` and calls the development
backend directly at `https://api.dev.subtrak.xyz`. The frontend and backend must
use the same Google OAuth web client ID.

## Vercel configuration

In the Vercel project, add these build-time environment variables to the Preview
environment and scope them to branch `dev` where Vercel offers branch scoping:

| Variable | Value |
| --- | --- |
| `VITE_API_BASE_URL` | `https://api.dev.subtrak.xyz` |
| `VITE_GOOGLE_CLIENT_ID` | development Google OAuth web client ID |

Do not include a trailing slash in `VITE_API_BASE_URL`. Values beginning with
`VITE_` are compiled into the browser bundle and are public, so never place a
client secret, AWS credential, JWT secret, database credential, or provider API
key in them. A Google OAuth client ID is an identifier and is safe to expose;
its client secret is not.

Redeploy the `dev` branch after changing a Vercel build variable. Changing the
variable does not modify an already built deployment.

The Vite build logs the selected public API base URL and refuses to build with
the retired `subtrak-api.duckdns.org` endpoint. If Vercel reports that failure,
inspect duplicate project, Preview, and branch-scoped values for
`VITE_API_BASE_URL`; do not bypass the guard or reintroduce the legacy host.

## Google OAuth configuration

Open the Google Cloud project that owns the development OAuth web client and add
this exact authorized JavaScript origin:

```text
https://dev.subtrak.xyz
```

Google Identity Services returns a credential to the page; Subtrak does not use
a browser redirect callback for this flow. Do not add paths or a trailing slash
to the JavaScript origin. Keep production origins authorized only where the same
client is intentionally shared; a separate development OAuth client is preferred
when environments need independent consent and access controls.

## Backend runtime configuration

Update the existing SSM Parameter Store value `/subtrak/dev/runtime` in the
development AWS account. Preserve every unrelated key and set these non-secret
values:

```json
{
  "GOOGLE_CLIENT_ID": "the-same-development-client-id.apps.googleusercontent.com",
  "PUBLIC_API_URL": "https://api.dev.subtrak.xyz",
  "FRONTEND_BASE_URL": "https://dev.subtrak.xyz",
  "FRONTEND_ORIGINS": "https://dev.subtrak.xyz",
  "REFRESH_COOKIE_SECURE": "true",
  "REFRESH_COOKIE_SAME_SITE": "None"
}
```

The JSON above is a partial illustration, not a replacement document. Do not
overwrite database, SES, JWT issuer/audience, inbound-domain, or other existing
runtime keys. Parameter Store keeps this object as a JSON string.

After updating the parameter, rerun **Build and Deploy Dev Backend** or restart
`subscription-tracker.service` through the approved deployment path. Runtime
values are loaded when the Java process starts; editing Parameter Store alone
does not update a running process.

`FRONTEND_ORIGINS` is an exact comma-separated origin allowlist. Do not add
Vercel wildcard domains. Add `https://localhost:5173` only for a deliberate local
browser-to-dev-API test, then remove it afterward. Never allow production to use
development origins or vice versa.

## Acceptance test

After both deployments are current:

1. Open `https://dev.subtrak.xyz` in a private browser window.
2. Sign in with Google and confirm no origin or OAuth client error appears.
3. Confirm `/auth/google` succeeds against `https://api.dev.subtrak.xyz`.
4. Reload the page and confirm `/auth/refresh` succeeds with its secure HttpOnly
   cookie while the access token remains in memory.
5. Open Settings, generate or retrieve the development forwarding address, and
   confirm authenticated suggestion APIs load without CORS errors.
6. Sign out and confirm the refresh session is revoked and the cookie cleared.

Use browser developer tools only to inspect request URLs, status codes, CORS
headers, and cookie attributes. Never copy credentials, JWTs, Google credential
responses, or cookie values into logs, screenshots, issues, or chat.
