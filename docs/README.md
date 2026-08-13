# 项目文档与 AI 全链路交付记录

本项目按“需求梳理 → 设计 → 编码 → 测试 → 打包”全流程使用 AI 辅助，并由本人逐项校验和修正。

## 文档导航

- [需求说明](requirements/REQUIREMENTS.md)
- [设计规格](design/scientific-calculator-design.md)
- [实施计划](plans/scientific-calculator-implementation.md)
- [curl 测试案例](testing/CURL_TEST_CASES.md)
- [测试与验收证据](testing/TEST_EVIDENCE.md)

## AI 全链路交付记录

| 阶段 | AI 辅助内容 | 人工校验与优化 | 证据 |
|---|---|---|---|
| 需求梳理 | 将极简题目拆为表达式、函数、角度、历史、错误、限制和验收项 | 明确保留清空接口、8 个单参数函数、1000 条历史、统一成功 `200 OK` | [需求说明](requirements/REQUIREMENTS.md) |
| 架构设计 | 生成单体分层、递归下降求值器、有界内存历史和统一异常映射方案 | 删除数据库、缓存、外部调用、脚本引擎及单实现抽象；确认线程安全边界 | [设计规格](design/scientific-calculator-design.md) |
| 实施计划 | 将工作拆为 Maven 初始化、求值器、历史、HTTP、文档和验收步骤 | 翻译为中文并将冻结决策写入计划；保留测试先行顺序 | [实施计划](plans/scientific-calculator-implementation.md) |
| 编码 | 生成 Java 17/Spring Boot 类、解析器、服务、控制器和异常处理器初稿 | 运行编译和测试，修正 `angleUnit` 必须严格匹配大写枚举值的问题；确认失败计算不写历史 | `src/main/java`、`src/test/java` |
| 测试 | 生成 JUnit 5、MockMvc 和 Spring 上下文测试矩阵 | 先观察缺失类导致的 RED，再补齐实现；增加历史、415/422/500、清空和失败原子性验收 | `./mvnw clean verify` |
| 打包 | 生成 Maven Wrapper、Spring Boot runnable jar 和启动命令 | 使用 JDK 17、代理和真实进程执行 curl；记录 SHA-256 与 HTTP 响应 | [测试证据](testing/TEST_EVIDENCE.md) |

人工优化重点：边界输入必须在 HTTP 层拒绝；求值器只走显式白名单；历史写入必须发生在求值成功之后；错误响应不得暴露堆栈或内部类名。

## 最终 curl 全量验收记录

2026-08-13 使用 Java 17 启动可执行 JAR：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
java -jar target/scientific-calculator-1.0.0.jar --server.port=18080
```

基于 [curl 测试案例](testing/CURL_TEST_CASES.md) 按顺序执行了 C01–C42，并补充执行文档中新增的缺少 `Content-Type`、尾随 token、负数/超大 ID、小数 `limit` 边界，以及失败不写历史、统一错误响应和最终收尾请求。

执行结果：

- HTTP 全量断言：`66` 条通过，`0` 条失败；
- 关键计算值复核：`13` 条通过，`0` 条失败；
- 健康检查、成功计算、历史查询、清空接口均返回预期状态码；
- 非法请求返回 `400/415`，领域错误返回 `422`，错误体包含 `code`、`message`、`path`、`timestamp`；
- 失败计算不会写入历史；最终执行清空后历史为 `[]`。

角度制组合表达式 `sin(90)+cos(180)+tan(45)` 实际返回 `0.9999999999999999`。该结果符合项目对 Java `double`/IEEE-754 浮点语义的约定，验收使用 `1e-9` 容差，不按字符串直接比较 `1.0`。

另外完成一次进程重启验收：重启前写入 `6*7`，重启后查询历史返回 `200 []`，符合“历史仅存于当前进程内存”的设计。

本次验收使用的服务进程已停止，未保留后台 Java 进程。
