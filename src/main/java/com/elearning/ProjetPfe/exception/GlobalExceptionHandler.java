package com.elearning.ProjetPfe.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gestionnaire global des erreurs applicatives.
 *
 * - Validation DTO (@Valid) → 400 avec détail des champs en erreur
 * - IllegalArgumentException → 400
 * - SecurityException       → 403
 * - Toute autre exception   → 500 (message générique, pas de stack trace exposée)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Identifiants incorrects (Spring Security AuthenticationManager) */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[BAD_CREDENTIALS] tentative de connexion invalide");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody(
            HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect.", null
        ));
    }

    /** Compte bloqué par l'admin (AccountStatus.BLOCKED) */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<Map<String, Object>> handleLocked(LockedException ex) {
        log.warn("[LOCKED_ACCOUNT] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(
            HttpStatus.BAD_REQUEST, "Votre compte a été bloqué par l'administrateur.", null
        ));
    }

    /** RuntimeException métier (authentification, accès ressource, règle métier) */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        log.warn("[RUNTIME] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(
            HttpStatus.BAD_REQUEST, ex.getMessage(), null
        ));
    }

    /** Corps JSON illisible ou mal formé (@RequestBody ne peut pas être désérialisé) */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("[UNREADABLE_BODY] {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errorBody(
            HttpStatus.BAD_REQUEST, "Corps de requête invalide ou vide.", null
        ));
    }

    /** Erreurs de validation Jakarta Bean Validation (@NotBlank, @Size, @Email…) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + " : " + fe.getDefaultMessage())
                .toList();

        log.warn("[VALIDATION] Erreurs sur {}: {}", ex.getObjectName(), errors);

        return ResponseEntity.badRequest().body(errorBody(
            HttpStatus.BAD_REQUEST, "Données invalides", errors
        ));
    }

    /** Arguments métier incorrects */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegal(IllegalArgumentException ex) {
        log.warn("[ILLEGAL_ARG] {}", ex.getMessage());
        return ResponseEntity.badRequest().body(errorBody(
            HttpStatus.BAD_REQUEST, ex.getMessage(), null
        ));
    }

    /** Accès refusé métier (non Spring Security) */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurity(SecurityException ex) {
        log.warn("[SECURITY] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorBody(
            HttpStatus.FORBIDDEN, "Accès refusé", null
        ));
    }

    /** Catch-all : ne jamais exposer la stack trace au client */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        log.error("[UNHANDLED_ERROR] {}: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Une erreur interne s'est produite. Contactez le support si le problème persiste.",
            null
        ));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Map<String, Object> errorBody(HttpStatus status, String message, List<String> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (details != null && !details.isEmpty()) {
            body.put("details", details);
        }
        return body;
    }
}
