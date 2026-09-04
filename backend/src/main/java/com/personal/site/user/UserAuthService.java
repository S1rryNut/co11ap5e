package com.personal.site.user;

import com.personal.site.content.ContentModels;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class UserAuthService {
    private static final int CODE_LIFETIME_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RESEND_COOLDOWN_SECONDS = 60;
    private static final long SESSION_HOURS = 24L * 30;

    private final JdbcTemplate jdbc;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserAuthService.class);

    public UserAuthService(JdbcTemplate jdbc, JavaMailSender mailSender) {
        this.jdbc = jdbc;
        this.mailSender = mailSender;
    }

    /** 发送验证码到邮箱（同一邮箱 60 秒冷却，旧码作废）；返回该邮箱是否已注册 */
    public ContentModels.AuthCodeResult sendCode(ContentModels.AuthCodeInput input) {
        String email = normalize(input.email());
        if (!isMailConfigured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "邮件服务尚未配置");
        Instant cooldown = Instant.now().minusSeconds(RESEND_COOLDOWN_SECONDS);
        List<Long> recent = jdbc.query("SELECT id FROM email_verifications WHERE email=? AND used=false AND created_at > ?",
                (rs, row) -> rs.getLong(1), email, Timestamp.from(cooldown));
        if (!recent.isEmpty()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "验证码发送过于频繁，请 1 分钟后再试");

        jdbc.update("UPDATE email_verifications SET used=true WHERE email=? AND used=false", email);
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        jdbc.update("INSERT INTO email_verifications(email, code_hash, expires_at) VALUES (?,?,?)",
                email, hash(email + ":" + code),
                Timestamp.from(Instant.now().plus(CODE_LIFETIME_MINUTES, ChronoUnit.MINUTES)));
        sendMail(email, "折叠思维 · 邮箱验证码",
                "你的验证码是：" + code + "\n\n5 分钟内有效，用于登录折叠思维。\n如果不是你本人操作，请忽略这封邮件。");
        return new ContentModels.AuthCodeResult(findUserId(email) != null);
    }

    /** 校验验证码并登录/注册，返回会话 token */
    @Transactional
    public ContentModels.UserAuthResponse verifyAndAuthenticate(ContentModels.AuthVerifyInput input) {
        String email = normalize(input.email());
        List<VerificationRow> rows = jdbc.query("""
                SELECT id, code_hash, expires_at, attempts FROM email_verifications
                WHERE email=? AND used=false ORDER BY created_at DESC LIMIT 1
                """, (rs, row) -> new VerificationRow(rs.getLong("id"), rs.getString("code_hash"),
                rs.getTimestamp("expires_at").toInstant(), rs.getInt("attempts")), email);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码不存在或已过期，请重新获取");
        VerificationRow verification = rows.getFirst();
        if (verification.expiresAt.isBefore(Instant.now())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码已过期，请重新获取");
        if (verification.attempts >= MAX_ATTEMPTS) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "尝试次数过多，请重新获取验证码");
        if (!hash(email + ":" + input.code()).equals(verification.codeHash)) {
            jdbc.update("UPDATE email_verifications SET attempts = attempts + 1 WHERE id=?", verification.id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误，请重新输入");
        }
        jdbc.update("UPDATE email_verifications SET used=true WHERE id=?", verification.id);

        Long userId = findUserId(email);
        if (userId == null) {
            if (input.nickname() == null || input.nickname().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "首次使用需要设置一个昵称");
            }
            userId = jdbc.queryForObject("INSERT INTO users(email, nickname) VALUES (?,?) RETURNING id",
                    Long.class, email, input.nickname().trim());
        } else if (input.nickname() != null && !input.nickname().isBlank()) {
            jdbc.update("UPDATE users SET nickname=? WHERE id=?", input.nickname().trim(), userId);
        }
        return new ContentModels.UserAuthResponse(createSession(userId), userView(userId));
    }

    public void logout(String token) {
        if (token != null && !token.isBlank()) jdbc.update("DELETE FROM user_sessions WHERE token_hash=?", hash(token));
    }

    public ContentModels.UserView currentUser(HttpServletRequest request) {
        String token = tokenFromRequest(request);
        if (token == null) return null;
        Long userId = findUserByToken(token);
        return userId == null ? null : userView(userId);
    }

    /** 返回已登录用户 id，未登录返回 null */
    public Long userIdFromRequest(HttpServletRequest request) {
        String token = tokenFromRequest(request);
        if (token == null) return null;
        return findUserByToken(token);
    }

    public String nicknameForUser(long userId) {
        List<String> nicknames = jdbc.query("SELECT nickname FROM users WHERE id=?", (rs, row) -> rs.getString(1), userId);
        return nicknames.isEmpty() ? null : nicknames.getFirst();
    }

    private Long findUserByToken(String token) {
        if (token == null || token.length() < 20) return null;
        List<Long> ids = jdbc.query("""
                SELECT s.user_id FROM user_sessions s
                WHERE s.token_hash=? AND s.expires_at > CURRENT_TIMESTAMP
                """, (rs, row) -> rs.getLong(1), hash(token));
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private Long findUserId(String email) {
        List<Long> ids = jdbc.query("SELECT id FROM users WHERE email=?", (rs, row) -> rs.getLong(1), email);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    @Transactional
    private String createSession(long userId) {
        jdbc.update("DELETE FROM user_sessions WHERE expires_at < CURRENT_TIMESTAMP");
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("INSERT INTO user_sessions(token_hash, user_id, expires_at) VALUES (?,?,?)",
                hash(token), userId, Timestamp.from(Instant.now().plus(SESSION_HOURS, ChronoUnit.HOURS)));
        return token;
    }

    private ContentModels.UserView userView(long userId) {
        return jdbc.queryForObject("SELECT id, email, nickname, is_owner FROM users WHERE id=?", (rs, row) ->
                new ContentModels.UserView(rs.getLong("id"), rs.getString("email"), rs.getString("nickname"),
                        rs.getBoolean("is_owner")), userId);
    }

    /** 该用户是否为站长本人账号（评论显示「作者」徽章） */
    public boolean isOwner(long userId) {
        List<Boolean> owners = jdbc.query("SELECT is_owner FROM users WHERE id=?", (rs, row) -> rs.getBoolean(1), userId);
        return !owners.isEmpty() && Boolean.TRUE.equals(owners.getFirst());
    }

    private String tokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) return authorization.substring(7).trim();
        return null;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isMailConfigured() {
        return mailSender instanceof JavaMailSenderImpl impl && impl.getHost() != null && !impl.getHost().isBlank();
    }

    private void sendMail(String to, String subject, String text) {
        try {
            String from = mailSender instanceof JavaMailSenderImpl impl ? impl.getUsername() : null;
            if (from == null || from.isBlank()) throw new IllegalStateException("SMTP username is not configured");
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom("折叠思维 <" + from + ">");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
        } catch (Exception exception) {
            log.error("sendMail failed: host={} port={} to={}",
                    mailSender instanceof JavaMailSenderImpl impl ? impl.getHost() : "?",
                    mailSender instanceof JavaMailSenderImpl impl2 ? impl2.getPort() : "?",
                    to, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "验证码邮件发送失败，请稍后重试");
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private record VerificationRow(long id, String codeHash, Instant expiresAt, int attempts) {}
}
