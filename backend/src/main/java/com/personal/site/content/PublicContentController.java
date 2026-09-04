package com.personal.site.content;

import jakarta.validation.Valid;
import com.personal.site.auth.AdminSessionService;
import com.personal.site.user.UserAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicContentController {
    private final ContentService content;
    private final ContentAdminService adminContent;
    private final MessageRateLimiter messageRateLimiter;
    private final AdminSessionService adminSessions;
    private final UserAuthService userAuth;

    public PublicContentController(ContentService content, ContentAdminService adminContent,
                                   MessageRateLimiter messageRateLimiter, AdminSessionService adminSessions,
                                   UserAuthService userAuth) {
        this.content = content;
        this.adminContent = adminContent;
        this.messageRateLimiter = messageRateLimiter;
        this.adminSessions = adminSessions;
        this.userAuth = userAuth;
    }

    @GetMapping("/articles")
    public List<ContentModels.ArticleSummary> articles(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String excludeCategory) {
        return content.publishedArticles(q, category, excludeCategory);
    }

    @GetMapping("/articles/{slug}")
    public ContentModels.Article article(@PathVariable String slug) { return content.publishedArticle(slug); }

    @GetMapping("/articles/{slug}/adjacent")
    public ContentModels.ArticleNeighbors adjacent(@PathVariable String slug) { return content.articleNeighbors(slug); }

    @GetMapping("/articles/{slug}/related")
    public List<ContentModels.ArticleSummary> related(@PathVariable String slug) { return content.relatedArticles(slug); }

    @GetMapping("/projects")
    public List<ContentModels.Project> projects() { return content.projects(); }

    @GetMapping("/projects/{slug}")
    public ContentModels.Project project(@PathVariable String slug) { return content.project(slug); }

    @GetMapping("/games")
    public List<ContentModels.GameEntry> games() { return adminContent.gameEntries(); }

    @GetMapping("/games/{slug}")
    public ContentModels.GameEntry game(@PathVariable String slug) { return adminContent.gameEntry(slug); }

    @GetMapping("/search")
    public List<ContentModels.SearchResult> search(@RequestParam(defaultValue = "") String q) {
        return content.search(q);
    }

    @GetMapping("/ai/news")
    public List<ContentModels.AiNews> news() { return content.aiNews(); }

    @GetMapping("/ai/learning")
    public List<ContentModels.LearningResource> learning() { return content.learningResources(); }

    @GetMapping("/ai/learning/{slug}")
    public ContentModels.LearningResource learningItem(@PathVariable String slug) { return content.learningResource(slug); }

    @GetMapping("/learning/progress")
    public List<ContentModels.LearningProgress> learningProgress() { return content.learningProgress(); }

    @GetMapping("/learning/logs")
    public List<ContentModels.LearningLog> learningLogs(@RequestParam(defaultValue = "100") int limit) {
        return content.learningLogs(limit);
    }

    @PostMapping("/learning/logs")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.LearningLog addLearningLog(@Valid @RequestBody ContentModels.LearningLogInput input,
                                                    HttpServletRequest request) {
        requireOwner(request);
        return adminContent.createLearningLog(input);
    }

    @PutMapping("/learning/progress/{id}")
    public ContentModels.LearningProgress updateLearningProgress(@PathVariable long id,
                                                                 @Valid @RequestBody ContentModels.LearningProgressUpdate input,
                                                                 HttpServletRequest request) {
        requireOwner(request);
        return adminContent.updateLearningProgress(id, input);
    }

    private void requireOwner(HttpServletRequest request) {
        boolean isAdmin = adminSessions.findFromRequest(request).isPresent();
        Long userId = userAuth.userIdFromRequest(request);
        boolean isOwner = userId != null && userAuth.isOwner(userId);
        if (!isAdmin && !isOwner)
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Only the site owner can edit");
    }

    @GetMapping("/resume")
    public ContentModels.Resume resume() { return content.resume(); }

    @GetMapping("/game-accounts")
    public List<ContentModels.GameAccount> gameAccounts() { return adminContent.gameAccounts(); }

    @GetMapping("/now")
    public ContentModels.NowProfile nowProfile() { return adminContent.nowProfile(); }

    @GetMapping("/messages")
    public List<ContentModels.PublicMessage> publicMessages(@RequestParam(defaultValue = "20") int limit) {
        return adminContent.publicMessages(Math.min(Math.max(limit, 1), 50));
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.Message leaveMessage(@Valid @RequestBody ContentModels.MessageInput input, HttpServletRequest request) {
        messageRateLimiter.check(request.getRemoteAddr());
        return adminContent.createMessage(input);
    }

    @PostMapping("/messages/{id}/like")
    public ContentModels.MessageLikeResult likeMessage(@PathVariable long id, HttpServletRequest request) {
        messageRateLimiter.checkInteraction(request.getRemoteAddr());
        return adminContent.likeMessage(id, fingerprint(request));
    }

    @DeleteMapping("/messages/{id}/like")
    public ContentModels.MessageLikeResult unlikeMessage(@PathVariable long id, HttpServletRequest request) {
        messageRateLimiter.checkInteraction(request.getRemoteAddr());
        return adminContent.unlikeMessage(id, fingerprint(request));
    }

    @GetMapping("/messages/{id}/comments")
    public List<ContentModels.MessageComment> comments(@PathVariable long id) {
        return adminContent.messageComments(id);
    }

    @PostMapping("/messages/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.MessageComment addComment(@PathVariable long id,
                                                   @Valid @RequestBody ContentModels.MessageCommentInput input,
                                                   HttpServletRequest request) {
        messageRateLimiter.checkInteraction(request.getRemoteAddr());
        // 「作者」徽章：后台登录会话，或前台登录的站长本人账号
        Long userId = userAuth.userIdFromRequest(request);
        boolean isAuthor = adminSessions.findFromRequest(request).isPresent() || (userId != null && userAuth.isOwner(userId));
        // 已登录前台用户：评论以注册昵称呈现，标记为「已认证」
        String name = userId != null ? userAuth.nicknameForUser(userId) : input.name();
        return adminContent.addMessageComment(id, new ContentModels.MessageCommentInput(name, input.content()), isAuthor, userId);
    }

    // 访客匿名指纹：对来源 IP 做 SHA-256，不落明文 IP，保护隐私
    private String fingerprint(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : remote;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((ip + "::folded-message-like").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ignored) {
            return "anonymous";
        }
    }
}
