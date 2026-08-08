# Kafka Sender + Listener — Full Walkthrough

> Simple mental model: **Kafka is a post office.**
> The **producer** writes a letter (message) and drops it in the postbox (topic).
> The **post office** (broker) stores the letter. The **consumer** opens the
> postbox, takes the letter out, and reads it. Sender and receiver never talk
> directly — they only share the post office.

This README explains the whole Kafka setup across the **two projects** in this
folder:

| Project        | Role                         | What it does                                  |
|----------------|------------------------------|-----------------------------------------------|
| `e-com1`      | **Producer / Sender**        | Builds a `HelloModel` and pushes it to Kafka  |
| `Hello-listener` | **Consumer / Listener**    | Subscribes to the topic and prints each message |

Both apps talk to the **same** Kafka broker (`localhost:9092`, started by
`e-com1/docker-compose.yml`).

---

## 1. The big picture

```
                       +--------------------------+
                       |   Kafka broker (docker)  |
                       |  localhost:9092          |
                       |  topic: greetings.created|
                       +--------------------------+
                                  ^
        sends message             |               consumes message
        (producer)                |               (consumer)
                                  |
   +------------------+           |           +------------------------+
   |     e-com1       |-----------+---------->|     Hello-listener     |
   |  KafkaPublisher  |                       |  GreetingKafkaListener |
   +------------------+                       +------------------------+
```

Flow in one sentence: `e-com1` writes `"Hello1"` to topic `greetings.created`
on the broker; `Hello-listener` has a `@KafkaListener` on that same topic, so the
broker delivers the message to it and it prints:

```
Received greeting from Kafka: Hello1
```

---

## 2. Producer side — `e-com1` (the sender)

Three files do the work (plus a model):

```
src/main/java/com/example/ecom1/
├── config/KafkaConfig.java       → declares the topic name + creates it
├── kafka/KafkaPublisher.java     → the actual sender (uses KafkaTemplate)
├── service/HelloKafkaService.java→ the "start button" (calls the sender)
└── model/HelloModel.java         → the message payload (just holds a String)

src/main/resources/application.properties → producer settings
```

### 2a. KafkaConfig.java — "where do I send?"

```java
public static final String GREETING_TOPIC = "greetings.created";

@Bean
public NewTopic greetings() {
    return TopicBuilder.name(GREETING_TOPIC).partitions(1).replicas(1).build();
}
```

- Holds the **topic name constant** (`greetings.created`) so the rest of the code
  never typos it.
- The `NewTopic` bean asks the broker to create that topic (1 partition, 1 replica)
  when the app starts. The broker also has `KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`,
  so the topic would be auto-created on first send anyway.

### 2b. KafkaPublisher.java — "the sender"

```java
@Component
public class KafkaPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(HelloModel helloModel) {
        kafkaTemplate.send(KafkaConfig.GREETING_TOPIC, "id1", helloModel.getName());
    }
}
```

- `@Component` → Spring creates it once and injects a `KafkaTemplate` (auto-built
  from the `spring.kafka.*` properties).
- `KafkaTemplate<String, String>` → **key** is a `String`, **value** is a `String`.
- `send(topic, key, value)` → pushes the message:
  - topic: `greetings.created`
  - key:   `"id1"`
  - value: `helloModel.getName()` → `"Hello1"`
- `send` is **async** — it returns a `Future`. It just hands the message to Kafka
  and keeps going; you don't have to wait.

### 2c. HelloKafkaService.java — "the start button"

```java
@Service
public class HelloKafkaService {
    private final KafkaPublisher kafkaPublisher;

    public HelloKafkaService(KafkaPublisher kafkaPublisher) {
        this.kafkaPublisher = kafkaPublisher;
    }

    public void send() {
        this.kafkaPublisher.sendMessage(new HelloModel("Hello1"));
    }
}
```

Just wraps the publisher: `new HelloModel("Hello1")` → `send()`. Something must
call `helloKafkaService.send()` for a message to actually go out.

### 2d. application.properties — producer settings

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.producer.properties.spring.json.add.type.headers=false
```

- `bootstrap-servers` → which broker to connect to (must equal the docker mapping).
- `key-serializer` → the key `"id1"` is written as a plain `String`.
- `value-serializer` → the value `"Hello1"` is written as **JSON** (`"Hello1"`).
- `add.type.headers=false` → don't embed Java type info in the message. (The
  listener must therefore be told the type explicitly — see section 4.)

---

## 3. Consumer side — `Hello-listener` (the listener)

```
src/main/java/com/example/Hello_listener/
└── listener/GreetingKafkaListener.java  → subscribes + prints
    HelloListenerApplication.java        → normal Spring Boot main class

src/main/resources/application.properties → consumer settings
```

### 3a. GreetingKafkaListener.java — "the listener"

```java
@Component
public class GreetingKafkaListener {

    @KafkaListener(topics = "greetings.created", groupId = "hello-listener-group")
    public void onGreeting(String message) {
        System.out.println("Received greeting from Kafka: " + message);
    }
}
```

- `@KafkaListener(topics = "greetings.created", ...)` → tells Spring: run this
  method every time a message lands on that topic. The topic name is the **same
  literal** as `KafkaConfig.GREETING_TOPIC` in e-com1 — the two apps share only
  the broker, not code, so it's written out here.
- `groupId = "hello-listener-group"` → the consumer group. Kafka tracks this
  group's read-offset so it doesn't re-deliver old messages on restart.
- The method parameter `String message` → Spring deserializes the message value
  (`"Hello1"`) and passes it in. If the producer had sent a `HelloModel` object
  instead, the parameter would be `HelloModel`.

### 3b. application.properties — consumer settings

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=hello-listener-group
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.value.default.type=java.lang.String
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

- `bootstrap-servers` → **must** point at the same broker as the producer.
- `group-id` → **must** match the `groupId` in `@KafkaListener`.
- `key-deserializer` → turn the key bytes back into `String`.
- `value-deserializer` → turn the JSON value bytes back into an object.
- `spring.json.value.default.type=java.lang.String` → because the producer disabled
  type headers, the payload has no type info; this says "assume it's a String."
- `trusted.packages=*` → trust all packages (fine locally; restrict in production).

---

## 4. The golden rule: producer and consumer must match

Kafka stores messages as raw **bytes**. The producer decides how to turn an
object into bytes (serializer); the consumer decides how to turn them back
(deserializer). They only work if both sides agree:

| Side      | Key                      | Value                     |
|-----------|--------------------------|---------------------------|
| Producer  | `StringSerializer`       | `JsonSerializer`          |
| Consumer  | `StringDeserializer`     | `JsonDeserializer` → `String` |

If they mismatch — e.g. consumer reads with `StringDeserializer` while the
producer wrote JSON — you'd see garbage like `"Hello1"` (with quotes) instead of
`Hello1`. That's why both values use the **Json** serializers and the consumer
declares the default type as `java.lang.String`.

The same rule applies to the **topic**: if producer sends to `greetings.created`
and the consumer listens on `greetings.other`, no message ever arrives.

---

## 5. End-to-end flow

1. Kafka broker starts via `docker compose up -d` in `e-com1` (listening on `9092`).
2. `e-com1` starts. The `NewTopic` bean creates topic `greetings.created`.
3. Something calls `HelloKafkaService.send()` (or injects `KafkaPublisher`
   directly and calls `sendMessage`).
4. `KafkaPublisher` → `kafkaTemplate.send("greetings.created", "id1", "Hello1")`.
5. The producer serializers turn it into bytes: key = `id1`, value = `"Hello1"` (JSON).
6. The broker stores the message under the topic and delivers it to subscribers.
7. `Hello-listener` starts. Its `@KafkaListener` subscribes to `greetings.created`.
8. The broker delivers the message; `JsonDeserializer` turns the value back into
   `String` `Hello1`.
9. `onGreeting("Hello1")` runs and prints:
   ```
   Received greeting from Kafka: Hello1
   ```

---

## 6. How to run

Prerequisites: Docker (for Kafka), JDK 17, Maven wrapper in each project.

```bash
# 1) Start Kafka (from e-com1)
cd e-com1
docker compose up -d

# 2) Start the listener (terminal A)
cd Hello-listener
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run

# 3) Start the sender (terminal B)
cd e-com1
./mvnw spring-boot:run

# 4) Trigger a send — however the app calls helloKafkaService.send(),
#    e.g. a runner or controller. The listener terminal then prints:
#    Received greeting from Kafka: Hello1
```

Order matters: start Kafka first, then the listener (so it subscribes early),
then the producer. If the producer runs before the listener exists, the message
is still stored by the broker — the listener receives it once it subscribes
(because the group's offset starts from the latest commit point).

---

## 7. Key terms (cheat sheet)

| Term             | Meaning                                                        |
|------------------|----------------------------------------------------------------|
| **Broker**       | The Kafka server that stores & delivers messages (`localhost:9092`) |
| **Topic**        | A named "postbox" (`greetings.created`) messages are written to |
| **Partition**    | A topic's internal ordering unit (here 1 partition)            |
| **Producer**     | Sender — writes messages (`KafkaPublisher`)                    |
| **Consumer**     | Receiver — reads messages (`GreetingKafkaListener`)            |
| **Consumer group** | A set of consumers that share a topic's messages (offset per group) |
| **Key**          | Optional label used for partitioning/ordering (`"id1"`)        |
| **Value**        | The actual payload (`"Hello1"`)                                |
| **Serializer**   | Producer-side: object → bytes                                  |
| **Deserializer** | Consumer-side: bytes → object                                  |
| **Offset**       | A consumer group's read-position in a partition                |

---

## 8. Troubleshooting

- **`Connection refused` / can't connect** → Kafka isn't running. Run
  `docker compose up -d` and check `docker ps`.
- **Listener starts, nothing arrives** → topics differ, or the producer and
  consumer use different `bootstrap-servers`. Both must be `localhost:9092`
  and topic must be `greetings.created` on both sides.
- **Output has extra quotes: `"Hello1"`** → deserializer mismatch. Producer writes
  JSON (`JsonSerializer`) but consumer reads plain text (`StringDeserializer`).
- **`Class not found` / deserialization error** → missing
  `spring.json.value.default.type` or a too-restrictive `trusted.packages`.
- **Old messages replay on restart** → the group id changed (Kafka starts a fresh
  group at the latest offset). Keep `hello-listener-group` stable.
