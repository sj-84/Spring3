package com.example.ecom1.controller;

import com.example.ecom1.auth.JWTService;
import com.example.ecom1.model.loginRequest;
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

    AuthController(JWTService jwtService, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
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