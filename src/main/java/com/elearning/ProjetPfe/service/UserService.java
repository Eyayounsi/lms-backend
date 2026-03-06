package com.elearning.ProjetPfe.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.AddRoleDto;
import com.elearning.ProjetPfe.dto.AuthResponseDto;
import com.elearning.ProjetPfe.dto.ChangePasswordDto;
import com.elearning.ProjetPfe.dto.ForgotPasswordDto;
import com.elearning.ProjetPfe.dto.LoginDto;
import com.elearning.ProjetPfe.dto.RegisterDto;
import com.elearning.ProjetPfe.dto.ResetPasswordDto;
import com.elearning.ProjetPfe.dto.UpdateProfileDto;
import com.elearning.ProjetPfe.dto.UserProfileDto;
import com.elearning.ProjetPfe.dto.VerifyOtpDto;
import com.elearning.ProjetPfe.entity.AccountStatus;
import com.elearning.ProjetPfe.entity.Role;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.CartItemRepository;
import com.elearning.ProjetPfe.repository.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.LessonProgressRepository;
import com.elearning.ProjetPfe.repository.ReviewRepository;
import com.elearning.ProjetPfe.repository.UserRepository;
import com.elearning.ProjetPfe.repository.WishlistItemRepository;
import com.elearning.ProjetPfe.security.JwtService;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    // 1. INSCRIPTION
    @Transactional
    public AuthResponseDto register(RegisterDto request) {
        // Vérifier si l'email existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // Définir le rôle par défaut
        String roleStr = request.getRole() != null ? request.getRole().toUpperCase() : "STUDENT";

        // Sécurité : interdire l'inscription en tant que SUPERADMIN via l'API publique
        if ("SUPERADMIN".equals(roleStr)) {
            throw new RuntimeException("Le rôle SUPERADMIN ne peut pas être attribué lors de l'inscription");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide. Utilisez STUDENT, INSTRUCTOR, ADMIN ou RECRUITER");
        }

        // Créer l'utilisateur avec tous les champs du script SQL
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(false);
        // Les comptes RECRUITER créés par l'admin doivent changer leur mot de passe dès la première connexion
        if (role == Role.RECRUITER) {
            user.setFirstLogin(true);
        }

        user = userRepository.save(user);

        // Générer le token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        String token = jwtService.generateToken(claims, user);

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setEmailVerified(user.getEmailVerified());
        response.setFirstLogin(user.getFirstLogin() != null && user.getFirstLogin());
        response.setMessage("Inscription réussie");

        return response;
    }

    // 2. CONNEXION
    @Transactional
    public AuthResponseDto login(LoginDto request) {
        // Vérifier d'abord si le compte existe et est accessible
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier le statut AVANT d'authentifier (message clair pour l'utilisateur)
        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new RuntimeException("Votre compte a été bloqué par l'administrateur. Contactez le support.");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Votre compte n'est pas actif. Contactez l'administrateur.");
        }

        // Authentifier (vérification du mot de passe)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Générer le token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        String token = jwtService.generateToken(claims, user);

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setEmailVerified(user.getEmailVerified());
        response.setFirstLogin(user.getFirstLogin() != null && user.getFirstLogin());
        response.setMessage("Connexion réussie");

        return response;
    }
    // 3. MOT DE PASSE OUBLIÉ - ENVOYER OTP
    @Transactional
    public void forgotPassword(ForgotPasswordDto request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email non trouvé"));

        // Générer un code OTP à 6 chiffres
        String otpCode = String.format("%06d", new Random().nextInt(999999));

        // Sauvegarder l'OTP et son expiration (10 minutes)
        user.setOtpCode(otpCode);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Envoyer l'OTP par email
        emailService.sendOtpEmail(user.getEmail(), otpCode);

        System.out.println("==========================================");
        System.out.println("CODE OTP ENVOYÉ: " + otpCode);
        System.out.println("Email: " + user.getEmail());
        System.out.println("==========================================");
    }

    // 4. VÉRIFIER OTP ET RÉINITIALISER MOT DE PASSE
    @Transactional
    public void verifyOtpAndResetPassword(VerifyOtpDto request) {
        // Vérifier si l'email existe avec ce code OTP
        User user = userRepository.findByEmailAndOtpCode(request.getEmail(), request.getOtpCode())
                .orElseThrow(() -> new RuntimeException("Code OTP invalide pour cet email"));

        // Vérifier si l'OTP a expiré
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le code OTP a expiré. Veuillez demander un nouveau code.");
        }

        // Réinitialiser le mot de passe
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Effacer l'OTP utilisé
        user.setOtpCode(null);
        user.setOtpExpiry(null);

        userRepository.save(user);
    }

    // N'OUBLIE PAS D'AJOUTER L'AUTOWIRED POUR EMAILSERVICE
    @Autowired
    private EmailService emailService;



    // 4. RÉINITIALISER MOT DE PASSE
    @Transactional
    public void resetPassword(ResetPasswordDto request) {
        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (user.getResetTokenExpiry() == null ||
                user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expiré");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    // ─── GESTION DE PROFIL ────────────────────────────────────────────────────

    // 5a. GET PROFIL : retourner les données de l'utilisateur connecté
    public UserProfileDto getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserProfileDto dto = new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getAccountStatus().name()
        );
        // Champs étendus
        dto.setAvatarPath(user.getAvatarPath());
        dto.setBio(user.getBio());
        dto.setAboutMe(user.getAboutMe());
        dto.setDesignation(user.getDesignation());
        dto.setAddress(user.getAddress());
        dto.setFacebookUrl(user.getFacebookUrl());
        dto.setInstagramUrl(user.getInstagramUrl());
        dto.setTwitterUrl(user.getTwitterUrl());
        dto.setYoutubeUrl(user.getYoutubeUrl());
        dto.setLinkedinUrl(user.getLinkedinUrl());
        dto.setEducationJson(user.getEducationJson());
        dto.setExperienceJson(user.getExperienceJson());
        dto.setShareWithRecruiters(user.getShareWithRecruiters());
        return dto;
    }

    // 5b. UPDATE PROFIL : modifier nom, téléphone, email
    @Transactional
    public UserProfileDto updateProfile(String currentEmail, UpdateProfileDto request) {
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Mettre à jour fullName si fourni
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        // Mettre à jour phone si fourni
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        // Mettre à jour email si fourni et différent
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && !request.getEmail().equalsIgnoreCase(currentEmail)) {
            // Vérifier que le nouvel email n'est pas déjà utilisé
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Cet email est déjà utilisé par un autre compte");
            }
            user.setEmail(request.getEmail());
        }

        // Champs étendus
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getAboutMe() != null) user.setAboutMe(request.getAboutMe());
        if (request.getDesignation() != null) user.setDesignation(request.getDesignation());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getFacebookUrl() != null) user.setFacebookUrl(request.getFacebookUrl());
        if (request.getInstagramUrl() != null) user.setInstagramUrl(request.getInstagramUrl());
        if (request.getTwitterUrl() != null) user.setTwitterUrl(request.getTwitterUrl());
        if (request.getYoutubeUrl() != null) user.setYoutubeUrl(request.getYoutubeUrl());
        if (request.getLinkedinUrl() != null) user.setLinkedinUrl(request.getLinkedinUrl());
        if (request.getEducationJson() != null) user.setEducationJson(request.getEducationJson());
        if (request.getExperienceJson() != null) user.setExperienceJson(request.getExperienceJson());

        user = userRepository.save(user);

        return getProfile(user.getEmail());
    }

    // 5c. CHANGER MOT DE PASSE : vérifier l'ancien avant d'accepter le nouveau
    @Transactional
    public void changePassword(String email, ChangePasswordDto request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que l'ancien mot de passe est correct
        // passwordEncoder.matches() compare le mot de passe en clair avec le hash en base
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        // Vérifier que le nouveau mot de passe est différent de l'ancien
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Le nouveau mot de passe ne peut pas être identique à l'ancien");
        }

        // Encoder et sauvegarder le nouveau mot de passe
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Si c'était la première connexion d'un RECRUITER, marquer comme complet
        if (Boolean.TRUE.equals(user.getFirstLogin())) {
            user.setFirstLogin(false);
        }
        userRepository.save(user);
    }

    // 5e. PARTAGE PROFIL AVEC RECRUTEURS
    @Transactional
    public void updateShareProfile(String email, boolean share) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setShareWithRecruiters(share);
        userRepository.save(user);
    }

    // 5d. SUPPRIMER COMPTE : demande confirmation du mot de passe pour la sécurité
    @Transactional
    public void deleteAccount(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier le mot de passe avant suppression (sécurité critique)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect. Suppression refusée.");
        }

        Long userId = user.getId();

        // Supprimer toutes les données liées avant de supprimer l'utilisateur
        // (éviter les FK constraint violations)
        lessonProgressRepository.deleteByStudentId(userId);
        courseProgressRepository.deleteByStudentId(userId);
        enrollmentRepository.deleteByStudentId(userId);
        cartItemRepository.deleteByStudentId(userId);
        wishlistItemRepository.deleteByStudentId(userId);
        reviewRepository.deleteByStudentId(userId);

        userRepository.delete(user);
    }

    // ─────────────────────────────────────────────────────────────────────────

    // 5. AJOUTER LE RÔLE INSTRUCTEUR À UN ÉTUDIANT (SEULEMENT STUDENT → STUDENT + INSTRUCTOR)
    @Transactional
    public AuthResponseDto addUserRole(AddRoleDto request) {
        // Vérifier les identifiants
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        // Récupérer l'utilisateur
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier le statut du compte
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Compte non actif. Statut: " + user.getAccountStatus());
        }

        // Vérifier si l'utilisateur est bien un étudiant
        if (user.getRole() != Role.STUDENT) {
            throw new RuntimeException("Seuls les étudiants peuvent devenir instructeurs");
        }

        // Mettre à jour le rôle vers INSTRUCTOR
        user.setRole(Role.INSTRUCTOR);
        user = userRepository.save(user);

        // Générer un nouveau token avec le nouveau rôle
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());
        String token = jwtService.generateToken(claims, user);

        // Construire la réponse
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setEmailVerified(user.getEmailVerified());
        response.setMessage("Félicitations! Vous êtes maintenant instructeur. Rôle actuel: " + user.getRole().name());

        return response;
    }
}