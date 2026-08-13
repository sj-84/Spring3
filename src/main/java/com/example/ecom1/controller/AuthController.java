package com.example.ecom1.controller;

import com.example.ecom1.auth.JWTService;
import com.example.ecom1.model.RegisterRequest;
import com.example.ecom1.model.Role;
import com.example.ecom1.model.User;
import com.example.ecom1.model.loginRequest;
import com.example.ecom1.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JWTService jwtService;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    AuthController(JWTService jwtService, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository; 
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) { //why RegisterRequest? what is  ResponseEntity<?>?
        if(userRepository.existsByUserName(registerRequest.username())) {
            return ResponseEntity.status(409).body("User name already exists"); //what is this return?
        }

        User user = new User(); //why we are creating instance, is it because for custom types which store data spring dont create objects but us since one object means one person and hence how spring create a person
        user.setUsername(registerRequest.username());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setEmail(registerRequest.email());
        user.setRole(registerRequest.role() == null ? Role.CUSTOMER : registerRequest.role());

        userRepository.save(user); //what this does?
        return ResponseEntity.status(201).body("User registered successfully"); //what is this return?
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody loginRequest loginRequest) { //what is ResponseEntity<?>
        try {
            UserDetails user = userDetailsService.loadUserByUsername(loginRequest.id()); //what is id()
            if (!passwordEncoder.matches(loginRequest.pass(), user.getPassword())) { //what is pass()
                return ResponseEntity.status(401).body(Map.of("error", "Bad credentials"));
            }
            return ResponseEntity.ok(Map.of("token", jwtService.createToken(user.getUsername())));
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Bad credentials"));
        }
    }

    @GetMapping("/me")
    public Map<String, String> me(@AuthenticationPrincipal UserDetails user) {
        return Map.of("username", user.getUsername());
    }
}

//The three questions in AuthController.java:
//        1. ResponseEntity<?> (line 29) — ? is a wildcard meaning "any type". ResponseEntity<T> wraps an HTTP response (status + body + headers) with a typed body. ResponseEntity<?> just says the body type varies (here it's Map.of(...) — a Map<String, String> on both the success and error paths). You could tighten it to ResponseEntity<Map<String, String>> since both branches return that.
//        2. loginRequest.id() (line 31) — loginRequest is a Java record (see model/loginRequest.java:6). The compiler auto-generates accessor methods named after each component, so id() returns the id field and pass() returns the pass field. That's why it reads like a method call instead of loginRequest.id.
//        3. loginRequest.pass() (line 32) — same as above; the auto-generated accessor for the pass component.
//        Note: unlike a normal class where fields are private and accessed via getters, a record's accessors drop the get prefix — record Point(int x) gives you point.x(), not point.getX().

//=============================================================================
// THE NEW DOUBTS IN register() — FRIENDLY EXPLANATIONS
//=============================================================================

// Line 35: public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest)
//   a) Why RegisterRequest and not User?
//      Because the client's JSON and your database entity are NOT the same shape.
//      The client sends only: { "username", "password", "email", "role" } — no id.
//      User, on the other hand, is a DATABASE entity with id + all the DB rules
//      (@Id, @GeneratedValue, unique constraints...). Accepting a User directly
//      from the internet would let a hacker send "id", "role=ADMIN", anything —
//      very dangerous. So RegisterRequest is just the "signup form" the client
//      fills out; it only carries the four safe fields.
//   b) What is ResponseEntity<?>?
//      It's the box that carries the HTTP answer back to the browser:
//      (status code + body + headers). The "?" just means "I'm not locking the
//      body to one type" — here the body is sometimes a String, sometimes a Map.
//      ResponseEntity.status(201).body(...) means: reply with status 201 and
//      put this text in the body.

// Line 37: return ResponseEntity.status(409).body("User name already exists");
//   409 = CONFLICT. HTTP's way of saying "what you're trying to create already
//   exists, so I refuse." This is the standard answer when a duplicate username
//   is submitted. The body message is what the caller sees.
//   (In the friendly chat earlier I showed a Map version — either is fine; a
//   plain String body is simpler.)

// Line 40: User user = new User(); — "why do WE create the instance?"
//   Because a real person is NEW — they don't exist in the database yet, so there
//   is nothing for Spring/JPA to load. Someone must say "make a fresh empty
//   person object" and then fill it in. That someone is you, here.
//   Spring's rule is: it MAPS objects to rows, but it does not INVENT your domain
//   objects for you. One User object = one future row = one person.
//   Then the next 4 lines fill the empty person with the form data:
//   username, a BCrypt-HASHED password (never the raw password!), email, and the
//   role (defaulting to CUSTOMER if the client sent none).

// Line 46: userRepository.save(user); — "what does this do?"
//   This is the moment the new person actually lands in Postgres. JPA takes the
//   User object and turns it into an INSERT statement:
//       INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)
//   Because the id is null, the database auto-increments it (that's the
//   @GeneratedValue(IDENTITY) we saw in User.java). The password column receives
//   the hashed value, never the plain text.

// Line 47: return ResponseEntity.status(201).body("User registered successfully");
//   201 = CREATED. The standard success code for "a new resource was made".
//   (200 would mean "OK" — but 201 is more precise: something was created.)
//   Together with the 409 above, the client now gets clear answers:
//   username free  -> 201 "User registered successfully"
//   username taken -> 409 "User name already exists"