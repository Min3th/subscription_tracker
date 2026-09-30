# Local backend memory test

Run commands from `subscription-service` with JDK 25 and Docker Desktop using Linux containers.
Both `java -version` and `.\mvnw.cmd -version` must report Java 25.
The backend container also uses Java 25.

```powershell
.\mvnw.cmd -DskipTests clean package
docker compose -f compose.memory-test.yml up -d
docker compose -f compose.memory-test.yml logs -f backend
```

Compose creates an isolated PostgreSQL 16 test database and applies the application's
Flyway migrations at startup. No existing database is needed. Its named volume preserves
test data across restarts. The database has no host port exposed.

The backend is available at http://localhost:8080. Its container is limited to 500 MiB
with no swap and a 192 MiB Java heap. Database memory is separate: this models a droplet
whose database is hosted elsewhere, not an entire 512 MiB server.

`.env.memory-test` contains local-only credentials and is ignored by Git. The dummy
SendGrid key satisfies startup validation but cannot deliver email. SES consumers and
SendGrid inbound endpoints are disabled. Set `GOOGLE_CLIENT_ID` to your test OAuth client
ID to exercise actual login; the supplied placeholder cannot authenticate Google users.
Do not substitute a production database or production queues.

Monitor in a second terminal:

```powershell
docker compose -f compose.memory-test.yml stats backend
```

Exercise authenticated subscription reads/writes with representative test data and
concurrent users for 30–60 minutes, then run a longer soak test. Check startup peaks,
scheduled jobs, response times, and memory growth. This default setup does not validate
real email delivery or SES consumer memory; those require separate test integrations
configured to match the intended deployment.

If the backend exits:

```powershell
$backendContainer = docker compose -f compose.memory-test.yml ps -aq backend
docker inspect $backendContainer --format '{{.State.OOMKilled}}'
docker compose -f compose.memory-test.yml logs --tail 100 backend
```

Stop the test (preserves database data):

```powershell
docker compose -f compose.memory-test.yml down
```

To deliberately delete the test database as well, use `down -v` instead.
Store profiling output in `memory-test-results/`, which is ignored along with heap
dumps and JVM crash logs. Keep Compose and this guide tracked so the setup is reproducible.
