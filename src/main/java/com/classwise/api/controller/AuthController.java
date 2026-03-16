package com.classwise.api.controller;

import com.classwise.api.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SESSION_COOKIE_NAME = "better-auth.session_token";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/sign-up/email")
    public ResponseEntity<?> signup(@RequestBody Map<String, Object> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String name = (String) body.get("name");
            String email = (String) body.get("email");
            String password = (String) body.get("password");
            String role = (String) body.get("role");
            String image = (String) body.get("image");
            String imageCldPubId = (String) body.get("imageCldPubId");

            if (name == null || email == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Name, email, and password are required"));
            }

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Map<String, Object> result = authService.signup(name, email, password, role, image, imageCldPubId,
                    ipAddress, userAgent);

            String sessionToken = (String) result.get("sessionToken");
            addSessionCookie(response, sessionToken);

            return ResponseEntity.ok(result.get("body"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", Map.of("message", ex.getMessage(), "status", 409)));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", Map.of("message", "Registration failed: " + ex.getMessage(), "status", 500)));
        }
    }

    @PostMapping("/sign-in/email")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            String email = (String) body.get("email");
            String password = (String) body.get("password");

            if (email == null || password == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", Map.of("message", "Email and password are required", "status", 400)));
            }

            String ipAddress = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            Map<String, Object> result = authService.login(email, password, ipAddress, userAgent);

            String sessionToken = (String) result.get("sessionToken");
            addSessionCookie(response, sessionToken);

            return ResponseEntity.ok(result.get("body"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", Map.of("message", "Invalid credentials", "status", 401)));
        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", Map.of("message", "Login failed: " + ex.getMessage(), "status", 500)));
        }
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(HttpServletRequest request, HttpServletResponse response) {
        try {
            String sessionToken = getSessionToken(request);
            if (sessionToken != null) {
                authService.signOut(sessionToken);
            }
            clearSessionCookie(response);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception ex) {
            clearSessionCookie(response);
            return ResponseEntity.ok(Map.of("success", true));
        }
    }

    @GetMapping("/get-session")
    public ResponseEntity<?> getSession(HttpServletRequest request) {
        try {
            String sessionToken = getSessionToken(request);
            if (sessionToken == null) {
                return ResponseEntity.ok(Map.of("session", (Object) null));
            }

            Map<String, Object> result = authService.getSession(sessionToken);
            if (result == null) {
                return ResponseEntity.ok(Map.of("session", (Object) null));
            }

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            return ResponseEntity.ok(Map.of("session", (Object) null));
        }
    }

    private String getSessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void addSessionCookie(HttpServletResponse response, String sessionToken) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        cookie.setSecure(false);
        response.addCookie(cookie);
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
