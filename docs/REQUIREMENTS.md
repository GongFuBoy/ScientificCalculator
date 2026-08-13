# 科学计算器后端服务需求说明书

## 1. 文档目的

本文档将面试题中的极简原始需求拆解为可设计、可开发、可测试、可验收的需求基线。后续架构设计、编码、测试和 AI 全链路交付文档均以本文档为依据。

## 2. 项目目标

实现一个可独立运行的 HTTP 科学计算器后端服务。调用方提交数学表达式，服务完成安全解析、计算并返回结果，同时提供有界的内存计算历史。

系统必须满足：

- Java 17；
- Spring Boot 3；
- Maven；
- JUnit 5；
- 全部数据仅存储于进程内存；
- 不使用 MySQL、Redis 等数据库或缓存中间件；
- 不调用 AI 模型、第三方数学 API 或任何外部接口；
- 最终生成可执行 runnable JAR；
- 执行 `java -jar` 即可启动，不依赖额外中间件；
- 仅提供 HTTP 后端接口，不开发前端页面。

## 3. 用户与使用场景

系统只有一种主要使用者：HTTP API 调用方。

主要场景：

1. 提交科学表达式并获取计算结果；
2. 指定三角函数使用弧度制或角度制；
3. 查询最近的计算记录；
4. 根据 ID 查询单条历史记录；
5. 清空当前进程中的历史记录；
6. 检查服务是否正常运行。

不在本次范围内：

- 用户注册、登录和权限控制；
- 多租户或个人历史隔离；
- 数据持久化和跨实例数据共享；
- 文件导入导出；
- 异步计算任务；
- WebSocket；
- 前端页面；
- 动态函数插件。

## 4. 功能需求

### 4.1 表达式计算

服务接收一个字符串表达式并返回计算结果。

#### 4.1.1 基础运算

- 加法：`+`；
- 减法：`-`；
- 乘法：`*`；
- 除法：`/`；
- 取模：`%`；
- 幂运算：`^`；
- 括号：`()`；
- 一元正号和负号：`+3`、`-2`。

#### 4.1.2 数字格式

支持：

```text
1
1.5
.5
1e3
1.5E-2
```

拒绝非十进制数值或 Java 特殊数值字面量：

```text
NaN
Infinity
0x10
1f
```

#### 4.1.3 常量

支持小写常量：

- `pi`；
- `e`。

首版不兼容其他大小写形式，避免同一标识符存在多种写法。

#### 4.1.4 科学函数

首版建议支持以下单参数函数：

- `sin`；
- `cos`；
- `tan`；
- `sqrt`；
- `abs`；
- `ln`；
- `log`；
- `exp`。

函数参数可以是完整表达式：

```text
sqrt(16 + 9)
sin(pi / 2)
```

首版不支持多参数函数，例如 `pow(2, 3)`、`max(1, 2)`。幂运算已经由 `^` 提供。

### 4.2 运算优先级与结合性

必须明确并通过测试验证以下规则：

1. 括号和函数调用；
2. 幂运算；
3. 一元正负号；
4. 乘法、除法和取模；
5. 加法和减法。

幂运算右结合：

```text
2^3^2 = 512
```

一元负号语义：

```text
-2^2 = -4
2^-2 = 0.25
```

不支持隐式乘法：

```text
2pi
2(3 + 4)
```

调用方必须显式写成：

```text
2 * pi
2 * (3 + 4)
```

### 4.3 角度单位

请求可以指定：

- `RADIAN`：弧度制；
- `DEGREE`：角度制。

省略时默认使用 `RADIAN`。

示例：

```text
sin(pi / 2) + 4  // RADIAN，结果为 5
sin(90) + 4      // DEGREE，结果为 5
```

角度单位只影响三角函数输入，不影响普通运算和其他函数。

### 4.4 计算历史

计算成功后写入当前进程的内存历史；计算失败不得写入历史。

支持：

- 查询最近历史；
- 按 ID 查询单条历史；
- 清空全部历史；
- 容量达到上限时淘汰最旧记录。

## 5. 表达式语法

使用固定白名单词法分析和递归下降解析，不使用 `ScriptEngine`、`eval`、SpEL、反射或动态代码执行。

```text
expression     := additive

additive       := multiplicative
                  (("+" | "-") multiplicative)*

multiplicative := unary
                  (("*" | "/" | "%") unary)*

unary          := ("+" | "-") unary
                  | power

power          := primary
                  ("^" unary)?

primary        := NUMBER
                  | CONSTANT
                  | FUNCTION "(" expression ")"
                  | "(" expression ")"
```

该语法确保：

- 运算符优先级明确；
- 幂运算右结合；
- 一元负号行为明确；
- 非白名单字符和标识符无法执行；
- 不存在脚本注入执行路径；
- 核心计算不依赖外部表达式引擎。

## 6. 数值语义

计算使用 Java `double` 和 `java.lang.Math`。

规则：

- 遵循 IEEE-754 浮点语义；
- 自动化测试使用误差容差断言；
- 不承诺金融级十进制定点精度；
- `-0.0` 可以规整为 `0.0`；
- 任意中间结果或最终结果为 `NaN`、正无穷或负无穷时拒绝请求；
- `log` 表示以 10 为底的对数；
- `ln` 表示自然对数。

以下情况返回明确错误，不向调用方返回 `NaN` 或 `Infinity`：

```text
1 / 0
1 % 0
sqrt(-1)
ln(0)
ln(-1)
exp(1000)
```

`tan(pi / 2)` 按 Java `Math` 的浮点近似计算，可能得到非常大的有限值。首版不加入基于 epsilon 的奇点推断。

## 7. HTTP API 需求

### 7.1 执行计算

```http
POST /api/v1/calculations
Content-Type: application/json
```

请求：

```json
{
  "expression": "sin(pi / 2) + sqrt(16)",
  "angleUnit": "RADIAN"
}
```

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `expression` | string | 是 | 数学表达式 |
| `angleUnit` | enum | 否 | `RADIAN` 或 `DEGREE`，默认 `RADIAN` |

成功响应：

```json
{
  "id": 1,
  "expression": "sin(pi / 2) + sqrt(16)",
  "angleUnit": "RADIAN",
  "result": 5.0,
  "createdAt": "2026-08-13T10:00:00Z"
}
```

成功状态码在第 16 节中待最终确认。

### 7.2 查询历史列表

```http
GET /api/v1/calculations?limit=20
```

要求：

- 按创建时间倒序；
- 默认返回 20 条；
- `limit` 最小为 1，最大为 100；
- 只查询当前进程中的内存数据。

响应示例：

```json
[
  {
    "id": 2,
    "expression": "1 + 2",
    "angleUnit": "RADIAN",
    "result": 3.0,
    "createdAt": "2026-08-13T10:01:00Z"
  }
]
```

### 7.3 查询单条历史

```http
GET /api/v1/calculations/{id}
```

- 记录存在时返回对应记录；
- 记录不存在时返回 `404 Not Found`。

### 7.4 清空历史

```http
DELETE /api/v1/calculations
```

成功建议返回：

```http
204 No Content
```

清空后：

- 历史列表为空；
- 新增记录继续使用递增 ID；
- 不复用已经使用过的 ID。

### 7.5 健康检查

```http
GET /health
```

响应：

```json
{
  "status": "UP"
}
```

## 8. 内存历史需求

历史记录用于体现题目要求的内存存储及并发设计。

要求：

- 默认最大容量建议为 1000 条；
- 只保留最新记录；
- 超过容量时淘汰最旧记录；
- 写入、查询和清空必须线程安全；
- 查询返回快照，不直接暴露内部可变集合；
- 服务重启后历史丢失；
- ID 在单个进程生命周期内唯一递增；
- 清空记录不重置 ID。

并发场景必须保证：

- ID 不重复；
- 不发生集合并发修改异常；
- 历史容量不超过上限；
- 已成功返回的计算记录能按照容量策略处理；
- 计算失败不写入历史。

首版不引入 Repository 接口、缓存抽象、分片或复杂无锁结构。出现第二种存储实现或压测证明简单同步结构成为瓶颈时，再调整设计。

## 9. 参数与资源限制

| 项目 | 限制 |
|---|---:|
| `expression` 最大长度 | 1000 字符 |
| 最大 Token 数 | 500 |
| 最大括号或解析递归深度 | 100 |
| 历史查询默认 `limit` | 20 |
| 历史查询最大 `limit` | 100 |
| 内存历史容量 | 建议 1000 条，待最终确认 |

限制目的：

- 防止超长表达式过度消耗 CPU；
- 防止过深递归导致栈溢出；
- 防止单次查询返回过多数据；
- 防止历史记录无限增长。

## 10. 错误处理需求

所有 API 错误统一返回 JSON，不返回 Spring Boot 默认 HTML 页面，不泄露堆栈和内部实现。

错误结构：

```json
{
  "code": "DIVISION_BY_ZERO",
  "message": "Division by zero",
  "path": "/api/v1/calculations",
  "timestamp": "2026-08-13T10:00:00Z"
}
```

| HTTP 状态 | 错误码示例 | 场景 |
|---:|---|---|
| 400 | `INVALID_ARGUMENT` | 空表达式、非法角度单位、非法 `limit` |
| 400 | `EXPRESSION_SYNTAX_ERROR` | 语法错误、非法字符、未知函数 |
| 400 | `EXPRESSION_LIMIT_EXCEEDED` | 表达式长度、Token 或深度超限 |
| 404 | `HISTORY_NOT_FOUND` | 历史记录不存在 |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | `Content-Type` 错误 |
| 422 | `DIVISION_BY_ZERO` | 除数或模数为零 |
| 422 | `DOMAIN_ERROR` | `sqrt`、`ln` 等函数定义域错误 |
| 422 | `NON_FINITE_RESULT` | 计算结果为 `NaN` 或无穷大 |

错误信息不得包含：

- Java 堆栈；
- 异常类名；
- 本地文件路径；
- 内部实现细节；
- 敏感数据。

## 11. 架构与代码职责

建议调用关系：

```text
CalculationController
        ↓
CalculatorService
        ↓
ExpressionEvaluator
        ↓
CalculationHistory
```

### 11.1 CalculationController

负责：

- HTTP 路由；
- JSON 参数接收；
- 参数基本校验；
- 状态码和响应 DTO。

不负责数学解析和计算。

### 11.2 CalculatorService

负责：

- 调用表达式计算器；
- 只在计算成功后保存历史；
- 组装计算结果。

### 11.3 ExpressionEvaluator

负责：

- Token 识别；
- 语法解析；
- 表达式求值；
- 函数与常量白名单；
- 数值和资源边界检查。

该组件应为不依赖 Spring 的纯 Java 代码，便于独立单元测试。

### 11.4 CalculationHistory

负责：

- 保存计算记录；
- 查询历史列表；
- 查询单条历史；
- 清空历史；
- 容量控制；
- 并发安全。

### 11.5 全局异常处理器

负责：

- 将输入、语法、计算和历史异常映射为 HTTP 状态码；
- 统一错误响应格式；
- 隐藏内部异常和堆栈。

## 12. 测试需求

### 12.1 表达式单元测试

至少覆盖：

- `1 + 2 * 3 = 7`；
- `(1 + 2) * 3 = 9`；
- `2^3^2 = 512`；
- `-2^2 = -4`；
- `2^-2 = 0.25`；
- `sin(pi / 2) = 1`；
- DEGREE 模式下 `sin(90) = 1`；
- `sqrt(16) + ln(e) = 5`；
- 小数和科学计数法；
- 空表达式；
- 非法字符和脚本形式输入；
- 未知函数或标识符；
- 隐式乘法；
- 尾随 Token；
- 除零和模零；
- 函数定义域错误；
- 数值溢出；
- 超长表达式；
- Token 数量超限；
- 解析深度超限。

### 12.2 历史单元测试

至少覆盖：

- 正常新增；
- 按倒序返回；
- `limit` 生效；
- 非法 `limit`；
- 超容量淘汰最旧记录；
- 按 ID 查询；
- 查询不存在 ID；
- 清空历史；
- 清空后 ID 不重复；
- 并发写入 ID 唯一；
- 并发后容量不超过上限；
- 返回快照不能修改内部状态。

### 12.3 Web 契约测试

使用 MockMvc 至少验证：

- 成功计算的状态码和响应字段；
- 省略角度单位时默认为 `RADIAN`；
- 非法请求返回 `400`；
- 计算领域错误返回 `422`；
- 错误响应包含稳定的 `code`、`message`、`path`、`timestamp`；
- 历史列表返回 `200`；
- 不存在记录返回 `404`；
- 清空历史返回 `204`；
- 健康检查返回 `200`；
- 非法 JSON 或错误 Content-Type 不返回 HTML 错误页；
- 失败计算不会写入历史。

### 12.4 应用集成测试

至少验证：

- Spring Boot 上下文能够启动；
- Controller、Service、Evaluator、History 正常装配；
- 一次真实应用调用链能够完成计算并写入历史。

### 12.5 产物测试

执行：

```bash
java -version
mvn -version
mvn clean verify
```

验证：

- Maven 实际使用 Java 17；
- 编译和所有自动化测试通过；
- Spring Boot Maven Plugin 生成可执行 JAR。

启动产物：

```bash
java -jar target/scientific-calculator-*.jar
```

HTTP 冒烟验证：

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

## 13. 非功能需求

### 13.1 可运行性

- `java -jar` 可直接启动；
- 不依赖数据库、Redis 或外部网络；
- 不要求密钥或额外中间件；
- 默认配置即可完成本地验收。

### 13.2 可维护性

- HTTP、业务编排、表达式计算和历史存储职责分离；
- 核心计算逻辑为纯 Java，可独立测试；
- 错误码和 API 契约稳定；
- 不为单一实现预建接口、工厂或插件体系；
- 关键规则由自动化测试保护。

### 13.3 安全性

- 不执行用户提交的代码；
- 不使用脚本引擎或反射调用函数；
- 只接受固定 Token、函数和常量；
- 限制输入长度、Token 数和递归深度；
- 不在响应中返回堆栈和内部异常。

### 13.4 并发性

- 多个 HTTP 请求可以同时处理；
- 历史写入和清空线程安全；
- ID 在进程生命周期内唯一；
- 历史容量始终受控。

### 13.5 数据特性

- 所有数据只存在于当前 JVM 内存；
- JVM 重启后数据丢失；
- 数据只在单实例内有效；
- 不承诺跨实例共享和一致性。

## 14. 交付物

建议最终交付：

```text
pom.xml
src/main/java/...
src/test/java/...
README.md
docs/
├── REQUIREMENTS.md
├── AI_DELIVERY.md
├── TEST_EVIDENCE.md
└── superpowers/
    ├── specs/
    └── plans/
target/
└── scientific-calculator-*.jar
```

### 14.1 README.md

包含：

- 项目简介和技术栈；
- 构建、启动和验证方式；
- API 示例；
- 表达式语法；
- 错误码；
- 已知限制。

### 14.2 AI_DELIVERY.md

按阶段记录 AI 辅助和候选人校验：

| 阶段 | AI 辅助内容 | 候选人校验、取舍和修正 |
|---|---|---|
| 需求分析 | 拆解候选接口、表达式和边界 | 根据题目约束裁剪范围，确认验收标准 |
| 架构设计 | 提供解析和存储方案 | 选择纯 Java 白名单解析器，删除不必要抽象 |
| 编码 | 生成代码初稿 | 评审并修正幂运算、一元负号、有限值和并发边界 |
| 测试 | 生成测试矩阵 | 补充回归用例并实际运行，核对失败原因 |
| 打包验收 | 提供 Maven 和运行命令 | 确认 Java 17、JAR 启动及 HTTP 响应证据 |

不得虚构“个人手写了哪些代码行”。以实际决策、校验方法、修改记录和运行证据区分 AI 产出与个人贡献。

### 14.3 TEST_EVIDENCE.md

记录：

- 测试和构建命令；
- 测试总数、通过数、失败数；
- runnable JAR 文件名、大小和 SHA-256；
- 独立启动日志；
- HTTP 冒烟请求和响应；
- 已知限制和环境信息。

## 15. 最终验收标准

以下条件全部满足才视为完成：

1. 正常表达式计算正确；
2. 运算优先级和结合性符合本文档；
3. `RADIAN` 和 `DEGREE` 均可用；
4. 非法表达式被拒绝且不能执行代码；
5. 除零、定义域错误和溢出具有明确错误响应；
6. 失败计算不写入历史；
7. 历史可以查询、单条读取和清空；
8. 历史容量有上限，超限淘汰最旧记录；
9. 并发写入不产生重复 ID 或集合异常；
10. 所有自动化测试通过；
11. `mvn clean verify` 通过；
12. runnable JAR 能够通过 `java -jar` 独立启动；
13. HTTP 冒烟测试通过；
14. 系统没有数据库、缓存组件和外部接口调用；
15. 交付文档完整记录 AI 辅助和候选人校验、优化过程。

## 16. 待最终确认的产品决策

以下内容不阻碍继续梳理，但在冻结需求和进入实现前需要确认：

1. 是否保留 `DELETE /api/v1/calculations` 清空历史接口；
2. 科学函数是否仅支持第 4.1.4 节列出的 8 个单参数函数；
3. 默认历史容量是否确定为 1000 条；
4. 计算成功使用 `201 Created` 还是 `200 OK`。
