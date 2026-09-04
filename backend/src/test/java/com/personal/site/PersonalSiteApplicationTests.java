package com.personal.site;

import com.personal.site.content.ContentModels;
import com.personal.site.content.ContentAdminService;
import com.personal.site.content.ContentService;
import com.personal.site.content.SiteMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PersonalSiteApplicationTests {
    @Autowired
    private ContentService content;
    @Autowired
    private ContentAdminService adminContent;
    @Autowired
    private SiteMetricsService metrics;

    @Test
    void contextLoads() {
        metrics.record(new ContentModels.MetricInput("/test", 123.0), "127.0.0.1", "test-agent/1");
        metrics.record(new ContentModels.MetricInput("/test", 77.0), "127.0.0.1", "test-agent/1");
        metrics.record(new ContentModels.MetricInput("/test", 100.0), "10.0.0.2", "other-agent/2");
        ContentModels.MetricsSummary summary = metrics.summary();
        assertThat(summary.viewsToday()).isEqualTo(3);
        assertThat(summary.visitorsToday()).isEqualTo(2);
        assertThat(summary.averageRenderMs()).isEqualTo(100.0);
        assertThat(summary.topPages()).extracting(ContentModels.PageMetric::path).contains("/test");
    }

    @Test
    @Transactional
    void createsAndFiltersTalkArticle() {
        ContentModels.Article created = content.createArticle(new ContentModels.ArticleInput(
                "test-game-review", "测试游戏锐评", "验证文章创建和杂谈分类筛选。", "杂谈",
                List.of("游戏锐评"), "测试正文", true, 3));

        assertThat(created.id()).isPositive();
        assertThat(created.category()).isEqualTo("杂谈");
        assertThat(content.articleVersions(created.id())).hasSize(1);
        content.updateArticle(created.id(), new ContentModels.ArticleInput(
                "test-game-review", "修改后的游戏锐评", "验证版本记录。", "杂谈",
                List.of("游戏锐评"), "修改后的正文", true, 4));
        assertThat(content.articleVersions(created.id())).hasSize(2);
        ContentModels.Article restored = content.restoreArticleVersion(
                created.id(), content.articleVersions(created.id()).getLast().id());
        assertThat(restored.title()).isEqualTo("测试游戏锐评");
        assertThat(content.articleVersions(created.id())).hasSize(3);
        assertThat(content.publishedArticles("", "杂谈", ""))
                .extracting(ContentModels.ArticleSummary::slug)
                .contains("test-game-review");
        assertThat(content.publishedArticles("", "", "杂谈"))
                .extracting(ContentModels.ArticleSummary::slug)
                .doesNotContain("test-game-review");
        assertThat(content.search("游戏锐评"))
                .extracting(ContentModels.SearchResult::url)
                .contains("/blog/test-game-review");
    }

    @Test
    @Transactional
    void managesStructuredSiteContent() {
        ContentModels.AdminProject project = adminContent.createProject(new ContentModels.ProjectInput(
                "test-project", "Test project", "Initial summary", List.of("Vue"),
                "Test background", "Test responsibility", List.of("Create", "Verify"),
                "Test architecture", List.of("Typed API"), "Initial retrospective",
                "https://example.com/repo", "", true, 9));
        project = adminContent.updateProject(project.id(), new ContentModels.ProjectInput(
                "test-project-updated", "Updated project", "Updated summary", List.of("Vue", "Java"),
                "Updated background", "Updated responsibility", List.of("Create", "Review", "Verify"),
                "Updated architecture", List.of("Typed API", "Validation"), "Updated retrospective",
                "", "https://example.com", false, 2));
        assertThat(project.name()).isEqualTo("Updated project");
        assertThat(project.slug()).isEqualTo("test-project-updated");
        assertThat(project.highlights()).containsExactly("Typed API", "Validation");
        assertThat(project.sortOrder()).isEqualTo(2);
        assertThat(content.project("test-project-updated").workflow()).containsExactly("Create", "Review", "Verify");
        assertThat(content.search("Updated project"))
                .extracting(ContentModels.SearchResult::url)
                .contains("/projects/test-project-updated");

        ContentModels.LearningResource resource = adminContent.createLearningResource(
                new ContentModels.LearningResourceInput("test-learning", "Test learning", "Description", "BEGINNER", "Testing", "", "", 7));
        resource = adminContent.updateLearningResource(resource.id(),
                new ContentModels.LearningResourceInput("test-learning-updated", "Updated learning", "Updated", "INTERMEDIATE", "Testing", "https://example.com/learn", "## 教程正文", 3));
        assertThat(resource.level()).isEqualTo("INTERMEDIATE");
        assertThat(resource.slug()).isEqualTo("test-learning-updated");
        assertThat(content.learningResource("test-learning-updated").content()).contains("教程正文");

        ContentModels.AiNews news = adminContent.createAiNews(new ContentModels.AiNewsInput(
                "Test AI news", "Summary", "Test source", "https://example.com/news-test", Instant.parse("2026-08-29T00:00:00Z"), List.of("AI")));
        news = adminContent.updateAiNews(news.id(), new ContentModels.AiNewsInput(
                "Updated AI news", "Updated summary", "Test source", "https://example.com/news-test", Instant.parse("2026-08-29T01:00:00Z"), List.of("AI", "Test")));
        assertThat(news.title()).isEqualTo("Updated AI news");

        ContentModels.GameAccount account = adminContent.createGameAccount(new ContentModels.GameAccountInput(
                "Steam", "Cls", "https://example.com/profile", "Test account", 1));
        account = adminContent.updateGameAccount(account.id(), new ContentModels.GameAccountInput(
                "Steam", "Co11ap5e", "https://example.com/profile", "Updated account", 2));
        assertThat(account.handle()).isEqualTo("Co11ap5e");

        ContentModels.GameEntry game = adminContent.createGameEntry(new ContentModels.GameEntryInput(
                "test-game", "Test Game", "", "PC", "PLAYING", 8.5, 12, null,
                "Test verdict", "", 1));
        game = adminContent.updateGameEntry(game.id(), new ContentModels.GameEntryInput(
                "test-game", "Test Game", "", "PC", "COMPLETED", 9.0, 20,
                LocalDate.parse("2026-08-29"), "Completed verdict", "test-game-review", 1));
        assertThat(game.status()).isEqualTo("COMPLETED");
        assertThat(adminContent.gameEntry("test-game").score()).isEqualTo(9.0);

        long projectId = project.id();
        long resourceId = resource.id();
        long newsId = news.id();
        long accountId = account.id();
        long gameId = game.id();

        ContentModels.Resume updatedResume = adminContent.updateResume(new ContentModels.Resume(
                "Test Name", "Developer", "Ningbo", "test@example.com", "Test summary",
                List.of("Java"), List.of(), List.of()));
        assertThat(updatedResume.displayName()).isEqualTo("Test Name");

        ContentModels.NowProfile now = adminContent.updateNowProfile(new ContentModels.NowProfile(
                "Testing now page", "2026-08-29", List.of("Building"), List.of("Learning"), List.of(), List.of(), List.of("Outlook")));
        assertThat(now.headline()).isEqualTo("Testing now page");
        assertThat(now.outlook()).containsExactly("Outlook");
        assertThat(adminContent.nowProfile().building()).containsExactly("Building");

        adminContent.deleteProject(projectId);
        adminContent.deleteLearningResource(resourceId);
        adminContent.deleteAiNews(newsId);
        adminContent.deleteGameAccount(accountId);
        adminContent.deleteGameEntry(gameId);
        assertThat(adminContent.projects()).noneMatch(item -> item.id() == projectId);
        assertThat(adminContent.learningResources()).noneMatch(item -> item.id() == resourceId);
        assertThat(adminContent.aiNews()).noneMatch(item -> item.id() == newsId);
        assertThat(adminContent.gameAccounts()).noneMatch(item -> item.id() == accountId);
        assertThat(adminContent.gameEntries()).noneMatch(item -> item.id() == gameId);
    }
}
