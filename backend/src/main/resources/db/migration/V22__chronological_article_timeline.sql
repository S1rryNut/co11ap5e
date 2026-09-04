INSERT INTO articles(slug,title,excerpt,category,tags,content,status,reading_minutes,published_at,updated_at)
VALUES
(
  'data-circulation-platform-retrospective',
  '数据流转确权平台项目复盘',
  '从任务申请、审核、签名、确权到仲裁，记录流程型 B 端前端的实现和联调。',
  '项目复盘',
  '["Vue","前端开发","业务系统","项目复盘"]',
  $$
## 项目范围

这个项目主要处理数据使用申请、任务审核、签名、确权和仲裁流程。多个角色会参与，操作顺序和权限会影响后续状态。

前端负责把这些业务规则转换成页面动作，而不是只展示接口返回的数据。

## 我负责的部分

主要参与签名生成、确权验证、在线身份校验和二次确认等交互，也处理接口联调、兼容性测试和问题修复。

其中比较重要的是：

- 签名进度
- 最终确权
- 高风险操作确认
- 异常状态反馈

## 页面状态

任务状态贯穿多个页面：

```text
发起申请
  -> 审核
  -> 签名
  -> 确权
  -> 完成
```

异常进入仲裁。按钮是否可用不能只看前端变量，还要结合服务端返回的任务状态和当前用户权限。

## 联调

这个项目里，单页面调通不等于流程完成。登录态、请求签名、任务状态和错误码要放在同一条流程中测试。

遇到过的问题大多不是页面布局，而是：

- 状态已经变化，页面仍然显示旧数据
- 重复点击造成重复提交
- 请求失败后错误信息不清楚
- 不同角色看到的操作不一致

## 收获

流程型页面要把角色、状态和异常路径放在一起考虑。前端隐藏按钮只能减少误操作，不能代替后端权限。
$$,
  'PUBLISHED',
  9,
  TIMESTAMP WITH TIME ZONE '2025-06-30 10:00:00+08',
  TIMESTAMP WITH TIME ZONE '2025-06-30 10:00:00+08'
);

UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2025-07-15 10:00:00+08' WHERE slug = 'frontend-security-is-not-ui-permission';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2025-10-10 10:00:00+08' WHERE slug = 'virtual-list-is-not-enough';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2025-12-05 10:00:00+08' WHERE slug = 'spring-session-auth-design';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-01-10 10:00:00+08' WHERE slug = 'vue-request-cache-implementation';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-03-31 10:00:00+08' WHERE slug = 'certificate-system-retrospective';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-05-15 10:00:00+08' WHERE slug = 'personal-site-feature-bloat';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-05-28 10:00:00+08' WHERE slug = 'warframe-long-term-notes';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-06-10 10:00:00+08' WHERE slug = 'rag-not-vector-demo';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-06-18 10:00:00+08' WHERE slug = 'apex-legends-shooter-notes';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-06-25 10:00:00+08' WHERE slug = 'rag-retrieval-evaluation';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-07-05 10:00:00+08' WHERE slug = 'agent-tool-call-design';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-07-12 10:00:00+08' WHERE slug = 'destiny-2-content-structure-review';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-07-18 10:00:00+08' WHERE slug = 'agent-complexity-is-not-ability';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-07-25 10:00:00+08' WHERE slug = 'notes-on-ai-learning';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-08-08 10:00:00+08' WHERE slug = 'black-myth-wukong-strict-review';
UPDATE articles SET published_at = TIMESTAMP WITH TIME ZONE '2026-08-20 10:00:00+08' WHERE slug = 'build-a-reliable-personal-site';

UPDATE articles
SET updated_at = published_at
WHERE updated_at < published_at;
