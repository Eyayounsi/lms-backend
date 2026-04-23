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
 * Controller réservé aux administrateurs.
 * Accès protégé par SecurityConfig : seul le rôle ADMIN peut appeler /api/admin/**
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

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
                .filter(u -> !u.getEmail().equals(currentAdmin.getEmail()))
                .map(u -> {
                    AdminUserDto dto = new AdminUserDto(
                            u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                            u.getRole().name(), u.getAccountStatus().name(), u.getCreatedAt()
                    );
                        dto.setAvatarPath(u.getAvatarPath());
                    // Construire la liste des rôles secondaires effectifs
                    java.util.List<String> secRoles = new java.util.ArrayList<>();
                    if (u.getSecondaryRoles() != null) {
                        u.getSecondaryRoles().stream().map(r -> r.name()).forEach(secRoles::add);
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
                })
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
        result.setAvatarPath(target.getAvatarPath());

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

        // Règle métier: seul le passage INSTRUCTOR -> STUDENT est autorisé
        if (target.getRole() != Role.INSTRUCTOR || !"STUDENT".equalsIgnoreCase(role)) {
            return ResponseEntity.badRequest().build();
        }

        target.setRole(Role.STUDENT);
        if (target.getSecondaryRoles() != null) {
            target.getSecondaryRoles().remove(Role.INSTRUCTOR);
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
        result.setAvatarPath(target.getAvatarPath());

        return ResponseEntity.ok(result);
    }

    // ─── CRÉER UN RECRUTEUR ───────────────────────────────────────
    // POST /api/admin/users/create-recruiter
    @PostMapping("/users/create-recruiter")
    public ResponseEntity<?> createRecruiter(@RequestBody CreateManagedUserDto request) {
        try {
            AdminUserDto created = userService.createManagedUser(request, com.elearning.ProjetPfe.entity.auth.Role.RECRUITER);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
