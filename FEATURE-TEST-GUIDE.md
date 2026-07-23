# Feature Test Guide — LinkedIn Clone

A sequential walkthrough of every feature, front to back. Work top to bottom;
each section builds on the previous one (accounts get connected, posts get
created, etc.). Tick the boxes as you go.

- **Setup / startup / credentials:** see `README-local-dev.md`.
- **Two browser profiles** (or one normal + one incognito) so you can act as
  two users at once — most social features need a second person.
- Frontend: <http://localhost:5173> · Gateway API: <http://localhost:9090>

---

## 0. Test accounts (seeded)

| Name | Email | Password |
|---|---|---|
| Augusta Ada King (Ada) | `ada@example.com` | `secret` |
| Grace Hopper | `grace@example.com` | `secret` |
| Alan Turing | `alan@example.com` | `anothersecret1` |
| Katherine Johnson | `katherine@example.com` | `secret` |
| Margaret Hamilton | `margaret@example.com` | `secret` |

> If you want a clean slate, sign up fresh accounts instead — signup is the first test below.

Throughout, **User A = Ada**, **User B = Grace** unless stated.

---

## 1. Authentication

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 1.1 | Open `/signup`, create a new account | Lands on the feed, logged in | ☐ |
| 1.2 | Sign out (navbar) | Redirected to `/login` | ☐ |
| 1.3 | Log in with wrong password | Clear "incorrect email or password" error, stays on login | ☐ |
| 1.4 | Log in correctly | Lands on feed | ☐ |
| 1.5 | Leave the tab idle >15 min, then act | Session auto-refreshes (access token is 15 min; refresh is transparent) — you are **not** kicked out | ☐ |
| 1.6 | Profile → **Change password** with wrong current password | Rejected | ☐ |
| 1.7 | Change password successfully | Succeeds; note you stay logged in on this tab | ☐ |
| 1.8 | Log in on a **second** device/browser, then change password on the first | The second session can no longer refresh (password change revokes other sessions) | ☐ |

**Talking point:** stateless JWT access tokens (validated at the gateway) + DB-backed **revocable refresh tokens**, rotated on every use. Password change revokes all sessions.

---

## 2. Profile & profile sections

Act as **Ada**.

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 2.1 | Open your own profile → **Edit profile** | Form with name, headline, about, location, company | ☐ |
| 2.2 | Set a headline + about + company, save | Header updates; company · location shown | ☐ |
| 2.3 | Upload an avatar image, save | Avatar appears in header, navbar, and on your posts *(this hits the real Cloudinary bucket — see note at the end)* | ☐ |
| 2.4 | **Experience** → Add a role (leave end date blank = current) | Appears, "Present" shown for the current role | ☐ |
| 2.5 | Edit that experience | Changes persist | ☐ |
| 2.6 | Add a second role with an end date | Current role sorts above the ended one | ☐ |
| 2.7 | **Education** → Add / edit / delete an entry | Works | ☐ |
| 2.8 | **Skills** → Add "Java", "Kubernetes" | Listed | ☐ |
| 2.9 | (as **Grace**) open Ada's profile → **Endorse** Java | Count increments; button shows "Endorsed ✓" | ☐ |
| 2.10 | (as **Ada**) try to endorse your own skill | No Endorse button on your own profile (self-endorsement is blocked) | ☐ |
| 2.11 | Profile → **Who viewed your profile** | Shows Grace (from 2.9); your own visits don't count | ☐ |

**Talking point:** rich profile aggregate; skills + endorsements; profile-view tracking recorded as a side effect of the profile read.

---

## 3. Network & connections

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 3.1 | (**Grace**) Search "Ada" → open profile → **Connect** | Button becomes "Pending — withdraw" | ☐ |
| 3.2 | (**Ada**) Network page → invitation from Grace → **Accept** | Grace now appears under Connections | ☐ |
| 3.3 | (**Ada**) Connection button on Grace's profile | Shows "Connected — remove" | ☐ |
| 3.4 | (**Grace**) Withdraw a *different* pending request (send one to Alan, then withdraw) | Disappears from Alan's invitations | ☐ |
| 3.5 | (**Ada**) **Follow** Katherine (one-directional, no acceptance) | Button shows "Following" | ☐ |
| 3.6 | (**Ada**) Network → **People you may know** | 2nd-degree suggestions ranked by mutual-connection count | ☐ |
| 3.7 | (**Ada**) Remove the connection with Grace, then reconnect | Removal reflects on both sides; suggestions/feed update | ☐ |

**Talking point:** Neo4j graph — connections (mutual, undirected) vs. follows (one-directional); 2-hop suggestion query with mutual counts; connection status drives the UI.

---

## 4. Posts, drafts & the feed

Act as **Ada** (connected to Grace).

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 4.1 | Compose a text post with `#hashtags` | Appears at the top of your feed; tags are clickable | ☐ |
| 4.2 | Compose a post with an image | Image renders *(real bucket write)* | ☐ |
| 4.3 | Compose a post set to **🔒 Connections only** | Shows the lock badge | ☐ |
| 4.4 | (**Grace**, connected) view Ada's profile | Sees the connections-only post | ☐ |
| 4.5 | (**Margaret**, not connected) view Ada's profile | Does **not** see the connections-only post (404 on its permalink) | ☐ |
| 4.6 | **Save draft** in the composer | Post is **not** in the feed; appears under **Drafts** | ☐ |
| 4.7 | Drafts → **Publish** | Now in the feed; connections get notified | ☐ |
| 4.8 | Edit one of your posts | Shows "· edited"; can drop the image | ☐ |
| 4.9 | Delete a post | Gone; its likes/comments/saves go too | ☐ |
| 4.10 | Feed → **Load more** (create >10 posts first) | Pagination works; no duplicates | ☐ |
| 4.11 | Feed tabs → **Following** | Shows public posts from people you follow (not connections) | ☐ |

**Talking point:** post visibility enforced server-side; draft lifecycle; paginated feed (fan-out-on-read); "following" feed kept separate so followees' connections-only posts stay hidden.

---

## 5. Engagement (likes, comments, saves, reposts)

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 5.1 | (**Grace**) Like Ada's post | Count increments; **Ada gets a notification** | ☐ |
| 5.2 | (**Grace**) Comment on it | Comment appears; **Ada gets a notification** | ☐ |
| 5.3 | (**Ada**) Reply to Grace's comment | Reply nests under it (one level) | ☐ |
| 5.4 | (**Ada**) Reply to the reply | Attaches to the same parent (threading stays flat) | ☐ |
| 5.5 | Edit your own comment | Shows "· edited"; you can't edit someone else's | ☐ |
| 5.6 | (**Ada**, post owner) delete Grace's comment | Allowed (owner can moderate) | ☐ |
| 5.7 | Like a comment | Count shows; per-viewer "liked" state | ☐ |
| 5.8 | **Save** a post → **Saved** tab | Appears there; unsave removes it | ☐ |
| 5.9 | (**Grace**) **Repost** Ada's post | Repost shows the original embedded; **Ada notified** | ☐ |
| 5.10 | Repost a repost | Embeds the *underlying original* (chain never nests) | ☐ |

**Talking point:** Kafka-driven notifications via the **outbox pattern** (see §9); one-level comment threading; repost chain flattening.

---

## 6. Discovery (search, hashtags, trending)

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 6.1 | Search a **company** name (e.g. from a headline) | Finds users by name **or** headline **or** company | ☐ |
| 6.2 | Search your own name | You are excluded from your own results | ☐ |
| 6.3 | Click a `#hashtag` in a post → tag page | Public posts using that tag | ☐ |
| 6.4 | **Follow tag** on the tag page | Button shows "Following" | ☐ |
| 6.5 | Feed → **Trending this week** box | Top tags by recent public-post count; click to browse | ☐ |

**Talking point:** multi-field search; hashtag extraction on create/edit; trending aggregation over a time window.

---

## 7. Messaging & blocking

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 7.1 | (**Ada**) On Grace's profile → **Message** → send | Lands in Messages; Grace sees it | ☐ |
| 7.2 | (**Grace**) Messages → open the thread → reply | Thread shows both sides; opening marks read | ☐ |
| 7.3 | Navbar **Messages** unread badge | Increments on new inbound message, clears on read | ☐ |
| 7.4 | (**Ada**) **Block** Margaret | Actions collapse to "Blocked — unblock" | ☐ |
| 7.5 | (**Ada**) Search for Margaret | She no longer appears (both directions) | ☐ |
| 7.6 | (**Margaret**) try to message Ada | Rejected (block in either direction stops messaging) | ☐ |
| 7.7 | Unblock Margaret | She reappears in search | ☐ |

**Talking point:** direct messaging (conversations + unread count, polled); block enforced in messaging + search.

---

## 8. Notifications

| # | Step | Expected result | ✓ |
|---|---|---|---|
| 8.1 | Notifications page | Unread items tinted; navbar bell shows a count | ☐ |
| 8.2 | Click a notification about a post | Opens the post permalink; that one marks read | ☐ |
| 8.3 | **Mark all as read** | Count clears | ☐ |
| 8.4 | Delete one notification / **Clear all** | Works | ☐ |
| 8.5 | **Notify me when…** → uncheck "Someone likes your post" | Preference saved | ☐ |
| 8.6 | (**Grace**) like Ada's post | Ada gets **no** like notification (muted at delivery) | ☐ |
| 8.7 | Re-enable, like again | Notification arrives | ☐ |

**Talking point:** structured notifications (type + target → clickable); read-state; per-type mute filtered **at delivery** (a muted type never accumulates unread rows).

---

## 9. Advanced behaviours (patterns worth demoing in an interview)

These need a terminal — the exact commands are in `README-local-dev.md`, and
you saw them run during development. They are the highest-value things to be
able to *explain and show*.

### 9.1 Saga (post creation with compensation)  ☐
Create a post with the header `X-Saga-Fail-At: PUBLISH`. Expect **HTTP 500
"rolled back"**, and the post does **not** persist. The posts-service log shows:
`persisted post N (committed)` → `saga failed …PUBLISH; compensating 1 step` →
`compensation: deleted post N`. **What it proves:** each step commits
independently; a later failure is undone by explicit compensation, not a DB
rollback.

### 9.2 Outbox (reliable event delivery)  ☐
Like a post → notification arrives. Then **stop Kafka** (`docker stop
linkedin-kafka`) and like another post → the like still **succeeds (200)** and
the event sits pending in the `outbox_events` table; no notification yet.
**Start Kafka** → the relay drains the outbox and the notification arrives.
**What it proves:** no dual-write race — an event survives a broker outage.

### 9.3 Circuit breaker (graceful degradation)  ☐
**Stop ConnectionsService**, then load the feed → still **HTTP 200** (your own
posts only), while a direct connections endpoint returns 500. The posts-service
log shows the breaker `OPEN` and the fallback firing. **What it proves:** a slow
/ down downstream degrades gracefully instead of failing the whole request.

### 9.4 Rate limiting  ☐
Hit `/api/v1/users/auth/login` >10 times in a minute → **HTTP 429** with a
`Retry-After` header. **What it proves:** the only unauthenticated surface is
protected from brute-force.

### 9.5 Authorization  ☐
As Ada, `GET /connections/core/2/first-degree` (Grace's id) → **400** (you can
only read your own). **What it proves:** access control, not just authentication.

---

## 10. Observability (the three signals)

Bring the stack up (`docker compose up -d`), generate some traffic, then:

| # | Check | URL / how | ✓ |
|---|---|---|---|
| 10.1 | **Metrics** | Prometheus <http://localhost:9099> → Status → Targets: all **up** | ☐ |
| 10.2 | **Traces** | Zipkin <http://localhost:9411> → find a `/feed` trace → spans across gateway → posts → connections | ☐ |
| 10.3 | **Logs** | Grafana <http://localhost:3000> → Explore → Loki → `{service="posts-service"}` | ☐ |
| 10.4 | **Correlation** | In a Loki log line, click the **trace id** → opens that trace in Zipkin | ☐ |
| 10.5 | **Dashboards** | Grafana → import a JVM/Micrometer dashboard against the Prometheus datasource | ☐ |

**Talking point:** the full observability triad (metrics, traces, logs) correlated by trace id; distributed tracing propagates across the Feign hop.

---

## 11. Interview talking points (map features → concepts)

| Concept | Where it lives here |
|---|---|
| Microservices, bounded contexts | 7 services split by capability, DB-per-service |
| API Gateway | Spring Cloud Gateway: routing, edge JWT auth, CORS, rate limit |
| Service discovery | Eureka + client-side load balancing (`lb://`) |
| Sync inter-service calls | OpenFeign (feed → connections, posts → uploader) |
| Async / event-driven | Kafka choreography for notifications |
| **Saga pattern** | orchestrated post-creation with compensation (§9.1) |
| **Outbox pattern** | reliable event publishing (§9.2) |
| **Circuit breaker** | Resilience4j on Feign, with fallbacks (§9.3) |
| Polyglot persistence | Postgres (users/posts/notifications) + Neo4j (graph) |
| Security | JWT access + revocable refresh tokens, edge validation, bcrypt |
| Caching/perf | batched queries to kill N+1 in the feed/comments |
| Observability | Micrometer + Prometheus + Zipkin + Loki/Promtail + Grafana |
| Pagination | server-side paging with a stable `PageResponse` wire shape |
| Frontend | React 19 + Vite + TypeScript + TanStack Query, JWT + refresh, optimistic updates |

---

## Notes / caveats
- **Image upload (2.3, 4.2)** writes real objects to the project's Cloudinary
  bucket — those credentials are **burned and must be rotated** (see
  `README-local-dev.md`). Skip these steps if you don't want real bucket writes.
- The dropped-first-event Kafka quirk is fixed for the outbox-backed events;
  see `README-local-dev.md` "Known issues" for the remaining note.
