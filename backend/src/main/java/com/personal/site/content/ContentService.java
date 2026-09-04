package com.personal.site.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ContentService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ContentService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Cacheable(cacheNames = "articleList", key = "{#query, #category, #excludeCategory}")
    public List<ContentModels.ArticleSummary> publishedArticles(String query, String category, String excludeCategory) {
        String search = query == null ? "" : query.trim();
        String categoryFilter = category == null ? "" : category.trim();
        String excludedCategory = excludeCategory == null ? "" : excludeCategory.trim();
        return jdbc.query("""
                SELECT * FROM articles
                WHERE status = 'PUBLISHED'
                  AND (? = '' OR category = ?)
                  AND (? = '' OR category <> ?)
                  AND (? = '' OR LOWER(title) LIKE LOWER(?) OR LOWER(excerpt) LIKE LOWER(?) OR LOWER(category) LIKE LOWER(?) OR LOWER(tags) LIKE LOWER(?))
                ORDER BY published_at DESC
                """, articleSummaryMapper(), categoryFilter, categoryFilter,
                excludedCategory, excludedCategory,
                search, like(search), like(search), like(search), like(search));
    }

    @Cacheable(cacheNames = "articleBySlug", key = "#slug")
    public ContentModels.Article publishedArticle(String slug) {
        List<ContentModels.Article> rows = jdbc.query(
                "SELECT * FROM articles WHERE slug = ? AND status = 'PUBLISHED'", articleMapper(), slug);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        return rows.getFirst();
    }

    @Cacheable(cacheNames = "articleNeighbors", key = "#slug")
    public ContentModels.ArticleNeighbors articleNeighbors(String slug) {
        ContentModels.Article current = publishedArticle(slug);
        Instant at = current.publishedAt();
        Timestamp atTimestamp = at == null ? null : Timestamp.from(at);
        long id = current.id();
        // 上一篇 / 下一篇只在同一板块内按时间排序：技术板块 = 工程实践 + 项目复盘等（非杂谈非 AI），
        // 游戏板块 = 杂谈，AI 板块 = AI 学习。只有到板块边界时前端才会提示跳转其他板块。
        String scope = sectionScope(current.category());
        List<ContentModels.ArticleSummary> prev = jdbc.query(
                "SELECT * FROM articles WHERE status = 'PUBLISHED' AND " + scope +
                " AND (published_at < ? OR (published_at = ? AND id < ?))" +
                " ORDER BY published_at DESC, id DESC LIMIT 1",
                articleSummaryMapper(), atTimestamp, atTimestamp, id);
        List<ContentModels.ArticleSummary> next = jdbc.query(
                "SELECT * FROM articles WHERE status = 'PUBLISHED' AND " + scope +
                " AND (published_at > ? OR (published_at = ? AND id > ?))" +
                " ORDER BY published_at ASC, id ASC LIMIT 1",
                articleSummaryMapper(), atTimestamp, atTimestamp, id);
        return new ContentModels.ArticleNeighbors(
                prev.isEmpty() ? null : prev.getFirst(),
                next.isEmpty() ? null : next.getFirst());
    }

    /** 根据文章分类返回所在板块的 SQL 条件，与技术 / 游戏 / AI 三个文章板块保持一致。 */
    private String sectionScope(String category) {
        if ("杂谈".equals(category)) {
            return "category = '杂谈'";
        }
        if ("AI 学习".equals(category)) {
            return "category = 'AI 学习'";
        }
        return "category NOT IN ('杂谈', 'AI 学习')";
    }

    @Cacheable(cacheNames = "articleRelated", key = "#slug")
    public List<ContentModels.ArticleSummary> relatedArticles(String slug) {
        ContentModels.Article current = publishedArticle(slug);
        List<ContentModels.ArticleSummary> sameCategory = jdbc.query("""
                SELECT * FROM articles WHERE status = 'PUBLISHED' AND slug <> ? AND category = ?
                ORDER BY published_at DESC LIMIT 3""", articleSummaryMapper(), slug, current.category());
        if (sameCategory.size() >= 3) return sameCategory;
        List<Long> excludedIds = sameCategory.stream().map(ContentModels.ArticleSummary::id).toList();
        String exclusion = excludedIds.isEmpty() ? "" : " AND id NOT IN (" + excludedIds.stream()
                .map(String::valueOf).collect(Collectors.joining(",")) + ")";
        int remaining = 3 - sameCategory.size();
        List<ContentModels.ArticleSummary> fill = jdbc.query("""
                SELECT * FROM articles WHERE status = 'PUBLISHED' AND slug <> ?""" + exclusion + """
                ORDER BY published_at DESC LIMIT ?""", articleSummaryMapper(), slug, remaining);
        List<ContentModels.ArticleSummary> result = new ArrayList<>(sameCategory);
        result.addAll(fill);
        return result;
    }

    public List<ContentModels.Article> allArticles() {
        return jdbc.query("SELECT * FROM articles ORDER BY updated_at DESC", articleMapper());
    }

    public ContentModels.Article article(long id) {
        List<ContentModels.Article> rows = jdbc.query("SELECT * FROM articles WHERE id = ?", articleMapper(), id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
        return rows.getFirst();
    }

    @Transactional
    @CacheEvict(cacheNames = {"articleList", "articleBySlug", "articleNeighbors", "articleRelated"}, allEntries = true)
    public ContentModels.Article copyArticle(long id) {
        ContentModels.Article source = article(id);
        String slug = source.slug() + "-copy";
        int suffix = 2;
        while (!jdbc.queryForObject("SELECT COUNT(*) FROM articles WHERE slug = ?", Integer.class, slug).equals(0)) {
            slug = source.slug() + "-copy-" + suffix++;
        }
        return createArticle(new ContentModels.ArticleInput(slug, source.title() + "（副本）", source.excerpt(),
                source.category(), source.tags(), source.content(), false, source.readingMinutes()));
    }

    @Transactional
    @CacheEvict(cacheNames = {"articleList", "articleBySlug", "articleNeighbors", "articleRelated"}, allEntries = true)
    public ContentModels.Article createArticle(ContentModels.ArticleInput input) {
        KeyHolder keys = new GeneratedKeyHolder();
        try {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO articles(slug,title,excerpt,category,tags,content,status,reading_minutes,published_at,updated_at)
                        VALUES (?,?,?,?,?,?,?,?,CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END,CURRENT_TIMESTAMP)
                        """, new String[]{"id"});
                bindArticle(statement, input);
                statement.setBoolean(9, input.published());
                return statement;
            }, keys);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Article slug already exists");
        }
        long id = Objects.requireNonNull(keys.getKey()).longValue();
        saveArticleVersion(id);
        return article(id);
    }

    @Transactional
    @CacheEvict(cacheNames = {"articleList", "articleBySlug", "articleNeighbors", "articleRelated"}, allEntries = true)
    public ContentModels.Article updateArticle(long id, ContentModels.ArticleInput input) {
        try {
            int changed = jdbc.update("""
                    UPDATE articles SET slug=?,title=?,excerpt=?,category=?,tags=?,content=?,status=?,reading_minutes=?,
                    published_at=CASE WHEN ? THEN COALESCE(published_at,CURRENT_TIMESTAMP) ELSE NULL END,updated_at=CURRENT_TIMESTAMP
                    WHERE id=?
                    """, input.slug(), input.title(), input.excerpt(), input.category(), json(input.tags()), input.content(),
                    input.published() ? "PUBLISHED" : "DRAFT", input.readingMinutes(), input.published(), id);
            if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
            saveArticleVersion(id);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Article slug already exists");
        }
        return article(id);
    }

    @CacheEvict(cacheNames = {"articleList", "articleBySlug", "articleNeighbors", "articleRelated"}, allEntries = true)
    public void deleteArticle(long id) {
        if (jdbc.update("DELETE FROM articles WHERE id = ?", id) == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
    }

    public List<ContentModels.ArticleVersion> articleVersions(long articleId) {
        article(articleId);
        return jdbc.query("SELECT * FROM article_versions WHERE article_id=? ORDER BY created_at DESC,id DESC",
                articleVersionMapper(), articleId);
    }

    @Transactional
    @CacheEvict(cacheNames = {"articleList", "articleBySlug", "articleNeighbors", "articleRelated"}, allEntries = true)
    public ContentModels.Article restoreArticleVersion(long articleId, long versionId) {
        List<ContentModels.ArticleVersion> rows = jdbc.query(
                "SELECT * FROM article_versions WHERE id=? AND article_id=?", articleVersionMapper(), versionId, articleId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article version not found");
        ContentModels.ArticleVersion version = rows.getFirst();
        try {
            int changed = jdbc.update("""
                    UPDATE articles SET slug=?,title=?,excerpt=?,category=?,tags=?,content=?,status=?,reading_minutes=?,
                    published_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?
                    """, version.slug(), version.title(), version.excerpt(), version.category(), json(version.tags()),
                    version.content(), version.published() ? "PUBLISHED" : "DRAFT", version.readingMinutes(),
                    version.publishedAt() == null ? null : Timestamp.from(version.publishedAt()), articleId);
            if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Article not found");
            saveArticleVersion(articleId);
            return article(articleId);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Article slug already exists");
        }
    }

    public List<ContentModels.Project> projects() {
        return jdbc.query("SELECT * FROM projects ORDER BY featured DESC, sort_order, id", projectMapper());
    }

    public ContentModels.Project project(String slug) {
        List<ContentModels.Project> rows = jdbc.query("SELECT * FROM projects WHERE slug = ?", projectMapper(), slug);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found");
        return rows.getFirst();
    }

    public List<ContentModels.SearchResult> search(String query) {
        String term = query == null ? "" : query.trim();
        if (term.isEmpty()) return List.of();
        if (term.length() > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query too long");
        String pattern = like(term);
        List<ContentModels.SearchResult> results = new ArrayList<>();
        results.addAll(jdbc.query("""
                SELECT title,excerpt,slug,category FROM articles WHERE status='PUBLISHED'
                AND (LOWER(title) LIKE LOWER(?) OR LOWER(excerpt) LIKE LOWER(?) OR LOWER(tags) LIKE LOWER(?))
                ORDER BY published_at DESC LIMIT 10
                """, (rs, row) -> new ContentModels.SearchResult("ARTICLE", rs.getString("title"),
                rs.getString("excerpt"), "/blog/" + rs.getString("slug"), rs.getString("category"), false),
                pattern, pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT name,summary,slug FROM projects WHERE LOWER(name) LIKE LOWER(?)
                OR LOWER(summary) LIKE LOWER(?) OR LOWER(stack) LIKE LOWER(?) ORDER BY featured DESC,sort_order LIMIT 10
                """, (rs, row) -> new ContentModels.SearchResult("PROJECT", rs.getString("name"),
                rs.getString("summary"), "/projects/" + rs.getString("slug"), "项目案例", false),
                pattern, pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT title,description,topic FROM learning_resources WHERE LOWER(title) LIKE LOWER(?)
                OR LOWER(description) LIKE LOWER(?) OR LOWER(topic) LIKE LOWER(?) ORDER BY sort_order LIMIT 10
                """, (rs, row) -> new ContentModels.SearchResult("LEARNING", rs.getString("title"),
                rs.getString("description"), "/ai", rs.getString("topic"), false), pattern, pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT title,summary,source_name,source_url FROM ai_news WHERE LOWER(title) LIKE LOWER(?)
                OR LOWER(summary) LIKE LOWER(?) OR LOWER(source_name) LIKE LOWER(?) ORDER BY published_at DESC LIMIT 10
                """, (rs, row) -> new ContentModels.SearchResult("AI_NEWS", rs.getString("title"),
                rs.getString("summary"), rs.getString("source_url"), rs.getString("source_name"), true),
                pattern, pattern, pattern));
        results.addAll(jdbc.query("""
                SELECT title,verdict,slug,platform FROM game_entries WHERE LOWER(title) LIKE LOWER(?)
                OR LOWER(verdict) LIKE LOWER(?) OR LOWER(platform) LIKE LOWER(?) ORDER BY sort_order LIMIT 10
                """, (rs, row) -> new ContentModels.SearchResult("GAME", rs.getString("title"),
                rs.getString("verdict"), "/games/" + rs.getString("slug"), rs.getString("platform"), false),
                pattern, pattern, pattern));
        return results.stream().limit(30).toList();
    }

    public List<ContentModels.AiNews> aiNews() {
        return jdbc.query("SELECT * FROM ai_news ORDER BY published_at DESC LIMIT 24", (rs, row) -> new ContentModels.AiNews(
                rs.getLong("id"), rs.getString("title"), rs.getString("summary"), rs.getString("source_name"),
                rs.getString("source_url"), instant(rs.getTimestamp("published_at")), strings(rs.getString("topics"))));
    }

    public List<ContentModels.LearningResource> learningResources() {
        return jdbc.query("SELECT * FROM learning_resources ORDER BY sort_order, id",
                (rs, row) -> new ContentModels.LearningResource(rs.getLong("id"), rs.getString("slug"),
                        rs.getString("title"), rs.getString("description"), rs.getString("level"),
                        rs.getString("topic"), rs.getString("url"), rs.getString("content"), rs.getInt("sort_order")));
    }

    public ContentModels.LearningResource learningResource(String slug) {
        List<ContentModels.LearningResource> rows = jdbc.query(
                "SELECT * FROM learning_resources WHERE slug = ?",
                (rs, row) -> new ContentModels.LearningResource(rs.getLong("id"), rs.getString("slug"),
                        rs.getString("title"), rs.getString("description"), rs.getString("level"),
                        rs.getString("topic"), rs.getString("url"), rs.getString("content"), rs.getInt("sort_order")),
                slug);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning resource not found");
        return rows.getFirst();
    }

    public List<ContentModels.LearningProgress> learningProgress() {
        return jdbc.query("""
                SELECT p.id, p.resource_id, r.slug, r.title, r.topic, r.level, p.status, p.note, p.sort_order
                FROM learning_progress p
                JOIN learning_resources r ON r.id = p.resource_id
                ORDER BY p.sort_order, p.id
                """, (rs, row) -> new ContentModels.LearningProgress(rs.getLong("id"), rs.getLong("resource_id"),
                rs.getString("slug"), rs.getString("title"), rs.getString("topic"), rs.getString("level"), rs.getString("status"),
                rs.getString("note"), rs.getInt("sort_order")));
    }

    public List<ContentModels.LearningLog> learningLogs(int limit) {
        return jdbc.query("SELECT * FROM learning_logs ORDER BY station, section, id LIMIT ?",
                (rs, row) -> new ContentModels.LearningLog(rs.getLong("id"), rs.getInt("station"),
                        rs.getInt("section"), (rs.getDate("log_date") == null ? null : rs.getDate("log_date").toLocalDate()), rs.getString("title"),
                        rs.getString("content"), strings(rs.getString("tags")),
                        rs.getTimestamp("created_at").toInstant()),
                Math.min(Math.max(limit, 1), 200));
    }

    public ContentModels.LearningLog learningLog(long id) {
        List<ContentModels.LearningLog> rows = jdbc.query("SELECT * FROM learning_logs WHERE id = ?",
                (rs, row) -> new ContentModels.LearningLog(rs.getLong("id"), rs.getInt("station"),
                        rs.getInt("section"), (rs.getDate("log_date") == null ? null : rs.getDate("log_date").toLocalDate()), rs.getString("title"),
                        rs.getString("content"), strings(rs.getString("tags")),
                        rs.getTimestamp("created_at").toInstant()), id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Learning log not found");
        return rows.getFirst();
    }

    public ContentModels.Resume resume() {
        List<String> rows = jdbc.query("SELECT profile_json FROM site_profiles WHERE profile_key='resume'",
                (rs, row) -> rs.getString(1));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resume not configured");
        try {
            return objectMapper.readValue(rows.getFirst(), ContentModels.Resume.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Resume data is invalid", exception);
        }
    }

    private RowMapper<ContentModels.ArticleSummary> articleSummaryMapper() {
        return (rs, row) -> new ContentModels.ArticleSummary(rs.getLong("id"), rs.getString("slug"),
                rs.getString("title"), rs.getString("excerpt"), rs.getString("category"),
                strings(rs.getString("tags")), instant(rs.getTimestamp("published_at")), rs.getInt("reading_minutes"));
    }

    private RowMapper<ContentModels.Project> projectMapper() {
        return (rs, row) -> new ContentModels.Project(rs.getLong("id"), rs.getString("slug"),
                rs.getString("name"), rs.getString("summary"), strings(rs.getString("stack")),
                rs.getString("background"), rs.getString("responsibility"), strings(rs.getString("workflow")),
                rs.getString("architecture"), strings(rs.getString("highlights")), rs.getString("retrospective"),
                rs.getString("repository_url"), rs.getString("live_url"), rs.getBoolean("featured"));
    }

    private RowMapper<ContentModels.Article> articleMapper() {
        return (rs, row) -> new ContentModels.Article(rs.getLong("id"), rs.getString("slug"), rs.getString("title"),
                rs.getString("excerpt"), rs.getString("category"), strings(rs.getString("tags")), rs.getString("content"),
                instant(rs.getTimestamp("published_at")), rs.getInt("reading_minutes"), "PUBLISHED".equals(rs.getString("status")));
    }

    private RowMapper<ContentModels.ArticleVersion> articleVersionMapper() {
        return (rs, row) -> new ContentModels.ArticleVersion(rs.getLong("id"), rs.getLong("article_id"),
                rs.getString("slug"), rs.getString("title"), rs.getString("excerpt"), rs.getString("category"),
                strings(rs.getString("tags")), rs.getString("content"), instant(rs.getTimestamp("published_at")),
                rs.getInt("reading_minutes"), "PUBLISHED".equals(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant());
    }

    private void saveArticleVersion(long articleId) {
        jdbc.update("""
                INSERT INTO article_versions(article_id,slug,title,excerpt,category,tags,content,status,reading_minutes,published_at)
                SELECT id,slug,title,excerpt,category,tags,content,status,reading_minutes,published_at
                FROM articles WHERE id=?
                """, articleId);
    }

    private void bindArticle(PreparedStatement statement, ContentModels.ArticleInput input) throws java.sql.SQLException {
        statement.setString(1, input.slug());
        statement.setString(2, input.title());
        statement.setString(3, input.excerpt());
        statement.setString(4, input.category());
        statement.setString(5, json(input.tags()));
        statement.setString(6, input.content());
        statement.setString(7, input.published() ? "PUBLISHED" : "DRAFT");
        statement.setInt(8, input.readingMinutes());
    }

    private String like(String value) { return "%" + value + "%"; }
    private Instant instant(Timestamp value) { return value == null ? null : value.toInstant(); }

    private List<String> strings(String source) {
        try {
            return objectMapper.readValue(source, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Unable to encode JSON", exception);
        }
    }
}
