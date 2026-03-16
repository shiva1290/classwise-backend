package com.classwise.api.security;

import com.classwise.api.entity.User;
import com.classwise.api.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String SESSION_COOKIE_NAME = "better-auth.session_token";

    private final AuthService authService;

    public SessionAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String sessionToken = getSessionTokenFromCookie(request);

        if (sessionToken == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                sessionToken = authHeader.substring(7);
            }
        }

        if (sessionToken != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Optional<User> userOpt = authService.getUserFromSessionToken(sessionToken);
                if (userOpt.isPresent()) {
                    UserPrincipal principal = new UserPrincipal(userOpt.get());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // invalid or expired session token — continue unauthenticated
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getSessionTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
