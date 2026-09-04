# AI Agent 开发 · 转行学习路线（全套资料）

> 面向：已有 Java/Spring 后端 + 前端（Nuxt）+ Linux 运维基础，且**已实操过 OpenClaw 网关、MCP 插件、火山引擎 SSO、API Key 管理**的开发者。
>
> 目标：3~6 个月内具备独立开发、部署、评估 AI Agent 产品的能力，并能拿出可展示的作品集完成转行。

---

## 一、先认清你的起点（不要从头学）

你已经掌握的、在 Agent 开发里直接可复用的能力：

| 已有能力 | 对应 Agent 开发中的价值 |
|---|---|
| Java/Spring Boot + REST API | 后端服务化、API 设计、鉴权、限流——部署 Agent 服务的核心能力 |
| 前端 Nuxt + 交互设计 | 给 Agent 产品做 Web UI（Chat 界面、工作流画布） |
| Linux + systemd + nginx + 部署 | 模型网关、Agent 服务上线的全部 DevOps 链路 |
| OpenClaw / MCP 插件实操 | **Agent 工具生态的核心协议 MCP，你已经有真实手感** |
| 火山引擎 SSO / API Key 管理 | 调用各家 LLM API、成本与密钥治理 |
| 离散数学 / 数据结构 | 理解 Agent 的状态机、图编排（LangGraph 底层就是图） |

**结论：你缺的不是"工程能力"，而是三样东西——**
1. **Python 生态**（Agent 框架主流在 Python）
2. **LLM / Agent 的领域知识**（提示工程、工具调用、记忆、规划、RAG）
3. **作品集**（用项目证明你会做，这是转行的敲门砖）

---

## 二、学习路线总览（10 个阶段）

```
阶段0  思维转变与方向定位（1 周）
  │
阶段1  Python 快速过渡（2 周）
  │
阶段2  LLM 基础 + 提示工程（2 周）
  │
阶段3  Agent 核心框架：LangGraph / 多框架选型（3 周）
  │
阶段4  MCP 与工具生态深化（2 周）← 你有基础，重点拔高
  │
阶段5  RAG 与知识增强（2 周）
  │
阶段6  Agent 工作流 / 状态机 / 可观测性（2 周）
  │
阶段7  部署与产品化（2 周）← 你的强项，做深做亮
  │
阶段8  进阶方向：多模态 / 浏览器 Agent / 安全（持续）
  │
阶段9  项目作品集 + 面试准备（贯穿到最后 4 周）
```

- 前 8 阶段约 **3 个月**，9 阶段冲刺 **1 个月**，合计约 4 个月可达到投简历水平。
- 每阶段都配了「动手产出」——**不要只看不写代码**，转行靠作品说话。

---

## 三、文档目录

| 文件 | 内容 |
|---|---|
| `01-python-foundation.md` | Python 快速过渡（面向 Java 开发者）+ 异步 + 类型注解 + 环境管理 |
| `02-llm-and-prompt.md` | LLM 原理直觉 + 提示工程 + Function Calling / Tool Use |
| `03-agent-core.md` | Agent 架构范式 + LangGraph + 记忆 + 多 Agent + 框架选型 |
| `04-mcp-and-tools.md` | MCP 协议详解 + 自建 MCP Server（Python / TS）+ 工具工程化 |
| `05-rag-and-knowledge.md` | Embedding + 向量库 + RAG 全流程 + 进阶（HyDE / Rerank / GraphRAG） |
| `06-workflow-observability.md` | 状态机 / 图编排 / 异步 / 日志 / 评估（Evals） |
| `07-deployment-product.md` | Agent 服务化 + 模型网关 + 成本控制 + 产品化 |
| `08-advanced-topics.md` | 多模态 Agent / 浏览器 Agent / 深度研究 / Agent 安全 |
| `09-projects-portfolio.md` | 5 个从易到难的项目 + 作品集包装 |
| `10-interview-prep.md` | 转行面试常见问题 + 简历策略 |
| `11-resources.md` | 全部官方文档 / 课程 / 书籍 / 论文 / 社区链接汇总 |

---

## 四、三条铁律（写在你开始之前）

1. **动手 > 看教程。** 每个阶段结束必须有一个"能跑起来"的产出，哪怕很糙。
2. **跟官方文档走，不跟二手教程走。** 框架版本迭代快（LangChain/LangGraph、OpenAI SDK 都在快速更新），官方文档永远是最新的，教程经常过期。
3. **围绕一个主线项目积累。** 从阶段 3 开始选一个主题（比如"个人知识库研究助手"），一路做到部署上线，把它打磨成你的代表作——比做 10 个半成品强 10 倍。

---

## 五、每周建议节奏

- **工作日**：每天 1.5~2 小时（通勤/晚间）看文档 + 写 30 分钟代码。
- **周末**：半天整块时间做项目，半天复盘 + 写文档记录（把踩坑写进笔记）。
- 用 GitHub 记录所有练习项目，README 写好架构图和"为什么这么做"，这是给面试官看的。
