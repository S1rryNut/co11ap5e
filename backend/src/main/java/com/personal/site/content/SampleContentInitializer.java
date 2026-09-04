package com.personal.site.content;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SampleContentInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public SampleContentInitializer(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws JsonProcessingException {
        seedArticles();
        seedProjects();
        seedLearning();
        seedResume();
    }

    private void seedArticles() throws JsonProcessingException {
        if (count("articles") > 0) return;
        insertArticle("build-a-reliable-personal-site", "构建一个长期可维护的个人网站",
                "从内容边界、部署成本和安全策略出发，梳理个人网站真正需要的工程能力。",
                "工程实践", List.of("Nuxt", "Spring Boot", "部署"), 6, """
                ## 为什么重新做个人网站

                个人网站不是一次性的作品展示。它更像一块长期维护的数字空间，需要同时照顾内容、性能、安全与可迁移性。

                ## 当前的技术选择

                前端采用 Nuxt 提供服务端渲染，后端使用 Spring Boot 暴露独立 API，PostgreSQL 保存文章、项目和学习资料。Nginx 负责 TLS 与反向代理。

                ```text
                Browser -> Nginx -> Nuxt / Spring Boot -> PostgreSQL
                ```

                ## 保持简单

                第一阶段不引入 Redis、搜索集群和消息队列。定时任务保持串行，图片交给对象存储或 CDN。结构允许增长，但不预付暂时用不到的复杂度。
                """);
        insertArticle("notes-on-ai-learning", "我如何整理 AI 学习路径",
                "面对快速变化的信息流，用问题、概念和实践三层结构建立可以复习的知识体系。",
                "AI 学习", List.of("AI", "学习方法", "知识管理"), 5, """
                ## 从问题开始

                学习一个新主题时，我会先写下希望解决的问题，再补齐必要概念，最后通过一个可以运行的小项目检验理解。

                ## 区分热点与知识

                热点有时效性，知识需要上下文。本站的 AI 动态保留原始来源，而学习笔记负责把多个来源组织成稳定的认知结构。

                ## 可验证的输出

                每个阶段都应该有可以验证的结果：一段代码、一份评测记录、一篇解释清楚的文章，或者一个能被别人使用的工具。
                """);
        insertArticle("certificate-system-retrospective", "分布式证书存储项目复盘",
                "回顾签名验证、证书管理和节点同步中的设计选择，以及从原型走向公网服务需要补齐的安全边界。",
                "项目复盘", List.of("密码学", "Java", "分布式系统"), 8, """
                ## 项目目标

                这个项目探索了证书验证、哈希链和多节点签名协作。原型帮助我理解了可信数据交换中的身份与完整性问题。

                ## 真正困难的部分

                算法只是安全系统的一部分。密钥生命周期、会话管理、输入校验、权限边界和故障恢复同样决定系统是否可靠。

                ## 下一步

                新网站会保留这些工程经验，但使用更简单的部署拓扑，并以成熟协议和最小权限原则保护管理后台。
                """);
    }

    private void insertArticle(String slug, String title, String excerpt, String category,
                               List<String> tags, int minutes, String content) throws JsonProcessingException {
        jdbc.update("""
                INSERT INTO articles(slug,title,excerpt,category,tags,content,status,reading_minutes,published_at)
                VALUES (?,?,?,?,?,?,'PUBLISHED',?,CURRENT_TIMESTAMP)
                """, slug, title, excerpt, category, objectMapper.writeValueAsString(tags), content, minutes);
    }

    private void seedProjects() throws JsonProcessingException {
        insertProjectIfMissing("data-flow-rights-confirmation", "数据流转确权平台",
                "面向数据流转与签名确权场景，完成任务申请、审核、签名、确认与仲裁流程的前端开发及联调。",
                List.of("Vue 3", "Vite", "Pinia", "Element Plus", "Spring Boot", "Oracle", "JWT / ECDH"), true, 0);
        insertProjectIfMissing("distributed-certificate-storage", "分布式证书存储系统",
                "围绕证书、数字签名与多节点一致性构建的 Java 工程实践。",
                List.of("Java", "Spring Boot", "SM2/SM3", "PostgreSQL"), true, 1);
        insertProjectIfMissing("personal-content-ai-site", "个人内容与 AI 学习站",
                "前后端分离的个人数字空间，包含博客、简历、AI 信息聚合与内容后台。",
                List.of("Nuxt", "Vue", "Spring Boot", "PostgreSQL"), true, 2);
    }

    private void insertProjectIfMissing(String slug, String name, String summary, List<String> stack, boolean featured, int order)
            throws JsonProcessingException {
        jdbc.update("""
                INSERT INTO projects(slug,name,summary,stack,featured,sort_order)
                SELECT ?,?,?,?,?,? WHERE NOT EXISTS (SELECT 1 FROM projects WHERE slug = ?)
                """, slug, name, summary, objectMapper.writeValueAsString(stack), featured, order, slug);
    }

    private void seedLearning() {
        if (count("learning_resources") > 0) return;
        insertLearning("理解机器学习的基本工作流", "从数据、目标函数、训练到评估，建立完整而不过度抽象的认识。", "BEGINNER", "基础概念", 1);
        insertLearning("用 Python 调用并评估大模型 API", "掌握结构化输出、流式响应、错误重试、费用控制和基础评测。", "BEGINNER", "应用开发", 2);
        insertLearning("构建可追踪的 RAG 系统", "学习切分、检索、重排、引用和离线评测，避免只凭主观感受优化。", "INTERMEDIATE", "RAG", 3);
        insertLearning("设计可靠的 Agent 工作流", "围绕工具权限、状态管理、终止条件和可观测性设计可控代理。", "INTERMEDIATE", "Agent", 4);
        insertLearning("模型微调与系统化评测", "理解数据质量、训练策略、基准污染与线上反馈之间的关系。", "ADVANCED", "模型工程", 5);
    }

    private void insertLearning(String title, String description, String level, String topic, int order) {
        jdbc.update("INSERT INTO learning_resources(title,description,level,topic,sort_order) VALUES (?,?,?,?,?)",
                title, description, level, topic, order);
    }

    private void seedResume() throws JsonProcessingException {
        if (count("site_profiles") > 0) return;
        ContentModels.Resume resume = new ContentModels.Resume(
                "林默", "前端开发 / 网络安全方向", "示例城市", "hello@example.com",
                "具备 Java 基础与前端实战经验，熟悉 Vue、React 及 Web 安全开发。参与过政企级数据确权与存证项目，具备跨领域协作与项目交付能力。",
                List.of("HTML5 / CSS", "JavaScript", "Vue 3", "React", "Vite", "Java", "C++", "Python", "Web 安全", "密码安全"),
                List.of(
                        new ContentModels.Experience("示例科技公司", "测试员", "2025.08 - 2025.09",
                                "参与智能硬件应用实践与设备测试，记录问题并反馈改进。"),
                        new ContentModels.Experience("示例数据服务公司", "前端开发", "2024.12 - 2025.06",
                                "参与数据确权平台开发，完成签名生成、确权验证、在线身份校验和二次确认等交互及测试。"),
                        new ContentModels.Experience("示例区块链存证系统", "前端开发", "2025.11 - 2026.03",
                                "基于 Vue 3、Vite、Pinia 和 Element Plus 构建联盟链业务前端，实现缓存、并发去重、虚拟列表和性能观测。")
                ),
                List.of(new ContentModels.Education("示例大学", "电子信息工程 | 本科", "2022.09 - 2026.06")));
        jdbc.update("INSERT INTO site_profiles(profile_key,profile_json) VALUES ('resume',?)",
                objectMapper.writeValueAsString(resume));
    }

    private int count(String table) {
        Integer value = jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
        return value == null ? 0 : value;
    }
}
