-- 站长本人账号标记：该邮箱对应的前台用户评论直接显示「作者」徽章
ALTER TABLE users ADD COLUMN is_owner BOOLEAN NOT NULL DEFAULT false;
UPDATE users SET is_owner = true WHERE email = 'admin@example.com';
