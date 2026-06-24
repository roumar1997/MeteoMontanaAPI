package com.meteomontana.api.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate-limit sencillo por IP (ventana fija de 60 s). En memoria, por instancia:
 * su objetivo no es seguridad fina, sino evitar que un cliente con un bug o
 * abusivo agote el pool de conexiones a base de peticiones. Para un límite
 * compartido entre varias réplicas haría falta un store externo (Redis); con
 * una sola instancia esto sobra.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_PER_MINUTE = 150;
    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

    private static final class Counter {
        long windowStart;
        int count;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        // No limitar el healthcheck: lo pinga el monitor de uptime cada pocos seg.
        if (path != null && path.startsWith("/actuator")) {
            chain.doFilter(req, res);
            return;
        }

        String ip = clientIp(req);
        long now = System.currentTimeMillis();
        boolean limited;
        Counter c = counters.computeIfAbsent(ip, k -> new Counter());
        synchronized (c) {
            if (now - c.windowStart >= WINDOW_MS) {
                c.windowStart = now;
                c.count = 0;
            }
            c.count++;
            limited = c.count > MAX_PER_MINUTE;
        }
        // Cota de memoria: si el mapa crece demasiado (muchas IPs), se reinicia.
        if (counters.size() > 10_000) counters.clear();

        if (limited) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.getWriter().write("Too many requests");
            return;
        }
        chain.doFilter(req, res);
    }

    /** IP real del cliente (Railway va detrás de proxy → X-Forwarded-For). */
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
