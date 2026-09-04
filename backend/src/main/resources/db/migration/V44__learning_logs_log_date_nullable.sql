-- 学习日志不再强制要求日期（学习路线内容按站/节组织，不依赖日期）
ALTER TABLE learning_logs ALTER COLUMN log_date DROP NOT NULL;
