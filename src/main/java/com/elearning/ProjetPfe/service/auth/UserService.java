package com.elearning.ProjetPfe.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.admin.AdminUserDto;
import com.elearning.ProjetPfe.dto.admin.CreateManagedUserDto;
import com.elearning.ProjetPfe.dto.auth.AddRoleDto;
import com.elearning.ProjetPfe.dto.auth.AuthResponseDto;
import com.elearning.ProjetPfe.dto.auth.ChangePasswordDto;
import com.elearning.ProjetPfe.dto.auth.FaceLoginDto;
import com.elearning.ProjetPfe.dto.auth.FaceRegisterDto;
import com.elearning.ProjetPfe.dto.auth.ForgotPasswordDto;
import com.elearning.ProjetPfe.dto.auth.LoginDto;
import com.elearning.ProjetPfe.dto.auth.RegisterDto;
import com.elearning.ProjetPfe.dto.auth.ResetPasswordDto;
import com.elearning.ProjetPfe.dto.auth.SwitchRoleDto;
import com.elearning.ProjetPfe.dto.auth.UpdateProfileDto;
import com.elearning.ProjetPfe.dto.auth.UserProfileDto;
import com.elearning.ProjetPfe.dto.auth.VerifyOtpDto;
import com.elearning.ProjetPfe.entity.auth.AccountStatus;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.repository.engagement.ReviewRepository;
import com.elearning.ProjetPfe.repository.learning.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.learning.LessonProgressRepository;
import com.elearning.ProjetPfe.repository.payment.CartItemRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.payment.WishlistItemRepository;
import com.elearning.ProjetPfe.security.JwtService;
import com.elearning.ProjetPfe.service.communication.EmailService;
import com.elearning.ProjetPfe.service.mongo.MongoAuditService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Value("${google.client.id}")
    private String googleClientId;

    @Value("${face.service.secret}")
    private String faceServiceSecret;

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

    @Autowired
    private MongoAuditService mongoAuditService;

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

        mongoAuditService.logAuthEvent("REGISTER", user.getEmail(), true, user.getRole().name(), "REGISTER_OK");

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
        // Inclure les rôles disponibles pour ce compte (échangeables via /auth/switch-role)
        java.util.List<String> availableRoles = new java.util.ArrayList<>();
        availableRoles.add(user.getRole().name());
        user.getSecondaryRoles().forEach(r -> availableRoles.add(r.name()));
        // Règle plateforme : un INSTRUCTOR a toujours accès implicite à STUDENT
        if (user.getRole() == Role.INSTRUCTOR && !availableRoles.contains(Role.STUDENT.name())) {
            availableRoles.add(Role.STUDENT.name());
        }
        response.setSecondaryRoles(availableRoles);

        mongoAuditService.logAuthEvent("PASSWORD", user.getEmail(), true, user.getRole().name(), "LOGIN_OK");

        return response;
    }

    // 3. CONNEXION GOOGLE OAUTH2
    @Transactional
    public AuthResponseDto loginWithGoogle(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
            ).setAudience(Collections.singletonList(googleClientId)).build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                throw new RuntimeException("Token Google invalide ou expiré");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            String fullName = (String) payload.get("name");
            if (fullName == null || fullName.isBlank()) {
                fullName = email;
            }

            final String finalFullName = fullName;
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFullName(finalFullName);
                // Mot de passe aléatoire — l'utilisateur se connecte uniquement via Google
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                newUser.setRole(Role.STUDENT);
                newUser.setAccountStatus(AccountStatus.ACTIVE);
                newUser.setEmailVerified(true);
                return userRepository.save(newUser);
            });

            if (user.getAccountStatus() == AccountStatus.BLOCKED) {
                throw new RuntimeException("Votre compte a été bloqué par l'administrateur. Contactez le support.");
            }
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new RuntimeException("Votre compte n'est pas actif. Contactez l'administrateur.");
            }

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
            response.setFirstLogin(false);
            response.setMessage("Connexion Google réussie");

            mongoAuditService.logAuthEvent("GOOGLE", user.getEmail(), true, user.getRole().name(), "LOGIN_OK");

            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la vérification du token Google: " + e.getMessage());
        }
    }

    // 4. INSCRIPTION FACE ID
    @Transactional
    public AuthResponseDto registerWithFace(FaceRegisterDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email déjà utilisé. Essayez de vous connecter avec Face ID.");
        }

        String roleStr = request.getRole() != null ? request.getRole().toUpperCase() : "STUDENT";
        if ("SUPERADMIN".equals(roleStr)) {
            throw new RuntimeException("Le rôle SUPERADMIN ne peut pas être attribué lors de l'inscription");
        }

        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Rôle invalide. Utilisez STUDENT, INSTRUCTOR, ADMIN ou RECRUITER");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        // Random non-usable password — this account authenticates only via Face ID
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        AuthResponseDto response = new AuthResponseDto();
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRole().name());
        response.setMessage("Compte créé avec succès. Vous pouvez maintenant vous connecter avec votre visage.");
        mongoAuditService.logAuthEvent("FACE_REGISTER", user.getEmail(), true, user.getRole().name(), "REGISTER_OK");
        return response;
    }

    // 5. CONNEXION FACE ID
    @Transactional
    public AuthResponseDto loginWithFace(FaceLoginDto request) {
        // Anti-replay: reject tokens older than 60 seconds
        try {
            long tokenTime = Long.parseLong(request.getTimestamp());
            long now = System.currentTimeMillis() / 1000L;
            if (Math.abs(now - tokenTime) > 60) {
                throw new RuntimeException("Token Face ID expiré. Veuillez réessayer.");
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException("Format de timestamp invalide.");
        }

        // Verify HMAC signature from Python service
        String expected = computeFaceHmac(request.getEmail(), request.getTimestamp());
        if (!expected.equalsIgnoreCase(request.getToken())) {
            throw new RuntimeException("Token Face ID invalide.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Compte non trouvé. Inscrivez-vous d'abord avec Face ID."));

        if (user.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new RuntimeException("Votre compte a été bloqué par l'administrateur. Contactez le support.");
        }
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Votre compte n'est pas actif.");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("id",    user.getId());
        claims.put("role",  user.getRole().name());
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
        response.setFirstLogin(false);
        response.setMessage("Connexion Face ID réussie. Bienvenue " + user.getFullName() + "!");
        mongoAuditService.logAuthEvent("FACE", user.getEmail(), true, user.getRole().name(), "LOGIN_OK");
        return response;
    }

    private String computeFaceHmac(String email, String timestamp) {
        try {
            String message = email + ":" + timestamp;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(faceServiceSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de vérification HMAC: " + e.getMessage());
        }
    }

    // 6. MOT DE PASSE OUBLIÉ - ENVOYER OTP
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
        dto.setChallengePoints(user.getChallengePoints());
        return dto;
    }

    // 5b. UPDATE PROFIL : modifier nom, téléphone, email
    @Transactional
    public UserProfileDto updateProfile(String currentEmail, UpdateProfileDto request) {
        log.info("[PROFILE] updateProfile called for user={} | fullName='{}' email='{}' phone='{}'",
                currentEmail,
                request.getFullName(),
                request.getEmail(),
                request.getPhone());

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // ── Validation explicite fullName ──
        String newName = request.getFullName();
        if (newName != null) {
            String trimmed = newName.trim();
            if (trimmed.isEmpty()) {
                throw new IllegalArgumentException("Le nom complet ne peut pas être vide.");
            }
            if (trimmed.length() > 150) {
                throw new IllegalArgumentException("Le nom complet ne doit pas dépasser 150 caractères.");
            }
            user.setFullName(trimmed);
        }

        // ── Mettre à jour phone si fourni ──
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (phone.length() > 30) {
                throw new IllegalArgumentException("Le numéro de téléphone ne doit pas dépasser 30 caractères.");
            }
            user.setPhone(phone.isEmpty() ? null : phone);
        }

        // ── Mettre à jour email si fourni et réellement différent ──
        // On compare après normalisation (lowercase + trim) pour être robuste
        // face à tout encodage intermédiaire (XSS filter, etc.)
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            String storedEmail = currentEmail.trim().toLowerCase();
            if (!newEmail.equals(storedEmail)) {
                // Validation basique : doit contenir exactement un @
                int atIdx = newEmail.indexOf('@');
                if (atIdx <= 0 || atIdx != newEmail.lastIndexOf('@') || atIdx >= newEmail.length() - 3) {
                    throw new IllegalArgumentException("Format d'email invalide.");
                }
                if (userRepository.existsByEmail(newEmail)) {
                    throw new RuntimeException("Cet email est déjà utilisé par un autre compte");
                }
                user.setEmail(newEmail);
            }
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
        log.info("[PROFILE] Profile updated successfully for user={}", user.getEmail());

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

    // 5. AJOUTER UN RÔLE SECONDAIRE (STUDENT uniquement)
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

        Role newRole = Role.valueOf(request.getNewRole());
        if (newRole != Role.STUDENT) {
            throw new RuntimeException("Seul le rôle STUDENT peut être ajouté");
        }

        // Vérifier que le rôle demandé n'est pas déjà actif (principal ou secondaire)
        if (user.hasRole(newRole)) {
            throw new RuntimeException("Vous possédez déjà le rôle " + newRole.name());
        }

        // Ajouter le rôle secondaire — le rôle principal reste inchangé
        user.getSecondaryRoles().add(newRole);
        user = userRepository.save(user);

        // Émettre un JWT avec le nouveau rôle comme rôle actif
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("role", newRole.name());   // rôle actif = nouveau rôle
        claims.put("email", user.getEmail());
        String token = jwtService.generateToken(claims, user);

        // Construire la réponse
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(newRole.name());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setEmailVerified(user.getEmailVerified());
        response.setSecondaryRoles(
            user.getSecondaryRoles().stream().map(Role::name).collect(Collectors.toList())
        );
        response.setMessage("Rôle " + newRole.name() + " ajouté avec succès. Rôle actif: " + newRole.name());

        mongoAuditService.logAuthEvent(
                "ROLE_ADD",
                user.getEmail(),
                true,
                newRole.name(),
                "ROLE_ADD_OK from=" + user.getRole().name() + " to=" + newRole.name()
        );

        return response;
    }

    // 6. CHANGER DE RÔLE ACTIF (émet un nouveau JWT avec le rôle cible)
    @Transactional(readOnly = true)
    public AuthResponseDto switchRole(SwitchRoleDto request, User currentUser) {
        Role targetRole = Role.valueOf(request.getTargetRole());
        String sourceRole = request.getSourceRole() == null || request.getSourceRole().isBlank()
                ? "UNKNOWN"
                : request.getSourceRole();

        // ✅ SÉCURITÉ: Vérifier le mot de passe de l'utilisateur (couche sécurité additionnelle)
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            mongoAuditService.logAuthEvent(
                    "ROLE_SWITCH",
                    currentUser.getEmail(),
                    false,
                    targetRole.name(),
                    "ROLE_SWITCH_DENIED from=" + sourceRole + " to=" + targetRole.name() + " (password missing)"
            );
            throw new RuntimeException("Mot de passe requis pour changer de rôle");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(currentUser.getEmail(), request.getPassword())
            );
        } catch (Exception e) {
            mongoAuditService.logAuthEvent(
                    "ROLE_SWITCH",
                    currentUser.getEmail(),
                    false,
                    targetRole.name(),
                    "ROLE_SWITCH_DENIED from=" + sourceRole + " to=" + targetRole.name() + " (invalid password)"
            );
            throw new RuntimeException("Mot de passe incorrect");
        }

        // Valider que l'utilisateur possède ce rôle (principal, secondaire, ou accès implicite)
        // hasRole() gère déjà le cas INSTRUCTOR → STUDENT implicite
        if (!currentUser.hasRole(targetRole)) {
            mongoAuditService.logAuthEvent(
                    "ROLE_SWITCH",
                    currentUser.getEmail(),
                    false,
                    targetRole.name(),
                    "ROLE_SWITCH_DENIED from=" + sourceRole + " to=" + targetRole.name()
            );
            throw new RuntimeException(
                "Vous ne possédez pas le rôle " + targetRole.name() +
                ". Utilisez /api/auth/add-role pour l'ajouter à votre compte."
            );
        }

        // Émettre un nouveau JWT avec le rôle actif = targetRole
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", currentUser.getId());
        claims.put("role", targetRole.name());
        claims.put("email", currentUser.getEmail());
        String token = jwtService.generateToken(claims, currentUser);

        // Calculer la liste complète des rôles disponibles pour ce compte
        List<String> allRoles = new java.util.ArrayList<>();
        allRoles.add(currentUser.getRole().name());
        currentUser.getSecondaryRoles().forEach(r -> allRoles.add(r.name()));
        // Ajouter STUDENT implicitement si l'utilisateur est INSTRUCTOR (règle plateforme)
        if (currentUser.getRole() == Role.INSTRUCTOR && !allRoles.contains(Role.STUDENT.name())) {
            allRoles.add(Role.STUDENT.name());
        }

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(currentUser.getId());
        response.setEmail(currentUser.getEmail());
        response.setFullName(currentUser.getFullName());
        response.setRole(targetRole.name());
        response.setAccountStatus(currentUser.getAccountStatus().name());
        response.setEmailVerified(currentUser.getEmailVerified());
        response.setSecondaryRoles(allRoles);
        response.setMessage("Rôle actif changé vers: " + targetRole.name());

        mongoAuditService.logAuthEvent(
            "ROLE_SWITCH",
            currentUser.getEmail(),
            true,
            targetRole.name(),
            "ROLE_SWITCH_OK from=" + sourceRole + " to=" + targetRole.name()
        );

        return response;
    }

    // ─── CRÉATION DE COMPTE GÉRÉ (Admin → Recruiter, SuperAdmin → Admin/Recruiter) ────
    @Transactional
    public AdminUserDto createManagedUser(CreateManagedUserDto request, Role role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé par un autre compte.");
        }

        // Mot de passe : utilise celui fourni si non vide, sinon génère automatiquement
        boolean isAutoPassword = (request.getPassword() == null || request.getPassword().isBlank());
        String plainPassword = isAutoPassword ? generateTempPassword() : request.getPassword();

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setCompanyName(request.getCompanyName());
        user.setPassword(passwordEncoder.encode(plainPassword));
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEnabled(true);
        user.setFirstLogin(true); // Force le changement de mot de passe à la première connexion
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        // ─── Email d'accueil avec identifiants ───────────────────────────
        String roleLabel = (role == Role.ADMIN) ? "Administrateur" : "Recruteur";
        String passwordTypeLabel = isAutoPassword
                ? "Mot de passe temporaire (généré automatiquement)"
                : "Mot de passe (défini par l'administrateur)";
        String companyLine = (request.getCompanyName() != null && !request.getCompanyName().isBlank())
                ? "  Société    : " + request.getCompanyName() + "\n"
                : "";
        String subject = "Activation de votre compte " + roleLabel + " — Identifiants de connexion";
        String body = "Bonjour " + request.getFullName() + ",\n\n"
                + "Un compte " + roleLabel + " a été créé pour vous sur la plateforme LMS.\n\n"
                + "══════════════════════════════════════\n"
                + "  Vos identifiants de connexion\n"
                + "══════════════════════════════════════\n"
                + "  Email      : " + request.getEmail() + "\n"
                + companyLine
                + "  " + passwordTypeLabel + " : " + plainPassword + "\n"
                + "══════════════════════════════════════\n\n"
                + "  Lien de connexion : http://localhost:4200/auth/login\n\n"
                + "⚠  IMPORTANT — Pour activer votre compte, vous devez\n"
                + "   OBLIGATOIREMENT changer votre mot de passe lors\n"
                + "   de votre première connexion.\n\n"
                + "Cordialement,\nL'équipe LMS";
        emailService.sendEmail(request.getEmail(), subject, body);

        AdminUserDto dto = new AdminUserDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getCompanyName(),
                user.getRole().name(),
                user.getAccountStatus().name(),
                user.getCreatedAt()
        );
        dto.setAvatarPath(user.getAvatarPath());
        return dto;
    }

    private String generateTempPassword() {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specials = "@#!$";
        String all = upper + lower + digits + specials;
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        // Garantir au moins un caractère de chaque catégorie
        sb.append(upper.charAt(rnd.nextInt(upper.length())));
        sb.append(lower.charAt(rnd.nextInt(lower.length())));
        sb.append(digits.charAt(rnd.nextInt(digits.length())));
        sb.append(specials.charAt(rnd.nextInt(specials.length())));
        // Compléter jusqu'à 10 caractères
        for (int i = 4; i < 10; i++) {
            sb.append(all.charAt(rnd.nextInt(all.length())));
        }
        // Mélanger
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            char tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        }
        return new String(arr);
    }
}