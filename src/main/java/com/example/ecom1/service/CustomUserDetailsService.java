package com.example.ecom1.service;

import com.example.ecom1.model.User;
import com.example.ecom1.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; //explain
import org.springframework.security.core.userdetails.UserDetailsService; //what is UserDetailsService?
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; //how is the bean created for UserRepository since no annotation is present for UserRepository

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUserName(username).orElseThrow(() -> new UsernameNotFoundException("User not found")); //why giving error with just Exception?
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .build(); //explain the return?
    }
}

//What this does: Spring Security's login code (and your AuthController.login) calls
// loadUserByUsername(username) to fetch the user. This loads them from Postgres instead of the in-memory
// list, and converts your entity into Spring's UserDetails with the role turned into ROLE_CUSTOMER / ROLE_SELLER / ROLE_ADMIN.

//=============================================================================
// LINE-BY-LINE EXPLANATION OF THE DOUBTS
//=============================================================================

// Line 6: import org.springframework.security.core.userdetails.UserDetails;
// UserDetails is Spring Security's interface for "a currently authenticated user".
// It holds getUsername(), getPassword(), getAuthorities() (your roles), and a few
// isEnabled()-style flags. It is what the SecurityContext stores after login —
// your JWTAuthFilter creates one, and your /me endpoint receives it via
// @AuthenticationPrincipal. Your DB User entity is NOT a UserDetails, which is
// why this class must convert it.

// Line 7: import org.springframework.security.core.userdetails.UserDetailsService;
// Spring Security's standard interface with exactly ONE method:
//     UserDetails loadUserByUsername(String username)
// Whenever login needs a user, Spring calls this method. This class implements
// the interface and is a @Service, so Spring uses this version to look users up
// in Postgres — replacing the old InMemoryUserDetailsManager (which was the same
// interface, just backed by hardcoded users in memory).

// Line 16: private final UserRepository userRepository;
// "How is the bean created for UserRepository since no annotation is present?"
// Spring Data JPA generates it automatically. The rule: ANY interface that
// extends JpaRepository gets a proxy implementation created at runtime and
// registered as a Spring bean. Spring Boot enables this via @EnableJpaRepositories
// (switched on by @SpringBootApplication auto-configuration). The interface
// itself is the signal — no annotation needed. So UserRepository IS a Spring
// bean, and the constructor above gets it injected.

// Line 25: .orElseThrow(() -> new Exception("User not found"));
// "Why does this give an error with just Exception?"
// orElseThrow's signature is:
//     <X extends Throwable> X orElseThrow(Supplier<? extends X> exceptionSupplier) throws X
// meaning the exception you supply is what orElseThrow THROWS. Exception is a
// CHECKED exception, so Java forces every method that can throw it to declare
// "throws Exception". loadUserByUsername only declares throws UsernameNotFoundException,
// so the compiler rejects it.
// Fix: use UsernameNotFoundException — it is a RuntimeException (no throws needed)
// and Spring Security understands it:
//     .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username))

// Line 30: .build();
// withUsername(...) is a BUILDER pattern — set fields one at a time instead of a
// long constructor. .withUsername(...) sets the username, .password(...) sets the
// encoded password, .authorities(...) sets the list of roles (ROLE_CUSTOMER /
// ROLE_SELLER / ROLE_ADMIN), and .build() assembles all of it and returns the
// final, ready-to-use UserDetails object. That object is what the login flow and
// your JWT filter trust.




//----------------------------------------------------------------------------------------------------------




//=============================================================================
// FRIENDLY VERSION — THE SAME IDEA WITHOUT THE TECHNICAL WORDS
//=============================================================================

// BIG PICTURE:
// Spring Security is like airport security. It guards your app's doors and every
// visitor must show an ID card. But Spring is stubborn: it only accepts ONE kind
// of ID card, and it has NO idea how to find your users in Postgres. This class
// teaches it both things.

// 1) UserDetails — the official ID card.
// Spring's ID cards always look the same: a name, a password, and a list of
// badges (roles) like ROLE_ADMIN or ROLE_SELLER. That's all "UserDetails" means:
// the shape Spring wants.
// Your User entity is the messy data in your spreadsheet. Spring won't accept a
// spreadsheet — so this class TRANSLATES the spreadsheet row into the official
// ID card format.

// 2) UserDetailsService — the "person-finder".
// Spring has one question it asks when someone logs in: "give me the ID card for
// this username?" That's the single method loadUserByUsername(...). Any class
// that can answer that question can plug into Spring.
//   - Before: a hardcoded list stuck on the wall answered it (InMemoryUserDetailsManager).
//   - Now: YOUR class answers by asking Postgres.
// Writing "implements UserDetailsService" is how you say "I'll handle the
// who-is-this-user question from now on."

// 3) Why does UserRepository have no @Repository annotation? — the magic.
// You never wrote the code for findByUsername. No implementation exists anywhere.
// Spring Data's auto-magic works like this: the MOMENT it sees any interface that
// says "extends JpaRepository", it secretly writes all the boring database code,
// creates the object, and registers it so other classes can use it. Declaring the
// interface IS the signal. No sticker needed.

// 4) Why does "new Exception(...)" make the compiler angry?
// Java has a rule: if your code can cause a "checked" (serious) problem, your
// method must ADVERTISE it with "throws ...". Like a delivery truck that must
// display a "heavy load" sign on its side.
//   - "new Exception(...)" is the serious type, but your method has no sign
//     ("throws Exception"), so the compiler screams.
//   - "UsernameNotFoundException" is the no-sign-needed type, and Spring already
//     knows it means "this user doesn't exist". So it compiles happily:
//         .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username))

// 5) What is .build()?
// ".withUsername(...)" ".password(...)" ".authorities(...)" is the BUILDER —
// like ordering a sandwich step by step: "bun? no onion. extra cheese. add bacon."
// ".build()" is the moment you say "MAKE IT!" — it assembles everything you
// picked and hands you the finished, ready-to-use ID card.

// ALL TOGETHER:
// "I am the person-finder. Someone gives me a username -> I ask Postgres for that
// user -> I check their badges -> I hand Spring back a proper ID card it can trust."

//=============================================================================
// THE LAMBDA SYNTAX: () -> new Exception("User not found")
//=============================================================================

// This little arrow thing is a LAMBDA — a short way to write a "do this later"
// instruction without creating a whole class for it.

// Step 1: what does orElseThrow WANT?
// It does NOT want an exception. It wants a small box (a Supplier) that CAN
// PRODUCE an exception, in case the user is missing. Java demands: "give me
// something that will hand me an exception when I ask for it."
// Why? Because creating the exception right away would be wasteful if the user
// IS found (9 times out of 10 nothing is thrown). So Java creates the exception
// ONLY when it's actually needed — lazily, at the last second.

// Step 2: what does the arrow mean?
//     () -> new Exception("User not found")
// The left side "()"  = "I take NO inputs" (empty parentheses).
// The right side       = "what to produce when asked".
// So read it as: "when you ask me for an exception, I will hand you a NEW
// Exception with the message 'User not found'."
// You can think of it like a vending machine button: you press it only when you
// actually want a snack (here, only when the user is missing).

// Step 3: why not just write "orElseThrow(new Exception(...))"?
// Because that would create the exception IMMEDIATELY, every single time the
// method runs — even when the user exists and nothing is wrong. That's wasted
// work. The lambda defers it: build the exception ONLY in the failure case.
//Here's the simple, non-lambda way — just check and throw manually:
//User user = userRepository.findByUserName(username);
//
//if (user == null) {
//    throw new UsernameNotFoundException("User not found: " + username);
//}

// Step 4: the long way vs the short way.
//     // Long, traditional way (a separate little class):
//     .orElseThrow(new Supplier<Exception>() {
//         public Exception get() {
//             return new Exception("User not found");
//         }
//     });
//
//     // Short lambda way (same thing, much less typing):
//     .orElseThrow(() -> new Exception("User not found"));
//
// The lambda is just sugar for the long version. Java wrote the Supplier class
// for you, so you only type the arrow part.

// Step 5: WHY it is "()" not "(username)" here.
// The Supplier's single method get() takes no parameters — so the lambda gets
// empty parentheses "()". Compare with orElseThrow(() -> ...) which has no input.
// If the interface method took a parameter, you'd write e.g. (x) -> x * 2.


//---------------------------------------------------------------------------------------------
//What this class does — in easy language:
//It's the phone book of your app for login.
//Before your change: when someone tried to log in, Spring Security looked up users in a hardcoded list in memory (admin / password, user / 1234).
//Now: when someone tries to log in, Spring Security asks this class: "Who is this username?" This class goes to Postgres, finds that user, and hands it back to Spring in the official format Spring understands (name + password + roles).
//The 3 lines inside do this:
//1.
//findByUserName(username) → asks Postgres for the user
//2.
//.orElseThrow(...) → if nobody has that name, shout "User not found" (no match = no login)
//3.
//.withUsername(...).password(...).authorities(...) → repackage the DB data as the ID card Spring Security wants
//Bottom line: your old InMemoryUserDetailsManager is deleted. This class is its replacement — but backed by the real database.