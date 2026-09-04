# 11 · 全套资源汇总

> 本页汇总整套路线的全部官方资源。**以官方文档为主**，因为 Agent 生态迭代快，教程类内容容易过期。

---

## 11.1 官方文档（第一优先级）

| 资源 | 链接 | 用途 |
|---|---|---|
| Python 官方教程 | https://docs.python.org/3/tutorial/ | Python 基础 |
| uv（Python 包管理） | https://docs.astral.sh/uv/ | 环境/依赖管理 |
| Pydantic | https://docs.pydantic.dev/ | 结构化数据校验 |
| FastAPI | https://fastapi.tiangolo.com/ | Agent 服务 API 层 |
| OpenAI API 文档 | https://platform.openai.com/docs/ | LLM / Function Calling |
| Anthropic 文档 | https://docs.anthropic.com/ | LLM / Tool use / 工程博客 |
| LangGraph 文档 | https://langchain-ai.github.io/langgraph/ | Agent 编排框架（主力） |
| OpenAI Agents SDK | https://openai.github.io/openai-agents-python/ | 轻量 Agent |
| MCP 官方文档 | https://modelcontextprotocol.io/ | MCP 协议 |
| MCP Python SDK | https://github.com/modelcontextprotocol/python-sdk | 自建 MCP Server |
| MCP 示例 Servers | https://github.com/modelcontextprotocol/servers | 参考实现 |
| pgvector | https://github.com/pgvector/pgvector | 向量数据库 |
| Langfuse | https://langfuse.com/docs | 可观测/评估 |
| RAGAS | https://docs.ragas.io/ | RAG 评估 |
| 微软 GraphRAG | https://microsoft.github.io/graphrag/ | 知识图谱 RAG |
| OWASP LLM Top 10 | https://genai.owasp.org/ | Agent 安全 |
| 字节 Coze | https://www.coze.cn/ | 低代码 Agent 平台 |
| Dify | https://dify.ai/ | 低代码 Agent 平台 |

## 11.2 免费课程

- 吴恩达 DeepLearning.AI 短课（免费，每门 1~2 小时）：
  - "ChatGPT Prompt Engineering for Developers"
  - "Building Agentic RAG with LlamaIndex"
  - "Building and Evaluating Advanced RAG"
  - "Functions, Tools and Agents with LangChain"
  - 入口：https://www.deeplearning.ai/short-courses/
- Google Prompting 指南：https://developers.google.com/machine-learning/resources/prompt-engineering
- Hugging Face 免费课程（LLM 原理）：https://huggingface.co/learn
- exercism Python 练习：https://exercism.org/tracks/python

## 11.3 必读经典论文（理解 Agent 原理）

| 论文 | 一句话 |
|---|---|
| ReAct（2022） | 思考-行动-观察循环，Agent 的基础范式 |
| Toolformer（2023） | 语言模型学会自己决定何时调用工具 |
| Reflexion（2023） | Agent 自我反思迭代改进 |
| Plan-and-Solve（2023） | 先规划再执行，提升复杂推理 |
| GraphRAG（2024） | 用知识图谱做多跳推理的 RAG |
| MRKL / Gorilla / ToolLLM | 工具调用相关（进阶选读） |

- 搜索入口：https://arxiv.org/（搜论文名）或 https://huggingface.co/papers

## 11.4 值得精读的工程博客（官方出品，质量高）

- Anthropic Engineering：https://www.anthropic.com/engineering
  - 必读：《Building effective agents》《Writing effective tools for agents》
- OpenAI Engineering：https://openai.com/index/?s=engineering
- LangChain Blog：https://blog.langchain.com/
- 微软 AI 博客：https://blogs.microsoft.com/ai/

## 11.5 社区与信息源（保持前沿）

- GitHub Trending（AI 相关）：https://github.com/trending
- Hacker News：https://news.ycombinator.com/
- X（Twitter）关注 AI 工程师/研究博主
- 中文社区：知乎 AI 话题、AI 相关公众号、豆包/DeepSeek 开发者社区
- 开源项目 Star 跟踪：Awesome LLM / Awesome-Agent 类仓库（搜 GitHub "awesome llm agents"）

## 11.6 国内模型 API 入口（你已会配火山 SSO）

- 火山引擎方舟（豆包 / DeepSeek 等模型）：https://www.volcengine.com/product/ark
- DeepSeek 开放平台：https://platform.deepseek.com/
- 阿里百炼：https://bailian.console.aliyun.com/
- 智谱开放平台：https://open.bigmodel.cn/

## 11.7 面试/作品集补充

- 简历排版与项目 README 参考：GitHub 上搜 "agent portfolio" 或直接参考你已开源的个人网站仓库风格。
- 面试题题库：GitHub 搜 "llm-interview-questions" 类仓库（注意甄别时效）。
