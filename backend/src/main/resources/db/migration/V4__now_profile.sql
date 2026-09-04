INSERT INTO site_profiles(profile_key, profile_json)
SELECT 'now', '
{
  "headline": "正在把学习、构建与生活记录成可持续更新的个人站点。",
  "updatedAt": "2026-08-29",
  "building": ["完善 Co11ap5e 个人站的内容管理与部署流程", "整理分布式证书存储项目的设计与复盘"],
  "learning": ["追踪 AI 工程、RAG 与 Agent 工作流", "持续补齐 Web 安全与密码安全实践"],
  "playing": [],
  "reading": []
}'
WHERE NOT EXISTS (SELECT 1 FROM site_profiles WHERE profile_key = 'now');
