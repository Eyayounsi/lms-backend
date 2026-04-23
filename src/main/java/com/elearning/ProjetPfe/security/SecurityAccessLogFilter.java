package com.elearning.ProjetPfe.security;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre de log d'accès sécurité.
 *
 * Logue chaque requête HTTP avec :
 *  - ID unique de requête (MDC + header X-Request-ID)
 *  - Méthode, URI, IP, statut HTTP, durée
 *  - Utilisateur authentifié (email si JWT valide)
 *  - Attention particulière sur les 401, 403, 429, 5xx
 */
@Component
@Order(3)
public class SecurityAccessLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("security.access");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long startMs = System.currentTimeMillis();

        // Injecte le request ID dans le MDC pour traçabilité dans tous les logs
        MDC.put("requestId", requestId);
        response.setHeader("X-Request-ID", requestId);

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startMs;
            int status   = response.getStatus();
            String user  = resolveUser();
            String ip    = extractIp(request);

            String msg = "[ACCESS] {} {} {} -> {} ({}ms) user={} ip={}";

            if (status >= 500) {
                log.error(msg, request.getMethod(), request.getRequestURI(),
                          request.getProtocol(), status, elapsed, user, ip);
            } else if (status == 429) {
                log.warn("[RATE-LIMIT] {} {} ip={} user={}", request.getMethod(),
                         request.getRequestURI(), ip, user);
            } else if (status == 401 || status == 403 || status == 423) {
                log.warn("[SECURITY] {} {} -> {} ip={} user={}",
                         request.getMethod(), request.getRequestURI(), status, ip, user);
            } else {
                log.info(msg, request.getMethod(), request.getRequestURI(),
                         request.getProtocol(), status, elapsed, user, ip);
            }

            MDC.clear();
        }
    }

    /** Récupère l'email de l'utilisateur authentifié si disponible. */
    private String resolveUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String name) {
            return name;
        }
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "anonymous";
    }

    /** IP réelle en tenant compte d'un proxy. */
    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
