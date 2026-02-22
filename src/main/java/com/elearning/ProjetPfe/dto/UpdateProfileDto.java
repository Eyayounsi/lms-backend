package com.elearning.ProjetPfe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * DTO pour la mise à jour du profil utilisateur.
 * L'utilisateur peut modifier : fullName, phone, email.
 * Il ne peut PAS modifier son mot de passe via ce DTO (endpoint séparé).
 */
public class UpdateProfileDto {

    @Size(min = 2, max = 150, message = "Le nom complet doit contenir entre 2 et 150 caractères")
    private String fullName;

    @Email(message = "Format d'email invalide")
    @Size(max = 150)
    private String email;

    @Size(max = 30, message = "Le numéro de téléphone ne doit pas dépasser 30 caractères")
    private String phone;

    // Getters et Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
