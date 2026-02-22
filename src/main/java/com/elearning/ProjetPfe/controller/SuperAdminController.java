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
 * Controller réservé au SUPERADMIN.
 * Peut gérer TOUS les utilisateurs, y compris les ADMINs.
 */
@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminController {

    @Autowired
    private UserRepository userRepository;

    // ─── STATISTIQUES ────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getSuperStats() {
        return ResponseEntity.ok(buildStats(userRepository.findAll()));
    }

    // ─── GET TOUS LES UTILISATEURS (inclus les admins, sauf soi-même) ────────
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getAllUsersForSuperAdmin(
            @AuthenticationPrincipal User currentUser) {

        List<AdminUserDto> users = userRepository.findAll()
                .stream()
                .filter(u -> !u.getEmail().equals(currentUser.getEmail()))
                .map(u -> new AdminUserDto(
                        u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                        u.getRole().name(), u.getAccountStatus().name(), u.getCreatedAt()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    // ─── BLOQUER / DÉBLOQUER (peut cibler les admins) ─────────────────────────
    @PutMapping("/users/{id}/toggle-block")
    public ResponseEntity<AdminUserDto> superToggleBlock(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (target.getEmail().equals(currentUser.getEmail())) {
            return ResponseEntity.badRequest().build();
        }
        if (target.getRole() == Role.SUPERADMIN) {
            return ResponseEntity.status(403).build();
        }

        if (target.getAccountStatus() == AccountStatus.BLOCKED) {
            target.setAccountStatus(AccountStatus.ACTIVE);
            target.setEnabled(true);
        } else {
            target.setAccountStatus(AccountStatus.BLOCKED);
            target.setEnabled(false);
        }
        userRepository.save(target);
        return ResponseEntity.ok(toDto(target));
    }

    // ─── CHANGER LE RÔLE (peut promouvoir en ADMIN) ───────────────────────────
    @PutMapping("/users/{id}/change-role")
    public ResponseEntity<AdminUserDto> superChangeRole(
            @PathVariable Long id,
            @RequestParam String role,
            @AuthenticationPrincipal User currentUser) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (target.getEmail().equals(currentUser.getEmail())) {
            return ResponseEntity.badRequest().build();
        }
        if (target.getRole() == Role.SUPERADMIN) {
            return ResponseEntity.status(403).build();
        }
        try {
            target.setRole(Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        userRepository.save(target);
        return ResponseEntity.ok(toDto(target));
    }

    // ─── SUPPRIMER UN UTILISATEUR ─────────────────────────────────────────────
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> superDeleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (target.getEmail().equals(currentUser.getEmail())) {
            return ResponseEntity.badRequest().body("Vous ne pouvez pas supprimer votre propre compte.");
        }
        if (target.getRole() == Role.SUPERADMIN) {
            return ResponseEntity.status(403).body("Impossible de supprimer un Super-Administrateur.");
        }
        userRepository.delete(target);
        return ResponseEntity.ok("Utilisateur supprimé avec succès.");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private AdminUserDto toDto(User u) {
        return new AdminUserDto(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole().name(), u.getAccountStatus().name(), u.getCreatedAt());
    }

    private DashboardStatsDto buildStats(List<User> allUsers) {
        DashboardStatsDto stats = new DashboardStatsDto();
        stats.setTotalUsers(allUsers.size());
        stats.setActiveUsers(allUsers.stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.ACTIVE).count());
        stats.setBlockedUsers(allUsers.stream()
                .filter(u -> u.getAccountStatus() == AccountStatus.BLOCKED).count());

        Map<String, Long> byRole = new LinkedHashMap<>();
        for (Role r : Role.values()) {
            byRole.put(r.name(), allUsers.stream().filter(u -> u.getRole() == r).count());
        }
        stats.setUsersByRole(byRole);

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
}
