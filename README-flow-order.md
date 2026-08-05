# Weather Demo — Bean, Component, Flow & Order (from A to Z)

This README explains everything discussed about the **weather demo** in this project:
what `@Bean` and `@Component` mean, the files involved, the exact flow from app
startup to console output, and **how Spring decides the order** of creation.

> Simple mental model: **Spring is a waiter.**
> `@Component` = "I make coffee here, add me to your list."
> `@Bean` = "I have milk in the fridge, take note of it."
> Wiring = the waiter hands the coffee maker the milk when asked.

---

## 1. The files involved

```
src/main/java/com/example/spring3/demo/
├── WeatherApiClient.java   (plain class — no annotation)
├── WeatherConfig.java      (@Configuration + @Bean)   → builds the client
├── WeatherService.java     (@Component)               → your own service
└── DemoRunner.java         (@Component, CommandLineRunner) → "start button"

src/main/resources/application.properties
└── app.weather.base-url=https://api.open-meteo.com
    app.weather.timeout=5
```

### WeatherApiClient.java — the "third-party" class (no annotation)

```java
public class WeatherApiClient {

    private final String baseUrl;
    private final int timeoutSeconds;

    public WeatherApiClient(String baseUrl, int timeoutSeconds) {
        this.baseUrl = baseUrl;
        this.timeoutSeconds = timeoutSeconds;
    }

    public double fetchTemperature(String city) {
        System.out.println("[GET " + baseUrl + "/current?city=" + city + " (timeout " + timeoutSeconds + "s)]");
        return 19.5;
    }
}
```

Why **no `@Component`**? Because it needs constructor arguments (`baseUrl`,
`timeoutSeconds`) that only a config can supply. A class annotated `@Component`
can only have Spring call its constructor as-is — it can't be told "build me with
these values from a properties file." So this is a `@Bean` job.

### WeatherConfig.java — the `@Bean` way

```java
@Configuration
public class WeatherConfig {

    @Bean
    public WeatherApiClient weatherApiClient(@Value("${app.weather.base-url}") String baseUrl,
                                             @Value("${app.weather.timeout}") int timeoutSeconds) {
        return new WeatherApiClient(baseUrl, timeoutSeconds);
    }
}
```

- `@Bean` on a method = "this method **produces** a bean."
- `@Value("${...}")` pulls values from `application.properties`.
- The `@Configuration` class just holds these factory methods.

### WeatherService.java — the `@Component` way

```java
@Component
public class WeatherService {

    private final WeatherApiClient weatherApiClient;

    WeatherService(WeatherApiClient weatherApiClient) {   // <-- injection point
        this.weatherApiClient = weatherApiClient;
    }

    public String report(String city) {
        double temp = weatherApiClient.fetchTemperature(city);
        return "Current temperature in " + city + " is " + temp + "°C";
    }
}
```

- `@Component` = "this class **is** a bean, manage an instance of me."
- The constructor needs a `WeatherApiClient`. Spring finds that bean and **passes
  it in** — this is called **constructor injection**. No `new`, no `@Autowired`
  needed (single constructor = automatic).

### DemoRunner.java — the "start button"

```java
@Component
public class DemoRunner implements CommandLineRunner {

    private final WeatherService weatherService;

    DemoRunner(WeatherService weatherService) { ... }

    @Override
    public void run(String... args) {
        System.out.println(weatherService.report("Tokyo"));
    }
}
```

- `CommandLineRunner.run(...)` fires **once, after all beans are created**, at startup.
- It exists only so we can *see* the demo work — it's not needed to understand the idea.

---

## 2. `@Bean` vs `@Component` — the difference at a glance

| | `@Bean` | `@Component` |
|---|---|---|
| Where | on a **method** in a `@Configuration` class | on the **class** itself |
| Who creates it | you: the method body (`return new ...`) | Spring, automatically |
| Class ownership | usually a library/third-party class | your own class |
| Needs custom setup? | yes — you decide args and logic | no — Spring calls the constructor |
| Return type | can return an interface/supertype | the class itself is the bean |

**Same end result:** both produce a bean stored in Spring's context. Different question:
- `@Component` → "this class **IS** a bean"
- `@Bean` → "this method **PRODUCES** a bean"

---

## 3. The flow — step by step (A to Z)

**Step 1 — Spring starts up.**
`@SpringBootApplication` enables component scanning of `com.example.spring3`.
Spring finds: `WeatherConfig`, `WeatherService`, `DemoRunner`. It does **not** order
them yet — it just makes a *list* of everything that exists.

**Step 2 — The `@Bean` method runs.**
Spring calls `weatherApiClient(...)`. `@Value` reads the properties, the method runs
`new WeatherApiClient("https://api.open-meteo.com", 5)`, and Spring stores the result
as a bean named `weatherApiClient`.

**Step 3 — The `@Component` classes are created.**
Spring creates `WeatherService`. Its constructor asks for a `WeatherApiClient` —
Spring has one (from step 2), so it hands it over. Same for `DemoRunner`: it needs a
`WeatherService`, which now exists, so Spring gives it that.

**Step 4 — The start button fires.**
After everything is created, Spring calls `DemoRunner.run(...)`, which calls
`weatherService.report("Tokyo")`.

**Step 5 — The call chain runs.**

```
DemoRunner.run("Tokyo")
   └─ WeatherService.report("Tokyo")
        └─ weatherApiClient.fetchTemperature("Tokyo")   // prints: [GET ...]
             returns 19.5
        └─ builds: "Current temperature in Tokyo is 19.5°C"
   └─ System.out.println(...)
```

**Console output when you run the app:**

```
[GET https://api.open-meteo.com/current?city=Tokyo (timeout 5s)]
Current temperature in Tokyo is 19.5°C
```

---

## 4. How Spring knows the order (the key question)

Spring does **not** follow a scripted sequence like "check config → then service →
then runner." It works like this:

1. **Collect everything first.** All beans (`@Bean` methods + `@Component` classes)
   are gathered into one list. Order in the list doesn't matter.
2. **Read each bean's needs.** Spring looks at each constructor:
   - `WeatherApiClient` → needs **nothing** → created immediately.
   - `WeatherService` → needs `WeatherApiClient` → waits for it.
   - `DemoRunner` → needs `WeatherService` → waits for it.
3. **The order falls out automatically** from dependencies:

```
WeatherApiClient   → created (no dependencies)
      ↓
WeatherService     → created (client now exists)
      ↓
DemoRunner         → created (service now exists)
```

So Spring never "knows the flow." It just creates a bean only once everything it
needs already exists. The order is a **consequence of dependencies**, not a script.
That's why the classes can be written in any order and it still works.

---

## 5. One-line summary of the whole demo

**One object built (`@Bean` in `WeatherConfig`), one class managed
(`@Component` on `WeatherService`), one start button (`DemoRunner`), and Spring
connects them by matching constructor types — in dependency order, automatically.**

---

## FAQ

### What is `CommandLineRunner`?

`CommandLineRunner` is Spring Boot's built-in "run this code after startup" hook.

- You make a class implement it and put `@Component` on it.
- Spring Boot calls `run(String... args)` **once, automatically, right after the
  app starts** — after all beans are created.
- `String... args` = the command-line arguments you passed to
  `mvn spring-boot:run ...` (usually empty).

In this demo it's the "start button" that exists so the weather report actually
prints at startup:

```java
@Component
public class DemoRunner implements CommandLineRunner {
    @Override
    public void run(String... args) {
        System.out.println(weatherService.report("Tokyo"));
    }
}
```

Without it, the beans would exist but nobody would ever call `report()` — the app
would just start and do nothing visible.

### What is the `@Bean` method doing? (the `WeatherConfig` method)

```java
public WeatherApiClient weatherApiClient(@Value("${app.weather.base-url}") String baseUrl,
                                         @Value("${app.weather.timeout}") int timeoutSeconds) {
    return new WeatherApiClient(baseUrl, timeoutSeconds);
}
```

- `public WeatherApiClient weatherApiClient(...)` — a normal method that returns a
  `WeatherApiClient`. The method name (`weatherApiClient`) becomes the **bean's name**
  in Spring.
- `@Value("${app.weather.base-url}") String baseUrl` — Spring reads
  `app.weather.base-url` from `application.properties` (`https://api.open-meteo.com`)
  and puts it into the `baseUrl` variable. The `${...}` is the "look this up in
  properties" syntax.
- `@Value("${app.weather.timeout}") int timeoutSeconds` — same, but for the number
  `5`. Spring converts the string `"5"` to an `int` automatically.
- `return new WeatherApiClient(baseUrl, timeoutSeconds);` — the method body constructs
  the object using those two values.
- `@Bean` (on the method above) — tells Spring: "the return value of this method is a
  bean, store it in the context."

So when Spring starts:

```
reads app.weather.base-url → "https://api.open-meteo.com"
reads app.weather.timeout  → 5
runs new WeatherApiClient("https://api.open-meteo.com", 5)
stores the result as bean named "weatherApiClient"
```

`@Bean` + method args = "I need these values from config, and here's how I build my
object with them."

### Is an actual API call happening?

**No.** The line in `WeatherApiClient` only *prints* the URL string to the console —
no real network request is sent.

```java
public double fetchTemperature(String city) {
    System.out.println("[GET " + baseUrl + "/current?city=" + city + " (timeout " + timeoutSeconds + "s)]");
    return 19.5;   // hardcoded temperature
}
```

- `System.out.println("..." + var + ...)` = **string concatenation**: the `+` glues
  text and variables into one string, which is then printed.

  `[GET https://api.open-meteo.com/current?city=Tokyo (timeout 5s)]`

- The `return 19.5;` is a **hardcoded** result.
- The whole thing is a **simulation** so the demo works without internet. A real
  client would replace that body with an HTTP call (e.g. via `RestClient`,
  `WebClient`, or `java.net.http.HttpClient`) and parse the JSON response.

---

## How to run

```
mvn spring-boot:run
```

Look for the weather line in the console (the demo prints at startup).
