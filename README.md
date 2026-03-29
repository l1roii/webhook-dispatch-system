# Webhook Dispatch System

A Quarkus microservice that lets clients manage webhook configurations and dispatches events asynchronously via Kafka.

---

## Prerequisites

- Java 21+
- Docker + Docker Compose
- [Task](https://taskfile.dev/installation/)
  - Linux: `sudo pacman -S go-task` / `sudo apt install go-task`
  - macOS: `brew install go-task`
  - Windows: `scoop install task` or `choco install go-task`
- `jq`
  - Linux: `sudo pacman -S jq` / `sudo apt install jq`
  - macOS: `brew install jq`
  - Windows: `scoop install jq` or `choco install jq`
- `openssl`
  - Linux/macOS: pre-installed
  - Windows: included with [Git for Windows](https://git-scm.com/download/win) — use Git Bash for all commands below

---

## Starting the stack

### 1. Generate RSA keys and create your `.env`

The application never stores keys in source control. Generate a key pair once and keep it in a local `.env` file (already git-ignored):

```bash
# Generate RSA key pair
openssl genrsa -out private.pem 2048
openssl pkcs8 -topk8 -inform PEM -in private.pem -out private_pkcs8.pem -nocrypt
openssl rsa -in private.pem -pubout -out public.pem

# Write .env
cat > .env <<EOF
DB_USERNAME=webhookuser
DB_PASSWORD=webhookpass
DB_URL=jdbc:postgresql://localhost:5432/webhookdb
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
JWT_PRIVATE_KEY=$(cat private_pkcs8.pem | tr -d '\n')
JWT_PUBLIC_KEY=$(cat public.pem | tr -d '\n')
INTERNAL_TOKEN=$(openssl rand -hex 32)
EOF

# Clean up raw key files
rm private.pem private_pkcs8.pem public.pem
```

**Windows users:** Run all commands in **Git Bash** (not PowerShell or CMD) — it ships with `openssl`, `cat`, and `tr` which are required for the key generation step above.

### 2. Start PostgreSQL and Kafka

```bash
task infra:up
```

### 3. Start the application

Flyway migrations run automatically on startup.

```bash
task dev
```

The app will be available at `http://localhost:8080`.

---

## Authentication

The webhook management API uses **JWT Bearer tokens**.

### Default credentials (seeded by migration)

| Username   | Password   |
|------------|------------|
| `client_a` | `secret-a` |
| `client_b` | `secret-b` |

### Obtain a token

```bash
task login USER=client_a PASS=secret-a
```

Or export it directly into your shell for use with subsequent commands:

```bash
source <(task token USER=client_a PASS=secret-a)
```

Tokens expire after **1 hour**.

---

## Internal endpoint

`POST /internal/events` simulates the core platform publishing an event. It is protected by a static shared secret passed via the `X-Internal-Token` header. The value is set via the `INTERNAL_TOKEN` environment variable (see `.env`).

---

## Postman collection

For a GUI alternative to the task commands, import both files from the `postman/` directory into Postman:

| File | Purpose |
|------|---------|
| `webhook-dispatch.postman_collection.json` | All requests with pre-written tests |
| `webhook-dispatch.postman_environment.json` | Environment variables (`base_url`, `internal_token`) |

**Quick start:**

1. In Postman: **Import** → select both files
2. Select the **Webhook Dispatch - Local** environment from the top-right dropdown
3. Run **Auth → Login (client_a)** — the JWT is saved automatically to `{{token}}`
4. All subsequent requests use `{{token}}` with no manual copy-pasting

The collection includes automated tests on every request (status codes, masked secrets, isolation checks) so you can run the entire collection via **Run collection** to verify the full flow in one click.

---

## Task reference

Run `task --list` to see all available tasks.

### Infrastructure

| Task | Description |
|------|-------------|
| `task infra:up` | Start PostgreSQL and Kafka |
| `task infra:down` | Stop and remove containers |
| `task infra:logs` | Tail container logs |

### Application

| Task | Description |
|------|-------------|
| `task dev` | Start the app in dev mode |
| `task build` | Compile the project |

### Auth

| Task | Description |
|------|-------------|
| `task login USER=client_a PASS=secret-a` | Login and pretty-print the JWT response |
| `source <(task token USER=client_a PASS=secret-a)` | Export `$TOKEN` into your shell |

### Webhook CRUD

All webhook tasks require `$TOKEN` to be set — run `source <(task token)` first.

| Task | Description |
|------|-------------|
| `task webhook:create URL=https://webhook.site/xxx EVENT=USER_CREATED SECRET=mysecret` | Create a webhook |
| `task webhook:list` | List your webhooks |
| `task webhook:get ID=<uuid>` | Get a single webhook |
| `task webhook:update ID=<uuid> ACTIVE=false` | Update a webhook |
| `task webhook:delete ID=<uuid>` | Delete a webhook |

### Events

| Task | Description |
|------|-------------|
| `task event:publish EVENT=USER_CREATED` | Publish an event to Kafka |

### Demos

| Task | Description |
|------|-------------|
| `task demo:isolation` | Proves client_b cannot access client_a's webhooks (expects 404) |
| `task demo:e2e` | Full flow — login, create webhook, publish event |

---

## Observe dispatch

After publishing an event, watch the application logs for structured output:

```
INFO  Webhook dispatched successfully, status=200  webhookId=<uuid> eventType=USER_CREATED targetUrl=https://...
```

Or use [webhook.site](https://webhook.site) as the `targetUrl` to inspect the incoming POST body and `X-Signature` header in a browser.

---

## Architectural decisions

### Kafka message shape

Messages published to the `platform-events` topic are UTF-8 JSON with the same structure as the HTTP body:

```json
{ "eventType": "USER_CREATED", "payload": { ... } }
```

The `eventType` is used as the Kafka message key, which co-locates all events of the same type on the same partition and preserves ordering per event type.

### HMAC signing scheme

For each outbound webhook POST:

1. Serialize the event to UTF-8 JSON bytes.
2. Compute `HMAC-SHA256(body_bytes, webhook.secret)`.
3. Encode the digest as **lowercase hex** (64 characters).
4. Send as the `X-Signature` request header.

The receiver re-computes the same HMAC over the raw body bytes and compares. Because the signature covers the exact bytes sent, any in-transit modification is detectable.

### Consumer behaviour under HTTP slowness or failure

#### Timeouts

Each outbound webhook call is bounded by two independent timeouts:

- **Connect timeout — 5 s:** if the TCP handshake does not complete within 5 seconds the call is abandoned.
- **Read timeout — 10 s:** if the remote server accepts the connection but does not respond within 10 seconds the call is abandoned.

#### Sequential vs parallel dispatch

Webhooks matching a given event are dispatched **sequentially** on the consumer thread. This keeps the implementation simple and naturally isolates failures — one broken endpoint never blocks delivery to others. The trade-off is that total dispatch time is the sum of all per-webhook call times. A production system with high fan-out would use a dedicated thread pool or reactive pipeline.

#### Failed POST handling

A failed HTTP call (non-2xx response, timeout, or network error) is logged and skipped. The loop moves on to the next webhook. A single broken endpoint never blocks delivery to others registered for the same event.

#### Offset commit and delivery guarantees

SmallRye Kafka commits the Kafka offset after the `@Incoming` consumer method returns successfully — **at-least-once delivery**. If the process crashes mid-dispatch, the message is re-consumed on restart. Receivers should be idempotent where possible.

#### Retries and DLQ

This implementation does not retry failed webhook calls. In production the right pattern is:

1. **Retry with exponential backoff** — attempt delivery 3–5 times with increasing delays before giving up.
2. **Dead-letter queue (DLQ)** — after exhausting retries, publish the failed dispatch to a separate Kafka topic. An operator can inspect, fix the root cause, and replay from the DLQ.
3. **Circuit breaker** — if a specific `targetUrl` has failed repeatedly, stop attempting it for a cool-down window to avoid wasting resources on a known-broken endpoint.

---

## Infrastructure (docker-compose.yml)

| Service       | Port | Credentials                                                   |
|---------------|------|---------------------------------------------------------------|
| PostgreSQL 16 | 5432 | user: `webhookuser` / pass: `webhookpass` / db: `webhookdb`  |
| Kafka (KRaft) | 9092 | —                                                             |
