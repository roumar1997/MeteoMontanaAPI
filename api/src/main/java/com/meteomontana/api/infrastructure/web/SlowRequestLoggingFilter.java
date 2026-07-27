package com.meteomontana.api.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Observabilidad mínima sin infraestructura nueva: cada request que tarde más
 * del umbral o acabe en 5xx deja UNA línea de WARN en el log (visible en
 * Railway → Deploy Logs). Así una degradación de prod se ve en los logs antes
 * de que la cuente un usuario.
 *
 * Umbral por env var SLOW_REQUEST_MS (default 1000 ms). No loguea query
 * strings (pueden llevar datos personales), solo método + ruta + status + ms.
 */
@Component
@Order(5)
public class SlowRequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SlowRequestLoggingFilter.class);

    private final long thresholdMs;

    public SlowRequestLoggingFilter(
            @org.springframework.beans.factory.annotation.Value("${SLOW_REQUEST_MS:1000}") long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            int status = response.getStatus();
            if (ms >= thresholdMs || status >= 500) {
                log.warn("SLOW/ERROR {} {} -> {} en {} ms",
                        request.getMethod(), request.getRequestURI(), status, ms);
            }
        }
    }

    /** El healthcheck de Railway pega cada pocos segundos: fuera del radar. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }
}
