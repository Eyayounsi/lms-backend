package com.elearning.ProjetPfe.dto.admin;

import com.elearning.ProjetPfe.entity.auth.Role;
/**
 * DTO pour la création d'un compte géré (Admin crée Recruiter, SuperAdmin crée Admin ou Recruiter).
 */
public class CreateManagedUserDto {

    private String fullName;
    private String email;
    private String phone;
    private String companyName;
    /** Si null ou vide → mot de passe temporaire généré automatiquement */
    private String password;
    private String role; // "ADMIN" ou "RECRUITER" — utilisé seulement par SuperAdmin

    public CreateManagedUserDto() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
