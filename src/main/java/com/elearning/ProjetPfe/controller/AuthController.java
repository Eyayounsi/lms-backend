package com.elearning.ProjetPfe.controller;

import com.elearning.ProjetPfe.dto.*;
import com.elearning.ProjetPfe.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterDto request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordDto request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok("Token de réinitialisation généré. Vérifiez la console!");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtpAndResetPassword(@Valid @RequestBody VerifyOtpDto request) {
        userService.verifyOtpAndResetPassword(request);
        return ResponseEntity.ok("Mot de passe réinitialisé avec succès");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API d'authentification fonctionne!");
    }

    @PostMapping("/add-role")
    public ResponseEntity<AuthResponseDto> addRole(@Valid @RequestBody AddRoleDto request) {
        return ResponseEntity.ok(userService.addUserRole(request));
    }

    // ✅ NOUVEAU: Méthode Logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // Efface le contexte de sécurité côté serveur
        SecurityContextHolder.clearContext();

        // Retourne un message de confirmation
        return ResponseEntity.ok("Déconnexion réussie. Veuillez supprimer le token du côté client.");
    }
}