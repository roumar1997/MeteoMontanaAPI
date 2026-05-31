package com.meteomontana.api.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * Decisiones:
 * - CSRF desactivado: somos una API REST stateless, no hay formularios ni cookies de sesión.
 * - Sesiones: STATELESS — cada request se autentica con su propio token, el servidor
 *   no guarda sesión en memoria.
 * - CORS: permitimos el origen de la PWA en local y producción.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;

    public SecurityConfig(FirebaseTokenFilter firebaseTokenFilter) {
        this.firebaseTokenFilter = firebaseTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF off — REST API stateless, no cookies de sesión
            .csrf(AbstractHttpConfigurer::disable)

            // CORS — usará el bean corsConfigurationSource() de abajo
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Sin sesiones HTTP — cada request es independiente
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Reglas de autorización por endpoint
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos de lectura — cualquiera puede llamarlos
                .requestMatchers(HttpMethod.GET, "/api/schools").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/schools/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/health").permitAll()

                // Todo lo demás requiere autenticación (Fases 5+)
                .anyRequest().authenticated()
            )

            // Nuestro filtro se ejecuta ANTES del filtro estándar de usuario/contraseña
            .addFilterBefore(firebaseTokenFilter,
                             UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Orígenes permitidos: PWA local + producción en Cloudflare Pages
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",       // dev local (Vite)
                "http://localhost:3000",       // dev local alternativo
                "http://127.0.0.1:5500",       // Live Server de VSCode
                "https://climbingteams.com"    // producción
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
