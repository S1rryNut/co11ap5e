UPDATE articles
SET excerpt = '从页面、API、数据库到部署和认证，记录一个前后端分离个人站的核心实现。',
    reading_minutes = 14,
    content = $$
## 整体结构

这个站分为四个部分：

```text
Browser
  -> Nginx
  -> Nuxt 4 SSR
  -> Spring Boot API
  -> PostgreSQL
```

公开页面由 Nuxt 在服务端渲染，浏览器接管后进行交互。后台接口全部挂在 `/api/admin`，公开数据挂在 `/api/public`。

页面和接口分开的原因是权限不同。公开接口只能读取已发布内容，后台接口需要登录后才能修改数据。不要在一个 Controller 里把查询和写入混在一起。

## 数据模型

核心表包括：

- `articles`：文章
- `projects`：项目案例
- `game_entries`：游戏档案
- `game_accounts`：游戏账号
- `learning_resources`：AI 学习资源
- `ai_news`：AI 热点
- `site_profiles`：简历和 Now 这类单例配置
- `admin_sessions`：后台会话

文章使用 `slug` 作为公开地址，分类不参与 URL，只影响列表筛选。

```text
文章分类：
  工程实践 -> /article/tech
  杂谈     -> /article/game
  AI 学习  -> /article/ai
```

这样 URL 保持稳定，即使以后调整分类名称，也不需要改链接。

## API 层

后端使用 Spring Boot，Controller 只负责参数和 HTTP 状态，具体 SQL 放在 Service。

公开文章查询核心条件：

```text
status = PUBLISHED
category 匹配
excludeCategory 不匹配
q 匹配标题、摘要、分类或标签
ORDER BY published_at DESC
```

搜索参数使用参数化 SQL：

```text
LOWER(title) LIKE LOWER(?)
```

不直接拼接用户输入。这是基本要求，尤其是文章标题、标签和搜索关键词都可能来自后台。

## 后台认证

密码使用 Argon2id 保存，不存明文。登录后服务端生成 32 字节随机 token，数据库只保存 SHA-256 后的值。

```text
登录成功
  -> 创建 admin_sessions 记录
  -> 返回 HttpOnly Cookie
  -> 浏览器后续请求携带 Cookie
```

为了避免个别浏览器或环境不保存 Cookie，后台也支持短期 Bearer Token：

```text
Authorization: Bearer <session-token>
```

网页后台仍然先使用 HttpOnly Cookie。Bearer Token 只放在当前标签页的 `sessionStorage`，关闭标签页后失效。

写操作需要 CSRF：

```text
GET /api/public/csrf
  -> 写入 XSRF-TOKEN Cookie
  -> 前端读取 Cookie
  -> 请求头携带 X-XSRF-TOKEN
```

登录接口还有 IP 限流，避免密码被连续爆破。

## 内容渲染

正文使用 Markdown，前端通过 `markdown-it` 渲染：

```js
const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: false
})
```

`html: false` 很关键。后台内容即使有 HTML，也不能原样注入页面。最终输出前还要保证高亮搜索词时先做 HTML 转义。

图片上传只允许 JPEG 和 PNG。服务端会：

1. 检查文件大小
2. 使用 ImageIO 重新解码图片
3. 限制长边到 1920px
4. 统一输出 JPEG
5. 使用 UUID 文件名

这样能避免上传伪装成图片的脚本，也能控制公开页面图片体积。

## SSR 和缓存

Nuxt 的 `useAsyncData` 负责页面数据：

```js
const { data: articles } = await useAsyncData(
  'article-tech',
  () => api.getArticles(query.value, '', '杂谈')
)
```

不同页面使用不同的 key，避免服务端 payload 被串用。公开页面和 API 设置 `no-store`，静态 JS/CSS 才允许长缓存。

后台保存后会直接写数据库，主站下一次请求就能拿到新数据。

## 备份和运行维护

日常维护包括：

- PostgreSQL 定时备份
- 前端发布前保留旧版本，发布失败时回滚
- 日志轮转
- 资源巡检
- 服务由 systemd 管理

发布脚本会先启动新后端，检查健康接口：

```text
GET /actuator/health
```

后端正常后，再切换 Nuxt 前端。前端也要验证 `/admin` 和 `/games` 能正常返回，否则恢复旧版本。
$$,
    published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'build-a-reliable-personal-site';

UPDATE articles
SET excerpt = '拆解授权、存证和核验页面的状态处理、请求缓存、虚拟列表与性能观测。',
    reading_minutes = 16,
    content = $$
## 页面要解决的核心问题

这个项目不是单纯的展示页。业务数据要经过授权、存证和核验，页面需要让使用者清楚知道：

- 当前数据处于哪个阶段
- 下一步可以做什么
- 为什么某个操作不能继续
- 失败时应该找谁或看哪里

所以前端不能把接口结果直接平铺，而要先把业务流程整理成状态。

```text
授权确认
  -> 等待审核
  -> 签名中
  -> 已签名
  -> 待核验
  -> 核验通过
  -> 完成
```

异常可以进入仲裁。页面上的按钮、提示和进度必须和这个状态一致。

## 请求缓存

列表、详情和状态接口很多，反复请求会拖慢操作。前端使用四类缓存策略：

### TTL

每个缓存项保存过期时间：

```ts
interface CacheEntry<T> {
  data: T
  expiresAt: number
  inflight?: Promise<T>
  tags: Set<string>
}
```

命中且未过期时直接返回数据。

### 并发去重

同一个 key 正在请求时，后续调用复用同一个 Promise：

```ts
if (entry.inflight) return entry.inflight

entry.inflight = fetchData().finally(() => {
  delete entry.inflight
})
```

这能避免列表页快速切换时发出大量重复请求。

### SWR

缓存过期后，先返回旧数据，再在后台刷新：

```ts
const cached = cache.get(key)
if (cached) {
  refresh(key)
  return cached
}
```

页面看起来响应很快，但要注意旧数据必须被明确标记或很快替换，不能误导用户。

### Tag 失效

写操作成功后，不能只清理单条 key。例如签名完成后，任务详情、列表、状态统计都可能变化。

```ts
cache.invalidateByTag(['task-list', 'task-status'])
```

每个业务操作都关联一组 Tag，比到处手动清缓存更可靠。

## 虚拟列表

长任务列表如果一次渲染几百条，DOM 数量会成为性能瓶颈。虚拟列表只渲染可见范围：

```ts
const startIndex = Math.floor(scrollTop / itemHeight)
const endIndex = Math.ceil((scrollTop + viewportHeight) / itemHeight)
const visibleItems = items.slice(startIndex, endIndex)
```

实现时要注意：

- 每一项高度稳定
- 容器高度要占位正确
- 滚动时使用 `requestAnimationFrame`
- 在可见范围上下增加 overscan
- 销毁离屏 DOM

如果行高不固定，需要额外维护高度测量和修正。

## 性能观测

页面记录三类数据：

1. Web Vitals：LCP、INP、CLS
2. 路由耗时
3. API 耗时

这些数据不要阻塞页面。采集失败时静默忽略，不能影响业务流程。

```ts
try {
  await reportMetric(payload)
} catch {
  // 性能采集不能影响主流程。
}
```

记录时带上页面路径和请求地址，便于后续定位是哪个页面、哪个接口变慢。

## 权限和不可否认性

签名和确权操作必须经过确认。前端重点处理：

- 操作前展示影响范围
- 高风险操作二次确认
- 提交后禁用重复按钮
- 请求失败时保留用户输入
- 服务端返回原因后原样展示

前端不能代替后端做权限判断。接口仍然要检查用户、任务状态和操作顺序，页面隐藏按钮只是为了减少误操作。

## 复盘

这个项目里最花时间的不是某个复杂动画，而是把不同接口的数据合并成一致的状态。缓存、虚拟列表和性能监控都只是手段，最终要解决的是“用户能不能快速判断下一步，并且相信页面给出的结果”。
$$,
    published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'certificate-system-retrospective';

UPDATE articles
SET excerpt = '从检索、上下文、生成、评测到 Agent 工具调用，梳理 AI 应用开发中需要搞清楚的环节。',
    reading_minutes = 15,
    content = $$
## 学习内容怎么组织

AI 应用开发包含很多层：

```text
数据
  -> 模型调用
  -> Prompt / 上下文
  -> RAG / Agent
  -> 评测
  -> 部署和成本
```

直接背概念效率很低。我的做法是每个阶段先写一个最小程序，跑通后再补理论。

## 模型调用

先写一个能运行的 Python 客户端，把几个关键行为弄清楚：

```python
import json
import requests

response = requests.post(
    api_url,
    headers={
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",
    },
    json={
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": question},
        ],
        "temperature": 0.2,
    },
    timeout=60,
)

data = response.json()
answer = data["choices"][0]["message"]["content"]
```

最小客户端需要处理：

- 请求超时
- 非 200 状态
- 限流和退避重试
- 返回格式异常
- token 使用量

不要一开始就接完整框架，否则失败时不知道问题出在模型、网络还是框架封装。

## RAG 的基本链路

RAG 不是为了“把向量数据库接上”，而是让生成结果能参考准确内容。

```text
文档
  -> 清洗
  -> 切分
  -> embedding
  -> 向量库

问题
  -> embedding
  -> 检索
  -> 重排
  -> 拼 Prompt
  -> LLM
  -> 带引用答案
```

每个环节都要验证：

- 文档是否被正确切分
- 检索结果是否和问题相关
- 上下文长度是否超限
- 引用的片段是否真的包含答案
- 没有命中时是否明确返回“资料不足”

常见的失败不是向量检索跑不起来，而是检索结果看似相关、实际没有回答用户问题。

## 上下文构建

生成前把检索结果整理成稳定结构：

```text
[来源 1]
标题：...
内容：...

[来源 2]
标题：...
内容：...
```

Prompt 中需要明确：

1. 只能依据提供的内容回答
2. 内容不足时直接说明
3. 输出引用编号
4. 不要编造来源

如果业务需要简短答案，就让模型只输出结论和引用；如果需要解释，再补充推理步骤。

## Agent 工作流

Agent 不是简单的多次模型调用。一个可维护的工作流通常有：

```text
用户目标
  -> 当前状态
  -> 选择工具
  -> 执行工具
  -> 检查结果
  -> 下一步 / 结束
```

工具定义要包含：

- 名称
- 作用
- 参数 schema
- 成功结果
- 错误结果
- 权限范围

伪代码：

```python
state = build_initial_state(user_input)

for step in range(max_steps):
    action = model.choose_action(state)

    if action == "finish":
        return action.answer

    result = execute_tool(action.name, action.arguments)
    state.append({
        "tool": action.name,
        "input": action.arguments,
        "output": result,
    })

    if result.is_terminal:
        return result.summary

raise MaxStepsExceeded(state)
```

必须限制最大步数，否则循环失败会一直消耗 token。

工具执行失败不能直接喂一个含糊字符串。错误信息应该包括：

- 失败原因
- 可以重试的参数
- 是否需要用户补充信息

## 评测

先准备几十条真实问题，每条包含：

- 问题
- 期望答案
- 必须出现的依据
- 不应出现的错误信息

评测不是只看模型“回答得像不像”，还要检查引用、事实和失败时的表现。

简单评测指标可以包括：

- 是否给出答案
- 是否有引用
- 引用是否来自给定资料
- 是否存在编造
- 回答是否完整

模型升级或 Prompt 调整后，用同一组题目重新跑。

## 当前做法

我现在先控制学习范围：优先做能运行、能评测的小项目。每完成一个版本，记录输入、输出、失败路径和成本，不追最新模型名称，也不把教程里的内容直接当作自己的结论。
$$,
    published_at = COALESCE(published_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE slug = 'notes-on-ai-learning';
