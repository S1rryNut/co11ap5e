package com.personal.site.content;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai")
@ConditionalOnBean(AiFeedService.class)
public class AdminAiController {
    private final AiFeedService feeds;
    public AdminAiController(AiFeedService feeds) { this.feeds = feeds; }

    @PostMapping("/sync")
    public AiFeedService.SyncResult sync() { return feeds.syncAll(); }
}

