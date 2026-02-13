package com.elearning.ProjetPfe.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordDto {

    @NotBlank(message = "Token requis")
    private String token;

    @NotBlank(message = "Nouveau mot de passe requis")
    @Size(min = 6, message = "6 caractères minimum")
    private String newPassword;

    // Constructeurs
    public ResetPasswordDto() {}

    public ResetPasswordDto(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    // Getters et Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}