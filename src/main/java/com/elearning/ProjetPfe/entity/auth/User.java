package com.elearning.ProjetPfe.entity.auth;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

   @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Column(length = 30)
    private String phone;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Role role;

    /** Rôles secondaires : permet à un seul compte d'avoir plusieurs rôles actifs */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_secondary_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 30)
    @Enumerated(EnumType.STRING)
    private Set<Role> secondaryRoles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "email_verified")
    private Boolean emailVerified = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private LocalDateTime resetTokenExpiry;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Profil étendu ────────────────────────────────────────────────────

    /** Chemin de l'avatar uploadé (ex: /uploads/avatars/uuid.jpg) */
    @Column(name = "avatar_path", length = 500)
    private String avatarPath;

    /** Bio courte (affichée sous le nom) */
    @Column(columnDefinition = "TEXT")
    private String bio;

    /** À propos de moi (texte long) */
    @Column(name = "about_me", columnDefinition = "TEXT")
    private String aboutMe;

    /** Titre professionnel (ex: "Développeur Full-Stack") */
    @Column(length = 150)
    private String designation;

    /** Adresse physique */
    @Column(length = 300)
    private String address;

    // ─── Réseaux sociaux ──────────────────────────────────────────────────
    @Column(name = "facebook_url", length = 300)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 300)
    private String instagramUrl;

    @Column(name = "twitter_url", length = 300)
    private String twitterUrl;

    @Column(name = "youtube_url", length = 300)
    private String youtubeUrl;

    @Column(name = "linkedin_url", length = 300)
    private String linkedinUrl;

    /** Éducation stockée en JSON (ex: [{"degree":"...","university":"...","years":"..."}]) */
    @Column(name = "education_json", columnDefinition = "TEXT")
    private String educationJson;

    /** Expérience stockée en JSON (ex: [{'title':'...','company':'...','years':'...'}]) */
    @Column(name = "experience_json", columnDefinition = "TEXT")
    private String experienceJson;

    // ─── Recruteur & Partage de profil ────────────────────────────────────────

    /** Première connexion (mis à true lors de la création d'un compte RECRUITER par un admin) */
    @Column(name = "first_login")
    private Boolean firstLogin = false;

    /** L'étudiant accepte de partager son profil/certificats avec les recruteurs */
    @Column(name = "share_with_recruiters")
    private Boolean shareWithRecruiters = false;

    /** Nom de la société (pour les recruteurs et admins créés par un gestionnaire) */
    @Column(name = "company_name", length = 200)
    private String companyName;

    /** Points de challenge cumulés par l'étudiant */
    @Column(name = "challenge_points", nullable = false)
    private int challengePoints = 0;

    // Constructeurs
    public User() {}

    public User(String fullName, String email, String password, String phone, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.role = role;
        this.accountStatus = AccountStatus.ACTIVE;
        this.emailVerified = false;
        this.enabled = true;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Set<Role> getSecondaryRoles() { return secondaryRoles; }
    public void setSecondaryRoles(Set<Role> secondaryRoles) { this.secondaryRoles = secondaryRoles; }

    /**
     * Vérifie si l'utilisateur peut exercer un rôle donné.
     * Règle plateforme (style Udemy) :
     *   - INSTRUCTOR ⊇ STUDENT : un instructeur peut toujours basculer en mode étudiant
     *     sans avoir à ajouter le rôle manuellement.
     *   - Sinon : le rôle doit être le rôle principal ou un rôle secondaire explicitement ajouté.
     */
    public boolean hasRole(Role r) {
        if (this.role == r || this.secondaryRoles.contains(r)) {
            return true;
        }
        // INSTRUCTOR inclut implicitement l'accès STUDENT (comme Udemy)
        if (r == Role.STUDENT && this.role == Role.INSTRUCTOR) {
            return true;
        }
        return false;
    }

    public AccountStatus getAccountStatus() { return accountStatus; }
    public void setAccountStatus(AccountStatus accountStatus) { this.accountStatus = accountStatus; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public boolean getEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // ─── Profil étendu ────────────────────────────────────────────────────
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getAboutMe() { return aboutMe; }
    public void setAboutMe(String aboutMe) { this.aboutMe = aboutMe; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public String getTwitterUrl() { return twitterUrl; }
    public void setTwitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; }

    public String getYoutubeUrl() { return youtubeUrl; }
    public void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }

    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }

    public String getEducationJson() { return educationJson; }
    public void setEducationJson(String educationJson) { this.educationJson = educationJson; }

    public String getExperienceJson() { return experienceJson; }
    public void setExperienceJson(String experienceJson) { this.experienceJson = experienceJson; }

    public Boolean getFirstLogin() { return firstLogin; }
    public void setFirstLogin(Boolean firstLogin) { this.firstLogin = firstLogin; }

    public Boolean getShareWithRecruiters() { return shareWithRecruiters; }
    public void setShareWithRecruiters(Boolean shareWithRecruiters) { this.shareWithRecruiters = shareWithRecruiters; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public int getChallengePoints() { return challengePoints; }
    public void setChallengePoints(int challengePoints) { this.challengePoints = challengePoints; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public LocalDateTime getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(LocalDateTime otpExpiry) { this.otpExpiry = otpExpiry; }

    // UserDetails methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // On retourne directement le nom du rôle sans préfixe ROLE_
        // SecurityConfig utilise hasAuthority("ADMIN") qui correspond exactement.
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatus.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return accountStatus == AccountStatus.ACTIVE; }
}