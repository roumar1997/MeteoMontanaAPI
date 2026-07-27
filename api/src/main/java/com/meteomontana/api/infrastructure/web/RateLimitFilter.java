package com.meteomontana.api.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * Configurable por env RATE_LIMIT_PER_MINUTE:
 *   - default 600/min por IP (generoso: no estorba a usuarios reales, ni siquiera
 *     a varios detrás del mismo NAT de una operadora móvil, pero corta un flood).
 *   - poner 0 lo DESACTIVA (útil para medir la capacidad real con una prueba de
 *     carga desde una sola IP, donde el limitador falsearía el resultado).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    /** Límite de ESCRITURAS por minuto y IP (POST/PUT/DELETE). Mucho más bajo
     *  que el global: 600 lecturas/min es razonable navegando, pero 600
     *  escrituras/min solo lo hace un bot. 0 = usar solo el límite global. */
    @Value("${RATE_LIMIT_WRITES_PER_MINUTE:60}")
    private int maxWritesPerMinute;

    @Value("${RATE_LIMIT_PER_MINUTE:600}")
    private int maxPerMinute;

    private static final long WINDOW_MS = 60_000L;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    /** Contadores aparte para las escrituras (cupo más estrecho). */
    private final ConcurrentHashMap<String, Counter> writeCounters = new ConcurrentHashMap<>();

    private static final class Counter {
        long windowStart;
        int count;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        // Desactivado (RATE_LIMIT_PER_MINUTE=0) o healthcheck → no limitar.
        if (maxPerMinute <= 0 || (path != null && path.startsWith("/actuator"))) {
            chain.doFilter(req, res);
            return;
        }

        String ip = clientIp(req);
        long now = System.currentTimeMillis();
        String method = req.getMethod();
        boolean isWrite = "POST".equals(method) || "PUT".equals(method)
                || "PATCH".equals(method) || "DELETE".equals(method);

        boolean limited = hit(counters, ip, now, maxPerMinute);
        // Las escrituras cuentan ADEMÁS contra su propio cupo, más estrecho.
        if (!limited && isWrite && maxWritesPerMinute > 0) {
            limited = hit(writeCounters, ip, now, maxWritesPerMinute);
        }
        // Cota de memoria: si el mapa crece demasiado (muchas IPs), se reinicia.
        if (counters.size() > 10_000) counters.clear();
        if (writeCounters.size() > 10_000) writeCounters.clear();

        if (limited) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.getWriter().write("Too many requests");
            return;
        }
        chain.doFilter(req, res);
    }

    /** Suma una petición al contador de esa IP y dice si pasó del cupo. */
    private static boolean hit(java.util.Map<String, Counter> map, String ip, long now, int max) {
        Counter c = map.computeIfAbsent(ip, k -> new Counter());
        synchronized (c) {
            if (now - c.windowStart >= WINDOW_MS) {
                c.windowStart = now;
                c.count = 0;
            }
            c.count++;
            return c.count > max;
        }
    }

    /** IP real del cliente (Railway va detrás de proxy → X-Forwarded-For). */
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }
}
