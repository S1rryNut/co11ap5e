const MarkdownIt = require("C:/Users/1/Desktop/design/frontend/node_modules/markdown-it");
const md = new MarkdownIt({ html: false, linkify: true, typographer: true });
const escapeHtml = (s) => s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
md.renderer.rules.fence = (tokens, idx, options, env, self) => {
  const token = tokens[idx];
  const info = token.info ? String(token.info).trim().split(/\s+/g)[0] : "";
  const lang = info || "";
  const escaped = escapeHtml(String(token.content));
  const lines = escaped.split("\n");
  if (lines.length && lines[lines.length - 1] === "") lines.pop();
  const body = lines.map((l) => `<span class="code-line">${l}</span>`).join("");
  const langLabel = lang ? `<span class="code-lang">${lang}</span>` : '<span class="code-lang is-empty"></span>';
  return `<div class="code-block" data-lang="${escapeHtml(lang)}"><div class="code-header">${langLabel}<button type="button" class="code-copy" aria-label="复制代码">复制</button></div><pre><code class="language-${escapeHtml(lang)}">${body}</code></pre></div>`;
};
const src = "## 示例\n\n```js\nconst a = 1 < 2 && 'x>y';\nconsole.log(a);\n\n// 空行测试\n```\n\n普通行内 `code` 不受影响。";
console.log(md.render(src));
