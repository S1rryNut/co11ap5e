-- 学习日志按「站」组织：站号 station + 站内小节号 section
-- 展示编号为 station.section，例如 0.1、0.2、1.1、1.2
ALTER TABLE learning_logs ADD COLUMN station INTEGER NOT NULL DEFAULT 0;
ALTER TABLE learning_logs ADD COLUMN section INTEGER NOT NULL DEFAULT 0;
CREATE INDEX idx_learning_logs_station ON learning_logs(station, section, id);

-- 已有日志若无指定，归到 站0 按 id 顺序编号
UPDATE learning_logs l SET section = sub.rn
FROM (
  SELECT id, ROW_NUMBER() OVER (ORDER BY log_date, id) AS rn
  FROM learning_logs
) sub
WHERE l.id = sub.id;
