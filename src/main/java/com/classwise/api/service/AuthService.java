package com.classwise.api.service;

import com.classwise.api.entity.Account;
import com.classwise.api.entity.Role;
import com.classwise.api.entity.Session;
import com.classwise.api.entity.User;
import com.classwise.api.repository.AccountRepository;
import com.classwise.api.repository.SessionRepository;
import com.classwise.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final SessionRepository sessionRepository;

    public AuthService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.sessionRepository = sessionRepository;
    }

    // ---- better-auth compatible scrypt: N=16384, r=16, p=1, dkLen=64 ----
    // Format: hex(salt):hex(derivedKey)

    private String hashPassword(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = scryptDerive(password, salt);
        return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
    }

    private boolean verifyPassword(String password, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] salt = HexFormat.of().parseHex(parts[0]);
        byte[] expectedHash = HexFormat.of().parseHex(parts[1]);
        byte[] actualHash = scryptDerive(password, salt);
        return MessageDigest.isEqual(expectedHash, actualHash);
    }

    private byte[] scryptDerive(String password, byte[] salt) {
        try {
            return org.bouncycastle.crypto.generators.SCrypt.generate(
                    password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    salt,
                    16384, // N (cpu/memory cost)
                    16,    // r (block size)
                    1,     // p (parallelization)
                    64     // dkLen (derived key length)
            );
        } catch (Exception e) {
            throw new RuntimeException("scrypt derivation failed", e);
        }
    }

    @Transactional
    public Map<String, Object> signup(String name,
                                      String email,
                                      String password,
                                      String role,
                                      String image,
                                      String imageCldPubId,
                                      String ipAddress,
                                      String userAgent) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            throw new IllegalStateException("Email already registered");
        }

        Instant now = Instant.now();

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName(name);
        user.setEmail(email);
        user.setEmailVerified(false);
        user.setImage(image);
        user.setImageCldPubId(imageCldPubId);
        user.setRole(role != null ? Role.valueOf(role) : Role.student);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);

        Account account = new Account();
        account.setId(UUID.randomUUID().toString());
        account.setUser(user);
        account.setAccountId(user.getId());
        account.setProviderId("credential");
        account.setPassword(hashPassword(password));
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        accountRepository.save(account);

        String sessionToken = UUID.randomUUID().toString();
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setToken(sessionToken);
        session.setExpiresAt(now.plus(7, ChronoUnit.DAYS));
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionToken", sessionToken);
        result.put("body", Map.of(
                "user", userToMap(user),
                "session", sessionToMap(session)
        ));
        return result;
    }

    @Transactional
    public Map<String, Object> login(String email,
                                     String password,
                                     String ipAddress,
                                     String userAgent) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = userOpt.get();

        Optional<Account> accountOpt = accountRepository.findByUserIdAndProviderId(user.getId(), "credential");
        if (accountOpt.isEmpty() || accountOpt.get().getPassword() == null) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!verifyPassword(password, accountOpt.get().getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        Instant now = Instant.now();

        String sessionToken = UUID.randomUUID().toString();
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setUser(user);
        session.setToken(sessionToken);
        session.setExpiresAt(now.plus(7, ChronoUnit.DAYS));
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionRepository.save(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionToken", sessionToken);
        result.put("body", Map.of(
                "user", userToMap(user),
                "session", sessionToMap(session)
        ));
        return result;
    }

    @Transactional
    public void signOut(String sessionToken) {
        sessionRepository.deleteByToken(sessionToken);
    }

    public Map<String, Object> getSession(String sessionToken) {
        Optional<Session> sessionOpt = sessionRepository.findByTokenWithUser(sessionToken);
        if (sessionOpt.isEmpty()) {
            return null;
        }

        Session session = sessionOpt.get();
        if (session.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }

        User user = session.getUser();
        return Map.of(
                "user", userToMap(user),
                "session", sessionToMap(session)
        );
    }

    public Optional<User> getUserFromSessionToken(String sessionToken) {
        Optional<Session> sessionOpt = sessionRepository.findByTokenWithUser(sessionToken);
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        Session session = sessionOpt.get();
        if (session.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(session.getUser());
    }

    private Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("name", user.getName());
        map.put("email", user.getEmail());
        map.put("emailVerified", user.isEmailVerified());
        map.put("image", user.getImage());
        map.put("role", user.getRole().name());
        map.put("imageCldPubId", user.getImageCldPubId());
        map.put("createdAt", user.getCreatedAt());
        map.put("updatedAt", user.getUpdatedAt());
        return map;
    }

    private Map<String, Object> sessionToMap(Session session) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", session.getId());
        map.put("token", session.getToken());
        map.put("expiresAt", session.getExpiresAt());
        return map;
    }
}
