package com.personal.site.content;

import com.fasterxml.jackson.databind.JsonNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SteamGameMetadataService {
    private static final Logger log = LoggerFactory.getLogger(SteamGameMetadataService.class);
    private final JdbcTemplate jdbc;
    private final RestClient client = RestClient.builder()
            .baseUrl("https://store.steampowered.com")
            .defaultHeader("User-Agent", "Co11ap5e-Personal-Site/1.0")
            .build();
    private final RestClient steamApiClient = RestClient.builder()
            .baseUrl("https://api.steampowered.com")
            .defaultHeader("User-Agent", "Co11ap5e-Personal-Site/1.0")
            .build();

    public SteamGameMetadataService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public int syncLibrary(String rawProfileUrl, String rawApiKey) {
        String apiKey = rawApiKey == null ? "" : rawApiKey.trim();
        if (!apiKey.isEmpty()) {
            try {
                return syncViaWebApi(rawProfileUrl, apiKey);
            } catch (ResponseStatusException exception) {
                throw exception;
            } catch (Exception exception) {
                log.warn("Steam Web API sync failed: {}", exception.toString());
                throw new ResponseStatusException(BAD_GATEWAY, "Steam Web API 同步失败，请检查 API Key", exception);
            }
        }
        try {
            URI profile = URI.create(rawProfileUrl.trim());
            if (!"https".equals(profile.getScheme()) || !"steamcommunity.com".equalsIgnoreCase(profile.getHost())) {
                throw new ResponseStatusException(NOT_FOUND, "仅支持 Steam Community HTTPS 主页");
            }
            String path = profile.getPath().replaceAll("/+$", "");
            if (!path.matches("/(id|profiles)/[^/]+")) throw new ResponseStatusException(NOT_FOUND, "Steam 主页地址无效");
            String libraryUrl = "https://steamcommunity.com" + path + "/games?tab=all&xml=1";
            Document xml = Jsoup.connect(libraryUrl).userAgent("Co11ap5e-Personal-Site/1.0")
                    .timeout(15_000).parser(Parser.xmlParser()).get();
            int count = 0;
            for (Element game : xml.select("games > game")) {
                long appId = Long.parseLong(game.selectFirst("appID").text());
                String title = game.selectFirst("name").text();
                String hoursText = game.selectFirst("hoursOnRecord") == null ? "0" : game.selectFirst("hoursOnRecord").text().replace(",", "");
                int hours = (int) Math.round(Double.parseDouble(hoursText.isBlank() ? "0" : hoursText));
                String status = hours < 20 ? "DROPPED" : "LIBRARY";
                Element lastPlayed = game.selectFirst("lastPlayed");
                if (lastPlayed != null && lastPlayed.text().matches("\\d+")) {
                    long epoch = Long.parseLong(lastPlayed.text());
                    if (Instant.ofEpochSecond(epoch).isAfter(Instant.now().minus(7, ChronoUnit.DAYS))) status = "PLAYING";
                }
                jdbc.update("""
                    INSERT INTO game_entries(slug,title,cover_url,platform,status,hours_played,verdict,sort_order)
                    VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(slug) DO UPDATE SET
                    title=EXCLUDED.title,cover_url=EXCLUDED.cover_url,hours_played=EXCLUDED.hours_played,
                    status=CASE WHEN game_entries.status IN ('COMPLETED','WISHLIST') THEN game_entries.status ELSE EXCLUDED.status END,
                    updated_at=CURRENT_TIMESTAMP
                    """, "steam-" + appId, title,
                        "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/" + appId + "/library_600x900.jpg",
                        "Steam", status, hours, "从 Steam 游戏库同步，个人评价待补充。", count++);
            }
            return count;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Steam Community sync failed: {}", exception.toString());
            throw new ResponseStatusException(BAD_GATEWAY, "Steam 游戏库同步失败，请确认游戏详情为公开", exception);
        }
    }

    private int syncViaWebApi(String rawProfileUrl, String apiKey) {
        URI profile = URI.create(rawProfileUrl.trim());
        String path = profile.getPath().replaceAll("/+$", "");
        if (!path.matches("/(id|profiles)/[^/]+")) throw new ResponseStatusException(NOT_FOUND, "Steam 主页地址无效");
        String[] parts = path.split("/");
        String steamId = "id".equals(parts[1])
                ? resolveSteamId(apiKey, parts[2])
                : parts[2];
        JsonNode root = steamApiClient.get()
                .uri(uri -> uri.path("/IPlayerService/GetOwnedGames/v0001/")
                        .queryParam("key", apiKey)
                        .queryParam("steamid", steamId)
                        .queryParam("include_appinfo", "1")
                        .queryParam("include_played_free_games", "1")
                        .queryParam("format", "json")
                        .build())
                .retrieve().body(JsonNode.class);
        JsonNode games = root == null ? null : root.path("response").path("games");
        int count = 0;
        if (games != null && games.isArray()) {
            for (JsonNode game : games) {
                long appId = game.path("appid").asLong();
                String title = game.path("name").asText("Steam Game " + appId);
                int hours = (int) Math.round(game.path("playtime_forever").asDouble(0) / 60.0);
                String status = game.path("playtime_2weeks").asInt(0) > 0 ? "PLAYING" : hours < 20 ? "DROPPED" : "LIBRARY";
                jdbc.update("""
                    INSERT INTO game_entries(slug,title,cover_url,platform,status,hours_played,verdict,sort_order)
                    VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(slug) DO UPDATE SET
                    title=EXCLUDED.title,cover_url=EXCLUDED.cover_url,hours_played=EXCLUDED.hours_played,
                    status=CASE WHEN game_entries.status IN ('COMPLETED','WISHLIST') THEN game_entries.status ELSE EXCLUDED.status END,
                    updated_at=CURRENT_TIMESTAMP
                    """, "steam-" + appId, title,
                        "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/" + appId + "/library_600x900.jpg",
                        "Steam", status, hours, "从 Steam Web API 同步，个人评价待补充。", count++);
            }
        }
        return count;
    }

    private String resolveSteamId(String apiKey, String vanityUrl) {
        JsonNode root = steamApiClient.get()
                .uri(uri -> uri.path("/ISteamUser/ResolveVanityURL/v0001/")
                        .queryParam("key", apiKey)
                        .queryParam("vanityurl", vanityUrl)
                        .queryParam("format", "json")
                        .build())
                .retrieve().body(JsonNode.class);
        String steamId = root == null ? "" : root.path("response").path("steamid").asText("");
        if (steamId.isEmpty()) throw new ResponseStatusException(NOT_FOUND, "无法解析 Steam 自定义主页");
        return steamId;
    }

    public ContentModels.GameEntryInput load(long appId) {
        if (appId <= 0) throw new ResponseStatusException(NOT_FOUND, "Steam App ID 无效");
        try {
            JsonNode root = client.get()
                    .uri(uri -> uri.path("/api/appdetails").queryParam("appids", appId).queryParam("l", "schinese").build())
                    .retrieve().body(JsonNode.class);
            JsonNode result = root == null ? null : root.path(String.valueOf(appId));
            if (result == null || !result.path("success").asBoolean()) {
                throw new ResponseStatusException(NOT_FOUND, "Steam 未找到该游戏");
            }
            JsonNode data = result.path("data");
            String description = Jsoup.parse(data.path("short_description").asText("Steam 游戏资料待补充。")).text();
            if (description.length() > 1000) description = description.substring(0, 1000);
            List<String> platforms = new ArrayList<>();
            JsonNode supported = data.path("platforms");
            if (supported.path("windows").asBoolean()) platforms.add("Windows");
            if (supported.path("mac").asBoolean()) platforms.add("macOS");
            if (supported.path("linux").asBoolean()) platforms.add("Linux");
            return new ContentModels.GameEntryInput(
                    "steam-" + appId,
                    data.path("name").asText("Steam Game " + appId),
                    data.path("header_image").asText(""),
                    platforms.isEmpty() ? "Steam" : "Steam / " + String.join(" / ", platforms),
                    "WISHLIST", null, 0, null, description, "", 0);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "Steam 信息读取失败", exception);
        }
    }
}
