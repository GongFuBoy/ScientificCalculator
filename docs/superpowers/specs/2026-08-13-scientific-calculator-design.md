# 科学计算器后端服务设计

## 1. 目标与范围

交付一个基于 Java 17、Spring Boot 3、Maven 和 JUnit 5 的 HTTP 科学计算器服务。服务只使用进程内存，不访问数据库、缓存中间件、AI 模型、第三方数学 API 或其他外部服务，并可打包为 `java -jar` 直接运行的可执行 JAR。

首版包含：

- 科学表达式计算；
- 弧度制与角度制切换；
- 有界、线程安全的内存计算历史；
- 历史查询、单条查询和清空；
- 健康检查、统一错误响应；
- 单元测试、HTTP 契约测试和交付过程文档。

明确不包含：用户体系、鉴权、持久化、分布式部署协调、动态函数插件、前端页面和外部接口调用。

## 2. HTTP API

### 2.1 执行计算

`POST /api/v1/calculations`

请求：

```json
{
  "expression": "sin(pi / 2) + sqrt(16)",
  "angleUnit": "RADIAN"
}
```

`angleUnit` 可取 `RADIAN` 或 `DEGREE`，省略时默认为 `RADIAN`。

成功返回 `200 OK`：

```json
{
  "id": 1,
  "expression": "sin(pi / 2) + sqrt(16)",
  "angleUnit": "RADIAN",
  "result": 5.0,
  "createdAt": "2026-08-13T10:00:00Z"
}
```

### 2.2 查询历史

- `GET /api/v1/calculations?limit=20`：按创建时间倒序返回，`limit` 范围为 1～100；
- `GET /api/v1/calculations/{id}`：按 ID 查询单条记录；
- `DELETE /api/v1/calculations`：清空全部内存历史，返回 `200 OK` 和 `{"cleared":true}`。

### 2.3 健康检查

`GET /health` 返回：

```json
{"status":"UP"}
```

## 3. 表达式能力与语义

白名单语法：

```text
expression     := additive
additive       := multiplicative (("+" | "-") multiplicative)*
multiplicative := unary (("*" | "/" | "%") unary)*
unary          := ("+" | "-") unary | power
power          := primary ("^" unary)?
primary        := NUMBER
                | CONSTANT
                | FUNCTION "(" expression ")"
                | "(" expression ")"
```

支持：

- 十进制整数、小数和科学计数法；
- `+ - * / % ^`、括号和一元正负号；
- 常量 `pi`、`e`；
- 单参数函数 `sin`、`cos`、`tan`、`sqrt`、`abs`、`ln`、`log`、`exp`；
- `^` 右结合，`-2^2` 等于 `-4`，`2^-2` 等于 `0.25`；
- `log` 表示以 10 为底，`ln` 表示自然对数；
- 三角函数输入依据请求的角度单位解释。

不支持隐式乘法、变量、赋值、多语句、成员访问、脚本或未知函数。计算使用 Java `double` 与 `Math`，不承诺金融级十进制定点精度；任一中间结果或最终结果为 `NaN`/无穷大时拒绝请求。

## 4. 架构与数据流

保持单体和少量清晰边界：

- `CalculationController`：HTTP 映射与请求参数约束；
- `CalculatorService`：编排表达式计算和历史写入；
- `ExpressionEvaluator`：纯 Java 白名单递归下降解析与求值；
- `CalculationHistory`：线程安全、有界的内存记录；
- `ApiExceptionHandler`：统一 JSON 错误映射。

数据流：Controller 接收请求 → Service 调用 Evaluator → 计算成功后写入 History → 返回记录。解析失败或领域错误不会写入历史。

不为单一实现创建接口、工厂或 Repository 抽象；当出现第二种实现或真实替换需求时再引入。

## 5. 内存与并发

历史记录使用同步保护的有界双端队列和单调递增 ID：

- 固定最多保留 1000 条；
- 新记录位于队首，超限时删除最旧记录；
- 查询返回不可变快照；
- 清空记录不重置 ID，避免同一进程生命周期内 ID 重用；
- 服务重启后历史丢失，这是题目“仅内存实现”的预期行为。

该实现以正确性和简单性优先。只有压测证明单锁成为瓶颈时，才考虑更复杂的并发结构。

## 6. 防滥用与错误处理

- 表达式必须非空，最大 1000 字符；
- 最多 500 个词法单元，最大解析嵌套深度 100；
- `limit` 只能为 1～100；
- 除零、模零、函数定义域错误和非有限结果返回 `422 Unprocessable Entity`；
- JSON/参数/表达式语法错误返回 `400 Bad Request`；
- 历史不存在返回 `404 Not Found`；
- 错误体仅包含稳定错误码、消息、时间和请求路径，不返回堆栈。

错误示例：

```json
{
  "code": "DIVISION_BY_ZERO",
  "message": "Division by zero",
  "path": "/api/v1/calculations",
  "timestamp": "2026-08-13T10:00:00Z"
}
```

## 7. 测试与验收

### 7.1 测试策略

测试按“纯 Java 单元测试 → Spring MVC 契约测试 → 可执行 JAR 冒烟测试”分层，优先在最小范围内定位失败：

| 层级 | 测试对象 | 工具 | 目标 |
|---|---|---|---|
| 单元测试 | 表达式解析、数值计算、内存历史 | JUnit 5 | 覆盖核心算法、边界和并发不变量，不启动 Spring |
| Web 契约测试 | Controller、参数绑定、状态码、JSON | MockMvc | 验证公开 HTTP 契约和统一错误格式 |
| 应用集成测试 | Spring 上下文与核心 Bean 协作 | `@SpringBootTest` | 确认应用可装配，真实调用链可工作 |
| 产物验收 | runnable JAR | Maven、`java -jar`、`curl` | 确认脱离 IDE 后可独立启动和提供服务 |

不引入 Testcontainers、数据库、外部服务模拟器或额外压测框架，因为系统没有对应依赖边界；并发正确性使用 JDK `ExecutorService` 完成可重复验证。

### 7.2 表达式计算测试矩阵

所有浮点结果使用容差断言，不直接比较字符串表现形式。

| 类别 | 输入示例 | 预期结果/行为 |
|---|---|---|
| 四则与优先级 | `1 + 2 * 3` | `7` |
| 括号 | `(1 + 2) * 3` | `9` |
| 小数与科学计数法 | `.5 + 1.5e2` | `150.5` |
| 幂右结合 | `2^3^2` | `512` |
| 一元负号优先级 | `-2^2` | `-4` |
| 负指数 | `2^-2` | `0.25` |
| 常量与组合函数 | `sqrt(16) + ln(e)` | `5` |
| 弧度制 | `sin(pi / 2)`，`RADIAN` | `1` |
| 角度制 | `sin(90)`，`DEGREE` | `1` |
| 绝对值与常用对数 | `abs(-3) + log(100)` | `5` |
| 空白输入 | `"   "` | `400 INVALID_EXPRESSION` |
| 不完整表达式 | `1 +` | `400 EXPRESSION_SYNTAX_ERROR` |
| 非法字符 | `1; system()` | `400 EXPRESSION_SYNTAX_ERROR` |
| 未知标识符/函数 | `foo(1)`、`x + 1` | `400 EXPRESSION_SYNTAX_ERROR` |
| 隐式乘法 | `2pi`、`2(3+4)` | `400 EXPRESSION_SYNTAX_ERROR` |
| 除零/模零 | `1/0`、`1%0` | `422 DIVISION_BY_ZERO` |
| 函数定义域 | `sqrt(-1)`、`ln(0)` | `422 DOMAIN_ERROR` |
| 数值溢出 | `exp(1000)` | `422 NON_FINITE_RESULT` |
| 尾随输入 | `1 + 2 abc` | `400 EXPRESSION_SYNTAX_ERROR` |
| 资源限制 | 超长表达式、Token 超限、嵌套超限 | `400 EXPRESSION_LIMIT_EXCEEDED` |

额外断言所有失败计算均不写入历史，避免出现“请求失败但留下记录”的部分成功状态。

### 7.3 内存历史与并发测试

历史测试验证以下不变量：

- 新记录按创建顺序倒序返回；
- `limit` 精确限制结果数，且只能取 1～100；
- 达到容量后只淘汰最旧记录，历史大小永不超过配置容量；
- 按 ID 查询存在记录成功，不存在记录返回 `404 HISTORY_NOT_FOUND`；
- 清空后查询为空，但新记录 ID 不与已清空记录重复；
- 多线程同时写入时无异常、无丢失的已接受请求、ID 全部唯一，最终容量仍受上限约束；
- 查询返回快照，调用方不能修改内部历史状态。

并发测试使用固定线程池、统一开始信号和有超时的完成等待，不使用 `sleep` 猜测执行时序，避免产生偶发失败的测试。

### 7.4 HTTP 契约测试

MockMvc 至少验证：

| 场景 | 关键断言 |
|---|---|
| 成功计算 | `200`、`Content-Type: application/json`、返回 ID/表达式/角度单位/结果/时间，随后可从历史查到 |
| 省略角度单位 | 使用 `RADIAN` 默认值 |
| 非法枚举或空表达式 | `400`，错误体包含稳定的 `code/message/path/timestamp` |
| 计算领域错误 | `422`，不暴露异常类名或堆栈 |
| 历史列表 | `200`、倒序、`limit` 生效 |
| 单条历史 | 存在时 `200`，不存在时 `404` |
| 清空历史 | `200`、返回 `{"cleared":true}`，后续列表为空 |
| 健康检查 | `200` 与 `{"status":"UP"}` |
| 非法 JSON/错误 Content-Type | 返回 JSON 格式的 `400`/`415`，不返回默认 HTML 错误页 |

### 7.5 构建与独立运行验收

构建环境必须实际使用 Java 17。执行：

```bash
java -version
mvn -version
mvn clean verify
```

通过标准：

- Maven 进程使用 Java 17；
- 编译、单元测试、契约测试和集成测试全部成功；
- `target/` 生成由 Spring Boot Maven Plugin 重打包的可执行 JAR；
- 测试报告中失败数和错误数均为 0。

随后在独立进程启动产物：

```bash
java -jar target/scientific-calculator-*.jar
```

另一个终端执行冒烟验收：

```bash
curl -i http://localhost:8080/health

curl -i -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE"}'

curl -i 'http://localhost:8080/api/v1/calculations?limit=10'

curl -i -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1/0"}'

curl -i -X DELETE http://localhost:8080/api/v1/calculations
```

通过标准：健康检查、成功计算和清空历史均为 `200`；计算结果为 `5.0`；历史查询包含该记录；除零为 `422` 且错误体符合契约；清空响应为 `{"cleared":true}`。进程启动期间不要求数据库、缓存、环境变量密钥或网络连接。

### 7.6 验收证据与退出标准

交付时保留以下可复核证据：

- `mvn clean verify` 的完整结果摘要和 Surefire 测试报告；
- runnable JAR 文件名、大小和 SHA-256；
- 独立启动日志中的端口与启动成功信息；
- 上述 HTTP 冒烟请求的状态码和响应体；
- 需求—源码—测试用例追踪表；
- 已知约束：`double` 精度、历史重启丢失、单实例内存范围。

只有在全部自动化测试通过、JAR 可独立启动、HTTP 冒烟用例通过且交付文档中的证据可复现时，才视为验收通过。

## 8. AI 全链路交付证据

`docs/AI_DELIVERY.md` 按需求、设计、编码、测试、打包五个阶段记录：

- AI 辅助的提示与产出摘要；
- 候选人的校验方法；
- 候选人的取舍、修正和优化；
- 对应源码、测试和命令输出证据；
- 需求—实现—测试追踪表。

文档不虚构逐行代码归属，以可复核的决策和验证证据区分 AI 产出与个人工作。
