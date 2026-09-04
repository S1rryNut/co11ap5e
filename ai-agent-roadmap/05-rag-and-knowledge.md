# 05 · RAG 与知识增强

> 时长：2 周。RAG（检索增强生成）是 Agent 落地最常用的能力——让模型"看到"你的私有/实时数据，解决幻觉和知识过期。

---

## 5.1 RAG 是什么

**RAG = 先检索相关资料，再让 LLM 基于资料回答。** 解决 LLM 的两个硬伤：训练知识截止、会幻觉。

```
用户问题
   │
   ▼
[Embedding 向量化] ──▶ [向量数据库检索 Top-K] ──▶ [拼进 Prompt] ──▶ [LLM 回答]
```

## 5.2 全流程五步（每步都要会）

1. **文档切分（Chunking）**：把文档切成有语义的块（按标题/段落/固定长度），块大小影响检索质量。
2. **向量化（Embedding）**：把文本转成向量。选型：BGE / 通义千问 embedding / OpenAI embedding（国内可选便宜模型）。
3. **入库**：向量 + 原文 + 元数据存进向量库。
4. **检索**：把用户问题向量化，相似度检索 Top-K，可加元数据过滤（如按分类/时间）。
5. **生成**：检索结果作为上下文拼进 Prompt，要求"只依据给定材料回答，不知道就说不知道"。

## 5.3 向量数据库选型

| 方案 | 特点 | 适合 |
|---|---|---|
| **pgvector**（PostgreSQL 插件） | 复用你已有的 PG，零新组件 | **首选**——你熟 PG，迁移成本最低 |
| Chroma | 轻量，本地文件 | 原型、学习 |
| Milvus | 分布式，大规模 | 生产大规模 |
| Qdrant | 高性能，Rust | 生产中等规模 |
| 云服务（阿里/腾讯向量库） | 托管 | 不想运维时 |

> 你有 PostgreSQL + 运维基础，直接上 **pgvector** 是最优解：一个库搞定业务 + 向量。

## 5.4 RAG 进阶（简历亮点）

- **HyDE**：先用 LLM 生成"假设答案"再检索，提高召回。
- **Rerank（重排）**：粗检索 Top-50 → 重排模型精排 Top-5，显著提质量。
- **混合检索**：向量 + 关键词（BM25）结合，覆盖不同查询。
- **GraphRAG**：构建知识图谱做多跳推理（微软方案，适合"关系型"问答）。
- **引用溯源**：回答附来源 chunk，用户可点开验证（你的网站文章页已有类似思路）。
- **评估**：用 RAGAS 或人工打分评估检索质量与回答质量（见第 6 章）。

## 5.5 动手产出

- **P4-1：个人知识库问答**（5~7 天）——把你的网站文章 / 学习笔记 / 博客全部喂进去
  - 建 PG + pgvector 表，写 ingestion 脚本（切分 + 向量化 + 入库）
  - 写检索 + 生成接口
  - 效果对比：加 HyDE / Rerank 前后回答质量差异
- **P4-2（进阶）**：RAG Agent 化——Agent 判断"要不要查库""查哪类资料"，再决定是否调用检索工具（这就是 RAG + Agent 的融合，面试高频题）。

## 5.6 推荐资源

- pgvector 官方文档：<https://github.com/pgvector/pgvector>
- 吴恩达短课 "Building and Evaluating Advanced RAG"：<https://www.deeplearning.ai/short-courses/>
- 微软 GraphRAG：<https://microsoft.github.io/graphrag/>
- RAGAS 评估库：<https://docs.ragas.io/>

## 5.7 本阶段完成标准

- [ ] 能跑通"文档入库 → 检索 → 生成回答"完整链路
- [ ] 能说出 chunking、embedding、retrieval、rerank 各自的作用
- [ ] 会用 pgvector 存向量并做相似度检索
- [ ] 能对比并解释"加 Rerank / HyDE 前后"的差异
