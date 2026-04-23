package com.elearning.ProjetPfe.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.elearning.ProjetPfe.entity.auth.AccountStatus;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsernameIfValid(jwt);

            if (userEmail != null) {
                User user = userRepository.findByEmail(userEmail).orElse(null);

                if (user != null && jwtService.isTokenValid(jwt, user)) {

                    // ─── VÉRIFICATION COMPTE BLOQUÉ ───────────────────────────
                    // Si l'admin a bloqué ce compte après la connexion,
                    // on rejette la requête avec HTTP 423 (Locked).
                    // L'interceptor Angular catch ce code et déconnecte l'utilisateur.
                    if (user.getAccountStatus() == AccountStatus.BLOCKED) {
                        response.setStatus(423);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(
                            "{\"blocked\":true,\"message\":\"Votre compte a \u00e9t\u00e9 bloqu\u00e9 par l'administrateur.\"}"
                        );
                        return; // Arrêter ici, ne pas continuer la chaîne
                    }

                    // Compte actif → authentifier normalement
                    if (SecurityContextHolder.getContext().getAuthentication() == null) {
                        // ─── MULTI-RÔLE : utiliser le claim "role" du JWT comme rôle actif ───
                        // Le claim "role" peut différer du rôle principal en base (switchRole).
                        // On valide que ce rôle appartient bien à l'utilisateur avant de l'utiliser.
                        GrantedAuthority activeAuthority = resolveActiveAuthority(jwt, user);

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        user, null, List.of(activeAuthority)
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[JWT] Erreur de validation token : {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Détermine le rôle actif à utiliser pour Spring Security :
     * - Si le JWT contient un claim "role" valide ET que l'utilisateur possède ce rôle
     *   (primaire ou secondaire), on utilise ce rôle actif.
     * - Sinon on retombe sur le rôle principal en base de données.
     */
    private GrantedAuthority resolveActiveAuthority(String jwt, User user) {
        try {
            String activeRoleName = jwtService.extractRole(jwt);
            if (activeRoleName != null) {
                Role activeRole = Role.valueOf(activeRoleName);
                if (user.hasRole(activeRole)) {
                    return new SimpleGrantedAuthority(activeRoleName);
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Valeur de rôle inconnue dans le JWT — fallback sur le rôle principal
        }
        // Fallback : rôle principal stocké en base
        return new SimpleGrantedAuthority(user.getRole().name());
    }
}
