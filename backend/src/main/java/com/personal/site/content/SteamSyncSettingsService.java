package com.personal.site.content;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;

@Service
public class SteamSyncSettingsService {
    private final Path directory;

    public SteamSyncSettingsService(@Value("${app.steam.settings-dir:/opt/personal-site/data}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath();
    }

    public Settings load() {
        try {
            Files.createDirectories(directory);
            return new Settings(read("steam-profile-url"), Files.exists(file("steam-api-key")));
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取 Steam 同步配置", exception);
        }
    }

    public void save(String profileUrl, String apiKey) {
        try {
            Files.createDirectories(directory);
            write("steam-profile-url", profileUrl.trim());
            if (apiKey != null && !apiKey.trim().isEmpty()) write("steam-api-key", apiKey.trim());
        } catch (IOException exception) {
            throw new IllegalStateException("无法保存 Steam 同步配置", exception);
        }
    }

    public String profileUrl() { return load().profileUrl(); }

    public String apiKey() {
        try {
            Path path = file("steam-api-key");
            return Files.exists(path) ? Files.readString(path).trim() : "";
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取 Steam API Key", exception);
        }
    }

    private void write(String name, String value) throws IOException {
        Path target = file(name);
        Path temp = target.resolveSibling(name + ".tmp");
        Files.writeString(temp, value);
        try {
            Files.setPosixFilePermissions(temp, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // Windows does not provide POSIX permissions.
        }
        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private String read(String name) throws IOException {
        Path path = file(name);
        return Files.exists(path) ? Files.readString(path).trim() : "";
    }

    private Path file(String name) { return directory.resolve(name); }

    public record Settings(String profileUrl, boolean hasApiKey) {}
}
