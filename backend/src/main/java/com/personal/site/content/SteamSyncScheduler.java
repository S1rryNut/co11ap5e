package com.personal.site.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SteamSyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(SteamSyncScheduler.class);
    private final SteamSyncSettingsService settings;
    private final SteamGameMetadataService metadata;

    public SteamSyncScheduler(SteamSyncSettingsService settings, SteamGameMetadataService metadata) {
        this.settings = settings;
        this.metadata = metadata;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void syncDaily() {
        SteamSyncSettingsService.Settings saved = settings.load();
        String profileUrl = saved.profileUrl();
        String apiKey = settings.apiKey();
        if (profileUrl.isEmpty() || apiKey.isEmpty()) return;
        try {
            int count = metadata.syncLibrary(profileUrl, apiKey);
            log.info("Scheduled Steam sync completed with {} games", count);
        } catch (Exception exception) {
            log.warn("Scheduled Steam sync failed: {}", exception.toString());
        }
    }
}
