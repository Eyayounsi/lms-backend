package com.elearning.ProjetPfe.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AddRoleDto {

    @NotBlank(message = "Email requis")
    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Mot de passe requis")
    private String password;

    @NotBlank(message = "Rôle à ajouter requis")
    @Pattern(regexp = "INSTRUCTOR", message = "Vous pouvez seulement ajouter le rôle INSTRUCTOR")
    private String newRole;

    // Constructeurs
    public AddRoleDto() {}

    public AddRoleDto(String email, String password, String newRole) {
        this.email = email;
        this.password = password;
        this.newRole = newRole;
    }

    // Getters et Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNewRole() { return newRole; }
    public void setNewRole(String newRole) { this.newRole = newRole; }
}