package com.elearning.ProjetPfe.security;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rate Limiting basé sur IP par type d'endpoint.
 *
 * Règles :
 *  /api/auth/**          → 10 req / 1 min  (anti brute-force login/register)
 *  /api/public/chatbot/  → 20 req / 1 min  (anti-spam chatbot visiteur)
 *  /api/public/**        → 60 req / 1 min  (navigation publique)
 *  Tout le reste         → 120 req / 1 min (endpoints authentifiés)
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // Cache IP → Bucket par catégorie (clé = "ip:type")
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip  = extractClientIp(request);
        String uri = request.getRequestURI();

        String bucketKey;
        Bandwidth limit;

        if (uri.startsWith("/api/auth/")) {
            bucketKey = ip + ":auth";
            limit     = Bandwidth.simple(10, Duration.ofMinutes(1));
        } else if (uri.startsWith("/api/public/chatbot/")) {
            bucketKey = ip + ":chatbot";
            limit     = Bandwidth.simple(20, Duration.ofMinutes(1));
        } else if (uri.startsWith("/api/public/")) {
            bucketKey = ip + ":public";
            limit     = Bandwidth.simple(60, Duration.ofMinutes(1));
        } else {
            bucketKey = ip + ":default";
            limit     = Bandwidth.simple(120, Duration.ofMinutes(1));
        }

        Bucket bucket = buckets.computeIfAbsent(bucketKey,
                k -> Bucket.builder().addLimit(limit).build());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("[RATE-LIMIT] IP={} bloquée sur URI={}", ip, uri);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\":\"Trop de requ\u00eates\",\"message\":\"Limite de d\u00e9bit atteinte. R\u00e9essayez dans une minute.\"}"
            );
        }
    }

    /** Extrait l'IP réelle en tenant compte d'un reverse proxy (X-Forwarded-For). */
    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
