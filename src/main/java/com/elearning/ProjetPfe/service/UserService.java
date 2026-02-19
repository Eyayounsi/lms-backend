package com.elearning.ProjetPfe.service;

import com.elearning.ProjetPfe.dto.*;
import com.elearning.ProjetPfe.entity.AccountStatus;
import com.elearning.ProjetPfe.entity.Role;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;
import com.elearning.ProjetPfe.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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
        user.addRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(false);

        user = userRepository.save(user);

        // Générer le token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("roles", user.getRoles().stream().map(Role::name).collect(Collectors.toList()));
        claims.put("email", user.getEmail());
        String token = jwtService.generateToken(claims, user);

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRoles().toString());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setEmailVerified(user.getEmailVerified());
        response.setMessage("Inscription réussie");

        return response;
    }

    // 2. CONNEXION
    @Transactional
    public AuthResponseDto login(LoginDto request) {
        // Authentifier
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Récupérer l'utilisateur
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        // Vérifier si le compte est actif
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Votre compte n'est pas actif. Statut: " + user.getAccountStatus());
        }

        // Générer le token
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("roles", user.getRoles().stream().map(Role::name).collect(Collectors.toList()));
        claims.put("email", user.getEmail());
        String token = jwtService.generateToken(claims, user);

        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRoles().toString());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setEmailVerified(user.getEmailVerified());
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
        if (!user.hasRole(Role.STUDENT)) {
            throw new RuntimeException("Seuls les étudiants peuvent devenir instructeurs");
        }

        // Vérifier si l'utilisateur a déjà le rôle INSTRUCTOR
        if (user.hasRole(Role.INSTRUCTOR)) {
            throw new RuntimeException("Vous êtes déjà instructeur");
        }

        // Ajouter le rôle INSTRUCTOR (le rôle STUDENT est conservé)
        user.addRole(Role.INSTRUCTOR);
        user = userRepository.save(user);

        // Générer un nouveau token avec les deux rôles
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("roles", user.getRoles().stream().map(Role::name).collect(Collectors.toList()));
        claims.put("email", user.getEmail());
        String token = jwtService.generateToken(claims, user);

        // Construire la réponse
        AuthResponseDto response = new AuthResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setRole(user.getRoles().toString());
        response.setAccountStatus(user.getAccountStatus().name());
        response.setEmailVerified(user.getEmailVerified());
        response.setMessage("Félicitations! Vous êtes maintenant étudiant et instructeur. Rôles actuels: " + user.getRoles());

        return response;
    }
}