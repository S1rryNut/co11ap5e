package com.personal.site.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminAuditFilter extends OncePerRequestFilter {
    private final JdbcTemplate jdbc;

    public AdminAuditFilter(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);
        if (!request.getRequestURI().startsWith("/api/admin/") || request.getRequestURI().endsWith("/auth/me")) return;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return;
        jdbc.update("INSERT INTO admin_audit_logs(admin_username,method,path,status) VALUES (?,?,?,?)",
                auth.getName(), request.getMethod(), request.getRequestURI(), response.getStatus());
    }
}
