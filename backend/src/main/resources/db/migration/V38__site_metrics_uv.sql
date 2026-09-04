-- 独立访客（UV）：按天 + 访客匿名哈希去重，不保存原始 IP/UA
CREATE TABLE site_metrics_visitors (
    metric_date DATE NOT NULL,
    visitor_hash VARCHAR(64) NOT NULL,
    PRIMARY KEY (metric_date, visitor_hash)
);

CREATE INDEX idx_metrics_visitors_hash ON site_metrics_visitors(visitor_hash, metric_date);
