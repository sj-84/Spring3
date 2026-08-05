# @Bean vs @Component

Both `@Bean` and `@Component` register beans in the Spring container. The difference is in *how* the bean gets created.

## Side-by-side

| Aspect | `@Component` | `@Bean` |
|---|---|---|
| Placement | On a class (your own code) | On a method inside a `@Configuration` class |
| Discovery | Auto-scanned and instantiated by Spring | You call the method yourself; the returned object is the bean |
| Creation control | Spring calls the constructor | You control construction (args, config, concrete class, shared instance) |
| Use for | Your own classes | Third-party/external classes you can't annotate (e.g. `RestTemplate`, `ObjectMapper`) |

## Example

```java
// @Component — class-level, scanned automatically
@Component
public class UserService {
    // fields/methods...
}

// @Bean — method-level, you control creation
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

## So do they serve the same purpose?

Yes — ultimately both produce a Spring-managed bean that ends up in the same container. The difference is purely the *mechanism*:

- `@Component`: Spring scans the classpath and calls the constructor itself (you give up control of instantiation).
- `@Bean`: you write the factory method, so you control how the object is built — what arguments are passed, what configuration it gets, which concrete implementation is returned, and whether it's new or shared.

## When to use which

- Use `@Component` (or its specializations `@Service`, `@Repository`, `@Controller`) for your own classes.
- Use `@Bean` only for third-party types you can't annotate, or when you need custom construction logic.

Most Spring apps use `@Component` for their own code and `@Bean` sparingly.
