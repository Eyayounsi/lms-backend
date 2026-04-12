package com.elearning.ProjetPfe.dto.chatbot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatbotMessageRequest {

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 1200, message = "Le message ne doit pas dépasser 1200 caractères")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
