package com.elearning.ProjetPfe.controller;

import com.elearning.ProjetPfe.dto.*;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Déconnexion réussie. Veuillez supprimer le token du côté client.");
    }

    // ✅ Profil utilisateur courant — utile pour vérifier le rôle côté client
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("fullName", user.getFullName());
        userData.put("role", user.getRole().name());
        userData.put("accountStatus", user.getAccountStatus().name());
        return ResponseEntity.ok(userData);
    }
}