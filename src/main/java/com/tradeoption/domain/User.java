package com.tradeoption.domain;

import java.io.Serializable;
import java.util.UUID;

public class User implements Serializable {
    private String id;
    private String username;
    private String password; // Hashed
    private String email;
    private String role; // ROLE_USER, ROLE_ADMIN

    public User() {
        this.id = UUID.randomUUID().toString();
        this.role = "ROLE_USER";
    }

    public User(String username, String password, String email) {
        this();
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
