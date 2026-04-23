package com.elearning.ProjetPfe.mongo.document;

import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "error_logs")
public class ErrorLogDocument {
    @Id
    private String id;
    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();
    private String service;
    private String event;
    private String message;
    private String stacktrace;
    private String details;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getStacktrace() { return stacktrace; }
    public void setStacktrace(String stacktrace) { this.stacktrace = stacktrace; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
