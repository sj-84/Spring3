# How `RestClient` Gets Injected — Q&A Deep Dive (Part 2)

This README captures the full Q&A discussion about the `RestClient` dependency
in `OrderController`. It answers one question at a time, building up from "how
does the injection work?" to "what actually happens inside Spring Boot?"

The code in question:

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
        // ... uses restClient.get().uri("/api/inventory/{itemId}", itemId) ...
    }
}
```

---

## Q1. `private final RestClient restClient;` — the dependency is injected how?

**Via constructor injection — not on the field.**

A `final` field can't be injected directly (see Q2). Instead:

1. Spring sees the constructor `public OrderController(RestClient.Builder builder)`.
2. Spring Boot auto-configures a `RestClient.Builder` bean (see Q4) and Spring
   passes it into the constructor parameter.
3. The constructor body runs `builder.baseUrl("http://localhost:8080").build()`
   to **build** the actual `RestClient`, then assigns it to the `final` field.

So the **injected** dependency is `RestClient.Builder`; the `RestClient` itself
is a plain object **derived** from it inside the constructor. No `@Autowired`
appears anywhere, because Spring treats a single constructor as the injection
point automatically.

---

## Q2. "final fields can't be field-injected" — what does that mean?

**Field injection** sets the field *after* the object is constructed, via
reflection:

```java
@Autowired
private RestClient restClient;   // Spring builds object, THEN pokes value in
```

But Java's rule for a `final` field: **it must be assigned exactly once, and that
assignment must happen during construction** (at declaration, in an initializer,
or in the constructor). After the constructor finishes, it's locked.

Because field injection happens *after* the constructor, it is a **reassignment**
→ illegal for `final`.

**Constructor injection** works because the assignment happens *inside* the
constructor, which is exactly when Java allows `final` to be set:

```java
public OrderController(RestClient.Builder builder) {  // during construction
    this.restClient = builder.build();                 // first and only assignment — legal
}
```

So `final` + constructor injection = the field is set once, never changeable
afterward (immutability). That's why it's the recommended style.

---

## Q3. So `@Autowired` will not work?

**Correct.** `@Autowired` field injection won't work on a `final` field. Spring
simply doesn't inject into it — newer Spring throws an error; older versions
leave it `null`, giving a `NullPointerException` on first use.

The rule of thumb:

| Field declaration              | Injection style                        |
|--------------------------------|----------------------------------------|
| `private final RestClient c;`  | must use **constructor injection**     |
| `@Autowired private RestClient c;` | field must **not** be `final`      |

The same rule applies to `@Value` on fields. This is a big reason constructor
injection is preferred — it's the only style that supports immutable (`final`)
dependencies.

---

## Q4. How is it injected in the constructor? Where's the `@Component` / `@Bean`?

The `RestClient.Builder` bean is **not created by your code** — it comes from
**Spring Boot's auto-configuration**.

Where it lives (behind the scenes):

- `spring-boot-starter-web` pulls in `spring-boot-autoconfigure`.
- That jar contains `RestClientAutoConfiguration` — itself a `@Configuration`
  class with a `@Bean` method that creates the `RestClient.Builder`.
- Because it's on the classpath, Spring Boot loads it automatically at startup.
  You never write it.

The container therefore already holds a `RestClient.Builder` bean before your
controller is created. Constructor injection is just Spring's normal lookup:

```java
public OrderController(RestClient.Builder builder) {   // (1) Spring reads this constructor
    this.restClient = builder.baseUrl(...).build();    // (3) you build your own RestClient
}
```

1. At startup Spring scans `OrderController`, sees **one** constructor (since
   Spring 4.3, a single constructor is auto-wired — no `@Autowired` needed).
2. It looks in the container for a `RestClient.Builder` bean → finds the
   auto-configured one.
3. Passes it into the constructor, where `.build()` creates your own `RestClient`.

Important distinction: **your `RestClient` is NOT a bean** — it's a plain object
built inside the constructor from an injected auto-configured builder.

---

## Q5. What does the internal auto-configuration code look like?

Spring Boot's real class (`RestClientAutoConfiguration`) does roughly this:

```java
@AutoConfiguration
@ConditionalOnClass(RestClient.class)     // load only if RestClient is on classpath
public class RestClientAutoConfiguration {

    @Bean
    @Scope("prototype")                    // a fresh builder on each request
    @ConditionalOnMissingBean              // skip if the user defined their own
    RestClient.Builder restClientBuilder(RestClientBuilderConfigurer configurer) {
        RestClient.Builder builder = RestClient.builder();   // blank factory object
        return configurer.configure(builder);                // apply all customizers
    }

    @Bean
    @ConditionalOnMissingBean
    RestClientBuilderConfigurer restClientBuilderConfigurer(
            RestClientBuilderCustomizer... customizers) {
        return new RestClientBuilderConfigurer(customizers);
    }
}
```

And the configurer:

```java
class RestClientBuilderConfigurer {
    private final RestClientBuilderCustomizer[] customizers;

    RestClient.Builder configure(RestClient.Builder builder) {
        for (RestClientBuilderCustomizer customizer : this.customizers) {
            customizer.customize(builder);          // tweak timeouts, factory, etc.
        }
        return builder;
    }
}
```

Key points:

- `RestClient.builder()` is a **static factory method** returning a new, empty
  `RestClient.Builder`.
- `RestClientBuilderConfigurer` loops over every registered **customizer** and
  lets each one tweak the builder (e.g. read `spring.http.client.*` timeout
  properties and apply them).
- The finished builder becomes the bean named `restClientBuilder`, which is what
  lands in your `OrderController` constructor.

You can see a commented teaching replica of this in:
`src/main/java/com/example/spring3/config/RestClientAutoConfigurationDemo.java`
(not registered, so it doesn't conflict — it exists purely to show the mechanism).

---

## Q6. What is the purpose of this constructor line?

```java
public OrderController(RestClient.Builder builder) {
    this.restClient = builder.baseUrl("http://localhost:8080").build();
}
```

Its purpose: **create the `RestClient` (the HTTP client) the controller uses to
call the Inventory API — once, at startup, when Spring builds the bean.**

- `builder` → the injected blank `RestClient.Builder`
- `.baseUrl("http://localhost:8080")` → sets the root address, so every call only
  writes the path (`.uri("/api/inventory/{itemId}")`) and the host is prepended
  automatically
- `.build()` → produces the **finished, immutable `RestClient`**
- `this.restClient = ...` → stored in the `final` field for reuse by every handler

So: **configure once, reuse many times.** Every `placeOrder(...)` call then runs:

```java
restClient.get().uri("/api/inventory/{itemId}", itemId).retrieve().body(Map.class);
```

without any reconfiguration. Without this constructor you'd have to build a fresh
`RestClient` inside every method.

---

## Summary (one-liners)

| Question | One-liner answer |
|----------|------------------|
| How is `restClient` injected? | Constructor injection — Spring passes in `RestClient.Builder`, constructor calls `.build()` |
| Why can't `final` be field-injected? | `final` must be assigned during construction; field injection happens after |
| Does `@Autowired` work on `final`? | No — must use constructor injection instead |
| Where's the `@Bean` for the builder? | Spring Boot's auto-configuration (`RestClientAutoConfiguration`), not user code |
| What's inside that auto-config? | `RestClient.builder()` factory + a configurer applying customizers |
| Purpose of the constructor line? | Configure the base URL once and build a reusable `RestClient` |
