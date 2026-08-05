# Spring 3 — JWT Security Demo

A Spring Boot app that secures endpoints with JWT. The purpose of this README is to explain, **through this project's own code**, the difference between the two main ways Spring registers beans: `@Bean` and `@Component`.

## What is a "bean"?

A bean is simply an object whose lifecycle is managed by Spring. Spring creates one instance, stores it in the application context, and injects it wherever it's needed — so you never write `new JWTService()` yourself.

There are two ways to register a bean:

| | `@Component` | `@Bean` |
|---|---|---|
| Where it goes | directly on the class | on a method inside a `@Configuration` class |
| Who owns the class | you (it's your application code) | usually a third-party / library class |
| Discovery | automatic via `@ComponentScan` | you explicitly call the factory method |
| Construction control | none — Spring calls the constructor as-is | full — you decide parameters and setup logic |
| Can return an interface/supertype | no (the class itself is registered) | yes |
| Typical use | services, repositories, controllers, filters | config objects, encoders, filter chains, external libs |

Both end with the same result — a bean in the context — but they answer different questions: `@Component` = "this class IS a bean", `@Bean` = "this method PRODUCES a bean".

---

## `@Component` — used in this project

### `auth/JWTService.java`

```java
@Component //see later why cannot be done with bean
public class JWTService {
    public JWTService(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    ...
}
```

- `JWTService` is **our own class**, so we can annotate it directly.
- Spring's scanner (enabled by `@SpringBootApplication`) finds the `@Component`, creates **one** instance, and stores it in the context.
- Its one dependency (the secret from `application.properties`) is provided via the constructor with `@Value`.

### `auth/JWTAuthFilter.java`

```java
@Component
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTService jwtService;
    private final UserDetailsService UserDetailsService;

    JWTAuthFilter(JWTService jwtService, UserDetailsService userDetailsService) {   // constructor injection
        this.jwtService = jwtService;
        this.UserDetailsService = userDetailsService;
    }
    ...
}
```

- Spring sees the constructor needs a `JWTService` (a `@Component` bean) and a `UserDetailsService` (a `@Bean` bean) and **auto-wires them** — no `@Autowired` needed with a single constructor.
- Note that this class mixes both worlds: it is a `@Component` that consumes a `@Bean`.

### Variants of `@Component`

`@Service`, `@Repository`, `@Controller` are all `@Component` under the hood — they just add semantic meaning (business logic, data access, web layer) and special handling (e.g. `@Controller` is picked up by Spring MVC).

---

## `@Bean` — used in this project

### `config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTAuthFilter jwtAuthFilter) throws IOException {
        ...
        return http.build();   // returns an interface/abstract impl, not a single class
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(   // concrete impl hidden behind the interface
                User.withUsername("admin").password(passwordEncoder.encode("password")).roles("USER").build(),
                User.withUsername("user").password(passwordEncoder.encode("1234")).roles("USER").build());
    }
}
```

Why each one is a `@Bean` and *cannot* be a `@Component`:

1. **`SecurityFilterChain`** — a library interface built through a builder chain (`http.build()`). You can't annotate it, and it needs setup logic.
2. **`PasswordEncoder`** — `BCryptPasswordEncoder` is a third-party class from Spring Security; you can't put `@Component` on it.
3. **`UserDetailsService`** — needs the `passwordEncoder` bean passed in as a **method parameter** (`@Bean` methods take their dependencies as args), and it returns an interface while the real object is an `InMemoryUserDetailsManager`. Plus the construction has real logic: encoding each password.

### Cross-dependency

`securityFilterChain(...)` takes `JWTAuthFilter` as a parameter. Because `JWTService` and `JWTAuthFilter` are `@Component` beans, Spring can inject them into this `@Bean` method's signature. This is how the two worlds connect:

```
@Component JWTService      ──>  injected into
@Component JWTAuthFilter   ──>  injected into
@Bean SecurityFilterChain  (via its method parameter)
```

---

## What if `JWTService` were a `@Bean` instead?

It's *possible*, but it's strictly worse for this case. You would need to strip `@Component`, write a config class, and hand-wire everything:

```java
// JWTService.java — no @Component now
public class JWTService { ... }

// BeanConfig.java
@Configuration
public class BeanConfig {

    @Bean
    public JWTService jwtService(@Value("${app.jwt.secret}") String secret) {
        return new JWTService(secret);
    }

    @Bean
    public JWTAuthFilter jwtAuthFilter(JWTService jwtService, UserDetailsService userDetailsService) {
        return new JWTAuthFilter(jwtService, userDetailsService);
    }
}
```

It works, but adds a config class, a factory method per bean, and manual constructor management. That's why the comment in `JWTService.java` says it *cannot be done better with `@Bean`*: `@Component` gives the same result with less code.

**When you *should* reach for `@Bean`:** you can't annotate the class (libraries), you need construction logic or conditional setup, or you want to return an interface/supertype while hiding the concrete class.

---

## How to run

```
mvn spring-boot:run
```

Public endpoints: `/`, `/api/public`, `/api/auth/login`
Everything else requires a `Authorization: Bearer <token>` header.

Login with:

```
POST /api/auth/login
{ "username": "admin", "password": "password" }
```
