package com.elearning.ProjetPfe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.ChangePasswordDto;
import com.elearning.ProjetPfe.dto.UpdateProfileDto;
import com.elearning.ProjetPfe.dto.UserProfileDto;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.service.UserService;

import jakarta.validation.Valid;

/**
 * Controller pour la gestion de profil des utilisateurs connectés.
 *
 * Toutes les routes /api/user/** sont PROTÉGÉES par le JWT Filter.
 * Spring injecte automatiquement l'utilisateur connecté via @AuthenticationPrincipal,
 * grâce au JwtFilter qui stocke l'objet User dans le SecurityContext.
 *
 * SÉCURITÉ : un utilisateur ne peut JAMAIS modifier le profil d'un autre.
 * On identifie toujours l'utilisateur par son email extrait du token JWT,
 * jamais par un ID passé dans l'URL (ce serait une faille IDOR).
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // ─── GET PROFIL ───────────────────────────────────────────────────────────
    // GET /api/user/profile
    // Retourne les données de l'utilisateur actuellement connecté
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(
            @AuthenticationPrincipal User currentUser) {

        UserProfileDto profile = userService.getProfile(currentUser.getEmail());
        return ResponseEntity.ok(profile);
    }

    // ─── UPDATE PROFIL ────────────────────────────────────────────────────────
    // PUT /api/user/profile
    // Modifier nom, téléphone, email
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileDto request) {

        UserProfileDto updated = userService.updateProfile(currentUser.getEmail(), request);
        return ResponseEntity.ok(updated);
    }

    // ─── CHANGER MOT DE PASSE ─────────────────────────────────────────────────
    // PUT /api/user/change-password
    // Exige l'ancien mot de passe pour changer le nouveau
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordDto request) {

        userService.changePassword(currentUser.getEmail(), request);
        return ResponseEntity.ok("Mot de passe modifié avec succès");
    }

    // ─── PING / VÉRIFICATION STATUT ──────────────────────────────────────────
    // GET /api/user/ping
    // Utilisé par le guard Angular pour vérifier si le compte est encore actif.
    // Si le compte est BLOQUÉ, JwtFilter retourne 423 avant d'atteindre ce code.
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    // ─── SUPPRIMER COMPTE ─────────────────────────────────────────────────────
    // DELETE /api/user/account?password=xxx
    // Exige le mot de passe comme confirmation avant suppression définitive
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String password) {

        userService.deleteAccount(currentUser.getEmail(), password);
        return ResponseEntity.ok("Compte supprimé avec succès");
    }
}
