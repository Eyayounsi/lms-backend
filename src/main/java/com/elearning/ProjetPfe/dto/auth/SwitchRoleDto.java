package com.elearning.ProjetPfe.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SwitchRoleDto {

    @NotBlank(message = "Rôle cible requis")
    @Pattern(regexp = "STUDENT|INSTRUCTOR|ADMIN|RECRUITER|SUPERADMIN",
             message = "Valeur de rôle invalide")
    private String targetRole;

    @Pattern(regexp = "|STUDENT|INSTRUCTOR|ADMIN|RECRUITER|SUPERADMIN",
             message = "Valeur de rôle source invalide")
    private String sourceRole;

    // ✅ Nouveau: Mot de passe requis pour sécurité renforcée
    @NotBlank(message = "Mot de passe requis pour changer de rôle")
    private String password;

    public SwitchRoleDto() {}

    public SwitchRoleDto(String targetRole) {
        this.targetRole = targetRole;
    }

    public SwitchRoleDto(String targetRole, String sourceRole, String password) {
        this.targetRole = targetRole;
        this.sourceRole = sourceRole;
        this.password = password;
    }

    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }

    public String getSourceRole() { return sourceRole; }
    public void setSourceRole(String sourceRole) { this.sourceRole = sourceRole; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
