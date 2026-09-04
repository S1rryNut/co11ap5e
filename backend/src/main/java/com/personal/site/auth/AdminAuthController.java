package com.personal.site.auth;

import com.personal.site.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminSessionService sessions;
    private final LoginRateLimiter limiter;
    private final AppProperties properties;

    public AdminAuthController(AdminSessionService sessions, LoginRateLimiter limiter, AppProperties properties) {
        this.sessions = sessions;
        this.limiter = limiter;
        this.properties = properties;
    }

    @PostMapping("/login")
    public Map<String, String> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response) {
        String address = clientAddress(request);
        if (!limiter.allow(address)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
        AdminSessionService.AdminIdentity admin = sessions.authenticate(body.username(), body.password())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        limiter.clear(address);
        String token = sessions.createSession(admin.id());
        ResponseCookie cookie = ResponseCookie.from(AdminSessionService.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(properties.getSecurity().isCookieSecure() && isSecureRequest(request))
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofHours(properties.getSecurity().getSessionHours()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return Map.of("username", admin.username(), "token", token);
    }

    @PostMapping("/mobile-login")
    public Map<String, String> mobileLogin(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        String address = clientAddress(request);
        if (!limiter.allow(address)) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
        AdminSessionService.AdminIdentity admin = sessions.authenticate(body.username(), body.password())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        limiter.clear(address);
        String token = sessions.createSession(admin.id());
        return Map.of("username", admin.username(), "token", token);
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of("username", authentication.getName());
    }

    @GetMapping("/audit")
    public java.util.List<AuditEntry> audit() {
        return sessions.auditLogs();
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        sessions.revoke(sessions.tokenFromRequest(request));
        ResponseCookie cookie = ResponseCookie.from(AdminSessionService.COOKIE_NAME, "")
                .httpOnly(true).secure(properties.getSecurity().isCookieSecure() && isSecureRequest(request)).sameSite("Strict")
                .path("/").maxAge(Duration.ZERO).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordChangeRequest body, Authentication authentication) {
        AdminSessionService.AdminIdentity admin = (AdminSessionService.AdminIdentity) authentication.getDetails();
        try {
            sessions.changePassword(admin.id(), body.currentPassword(), body.newPassword());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is invalid");
        }
    }

    private String clientAddress(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-Proto");
        return "https".equalsIgnoreCase(forwarded) || (forwarded == null && request.isSecure());
    }

    public record LoginRequest(@NotBlank @Size(max = 80) String username,
                               @NotBlank @Size(max = 200) String password) {}

    public record PasswordChangeRequest(@NotBlank @Size(max = 200) String currentPassword,
                                        @NotBlank @Size(min = 14, max = 200) String newPassword) {}

    public record AuditEntry(long id, String adminUsername, String method, String path, int status, java.time.Instant createdAt) {}
}
