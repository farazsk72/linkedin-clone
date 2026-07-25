# Running the stack locally

The frontend lives in a sibling folder (`../linkedInProject FD`) and in its own repo: **[linkedin-clone-frontend](https://github.com/farazsk72/linkedin-clone-frontend)**.

## 1. Prerequisites

### Java 21 — required

The project targets Java 21 and its Lombok version cannot process JDK 25. With
JDK 25 on `PATH` every module fails with `TypeTag :: UNKNOWN` or missing
Lombok-generated members. Point `JAVA_HOME` at a 21 before building or running:

```bash
export JAVA_HOME="C:/Users/faraz/.jdks/ms-21.0.8"
```

### Port 9090 must be free

The API gateway binds 9090. Check nothing else holds it:

```powershell
Get-NetTCPConnection -LocalPort 9090 -State Listen
```

In-cluster the gateway still listens on 8080 — the `k8s` Spring profile pins it
so the existing manifests (containerPort, probe, Service targetPort) keep
working. Override locally with `SERVER_PORT` if you need a different port.

The `/auth/**` routes are rate limited to 10 requests / 60s **per client IP**,
so a test script that logs several accounts in quickly will hit HTTP 429. The
limiter is in-memory, so this multiplies by the number of gateway instances;
tune with `RATELIMIT_REQUESTS` / `RATELIMIT_WINDOW_SECONDS`.

### Postgres

Expected on `localhost:5432` with these databases:

| Database | Service |
|---|---|
| `userdb` | userService |
| `postsdb` | postsService |
| `notificationdb` | notification-service |

The names are lower case on purpose. Postgres folds unquoted identifiers and the
JDBC URL compares the database name exactly, so `CREATE DATABASE userDB` yields
`userdb` and a URL ending `/userDB` would then fail to connect. Quote the
identifier if you want the camelCase form, and set `DB_NAME` to match.

```sql
CREATE DATABASE userdb;
CREATE DATABASE postsdb;
CREATE DATABASE notificationdb;
```

Schemas are created automatically (`spring.jpa.hibernate.ddl-auto=update`).

### Database credentials — required

This server uses `scram-sha-256` for local and host connections, so the blank
username/password the repo ships with cannot authenticate. Each of the three
JPA services reads its credentials from a gitignored local profile:

```
<service>/src/main/resources/application-local.properties
    spring.datasource.username=postgres
    spring.datasource.password=<yours>
```

Create those three files, then start those services with the profile active:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Uploader storage backend

uploader-service supports three storage backends via `uploader.backend`:
`cloudinary` (default), `gcs`, or `local`. For local development the easiest
path needs **no cloud credentials** — set `uploader.backend=local` (the
gitignored `application-local.yml` already does this) and uploads are written to
`uploader-service/uploads-data/` and served back through the gateway's
unauthenticated `GET /api/v1/uploads/file/**` route.

For the cloud backends the secrets come only from the gitignored
`application-local.yml` (or `CLOUDINARY_API_SECRET` / `GCLOUD_STORAGE_ACCESS_KEY`
env vars) — never committed. The previously-committed values were removed and
scrubbed from git history; if you ever reuse the old keys, rotate them first.

DB connection settings are likewise overridable by environment variable —
`DB_SERVER`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` — the same names
the `k8s` profile uses.

### Neo4j and Kafka

```bash
docker compose up -d
```

Neo4j on 7687 (`neo4j`/`password`), Kafka on 9092. Both are required: the
network page is dead without Neo4j, and notifications never arrive without
Kafka. A Kafka UI is bundled at http://localhost:8090 for inspecting topics,
messages, and consumer-group lag (it reaches the broker over an internal
`linkedin-kafka:29092` listener; host clients still use `localhost:9092`).

### Observability (Actuator + Prometheus + Zipkin + Grafana)

`docker compose up -d` also starts the monitoring stack. All seven services
carry Actuator, a Micrometer Prometheus registry, and Micrometer tracing that
reports to Zipkin.

| Tool | URL | Notes |
|---|---|---|
| Zipkin | http://localhost:9411 | distributed traces across gateway → services → Kafka |
| Prometheus | http://localhost:9099 | 9090 is the gateway, so Prometheus is on 9099 |
| Loki | http://localhost:3100 | aggregated logs; Promtail tails the services' log files |
| Grafana | http://localhost:3000 | `admin`/`admin`; Prometheus + Loki pre-wired |
| Kafka UI | http://localhost:8090 | browse topics, messages, and consumer-group lag |

Each service writes `logs/<app-name>.log` (repo-root `logs/`, gitignored) in addition
to stdout; Promtail ships those to Loki, labelled by `service`. In Grafana's Loki
view a log line's trace id is a clickable link to that trace in Zipkin — so the
three signals (metrics, logs, traces) are joined up. Query logs directly too:
`curl -sG http://localhost:3100/loki/api/v1/query_range --data-urlencode 'query={service="posts-service"}'`.

Per-service actuator endpoints (`health`, `info`, `metrics`, `prometheus`) live
**under each service's context-path**, e.g.:

```bash
curl http://localhost:9020/users/actuator/health          # user-service
curl http://localhost:9020/users/actuator/prometheus      # its metrics scrape
curl http://localhost:9090/actuator/health                # gateway (no context-path)
curl http://localhost:9090/actuator/gateway/routes        # gateway route table
```

Tracing samples 100% locally (`management.tracing.sampling.probability=1.0`);
dial it down in production. Trace and span ids are stamped into every log line.
Prometheus scrapes the host services via `host.docker.internal` — the targets
and their context-path metric-paths are in `docker/prometheus/prometheus.yml`.
Override the collector per service with `ZIPKIN_ENDPOINT` if Zipkin runs
elsewhere.

## 2. Start the services

Order matters — Eureka first, gateway last:

| Service | Port | Profile |
|---|---|---|
| DiscoverServer | 8761 | — |
| userService | 9020 | `local` |
| postsService | 9010 | `local` |
| ConnectionsService | 9030 | — |
| notification-service | 9040 | `local` |
| uploader-service | 9050 | `local` |
| APIGateway | 9090 | — |

```bash
cd <service> && ./mvnw spring-boot:run [-Dspring-boot.run.profiles=local]
```

Confirm all five services register at http://localhost:8761, and that their
`hostName` is an **IP address**. Each service sets
`eureka.instance.prefer-ip-address=true` for this reason: without it they
register under this machine's `.mshome.net` name, which does not resolve, and
every gateway route then fails with:

```
java.net.UnknownHostException: Failed to resolve '<host>.mshome.net' ... NXDOMAIN
```

That surfaces as a 500 from the gateway on every request, which looks like a
routing bug but is DNS.

To stop a service, kill it by port — `Ctrl-C` on the Maven wrapper can leave the
forked JVM holding the port:

```powershell
Stop-Process -Id (Get-NetTCPConnection -LocalPort 9020 -State Listen).OwningProcess -Force
```

## 3. Start the frontend

```bash
cd "../linkedInProject FD"
npm install
npm run dev
```

Opens on http://localhost:5173. Vite proxies `/api` to the gateway so dev is
same-origin; the gateway's CORS block is what a deployed build relies on.

## 4. Automated tests (postsService)

`postsService` carries a representative JUnit 5 + Mockito + Testcontainers suite.
It needs JDK 21 but **no running services** — the unit tests are self-contained
and the repository slice test spins its own Postgres via Docker.

```bash
cd postsService

# Unit + Mockito tests only (no Docker needed) - runs in seconds:
JAVA_HOME="C:/Users/faraz/.jdks/ms-21.0.8" ./mvnw test \
  -Dtest=HashtagExtractorTest,PostCreationSagaTest,OutboxRelayTest

# Everything, including the Testcontainers Postgres slice test:
JAVA_HOME="C:/Users/faraz/.jdks/ms-21.0.8" ./mvnw test
```

What each class demonstrates:

| Test | Kind | What it pins down |
|---|---|---|
| `HashtagExtractorTest` | pure JUnit | the hashtag regex edge cases (dedupe, `C#`, trailing punctuation) |
| `PostCreationSagaTest` | Mockito | the saga **compensation** logic — a failed publish deletes the committed post; a pre-commit failure compensates nothing; drafts don't publish |
| `OutboxRelayTest` | Mockito | the relay marks an event processed only after Kafka acks, and leaves it pending on broker failure |
| `PostsRepositoryIntegrationTest` | Testcontainers | the batched N+1-avoiding queries + outbox queries against **real Postgres** |

**Testcontainers on Docker Desktop (Windows):** the slice test needs the Docker
HTTP API over the named pipe. Docker Desktop 29's pipe proxy rejects that raw
`/info` call with HTTP 400 (the `docker` CLI itself is unaffected), so the test
errors with *"Could not find a valid Docker environment"* even though `docker ps`
works. Fix by exposing the daemon over TCP — Docker Desktop → Settings → General →
**"Expose daemon on tcp://localhost:2375 without TLS"**, then
`export DOCKER_HOST=tcp://localhost:2375` — or just run the suite in CI (Linux
Docker), where it connects with no extra setup. The three unit/Mockito classes
above are unaffected and always run.

## 5. Smoke test the API

```bash
# Signup + login. Login returns {accessToken, refreshToken} - it used to be a
# bare token string. Access tokens last 15 minutes; sessions survive on the
# refresh token, which is stored in Postgres and can be revoked.
curl -X POST http://localhost:9090/api/v1/users/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"Ada","email":"ada@example.com","password":"secret"}'

AUTH=$(curl -s -X POST http://localhost:9090/api/v1/users/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ada@example.com","password":"secret"}')
TOKEN=$(echo "$AUTH" | sed -E 's/.*"accessToken":"([^"]*)".*/\1/')
REFRESH=$(echo "$AUTH" | sed -E 's/.*"refreshToken":"([^"]*)".*/\1/')

# Refresh rotates the token - the old one is dead immediately afterwards.
curl -X POST http://localhost:9090/api/v1/users/auth/refresh \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$REFRESH\"}"

# Logout revokes server-side. Changing your password revokes every session.
curl -X POST http://localhost:9090/api/v1/users/auth/logout \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$REFRESH\"}"

curl http://localhost:9090/api/v1/users/profile/me      -H "Authorization: Bearer $TOKEN"

# Profile edit. Omitted fields are left alone, "" clears one, and the target is
# always the caller's own id from the token - there is no path variable.
curl -X PUT http://localhost:9090/api/v1/users/profile/me \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"headline":"Backend engineer","location":"Pune"}'

# Experience and education. Reads take a userId (any profile is viewable);
# writes take none, because they always act on the caller. A client-supplied
# id in the body is ignored, and deleting someone else's row returns 404.
curl http://localhost:9090/api/v1/users/profile/1/experience -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:9090/api/v1/users/profile/experience \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Engineer","company":"Acme","startDate":"2020-01-01"}'   # null endDate = current role
curl -X DELETE http://localhost:9090/api/v1/users/profile/experience/1 -H "Authorization: Bearer $TOKEN"
# ... and the same three shapes under /education

# Search matches name, headline OR company, excludes you, and is paged.
curl "http://localhost:9090/api/v1/users/profile/search?q=Acme&page=0&size=10" \
  -H "Authorization: Bearer $TOKEN"

# Change password. Under /profile, not /auth - the gateway leaves /auth/** open.
# Requires the current password; minimum 8 characters. Existing tokens stay
# valid until they expire, since there is no token store to revoke against.
curl -X POST http://localhost:9090/api/v1/users/profile/change-password \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"currentPassword":"secret","newPassword":"newsecret123"}'

# Reposts. Optional commentary; resharing your own post requires it. Reposting
# a repost points at the underlying original, so the chain never nests.
curl -X POST http://localhost:9090/api/v1/posts/core/1/repost \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"Worth reading"}'

# Hashtags are extracted from post content on create and edit, stored
# lower-cased. Tag browsing returns PUBLIC posts only.
curl "http://localhost:9090/api/v1/posts/core/tag/java" -H "Authorization: Bearer $TOKEN"

# Post visibility: PUBLIC (default) or CONNECTIONS. A CONNECTIONS post is
# hidden from non-connections viewing the author's profile, and is filtered in
# the query so totalElements stays honest.
curl -X POST http://localhost:9090/api/v1/posts/core -H "Authorization: Bearer $TOKEN" \
  -F 'post={"content":"Just for my network","visibility":"CONNECTIONS"};type=application/json'

# Drafts. A draft is excluded from every read except its author's draft list -
# feed, profile, tag browse and permalink all filter on status.
curl -X POST http://localhost:9090/api/v1/posts/core -H "Authorization: Bearer $TOKEN" \
  -F 'post={"content":"Work in progress","draft":true};type=application/json'
curl http://localhost:9090/api/v1/posts/core/drafts -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:9090/api/v1/posts/core/1/publish -H "Authorization: Bearer $TOKEN"

# Comment replies. Threading is one level: replying to a reply attaches to the
# same parent, and deleting a parent takes its replies with it.
curl -X POST http://localhost:9090/api/v1/posts/comments/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"A reply","parentCommentId":7}'

# Who viewed your profile. Recorded as a side effect of GET /profile/{userId};
# viewing your own profile does not count.
curl http://localhost:9090/api/v1/users/profile/viewers -H "Authorization: Bearer $TOKEN"
curl http://localhost:9090/api/v1/users/profile/viewers/count -H "Authorization: Bearer $TOKEN"

# Notification housekeeping.
curl -X DELETE http://localhost:9090/api/v1/notifications/core/1 -H "Authorization: Bearer $TOKEN"
curl -X DELETE http://localhost:9090/api/v1/notifications/core -H "Authorization: Bearer $TOKEN"

# Direct messaging. Own gateway route (/users/messages/**). Blocked users in
# either direction cannot message each other; fetching a thread marks it read.
curl -X POST http://localhost:9090/api/v1/users/messages \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"recipientId":2,"content":"Hello"}'
curl http://localhost:9090/api/v1/users/messages -H "Authorization: Bearer $TOKEN"          # conversations
curl http://localhost:9090/api/v1/users/messages/2 -H "Authorization: Bearer $TOKEN"        # thread (marks read)
curl http://localhost:9090/api/v1/users/messages/unread-count -H "Authorization: Bearer $TOKEN"

# Block / unblock. Blocked users vanish from search in both directions and
# cannot message you. Under /profile, so it rides the existing route.
curl -X POST http://localhost:9090/api/v1/users/profile/block/2 -H "Authorization: Bearer $TOKEN"
curl http://localhost:9090/api/v1/users/profile/blocks -H "Authorization: Bearer $TOKEN"

# Follow (one-directional, no acceptance). Followed users' PUBLIC posts show in
# the "following feed"; their connections-only posts stay hidden.
curl -X POST http://localhost:9090/api/v1/connections/core/follow/2 -H "Authorization: Bearer $TOKEN"
curl http://localhost:9090/api/v1/connections/core/2/followers -H "Authorization: Bearer $TOKEN"
curl http://localhost:9090/api/v1/posts/core/following-feed -H "Authorization: Bearer $TOKEN"

# Follow hashtags + trending. Trending counts public, published posts per tag
# over a recent window.
curl -X POST http://localhost:9090/api/v1/posts/hashtags/java/follow -H "Authorization: Bearer $TOKEN"
curl http://localhost:9090/api/v1/posts/hashtags/following/feed -H "Authorization: Bearer $TOKEN"
curl "http://localhost:9090/api/v1/posts/hashtags/trending?days=7&limit=10" -H "Authorization: Bearer $TOKEN"

# Skills + endorsements. Add your own; endorse other people's. Self-endorsement
# is rejected. Re-adding an existing skill returns the existing row.
curl http://localhost:9090/api/v1/users/profile/1/skills -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:9090/api/v1/users/profile/skills \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"name":"Java"}'
curl -X POST http://localhost:9090/api/v1/users/profile/skills/1/endorse -H "Authorization: Bearer $TOKEN"

# Comment edit (author only) and comment likes.
curl -X PUT http://localhost:9090/api/v1/posts/comments/single/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"content":"Reworded"}'
curl -X POST http://localhost:9090/api/v1/posts/comments/single/1/like -H "Authorization: Bearer $TOKEN"

# Notification preferences. Muting drops events at delivery, so a muted type
# never reaches the list. Types: POST_CREATED, POST_LIKED, POST_COMMENTED,
# POST_REPOSTED. Absence of a stored row means enabled.
curl http://localhost:9090/api/v1/notifications/core/preferences -H "Authorization: Bearer $TOKEN"
curl -X PUT http://localhost:9090/api/v1/notifications/core/preferences \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"type":"POST_LIKED","enabled":false}'

# Saved posts.
curl -X POST http://localhost:9090/api/v1/posts/saved/1 -H "Authorization: Bearer $TOKEN"
curl "http://localhost:9090/api/v1/posts/saved?page=0&size=10" -H "Authorization: Bearer $TOKEN"
curl -X DELETE http://localhost:9090/api/v1/posts/saved/1 -H "Authorization: Bearer $TOKEN"

# Connection status drives the UI button:
# SELF | CONNECTED | PENDING_OUTGOING | PENDING_INCOMING | NOT_CONNECTED
curl http://localhost:9090/api/v1/connections/core/status/2 -H "Authorization: Bearer $TOKEN"

# Feed is paged. `size` is clamped to 1-50 server-side; the response is a
# PageResponse ({content, page, size, totalElements, totalPages, last}), not a
# bare array. Trust `last` rather than inferring the end from a short page.
curl "http://localhost:9090/api/v1/posts/core/feed?page=0&size=10" -H "Authorization: Bearer $TOKEN"

# Edit / delete your own post. Deleting also removes its likes and comments -
# they key off postId with no FK, so nothing cascades on its own.
curl -X PUT http://localhost:9090/api/v1/posts/core/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"Updated text","removeImage":false}'
curl -X DELETE http://localhost:9090/api/v1/posts/core/1 -H "Authorization: Bearer $TOKEN"
curl http://localhost:9090/api/v1/connections/core/requests -H "Authorization: Bearer $TOKEN"

# People you may know - second-degree connections ranked by mutual count.
# Scoped to the token; limit is clamped to 1-50 server-side.
curl "http://localhost:9090/api/v1/connections/core/suggestions?limit=10" \
  -H "Authorization: Bearer $TOKEN"

# Withdraw an invitation you sent, or remove an existing connection. Removal is
# undirected - it disappears from both sides.
curl -X DELETE http://localhost:9090/api/v1/connections/core/request/5 -H "Authorization: Bearer $TOKEN"
curl -X DELETE http://localhost:9090/api/v1/connections/core/3 -H "Authorization: Bearer $TOKEN"
curl http://localhost:9090/api/v1/notifications/core    -H "Authorization: Bearer $TOKEN"

# Read state. Marking a notification that belongs to someone else returns 404,
# not 403 - a 403 would confirm it exists.
curl http://localhost:9090/api/v1/notifications/core/unread-count -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:9090/api/v1/notifications/core/1/read -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:9090/api/v1/notifications/core/read-all -H "Authorization: Bearer $TOKEN"

# Comments. Delete is keyed by comment id (hence /single) and is allowed for the
# comment's author or the post's owner.
curl -X POST http://localhost:9090/api/v1/posts/comments/1 \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"content":"Nice post"}'
curl http://localhost:9090/api/v1/posts/comments/1 -H "Authorization: Bearer $TOKEN"
curl -X DELETE http://localhost:9090/api/v1/posts/comments/single/1 -H "Authorization: Bearer $TOKEN"
```

CORS preflight should return 200 with `Access-Control-Allow-Origin`, not 401:

```bash
curl -i -X OPTIONS http://localhost:9090/api/v1/posts/core \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST"
```

## 5. End-to-end check

Two accounts in separate browser profiles:

1. A posts with an image → it renders in A's feed
2. B searches for A → sends a connection request
3. A sees it under **Network** → accepts
4. B's feed now shows A's post
5. B likes it → count increments and survives a reload
6. A sees a notification under **Notifications** (proves the Kafka round-trip)

## Known issues

- **The first event published to a brand-new Kafka topic is dropped.** Consumers
  run with `auto.offset.reset=latest`, so until the group has committed an
  offset it starts at the end of the log. If you add a topic and publish to it
  before the consumer has ever joined, that event is skipped — every later one
  arrives. Switching those consumers to `earliest` would fix it, at the cost of
  replaying the whole topic (and re-sending every notification) any time a
  consumer group is reset.
- Every service declares the topics it consumes as well as the ones it produces.
  That is deliberate: a consumer subscribing to a missing topic makes the broker
  auto-create it with `num.partitions=1`, and the group then keeps that stale
  single-partition assignment after the owning service widens the topic to 3.

- **Uploader secrets are no longer committed.** The previously-committed
  Cloudinary/GCP values were removed from the current tree and scrubbed from git
  history. Real credentials now come from the gitignored local profile or env
  vars, and a credential-free `local` disk backend is available for development.
- The JWT secret is duplicated in `APIGateway` and `userService` configs.
- Tokens last 100 minutes with no refresh endpoint; the frontend redirects to
  `/login` on the resulting 401.
- `GET /connections/core/{userId}/first-degree` takes the user id from the path
  rather than the token, so any user can read anyone's connection list.
