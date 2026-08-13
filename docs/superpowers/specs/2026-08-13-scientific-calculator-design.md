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

成功返回 `201 Created`：

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
- `DELETE /api/v1/calculations`：清空全部内存历史，返回 `204 No Content`。

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

- 默认最多保留 1000 条；
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

JUnit 5 单元测试覆盖：

- 运算优先级、括号、科学计数法；
- 幂的右结合、一元负号；
- 常量、科学函数、RADIAN/DEGREE；
- 非法字符、未知标识符、尾随输入；
- 除零、定义域、溢出、长度和深度限制；
- 历史倒序、容量、查询、清空和并发 ID 唯一性。

Spring MVC 契约测试覆盖成功计算、错误响应和历史接口。最终执行：

```bash
mvn clean verify
java -jar target/scientific-calculator-*.jar
curl http://localhost:8080/health
curl -X POST http://localhost:8080/api/v1/calculations \
  -H 'Content-Type: application/json' \
  -d '{"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE"}'
```

## 8. AI 全链路交付证据

`docs/AI_DELIVERY.md` 按需求、设计、编码、测试、打包五个阶段记录：

- AI 辅助的提示与产出摘要；
- 候选人的校验方法；
- 候选人的取舍、修正和优化；
- 对应源码、测试和命令输出证据；
- 需求—实现—测试追踪表。

文档不虚构逐行代码归属，以可复核的决策和验证证据区分 AI 产出与个人工作。
