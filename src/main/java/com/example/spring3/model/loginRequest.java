package com.example.spring3.model;

//public class LoginRequest {
//}

public record loginRequest(String id, String pass) {

}

//A record is a Java 16+ feature for defining immutable data carriers concisely. You declare the fields in the header, and the compiler generates the constructor, equals()/hashCode(), toString(), and accessor methods for you:
//public record Point(int x, int y) {}
//Equivalent to ~30 lines of boilerplate in a normal class. Fields are final, you can add custom constructors and methods inside the body, and records can't extend classes.

//Records are classes, just with a different (restricted) syntax. You can't write class in the header, but a record compiles to an ordinary final class. So:
//        •
//public record Point(int x, int y) {} is a class header, not public class Point.
//        •
//You can still use it as a type, implement interfaces, be nested, etc.
//        •
//It's implicitly final and can't extend other classes.

//write the boiler code for understanding