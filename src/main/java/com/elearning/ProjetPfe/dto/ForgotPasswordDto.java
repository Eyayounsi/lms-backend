package com.elearning.ProjetPfe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordDto {

    @NotBlank(message = "Email requis")
    @Email(message = "Email invalide")
    @Size(max = 150, message = "Email trop long")
    private String email;

    // Constructeurs
    public ForgotPasswordDto() {}

    public ForgotPasswordDto(String email) {
        this.email = email;
    }

    // Getters et Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}