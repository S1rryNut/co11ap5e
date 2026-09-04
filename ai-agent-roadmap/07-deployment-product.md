# 07 · 部署与产品化

> 时长：2 周。**这是你最强的一章**——你有 Spring Boot + systemd + nginx 的完整部署经验，这里全部迁移过来，做深做亮就是你的简历王牌。

---

## 7.1 Agent 服务化架构

```
┌─ 前端（Nuxt/React 聊天界面）
│        │ HTTP
│        ▼
┌─ 应用层（FastAPI / Node）← Agent 编排、会话管理
│        │
│        ▼
┌─ Agent 核心（LangGraph 图 / 工具 / RAG）
│        │
│        ▼
├─ 模型网关（OpenAI/DeepSeek/豆包/火山）  ← 你已会配 SSO + Key
├─ 数据库（PostgreSQL + pgvector + Redis）
├─ 可观测（Langfuse / Prometheus + Grafana）
└─ 工具 / MCP（内部 API + 外部服务）
```

## 7.2 每层要点（对照你的既有技能）

1. **API 层**：用 **FastAPI**（Python 主流，自带 OpenAPI 文档 + 异步）。对应你的 Spring 经验——路径、鉴权、限流、CORS 一个不落。
2. **会话管理**：用户会话 / Agent 状态持久化（Redis + PG），断点续跑。
3. **流式输出**：SSE（Server-Sent Events）流式返回 token——聊天体验刚需，前端 Nuxt 用 `fetch` 读流即可。
4. **模型网关**：统一封装多模型（OpenAI 兼容格式），做密钥管理、路由、重试、成本统计。火山引擎 + 各家 key 统一管。
5. **限流与成本**：按用户/按 IP 限流（复用你 MessageRateLimiter 思路）、token 用量统计、预算告警。
6. **安全**：提示注入防护（见第 8 章）、工具权限白名单、敏感信息脱敏、鉴权。

## 7.3 部署（直接复用你现有栈）

- **进程管理**：systemd 服务（对应你 `personal-site-api` 的做法）。
- **反向代理**：nginx 做 TLS + 转发（对应 `personal-site-web`）。
- **容器化（加分）**：Dockerfile + docker-compose（app + pg + redis + langfuse），一键启动。
- **环境变量管理**：`backend.env` 式配置（SMTP/DB/模型 Key 全走 env，不落仓库）——你已经是最佳实践。
- **CI/CD（加分）**：GitHub Actions 自动构建 + 部署。

## 7.4 动手产出

- **P6-1：把 P2 的 Agent 完整产品化**（1.5~2 周）
  - FastAPI 提供 `/chat` 接口 + SSE 流式
  - Nuxt 前端聊天界面（你有现成经验）
  - PG 持久化会话，Redis 做缓存/限流
  - systemd + nginx 部署上线，配 HTTPS
  - 接入 Langfuse 看线上追踪
- **P6-2（加分）**：Docker 化，写 docker-compose 一键部署；写 README 架构图 + 部署文档。

> 这一步做完，你就有第一个"从零到生产上线"的完整 Agent 产品，面试时直接演示。

## 7.5 推荐资源

- FastAPI 官方文档：<https://fastapi.tiangolo.com/>
- SSE 流式（FastAPI StreamingResponse）：<https://fastapi.tiangolo.com/advanced/custom-response/>
- LangGraph 部署（LangGraph Server / 自部署）：<https://langchain-ai.github.io/langgraph/cloud/>

## 7.6 本阶段完成标准

- [ ] Agent 产品能通过 HTTPS 对外提供流式聊天
- [ ] 有完整的环境变量管理（无密钥入库）
- [ ] 有追踪 + 日志 + 限流 + 成本统计
- [ ] 前端能实时显示流式回复
