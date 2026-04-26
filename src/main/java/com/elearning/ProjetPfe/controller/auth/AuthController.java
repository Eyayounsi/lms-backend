package com.elearning.ProjetPfe.controller.auth;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.auth.AddRoleDto;
import com.elearning.ProjetPfe.dto.auth.AuthResponseDto;
import com.elearning.ProjetPfe.dto.auth.FaceLoginDto;
import com.elearning.ProjetPfe.dto.auth.FaceRegisterDto;
import com.elearning.ProjetPfe.dto.auth.ForgotPasswordDto;
import com.elearning.ProjetPfe.dto.auth.GoogleLoginDto;
import com.elearning.ProjetPfe.dto.auth.LoginDto;
import com.elearning.ProjetPfe.dto.auth.RegisterDto;
import com.elearning.ProjetPfe.dto.auth.SwitchRoleDto;
import com.elearning.ProjetPfe.dto.auth.VerifyOtpDto;
import com.elearning.ProjetPfe.dto.auth.VerifyRegisterOtpDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.service.auth.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterDto request) {
        return ResponseEntity.ok(userService.register(request));
    }

    @PostMapping("/request-register-otp")
    public ResponseEntity<?> requestRegisterOtp(@Valid @RequestBody RegisterDto request) {
        try {
            userService.requestRegisterOtp(request);
            return ResponseEntity.ok(Map.of("message", "Code OTP envoyé à " + request.getEmail()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-register-otp")
    public ResponseEntity<?> verifyRegisterOtp(@Valid @RequestBody VerifyRegisterOtpDto dto) {
        try {
            return ResponseEntity.ok(userService.verifyRegisterOtp(dto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordDto request) {
        userService.forgotPassword(request);
        return ResponseEntity.ok("Token de réinitialisation généré. Vérifiez la console!");
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtpAndResetPassword(@Valid @RequestBody VerifyOtpDto request) {
        userService.verifyOtpAndResetPassword(request);
        return ResponseEntity.ok("Mot de passe réinitialisé avec succès");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API d'authentification fonctionne!");
    }

    @PostMapping("/add-role")
    public ResponseEntity<AuthResponseDto> addRole(@Valid @RequestBody AddRoleDto request) {
        return ResponseEntity.ok(userService.addUserRole(request));
    }

    @PostMapping("/switch-role")
    public ResponseEntity<AuthResponseDto> switchRole(
            @Valid @RequestBody SwitchRoleDto request,
            @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.switchRole(request, user));
    }

    @PostMapping("/google-login")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginDto request) {
        try {
            return ResponseEntity.ok(userService.loginWithGoogle(request.getIdToken()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/face-register")
    public ResponseEntity<?> faceRegister(@Valid @RequestBody FaceRegisterDto request) {
        try {
            return ResponseEntity.ok(userService.registerWithFace(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/face-login")
    public ResponseEntity<?> faceLogin(@Valid @RequestBody FaceLoginDto request) {
        try {
            return ResponseEntity.ok(userService.loginWithFace(request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Déconnexion réussie. Veuillez supprimer le token du côté client.");
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@org.springframework.web.bind.annotation.RequestParam String email) {
        boolean exists = userService.emailExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ✅ Profil utilisateur courant — utile pour vérifier le rôle côté client
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getId());
        userData.put("email", user.getEmail());
        userData.put("fullName", user.getFullName());
        userData.put("role", user.getRole().name());
        userData.put("accountStatus", user.getAccountStatus().name());
        return ResponseEntity.ok(userData);
    }
}
