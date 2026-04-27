package com.elearning.ProjetPfe.security;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre de sanitisation XSS.
 * Enveloppe chaque requête dans XssRequestWrapper pour nettoyer
 * toutes les entrées utilisateur avant qu'elles n'atteignent les controllers.
 *
 * S'applique à toutes les requêtes sauf les fichiers uploadés
 * (multipart/form-data : déjà limité par FileStorageService).
 */
@Component
@Order(2)
public class XssSanitizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String contentType = request.getContentType();
        boolean isMultipart = contentType != null && contentType.startsWith("multipart/");

        // Ne pas sanitiser les endpoints d'auth ni le webhook Stripe (signature doit rester intacte)
        String uri = request.getRequestURI();
        boolean isAuth = uri.startsWith("/api/auth/");
        boolean isStripeWebhook = uri.equals("/api/payment/webhook");

        if (isMultipart || isAuth || isStripeWebhook) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(new XssRequestWrapper(request), response);
        }
    }
}
