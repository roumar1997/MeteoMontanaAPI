package com.meteomontana.api.infrastructure.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
