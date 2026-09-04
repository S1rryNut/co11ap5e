CREATE TABLE site_metrics_daily (
    metric_date DATE NOT NULL,
    path VARCHAR(200) NOT NULL,
    views BIGINT NOT NULL DEFAULT 0,
    render_samples BIGINT NOT NULL DEFAULT 0,
    render_total_ms DOUBLE PRECISION NOT NULL DEFAULT 0,
    render_max_ms DOUBLE PRECISION NOT NULL DEFAULT 0,
    PRIMARY KEY(metric_date, path)
);

CREATE INDEX idx_site_metrics_date ON site_metrics_daily(metric_date DESC);
