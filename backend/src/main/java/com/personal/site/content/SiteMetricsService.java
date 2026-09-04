package com.personal.site.content;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class SiteMetricsService {
    private final JdbcTemplate jdbc;

    public SiteMetricsService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void record(ContentModels.MetricInput input, String ip, String userAgent) {
        int hasRender = input.renderMs() == null ? 0 : 1;
        double render = input.renderMs() == null ? 0 : input.renderMs();
        int changed = jdbc.update("""
                UPDATE site_metrics_daily SET views=views+1,render_samples=render_samples+?,
                render_total_ms=render_total_ms+?,render_max_ms=GREATEST(render_max_ms,?)
                WHERE metric_date=CURRENT_DATE AND path=?
                """, hasRender, render, render, input.path());
        if (changed == 0) {
            try {
                jdbc.update("""
                        INSERT INTO site_metrics_daily(metric_date,path,views,render_samples,render_total_ms,render_max_ms)
                        VALUES (CURRENT_DATE,?,1,?,?,?)
                        """, input.path(), hasRender, render, render);
            } catch (DuplicateKeyException exception) {
                jdbc.update("""
                        UPDATE site_metrics_daily SET views=views+1,render_samples=render_samples+?,
                        render_total_ms=render_total_ms+?,render_max_ms=GREATEST(render_max_ms,?)
                        WHERE metric_date=CURRENT_DATE AND path=?
                        """, hasRender, render, render, input.path());
            }
        }
        // 独立访客：按天 + 匿名哈希去重
        String hash = visitorHash(ip, userAgent);
        try {
            jdbc.update("""
                    INSERT INTO site_metrics_visitors(metric_date, visitor_hash)
                    VALUES (CURRENT_DATE, ?)
                    """, hash);
        } catch (DuplicateKeyException ignored) {
            // 今日已记录过该访客
        }
    }

    /** 匿名访客标识：IP + UA 的 SHA-256，不落原始身份信息 */
    static String visitorHash(String ip, String userAgent) {
        String raw = (ip == null ? "" : ip) + "|" + (userAgent == null ? "" : userAgent);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    /** 按路径批量返回累计浏览量（全部历史），缺省 0 */
    public Map<String, Long> viewsByPaths(List<String> paths) {
        if (paths.isEmpty()) return Map.of();
        String placeholders = String.join(",", Collections.nCopies(paths.size(), "?"));
        Map<String, Long> result = new HashMap<>();
        jdbc.query("SELECT path, COALESCE(SUM(views),0) AS views FROM site_metrics_daily " +
                        "WHERE path IN (" + placeholders + ") GROUP BY path",
                paths.toArray(), (RowCallbackHandler) rs -> result.put(rs.getString("path"), rs.getLong("views")));
        return result;
    }

    public ContentModels.MetricsSummary summary() {
        Long today = jdbc.queryForObject("SELECT COALESCE(SUM(views),0) FROM site_metrics_daily WHERE metric_date=CURRENT_DATE", Long.class);
        Long thirtyDays = jdbc.queryForObject("SELECT COALESCE(SUM(views),0) FROM site_metrics_daily WHERE metric_date>=CURRENT_DATE-29", Long.class);
        Long visitorsToday = jdbc.queryForObject("SELECT COUNT(*) FROM site_metrics_visitors WHERE metric_date=CURRENT_DATE", Long.class);
        Long visitorsThirtyDays = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT visitor_hash) FROM site_metrics_visitors
                WHERE metric_date>=CURRENT_DATE-29
                """, Long.class);
        Double average = jdbc.queryForObject("""
                SELECT CASE WHEN COALESCE(SUM(render_samples),0)=0 THEN 0
                ELSE SUM(render_total_ms)/SUM(render_samples) END
                FROM site_metrics_daily WHERE metric_date>=CURRENT_DATE-29
                """, Double.class);
        List<ContentModels.PageMetric> pages = jdbc.query("""
                SELECT path,SUM(views) AS views,SUM(render_samples) AS samples,
                SUM(render_total_ms) AS total_ms,MAX(render_max_ms) AS max_ms
                FROM site_metrics_daily WHERE metric_date>=CURRENT_DATE-29 GROUP BY path
                ORDER BY views DESC,path LIMIT 10
                """, (rs, row) -> {
            long samples = rs.getLong("samples");
            return new ContentModels.PageMetric(rs.getString("path"), rs.getLong("views"),
                    samples == 0 ? 0 : rs.getDouble("total_ms") / samples, rs.getDouble("max_ms"));
        });
        List<ContentModels.DailyMetric> daily = jdbc.query("""
                SELECT d.metric_date AS metric_date, d.views AS views, COALESCE(v.cnt,0) AS visitors
                FROM (
                  SELECT metric_date, SUM(views) AS views FROM site_metrics_daily
                  WHERE metric_date>=CURRENT_DATE-29 GROUP BY metric_date
                ) d
                LEFT JOIN (
                  SELECT metric_date, COUNT(DISTINCT visitor_hash) AS cnt FROM site_metrics_visitors
                  WHERE metric_date>=CURRENT_DATE-29 GROUP BY metric_date
                ) v ON v.metric_date = d.metric_date
                ORDER BY d.metric_date
                """, (rs, row) -> new ContentModels.DailyMetric(rs.getDate("metric_date").toLocalDate(),
                rs.getLong("views"), rs.getLong("visitors")));
        return new ContentModels.MetricsSummary(today == null ? 0 : today, thirtyDays == null ? 0 : thirtyDays,
                visitorsToday == null ? 0 : visitorsToday, visitorsThirtyDays == null ? 0 : visitorsThirtyDays,
                average == null ? 0 : average, pages, daily);
    }
}
