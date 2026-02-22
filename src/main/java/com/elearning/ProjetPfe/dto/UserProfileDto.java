package com.elearning.ProjetPfe.dto;

/**
 * DTO en lecture seule pour retourner les données de profil.
 * On ne retourne JAMAIS le mot de passe hashé au frontend.
 */
public class UserProfileDto {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String accountStatus;

    // Constructeur
    public UserProfileDto() {}

    public UserProfileDto(Long id, String fullName, String email, String phone,
                          String role, String accountStatus) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.accountStatus = accountStatus;
    }

    // Getters et Setters
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
}
