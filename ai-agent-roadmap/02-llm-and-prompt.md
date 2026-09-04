# 02 · LLM 基础 + 提示工程

> 时长：2 周。你不需要成为算法工程师，但要建立对 LLM 行为机制的**直觉**——这是写好 Agent 的前提。

---

## 2.1 LLM 工作原理（直觉版，够用就行）

- **Token**：文本被切分成 token（中文约 1 字 ≈ 1~2 token），计费按 token。
- **预测下一个 token**：LLM 本质是"根据前文预测下一个最可能的 token"，生成是逐 token 的。
- **上下文窗口**：一次能塞进多少 token（如 128K/200K）。超出的部分会被截断——**这是 Agent 记忆设计的根本约束**。
- **温度 temperature**：越低越确定（写代码），越高越发散（创意）。0~1 常用。
- **System / User / Assistant 角色**：System 定规则、User 是输入、Assistant 是模型输出。
- **能力边界**：会"自信地编造"（幻觉）、会数错 token、数学和长上下文容易出错——设计 Agent 时要规避。

## 2.2 提示工程（Prompt Engineering）

核心方法（从最常用到进阶）：

1. **结构化 System Prompt**：角色 + 任务 + 约束 + 输出格式。用明确的格式块，别写散文。
2. **Few-shot**：给 2~5 个输入输出示例，比纯描述可靠。
3. **思维链 CoT**：让模型"先推理再回答"，复杂问题准确率大幅提升。
4. **结构化输出**：要求输出 JSON / 用 tool calling 强制 schema（比让模型"按 JSON 输出"稳定得多）。
5. **自我修正 / Reflection**：让模型检查自己的答案再改一遍。
6. **分步与拆解**：复杂任务拆成多步调用，别一次塞给模型。

> 注意：提示工程正在被"结构化 tool calling + 代码执行"部分替代，但理解它仍是基本功。

## 2.3 Function Calling / Tool Use（Agent 的基石）

这是从"聊天机器人"到"Agent"的分水岭：

- **原理**：给模型声明一组函数（name + 参数 JSON Schema），模型不执行，只返回"该调用哪个函数 + 参数"。由你的代码真正执行并把结果回传给模型。
- **为什么重要**：模型因此能查数据库、调 API、执行代码、访问文件——突破了"只靠训练知识"的局限。
- **实现方式对比**：
  - OpenAI / Anthropic 原生 function calling（最简单，先学这个）
  - MCP 工具（跨应用的标准协议，你已有基础）
  - 框架封装（LangGraph `tool`、OpenAI Agents SDK `@function_tool`）

**动手产出（第 2 周）**：
- P1-1：用 OpenAI/Anthropic/DeepSeek 任一 SDK，定义 3 个函数（查天气、算数学、读本地文件），实现"模型决定调用 → 代码执行 → 结果回传"的完整循环。
- P1-2：做一个"多轮工具调用"场景（比如：用户问"上海明天适合户外跑步吗"→ 模型调天气函数 → 返回结果 → 综合回答）。

## 2.4 选哪个模型 / 哪个 API？

- **上手**：DeepSeek / Qwen（国内直连、便宜、中文好）或 OpenAI / Anthropic（生态最全、文档最好）。
- **你已有的优势**：火山引擎 SSO + API Key 管理直接复用，豆包/DeepSeek 都是火山系。
- **开发建议**：用 `OpenAI 兼容格式` 的 SDK（多数国内模型都兼容），代码可移植。

## 2.5 推荐资源

- OpenAI Function Calling 文档：<https://platform.openai.com/docs/guides/function-calling>
- Anthropic Tool use 文档：<https://docs.anthropic.com/en/docs/build-with-claude/tool-use>
- 吴恩达 DeepLearning.AI 免费短课（搜 "ChatGPT Prompt Engineering for Developers" 与 "Building Agentic RAG"）：<https://www.deeplearning.ai/short-courses/>
- Google 的 Prompting 指南（全面）：<https://developers.google.com/machine-learning/resources/prompt-engineering>

## 2.6 本阶段完成标准

- [ ] 能解释 token、上下文窗口、temperature 的作用
- [ ] 能写出结构化 System Prompt 并稳定输出 JSON
- [ ] 能独立实现"工具调用闭环"（模型→工具→回传→再回答）
- [ ] 知道幻觉、长上下文失效等边界，并会在设计时规避
