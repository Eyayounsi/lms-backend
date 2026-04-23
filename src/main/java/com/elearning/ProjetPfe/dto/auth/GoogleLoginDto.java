package com.elearning.ProjetPfe.dto.auth;

import jakarta.validation.constraints.NotBlank;

public class GoogleLoginDto {

    @NotBlank(message = "Le token Google est requis")
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
