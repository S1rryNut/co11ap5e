UPDATE articles
SET content = $$
## 为什么要做这个站

之前的内容散落在本地笔记、项目目录和简历里，查找和展示都不方便。这个站把文章、项目案例、AI 学习、游戏档案和留言放在一起，后台只给个人使用，公开页面尽量简洁。

## 技术选择

前端用 Nuxt 和 Vue，后端用 Spring Boot，数据放在 PostgreSQL，入口由 Nginx 处理。前后端分开，内容接口和后台接口保持独立。

部署环境不公开具体规格。功能上控制依赖数量和图片体积，优先保证文章和页面加载稳定。

## 后台与公开内容

后台负责文章、项目、AI 学习、AI 热点、游戏档案、账号和 Now 的增删改。公开页面只读取已发布内容，草稿和历史版本不会暴露。

## 安全和备份

后台登录使用 Argon2 密码哈希、会话 Cookie、CSRF 和登录限流。数据库和前端发布都有备份，证书、系统服务、日志和资源巡检按服务器环境配置。

## 后面主要维护

先把内容填准确，再继续整理项目案例和游戏评价。功能上以能用、好维护为主，不提前增加暂时不需要的复杂度。
$$,
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'build-a-reliable-personal-site';

UPDATE projects
SET architecture = 'Nuxt 4、Vue 3、Spring Boot、PostgreSQL、Nginx 和 systemd。',
    highlights = '["统一管理博客、项目、AI、游戏档案和 Now","使用 Argon2id、会话、CSRF 和登录限流保护后台","控制依赖、图片体积和静态资源传输","加入数据库备份、日志轮转和资源巡检"]'
WHERE slug = 'personal-content-ai-site';
