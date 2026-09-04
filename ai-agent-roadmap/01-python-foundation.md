# 01 · Python 快速过渡（面向 Java 开发者）

> 时长：2 周。你有 Java 基础，Python 学语法是"翻译"不是"学习"。重点放在**面向 AI 生态的 Python**，而不是 Python 全量。

---

## 1.1 语法翻译表（对照 Java 记忆）

| 概念 | Java | Python |
|---|---|---|
| 变量 | `int x = 1;` | `x = 1`（动态类型） |
| 常量 | `final` | 无内置，约定全大写 |
| if | `if (a > b) {}` | `if a > b:` |
| for | `for (int i=0; i<n; i++)` | `for i in range(n)` |
| 列表 | `List<String> list = new ArrayList<>()` | `list = []` |
| 字典 | `Map<String,Object> m = new HashMap<>()` | `d = {}` |
| 函数 | `public int f(int x) { return x; }` | `def f(x): return x` |
| 类 | `class A { private int x; }` | `class A:`（无 private，靠 `_x` 约定） |
| 接口 | `interface` | `Protocol` / ABC |
| 异常 | `try/catch/finally` | `try/except/finally` |
| 包 | `package` + `import` | `import` / `from ... import` |
| 依赖 | Maven/Gradle（pom.xml） | pip + `requirements.txt` / `pyproject.toml` / uv |
| 构建 | `mvn package` | 通常无编译，直接跑脚本 |

## 1.2 第一周：必学清单

1. **环境管理（第一天就配好）**
   - 安装 [uv](https://docs.astral.sh/uv/)（现代 Python 包管理，比 pip+venv 快且干净，2026 年事实标准）
   - `uv init` + `uv add` 管理依赖，对应你的 Maven 习惯
2. **语法核心**
   - 变量、字符串（f-string `f"hello {name}"`）、列表/字典/元组/集合
   - 函数、`*args` / `**kwargs`、lambda
   - 类和 `__init__`、`@property`、继承、`@staticmethod`
   - 异常处理、with 上下文管理器（对应 Java try-with-resources）
   - 推导式：`[x*2 for x in lst]`
3. **面向 AI 生态的必备特性**
   - **类型注解**：`def f(x: int) -> str:`（LangChain/pydantic 重度依赖，用于结构化输出）
   - **dataclass**：`@dataclass`（对应 Java record，定义 Agent 数据结构）
   - **pydantic**：`from pydantic import BaseModel`（AI 开发最常用，做输出校验）
   - **装饰器**：`@app.get(...)`、`@retry`——理解"函数包装函数"
4. **文件与 JSON**
   - `json.loads` / `json.dumps`（Agent 和 LLM 交互全是 JSON）
   - `pathlib.Path` 读写文件
   - `os.environ` 读环境变量（对应你后端 env 管理习惯）

## 1.3 第二周：异步与实战

- **asyncio 基础**（Agent 开发是 I/O 密集，异步是刚需）
  - `async def` / `await` / `asyncio.run()`
  - `asyncio.gather` 并发调用多个 LLM
  - 对比：Java 的 CompletableFuture / 虚拟线程 → Python asyncio
- **HTTP 客户端**：`httpx`（异步，AI SDK 底层用）
- **快速跑通两个练习**（一晚上一个）：
  - P0-1：写一个脚本，并发请求一个公开 API 的 10 个端点，汇总 JSON 结果
  - P0-2：用 pydantic 定义"用户信息"模型，解析一段 LLM 返回的 JSON，校验字段

## 1.4 推荐资源

- 官方教程：<https://docs.python.org/3/tutorial/>（只需过一遍语法，不用学 GUI/爬虫深度）
- uv 文档：<https://docs.astral.sh/uv/>
- Pydantic 文档：<https://docs.pydantic.dev/>
- 免费练习：<https://exercism.org/tracks/python>（每天 15 分钟刷题保持手感）

## 1.5 本阶段完成标准

- [ ] 能用 uv 建项目、加依赖、跑脚本
- [ ] 能读懂 LangChain/LangGraph 官方示例代码（不慌）
- [ ] 能写带类型注解 + pydantic 校验的函数
- [ ] 会用 asyncio 并发调用外部 API
