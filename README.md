# LinkedIn Clone — Spring Cloud Microservices

A LinkedIn-style professional network built as a **Spring Cloud microservices** backend with a **React + TypeScript** single-page frontend. A learning project that exercises distributed-systems patterns end to end: service discovery, an API gateway, event-driven notifications over Kafka, a graph-backed connection network on Neo4j, the **saga** and **transactional-outbox** patterns, circuit breakers, and full observability.

> **Frontend:** the React SPA lives in a separate repo → **[linkedin-clone-frontend](https://github.com/farazsk72/linkedin-clone-frontend)**

## Architecture

| Service | Port | Responsibility | Store |
|---|---|---|---|
| DiscoverServer (Eureka) | 8761 | service discovery | — |
| APIGateway | 9090 | routing, JWT auth filter, rate limiting, CORS | — |
| userService | 9020 | auth, profiles, experience/education/skills, messaging, blocking | Postgres |
| postsService | 9010 | posts, comments, reactions, hashtags, saved posts, drafts, reposts, @mentions | Postgres |
| ConnectionsService | 9030 | connection graph, follow, suggestions | Neo4j |
| notification-service | 9040 | notifications + real-time WebSocket push | Postgres |
| uploader-service | 9050 | image/avatar upload (Cloudinary / GCS / local disk) | — |

Cross-cutting: OpenFeign + Resilience4j circuit breakers, Kafka event fan-out, a Transactional Outbox, an orchestrated Saga (post creation with compensations), and observability via Actuator + Prometheus + Zipkin + Loki/Grafana.

## Features

- **Auth** — signup/login with short-lived access + revocable refresh tokens, password change, gateway rate limiting
- **Profiles** — headline/about/avatar, experience, education, skills + endorsements, profile viewers, block/unblock, search
- **Posts** — text/image posts, PUBLIC / Connections-only visibility, drafts, reposts, feed with connection fan-out, hashtags + trending
- **Engagement** — reactions (like / celebrate / support / insightful / funny), threaded comments + comment likes, @mentions, saved posts
- **Network** — connection requests, accept / withdraw / remove, follow, "people you may know" suggestions (Neo4j)
- **Messaging** — 1:1 direct messages with unread counts
- **Notifications** — per-type mute preferences, plus **real-time WebSocket / STOMP** delivery with toasts
- **Media** — pluggable upload backend: Cloudinary, Google Cloud Storage, or local filesystem

## Tech

Java 21 · Spring Boot 3 · Spring Cloud (Gateway, Eureka, OpenFeign, LoadBalancer) · Spring Security + JWT · Spring Data JPA (Postgres) · Spring Data Neo4j · Apache Kafka · Resilience4j · Micrometer + Prometheus + Zipkin · Docker Compose · Kubernetes manifests · JUnit 5 + Mockito + Testcontainers

## Running locally

See **[README-local-dev.md](./README-local-dev.md)** for the full setup — prerequisites (JDK 21), Postgres / Neo4j / Kafka via Docker Compose, service start order, the observability stack, and an API smoke-test script.

Quick version:

```bash
export JAVA_HOME="<path-to-jdk-21>"
docker compose up -d          # Postgres, Neo4j, Kafka, observability
# start Eureka first, gateway last — see the local-dev guide for the table
```

Then run the [frontend](https://github.com/farazsk72/linkedin-clone-frontend) and open http://localhost:5173.

## Tests

`postsService` carries a JUnit 5 + Mockito + Testcontainers suite — saga compensation, outbox relay, hashtag/mention extraction, and a real-Postgres repository slice. See [README-local-dev.md](./README-local-dev.md) §4.
