# 04 · MCP 与工具生态深化

> 时长：2 周。**这是你的差异化优势区**——你已实操过 OpenClaw/MCP，别人从零学，你直接拔高到"能自研 MCP Server + 工具工程化"。

---

## 4.1 MCP 是什么（回顾 + 拔高）

MCP（Model Context Protocol）是"AI 应用 ↔ 外部工具/数据"的**开放标准协议**，类比"AI 世界的 USB-C"。你的 OpenClaw 网关 + MCP 插件实操，已经摸过它的 Client/Server 模型。

**核心概念三件套（MCP Server 暴露的资源）：**
| 概念 | 作用 | 类比 |
|---|---|---|
| **Tools** | 可被模型调用的函数（最常用） | 函数调用 |
| **Resources** | 只读数据（文件、数据库记录） | GET 接口 |
| **Prompts** | 预置的提示模板 | 复用模板 |

## 4.2 MCP 架构回顾

```
┌──────────────┐   JSON-RPC 2.0   ┌──────────────┐
│ MCP Client   │ ◀───────────────▶ │ MCP Server   │
│ (OpenClaw/   │   stdio / SSE /   │ (工具提供方)  │
│  LangChain)  │   Streamable HTTP│              │
└──────────────┘                  └──────────────┘
```

- **传输方式**：`stdio`（本地子进程，最简单）、`Streamable HTTP`（远程服务，生产常用）、`SSE`。
- 你已经在用 OpenClaw 网关 → 本质就是 MCP Client + 工具聚合层。

## 4.3 动手产出：自建 MCP Server（Python）

**P3-1：做一个"项目文件助手" MCP Server**（5~7 天）
功能：列出目录、读文件、按关键词搜文件、写文件（安全限制在白名单目录）。这就是一个能真正被 Claude/OpenClaw 调用的工具。

用官方 SDK 写，30~60 行核心代码：
```python
from mcp.server.fastmcp import FastMCP

mcp = FastMCP("file-assistant")

@mcp.tool()
def list_files(path: str) -> list[str]:
    """列出目录下的文件名"""
    import os
    return os.listdir(path)

@mcp.tool()
def read_file(path: str) -> str:
    """读取文本文件内容"""
    with open(path, "r", encoding="utf-8") as f:
        return f.read()

if __name__ == "__main__":
    mcp.run(transport="stdio")  # 或 transport="streamable-http"
```

**验收**：用 MCP Inspector 调试工具，或接入 OpenClaw/Claude Desktop 实际调用一次。

**P3-2（进阶，可选）**：把上面的 Server 改成 `streamable-http` 模式部署到服务器，配鉴权（对应你的 Spring Security 经验——token 校验、CORS、限流），这直接就是"生产级工具服务"。

## 4.4 工具工程化（从"能调"到"好用"）

写 Agent 工具不是"写个函数"这么简单，生产级工具要注意：

1. **Schema 质量**：参数名和描述写清楚（模型靠它决定怎么调用），`description` 里写清"什么时候用、传什么"。
2. **错误处理**：工具抛异常要有结构化错误信息返回给模型，让它能自己修正后重试。
3. **安全边界**：白名单路径、命令校验、权限最小化——**这是 MCP 工具最容易出安全问题的地方**（提示注入、路径穿越）。
4. **幂等与限流**：写操作要防重、外部 API 要限流（复用你 MessageRateLimiter 的思路）。
5. **可观测**：每次工具调用记日志（入参、出参、耗时、token），方便调试 Agent 行为。

## 4.5 框架内如何用 MCP 工具

- LangChain / LangGraph：`load_mcp_adapters` / `MCPStdio` 把 MCP Server 包装成 Agent 工具。
- 直接原生：OpenAI/Anthropic 的 MCP 客户端支持（SDK 已内置）。

## 4.6 推荐资源

- MCP 官方文档（协议 + SDK，权威）：<https://modelcontextprotocol.io/>
- MCP Python SDK：<https://github.com/modelcontextprotocol/python-sdk>
- MCP Inspector 调试工具：<https://modelcontextprotocol.io/docs/tools/inspector>
- 官方示例 Servers 仓库：<https://github.com/modelcontextprotocol/servers>

## 4.7 本阶段完成标准

- [ ] 能独立用 FastMCP 写一个可用工具并接入 Agent 实际调用
- [ ] 理解 Tools / Resources / Prompts 三者区别
- [ ] 掌握 stdio 与 streamable-http 两种传输方式
- [ ] 能说出工具的安全边界设计（路径/命令/权限/限流）
