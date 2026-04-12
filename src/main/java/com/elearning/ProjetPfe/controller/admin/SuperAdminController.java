package com.elearning.ProjetPfe.controller.admin;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.admin.AdminUserDto;
import com.elearning.ProjetPfe.dto.admin.CreateManagedUserDto;
import com.elearning.ProjetPfe.dto.admin.DashboardStatsDto;
import com.elearning.ProjetPfe.entity.auth.AccountStatus;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.auth.UserService;

/**
 * Controller réservé au SUPERADMIN.
 * Peut gérer TOUS les utilisateurs, y compris les ADMINs.
 */
@RestController
@RequestMapping("/api/superadmin")
public class SuperAdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

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
                .map(this::toDto)
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
        AdminUserDto dto = new AdminUserDto(
                u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getCompanyName(), u.getRole().name(),
                u.getAccountStatus().name(), u.getCreatedAt());
        dto.setAvatarPath(u.getAvatarPath());
        // Construire la liste des rôles secondaires effectifs
        java.util.List<String> secRoles = new java.util.ArrayList<>();
        if (u.getSecondaryRoles() != null) {
            u.getSecondaryRoles().stream().map(Enum::name).forEach(secRoles::add);
        }
        // Règle implicite : INSTRUCTOR inclut toujours STUDENT (comme switchRole)
        if (u.getRole() == com.elearning.ProjetPfe.entity.auth.Role.INSTRUCTOR
                && !secRoles.contains("STUDENT")) {
            secRoles.add("STUDENT");
        }
        if (!secRoles.isEmpty()) {
            dto.setSecondaryRoles(secRoles);
        }
        return dto;
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

    // ─── CRÉER UN COMPTE (ADMIN ou RECRUITER) ──────────────────────────
    // POST /api/superadmin/users/create-account
    // Le champ "role" du body doit être "ADMIN" ou "RECRUITER"
    @PostMapping("/users/create-account")
    public ResponseEntity<?> createAccount(@RequestBody CreateManagedUserDto request) {
        String roleStr = request.getRole() == null ? "" : request.getRole().toUpperCase();
        if (!roleStr.equals("ADMIN") && !roleStr.equals("RECRUITER")) {
            return ResponseEntity.badRequest().body("Le rôle doit être ADMIN ou RECRUITER.");
        }
        try {
            Role role = Role.valueOf(roleStr);
            AdminUserDto created = userService.createManagedUser(request, role);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── AJOUTER UN RÔLE SECONDAIRE ────────────────────────────────────────
    @PostMapping("/users/{id}/secondary-roles/{role}")
    public ResponseEntity<AdminUserDto> addSecondaryRole(
            @PathVariable Long id,
            @PathVariable String role,
            @AuthenticationPrincipal User currentUser) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (target.getRole() == Role.SUPERADMIN) {
            return ResponseEntity.status(403).build();
        }
        try {
            Role r = Role.valueOf(role.toUpperCase());
            if (r == target.getRole()) {
                return ResponseEntity.badRequest().build();
            }
            target.getSecondaryRoles().add(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        userRepository.save(target);
        return ResponseEntity.ok(toDto(target));
    }

    // ─── RETIRER UN RÔLE SECONDAIRE ────────────────────────────────────────
    @DeleteMapping("/users/{id}/secondary-roles/{role}")
    public ResponseEntity<AdminUserDto> removeSecondaryRole(
            @PathVariable Long id,
            @PathVariable String role,
            @AuthenticationPrincipal User currentUser) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        try {
            Role r = Role.valueOf(role.toUpperCase());
            target.getSecondaryRoles().remove(r);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        userRepository.save(target);
        return ResponseEntity.ok(toDto(target));
    }
}
