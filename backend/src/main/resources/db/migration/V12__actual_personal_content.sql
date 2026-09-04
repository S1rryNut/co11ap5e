ALTER TABLE game_entries DROP CONSTRAINT IF EXISTS chk_game_status;
ALTER TABLE game_entries ADD CONSTRAINT chk_game_status CHECK (
    status IN ('PLAYING','COMPLETED','WISHLIST','DROPPED','LIBRARY')
);

UPDATE game_entries
SET status = 'LIBRARY',
    verdict = '来自 Steam 游戏库，后续补充个人评价。'
WHERE slug LIKE 'steam-%'
  AND status = 'WISHLIST';

DELETE FROM articles
WHERE slug = '1'
  AND title = '1'
  AND excerpt = '1'
  AND content = '1';

UPDATE articles
SET title = '构建一个长期可维护的个人网站',
    excerpt = '记录这个个人站的技术选型、内容结构和在低配服务器上的部署维护。',
    category = '工程实践',
    content = $$
## 为什么要做这个站

之前的内容散落在本地笔记、项目目录和简历里，查找和展示都不方便。这个站把文章、项目案例、AI 学习、游戏档案和留言放在一起，后台只给个人使用，公开页面尽量简洁。

## 技术选择

前端用 Nuxt 和 Vue，后端用 Spring Boot，数据放在 PostgreSQL，入口由 Nginx 处理。前后端分开，内容接口和后台接口保持独立。

服务器是 2C2G、3Mbps 带宽的轻量服务器。这个配置不适合堆缓存、搜索引擎和大型任务，所以先控制依赖数量，图片使用压缩，文章和页面由服务端渲染。

## 后台与公开内容

后台负责文章、项目、AI 学习、AI 热点、游戏档案、账号和 Now 的增删改。公开页面只读取已发布内容，草稿和历史版本不会暴露。

## 安全和备份

后台登录使用 Argon2 密码哈希、会话 Cookie、CSRF 和登录限流。数据库和前端发布都有备份，证书、系统服务、日志和资源巡检按服务器环境配置。

## 后面主要维护

先把内容填准确，再继续整理项目案例和游戏评价。功能上以能用、好维护为主，不提前增加暂时不需要的复杂度。
$$,
    published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'build-a-reliable-personal-site';

UPDATE articles
SET title = '业务数据授权去中心化存证系统复盘',
    excerpt = '围绕授权确认、链上存证和结果核验，整理一个 Vue 前端项目里的交互与性能处理。',
    category = '项目复盘',
    content = $$
## 项目在解决什么

这个项目面向联盟链场景，主要做业务数据授权后的去中心化存证。页面围绕“授权确认、链上存证、结果核验”展开，目标是让链上流程对业务用户可见、可追踪，并能在异常时找到原因。

## 前端结构

技术栈是 Vue 3、Vite、Pinia 和 Element Plus。接口请求加了 TTL 缓存、并发去重、SWR 和按 Tag 失效，长列表使用虚拟滚动。性能观测记录 Web Vitals、路由耗时和接口耗时。

## 主要改动

比较花时间的是把分散流程整理成一致的状态和反馈。页面需要同时表达授权状态、签名进度、核验结果和异常原因，不能只把接口数据平铺出来。

## 复盘

这个项目让我确认了一个判断：业务系统里的“可用”不只是接口调通。状态怎么流转、失败如何解释、长列表是否流畅，都会影响使用者能不能继续操作。后面写这类页面，我会先把异常路径和加载状态一起考虑。
$$,
    published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'certificate-system-retrospective';

UPDATE articles
SET title = '我如何整理 AI 学习路径',
    excerpt = '把资料、实践和项目放在一起，避免只收集不验证。',
    category = 'AI 学习',
    content = $$
## 先整理来源

AI 资料更新很快。我会把官方文档、课程和本地笔记分开，避免收藏很多链接但从来不复习。学习资源只保留当前会看、会试的内容。

## 当前重点

现在主要看 RAG 和 Agent 的应用层实现，理解检索、上下文、工具调用和失败处理。不会一开始就追模型细节，先把能跑通的小项目做出来。

## 怎么判断学会了

能够用自己的话解释流程，并且写出一段可以运行、可以验证结果的代码。只看教程、只复制示例不算完成。

## 记录方式

公开文章写结论和踩坑，本地笔记保留过程。新闻和课程资料只做入口，不在文章里堆大量术语。
$$,
    published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'notes-on-ai-learning';

UPDATE projects
SET name = '业务数据授权去中心化存证系统',
    summary = '联盟链场景下的业务数据授权与存证前端，覆盖授权确认、链上存证和结果核验。',
    stack = '["Vue 3","Vite","Pinia","Element Plus","Java","Spring Boot","MyBatis-Plus","Redis","SQLite","Oracle","JWT","SM2/SM3"]',
    background = '面向联盟链中的业务数据授权场景，把授权确认、链上存证和结果核验组织成可追踪的前端流程。',
    responsibility = '参与前端方案和页面开发，处理接口缓存、并发请求、长列表性能与异常状态展示。',
    workflow = '["业务数据发起授权确认","授权结果进入链上存证","参与者核验数据和签名状态","异常结果返回可追踪反馈"]',
    architecture = '前端使用 Vue 3、Vite、Pinia 和 Element Plus，通过接口缓存与虚拟列表控制页面请求和渲染成本。后端拆分为协调器与节点服务，使用 Spring Boot、MyBatis-Plus、Redis、SQLite/Oracle 和 JWT。',
    highlights = '["使用 TTL、并发去重、SWR 和 Tag 失效处理接口缓存","长列表使用虚拟滚动，并记录 Web Vitals、路由与接口耗时","整理授权、存证、核验和异常反馈页面","对证书上传、地址校验和节点通信增加安全边界"]',
    retrospective = '链上业务不是把接口显示出来就结束。状态流转、失败原因和操作反馈比单纯增加功能更重要。'
WHERE slug = 'distributed-certificate-storage';

UPDATE projects
SET name = '数据流转确权平台',
    summary = '完成数据流转与签名确权流程中的前端页面、接口联调和兼容性处理。',
    stack = '["Vue 3","Vite","Pinia","Element Plus","Spring Boot","Oracle","JWT / ECDH"]',
    background = '平台把数据使用申请、任务审核、签名、确权和仲裁组织成多角色流程。',
    responsibility = '负责签名生成、确权验证、在线身份校验和二次确认等前端交互，并配合后端完成接口联调与问题修复。',
    workflow = '["发起数据使用与签名申请","审核申请并创建任务","参与方完成签名与确权","异常结果进入仲裁并形成最终状态"]',
    architecture = '前端使用 Vue 3、Vite、Pinia 与 Element Plus；后端基于 Spring Boot 和 Oracle，认证与请求链路结合 JWT、ECDH 和请求签名。',
    highlights = '["完成签名生成和确权验证页面","完成在线身份校验和操作二次确认","整理任务申请、审核、签名、确权和仲裁状态","处理兼容性测试、缺陷修复和接口联调"]',
    retrospective = '流程型页面要把角色、状态和异常路径放在一起考虑。前期多花时间统一反馈和进度展示，后面迭代会稳定很多。'
WHERE slug = 'ningbo-citizen-card-rights-confirmation';

UPDATE projects
SET summary = '个人数字空间，统一管理博客、项目案例、AI 学习、游戏档案、留言和内容后台。',
    background = '把分散在本地笔记、简历和项目目录里的内容整理成一个可维护的个人站点。',
    responsibility = '独立完成产品结构、前后端、数据库、部署和安全维护。',
    workflow = '["在后台维护文章、项目、AI 和游戏内容","Spring Boot 校验并保存数据","Nuxt 服务端渲染公开页面","Nginx 统一代理与安全响应"]',
    architecture = 'Nuxt 4、Vue 3、Spring Boot、PostgreSQL、Nginx 和 systemd，部署在 2C2G、3Mbps 的轻量云服务器。',
    highlights = '["统一管理博客、项目、AI、游戏档案和 Now","使用 Argon2id、会话、CSRF 和登录限流保护后台","控制图片、构建体积和接口请求以适应低配服务器","加入数据库备份、日志轮转和资源巡检"]',
    retrospective = '低配置服务器更适合控制依赖和运维复杂度，先把内容维护起来，再按需要扩展。'
WHERE slug = 'personal-content-ai-site';

UPDATE site_profiles
SET profile_json = '{
  "headline": "继续整理个人站，把项目经历、学习内容和游戏档案放到一起。",
  "updatedAt": "2026-08-29",
  "building": [
    "整理个人站的文章、项目案例和公开内容",
    "整理游戏档案与 Steam 游戏库",
    "完善后台在手机和电脑上的使用体验"
  ],
  "learning": [
    "RAG 与 Agent 的应用层实现",
    "Web 安全、密码学与前端安全边界"
  ],
  "playing": [],
  "reading": [
    "AIGC 系统学习指南"
  ]
}',
    updated_at = CURRENT_TIMESTAMP
WHERE profile_key = 'now';

UPDATE learning_resources
SET title = '先跑通一个模型调用项目',
    description = '用 Python 调用一个可访问的大模型接口，记录请求、响应、错误和成本。',
    level = 'BEGINNER',
    topic = '应用开发',
    sort_order = 1
WHERE id = 1;

UPDATE learning_resources
SET title = 'RAG 的最小可运行版本',
    description = '从文档切分、检索、拼接到生成和评测，先完成一条能观察的链路。',
    level = 'INTERMEDIATE',
    topic = 'RAG',
    sort_order = 2
WHERE id = 2;

UPDATE learning_resources
SET title = 'Agent 工具调用与失败路径',
    description = '把任务拆成步骤，重点看工具调用结果、边界和失败后的回退。',
    level = 'INTERMEDIATE',
    topic = 'Agent',
    sort_order = 3
WHERE id = 3;

UPDATE learning_resources
SET title = '微调与系统化评测',
    description = '在数据集、评测集和成本可控的前提下，比较模型在具体任务上的表现。',
    level = 'ADVANCED',
    topic = '模型工程',
    sort_order = 4
WHERE id = 4;
