package com.elearning.ProjetPfe.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FaceRegisterDto {

    @NotBlank(message = "Le nom complet est requis")
    private String fullName;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    private String role;

    /** Mot de passe optionnel — si fourni, le compte pourra aussi se connecter avec mot de passe */
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
