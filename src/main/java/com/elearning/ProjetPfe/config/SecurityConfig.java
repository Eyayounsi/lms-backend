package com.elearning.ProjetPfe.config;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.security.JwtFilter;
import com.elearning.ProjetPfe.security.RateLimitFilter;
import com.elearning.ProjetPfe.security.SecurityAccessLogFilter;
import com.elearning.ProjetPfe.security.XssSanitizationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private SecurityAccessLogFilter accessLogFilter;

    @Autowired
    private XssSanitizationFilter xssSanitizationFilter;

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
                // CSRF désactivé : API REST stateless JWT (pas de session serveur)
                .csrf(csrf -> csrf.disable())
                // ── Security Headers ──────────────────────────────────────────
                .headers(headers -> headers
                        // X-Frame-Options désactivé uniquement pour l'affichage des PDF en iframe
                        .frameOptions(opts -> opts.disable())
                        // Empêche le sniffing MIME (ex: upload de fichier déguisé)
                        .contentTypeOptions(c -> {})
                        // Force HTTPS pendant 1 an (en prod derrière nginx TLS)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)
                                .includeSubDomains(true)
                                .preload(true))
                        // Politique de référent : pas d'info sensible dans les URL
                        .referrerPolicy(r -> r
                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Content Security Policy : limite les origines des scripts/styles/images
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                    "default-src 'self'; " +
                                    "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://accounts.google.com; " +
                                    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com https://cdn.jsdelivr.net; " +
                                    "font-src 'self' https://fonts.gstatic.com https://cdn.jsdelivr.net; " +
                                    "img-src 'self' data: blob: https:; " +
                                    "connect-src 'self' https://api.stripe.com https://api-inference.huggingface.co; " +
                                    "frame-src 'self' blob:; " +
                                    "object-src 'none'; " +
                                    "base-uri 'self'"
                                ))
                        // Permissions-Policy : désactive les fonctionnalités non nécessaires
                        .permissionsPolicy(p -> p
                                .policy("geolocation=(), microphone=(), payment=(), usb=()")))
                // ─────────────────────────────────────────────────────────────


                .authorizeHttpRequests(auth -> auth
                        // 🔓 Preflight CORS (OPTIONS) : toujours autoriser
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 🔓 Swagger UI — documentation API
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // 🔓 Endpoints publics (authentification)
                        .requestMatchers("/api/auth/**").permitAll()
                        // 🔓 /api/public/** → catégories, avis, liste cours, profil instructor (sans token)
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/public/chatbot/**").permitAll()
                        // 🔒 AI chatbot par rôle métier
                        .requestMatchers("/api/ai/chatbot/instructor/**").hasAuthority("INSTRUCTOR")
                        .requestMatchers("/api/ai/chatbot/student/**").hasAnyAuthority("STUDENT", "INSTRUCTOR", "ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/ai/chatbot/admin/**").hasAnyAuthority("ADMIN", "SUPERADMIN")
                        // 🔓 Handshake WebSocket notifications/messagerie (auth gérée par l'intercepteur JWT WebSocket)
                        .requestMatchers("/ws/**", "/ws/messages", "/ws/messages/**").permitAll()
                        // 🔓 Liste des cours publiés — accessible sans connexion
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/{id}", "/api/courses/featured", "/api/courses/search").permitAll()
                        // 🔓 Vérification publique d'un certificat (sans token)
                        .requestMatchers(HttpMethod.GET, "/api/courses/certificates/verify/**").permitAll()
                        // 🔓 Fichiers uploadés (images, vidéos) — accessibles publiquement
                        .requestMatchers("/uploads/**").permitAll()
                        // 🔓 Webhook Stripe — appelé par Stripe (pas de JWT)
                        .requestMatchers(HttpMethod.POST, "/api/payment/webhook").permitAll()
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
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(xssSanitizationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(accessLogFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "Accept", "X-Requested-With", "Origin"));
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