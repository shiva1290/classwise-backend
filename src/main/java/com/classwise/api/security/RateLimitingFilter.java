package com.classwise.api.security;

import com.classwise.api.entity.Role;
import com.classwise.api.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static class Window {
        Instant windowStart;
        int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.equals("/ping") || path.equals("/health") || path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Object principal = request.getUserPrincipal();
        Role role = Role.student;
        if (principal instanceof UserPrincipal userPrincipal) {
            User user = userPrincipal.getUser();
            role = user.getRole();
        }

        int limit;
        switch (role) {
            case admin -> limit = 200;
            case teacher -> limit = 100;
            case student -> limit = 100;
            default -> limit = 60;
        }

        String key = request.getRemoteAddr() + ":" + role.name();
        Instant now = Instant.now();
        Window window = windows.computeIfAbsent(key, k -> {
            Window w = new Window();
            w.windowStart = now;
            w.count = 0;
            return w;
        });

        synchronized (window) {
            if (now.isAfter(window.windowStart.plusSeconds(60))) {
                window.windowStart = now;
                window.count = 0;
            }
            window.count++;
            if (window.count > limit) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter()
                        .write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please wait.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
