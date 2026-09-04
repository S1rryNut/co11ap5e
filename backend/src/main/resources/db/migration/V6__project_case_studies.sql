ALTER TABLE projects ADD COLUMN slug VARCHAR(180);
ALTER TABLE projects ADD COLUMN background TEXT NOT NULL DEFAULT '';
ALTER TABLE projects ADD COLUMN responsibility TEXT NOT NULL DEFAULT '';
ALTER TABLE projects ADD COLUMN workflow TEXT NOT NULL DEFAULT '[]';
ALTER TABLE projects ADD COLUMN architecture TEXT NOT NULL DEFAULT '';
ALTER TABLE projects ADD COLUMN highlights TEXT NOT NULL DEFAULT '[]';
ALTER TABLE projects ADD COLUMN retrospective TEXT NOT NULL DEFAULT '';

INSERT INTO projects(name, summary, stack, featured, sort_order)
SELECT '分布式证书存储系统',
       '围绕证书、数字签名与多节点一致性构建的 Java 工程实践。',
       '["Java","Spring Boot","SM2/SM3","PostgreSQL"]', TRUE, 1
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE name = '分布式证书存储系统');

INSERT INTO projects(name, summary, stack, featured, sort_order)
SELECT '个人内容与 AI 学习站',
       '前后端分离的个人数字空间，包含博客、简历、AI 信息聚合与内容后台。',
       '["Nuxt","Vue","Spring Boot","PostgreSQL"]', TRUE, 2
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE name = '个人内容与 AI 学习站');

UPDATE projects SET
    slug = 'ningbo-citizen-card-rights-confirmation',
    background = '面向企业内部数据流转与签名确权场景，将多角色参与的申请、审核和验证过程组织为可追踪的业务流程。',
    responsibility = '负责 Vue 3 前端功能开发、接口联调与交互性能优化，配合后端完成认证和安全通信流程。',
    workflow = '["发起数据使用与签名申请","业务人员审核申请并创建任务","参与方按顺序完成签名与确权","异常结果进入仲裁并形成最终状态"]',
    architecture = '前端基于 Vue 3、Vite、Pinia 与 Element Plus，使用 Axios 对接 Spring Boot 与 Oracle 服务；认证与通信链路结合 JWT、ECDH 和请求签名。',
    highlights = '["按业务状态组织任务申请、签名、确权与仲裁界面","通过请求缓存、虚拟列表与性能监控改善 B 端高频操作体验","对接 JWT、ECDH 与请求签名机制，处理登录态和安全请求流程","统一异常反馈、加载状态与任务进度展示"]',
    retrospective = '流程型 B 端系统的关键不只是完成页面，而是让角色、状态流转和异常路径保持一致。前端需要把复杂业务规则转化为可确认、可追踪的操作反馈。'
WHERE name = '宁波市市民卡公司签名确权平台';

UPDATE projects SET
    slug = 'distributed-certificate-storage',
    background = '围绕数字证书、节点协作和签名验证构建工程实践，探索证书数据在协调器与多个存储节点之间的可信流转。',
    responsibility = '参与协调器与节点服务设计，实现证书管理、节点通信、签名验证及相关安全校验。',
    workflow = '["成员注册并维护节点与证书信息","协调器组织证书和区块数据流转","节点验证证书有效期、签发方与签名","多节点保存结果并返回处理状态"]',
    architecture = '工程拆分为 coordinator 与 node 两类 Spring Boot 服务，通过 HTTP 客户端协作；数据层使用 MyBatis-Plus，并集成 Bouncy Castle、JWT 和缓存组件。',
    highlights = '["实现 SM2/SM3 数字签名生成与验证","校验证书有效期、签发方白名单和证书签名","拆分协调器与节点职责，处理节点通信异常","对输入地址和证书上传流程增加安全校验"]',
    retrospective = '分布式安全系统需要同时处理密码算法、证书生命周期和节点故障。相比单点功能实现，边界校验、失败反馈与跨节点一致性更影响系统可信度。'
WHERE name = '分布式证书存储系统';

UPDATE projects SET
    slug = 'personal-content-ai-site',
    background = '构建可长期维护的个人数字空间，统一承载博客、简历、AI 学习、项目案例和内容管理。',
    responsibility = '独立完成产品设计、前后端开发、数据库设计、安全加固、服务器部署与日常运维。',
    workflow = '["在管理后台创建和维护内容","Spring Boot API 校验并持久化数据","Nuxt 服务端渲染公开页面","Nginx 统一代理并提供安全响应头"]',
    architecture = '采用 Nuxt 4、Vue 3、Spring Boot、PostgreSQL、Nginx 与 systemd 的前后端分离架构，运行于 2C2G 轻量云服务器。',
    highlights = '["实现博客、杂谈、AI 学习、简历与项目的统一内容管理","使用 Argon2id、HttpOnly Cookie、CSRF 与登录限流保护后台","针对 2C2G 和 3Mbps 环境控制内存、构建体积与静态资源传输","配置数据库自动备份并执行真实恢复校验"]',
    retrospective = '资源受限环境更需要控制依赖、内存和运维复杂度。先保证内容可持续维护、安全边界清晰，再逐步扩展功能。'
WHERE name = '个人内容与 AI 学习站';

UPDATE projects SET slug = 'project-' || id WHERE slug IS NULL;
ALTER TABLE projects ALTER COLUMN slug SET NOT NULL;
ALTER TABLE projects ADD CONSTRAINT uq_projects_slug UNIQUE (slug);
