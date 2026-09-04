package com.personal.site.content;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminContentController {
    private final ContentService content;
    private final ContentAdminService adminContent;
    private final SteamGameMetadataService steamMetadata;
    private final SteamSyncSettingsService steamSettings;

    public AdminContentController(ContentService content, ContentAdminService adminContent,
                                  SteamGameMetadataService steamMetadata, SteamSyncSettingsService steamSettings) {
        this.content = content;
        this.adminContent = adminContent;
        this.steamMetadata = steamMetadata;
        this.steamSettings = steamSettings;
    }

    @GetMapping("/articles")
    public List<ContentModels.Article> all() { return content.allArticles(); }

    @GetMapping("/articles/{id}")
    public ContentModels.Article one(@PathVariable long id) { return content.article(id); }

    @PostMapping("/articles")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.Article create(@Valid @RequestBody ContentModels.ArticleInput input) {
        return content.createArticle(input);
    }

    @PutMapping("/articles/{id}")
    public ContentModels.Article update(@PathVariable long id, @Valid @RequestBody ContentModels.ArticleInput input) {
        return content.updateArticle(id, input);
    }

    @PostMapping("/articles/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.Article copy(@PathVariable long id) { return content.copyArticle(id); }

    @DeleteMapping("/articles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { content.deleteArticle(id); }

    @GetMapping("/articles/{id}/versions")
    public List<ContentModels.ArticleVersion> articleVersions(@PathVariable long id) {
        return content.articleVersions(id);
    }

    @PostMapping("/articles/{id}/versions/{versionId}/restore")
    public ContentModels.Article restoreArticleVersion(@PathVariable long id, @PathVariable long versionId) {
        return content.restoreArticleVersion(id, versionId);
    }

    @GetMapping("/projects")
    public List<ContentModels.AdminProject> projects() { return adminContent.projects(); }

    @GetMapping("/projects/{id}")
    public ContentModels.AdminProject project(@PathVariable long id) { return adminContent.project(id); }

    @PostMapping("/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.AdminProject createProject(@Valid @RequestBody ContentModels.ProjectInput input) {
        return adminContent.createProject(input);
    }

    @PutMapping("/projects/{id}")
    public ContentModels.AdminProject updateProject(@PathVariable long id, @Valid @RequestBody ContentModels.ProjectInput input) {
        return adminContent.updateProject(id, input);
    }

    @DeleteMapping("/projects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable long id) { adminContent.deleteProject(id); }

    @GetMapping("/games")
    public List<ContentModels.GameEntry> games() { return adminContent.gameEntries(); }

    @GetMapping("/games/{id}")
    public ContentModels.GameEntry game(@PathVariable long id) { return adminContent.gameEntry(id); }

    @GetMapping("/games/steam/{appId}")
    public ContentModels.GameEntryInput steamGame(@PathVariable long appId) { return steamMetadata.load(appId); }

    @GetMapping("/games/steam/settings")
    public SteamSyncSettingsService.Settings steamSettings() { return steamSettings.load(); }

    @PutMapping("/games/steam/settings")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveSteamSettings(@Valid @RequestBody ContentModels.SteamSyncSettingsInput input) {
        steamSettings.save(input.profileUrl(), input.apiKey());
    }

    @PostMapping("/games/steam/sync")
    public java.util.Map<String, Integer> syncSteamGames(@Valid @RequestBody ContentModels.SteamSyncInput input) {
        String profileUrl = input.profileUrl();
        String apiKey = input.apiKey();
        if (profileUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            SteamSyncSettingsService.Settings saved = steamSettings.load();
            if (profileUrl.isBlank()) profileUrl = saved.profileUrl();
            if (apiKey == null || apiKey.isBlank()) apiKey = steamSettings.apiKey();
        }
        return java.util.Map.of("count", steamMetadata.syncLibrary(profileUrl, apiKey));
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.GameEntry createGame(@Valid @RequestBody ContentModels.GameEntryInput input) {
        return adminContent.createGameEntry(input);
    }

    @PutMapping("/games/{id}")
    public ContentModels.GameEntry updateGame(@PathVariable long id, @Valid @RequestBody ContentModels.GameEntryInput input) {
        return adminContent.updateGameEntry(id, input);
    }

    @DeleteMapping("/games/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGame(@PathVariable long id) { adminContent.deleteGameEntry(id); }

    @GetMapping("/messages")
    public List<ContentModels.Message> messages() { return adminContent.messages(); }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable long id) { adminContent.deleteMessage(id); }

    @GetMapping("/learning-resources")
    public List<ContentModels.LearningResource> learningResources() { return adminContent.learningResources(); }

    @GetMapping("/learning-resources/{id}")
    public ContentModels.LearningResource learningResource(@PathVariable long id) { return adminContent.learningResource(id); }

    @PostMapping("/learning-resources")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.LearningResource createLearningResource(@Valid @RequestBody ContentModels.LearningResourceInput input) {
        return adminContent.createLearningResource(input);
    }

    @PutMapping("/learning-resources/{id}")
    public ContentModels.LearningResource updateLearningResource(@PathVariable long id, @Valid @RequestBody ContentModels.LearningResourceInput input) {
        return adminContent.updateLearningResource(id, input);
    }

    @DeleteMapping("/learning-resources/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLearningResource(@PathVariable long id) { adminContent.deleteLearningResource(id); }

    @GetMapping("/learning-logs")
    public List<ContentModels.LearningLog> learningLogs() { return adminContent.learningLogs(); }

    @GetMapping("/learning-logs/{id}")
    public ContentModels.LearningLog learningLog(@PathVariable long id) { return adminContent.learningLog(id); }

    @PostMapping("/learning-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.LearningLog createLearningLog(@Valid @RequestBody ContentModels.LearningLogInput input) {
        return adminContent.createLearningLog(input);
    }

    @PutMapping("/learning-logs/{id}")
    public ContentModels.LearningLog updateLearningLog(@PathVariable long id, @Valid @RequestBody ContentModels.LearningLogInput input) {
        return adminContent.updateLearningLog(id, input);
    }

    @DeleteMapping("/learning-logs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLearningLog(@PathVariable long id) { adminContent.deleteLearningLog(id); }

    @GetMapping("/ai-news")
    public List<ContentModels.AiNews> aiNews() { return adminContent.aiNews(); }

    @GetMapping("/ai-news/{id}")
    public ContentModels.AiNews aiNewsItem(@PathVariable long id) { return adminContent.aiNewsItem(id); }

    @PostMapping("/ai-news")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.AiNews createAiNews(@Valid @RequestBody ContentModels.AiNewsInput input) {
        return adminContent.createAiNews(input);
    }

    @PutMapping("/ai-news/{id}")
    public ContentModels.AiNews updateAiNews(@PathVariable long id, @Valid @RequestBody ContentModels.AiNewsInput input) {
        return adminContent.updateAiNews(id, input);
    }

    @DeleteMapping("/ai-news/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAiNews(@PathVariable long id) { adminContent.deleteAiNews(id); }

    @GetMapping("/game-accounts")
    public List<ContentModels.GameAccount> gameAccounts() { return adminContent.gameAccounts(); }

    @GetMapping("/game-accounts/{id}")
    public ContentModels.GameAccount gameAccount(@PathVariable long id) { return adminContent.gameAccount(id); }

    @PostMapping("/game-accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentModels.GameAccount createGameAccount(@Valid @RequestBody ContentModels.GameAccountInput input) {
        return adminContent.createGameAccount(input);
    }

    @PutMapping("/game-accounts/{id}")
    public ContentModels.GameAccount updateGameAccount(@PathVariable long id, @Valid @RequestBody ContentModels.GameAccountInput input) {
        return adminContent.updateGameAccount(id, input);
    }

    @DeleteMapping("/game-accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGameAccount(@PathVariable long id) { adminContent.deleteGameAccount(id); }

    @GetMapping("/resume")
    public ContentModels.Resume resume() { return content.resume(); }

    @PutMapping("/resume")
    public ContentModels.Resume updateResume(@Valid @RequestBody ContentModels.Resume input) {
        return adminContent.updateResume(input);
    }

    @GetMapping("/now")
    public ContentModels.NowProfile nowProfile() { return adminContent.nowProfile(); }

    @PutMapping("/now")
    public ContentModels.NowProfile updateNowProfile(@Valid @RequestBody ContentModels.NowProfile input) {
        return adminContent.updateNowProfile(input);
    }
}
