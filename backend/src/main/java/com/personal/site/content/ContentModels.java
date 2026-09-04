package com.personal.site.content;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ContentModels {
    private ContentModels() {}

    public record Message(long id, String name, String email, String content, Instant createdAt) {}

// 公开留言（脱敏视图：不含邮箱，仅用于留言墙展示）
public record PublicMessage(long id, String name, String content, Instant createdAt, boolean pinned,
                            long likeCount, long commentCount) {}
    public record MessageInput(@NotBlank @Size(max = 80) String name,
                               @Email @Size(max = 200) String email,
                               @NotBlank @Size(max = 2000) String content) {}
    public record MessageComment(long id, long messageId, String name, String content, Instant createdAt,
                                 boolean isAuthor, boolean verified) {}
    public record MessageCommentInput(@NotBlank @Size(max = 80) String name,
                                      @NotBlank @Size(max = 500) String content) {}
    public record MessageLikeResult(long messageId, long likeCount, boolean liked) {}

    public record UserView(long id, String email, String nickname, boolean isOwner) {}
    public record UserAuthResponse(String token, UserView user) {}

    public record AuthCodeResult(boolean registered) {}
    public record AuthCodeInput(@NotBlank @Email @Size(max = 254) String email) {}
    public record AuthVerifyInput(@NotBlank @Email @Size(max = 254) String email,
                                  @NotBlank @Pattern(regexp = "\\d{6}") String code,
                                  @Size(max = 80) String nickname) {}
    public record SteamSyncInput(@NotBlank @Size(max = 1000) String profileUrl,
                                 @Size(max = 200) String apiKey) {}
    public record SteamSyncSettingsInput(@NotBlank @Size(max = 1000) String profileUrl,
                                         @Size(max = 200) String apiKey) {}

    public record ArticleSummary(long id, String slug, String title, String excerpt, String category,
                                 List<String> tags, Instant publishedAt, int readingMinutes) {}

    public record ArticleNeighbors(ArticleSummary prev, ArticleSummary next) {}

    public record Article(long id, String slug, String title, String excerpt, String category,
                          List<String> tags, String content, Instant publishedAt, int readingMinutes,
                          boolean published) {}

    public record ArticleVersion(long id, long articleId, String slug, String title, String excerpt,
                                 String category, List<String> tags, String content, Instant publishedAt,
                                 int readingMinutes, boolean published, Instant createdAt) {}

    public record ArticleInput(
            @NotBlank @Size(max = 180) @Pattern(regexp = "[a-z0-9-]+") String slug,
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 320) String excerpt,
            @NotBlank @Size(max = 80) String category,
            @NotNull @Size(max = 12) List<@Size(max = 40) String> tags,
            @NotBlank @Size(max = 200_000) String content,
            boolean published,
            @Min(1) @Max(120) int readingMinutes) {}

    public record Project(long id, String slug, String name, String summary, List<String> stack,
                          String background, String responsibility, List<String> workflow,
                          String architecture, List<String> highlights, String retrospective,
                          String repositoryUrl, String liveUrl, boolean featured) {}

    public record AdminProject(long id, String slug, String name, String summary, List<String> stack,
                               String background, String responsibility, List<String> workflow,
                               String architecture, List<String> highlights, String retrospective,
                               String repositoryUrl, String liveUrl, boolean featured, int sortOrder) {}

    public record ProjectInput(
            @NotBlank @Size(max = 180) @Pattern(regexp = "[a-z0-9-]+") String slug,
            @NotBlank @Size(max = 160) String name,
            @NotBlank @Size(max = 10_000) String summary,
            @NotNull @Size(max = 20) List<@Size(max = 50) String> stack,
            @NotBlank @Size(max = 10_000) String background,
            @NotBlank @Size(max = 10_000) String responsibility,
            @NotNull @Size(max = 12) List<@NotBlank @Size(max = 300) String> workflow,
            @NotBlank @Size(max = 10_000) String architecture,
            @NotNull @Size(max = 12) List<@NotBlank @Size(max = 300) String> highlights,
            @NotBlank @Size(max = 10_000) String retrospective,
            @Size(max = 500) String repositoryUrl,
            @Size(max = 500) String liveUrl,
            boolean featured,
            @Min(0) @Max(10_000) int sortOrder) {}

    public record SearchResult(String type, String title, String summary, String url,
                               String meta, boolean external) {}

    public record MetricInput(
            @NotBlank @Size(max = 200) @Pattern(regexp = "/[a-zA-Z0-9/_-]*") String path,
            @DecimalMin("0.0") @DecimalMax("60000.0") Double renderMs) {}
    public record PageMetric(String path, long views, double averageRenderMs, double maxRenderMs) {}
    public record DailyMetric(LocalDate date, long views, long visitors) {}
    public record MetricsSummary(long viewsToday, long viewsThirtyDays, long visitorsToday, long visitorsThirtyDays,
                                 double averageRenderMs,
                                 List<PageMetric> topPages, List<DailyMetric> dailyViews) {}

    public record AiNews(long id, String title, String summary, String sourceName, String sourceUrl,
                         Instant publishedAt, List<String> topics) {}

    public record AiNewsInput(
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(max = 10_000) String summary,
            @NotBlank @Size(max = 100) String sourceName,
            @NotBlank @Size(max = 1000) String sourceUrl,
            @NotNull Instant publishedAt,
            @NotNull @Size(max = 12) List<@Size(max = 50) String> topics) {}

    public record LearningResource(long id, String slug, String title, String description, String level,
                                   String topic, String url, String content, int sortOrder) {}

    public record LearningResourceInput(
            @NotBlank @Size(max = 180) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 10_000) String description,
            @NotBlank @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED") String level,
            @NotBlank @Size(max = 100) String topic,
            @Size(max = 1000) String url,
            @Size(max = 200_000) String content,
            @Min(0) @Max(10_000) int sortOrder) {}

    public record GameAccount(long id, String platform, String handle, String url,
                              String description, int sortOrder) {}

    public record GameAccountInput(
            @NotBlank @Size(max = 80) String platform,
            @NotBlank @Size(max = 120) String handle,
            @NotBlank @Size(max = 1000) String url,
            @NotBlank @Size(max = 500) String description,
            @Min(0) @Max(10_000) int sortOrder) {}

    public record GameEntry(long id, String slug, String title, String coverUrl, String platform,
                            String status, Double score, int hoursPlayed, LocalDate completedOn,
                            String verdict, String reviewArticleSlug, int sortOrder) {}

    public record GameEntryInput(
            @NotBlank @Size(max = 180) @Pattern(regexp = "[a-z0-9-]+") String slug,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 1000) String coverUrl,
            @NotBlank @Size(max = 100) String platform,
            @NotBlank @Pattern(regexp = "PLAYING|COMPLETED|WISHLIST|DROPPED|LIBRARY") String status,
            @DecimalMin("0.0") @DecimalMax("10.0") Double score,
            @Min(0) @Max(100_000) int hoursPlayed,
            LocalDate completedOn,
            @NotBlank @Size(max = 1000) String verdict,
            @Size(max = 180) @Pattern(regexp = "[a-z0-9-]*") String reviewArticleSlug,
            @Min(0) @Max(10_000) int sortOrder) {}

    public record Experience(@NotBlank @Size(max = 160) String organization,
                             @NotBlank @Size(max = 120) String role,
                             @NotBlank @Size(max = 80) String period,
                             @NotBlank @Size(max = 5_000) String description) {}
    public record Education(@NotBlank @Size(max = 160) String institution,
                            @NotBlank @Size(max = 160) String major,
                            @NotBlank @Size(max = 80) String period) {}
    public record Resume(@NotBlank @Size(max = 120) String displayName,
                         @NotBlank @Size(max = 160) String headline,
                         @NotBlank @Size(max = 120) String location,
                         @NotBlank @Email @Size(max = 200) String email,
                         @NotBlank @Size(max = 5_000) String summary,
                         @NotNull @Size(max = 30) List<@NotBlank @Size(max = 50) String> skills,
                         @NotNull @Size(max = 20) List<@Valid Experience> experiences,
                         @NotNull @Size(max = 10) List<@Valid Education> education) {}

    public record LearningProgress(long id, long resourceId, String slug, String title, String topic,
                                   String level, String status, String note, int sortOrder) {}

    public record LearningProgressUpdate(@NotBlank @Pattern(regexp = "NOT_STARTED|IN_PROGRESS|DONE") String status,
                                         @Size(max = 500) String note) {}

    public record LearningLog(long id, int station, int section, LocalDate logDate, String title,
                              String content, List<String> tags, Instant createdAt) {}

    public record LearningLogInput(@Min(0) @Max(999) int station,
                                   @Min(1) @Max(999) int section,
                                   LocalDate logDate,
                                   @NotBlank @Size(max = 200) String title,
                                   @NotBlank @Size(max = 10_000) String content,
                                   @NotNull @Size(max = 12) List<@Size(max = 40) String> tags) {}

    public record NowProfile(
            @NotBlank @Size(max = 160) String headline,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}") String updatedAt,
            @NotNull @Size(max = 12) List<@NotBlank @Size(max = 200) String> building,
            @NotNull @Size(max = 12) List<@NotBlank @Size(max = 200) String> learning,
            @NotNull @Size(max = 12) List<@NotBlank @Size(max = 200) String> playing,
            @NotNull @Size(max = 12) List<@NotBlank @Size(max = 200) String> reading,
            @Size(max = 12) List<@Size(max = 200) String> outlook) {}
}
