package com.elearning.ProjetPfe.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.elearning.ProjetPfe.entity.AccountStatus;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {

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
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        user, null, user.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("JWT Filter Error: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
