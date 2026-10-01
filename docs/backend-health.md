# Backend health checks

Two public GET endpoints return only `{"status":"UP"}` (HTTP 200) or
`{"status":"DOWN"}` (HTTP 503), with `Cache-Control: no-store`.
No database host, exception, credentials, version, memory dump, or component
details are exposed. Other routes retain their existing authentication rules.

| Endpoint | Checks | Use |
| --- | --- | --- |
| `/health/live` | Spring application liveness; no database or AWS calls | Routine uptime/container monitoring |
| `/health/ready` | Application is accepting traffic, then validates a pooled JDBC connection | Deployment acceptance and dependency diagnostics |

A temporary Neon outage makes readiness fail, but does not change liveness.
Readiness also fails while Spring reports that it is refusing traffic, including
shutdown. Liveness does not assert that business operations can currently succeed.
Startup still requires Neon because Flyway and Hibernate initialize before the
application can serve requests; these endpoints do not bypass startup failures.

Use `/health/live` for frequent App Platform monitoring if the objective is to
allow Neon to suspend. Use `/health/ready` for a deployment smoke test. Polling
readiness frequently generates database activity and can prevent suspension;
using it as a restart trigger also couples restarts to database availability.
If the platform only offers one check, select it based on that tradeoff and
monitor dependency failures separately. Allow enough startup time for cold Neon
connections and migrations (measure the deployed application's startup).

Readiness reuses the application's pool and checks `Connection.isValid(1)`.
Only one database probe runs at a time; overlapping readiness requests return 503
instead of consuming more pool connections. Pool acquisition defaults to 2 seconds
and the validation call has a 1-second JDBC timeout. No per-request pool, executor,
or uncancelled background probe is created. Raising pool timeouts raises the
possible readiness latency; allow cold-start retries rather than treating one
failed probe as proof the application needs a restart.

## Connection pool settings

| Runtime variable | Default | Purpose |
| --- | --- | --- |
| `DB_POOL_MAX_SIZE` | `5` | Maximum application connections |
| `DB_POOL_MIN_IDLE` | `0` | Allow the idle pool to drain |
| `DB_POOL_CONNECTION_TIMEOUT_MS` | `2000` | Maximum pool acquisition wait |
| `DB_POOL_VALIDATION_TIMEOUT_MS` | `1000` | Hikari validation timeout; keep below acquisition timeout |
| `DB_POOL_IDLE_TIMEOUT_MS` | `60000` | Retire unused connections (housekeeping adds some variation) |
| `DB_POOL_KEEPALIVE_MS` | `0` | Disable Hikari's periodic idle-connection pings |

Pool limits apply to normal application requests as well as readiness. A cold
database or saturated pool can therefore cause transient request failures;
measure latency before increasing limits. These settings do not eliminate queries
from HTTP requests, startup backfill, or scheduled workers. See
[background worker controls](background-workers.md).

After rebuilding/restarting the backend, check locally:

```powershell
curl.exe -i http://localhost:8080/health/live
curl.exe -i http://localhost:8080/health/ready
```

With the frontend proxy, the paths are `/api/health/live` and `/api/health/ready`.
App Platform should use the direct backend paths without `/api`.

The supplied [health-check reference](https://nurbak.com/en/blog/health-check-endpoint/)
informed status codes, bounded checks, and cache prevention. This application
does not need the article's Redis checks, external API calls, memory thresholds,
or detailed public diagnostic payloads. The dependency-independent liveness
design follows [Spring Boot availability guidance](https://docs.spring.io/spring-boot/reference/features/spring-application.html).
