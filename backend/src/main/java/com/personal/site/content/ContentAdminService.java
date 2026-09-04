package com.personal.site.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;

@Service
public class ContentAdminService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ContentAdminService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    public ContentModels.Message createMessage(ContentModels.MessageInput input) {
        Long id = jdbc.queryForObject("INSERT INTO messages(name,email,content) VALUES (?,?,?) RETURNING id", Long.class,
                input.name().trim(), input.email() == null ? "" : input.email().trim(), input.content().trim());
        return jdbc.queryForObject("SELECT * FROM messages WHERE id=?", messageMapper(), id);
    }

    public List<ContentModels.Message> messages() {
        return jdbc.query("SELECT * FROM messages ORDER BY pinned DESC, created_at DESC", messageMapper());
    }

    public List<ContentModels.PublicMessage> publicMessages(int limit) {
        return jdbc.query("""
                SELECT m.id, m.name, m.content, m.created_at, m.pinned,
                       COUNT(DISTINCT l.fingerprint) AS like_count,
                       (SELECT COUNT(*) FROM message_comments c WHERE c.message_id = m.id) AS comment_count
                FROM messages m
                LEFT JOIN message_likes l ON l.message_id = m.id
                GROUP BY m.id, m.name, m.content, m.created_at, m.pinned
                ORDER BY m.pinned DESC, m.created_at DESC
                LIMIT ?
                """, (rs, row) -> new ContentModels.PublicMessage(rs.getLong("id"), rs.getString("name"),
                rs.getString("content"), rs.getTimestamp("created_at").toInstant(), rs.getBoolean("pinned"),
                rs.getLong("like_count"), rs.getLong("comment_count")), limit);
    }

    public void deleteMessage(long id) { jdbc.update("DELETE FROM messages WHERE id=?", id); }

    public ContentModels.MessageLikeResult likeMessage(long messageId, String fingerprint) {
        requireMessage(messageId);
        jdbc.update("INSERT INTO message_likes(message_id, fingerprint) VALUES (?,?) ON CONFLICT DO NOTHING", messageId, fingerprint);
        return likeResult(messageId, fingerprint);
    }

    public ContentModels.MessageLikeResult unlikeMessage(long messageId, String fingerprint) {
        requireMessage(messageId);
        jdbc.update("DELETE FROM message_likes WHERE message_id=? AND fingerprint=?", messageId, fingerprint);
        return likeResult(messageId, fingerprint);
    }

    public List<ContentModels.MessageComment> messageComments(long messageId) {
        requireMessage(messageId);
        return jdbc.query("SELECT id, message_id, name, content, is_author, user_id, created_at FROM message_comments WHERE message_id=? ORDER BY created_at ASC, id ASC",
                messageCommentMapper(), messageId);
    }

    public ContentModels.MessageComment addMessageComment(long messageId, ContentModels.MessageCommentInput input, boolean isAuthor, Long userId) {
        requireMessage(messageId);
        Long id = jdbc.queryForObject("INSERT INTO message_comments(message_id, name, content, is_author, user_id) VALUES (?,?,?,?,?) RETURNING id",
                Long.class, messageId, input.name().trim(), input.content().trim(), isAuthor, userId);
        return jdbc.queryForObject("SELECT id, message_id, name, content, is_author, user_id, created_at FROM message_comments WHERE id=?",
                messageCommentMapper(), id);
    }

    private void requireMessage(long messageId) {
        List<Long> ids = jdbc.query("SELECT id FROM messages WHERE id=?", (rs, row) -> rs.getLong(1), messageId);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found");
    }

    private ContentModels.MessageLikeResult likeResult(long messageId, String fingerprint) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM message_likes WHERE message_id=?", Long.class, messageId);
        Long liked = jdbc.queryForObject("SELECT COUNT(*) FROM message_likes WHERE message_id=? AND fingerprint=?", Long.class, messageId, fingerprint);
        return new ContentModels.MessageLikeResult(messageId, count == null ? 0 : count, liked != null && liked > 0);
    }

    private RowMapper<ContentModels.MessageComment> messageCommentMapper() {
        return (rs, row) -> new ContentModels.MessageComment(rs.getLong("id"), rs.getLong("message_id"),
                rs.getString("name"), rs.getString("content"), rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("is_author"), rs.getObject("user_id") != null);
    }

    private RowMapper<ContentModels.Message> messageMapper() {
        return (rs, row) -> new ContentModels.Message(rs.getLong("id"), rs.getString("name"), rs.getString("email"),
                rs.getString("content"), rs.getTimestamp("created_at").toInstant());
    }

    public List<ContentModels.AdminProject> projects() {
        return jdbc.query("SELECT * FROM projects ORDER BY featured DESC, sort_order, id", projectMapper());
    }

    public ContentModels.AdminProject project(long id) {
        return one("SELECT * FROM projects WHERE id = ?", projectMapper(), id, "Project not found");
    }

    @Transactional
    public ContentModels.AdminProject createProject(ContentModels.ProjectInput input) {
        try {
            long id = insert("""
                    INSERT INTO projects(slug,name,summary,stack,background,responsibility,workflow,architecture,
                    highlights,retrospective,repository_url,live_url,featured,sort_order)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, statement -> bindProject(statement, input));
            return project(id);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project slug already exists");
        }
    }

    public ContentModels.AdminProject updateProject(long id, ContentModels.ProjectInput input) {
        try {
            requireChanged(jdbc.update("""
                    UPDATE projects SET slug=?,name=?,summary=?,stack=?,background=?,responsibility=?,workflow=?,
                    architecture=?,highlights=?,retrospective=?,repository_url=?,live_url=?,featured=?,sort_order=? WHERE id=?
                    """, input.slug(), input.name(), input.summary(), json(input.stack()), input.background(),
                    input.responsibility(), json(input.workflow()), input.architecture(), json(input.highlights()),
                    input.retrospective(), blankToNull(input.repositoryUrl()), blankToNull(input.liveUrl()),
                    input.featured(), input.sortOrder(), id), "Project not found");
            return project(id);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Project slug already exists");
        }
    }

    public void deleteProject(long id) {
        requireChanged(jdbc.update("DELETE FROM projects WHERE id = ?", id), "Project not found");
    }

    public List<ContentModels.LearningResource> learningResources() {
        return jdbc.query("SELECT * FROM learning_resources ORDER BY sort_order, id", learningMapper());
    }

    public ContentModels.LearningResource learningResource(long id) {
        return one("SELECT * FROM learning_resources WHERE id = ?", learningMapper(), id, "Learning resource not found");
    }

    @Transactional
    public ContentModels.LearningResource createLearningResource(ContentModels.LearningResourceInput input) {
        long id = insert("""
                INSERT INTO learning_resources(slug,title,description,level,topic,url,content,sort_order) VALUES (?,?,?,?,?,?,?,?)
                """, statement -> {
            statement.setString(1, input.slug());
            statement.setString(2, input.title());
            statement.setString(3, input.description());
            statement.setString(4, input.level());
            statement.setString(5, input.topic());
            statement.setString(6, blankToNull(input.url()));
            statement.setString(7, blankToNull(input.content()));
            statement.setInt(8, input.sortOrder());
        });
        return learningResource(id);
    }

    public ContentModels.LearningResource updateLearningResource(long id, ContentModels.LearningResourceInput input) {
        requireChanged(jdbc.update("""
                UPDATE learning_resources SET slug=?,title=?,description=?,level=?,topic=?,url=?,content=?,sort_order=? WHERE id=?
                """, input.slug(), input.title(), input.description(), input.level(), input.topic(),
                blankToNull(input.url()), blankToNull(input.content()), input.sortOrder(), id), "Learning resource not found");
        return learningResource(id);
    }

    public void deleteLearningResource(long id) {
        requireChanged(jdbc.update("DELETE FROM learning_resources WHERE id = ?", id), "Learning resource not found");
    }

    @Transactional
    public ContentModels.LearningLog createLearningLog(ContentModels.LearningLogInput input) {
        long id = insert("""
                INSERT INTO learning_logs(station,section,log_date,title,content,tags) VALUES (?,?,?,?,?,?)
                """, statement -> {
            statement.setInt(1, input.station());
            statement.setInt(2, input.section());
            statement.setObject(3, input.logDate());
            statement.setString(4, input.title().trim());
            statement.setString(5, input.content().trim());
            statement.setString(6, json(input.tags()));
        });
        return jdbc.queryForObject("SELECT * FROM learning_logs WHERE id = ?", learningLogMapper(), id);
    }

    public ContentModels.LearningLog updateLearningLog(long id, ContentModels.LearningLogInput input) {
        requireChanged(jdbc.update("""
                UPDATE learning_logs SET station=?, section=?, log_date=?, title=?, content=?, tags=?
                WHERE id=?
                """, input.station(), input.section(), input.logDate(), input.title().trim(),
                input.content().trim(), json(input.tags()), id), "Learning log not found");
        return jdbc.queryForObject("SELECT * FROM learning_logs WHERE id = ?", learningLogMapper(), id);
    }

    public void deleteLearningLog(long id) {
        requireChanged(jdbc.update("DELETE FROM learning_logs WHERE id = ?", id), "Learning log not found");
    }

    public List<ContentModels.LearningLog> learningLogs() {
        return jdbc.query("SELECT * FROM learning_logs ORDER BY station, section, id", learningLogMapper());
    }

    public ContentModels.LearningLog learningLog(long id) {
        return one("SELECT * FROM learning_logs WHERE id = ?", learningLogMapper(), id, "Learning log not found");
    }

    public ContentModels.LearningProgress updateLearningProgress(long id, ContentModels.LearningProgressUpdate input) {
        requireChanged(jdbc.update("""
                UPDATE learning_progress SET status=?, note=?, updated_at=CURRENT_TIMESTAMP WHERE id=?
                """, input.status(), input.note() == null ? "" : input.note().trim(), id), "Learning progress not found");
        return jdbc.queryForObject("""
                SELECT p.id, p.resource_id, r.slug, r.title, r.topic, r.level, p.status, p.note, p.sort_order
                FROM learning_progress p JOIN learning_resources r ON r.id = p.resource_id WHERE p.id = ?
                """, learningProgressMapper(), id);
    }

    private org.springframework.jdbc.core.RowMapper<ContentModels.LearningProgress> learningProgressMapper() {
        return (rs, row) -> new ContentModels.LearningProgress(rs.getLong("id"), rs.getLong("resource_id"),
                rs.getString("slug"), rs.getString("title"), rs.getString("topic"), rs.getString("level"), rs.getString("status"),
                rs.getString("note"), rs.getInt("sort_order"));
    }

    private org.springframework.jdbc.core.RowMapper<ContentModels.LearningLog> learningLogMapper() {
        return (rs, row) -> new ContentModels.LearningLog(rs.getLong("id"), rs.getInt("station"),
                rs.getInt("section"), (rs.getDate("log_date") == null ? null : rs.getDate("log_date").toLocalDate()), rs.getString("title"),
                rs.getString("content"), strings(rs.getString("tags")),
                rs.getTimestamp("created_at").toInstant());
    }

    public List<ContentModels.AiNews> aiNews() {
        return jdbc.query("SELECT * FROM ai_news ORDER BY published_at DESC, id DESC", aiNewsMapper());
    }

    public ContentModels.AiNews aiNewsItem(long id) {
        return one("SELECT * FROM ai_news WHERE id = ?", aiNewsMapper(), id, "AI news item not found");
    }

    @Transactional
    public ContentModels.AiNews createAiNews(ContentModels.AiNewsInput input) {
        try {
            jdbc.update("DELETE FROM ai_news_suppressions WHERE source_url = ?", input.sourceUrl());
            long id = insert("""
                    INSERT INTO ai_news(title,summary,source_name,source_url,published_at,topics) VALUES (?,?,?,?,?,?)
                    """, statement -> bindAiNews(statement, input));
            return aiNewsItem(id);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Source URL already exists");
        }
    }

    public ContentModels.AiNews updateAiNews(long id, ContentModels.AiNewsInput input) {
        try {
            jdbc.update("DELETE FROM ai_news_suppressions WHERE source_url = ?", input.sourceUrl());
            requireChanged(jdbc.update("""
                    UPDATE ai_news SET title=?,summary=?,source_name=?,source_url=?,published_at=?,topics=? WHERE id=?
                    """, input.title(), input.summary(), input.sourceName(), input.sourceUrl(),
                    Timestamp.from(input.publishedAt()), json(input.topics()), id), "AI news item not found");
            return aiNewsItem(id);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Source URL already exists");
        }
    }

    public void deleteAiNews(long id) {
        ContentModels.AiNews item = aiNewsItem(id);
        jdbc.update("INSERT INTO ai_news_suppressions(source_url) VALUES (?)", item.sourceUrl());
        requireChanged(jdbc.update("DELETE FROM ai_news WHERE id = ?", id), "AI news item not found");
    }

    public List<ContentModels.GameAccount> gameAccounts() {
        return jdbc.query("SELECT * FROM game_accounts ORDER BY sort_order, id", gameAccountMapper());
    }

    public ContentModels.GameAccount gameAccount(long id) {
        return one("SELECT * FROM game_accounts WHERE id = ?", gameAccountMapper(), id, "Game account not found");
    }

    @Transactional
    public ContentModels.GameAccount createGameAccount(ContentModels.GameAccountInput input) {
        long id = insert("""
                INSERT INTO game_accounts(platform,handle,url,description,sort_order) VALUES (?,?,?,?,?)
                """, statement -> {
            statement.setString(1, input.platform());
            statement.setString(2, input.handle());
            statement.setString(3, input.url());
            statement.setString(4, input.description());
            statement.setInt(5, input.sortOrder());
        });
        return gameAccount(id);
    }

    public ContentModels.GameAccount updateGameAccount(long id, ContentModels.GameAccountInput input) {
        requireChanged(jdbc.update("""
                UPDATE game_accounts SET platform=?,handle=?,url=?,description=?,sort_order=? WHERE id=?
                """, input.platform(), input.handle(), input.url(), input.description(), input.sortOrder(), id),
                "Game account not found");
        return gameAccount(id);
    }

    public void deleteGameAccount(long id) {
        requireChanged(jdbc.update("DELETE FROM game_accounts WHERE id = ?", id), "Game account not found");
    }

    public List<ContentModels.GameEntry> gameEntries() {
        return jdbc.query("""
                SELECT * FROM game_entries
                ORDER BY CASE WHEN status='PLAYING' THEN 0 ELSE 1 END,
                         hours_played DESC,
                         sort_order,
                         id
                """, gameEntryMapper());
    }

    public ContentModels.GameEntry gameEntry(long id) {
        return one("SELECT * FROM game_entries WHERE id=?", gameEntryMapper(), id, "Game entry not found");
    }

    public ContentModels.GameEntry gameEntry(String slug) {
        List<ContentModels.GameEntry> rows = jdbc.query("SELECT * FROM game_entries WHERE slug=?", gameEntryMapper(), slug);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game entry not found");
        return rows.getFirst();
    }

    @Transactional
    public ContentModels.GameEntry createGameEntry(ContentModels.GameEntryInput input) {
        try {
            long id = insert("""
                    INSERT INTO game_entries(slug,title,cover_url,platform,status,score,hours_played,completed_on,
                    verdict,review_article_slug,sort_order) VALUES (?,?,?,?,?,?,?,?,?,?,?)
                    """, statement -> bindGameEntry(statement, input));
            return gameEntry(id);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game slug already exists");
        }
    }

    public ContentModels.GameEntry updateGameEntry(long id, ContentModels.GameEntryInput input) {
        try {
            requireChanged(jdbc.update("""
                    UPDATE game_entries SET slug=?,title=?,cover_url=?,platform=?,status=?,score=?,hours_played=?,
                    completed_on=?,verdict=?,review_article_slug=?,sort_order=?,updated_at=CURRENT_TIMESTAMP WHERE id=?
                    """, input.slug(), input.title(), blankToNull(input.coverUrl()), input.platform(), input.status(),
                    input.score(), input.hoursPlayed(), input.completedOn(), input.verdict(),
                    blankToNull(input.reviewArticleSlug()), input.sortOrder(), id), "Game entry not found");
            return gameEntry(id);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game slug already exists");
        }
    }

    public void deleteGameEntry(long id) {
        requireChanged(jdbc.update("DELETE FROM game_entries WHERE id=?", id), "Game entry not found");
    }

    public ContentModels.Resume updateResume(ContentModels.Resume input) {
        int changed = jdbc.update("""
                UPDATE site_profiles SET profile_json=?,updated_at=CURRENT_TIMESTAMP WHERE profile_key='resume'
                """, json(input));
        if (changed == 0) {
            jdbc.update("INSERT INTO site_profiles(profile_key,profile_json) VALUES ('resume',?)", json(input));
        }
        return input;
    }

    public ContentModels.NowProfile nowProfile() {
        List<String> rows = jdbc.query("SELECT profile_json FROM site_profiles WHERE profile_key='now'",
                (rs, row) -> rs.getString(1));
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Now profile not configured");
        try {
            return objectMapper.readValue(rows.getFirst(), ContentModels.NowProfile.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Now profile data is invalid", exception);
        }
    }

    public ContentModels.NowProfile updateNowProfile(ContentModels.NowProfile input) {
        int changed = jdbc.update("""
                UPDATE site_profiles SET profile_json=?,updated_at=CURRENT_TIMESTAMP WHERE profile_key='now'
                """, json(input));
        if (changed == 0) {
            jdbc.update("INSERT INTO site_profiles(profile_key,profile_json) VALUES ('now',?)", json(input));
        }
        return input;
    }

    private void bindAiNews(PreparedStatement statement, ContentModels.AiNewsInput input) throws SQLException {
        statement.setString(1, input.title());
        statement.setString(2, input.summary());
        statement.setString(3, input.sourceName());
        statement.setString(4, input.sourceUrl());
        statement.setTimestamp(5, Timestamp.from(input.publishedAt()));
        statement.setString(6, json(input.topics()));
    }

    private void bindProject(PreparedStatement statement, ContentModels.ProjectInput input) throws SQLException {
        statement.setString(1, input.slug());
        statement.setString(2, input.name());
        statement.setString(3, input.summary());
        statement.setString(4, json(input.stack()));
        statement.setString(5, input.background());
        statement.setString(6, input.responsibility());
        statement.setString(7, json(input.workflow()));
        statement.setString(8, input.architecture());
        statement.setString(9, json(input.highlights()));
        statement.setString(10, input.retrospective());
        statement.setString(11, blankToNull(input.repositoryUrl()));
        statement.setString(12, blankToNull(input.liveUrl()));
        statement.setBoolean(13, input.featured());
        statement.setInt(14, input.sortOrder());
    }

    private void bindGameEntry(PreparedStatement statement, ContentModels.GameEntryInput input) throws SQLException {
        statement.setString(1, input.slug());
        statement.setString(2, input.title());
        statement.setString(3, blankToNull(input.coverUrl()));
        statement.setString(4, input.platform());
        statement.setString(5, input.status());
        if (input.score() == null) statement.setNull(6, java.sql.Types.NUMERIC);
        else statement.setDouble(6, input.score());
        statement.setInt(7, input.hoursPlayed());
        if (input.completedOn() == null) statement.setNull(8, java.sql.Types.DATE);
        else statement.setDate(8, java.sql.Date.valueOf(input.completedOn()));
        statement.setString(9, input.verdict());
        statement.setString(10, blankToNull(input.reviewArticleSlug()));
        statement.setInt(11, input.sortOrder());
    }

    private long insert(String sql, StatementBinder binder) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            binder.bind(statement);
            return statement;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    private <T> T one(String sql, RowMapper<T> mapper, long id, String message) {
        List<T> rows = jdbc.query(sql, mapper, id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
        return rows.getFirst();
    }

    private void requireChanged(int changed, String message) {
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }

    private RowMapper<ContentModels.AdminProject> projectMapper() {
        return (rs, row) -> new ContentModels.AdminProject(rs.getLong("id"), rs.getString("slug"),
                rs.getString("name"), rs.getString("summary"), strings(rs.getString("stack")),
                rs.getString("background"), rs.getString("responsibility"), strings(rs.getString("workflow")),
                rs.getString("architecture"), strings(rs.getString("highlights")), rs.getString("retrospective"),
                rs.getString("repository_url"), rs.getString("live_url"), rs.getBoolean("featured"),
                rs.getInt("sort_order"));
    }

    private RowMapper<ContentModels.LearningResource> learningMapper() {
        return (rs, row) -> new ContentModels.LearningResource(rs.getLong("id"), rs.getString("slug"),
                rs.getString("title"), rs.getString("description"), rs.getString("level"), rs.getString("topic"),
                rs.getString("url"), rs.getString("content"), rs.getInt("sort_order"));
    }

    private RowMapper<ContentModels.AiNews> aiNewsMapper() {
        return (rs, row) -> new ContentModels.AiNews(rs.getLong("id"), rs.getString("title"),
                rs.getString("summary"), rs.getString("source_name"), rs.getString("source_url"),
                rs.getTimestamp("published_at").toInstant(), strings(rs.getString("topics")));
    }

    private RowMapper<ContentModels.GameAccount> gameAccountMapper() {
        return (rs, row) -> new ContentModels.GameAccount(rs.getLong("id"), rs.getString("platform"),
                rs.getString("handle"), rs.getString("url"), rs.getString("description"), rs.getInt("sort_order"));
    }

    private RowMapper<ContentModels.GameEntry> gameEntryMapper() {
        return (rs, row) -> new ContentModels.GameEntry(rs.getLong("id"), rs.getString("slug"),
                rs.getString("title"), rs.getString("cover_url"), rs.getString("platform"),
                rs.getString("status"), rs.getObject("score") == null ? null : rs.getDouble("score"),
                rs.getInt("hours_played"), rs.getDate("completed_on") == null ? null : rs.getDate("completed_on").toLocalDate(),
                rs.getString("verdict"), rs.getString("review_article_slug"), rs.getInt("sort_order"));
    }

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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
