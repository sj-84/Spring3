package com.example.ecom1.model;

import jakarta.persistence.*;

@Entity //explain - @Entity tells JPA "this Java class maps to a table."
@Table(name = "users") //@Table(name = "users") — user is a reserved word in Postgres, so we pluralize it.
public class User {
    @Id //explain
    @GeneratedValue(strategy = GenerationType.IDENTITY) //explain - DB auto-increments the id.DB auto-increments the id.
    private Long id;

    @Column(unique = true, nullable = false) //explain
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) //explain
    @Column(nullable = false)
    private Role role;

    private String email; //why no annotation?

    public User() { //constructor - no use - The empty constructor User() is required by JPA (Hibernate needs it internally).

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}

//=============================================================================
// EXPLANATION OF ALL THE ANNOTATIONS — THE FRIENDLY WAY
//=============================================================================

// BIG PICTURE:
// JPA = a translator between Java classes and database tables. It reads the
// annotations on this class and decides how to build the "users" table.

// 1) @Entity — "make me a table."
// Tells JPA: "this Java class represents one row in the database." Without it,
// JPA ignores the class completely. Each instance of User (new User()) later
// becomes a ROW in the table.

// 2) @Table(name = "users") — "call the table 'users'."
// By default JPA would name the table "User" (same as the class). But "user" is
// a RESERVED word in Postgres (it has special meaning to the database). So we
// rename the table to "users" to avoid a syntax error.

// 3) @Id — "this column is the row's identity card number."
// Every table needs a primary key — a unique number that identifies each row,
// like a serial number. Marking a field @Id tells JPA "this is the primary key."
// No two rows may have the same id.

// 4) @GeneratedValue(strategy = GenerationType.IDENTITY) — "the DB invents the id."
// Says: "don't ask Java to pick the id — let the database auto-increment it."
// Postgres creates a sequence; with each new row it takes the next number
// (1, 2, 3...). You never set the id yourself; you leave it null and the DB
// fills it in on save. IDENTITY = "the database does the counting."

// 5) @Column(unique = true, nullable = false) — "this column must be filled and never repeated."
//   - nullable = false  -> the column may NOT be empty. Saving a User without a
//                          username throws an error at the database level.
//   - unique = true     -> no two rows may have the same username. Trying to
//                          insert "alice" twice violates this constraint.
// These are SAFETY NETS at the database level. They protect you even if someone
// forgets to check in Java code (like our existsByUserName check in register).

// 6) @Enumerated(EnumType.STRING) — "store the role as its text name, not a number."
// A Java enum (Role.CUSTOMER) can be stored two ways in a database:
//   - ORDINAL (default): saves the POSITION number — CUSTOMER=0, SELLER=1, ADMIN=2.
//     Dangerous: if you ever add a new role in the middle, all existing numbers shift
//     and suddenly everyone's role changes!
//   - STRING: saves the NAME — "CUSTOMER", "SELLER", "ADMIN". Safe and readable
//     in DBeaver. This is what we chose. ALWAYS use EnumType.STRING.

// 7) private String email; — "why does email have no annotation?"
// Because no annotation = "use all the DEFAULTS". Email is allowed to be empty
// (no nullable = false) and can repeat (no unique = true). The defaults are
// exactly what we want here, so nothing is written. To make email required you
// would add: @Column(nullable = false)
// Remember: annotations only CHANGE the defaults — no annotation means "leave
// the defaults as they are".

// 8) public User() { } — "why is there an empty constructor with no use?"
// JPA needs it INTERNALLY. When JPA reads a row from the database, it does not
// call your setter-heavy logic — it first creates an empty User with this
// constructor (JPA can call private no-arg constructors, but public is standard),
// then fills the fields in directly. Your own code never calls it — it's purely
// for JPA. That's why "no use" — but it MUST exist, or JPA refuses to work.

// ONE MORE THING — what does a row actually look like?
// Your Java field  "username"  becomes a table column "username".
// Your Java field  "role"     becomes a column "role" (stored as a text string
//                             like 'SELLER', thanks to EnumType.STRING).
// The getters/setters (getUsername, setUsername, ...) are just the normal Java
// way to read/write these values from your code — JPA uses the fields to build
// the table and the getters/setters don't change that mapping.
