package com.example.ecom1.repository;

import com.example.ecom1.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> { //Class 'UserRepository' must either be declared abstract or implement abstract method 'flush()' in 'JpaRepository' - getting this error when type class not interface
    //why Long?
    Optional<User> findByUserName(String userName); //explain - where used?
    boolean existsByUserName(String userName); //explain - where used?
}

//Why an interface? Spring Data generates the implementation at runtime. You just declare findByUsername and it writes SELECT ... WHERE username = ? for you. existsByUsername similarly produces an EXISTS check.

//=============================================================================
// EXPLANATION OF THE DOUBTS
//=============================================================================

// Line 8: public interface UserRepository extends JpaRepository<User, Long>
// "Getting error 'Class UserRepository must either be declared abstract or
//  implement abstract method flush()' when I wrote 'class' instead of 'interface'"
// JpaRepository comes with a long list of already-declared methods: save(),
// findById(), delete(), flush(), and dozens more — but their CODE is not written
// yet (they are abstract, just a signature). 
//   - If you write "interface", that's fine: an interface is allowed to leave
//     methods unfinished — Spring Data fills them in later.
//   - If you write "class", Java demands: a class must implement every method it
//     inherits. You haven't implemented flush() etc., so the compiler complains.
// So the answer is: keep it an "interface". Spring Data writes all the missing
// code automatically at runtime.

// Line 9: why Long?
// JpaRepository<User, Long> means "a repository that manages User rows, and the
// @Id field of User is of type Long." The first generic is the ENTITY, the second
// is the type of that entity's PRIMARY KEY (id). That's why it's Long — because
// User's id field is Long, not Integer or String.

// Line 10: Optional<User> findByUserName(String userName)
// This is a QUERY-BY-METHOD-NAME. Spring Data reads the name and builds SQL from it:
//     findBy    -> "SELECT"
//     UserName  -> "WHERE username = ?"   (the property on the User entity)
// So calling repo.findByUserName("alice") runs:
//     SELECT * FROM users WHERE username = 'alice';
// and returns the matching user (or nothing).
//
// "Optional<User>" is the safe box: it is either FULL (a user was found) or
// EMPTY (no user with that name). Never null. That's why in the service you can
// write .orElseThrow(...) — if the box is empty, throw.
//
// WHERE IT IS USED: in CustomUserDetailsService.loadUserByUsername(...) — during
// LOGIN, Spring Security calls it to fetch the user by their username.

// Line 11: boolean existsByUserName(String userName)
// Same idea, but it only asks "is there at least one row?" and returns true/false.
//     existsBy -> "SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)"
// WHERE IT IS USED: in AuthController.register(...) — before saving a new user we
// call repo.existsByUserName(request.username()) to check the username is not
// already taken. If it returns true, we answer 409 "Username already taken".

// Line 16: where is the implementation of the two methods?
// Nowhere in your code — and that is the whole trick. Spring Data sees the
// method NAME, understands findByUserName / existsByUserName, generates the SQL
// and the Java code behind the scenes, and creates an invisible class (a "proxy")
// that Spring registers as a bean. Your service gets that bean injected and calls
// the methods like normal. The method name IS the implementation.



//-----------------------------------------------------

//what is the difference between existsByUserName and existsByUsername? does the case matter?
//No real difference — Spring Data matches property names case-insensitively.
//Both existsByUserName and existsByUsername resolve to the same entity field username, and both compile. Spring Data splits the name after By, then compares against your User entity's properties using case-insensitive matching, so:
//existsByUserName(String userName)   // same
//existsByUsername(String username)   // same
//→ both generate SELECT EXISTS(... WHERE username = ?).
//The parameter name doesn't matter either — it's just a label; Spring binds by position.
//So does case ever matter? Only in a naming-convention sense: Spring Data splits on uppercase letters (UserName → User + Name), and matching is case-insensitive, so it's forgiving. But best practice is to spell the field name exactly like the entity property (username → findByUsername) — it's clearer, avoids surprises, and reads like the SQL. Use the same spelling consistently in the repository, service, and controller so you don't mix findByUserName/findByUsername across files (right now your code uses ...UserName everywhere, which is fine, but keep it uniform).