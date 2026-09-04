INSERT INTO articles(slug,title,excerpt,category,tags,content,status,reading_minutes,published_at,updated_at)
VALUES
(
  'vue-request-cache-implementation',
  'Vue 请求缓存的工程实现',
  '用 TTL、并发去重、SWR 和 Tag 失效组织前端请求，减少重复调用。',
  '工程实践',
  '["Vue","TypeScript","缓存","性能"]',
  $$
## 为什么需要请求缓存

业务页面经常同时出现列表、详情和状态接口。如果每次切换页面都重新请求，不仅慢，还会因为响应返回顺序不同造成状态错乱。

前端缓存不是简单把响应存进 Map，而是需要处理：

- 数据什么时候过期
- 同一个请求如何复用
- 旧数据能否暂时展示
- 写操作后哪些数据需要更新

## 缓存结构

一个请求缓存项可以这样设计：

```ts
interface CacheEntry<T> {
  data: T
  expiresAt: number
  inflight?: Promise<T>
  tags: Set<string>
}
```

`expiresAt` 控制新鲜度，`inflight` 负责并发去重，`tags` 用于批量失效。

## TTL

读取时先判断是否过期：

```ts
const cached = cache.get(key)

if (cached && cached.expiresAt > Date.now()) {
  return cached.data
}
```

短生命周期的状态接口可以设置几秒或几十秒。静态列表可以稍长，但不能让用户看到明显过期数据。

## 并发去重

同一个 key 正在请求时，后续调用应该复用同一个 Promise：

```ts
if (cached?.inflight) {
  return cached.inflight
}

const request = fetchData().finally(() => {
  if (cache.get(key)?.inflight === request) {
    delete cache.get(key)!.inflight
  }
})
```

不要直接在 `finally` 里无条件删除，否则两次请求交错时可能误删新请求。

## SWR

缓存过期后，可以先把旧数据返回，再在后台刷新：

```ts
if (cached) {
  void refresh(key)
  return cached.data
}
```

SWR 适合详情页和列表，但不适合支付、提交、签名等强一致性操作。写操作必须等待真实响应。

## Tag 失效

写完任务状态后，只清理当前详情缓存是不够的。任务列表、数量统计、侧栏状态可能同时失效。

```ts
cache.put(key, {
  data,
  expiresAt: Date.now() + ttl,
  tags: new Set(['task-list', 'task-status'])
})

cache.invalidateByTag('task-status')
```

Tag 应该由业务动作定义，不应该随手使用 `clearAll()`。全量清理短期能解决状态问题，长期会掩盖依赖关系。

## 注意点

- 缓存 key 要包含 query、分页和用户身份
- POST、PUT、DELETE 不要做透明缓存
- 旧数据展示时要有刷新标记
- 请求失败不能覆盖已有数据
- 清理逻辑要和接口错误码一起测试
$$,
  'PUBLISHED',
  12,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
),
(
  'spring-session-auth-design',
  'Spring Boot 后台认证与会话设计',
  '从密码哈希、会话 token、Cookie、CSRF 到限流，整理个人后台的安全边界。',
  '工程实践',
  '["Spring Boot","安全","认证","PostgreSQL"]',
  $$
## 认证链路

后台不保存明文密码，也不把用户名放在普通查询参数里。

```text
POST /api/admin/auth/login
  -> 校验用户名和密码
  -> 生成随机会话 token
  -> token 原文返回浏览器
  -> 数据库保存 token 的 SHA-256
```

浏览器通过 HttpOnly Cookie 保存 token。服务端收到请求后重新计算哈希，再和数据库比较。

## 密码哈希

密码使用 Argon2id：

```text
Argon2id
  -> 盐
  -> 迭代
  -> 内存成本
  -> 并行度
```

数据库只保存编码结果，不保存原始密码。修改密码后删除该管理员的全部旧会话，避免其他设备继续使用旧密码访问。

## 会话 token

随机 token 使用安全随机数：

```text
32 bytes
  -> Base64 URL
  -> 数据库保存 SHA-256
```

不要在数据库保存 token 原文。即使数据库被读取，攻击者也不能直接拿 token 登录。

会话表至少包含：

- token 哈希
- 管理员 ID
- 过期时间

定时清理过期记录，避免会话表无限增长。

## Cookie

后台 Cookie 设置：

```text
HttpOnly
SameSite=Strict
Secure（HTTPS 下）
Path=/
```

HttpOnly 防止普通 JavaScript 读取会话。CSRF 使用单独的、可由前端读取的 token Cookie。

## CSRF

写操作前先获取 token：

```text
GET /api/public/csrf
  -> 返回 CSRF token
  -> 写入 Cookie
```

前端读取 Cookie 后，在后续请求中携带：

```text
X-XSRF-TOKEN: <token>
```

只有 GET、POST、PUT、DELETE 中真正改变状态的操作才需要保护。公开读取接口不要要求 CSRF。

## 登录限流

限流按客户端地址记录时间窗口：

```text
同一地址
  10 分钟内
  最多 N 次失败
```

成功登录后清空该地址的失败计数。不要只在前端禁用按钮，后端限流才是关键。

## 其他边界

- 登录接口不能返回具体是用户名错误还是密码错误
- 管理接口统一做权限检查
- 审计日志不记录密码和请求正文
- 后台 session 和内容 API 分开
- 图片、Markdown 和搜索输入都要在服务端限制长度和类型
$$,
  'PUBLISHED',
  13,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
),
(
  'warframe-long-term-notes',
  'Warframe 长期游玩的几点记录',
  '从资源循环、任务机制到更新节奏，记录长时间游戏库里的观察。',
  '杂谈',
  '["Warframe","游戏杂谈","Steam"]',
  $$
## 为什么时长会累积

Warframe 不是靠单次通关结束的游戏。日常刷材料、制作装备、提升段位、活动任务和版本更新，会不断带来新的短目标。

Steam 库当前记录时长超过 1300 小时。长时间游玩主要来自重复任务和不同版本的装备目标。

## 装备和资源循环

装备需要通过蓝图制作。制作需要材料和等待时间，所以玩家通常会同时准备多个目标：

```text
获取蓝图
  -> 刷对应材料
  -> 启动制作
  -> 领取成品
  -> 升级和配置
```

前期比较累的是材料来源分散。后面开了更多地图和模式后，获取速度会快很多。

## 任务机制

不同任务对角色配置的要求不同。部分任务适合快速跑图，部分需要持续生存，还有一部分需要团队配合完成目标。

实际体验中，任务失败的判断不复杂，但能不能稳定完成取决于装备、模组和对地图机制的理解。

## 更新节奏

Warframe 的内容更新比较频繁。新战甲、新任务、新武器和数值调整都可能改变旧配置。

好处是总能找到事情做，坏处是刚整理好的配置可能很快要重看。

## 适合什么样的玩法

如果只想快速通关一个主线故事，这个游戏不短。更适合能接受重复任务、刷材料和装备配置的玩家。
$$,
  'PUBLISHED',
  8,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
),
(
  'apex-legends-shooter-notes',
  'Apex Legends 的射击节奏和团队体验',
  '记录这款大逃杀游戏在移动、交火、团队配合和版本变化上的长期观察。',
  '杂谈',
  '["Apex Legends","游戏杂谈","Steam"]',
  $$
## 游戏节奏

Apex Legends 和传统竞技射击不同。角色技能、滑铲、护甲、复活机制和队伍配合让战斗节奏更复杂。

Steam 记录时长约 466 小时。大量时间并不是纯对枪，还包括跳点选择、搜物资、转点、拉扯和等待复活。

## 移动和枪法

移动不只是跑和跳：

- 滑铲用于快速脱离掩体
- 连续移动保持节奏
- 技能可以改变交战位置

枪法需要压枪和快速开镜，但团队信息同样重要。什么时候开战、什么时候撤离，往往比单次准度更影响结果。

## 队伍配合

三名玩家组成一队，角色可以形成不同组合。好的配合通常表现在：

- 报点清楚
- 不单独脱离队伍
- 资源合理分配
- 倒地后能判断是否救援
- 交火前统一目标

匹配到陌生玩家时，信息沟通不稳定是常见问题。

## 版本变化

角色强度、武器、地图和游戏机制经常调整。旧配置可能很快失效，需要持续熟悉版本。

## 总结

这个游戏上手快，但上限较高。适合愿意练习移动、枪法和团队沟通的玩家。单排的随机性比固定队伍大很多。
$$,
  'PUBLISHED',
  8,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
),
(
  'rag-retrieval-evaluation',
  'RAG 检索质量如何评测',
  '把检索、引用和回答质量拆开评测，避免只看最终文本是否通顺。',
  'AI 学习',
  '["RAG","评测","AI 工程"]',
  $$
## 为什么要单独评测检索

一个 RAG 回答看起来通顺，不代表它使用了正确资料。模型可能忽略检索结果，也可能检索到无关内容后强行回答。

所以评测至少要分三层：

1. 检索内容是否相关
2. 引用是否来自给定资料
3. 最终回答是否准确

## 建立题目集

准备几十条真实问题，每条包含：

```text
问题
期望答案
必须出现的依据
不应出现的错误信息
```

题目来源最好是真实业务问题，不能只用模型生成的题目。模型生成的题目会偏向它已经擅长的情况。

## 检索指标

简单指标可以使用 Precision 和 Recall：

```text
Precision = 检索结果中相关内容 / 返回结果总数
Recall    = 命中的相关内容 / 测试集中相关内容总数
```

还可以记录：

- Top-K 命中率
- 正确内容出现在第几名
- 重排前后排名变化
- 查询失败率

向量检索不是唯一手段。关键词、过滤条件和重排模型都可能改变结果。

## 引用检查

每个回答应该标明来源。评测时检查：

- 引用是否真实存在
- 引用内容是否包含答案
- 引用是否和问题有关
- 模型是否把不同来源拼接成新事实

没有命中资料时，应该返回“资料不足”，不能强制生成结论。

## 输出评测

可以分别检查：

```text
完整性：是否回答了问题
准确性：事实是否正确
忠实度：是否忠于资料
拒答：无资料时是否正确拒绝
```

不要只让一个模型给另一个模型打 0-10 分。没有标准答案的评分容易波动。

## 建立回归集

每次修改切分、embedding、检索数量、Prompt 或模型，都用同一组题目重新跑。

重点看：

- 原先正确的题目是否变差
- 检索召回是否下降
- 引用是否减少
- 无资料题是否开始乱答
$$,
  'PUBLISHED',
  11,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
),
(
  'agent-tool-call-design',
  'Agent 工具调用的设计边界',
  '从工具 schema、执行结果、最大步数和失败反馈，整理一个可控的 Agent 工作流。',
  'AI 学习',
  '["Agent","工具调用","AI 工程"]',
  $$
## Agent 不等于无限调用模型

Agent 的核心是把复杂任务拆成步骤，再通过工具读取或改变外部状态。

```text
目标
  -> 当前状态
  -> 选择工具
  -> 执行工具
  -> 检查结果
  -> 继续 / 结束
```

没有边界时，模型可能一直调用工具、重复失败或消耗大量 token。

## 工具定义

工具描述要具体，不能只写“查询数据”。至少包括：

- 名称
- 适用场景
- 参数 schema
- 返回格式
- 失败情况
- 权限范围

例如：

```json
{
  "name": "search_articles",
  "description": "在已发布文章中检索。",
  "parameters": {
    "query": {
      "type": "string"
    },
    "limit": {
      "type": "integer",
      "minimum": 1,
      "maximum": 10
    }
  }
}
```

参数约束要在工具层验证，不能只依赖模型生成正确。

## 执行结果

工具结果需要结构化：

```text
success / failure
data / error
suggestion
```

失败时不能只返回“调用失败”。更有效的信息是：

- 哪里失败
- 可以修改什么
- 是否需要用户补充
- 下一步可以尝试什么

## 最大步数

工作流必须设置上限：

```python
for step in range(max_steps):
    action = model.choose_action(state)
    if action.finish:
        return action.answer
    result = execute_tool(action)
    state.append(result)
```

超过上限时返回中间状态和原因，而不是继续无限循环。

## 权限

工具权限要和业务隔离：

- 只读工具不能修改数据
- 写操作必须经过确认
- 文件操作限制目录
- 网络操作限制域名
- 敏感数据不能进入日志

不要给 Agent 一个“可以执行任意代码”的万能工具，除非确实需要，并且有沙箱和审计。

## 状态和可观测性

每次步骤记录：

- 用户目标
- 模型选择的动作
- 工具参数
- 工具输出
- 消耗时间
- 结束原因

调试 Agent 问题时，首先看工具是否成功，再看模型是否根据结果调整了下一步。
$$,
  'PUBLISHED',
  11,
  CURRENT_TIMESTAMP,
  CURRENT_TIMESTAMP
);
