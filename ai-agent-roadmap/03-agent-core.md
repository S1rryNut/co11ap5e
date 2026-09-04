# 03 · Agent 核心框架

> 时长：3 周（最关键的一章）。学完这一章，你就能写出"真正的 Agent"，而不是聊天机器人。

---

## 3.1 Agent 是什么（一图理解）

```
用户请求
   │
   ▼
┌───────────────┐   决定下一步     ┌───────────────┐
│   LLM（大脑）  │ ───────────────▶ │  工具集         │
│  规划 / 推理   │                 │  API / DB / 代码│
└───────────────┘ ◀─────────────── └───────────────┘
   │        ▲       工具执行结果
   │        │
   ▼        │
┌───────────────┐
│  记忆（会话/长期）│
└───────────────┘
```

**Agent = LLM 规划 + 工具调用 + 记忆 + 循环执行**，直到完成目标或达到上限。

## 3.2 核心架构范式（都要懂）

| 范式 | 思路 | 适合场景 |
|---|---|---|
| **ReAct** | 交替"思考(Thought)→行动(Action)→观察(Observation)" | 通用，Agent 的默认基线 |
| **Plan-and-Execute** | 先整体规划步骤，再逐步执行 | 长任务、多步骤 |
| **Reflection / Self-Critique** | 输出后自我检查、迭代修正 | 写作、代码、研究报告 |
| **Multi-Agent** | 多个专职 Agent 协作（规划者/执行者/审查者） | 复杂流水线、研究 |
| **Code-as-Action** | Agent 生成并执行代码来完成任务 | 数据分析、工程任务 |

## 3.3 主流框架选型（2026 视角）

| 框架 | 特点 | 适合 |
|---|---|---|
| **LangGraph** | 图结构状态机，细粒度控制，生产级 | **主力推荐**——你懂数据结构/图论，上手快 |
| **OpenAI Agents SDK** | 轻量、原生 tool calling，代码少 | 快速原型、简单 Agent |
| **CrewAI** | 角色化多 Agent，上手最简单 | 演示、多角色协作场景 |
| **AutoGen (Microsoft)** | 多 Agent 对话驱动 | 研究、复杂协作 |
| **LlamaIndex** | 强在 RAG / 数据接入 | 知识库类应用 |
| **Dify / Coze** | 低代码平台，可视化编排 | 快速验证产品、非工程师 |

> **给你的建议**：主学 **LangGraph**（生产主流 + 贴合你的工程背景），辅学 **OpenAI Agents SDK**（轻量明白原理）。CrewAI/Dify 了解一下能搭 demo 即可。**不要贪多**，框架只是工具，Agent 原理才是内核。

## 3.4 LangGraph 核心概念（结合你的图论知识）

- **State**：Agent 运行时的共享状态（dict / TypedDict），类似一个"工作内存"。
- **Node**：一个处理单元（调 LLM、调工具、写数据库）——就是图里的顶点。
- **Edge**：节点间的转移；**Conditional Edge** = 根据状态条件决定下一步——就是有向图的边 + 分支。
- **Graph 编译**：把节点/边编排成一个可执行的 StateGraph，`invoke()` 执行。
- **Memory / Checkpointer**：跨轮次保存状态（对应你的"持久化会话"），支持断点续跑。
- **Human-in-the-loop**：关键步骤暂停等人确认（审批类 Agent 必备）。

**第一个 LangGraph 例子（概念）**：一个"研究助手"图——
`接收问题 → [节点1: LLM 判断需要哪些信息] → [节点2: 调搜索工具] → [节点3: 汇总答案]`，其中"是否需要再搜一次"由 conditional edge 判断，最多循环 3 次。

## 3.5 记忆系统设计

- **短期记忆**：当前会话的对话历史（注意控制 token，超长要压缩/摘要）。
- **长期记忆**：向量存储（RAG，见第 5 章）存"知识"；KV 存"用户偏好/事实"。
- **实现要点**：会话摘要（每 N 轮让模型总结一次）、消息窗口截断、语义检索最近相关片段。

## 3.6 多 Agent 协作

- **模式**：主管-下属（Orchestrator-Worker）、流水线（Pipeline）、辩论/审查（Critique）。
- **价值**：任务拆分、并行、专业化。**代价**：token 成本翻倍、错误放大、难调试。
- **建议**：能单 Agent 解决就别上多 Agent；多 Agent 一定要加"总协调者"和明确的输入输出契约。

## 3.7 动手产出

- P2-1：用 LangGraph 写"多步工具调用 Agent"（规划→执行→反思，2 个工具以上）。
- P2-2：给 Agent 加"会话摘要记忆"，跑通多轮对话不爆上下文。
- P2-3：做一个"主管 + 2 个专家"的多 Agent 研究助手（可选挑战）。

## 3.8 推荐资源

- LangGraph 官方文档（以它为准，教程易过期）：<https://langchain-ai.github.io/langgraph/>
- LangGraph 官方教程 Concepts 与 Tutorials 从头过一遍
- OpenAI Agents SDK：<https://openai.github.io/openai-agents-python/>
- 必读经典论文（见 `11-resources.md`）：ReAct、Reflexion、Toolformer、Plan-and-Solve

## 3.9 本阶段完成标准

- [ ] 能画出自己 Agent 的状态图（节点/边/条件分支）
- [ ] 能用 LangGraph 跑通含工具调用 + 条件分支 + 循环的 Agent
- [ ] 理解并实现至少一种记忆策略
- [ ] 能说清 ReAct / Plan-and-Execute / Multi-Agent 的区别与适用场景
