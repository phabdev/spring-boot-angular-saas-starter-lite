package com.phabdev.starter.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/** Single-instance fixed-window limiter. Do not trust client-supplied forwarded IP headers. */
@Component
public class AuthRequestFilter extends OncePerRequestFilter {
    private final String origin;
    private final int limit;
    private final Map<String, Window> windows = new LinkedHashMap<>();

    public AuthRequestFilter(
            @Value("${app.origin}") String origin,
            @Value("${app.auth-rate-limit}") int limit,
            @Value("${app.cookie-secure}") boolean secure) {
        URI uri = URI.create(origin);
        if (uri.getHost() == null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
            throw new IllegalArgumentException("APP_ORIGIN must be an origin without a path.");
        boolean local = ListOfLocalHosts.contains(uri.getHost());
        if (!local && (!secure || !"https".equals(uri.getScheme())))
            throw new IllegalArgumentException(
                    "Non-local origins require HTTPS and COOKIE_SECURE=true.");
        if (limit < 1) throw new IllegalArgumentException("AUTH_RATE_LIMIT must be positive.");
        this.origin = origin.replaceAll("/$", "");
        this.limit = limit;
    }

    private static final java.util.Set<String> ListOfLocalHosts =
            java.util.Set.of("localhost", "127.0.0.1", "[::1]");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/auth/")
                && !request.getMethod().equals("GET")) {
            String suppliedOrigin = request.getHeader("Origin");
            if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                    || (suppliedOrigin != null && !origin.equals(suppliedOrigin))) {
                SecurityConfig.error(response, 403, "Same-origin auth request header required.");
                return;
            }
            if (!allow(request.getRemoteAddr())) {
                response.setHeader("Retry-After", "60");
                SecurityConfig.error(
                        response, 429, "Too many authentication requests. Try again later.");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private synchronized boolean allow(String key) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> now - entry.getValue().start >= 60_000);
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= 10_000) return false;
            window = new Window(now);
            windows.put(key, window);
        }
        return ++window.count <= limit;
    }

    private static final class Window {
        final long start;
        int count;

        Window(long start) {
            this.start = start;
        }
    }
}
