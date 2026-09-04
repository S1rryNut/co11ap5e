package com.personal.site.content;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SiteMetricsController {
    private final SiteMetricsService metrics;

    public SiteMetricsController(SiteMetricsService metrics) {
        this.metrics = metrics;
    }

    @PostMapping("/public/metrics")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@Valid @RequestBody ContentModels.MetricInput input, HttpServletRequest request) {
        // 反向代理下取 X-Forwarded-For 的第一个地址
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        else ip = ip.split(",")[0].trim();
        metrics.record(input, ip, request.getHeader("User-Agent"));
    }

    /** 公开接口：按路径批量返回累计浏览量，供文章列表/详情页展示阅读量 */
    @GetMapping("/public/metrics/views")
    public Map<String, Long> viewsByPaths(@RequestParam(name = "paths") String paths) {
        List<String> list = Arrays.stream(paths.split(",")).map(String::trim)
                .filter(path -> !path.isBlank() && path.length() <= 200).distinct().toList();
        return metrics.viewsByPaths(list);
    }

    @GetMapping("/admin/metrics")
    public ContentModels.MetricsSummary summary() {
        return metrics.summary();
    }
}
