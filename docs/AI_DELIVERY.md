# AI 全链路交付记录

本项目按“需求梳理 → 设计 → 编码 → 测试 → 打包”全流程使用 AI 辅助，并由本人逐项校验和修正。

| 阶段 | AI 辅助内容 | 人工校验与优化 | 证据 |
|---|---|---|---|
| 需求梳理 | 将极简题目拆为表达式、函数、角度、历史、错误、限制和验收项 | 明确保留清空接口、8 个单参数函数、1000 条历史、统一成功 `200 OK` | `docs/REQUIREMENTS.md` |
| 架构设计 | 生成单体分层、递归下降求值器、有界内存历史和统一异常映射方案 | 删除数据库、缓存、外部调用、脚本引擎及单实现抽象；确认线程安全边界 | `docs/superpowers/specs/2026-08-13-scientific-calculator-design.md` |
| 实施计划 | 将工作拆为 Maven 初始化、求值器、历史、HTTP、文档和验收步骤 | 翻译为中文并将冻结决策写入计划；保留测试先行顺序 | `docs/superpowers/plans/2026-08-13-scientific-calculator-implementation.md` |
| 编码 | 生成 Java 17/Spring Boot 类、解析器、服务、控制器和异常处理器初稿 | 运行编译和测试，修正 `angleUnit` 必须严格匹配大写枚举值的问题；确认失败计算不写历史 | `src/main/java`、`src/test/java` |
| 测试 | 生成 JUnit 5、MockMvc 和 Spring 上下文测试矩阵 | 先观察缺失类导致的 RED，再补齐实现；增加历史、415/422/500、清空和失败原子性验收 | `./mvnw clean verify` |
| 打包 | 生成 Maven Wrapper、Spring Boot runnable jar 和启动命令 | 使用 JDK 17、代理和真实进程执行 curl；记录 SHA-256 与 HTTP 响应 | `docs/TEST_EVIDENCE.md` |

人工优化重点：边界输入必须在 HTTP 层拒绝；求值器只走显式白名单；历史写入必须发生在求值成功之后；错误响应不得暴露堆栈或内部类名。
