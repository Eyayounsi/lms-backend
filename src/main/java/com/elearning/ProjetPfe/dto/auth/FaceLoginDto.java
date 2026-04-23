package com.elearning.ProjetPfe.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class FaceLoginDto {

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le token est requis")
    private String token;

    @NotBlank(message = "Le timestamp est requis")
    private String timestamp;

    public String getEmail()     { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken()     { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
