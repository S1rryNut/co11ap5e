package com.personal.site.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 启动预热：应用就绪后延迟几秒，在后台线程请求几个核心公共接口，
 * 让数据库连接池、Web 容器与 JIT 提前热起来，避免冷启动首请求变慢。
 */
@Component
public class StartupWarmer {

    private static final Logger log = LoggerFactory.getLogger(StartupWarmer.class);

    private static final List<String> WARM_PATHS = List.of(
        "/api/public/articles",
        "/api/public/games",
        "/api/public/ai/news",
        "/api/public/ai/learning",
        "/api/public/now",
        "/api/public/projects"
    );

    @EventListener(ApplicationReadyEvent.class)
    public void warm() {
        new Thread(this::run, "startup-warmer").start();
    }

    private void run() {
        try {
            Thread.sleep(3000);
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
            for (String path : WARM_PATHS) {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:8080" + path))
                        .timeout(Duration.ofSeconds(10))
                        .GET().build();
                    client.send(req, HttpResponse.BodyHandlers.discarding());
                } catch (Exception e) {
                    log.warn("warmup failed for {}: {}", path, e.getMessage());
                }
            }
            log.info("startup warmup done");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
