package com.meteomontana.api.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.meteomontana.api.infrastructure.persistence.jpa.SpringDataUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Se ejecuta UNA vez por request (OncePerRequestFilter).
 *
 * Flujo:
 *   1. Lee el header Authorization: Bearer <token>
 *   2. Si no hay token → deja pasar sin autenticar (endpoints públicos seguirán funcionando)
 *   3. Si hay token → lo valida con Firebase Admin SDK
 *   4. Si es válido → crea un FirebaseUser y lo mete en el SecurityContext
 *   5. Si no es válido → limpia el contexto (la request llegará sin usuario)
 */
@Component
public class FirebaseTokenFilter extends OncePerRequestFilter {

    // ObjectProvider = acceso perezoso al repo (evita ciclos en el arranque de
    // seguridad) para comprobar si el usuario está baneado.
    private final ObjectProvider<SpringDataUserRepository> users;

    public FirebaseTokenFilter(ObjectProvider<SpringDataUserRepository> users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Sin header → continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7); // quita "Bearer "

        try {
            // Firebase Admin SDK verifica la firma, expiración y proyecto
            FirebaseToken decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);

            // Usuario BANEADO → cortamos aquí con 403 y un código que la app
            // reconoce para cerrar sesión. El baneo es reversible (unban).
            Boolean banned = users.getObject().isBanned(decoded.getUid());
            if (Boolean.TRUE.equals(banned)) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"BANNED\",\"message\":\"Tu cuenta ha sido suspendida por un administrador.\"}");
                return;
            }

            FirebaseUser user = new FirebaseUser(
                    decoded.getUid(),
                    decoded.getEmail(),
                    decoded.getName()
            );

            // Envolvemos el FirebaseUser en el objeto que Spring Security entiende.
            // El tercer parámetro es la lista de roles — por ahora solo ROLE_USER.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {
            // Token inválido, expirado o de otro proyecto → limpiar contexto
            logger.warn("Firebase token rechazado para " + request.getMethod() + " "
                    + request.getRequestURI() + ": " + e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
