package com.personal.site.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Admin admin = new Admin();
    private final Security security = new Security();
    private final Ai ai = new Ai();

    public Admin getAdmin() { return admin; }
    public Security getSecurity() { return security; }
    public Ai getAi() { return ai; }

    public static class Admin {
        private String username = "admin";
        private String password = "";
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Security {
        private boolean cookieSecure;
        private int sessionHours = 12;
        private String corsOrigin = "http://localhost:3000";
        public boolean isCookieSecure() { return cookieSecure; }
        public void setCookieSecure(boolean cookieSecure) { this.cookieSecure = cookieSecure; }
        public int getSessionHours() { return sessionHours; }
        public void setSessionHours(int sessionHours) { this.sessionHours = sessionHours; }
        public String getCorsOrigin() { return corsOrigin; }
        public void setCorsOrigin(String corsOrigin) { this.corsOrigin = corsOrigin; }
    }

    public static class Ai {
        private boolean syncEnabled = true;
        private List<FeedSource> sources = new ArrayList<>();
        public boolean isSyncEnabled() { return syncEnabled; }
        public void setSyncEnabled(boolean syncEnabled) { this.syncEnabled = syncEnabled; }
        public List<FeedSource> getSources() { return sources; }
        public void setSources(List<FeedSource> sources) { this.sources = sources; }
    }

    public static class FeedSource {
        private String name;
        private String url;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}

