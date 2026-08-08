# API Calling Another API — Full Walkthrough

> Simple mental model: **One waiter asking another waiter.**
> The **Order API** is a waiter who takes your order. Before writing it down, he
> walks to the **Inventory API** (another waiter behind the counter) and asks:
> "How many iPhones are left?" Only after hearing the answer does he reply to you.

This README explains the scenario where **one REST API calls another REST API**
over HTTP, using two new controllers added to this project:

| API             | Route                    | Role                          |
|-----------------|--------------------------|-------------------------------|
| **Inventory API** | `GET /api/inventory/{itemId}` | The **inner / called** API — knows how many items are in stock |
| **Order API**     | `GET /api/order/{itemId}`    | The **outer / calling** API — calls Inventory, then answers the client |

Both run in the **same** Spring Boot app on `localhost:8080`.

---

## 1. The big picture

```
Client (browser / Postman)
        |
        |  GET /api/order/iphone
        v
+-------------------+          GET /api/inventory/iphone          +-------------------+
|   OrderController | -------------------------------------------> | InventoryController|
|   (outer API)     | <------------------------------------------- | (inner API)       |
+-------------------+          {"available":10,"inStock":true}     +-------------------+
        |
        |  {"status":"PLACED", "message":"Order placed successfully", ...}
        v
Client
```

- The **client only knows the Order API**. It never talks to Inventory directly.
- The Order API acts as a **client to the Inventory API** — it makes its own HTTP
  request using Spring's `RestClient`.
- This is how microservices communicate: one service calls another service's
  endpoint just like an external user would.

---

## 2. The inner API — InventoryController.java

```java
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final Map<String, Integer> stock = Map.of(
            "iphone", 10,
            "laptop", 3,
            "headphones", 0
    );

    @GetMapping("/{itemId}")
    public Map<String, Object> checkStock(@PathVariable String itemId) {
        int available = stock.getOrDefault(itemId.toLowerCase(), 0);
        return Map.of(
                "itemId", itemId,
                "available", available,
                "inStock", available > 0
        );
    }
}
```

Line by line:

- `@RestController` + `@RequestMapping("/api/inventory")` → this class handles
  HTTP requests whose path starts with `/api/inventory`.
- `stock` map → a fake in-memory "database". In a real app this would be rows in
  a Postgres/MySQL table. Only three items exist.
- `@GetMapping("/{itemId}")` → handles `GET /api/inventory/<anything>`. The
  `{itemId}` is a **path variable**.
- `@PathVariable String itemId` → Spring extracts whatever was in the `{itemId}`
  slot of the URL and passes it in as `itemId`. Requesting
  `/api/inventory/iphone` gives `itemId = "iphone"`.
- `stock.getOrDefault(itemId.toLowerCase(), 0)` → looks up the item; if missing,
  returns `0`. `.toLowerCase()` makes it case-insensitive (`IPhone` works too).
- The `Map.of(...)` return → Spring converts this Java map into a **JSON body**:
  ```json
  { "itemId": "iphone", "available": 10, "inStock": true }
  ```

Try it yourself: `http://localhost:8080/api/inventory/headphones` →
`{"itemId":"headphones","available":0,"inStock":false}`.

---

## 3. The outer API — OrderController.java

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final RestClient restClient;

    public OrderController(RestClient.Builder builder) {
        this.restClient = builder.baseUrl("http://localhost:8080").build();
    }

    @GetMapping("/{itemId}")
    public Map<String, Object> placeOrder(@PathVariable String itemId) {
        Map<String, Object> inventory = restClient.get()
                .uri("/api/inventory/{itemId}", itemId)
                .retrieve()
                .body(Map.class);

        int available = ((Number) inventory.get("available")).intValue();

        if (available == 0) {
            return Map.of(
                    "itemId", itemId,
                    "status", "OUT_OF_STOCK",
                    "message", "Cannot place order, item is unavailable"
            );
        }

        return Map.of(
                "itemId", itemId,
                "status", "PLACED",
                "message", "Order placed successfully",
                "unitsAvailable", available
        );
    }
}
```

Line by line:

- `private final RestClient restClient;` → the HTTP client this API uses to talk
  to other APIs. (`RestClient` is Spring's modern replacement for the older
  `RestTemplate`.)
- `public OrderController(RestClient.Builder builder)` → constructor injection.
  Spring Boot auto-creates a `RestClient.Builder` bean; we just ask for it.
- `builder.baseUrl("http://localhost:8080")` → the root address of the service
  we want to call. Right now both APIs share port `8080`. If the Inventory API
  lived in a separate app on port `8081`, only this line would change.
- `restClient.get()` → "I want to make a GET request."
- `.uri("/api/inventory/{itemId}", itemId)` → the path of the inner API, with
  `{itemId}` replaced by the value from the URL. This is the **call to the other
  API**.
- `.retrieve()` → actually perform the request and get back the response.
- `.body(Map.class)` → deserialize the JSON response into a `Map<String, Object>`.
  The inner API's `{"available":10,...}` becomes a Java map.
- `((Number) inventory.get("available")).intValue()` → `available` arrives as a
  number. Casting through `Number` and calling `intValue()` is the safe way to get
  an `int` regardless of whether Jackson produced an `Integer` or `Long`.
- The `if (available == 0)` / `else` → the **business logic**: the Order API
  decides the answer based on what the Inventory API told it. The inner API just
  reports facts; the outer API makes the decision.

Two possible responses:

```
GET /api/order/iphone
→ {"itemId":"iphone","status":"PLACED","message":"Order placed successfully","unitsAvailable":10}

GET /api/order/headphones
→ {"itemId":"headphones","status":"OUT_OF_STOCK","message":"Cannot place order, item is unavailable"}
```

---

## 4. Why this pattern matters

1. **Separation of concerns** — Inventory only knows about stock; Order only knows
   about ordering. Each can be changed or scaled independently.
2. **Microservices** — this is exactly how services talk in a distributed system:
   Service A calls Service B's public endpoint over HTTP.
3. **Reuse** — any other API (cart, checkout, reporting) can also call the same
   Inventory endpoint instead of duplicating stock logic.
4. **Single entry point** — the client deals with one API (Order) and gets a
   combined, user-friendly answer; it never needs to know the inner API exists.

---

## 5. Where the security fits in

`SecurityConfig.java` now whitelists both new routes:

```java
.requestMatchers("/", "/api/public", "/api/auth/login",
                 "/api/inventory/**", "/api/order/**").permitAll()
```

Why? The app uses JWT auth where every request needs a token. When the Order API
makes its internal HTTP call to the Inventory API, that call has **no token** (it's
server-to-server, not browser-to-server). Whitelisting the routes lets the demo
work without handling token forwarding. In production you'd instead forward the
JWT (e.g. via a `RestClient` interceptor) or use service-to-service auth — but that
is a separate topic.

---

## 6. Files involved

```
src/main/java/com/example/ecom1/controller/
├── InventoryController.java   (NEW — inner API, knows stock levels)
├── OrderController.java       (NEW — outer API, calls Inventory via RestClient)
└── HomeController.java        (existing, unaffected)

src/main/java/com/example/ecom1/config/SecurityConfig.java  (modified — permitAll for the two new routes)
```

---

## 7. How to run & test

```bash
cd e-com1
docker compose up -d      # only if you also want Kafka — not needed for this demo
.\mvnw.cmd spring-boot:run
```

Then in a browser or Postman:

| Request                      | Response                                        |
|------------------------------|-------------------------------------------------|
| `GET /api/inventory/iphone`  | `{"itemId":"iphone","available":10,"inStock":true}` |
| `GET /api/inventory/headphones` | `{"itemId":"headphones","available":0,"inStock":false}` |
| `GET /api/order/iphone`      | `{"itemId":"iphone","status":"PLACED", ...}`    |
| `GET /api/order/laptop`      | `{"itemId":"laptop","status":"PLACED", ...}`    |
| `GET /api/order/headphones`  | `{"itemId":"headphones","status":"OUT_OF_STOCK", ...}` |

Watch the console too: `RestClient` fires a real `GET /api/inventory/...` request
inside `placeOrder`, exactly like a browser would — just from inside the server.

---

## 8. Key terms (cheat sheet)

| Term               | Meaning                                                      |
|--------------------|--------------------------------------------------------------|
| **REST API**       | An HTTP endpoint a client (or another API) can call          |
| **Path variable**  | The `{itemId}` part of a URL, captured by `@PathVariable`    |
| **`@RestController`** | Marks a class whose methods return JSON (auto-serialized) |
| **`RestClient`**   | Spring's HTTP client used to call other APIs                 |
| **`@GetMapping`**  | Says "this method answers a GET request"                    |
| **JSON body**      | The response format; Spring converts Java `Map` → JSON       |
| **Outer / inner API** | The caller vs. the one being called (service consumer vs. provider) |
