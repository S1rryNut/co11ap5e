UPDATE site_profiles
SET profile_json = '
{
  "displayName": "张三",
  "headline": "全栈开发 / AI 应用方向",
  "location": "江苏苏州",
  "email": "admin@example.com",
  "summary": "具备 Java 基础与前端实战经验，熟悉 Vue、React 及 Web 安全开发。参与过市级重点项目和多项网络安全竞赛，具备跨领域协作与项目交付能力。",
  "skills": ["HTML5 / CSS", "JavaScript", "Vue 3", "React", "Vite", "Java", "C++", "Python", "Web 安全", "密码安全"],
  "experiences": [
    {"organization":"示例机器人科技有限公司","role":"测试员","period":"2025.08 - 2025.09","description":"参与智能小车应用实践与车床操控测试，记录问题并反馈改进。"},
    {"organization":"示例市民卡公司","role":"前端开发","period":"2024.12 - 2025.06","description":"参与签名确权平台开发，完成签名生成、确权验证、在线身份校验和二次确认等交互及测试。"},
    {"organization":"业务数据授权去中心化存证系统","role":"前端开发","period":"2025.11 - 2026.03","description":"基于 Vue 3、Vite、Pinia 和 Element Plus 构建联盟链业务前端，实现缓存、并发去重、虚拟列表和性能观测。"}
  ],
  "education": [
    {"institution":"示例理工大学","major":"电子信息工程 | 本科","period":"2022.09 - 2026.06"}
  ]
}
',
updated_at = CURRENT_TIMESTAMP
WHERE profile_key = 'resume';
