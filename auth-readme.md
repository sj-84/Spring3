# Spring3 Auth — Key Points

## Users (in-memory, no DB)
Defined in `config/SecurityConfig.java` (`userDetailsService` bean), passwords hashed with BCrypt:

| Username | Password | Role |
|----------|----------|------|
| admin    | password | USER |
| user     | 1234     | USER |

## JWT Configuration
- Created/validated by `auth/JWTService.java`
- **Expiry:** 3600 seconds (1 hour) — change `EXPIRATION_SECONDS`
- **Secret:** read from `app.jwt.secret` in `application.properties`
- Token subject = username; expired/tampered tokens are rejected automatically on validation

## Endpoints
| Method | Path             | Auth required | Description                     |
|--------|------------------|---------------|---------------------------------|
| GET    | /                | No            | Root health check               |
| GET    | /api/public      | No            | Public check                    |
| POST   | /api/auth/login  | No            | Returns a JWT token             |
| GET    | /api/private     | Yes           | Private check                   |
| GET    | /api/auth/me     | Yes           | Returns username from token     |

## Security Flow
1. Client POSTs credentials to `/api/auth/login` → `AuthController` checks via `passwordEncoder.matches(...)` → returns `{"token":"..."}`
2. Client sends the token on every request as `Authorization: Bearer <token>`
3. `auth/JWTAuthFilter` validates the token and sets Spring Security's `SecurityContext` (only for valid tokens)
4. Authorization rules in `SecurityConfig` decide access:
   - `.permitAll()`: `/`, `/api/public`, `/api/auth/login`
   - `.anyRequest().authenticated()`: everything else

## Security Config Details
- Stateless sessions (`SessionCreationPolicy.STATELESS`)
- CSRF, form login, and HTTP basic disabled
- JWT filter added before `UsernamePasswordAuthenticationFilter`

## How to Test
Login:
```
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"id\":\"admin\",\"pass\":\"password\"}"
```
Response: `{"token":"eyJ..."}`

Protected route:
```
curl http://localhost:8080/api/private -H "Authorization: Bearer eyJ..."
```
Response: `{"message":"Private - accessible only after login"}`

## Q&A (points covered earlier)

**Q1. What is `ResponseEntity<?>` in the login method?**
`ResponseEntity<T>` is Spring's class for wrapping an HTTP response — it holds the status code, body, and headers together, so a controller can return status + body in one object. `<T>` is the generic type of the body. `<?>` is a wildcard meaning "any type" — it says "the body could be anything", which is a bit loose. In `AuthController` both branches actually return a `Map<String, String>` (the `{"error": ...}` and `{"token": ...}` bodies), so you could tighten it to `ResponseEntity<Map<String, String>>`. The `?>` works, but `Map<String, String>` is more precise and self-documenting.

**Q2. What is `loginRequest.id()`?**
`loginRequest` is declared as a Java **record** (`public record loginRequest(String id, String pass)`) — a Java 16+ feature for immutable data carriers. The compiler auto-generates the constructor, `equals()`, `hashCode()`, `toString()`, and accessor methods. Records drop the `get` prefix: a record `Point(int x)` gives you `point.x()`, not `point.getX()`. So `loginRequest.id()` is just the auto-generated accessor that returns the `id` component. It reads like a method call because it *is* one. Records are implicitly final, can't extend other classes, and their fields are final — roughly 30 lines of boilerplate replaced by one line.

**Q3. What is `loginRequest.pass()`?**
Same as Q2 — the compiler-generated accessor for the `pass` component of the record. `id` and `pass` are the two components declared in the record header, and each gets a matching accessor.

**Q4. What is `Map.of("status", "OK")`?**
A **static factory method** added in Java 9 that builds an immutable `Map` in one line. `Map.of("status", "OK")` creates a `Map<String, String>` with one entry (key `"status"`, value `"OK"`). Arguments are key-value pairs, key first, so `Map.of("a", 1, "b", 2)` makes a two-entry map. Unlike `new HashMap<>()`, the result cannot be modified (no `put`), and the type parameters are inferred automatically. The slightly more explicit version that stores it in a variable first:
```java
Map<String, String> result = Map.of("status", "OK");
return result;
```

**Q5. Why doesn't `home()` ask for authentication?**
Because `/` is explicitly whitelisted with `.permitAll()` in `SecurityConfig`:
```java
.requestMatchers("/", "/api/public", "/api/auth/login").permitAll()
```
Spring Security's JWT filter (`JWTAuthFilter`) runs on *every* request, but it only ever **sets** authentication into the `SecurityContext` when a valid token is present — it never blocks a request itself. Blocking happens later in the authorization filter, which matches the URL against the rules. `/` matches the `permitAll()` rule, so access is granted even with no token at all. Every other path falls to `.anyRequest().authenticated()` and is rejected unless a valid token was found.

**Q6. How long is the token valid?**
1 hour (3600 seconds), set as a constant in `JWTService`:
```java
public static final long EXPIRATION_SECONDS = 3600;
```
`createToken()` stamps the token with `.issuedAt(now)` and `.expiration(now + 3600s)`. When `validate()` runs, `parseSignedClaims(token)` automatically throws `ExpiredJwtException` if the token is past its expiry (it also throws if the signature was tampered with). `validate()` catches both cases and returns `Optional.empty()`. So expired tokens are rejected automatically — no manual date check in your code. Change `EXPIRATION_SECONDS` (e.g. 7200 = 2 hours) to adjust.

**Q7. Where are the username/password set?**
In `SecurityConfig`'s `userDetailsService` bean, which returns an `InMemoryUserDetailsManager` — a Spring Security store that keeps users in memory, not a database:
```java
User.withUsername("admin").password(passwordEncoder.encode("password")).roles("USER").build(),
User.withUsername("user").password(passwordEncoder.encode("1234")).roles("USER").build()
```
So the two accounts are **admin/password** and **user/1234**. The `passwordEncoder` bean is a `BCryptPasswordEncoder`, so the stored values are BCrypt hashes, never plain text. On login, `AuthController` calls `passwordEncoder.matches(loginRequest.pass(), user.getPassword())` to compare the submitted password against the hash. Because the store is in memory, the same two users exist after every restart — there is no persistence.

**Q8. How does the public API work without login?**
`permitAll()` is an *authorization* rule, not a login requirement. The request flow for `GET /api/public`:
1. `JWTAuthFilter` runs first (it's inserted before `UsernamePasswordAuthenticationFilter`). It reads the `Authorization` header; if a valid `Bearer` token is there it loads the user and sets authentication in the `SecurityContext`. It never rejects anything.
2. The authorization filter then matches the request URL against the configured rules. `/api/public` matches `.permitAll()`, so access is granted immediately — a token is never required.
3. Your controller method runs and returns the response.
For `/api/private`, no rule matches except `.anyRequest().authenticated()`, so Spring checks the `SecurityContext` for an authenticated user. No valid token → no authentication → request is rejected with 403.

**Q9. How to run the app?**
```
mvn spring-boot:run
```
Maven downloads dependencies (first run takes longer), compiles, and starts the embedded Tomcat server. Wait for the "Started Spring3Application" line in the log — the app listens on port 8080 (`spring.application.name=spring3` and no `server.port`, so 8080 is the default). Stop with `Ctrl+C` in that terminal.

**Q10. How to test login in Postman?**
1. Click **New** → **HTTP Request**: method `POST`, URL `http://localhost:8080/api/auth/login`
2. **Body** tab → select **raw** → change the dropdown from `Text` to **JSON**
3. Paste `{"id":"admin","pass":"password"}` and click **Send**
4. Success → `200` with `{"token":"eyJhbGci..."}`. Wrong credentials → `401` with `{"error":"Bad credentials"}`
5. To test a protected route, create a new request `GET http://localhost:8080/api/private`, go to the **Authorization** tab, set Type to **Bearer Token**, paste the token from step 4, and **Send** → `200` with `{"message":"Private - accessible only after login"}`. Without the token it returns `403`.

