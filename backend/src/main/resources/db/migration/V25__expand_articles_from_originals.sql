UPDATE articles SET content = content || $$

## 更具体的落地方案

### 分层

```text
Browser
  -> Nginx
  -> Nuxt SSR
  -> Spring Boot API
  -> PostgreSQL
```

Controller 只处理 HTTP 入参和响应，Service 放业务规则，DAO 只做 SQL，Entity 描述数据库结构。

### 数据库约定

每张内容表都包含：

- `id`
- `slug`
- `created_at`
- `updated_at`
- 逻辑删除标记

文章表还需要 `status`、`published_at`、`reading_minutes`。

### 接口约定

统一返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

异常由全局处理器统一返回，不把 Java 异常直接抛给前端。

### 发布流程

```text
构建后端
  -> 备份旧版本
  -> 上传新包
  -> 检查健康接口
  -> 切换前端
  -> 验证页面
```

发布失败要能回滚到上一版，不能只记录“上线成功”。
$$ WHERE slug='build-a-reliable-personal-site';

UPDATE articles SET content = content || $$

## 常见功能废墟案例

### 案例一：后台功能越来越多

一开始只做文章，后来加简历、项目、AI、游戏、统计、留言。

如果每个模块没有独立数据表、独立后台入口和独立错误处理，页面会越来越难维护。

### 案例二：通用逻辑复制到页面

同一个请求缓存逻辑在三个页面复制三次，修改缓存策略时漏掉一个页面。

正确做法是抽成公共方法，所有页面统一调用。

### 案例三：废弃接口长期保留

旧接口没有调用方，却继续占用代码、测试和文档。

每季度检查一次：

- 接口是否有调用
- 组件是否被引用
- 依赖是否仍然需要
- 注释是否还有意义

### 治理检查表

- 新功能是否有数据模型
- 新页面是否复用公共组件
- 新接口是否有统一响应
- 新依赖是否可替代
- 新功能是否能下线

如果功能不能下线，就不要开始做。
$$ WHERE slug='personal-site-feature-bloat';

UPDATE articles SET content = content || $$

## 攻击路径示例

假设系统只有前端按钮隐藏：

```text
用户打开控制台
  -> 找到删除按钮
  -> 修改 DOM 显示
  -> 点击删除
```

更直接的方式是：

```text
curl -X DELETE /api/admin/articles/1
```

服务端没有校验管理员权限时，这条请求会成功。

### 服务端校验顺序

```text
读取 token
  -> 解析用户
  -> 检查角色
  -> 检查资源归属
  -> 检查资源状态
  -> 执行删除
  -> 写入审计
```

### 前端可以保留的权限能力

- 动态菜单
- 按钮显隐
- 路由拦截
- 无权限提示
- 提交前二次确认

这些能提升体验，但不能作为安全依据。

### 批量资源接口

批量接口要逐条校验，不能只看第一条：

```java
for (Long id : ids) {
  if (!permissionService.canDelete(userId, id)) {
    throw new ForbiddenException(id);
  }
}
```

前端隐藏权限后，还要用直接调用接口的方式测试一遍。
$$ WHERE slug='frontend-security-is-not-ui-permission';

UPDATE articles SET content = content || $$

## 完整优化链路

### 网络层

```text
GET /articles?page=1&size=20
```

后端限制最大 pageSize，避免一次返回几千条。

### 数据层

前端根据需求过滤字段，不把全部字段传入组件。

### 逻辑层

使用计算属性时，避免在模板中执行复杂函数。长列表每行只绑定必要数据。

### 渲染层

虚拟列表只在无法分页时使用。

## 高度不固定的处理

```ts
const measuredHeights = new Map<number, number>()

function measureRow(index: number, height: number) {
  measuredHeights.set(index, height)
}
```

通过 `ResizeObserver` 监听行高变化，再更新总偏移。

### 滚动位置

筛选后要计算新的 startIndex：

```ts
const newStart = Math.floor(scrollTop / averageHeight)
```

不要直接复用原来的 index。

### 适合虚拟列表的场景

- 日志
- 实时消息
- 未分页的监控数据

### 不适合的场景

- 普通分页表格
- 数据量只有几十条
- 每行高度不确定且频繁变化
$$ WHERE slug='virtual-list-is-not-enough';

UPDATE articles SET content = content || $$

## 表结构设计

```text
存证表
  id
  business_id
  data_hash
  prev_hash
  timestamp_source
  created_at
```

`data_hash` 不存原始数据，只存摘要。

### 校验流程

```text
读取业务数据
  -> 计算 SHA-256
  -> 查存证记录
  -> 比对摘要
  -> 从最后一条向前验证链式关系
```

### 链式校验伪代码

```java
List<Proof> proofs = repository.findByBusinessId(id);

for (int i = 1; i < proofs.size(); i++) {
  String expected = proofs.get(i - 1).getDataHash();
  if (!expected.equals(proofs.get(i).getPrevHash())) {
    throw new TamperDetectedException(i);
  }
}
```

### 时间戳

不要直接信任客户端时间。服务端从可信时间源获取时间，再写入存证。

### 数据更新

业务修改后生成新的存证记录，不覆盖旧记录。这样历史仍然可验证。
$$ WHERE slug='certificate-system-retrospective';

UPDATE articles SET content = content || $$

## 数据模型示例

### 原始资源层

```text
resource
  id
  owner_id
  resource_type
  data_ref
```

### 授权链路层

```text
authorization
  id
  resource_id
  grantor_id
  grantee_id
  start_time
  end_time
  fields
```

### 操作审计层

```text
audit_log
  id
  operator_id
  action
  resource_id
  before_state
  after_state
  created_at
```

### 权限判断

```java
boolean canRead = permissionService.hasFieldPermission(
    userId,
    resourceId,
    field
);
```

权限不是只有“能看/不能看”，还要分字段、分时效。

### 迭代建议

- 先确定资源、授权和审计关系
- 再写页面
- 权限校验集中在一个服务
- 每次授权变化都写审计
$$ WHERE slug='data-circulation-platform-retrospective';

UPDATE articles SET content = content || $$

## 认证时序

```text
客户端
  1. 登录
  2. 获得 AccessToken
  3. 请求头带 Authorization
  4. 后端校验签名
  5. 返回数据
```

### 后端过滤器

```java
String header = request.getHeader("Authorization");
String token = header.replace("Bearer ", "");

Jws<Claims> jws = Jwts.parser()
    .verifyWith(secretKey)
    .build()
    .parseSignedClaims(token);
```

### 双 token 流程

```text
AccessToken 过期
  -> 使用 RefreshToken
  -> 后端校验 RefreshToken
  -> 生成新 AccessToken
```

RefreshToken 要能吊销，不能只靠 JWT 无状态。

### 常见配置

```yaml
jwt:
  access-expire: 15m
  refresh-expire: 7d
```

不要把所有接口都放开，管理接口必须有全局拦截。
$$ WHERE slug='spring-session-auth-design';

UPDATE articles SET content = content || $$

## 缓存类结构

```ts
interface CacheStore {
  data: unknown
  expiresAt: number
  inflight?: Promise<unknown>
  tags: string[]
}
```

### Key 生成

```ts
function makeKey(url: string, params: Record<string, unknown>) {
  return url + ':' + JSON.stringify(params)
}
```

### 写操作清理

```ts
function invalidateByTag(tag: string) {
  for (const [key, entry] of cache.entries()) {
    if (entry.tags.includes(tag)) cache.delete(key)
  }
}
```

### 并发去重

```ts
if (!cache.has(key)) {
  const promise = request(url).finally(() => {
    if (cache.get(key)?.inflight === promise) {
      cache.delete(key)
    }
  })
  cache.set(key, { data: null, expiresAt: now + ttl, inflight: promise })
  return promise
}
```

### 分级建议

静态接口缓存 10 分钟，列表缓存 60 秒，状态接口不缓存。
$$ WHERE slug='vue-request-cache-implementation';

UPDATE articles SET content = content || $$

## 学习路线分阶段

### 第 1 阶段：Prompt

做 10 个任务：

- 结构化输出
- 内容摘要
- 分类
- 提取

每个任务都写输出约束。

### 第 2 阶段：RAG

准备 20 篇真实文档，跑：

- 清洗
- 分块
- 向量化
- 检索
- 重排
- 生成

### 第 3 阶段：Agent

做一个带 3 个工具的小项目：

- 搜索
- 查询状态
- 写结果

重点看失败和限制。

### 第 4 阶段：工程化

- 日志
- 权限
- 限流
- 成本统计
- 评测集

### 判断标准

能独立完成一个应用，而不是只跑通官方示例。
$$ WHERE slug='notes-on-ai-learning';

UPDATE articles SET content = content || $$

## 工具定义完整示例

```json
{
  "name": "search_documents",
  "description": "在已发布文档中搜索相关内容",
  "parameters": {
    "type": "object",
    "properties": {
      "query": { "type": "string" },
      "limit": { "type": "integer", "minimum": 1, "maximum": 10 }
    },
    "required": ["query"]
  }
}
```

### 执行循环

```python
state = []
for step in range(max_steps):
    action = choose_action(state)
    if action.type == "finish":
        return action.output
    result = execute_tool(action)
    state.append(result)
raise MaxStepsError(state)
```

### 高危拦截

```python
if tool.risk_level == "high":
    if not human_confirm(action):
        return "需要人工确认"
```

### 日志字段

```text
step
tool
input
output
error
elapsed_ms
```
$$ WHERE slug='agent-tool-call-design';

UPDATE articles SET content = content || $$

## 复杂链路风险场景

### 场景一

模型第一次解析工具参数错误，后续所有步骤基于错误参数执行。

### 场景二

上下文超过模型限制，最后几步不再参考前面信息。

### 场景三

工具失败后模型重复调用相同参数，直到轮次上限。

### 场景四

Agent 拥有高权限工具，误删数据后才被发现。

## Workflow 与 Agent 选择

```text
步骤固定 -> Workflow
分支多且动态 -> Agent
高权限操作 -> 必须人工确认
```

### 稳定性优先

生产系统可以先固化成功路径，再开放动态路径。

Agent 的作用不是展示模型能力，而是解决固定流程解决不了的任务。
$$ WHERE slug='agent-complexity-is-not-ability';

UPDATE articles SET content = content || $$

## 流水线每个阶段的细节

### 预处理

```text
去除页眉页脚
去除广告
合并重复段落
保留标题层级
```

### 语义分块

先按标题切分，再在语义完整的位置断句，不固定 500 字。

### 混合检索

```text
向量检索 Top20
BM25 Top20
合并去重
Rerank Top5
```

### 生成约束

```text
只使用给定资料
引用编号
资料不足时拒答
不编造来源
```

### 评估

记录：

- 召回率
- 精准率
- 引用命中
- 拒答率

每次改动跑同一组测试。
$$ WHERE slug='rag-not-vector-demo';

UPDATE articles SET content = content || $$

## 测试集格式

```json
[
  {
    "question": "如何配置 Redis？",
    "reference": ["config/redis.md"],
    "must_have": ["bind", "port"],
    "must_not_have": ["MySQL"]
  }
]
```

### 计算指标

```text
Recall = 命中的标准片段 / 标准片段总数
Precision = 有效召回 / 总召回
```

### 问题定位

召回率低：

- 分块粒度
- Top-K
- 关键词检索

精准率低：

- 重排
- 语料噪声
- 检索范围

### 回归测试

每次修改后记录：

- 原先正确的题目是否还正确
- 错误是否修复
- 是否出现新错误

不要只看一条回答。
$$ WHERE slug='rag-retrieval-evaluation';

UPDATE articles SET content = content || $$

## 扩展实现细节

### 前四章

前四章通过场景高低差、建筑轮廓和隐藏路径引导探索。箱庭与大地图结合得比较紧。

### 后两章

复用内容变多，关卡结构变线性，空气墙和导航体验下降。

### 战斗

Boss 战做得精细，但杂兵和变身缺少差异化。

变身需要：

- 专属被动
- 场景交互
- 资源差异

### 叙事

可以增加：

- 主线精简引导
- 核心剧情和彩蛋分层
- 角色动机补全

### 结论

制作能力已经证明，设计短板也应该被正视。
$$ WHERE slug='black-myth-wukong-strict-review';

UPDATE articles SET content = content || $$

## 市场现象展开

### 模板复用

换 IP、换立绘、换场景，但核心玩法不变。

### 付费设计

卡池、战令、限时活动组成完整收割链路。

### 玩家反应

一开始新鲜，后期倦怠，最终退坑。

### 改善方向

- 玩法投入
- 数值平衡
- 内容更新
- 减少强制在线
- 控制数值膨胀

### 可落地判断

如果游戏只靠每日任务留人，说明核心玩法不足。
$$ WHERE slug='mobile-market-2023-2026-review';

UPDATE articles SET content = content || $$

## 开放世界细节

### 环境引导

远处建筑、高差、怪物位置都成为方向线索。

### 空间结构

表层大地图连接多个地下箱庭，捷径让探索形成回环。

### 不依赖地图标点

玩家靠视觉判断去哪，而不是跟着箭头走。

### 对行业的意义

开放世界不是堆面积，而是空间关系和环境信息要清楚。
$$ WHERE slug='elden-ring-open-world-review';

UPDATE articles SET content = content || $$

## 细节系统拆解

### NPC

NPC 有作息，会起床、工作、睡觉，不会一直站在原地。

### 动物

动物会捕食、迁徙、休息，不只是随机刷怪。

### 物理

雪地、泥地、枪械状态都会变化。

### 节奏

慢交互强化代入感，但不适合所有玩家。

### 当代缺失

很多开放世界靠收集物和据点填充，缺少真实生态。
$$ WHERE slug='red-dead-redemption-2-review';

UPDATE articles SET content = content || $$

## 双循环如何互促

### 探索给经营提供材料

```text
捕捞新鱼
  -> 寿司店新菜单
  -> 更高收入
```

### 经营给探索提供装备

```text
收入
  -> 升级氧气瓶
  -> 去更深海域
```

### 避免疲劳

数值简单、反馈直接，不会强制玩家长期挂机。

### 短板

后期支线重复，终局深度有限，这是小团队体量限制。
$$ WHERE slug='dave-the-diver-review';

UPDATE articles SET content = content || $$

## 重制版对比

### 真正重制

- 重做底层
- 重构战斗
- 修复旧问题
- 补全人物动机

### 模板炒冷饭

- 高清贴图
- 保留旧 bug
- 保留落后交互
- 只靠情怀

### 生化 4 重制做了哪些

- 移动射击
- 匕首弹反
- 潜行处决
- 删除强制 QTE
- 优化人物

### 行业判断

重制不能只升级美术。旧系统问题必须一起处理。
$$ WHERE slug='resident-evil-4-remake-review';

UPDATE articles SET content = content || $$

## 评价体系问题

### 专业评审

看叙事、表达、实验性。

### 大众玩家

看内容、流畅度、可玩性。

### 冲突来源

两套标准不同，不代表哪一方错误。

### 平台问题

宣发影响评审认知，独立作品曝光不足。

### 改善方向

- 平衡权重
- 增加打磨维度
- 分赛道评选

奖项必须清楚自己在评价什么。
$$ WHERE slug='tga-awards-review';
