- v24 (2026-09-03): 学习页 slug 含义化 station-XX→part-N-xxx + 旧slug 301重定向；删除站18「30系列手把手教程文件导航」表格；站1扩写为超详细小白版(4.5K→6.2K字)。部署成功，健康检查 /learn 与 /learn/part-1-llm-basics 通过。
- 站1底层扩写 (2026-09-03): 从 6.2K→11.3K 字。新增 Section5 Transformer心脏(Attention/Q/K/V/多头/并行) + Section6 预训练/微调/推理/大模型四脾气；Section1-4 加入底层视角(Token/向量/神经网络/预测下一个字/概率采样/温度/参数量/API底层/token计费)。直接改库生效，无需重新构建前端。

- v25 (2026-09-03): 站2《大模型原理》拆分为 Part 2·入门实操 / Part 3·面试深入 / Part 4·进阶可选 三站；原站3~18 顺延为 Part 5~20（共20站）。同步更新：内容内 Part N 引用 +2、清理 station-XX md 文件名引用、清理 Part 2A/2B 残留；前端 LEGACY_SLUGS 更新（station-XX→新 part + 旧含义 part 二次跳转）。部署成功，旧 slug 301 到新 slug 验证通过，学习路线首页显示 Part 1~20。

- v25 补充 (2026-09-03): Part 3/Part 4 内部 Section 重编号为从 1 开始连续（Part 3: 1~12, Part 4: 1~8），跨站 Section 引用改为 'Part N · Section M' 明确指向。已重新导出 learning_20_stations.sql 备份。

- v25 内容清理 (2026-09-03): 删除全部 20 站 Subsection 标题中的纯时长标注（（X 分钟/小时/天）共 413 处），保留正文中说明性提示（如'第一次下载模型可能 10 分钟'）。无需重新构建前端，改库即生效。

## [2026-09-03] 修复 Part 5-8 学习站 content 污染

- **问题**：id=3/4/5/6（Part 5·Prompt / Part 6·大模型API / Part 7·RAG / Part 8·RAG优化）的 content 字段被 psql 对齐输出污染：开头混入 `content` 列头行 + `---` 分隔线，且每行带对齐填充空格，导致学习页渲染出裸 `<code>content</code>` 黑色块。
- **根因**：早期批量同步 18 站教程时，误将 psql 终端输出的对齐格式（含列头）写入了数据库。
- **修复**：从本地 `learning_20_stations.sql`（16:49 备份，含污染原始内容）提取 id3-6 content，本地脚本清理（跳过开头 content/--- 行、去每行 1 个对齐前导空格、去全部尾随空格、保留代码块相对缩进），生成 `learn_clean_3456.json` 上传服务器写库。
- **结果**：4 站 content 长度 44412/14260/55356/44152，开头干净 `# Part N`，线上 4 页验证无 content 块、正文完整渲染。全表 20 站检查均无污染。
- **备份**：`/tmp/learn_pollute_backup.csv`（污染原样）、本地 `learn_clean_3456.json`（清理后）、`learning_20_stations.sql`（最新）。

## [2026-09-03] 清理 Part 5-8 行尾续行标记「+」

- **补充问题**：id=3/4/5/6 的 content 在首次污染修复后，每行末尾仍残留 psql 终端输出的续行标记「空格+加号」以及结尾的 `(1 row)` 行数统计。
- **修复**：本地对当前库内容（learn_clean_3456.json）做精确清理：`re.sub(r'[ \t]+\+$', '', ln)` 只删「空格/tab+行尾加号」的续行标记（保留代码前导缩进），并移除 `(N row)` 残留行、清理首尾空行。生成 `learn_clean_3456_final2.json` 上传写库。
- **结果**：4 站 content 真实正文长度 8335/2968/11184/7039，开头干净 `# Part N`，无行尾 `+`、无 `(N row)`。线上 4 页验证正文正常，代码缩进保留（`api_key=` 4 空格缩进正确）。
- **备份**：`learn_clean_3456.json`（清理前，带续行+）、`learn_clean_3456_final2.json`（最终干净版）。
