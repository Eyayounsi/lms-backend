package com.elearning.ProjetPfe.dto;

public class AuthResponseDto {
    private String token;
    private Long id;
    private String email;
    private String fullName;
    private String role;
    private String accountStatus;
    private Boolean emailVerified;
    private Boolean firstLogin;
    private String message;

    // Constructeurs
    public AuthResponseDto() {}

    public AuthResponseDto(String token, Long id, String email, String fullName,
                           String role, String accountStatus, Boolean emailVerified, String message) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.accountStatus = accountStatus;
        this.emailVerified = emailVerified;
        this.message = message;
    }

    // Getters et Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public Boolean getFirstLogin() { return firstLogin; }
    public void setFirstLogin(Boolean firstLogin) { this.firstLogin = firstLogin; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}