package com.personal.site.auth;

import com.personal.site.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class AdminSessionService {
    public static final String COOKIE_NAME = "admin_session";
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminSessionService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, AppProperties properties) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    public Optional<AdminIdentity> authenticate(String username, String password) {
        return jdbc.query(
                "SELECT id, username, password_hash FROM admins WHERE username = ?",
                rs -> {
                    if (!rs.next() || !passwordEncoder.matches(password, rs.getString("password_hash"))) return Optional.empty();
                    return Optional.of(new AdminIdentity(rs.getLong("id"), rs.getString("username")));
                }, username);
    }

    @Transactional
    public void changePassword(long adminId, String currentPassword, String newPassword) {
        String hash = jdbc.queryForObject("SELECT password_hash FROM admins WHERE id = ?", String.class, adminId);
        if (hash == null || !passwordEncoder.matches(currentPassword, hash)) throw new IllegalArgumentException("invalid password");
        jdbc.update("UPDATE admins SET password_hash = ? WHERE id = ?", passwordEncoder.encode(newPassword), adminId);
        jdbc.update("DELETE FROM admin_sessions WHERE admin_id = ?", adminId);
    }

    public java.util.List<AdminAuthController.AuditEntry> auditLogs() {
        return jdbc.query("SELECT id,admin_username,method,path,status,created_at FROM admin_audit_logs ORDER BY created_at DESC LIMIT 100",
                (rs, row) -> new AdminAuthController.AuditEntry(rs.getLong("id"), rs.getString("admin_username"),
                        rs.getString("method"), rs.getString("path"), rs.getInt("status"), rs.getTimestamp("created_at").toInstant()));
    }

    @Transactional
    public String createSession(long adminId) {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        Instant expiresAt = Instant.now().plus(properties.getSecurity().getSessionHours(), ChronoUnit.HOURS);
        jdbc.update("DELETE FROM admin_sessions WHERE expires_at < CURRENT_TIMESTAMP");
        jdbc.update("INSERT INTO admin_sessions(token_hash, admin_id, expires_at) VALUES (?, ?, ?)",
                hash(token), adminId, Timestamp.from(expiresAt));
        return token;
    }

    public Optional<AdminIdentity> findByToken(String token) {
        if (token == null || token.length() < 40) return Optional.empty();
        return jdbc.query("""
                SELECT a.id, a.username
                FROM admin_sessions s
                JOIN admins a ON a.id = s.admin_id
                WHERE s.token_hash = ? AND s.expires_at > CURRENT_TIMESTAMP
                """, rs -> rs.next()
                ? Optional.of(new AdminIdentity(rs.getLong("id"), rs.getString("username")))
                : Optional.empty(), hash(token));
    }

    public Optional<AdminIdentity> findFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            Optional<AdminIdentity> bearer = findByToken(authorization.substring(7).trim());
            if (bearer.isPresent()) return bearer;
        }
        if (request.getCookies() == null) return Optional.empty();
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return findByToken(cookie.getValue());
        }
        return Optional.empty();
    }

    public void revoke(String token) {
        if (token != null) jdbc.update("DELETE FROM admin_sessions WHERE token_hash = ?", hash(token));
    }

    public String tokenFromRequest(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record AdminIdentity(long id, String username) {}
}
