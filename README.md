# Co11ap5e 的个人博客

一个自托管、前后端分离的个人网站：游戏杂谈、技术文章与人工智能学习记录。

- 线上地址：https://co11ap5e.site
- 技术栈：**Nuxt 4（SSR）+ Spring Boot 3.5 + PostgreSQL**，Nginx 反向代理，systemd 托管
- 认证：Argon2id 密码哈希、HttpOnly Cookie / Bearer Token、CSRF、登录限流
- 内容：Flyway 迁移管理数据库结构与文章内容，后台支持 Markdown 编辑与版本回滚

## 目录结构

```
frontend/    Nuxt 应用（SSR 渲染、文章/项目/游戏/简历/留言等页面）
backend/     Spring Boot API（文章、项目、游戏档案、留言、站点指标、AI 资讯同步）
deploy/      部署脚本、Nginx 配置、systemd 服务、备份与监控
articles/    文章正文（Markdown 源稿）
```

## 本地开发

```bash
# 前端（Node 22+）
cd frontend
npm ci
npm run dev

# 后端（JDK 21 + Maven，依赖本地 PostgreSQL）
cd backend
mvn spring-boot:run
```

后端启动依赖环境变量，参考 `backend/.env.example`（数据库地址、管理员账号、CORS 来源等）。

## 构建与部署

```bash
# 本机构建产物
./deploy/build-release.ps1     # Windows 下构建 frontend/.output 与 backend/target/*.jar

# 服务器部署（一次性初始化，root 执行）
SITE_HOST=co11ap5e.site bash deploy/bootstrap-ubuntu.sh
```

更新部署参考 `deploy/deploy-*.sh`（均带旧版本备份与健康检查回滚）。服务器侧已配置：
- Nginx 安全响应头（CSP、X-Frame-Options、HSTS 等）
- systemd 服务 `personal-site-api` / `personal-site-web`
- 每日数据库备份（`personal-site-backup.timer`，恢复校验后保留 7 天）
- 资源与进程健康检查（`personal-site-resource-check.timer`，每 10 分钟）

## 功能一览

- 文章列表（按分类：技术 / 游戏杂谈 / 人工智能）与站内搜索
- 文章详情：目录锚点、上一篇 / 下一篇、相关文章推荐、结构化数据（Article Schema）、OG / Twitter 卡片
- 项目案例、游戏档案（含 Steam 同步）、Now 页面、留言板、简历页
- 后台管理：文章 / 项目 / 游戏 / 留言管理，Markdown 双栏编辑，版本历史与恢复
- SSR 文章缓存（Spring Cache + 内存缓存，写操作自动失效）

## 许可

代码与部署脚本开源；文章内容保留署名权（© Co11ap5e）。
