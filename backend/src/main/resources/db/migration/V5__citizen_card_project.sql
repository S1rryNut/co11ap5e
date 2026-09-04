INSERT INTO projects(name, summary, stack, featured, sort_order)
SELECT
    '宁波市市民卡公司签名确权平台',
    '面向数据流转与签名确权场景，完成任务申请、审核、签名、确认与仲裁流程的前端开发及联调。',
    '["Vue 3","Vite","Pinia","Element Plus","Spring Boot","Oracle","JWT / ECDH"]',
    TRUE,
    0
WHERE NOT EXISTS (SELECT 1 FROM projects WHERE name = '宁波市市民卡公司签名确权平台');
