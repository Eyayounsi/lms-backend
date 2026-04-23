package com.elearning.ProjetPfe.dto.chatbot;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiAgentRequest {

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 3000, message = "Le message ne doit pas dépasser 3000 caractères")
    private String message;

    @Size(max = 120, message = "userId ne doit pas dépasser 120 caractères")
    private String userId;

    @Size(max = 120, message = "sessionId ne doit pas dépasser 120 caractères")
    private String sessionId;

    @Size(max = 120, message = "courseId ne doit pas dépasser 120 caractères")
    private String courseId;

    private Map<String, Object> context;

    // Conversation history: [{role: "user"|"assistant", content: "..."}]
    private List<Map<String, String>> history;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public List<Map<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, String>> history) {
        this.history = history;
    }
}
