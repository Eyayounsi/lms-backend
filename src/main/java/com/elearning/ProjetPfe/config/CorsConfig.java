package com.elearning.ProjetPfe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Récupère les origines autorisées depuis application.properties
    // Valeur par défaut: http://localhost:4200
    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // Applique CORS uniquement aux routes /api/
                .allowedOrigins(allowedOrigins)  // Origines autorisées (depuis properties)
                .allowedMethods(                   // Méthodes HTTP autorisées
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders(                    // En-têtes autorisés
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
                .exposedHeaders(                     // En-têtes exposés au frontend
                        "Authorization"
                )
                .allowCredentials(true)              // Autorise les cookies/sessions
                .maxAge(3600);                        // Cache CORS pendant 1 heure
    }
}