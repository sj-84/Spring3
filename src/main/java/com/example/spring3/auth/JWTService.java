package com.example.spring3.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component //see later why cannot be done with bean
public class JWTService { //no dependency from other class
    public static final long EXPIRATION_SECONDS = 3600;
    private final SecretKey secretKey;

    public JWTService(@Value("${app.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
                .signWith(secretKey)
                .compact();
    }

    public Optional<String> validate(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
            return Optional.of(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

//@Component is a Spring annotation that marks a class as a Spring-managed bean. Spring's component scanner detects it and registers an instance in the application context, so you can inject it with @Autowired:
//@Component
//public class UserService { ... }
//You enable scanning with @ComponentScan (or via @SpringBootApplication in Spring Boot). Variants with more specific roles: @Service, @Repository, @Controller — all are @Component under the hood.

//=================================================================================
// LINE-BY-LINE EXPLANATION (line numbers refer to the code above, before these comments)
// A JWT (JSON Web Token) is a string of text that acts like a "signed ID card".
// The server creates one when you log in, and trusts it later because it can
// verify the signature with the secret key.
//=================================================================================

// Line 1: package com.example.spring3.service;
// Declares that this class lives in the "service" folder. "package" is how Java
// groups related classes and avoids name collisions between projects.

// Line 3: import io.jsonwebtoken.Claims;
// "Claims" is the library's object that holds the data INSIDE a token
// (the username, issued date, expiry date). "import" lets us use it by short name.

// Line 4: import io.jsonwebtoken.JwtException;
// The exception type thrown when a token is invalid: tampered, expired, or malformed.

// Line 5: import io.jsonwebtoken.Jwts;
// The main class of the library. It has the .builder() (to create tokens) and
// .parser() (to read/verify tokens) methods.

// Line 6: import io.jsonwebtoken.security.Keys;
// Helper class that turns a plain String secret into a real crypto SecretKey.

// Line 7: import org.springframework.beans.factory.annotation.Value;
// @Value lets Spring inject a value from config files (application.properties/yml)
// directly into a method parameter or field.

// Line 8: import org.springframework.stereotype.Component;
// @Component is the annotation that tells Spring "manage an instance of this class."

// Line 10: import javax.crypto.SecretKey;
// Java's standard interface for a symmetric (single-key) encryption key.

// Line 11: import java.nio.charset.StandardCharsets;
// Provides UTF_8, needed to convert the secret String into bytes correctly.

// Line 12: import java.time.Instant;
// Instant represents a precise moment in time (e.g. "2026-08-03 12:00:00Z").

// Line 13: import java.util.Date;
// Date is the older time type that the JWT library actually wants. We convert
// Instant -> Date.

// Line 14: import java.util.Optional;
// Optional is a box that is either full (holds a value) or empty. A safe way to
// return "maybe there is a result, maybe not" without using null.

// Line 16: @Component
// Spring scans for classes annotated with @Component, creates ONE instance
// ("bean") of each, and stores it in the application context so other classes
// can inject it. (Detailed explanation right below this comment block.)

// Line 17: public class JWTService {
// Opens the class. "public" means any other class can use it. All the JWT logic
// lives inside these braces.

// Line 18: public static final long EXPIRATION_SECONDS = 3600;
// A constant. public = readable by anyone, static = one shared copy (not one per
// object), final = cannot be changed. Value 3600 = tokens expire after 1 hour.

// Line 19: private final SecretKey secretKey;
// A field (a variable that belongs to each JWTService object). private = only this
// class can access it. final = assigned once, in the constructor, and never changed.
// It is the key used to sign and verify every token.

// Line 21: public JWTService(@Value("${app.jwt.secret}") String secret) {
// The constructor: runs when Spring creates the JWTService bean. Spring reads the
// value of "app.jwt.secret" from the config file and passes it in as "secret".

// Line 22: this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
// Converts the secret String into bytes (getBytes(UTF_8)), then turns those bytes
// into a real SecretKey for the HMAC-SHA signing algorithm.

// Line 23: }  // closes the constructor.

// Line 25: public String createToken(String username) {
// Method that anyone can call. You give it a username, it returns a token String.

// Line 26: Instant now = Instant.now();
// Stores the current moment in time, so "issued at" and "expires at" are based on
// the same starting point.

// Line 27: return Jwts.builder()
// Starts building a new token (chained calls follow). The finished token is returned.

// Line 28:     .subject(username)
// Sets the "subject" field of the token to the username. This is the piece of data
// we read back later to know WHO the token belongs to.

// Line 29:     .issuedAt(Date.from(now))
// Records when the token was created. Date.from() converts Instant back to Date.

// Line 30:     .expiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
// Sets expiry = now + 3600 seconds (one hour from creation). After this the token
// is considered invalid.

// Line 31:     .signWith(secretKey)
// Cryptographically signs all the above data with the secret key. This is the
// security guarantee: anyone who modifies the token cannot fake a valid signature.

// Line 32:     .compact();
// Builds the final token String in the "xxx.yyy.zzz" format (header.payload.signature).

// Line 33: }  // closes createToken.

// Line 35: public Optional<String> validate(String token) {
// Method that checks a token and returns an Optional<String>: the username if the
// token is valid, or an empty Optional if it is not.

// Line 36: try {
// Starts a block that attempts the risky operation and catches failures below.

// Line 37: Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
// Read right-to-left:
//   .verifyWith(secretKey)  -> use our key to check the signature
//   .build()                -> assemble the configured parser
//   .parseSignedClaims(token) -> verify signature + expiry and read the token's data
//   .getPayload()           -> pull out the actual data as a Claims object
// If the signature is wrong or the token has expired, this throws an exception.

// Line 38: return Optional.of(claims.getSubject());
// If verification passed: get the username we stored in createToken() (getSubject())
// and wrap it in Optional.of(...) so callers get a guaranteed non-null value.

// Line 39: } catch (JwtException | IllegalArgumentException e) {
// Catches the failure cases. JwtException = bad signature, expired, or malformed.
// IllegalArgumentException = null/empty token. The "|" means "catch either type."

// Line 40: return Optional.empty();
// On failure return an empty Optional, meaning "invalid token". No crash; callers
// simply see there is no username.

// Line 41: }  // closes the catch block.

// Line 42: }  // closes the validate method.

// Line 43: }  // closes the JWTService class.

// HOW IT ALL FITS TOGETHER:
// 1) User logs in -> app calls createToken(username) -> server hands the token to the client.
// 2) Client sends the token back with each request (usually in an "Authorization" header).
// 3) App calls validate(token) -> if the signature checks out and it is not expired,
//    the username is returned and the server knows who is making the request — without
//    the client ever sending a password again.