package com.personal.site.auth;

import com.personal.site.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AppProperties properties;

    public AdminBootstrap(JdbcTemplate jdbc, PasswordEncoder encoder, AppProperties properties) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM admins", Integer.class);
        if (count != null && count > 0) return;
        String password = properties.getAdmin().getPassword();
        if (password == null || password.length() < 14) {
            log.warn("No administrator created. Set APP_ADMIN_PASSWORD to at least 14 characters and restart.");
            return;
        }
        jdbc.update("INSERT INTO admins(username, password_hash) VALUES (?, ?)",
                properties.getAdmin().getUsername(), encoder.encode(password));
        log.info("Initial administrator account created for '{}'.", properties.getAdmin().getUsername());
    }
}

