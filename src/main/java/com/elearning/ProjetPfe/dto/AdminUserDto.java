package com.elearning.ProjetPfe.dto;

import java.time.LocalDateTime;

/**
 * DTO utilisé par l'admin pour voir la liste des utilisateurs.
 * Ne contient JAMAIS le mot de passe.
 */
public class AdminUserDto {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String accountStatus;
    private LocalDateTime createdAt;

    public AdminUserDto() {}

    public AdminUserDto(Long id, String fullName, String email, String phone,
                        String role, String accountStatus, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.accountStatus = accountStatus;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
