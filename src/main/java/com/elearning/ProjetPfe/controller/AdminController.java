package com.elearning.ProjetPfe.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.AdminUserDto;
import com.elearning.ProjetPfe.dto.DashboardStatsDto;
import com.elearning.ProjetPfe.entity.AccountStatus;
import com.elearning.ProjetPfe.entity.Role;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;

/**
 * Controller réservé aux administrateurs.
 * Accès protégé par SecurityConfig : seul le rôle ADMIN peut appeler /api/admin/**
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    // ─── STATISTIQUES TABLEAU DE BORD ────────────────────────────────────────
    // GET /api/admin/stats
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        List<User> allUsers = userRepository.findAll();
        return ResponseEntity.ok(buildStats(allUsers));
    }

    // ─── MÉTHODE UTILITAIRE ───────────────────────────────────────────────────
    private DashboardStatsDto buildStats(List<User> allUsers) {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setTotalUsers(allUsers.size());
        stats.setActiveUsers(allUsers.stream().filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE).count());
        stats.setBlockedUsers(allUsers.stream().filter(u -> u.getAccountStatus() == AccountStatus.BLOCKED).count());

        // Par rôle
        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            long count = allUsers.stream().filter(u -> u.getRole() == r).count();
            byRole.put(r.name(), count);
        }
        stats.setUsersByRole(byRole);

        // Inscriptions par mois (12 derniers mois)
        Map<String, Long> byMonth = new LinkedHashMap<>();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (int i = 11; i >= 0; i--) {
            java.time.LocalDateTime month = now.minusMonths(i);
            String key = String.format("%04d-%02d", month.getYear(), month.getMonthValue());
            long count = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null
                    && u.getCreatedAt().getYear() == month.getYear()
                    && u.getCreatedAt().getMonthValue() == month.getMonthValue())
                .count();
            byMonth.put(key, count);
        }
        stats.setRegistrationsByMonth(byMonth);
        return stats;
    }

    // ─── GET TOUS LES UTILISATEURS ────────────────────────────────────────────
    // GET /api/admin/users
    // Retourne la liste complète des utilisateurs (sans mot de passe)
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsers(
            @AuthenticationPrincipal User currentAdmin) {

        List<AdminUserDto> users = userRepository.findAll()
                .stream()
                // Ne pas retourner l'admin lui-même dans la liste
                .filter(u -> !u.getEmail().equals(currentAdmin.getEmail()))
                .map(u -> new AdminUserDto(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.getPhone(),
                        u.getRole().name(),
                        u.getAccountStatus().name(),
                        u.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    // ─── SUPPRIMER UN UTILISATEUR ─────────────────────────────────────────────
    // DELETE /api/admin/users/{id}
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentAdmin) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Sécurité : l'admin ne peut pas se supprimer lui-même
        if (target.getEmail().equals(currentAdmin.getEmail())) {
            return ResponseEntity.badRequest().body("Vous ne pouvez pas supprimer votre propre compte.");
        }

        // Sécurité : un admin ne peut pas supprimer un autre admin (seul SUPERADMIN peut)
        if (target.getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).body("Un admin ne peut pas supprimer un autre admin. Contactez le SUPERADMIN.");
        }

        userRepository.delete(target);
        return ResponseEntity.ok("Utilisateur supprimé avec succès.");
    }

    // ─── BLOQUER / DÉBLOQUER UN UTILISATEUR ───────────────────────────────────
    // PUT /api/admin/users/{id}/toggle-block
    @PutMapping("/users/{id}/toggle-block")
    public ResponseEntity<AdminUserDto> toggleBlockUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentAdmin) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Sécurité : l'admin ne peut pas se bloquer lui-même
        if (target.getEmail().equals(currentAdmin.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        // Sécurité : un admin ne peut pas bloquer un autre admin
        if (target.getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        // Basculer le statut
        if (target.getAccountStatus() == AccountStatus.BLOCKED) {
            target.setAccountStatus(AccountStatus.ACTIVE);
            target.setEnabled(true);
        } else {
            target.setAccountStatus(AccountStatus.BLOCKED);
            target.setEnabled(false);
        }

        userRepository.save(target);

        AdminUserDto result = new AdminUserDto(
                target.getId(),
                target.getFullName(),
                target.getEmail(),
                target.getPhone(),
                target.getRole().name(),
                target.getAccountStatus().name(),
                target.getCreatedAt()
        );

        return ResponseEntity.ok(result);
    }

    // ─── CHANGER LE RÔLE D'UN UTILISATEUR ────────────────────────────────────
    // PUT /api/admin/users/{id}/change-role?role=STUDENT
    @PutMapping("/users/{id}/change-role")
    public ResponseEntity<AdminUserDto> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @AuthenticationPrincipal User currentAdmin) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (target.getEmail().equals(currentAdmin.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        // Sécurité : un admin ne peut pas changer le rôle d'un autre admin
        if (target.getRole() == Role.ADMIN) {
            return ResponseEntity.status(403).build();
        }

        try {
            target.setRole(Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        userRepository.save(target);

        AdminUserDto result = new AdminUserDto(
                target.getId(),
                target.getFullName(),
                target.getEmail(),
                target.getPhone(),
                target.getRole().name(),
                target.getAccountStatus().name(),
                target.getCreatedAt()
        );

        return ResponseEntity.ok(result);
    }
}
