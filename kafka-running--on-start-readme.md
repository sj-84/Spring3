# Kafka Must Be Running at App Start — The "Rebootstrapping" Issue

> Simple mental model: **Kafka is a post office.**
> At startup your app sends the post office a note: *"please create the
> `greetings.created` mailbox."*
> If the post office is closed (broker not running), the app just keeps knocking
> on the door and waiting — over and over.

---

## 1. The logs we saw

```text
INFO  [e-com1-admin-0] o.a.k.c.a.i.AdminMetadataManager:
      [AdminClient clientId=e-com1-admin-0] Rebootstrapping with
      Cluster(id = null, nodes = [localhost:9092 (id: -1 rack: null isFenced: false)],
      partitions = [], controller = null)
...
ERROR o.springframework.kafka.core.KafkaAdmin:
      Could not configure topics
org.apache.kafka.common.errors.TimeoutException:
      Timed out waiting for a node assignment. Call: fetchMetadata
```

---

## 2. What the error means (in plain words)

- The app uses `spring-boot-starter-kafka`, and `config/KafkaConfig.java`
  declares a `NewTopic` bean:

  ```java
  @Bean
  public NewTopic greetings() {
      return TopicBuilder.name("greetings.created").partitions(1).replicas(1).build();
  }
  ```

- Spring Boot auto-configures a **`KafkaAdmin`**. Its startup job:
  1. Connect to the broker at `spring.kafka.bootstrap-servers=localhost:9092`.
  2. Create every topic declared as a `NewTopic` bean (`greetings.created`).

- The problem: **no broker was running** (Docker was off), so the AdminClient
  could not connect to `localhost:9092`. It kept retrying and timing out.

---

## 3. Why "Rebootstrapping" repeats endlessly

```
AdminClient asks bootstrap server:  "What does your cluster look like?"
        │
        ▼
No one answers  →  AdminClient has NO node list (nodes = [], controller = null)
        │
        ▼
Retry round: "Rebootstrapping" = re-contact the bootstrap address & ask again
        │
        ▼
Still no answer  →  repeat... until "Timed out waiting for a node assignment"
```

- "Rebootstrapping" is the Kafka client trying to obtain cluster metadata and
  re-establishing contact with the bootstrap server each attempt.
- `controller = null` / `nodes = []` means: *"I have not learned about any broker
  yet."*
- After ~45 seconds of retries the client gives up that round →
  `Timed out waiting for a node assignment`.

---

## 4. Why it did NOT fail the build (important!)

This issue is **non-fatal**:

- `KafkaAdmin` logs "Could not configure topics" as an **error** and moves on.
- Spring Boot tolerates topic-creation failure at startup — the app still runs,
  and the topic gets created later the moment the broker becomes reachable.
- Net effect: the build/test **passes**, but with ~45 seconds wasted and a wall
  of scary-looking error logs.

Compare with the `RestClient.Builder` issue: that one was a **hard startup
failure** (app refuses to start). This one is a **soft failure** (app starts,
broker just isn't there yet).

---

## 5. The fix — start Kafka first

In the project root (`e-com1/`):

```bash
docker compose up -d
```

Check it worked:

```bash
docker compose ps
```

Expected:

```text
NAME              IMAGE              SERVICE   STATUS        PORTS
e-com1-kafka-1    apache/kafka:latest kafka     Up           0.0.0.0:9092->9092/tcp
```

Then start the app (or just let the running app retry — `KafkaAdmin` will
connect on its next attempt):

```bash
.\mvnw.cmd spring-boot:run
```

On a healthy start you should see the broker connect and the topic get created,
with clean logs and no timeouts.

---

## 6. Why Docker needed a nudge first

`docker compose up` failed with:

```text
error during connect ... docker daemon is not running
```

The **Docker daemon** (Docker Desktop) wasn't running either. Starting Docker
Desktop first, waiting for the daemon to be ready, then `docker compose up -d`
fixed it.

Order matters:

```
1. Start Docker Desktop          → daemon ready (docker info works)
2. docker compose up -d          → Kafka container starts
3. Start the Spring app          → KafkaAdmin connects, creates the topic
```

---

## 7. The golden rule

**Start Kafka before the Spring app.** If the producer runs before the broker
exists, it cannot write — and at startup `KafkaAdmin` will knock on the post
office door for ~45 seconds before giving up for that round. Once the broker is
up, everything wires itself on the next retry.

---

## 8. Key takeaways

1. `KafkaAdmin` (auto-configured) tries to create `NewTopic` beans at startup —
   it needs a **reachable broker**.
2. No broker → endless "Rebootstrapping" logs and `TimeoutException`s for ~45s.
3. These are **non-fatal**: the app still starts; the topic just isn't created yet.
4. Fix = `docker compose up -d` (after making sure Docker Desktop is running),
   then start/restart the app.
5. For local dev: Kafka up → clean logs, topic auto-created, messages flow.
