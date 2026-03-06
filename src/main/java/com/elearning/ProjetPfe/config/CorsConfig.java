package com.elearning.ProjetPfe.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Récupère les origines autorisées depuis application.properties
    // Valeur par défaut: http://localhost:4200
    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")  // Applique CORS aux routes /api/
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);

        // CORS pour les fichiers statiques (PDF, vidéos, images uploadées)
        registry.addMapping("/uploads/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}