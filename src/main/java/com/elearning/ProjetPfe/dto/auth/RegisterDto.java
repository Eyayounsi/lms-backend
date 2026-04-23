package com.elearning.ProjetPfe.dto.auth;

import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.communication.Message;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterDto {

    @NotBlank(message = "Nom complet requis")
    @Size(min = 3, max = 150, message = "Le nom doit contenir entre 3 et 150 caractères")
    private String fullName;

    @NotBlank(message = "Email requis")
    @Email(message = "Email invalide")
    @Size(max = 150, message = "Email trop long")
    private String email;

    @NotBlank(message = "Mot de passe requis")
    @Size(min = 6, message = "6 caractères minimum")
    private String password;

    @Pattern(regexp = "^[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    @Size(max = 30, message = "Téléphone trop long")
    private String phone;

    private String role; // STUDENT, INSTRUCTOR, ADMIN, RECRUITER

    // Constructeurs
    public RegisterDto() {}

    public RegisterDto(String fullName, String email, String password, String phone, String role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
    }

    // Getters et Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}