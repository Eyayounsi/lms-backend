package com.elearning.ProjetPfe.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.elearning.ProjetPfe.repository.UserRepository;
import com.elearning.ProjetPfe.security.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // ⚠️ IMPORTANT: CSRF doit être désactivé pour les API REST
                .csrf(csrf -> csrf.disable())
                // Désactiver X-Frame-Options pour permettre l'affichage des PDF en iframe
                .headers(headers -> headers.frameOptions(opts -> opts.disable()))


                .authorizeHttpRequests(auth -> auth
                        // 🔓 Preflight CORS (OPTIONS) : toujours autoriser
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 🔓 Endpoints publics (authentification)
                        .requestMatchers("/api/auth/**").permitAll()
                        // 🔓 /api/public/** → catégories, avis, liste cours, profil instructor (sans token)
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        // 🔓 Liste des cours publiés — accessible sans connexion
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/{id}", "/api/courses/featured", "/api/courses/search").permitAll()
                        // 🔓 Vérification publique d'un certificat (sans token)
                        .requestMatchers(HttpMethod.GET, "/api/courses/certificates/verify/**").permitAll()
                        // 🔓 Fichiers uploadés (images, vidéos) — accessibles publiquement
                        .requestMatchers("/uploads/**").permitAll()
                        // 🔓 Webhook Stripe — appelé par Stripe (pas de JWT)
                        .requestMatchers(HttpMethod.POST, "/api/payment/webhook").permitAll()
                        // 🔓 Confirmation paiement après retour Stripe (pas de JWT nécessaire)
                        .requestMatchers(HttpMethod.POST, "/api/payment/confirm").permitAll()
                        // 🔒 Endpoints réservés au super-admin
                        .requestMatchers("/api/superadmin/**").hasAuthority("SUPERADMIN")
                        // 🔒 Endpoints réservés à l'admin
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")
                        // 🔒 Endpoints réservés à l'instructor
                        .requestMatchers("/api/instructor/**").hasAuthority("INSTRUCTOR")
                        // 🔒 Endpoints réservés au recruteur
                        .requestMatchers("/api/recruiter/**").hasAuthority("RECRUITER")
                        // 🔒 Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // 401 si non authentifié (token manquant / expiré)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                "{\"error\":\"Non authentifi\u00e9\",\"message\":\"Token manquant ou expir\u00e9. Veuillez vous reconnecter.\"}"
                            );
                        })
                        // 403 si authentifié mais mauvais rôle
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                "{\"error\":\"Acc\u00e8s refus\u00e9\",\"message\":\"Vous n'avez pas les droits requis pour cette ressource.\"}"
                            );
                        })
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}