# The `RestClient.Builder` Bean Issue — Why It Happened & How It Was Fixed

> Simple mental model: **Spring is a manager hiring a team before the shop opens.**
> Before opening, the manager hands every worker the tool they asked for.
> If a tool is missing from the toolbox, the whole shop can't open.

---

## 1. The error we saw

```text
***************************
APPLICATION FAILED TO START
***************************

Description:

Parameter 0 of constructor in com.example.ecom1.controller.OrderController
required a bean of type 'org.springframework.web.client.RestClient$Builder'
that could not be found.

Action:

Consider defining a bean of type 'org.springframework.web.client.RestClient$Builder'
in your configuration.
```

---

## 2. What the error means (in plain words)

- `OrderController` is a Spring bean whose constructor asks for a
  `RestClient.Builder` — the tool you use to make HTTP calls to another app.
- Spring's #1 job at startup is **dependency injection**: for every bean, figure
  out what it needs and hand it over before anything runs.
- Here, Spring looked around for a `RestClient.Builder` bean and found **none**.
- A controller is a required singleton — if Spring can't build it, it aborts the
  **entire context**. The app never starts, and the `@SpringBootTest` test fails.

---

## 3. Why the bean was missing

| Version            | What Spring Boot did with `RestClient.Builder`              |
|--------------------|-------------------------------------------------------------|
| Spring Boot 3.x    | **Auto-configured it.** `RestClientAutoConfiguration` registered a prototype `RestClient.Builder` bean whenever `RestClient` was on the classpath. You did nothing. |
| Spring Boot 4.1.0  | **No longer auto-configured** in the artifacts that `spring-boot-starter-web` pulls in. The modularized Boot 4 does not ship that auto-config here. |

Facts from our project:

- `pom.xml` uses `spring-boot-starter-parent` **4.1.0**.
- `OrderController` was added in the last commit while the project was **already
  on 4.1.0**.
- I verified in the local Maven repo: no jar on this classpath contains
  `RestClientAutoConfiguration`. The bean genuinely does not exist by default.
- The context had never started successfully after `OrderController` was added
  (the last run log predates that commit), so this was a **dormant, pre-existing
  bug** — nothing to do with the spring3 → e-com1 rename.

> The teaching file `config/RestClientAutoConfigurationDemo.java` documents the
> exact mechanism Boot 3 used to provide this bean automatically. It is a plain,
> unregistered replica — read it to see "behind the magic."

---

## 4. The fix

Added `src/main/java/com/example/ecom1/config/RestClientConfig.java`:

```java
package com.example.ecom1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	@Scope("prototype")
	public RestClient.Builder restClientBuilder() {
		return RestClient.builder();
	}
}
```

Breaking it down:

| Piece                  | What it does                                                        |
|------------------------|---------------------------------------------------------------------|
| `@Configuration`       | "This class is a recipe book for creating beans."                   |
| `@Bean`                | "The return value of this method is a bean Spring stores."          |
| `RestClient.builder()` | The static factory that creates a blank `RestClient.Builder`.       |
| `@Scope("prototype")`  | Give **every** consumer a fresh builder — never share one.          |

**Why `@Scope("prototype")` matters:**

A `RestClient.Builder` is **mutable**. You chain calls on it before finishing:

```java
builder.baseUrl("http://localhost:8080").build()   // ← modifies the builder
```

If Spring handed everyone the *same* singleton builder, one consumer's
`baseUrl(...)` would leak into another's configuration — like a shared notepad
where everyone's notes get mixed together. Prototype scope gives each worker a
clean copy of the tool.

---

## 5. How the chain works now

```
OrderController(RestClient.Builder builder) { ... }     // asks for the tool
        │
        ▼
RestClientConfig.restClientBuilder()                    // @Bean creates a fresh one
        │
        ▼
RestClient.builder()                                    // the actual tool
```

Spring resolves the constructor argument, creates the builder, hands it to
`OrderController`, and the context starts.

---

## 6. Key takeaways

1. Spring Boot 4 changed what is auto-configured. Code written against Boot 3
   assumptions ("the framework provides this bean") can break on Boot 4.
2. A missing bean in a constructor is a **hard startup failure** — Spring refuses
   to run with an incomplete team.
3. The fix is a small explicit `@Bean` that says "here's how to make one."
4. Remember `@Scope("prototype")` for mutable builder-style objects.
