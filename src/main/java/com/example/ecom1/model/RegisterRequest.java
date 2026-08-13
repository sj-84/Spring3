package com.example.ecom1.model;

public record RegisterRequest(String username, String password, String email, Role role) {
}
