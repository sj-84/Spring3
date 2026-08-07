package com.example.spring3.config;

import com.example.spring3.auth.JWTAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig { //jwtauthfilter used

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JWTAuthFilter jwtAuthFilter) throws IOException {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/api/public", "/api/auth/login", "/api/inventory/**", "/api/order/**").permitAll() //requestMatchers takes varargs as arg which is an array
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) { //configuration of user and pass
        return new InMemoryUserDetailsManager(
                User.withUsername("admin").password(passwordEncoder.encode("password")).roles("USER").build(),
                User.withUsername("user").password(passwordEncoder.encode("1234")).roles("USER").build());
    }
}

//Because / is explicitly whitelisted in SecurityConfig.java:
//.requestMatchers("/", "/api/auth/login").permitAll()
//.permitAll() means Spring Security skips authentication for that path. Any request matching / (or /api/auth/login) passes through without a token. All other paths (anyRequest().authenticated()) still require one.
//The JWT filter runs regardless, but since home() is in the public list, a missing/invalid token doesn't block it.


//Because Spring Security's authorization rules decide who gets blocked — and /api/public is explicitly marked permitAll() in SecurityConfig.java.
//Flow when a request hits /api/public:
//1.
//JWTAuthFilter runs first (it's added before UsernamePasswordAuthenticationFilter) — but it only sets authentication if a valid token is present. It never blocks anything.
//2.
//Next, Spring Security's authorization filter checks the request against your rules:
//.requestMatchers("/", "/api/public", "/api/auth/login").permitAll()
//.anyRequest().authenticated()
//3.
/// api/public matches the first rule → permitAll() → the filter chain says "access granted" without checking for a token → your controller runs.
//For /api/private, no rule matches except .anyRequest().authenticated(), so it requires an authenticated user (i.e., a valid token).
//So "public" just means the authorization rule allows it through anonymously — the JWT filter still processes every request, it just isn't enforced there.