package com.personal.site.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.site.config.AppProperties;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Service
@ConditionalOnProperty(prefix = "app.ai", name = "sync-enabled", havingValue = "true", matchIfMissing = true)
public class AiFeedService {
    private static final Logger log = LoggerFactory.getLogger(AiFeedService.class);
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public AiFeedService(JdbcTemplate jdbc, ObjectMapper objectMapper, AppProperties properties) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Scheduled(initialDelayString = "PT20S", fixedDelayString = "PT1H")
    public void scheduledSync() {
        syncAll();
    }

    public SyncResult syncAll() {
        int inserted = 0;
        List<String> failures = new ArrayList<>();
        for (AppProperties.FeedSource source : properties.getAi().getSources()) {
            try {
                inserted += sync(source);
            } catch (Exception exception) {
                failures.add(source.getName());
                log.warn("AI feed sync failed for {}: {}", source.getName(), exception.getMessage());
            }
        }
        jdbc.update("DELETE FROM ai_news WHERE id NOT IN (SELECT id FROM ai_news ORDER BY published_at DESC LIMIT 200)");
        return new SyncResult(inserted, failures);
    }

    private int sync(AppProperties.FeedSource source) throws Exception {
        URI uri = URI.create(source.getUrl());
        if (!"https".equalsIgnoreCase(uri.getScheme())) throw new IllegalArgumentException("Feed URL must use HTTPS");
        HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(15))
                .header("User-Agent", "PersonalSiteFeedReader/1.0")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                .GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length > 2_000_000)
            throw new IllegalStateException("Unexpected feed response");
        var feed = new SyndFeedInput().build(new XmlReader(new ByteArrayInputStream(response.body())));
        int inserted = 0;
        for (SyndEntry entry : feed.getEntries().stream().limit(20).toList()) {
            if (entry.getLink() == null || entry.getTitle() == null) continue;
            Integer suppressed = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM ai_news_suppressions WHERE source_url = ?", Integer.class, entry.getLink());
            if (suppressed != null && suppressed > 0) continue;
            String summary = entry.getDescription() == null ? "点击查看来源文章。" : clean(entry.getDescription().getValue());
            Date date = entry.getPublishedDate() != null ? entry.getPublishedDate() : entry.getUpdatedDate();
            try {
                inserted += jdbc.update("""
                        INSERT INTO ai_news(title,summary,source_name,source_url,published_at,topics)
                        VALUES (?,?,?,?,?,?)
                        """, truncate(entry.getTitle(), 300), truncate(summary, 500), source.getName(),
                        entry.getLink(), normalizeFeedDate(date), topics(entry.getTitle()));
            } catch (DuplicateKeyException ignored) {
                // Already synchronized.
            }
        }
        return inserted;
    }

    private String clean(String html) {
        return Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    }

    /**
     * 部分源（如 InfoQ）会把北京时间误标成 GMT，直接解析会得到未来时间。
     * 兜底：若发布时间比当前晚 1 小时以上，按"原 UTC 挂钟时间 = 北京时间(UTC+8)"重新解释。
     */
    private Timestamp normalizeFeedDate(Date date) {
        if (date == null) return Timestamp.from(Instant.now());
        Instant instant = date.toInstant();
        Instant now = Instant.now();
        if (instant.isAfter(now.plus(Duration.ofHours(1)))) {
            return Timestamp.from(instant.atZone(ZoneOffset.UTC)
                    .withZoneSameLocal(ZoneId.of("Asia/Shanghai"))
                    .toInstant());
        }
        return Timestamp.from(instant);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private String topics(String title) throws JsonProcessingException {
        String value = title.toLowerCase(Locale.ROOT);
        List<String> topics = new ArrayList<>();
        if (value.contains("model") || value.contains("模型")) topics.add("模型");
        if (value.contains("agent") || value.contains("智能体")) topics.add("Agent");
        if (value.contains("research") || value.contains("paper")) topics.add("研究");
        if (value.contains("open source") || value.contains("开源")) topics.add("开源");
        if (topics.isEmpty()) topics.add("AI 动态");
        return objectMapper.writeValueAsString(topics);
    }

    public record SyncResult(int inserted, List<String> failedSources) {}
}
