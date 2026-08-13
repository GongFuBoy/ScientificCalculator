# Scientific Calculator curl 测试案例

本文档基于当前源码、需求与设计整理，可直接用于本地 HTTP 验收。

覆盖范围：健康检查（1）、成功计算（10）、历史 CRUD（8）、请求/传输错误（9）、表达式语法错误（7）、计算领域错误（3）、资源限制（3）、失败不写历史和统一错误响应。按 C01→C42 顺序执行时，历史相关案例的前置条件成立；单独执行某个案例时，先执行 C02。

## 1. 测试前准备

项目无鉴权、数据库、Redis 或外部服务依赖。默认端口为 `8080`，如果启动时使用了其他端口，只需修改 `BASE_URL`。

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"

java -version
./mvnw -version

./mvnw clean verify
java -jar target/scientific-calculator-1.0.0.jar --server.port=8080
```

另开一个终端：

```bash
BASE_URL="${BASE_URL:-http://localhost:8080}"
```

如果本机配置了 HTTP 代理，以下命令可额外加上 `--noproxy localhost`，避免本地请求经过代理。

建议把响应头和响应体一起保留（`-i`），并在 CI/脚本中用 `--fail-with-body` 配合状态码断言；本文为人工验收案例，预期值写在每个案例下方。

除特别说明外：

- 成功请求预期为 `200 OK`；
- POST 请求必须携带 `Content-Type: application/json`；
- `angleUnit` 仅支持 `RADIAN`、`DEGREE`，省略或传空字符串时默认 `RADIAN`；
- 错误响应统一包含 `code`、`message`、`path`、`timestamp`；
- 计算成功才写入历史，失败请求不会产生历史记录；
- 历史仅存于当前进程，服务重启后丢失。

## 2. 健康检查与初始化

### C01 健康检查

```bash
curl -i -sS "$BASE_URL/health"
```

预期：`200 OK`。

```json
{"status":"UP"}
```

### C02 清空历史，保证测试起点一致

```bash
curl -i -sS -X DELETE "$BASE_URL/api/v1/calculations"
```

预期：`200 OK`。

```json
{"cleared":true}
```

## 3. 成功计算

### C03 默认弧度制与运算优先级

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1 + 2 * 3"}'
```

预期：`result` 为 `7.0`，`angleUnit` 为 `RADIAN`。

### C04 括号

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"(1 + 2) * 3"}'
```

预期：`result` 为 `9.0`。

### C05 整数、小数和科学计数法

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1 + 1.5 + .5 + 1e3 + 1.5E-2"}'
```

预期：`result` 为 `1003.015`。

### C06 加减乘除与取模

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"((8 + 2) * 3 - 4) / 2 % 7"}'
```

预期：`result` 为 `6.0`。

### C07 幂运算右结合

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"2^3^2"}'
```

预期：`result` 为 `512.0`。

### C08 一元负号优先级

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"-2^2"}'
```

预期：`result` 为 `-4.0`。

### C09 负指数

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"2^-2"}'
```

预期：`result` 为 `0.25`。

### C10 常量与全部科学函数

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"sin(pi/2)+cos(0)+tan(0)+sqrt(16)+abs(-3)+ln(e)+log(100)+exp(0)","angleUnit":"RADIAN"}'
```

预期：`result` 为 `13.0`。

### C11 角度制

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"sin(90)+cos(180)+tan(45)","angleUnit":"DEGREE"}'
```

预期：浮点结果接近 `1.0`。

### C12 空字符串角度单位使用默认值

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"sin(pi/2)","angleUnit":""}'
```

预期：`result` 为 `1.0`，`angleUnit` 为 `RADIAN`。

## 4. 历史记录

### C13 查询历史，使用默认 limit=20

```bash
curl -i -sS "$BASE_URL/api/v1/calculations"
```

预期：返回 JSON 数组，最新记录位于第一项，最多 20 条。

### C14 查询最近 1 条

```bash
curl -i -sS "$BASE_URL/api/v1/calculations?limit=1"
```

预期：返回长度为 1 的数组。

### C15 limit 最大边界

```bash
curl -i -sS "$BASE_URL/api/v1/calculations?limit=100"
```

预期：`200 OK`，最多返回 100 条。

### C16 按 ID 查询

先从 C13 或任意成功计算响应中取得 `id`：

```bash
CALC_ID=1
curl -i -sS "$BASE_URL/api/v1/calculations/$CALC_ID"
```

预期：`200 OK`，返回对应的单条计算记录。

### C17 不存在的 ID

```bash
curl -i -sS "$BASE_URL/api/v1/calculations/999999999"
```

预期：`404 Not Found`，`code` 为 `HISTORY_NOT_FOUND`。

### C18 非数字 ID

```bash
curl -i -sS "$BASE_URL/api/v1/calculations/not-a-number"
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C19 清空后 ID 不复用

先记录清空前任意成功响应中的 `id`，执行清空后再次计算：

```bash
curl -i -sS -X DELETE "$BASE_URL/api/v1/calculations"

curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1+1"}'
```

预期：新记录 `id` 大于清空前的最后一个 `id`。

### C20 清空后历史为空

```bash
curl -i -sS -X DELETE "$BASE_URL/api/v1/calculations"
curl -i -sS "$BASE_URL/api/v1/calculations"
```

预期：第二个请求返回 `[]`。

## 5. 请求参数与传输错误

### C21 缺少 expression

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{}'
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C22 expression 为空白

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"   "}'
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C23 JSON null 请求体

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d 'null'
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C24 非法 angleUnit

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1+1","angleUnit":"degree"}'
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C25 limit 小于最小值

```bash
curl -i -sS "$BASE_URL/api/v1/calculations?limit=0"
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C26 limit 大于最大值

```bash
curl -i -sS "$BASE_URL/api/v1/calculations?limit=101"
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C27 limit 类型错误

```bash
curl -i -sS "$BASE_URL/api/v1/calculations?limit=abc"
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C28 非法 JSON

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{bad json'
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`，响应仍为 JSON。

### C29 不支持的 Content-Type

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: text/plain' \
  -d '1+1'
```

预期：`415 Unsupported Media Type`，`code` 为 `UNSUPPORTED_MEDIA_TYPE`。

### C29a 缺少 Content-Type

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -d '{"expression":"1+1"}'
```

预期：`415 Unsupported Media Type`，`code` 为 `UNSUPPORTED_MEDIA_TYPE`。

## 6. 表达式语法错误

以下请求均预期返回 `400 Bad Request`，`code` 为 `EXPRESSION_SYNTAX_ERROR`。

### C30 不完整表达式

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1 +"}'
```

### C31 非法字符或脚本式输入

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1; system()"}'
```

### C32 未知函数

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"foo(1)"}'
```

### C33 隐式乘法：数字与常量

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"2pi"}'
```

### C34 隐式乘法：数字与括号

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"2(3+4)"}'
```

### C35 多参数函数

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"sqrt(1,2)"}'
```

### C36 不支持的数值或标识符格式

分别执行：

```bash
for expression in 'NaN' 'Infinity' '0x10' '1f' 'PI'; do
  curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
    -H 'Content-Type: application/json' \
    -d "{\"expression\":\"$expression\"}"
done
```

预期：每个请求均为 `400 Bad Request`，`code` 为 `EXPRESSION_SYNTAX_ERROR`。

### C36a 尾随 token

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1 + 2 abc"}'
```

预期：`400 Bad Request`，`code` 为 `EXPRESSION_SYNTAX_ERROR`；不会保存历史记录。

## 7. 计算领域错误

### C37 除零与模零

```bash
for expression in '1/0' '1%0'; do
  curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
    -H 'Content-Type: application/json' \
    -d "{\"expression\":\"$expression\"}"
done
```

预期：`422 Unprocessable Entity`，`code` 为 `DIVISION_BY_ZERO`。

### C38 函数定义域错误

```bash
for expression in 'sqrt(-1)' 'ln(0)' 'ln(-1)' 'log(0)' '(-1)^0.5'; do
  curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
    -H 'Content-Type: application/json' \
    -d "{\"expression\":\"$expression\"}"
done
```

预期：`422 Unprocessable Entity`，`code` 为 `DOMAIN_ERROR`。

### C39 非有限结果

```bash
for expression in 'exp(1000)' '1e309' '1e308*1e308'; do
  curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
    -H 'Content-Type: application/json' \
    -d "{\"expression\":\"$expression\"}"
done
```

预期：`422 Unprocessable Entity`，`code` 为 `NON_FINITE_RESULT`。

## 7a. 路径与查询参数边界

### C39a 负数 ID

```bash
curl -i -sS "$BASE_URL/api/v1/calculations/-1"
```

预期：`404 Not Found`，`code` 为 `HISTORY_NOT_FOUND`。

### C39b 超出 long 范围的 ID

```bash
curl -i -sS "$BASE_URL/api/v1/calculations/999999999999999999999999"
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

### C39c 小数 limit

```bash
curl -i -sS "$BASE_URL/api/v1/calculations?limit=10.5"
```

预期：`400 Bad Request`，`code` 为 `INVALID_ARGUMENT`。

## 8. 表达式资源限制

以下命令使用 zsh/bash 生成边界输入，不依赖 `jq` 或额外脚本。

### C40 长度超过 1000 字符

```bash
LONG_EXPRESSION="$(printf '1%.0s' {1..1001})"
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d "{\"expression\":\"$LONG_EXPRESSION\"}"
```

预期：`400 Bad Request`，`code` 为 `EXPRESSION_LIMIT_EXCEEDED`。

### C41 Token 超过 500 个

```bash
TOKEN_EXPRESSION="$(printf '1+%.0s' {1..250})1"
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d "{\"expression\":\"$TOKEN_EXPRESSION\"}"
```

预期：`400 Bad Request`，`code` 为 `EXPRESSION_LIMIT_EXCEEDED`。

### C42 嵌套深度超过 100 层

```bash
DEEP_EXPRESSION="$(printf '(%.0s' {1..101})1$(printf ')%.0s' {1..101})"
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d "{\"expression\":\"$DEEP_EXPRESSION\"}"
```

预期：`400 Bad Request`，`code` 为 `EXPRESSION_LIMIT_EXCEEDED`。

## 9. 失败计算不写入历史

先清空并读取当前历史：

```bash
curl -sS -X DELETE "$BASE_URL/api/v1/calculations"
curl -sS "$BASE_URL/api/v1/calculations"
```

制造一次失败计算后再次查询：

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"sqrt(-1)"}'

curl -i -sS "$BASE_URL/api/v1/calculations"
```

预期：失败请求为 `422 DOMAIN_ERROR`，随后历史仍为 `[]`。

## 10. 统一错误响应检查

任选一个错误请求，例如：

```bash
curl -i -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"1/0"}'
```

预期响应包含 JSON Content-Type，响应体结构如下，且不包含堆栈、异常类名或内部实现细节：

```json
{
  "code": "DIVISION_BY_ZERO",
  "message": "Division by zero",
  "path": "/api/v1/calculations",
  "timestamp": "2026-08-13T10:00:00Z"
}
```

## 11. 收尾

```bash
curl -i -sS -X DELETE "$BASE_URL/api/v1/calculations"
curl -i -sS "$BASE_URL/api/v1/calculations"
```

预期：清空响应为 `{"cleared":true}`，最终历史为 `[]`。

## 11a. 进程重启后的内存语义（可选）

该服务不持久化历史。启动服务后先执行一次成功计算并记下 `id`，停止进程，再用同一个 JAR 启动：

```bash
curl -sS -X POST "$BASE_URL/api/v1/calculations" \
  -H 'Content-Type: application/json' \
  -d '{"expression":"6*7"}'

# 停止并重新启动 java -jar 进程后
curl -i -sS "$BASE_URL/api/v1/calculations"
```

预期：重启后的历史返回 `[]`；这是设计要求，不是数据丢失缺陷。

## 12. 不建议通过公开 curl 强行覆盖的场景

以下场景当前已有 JUnit/MockMvc 测试，更适合自动化验证：

- 历史容量达到 1000 后淘汰最旧记录；
- 多线程并发写入时 ID 唯一、容量不超限；
- 通用 `500 INTERNAL_ERROR` 脱敏。当前没有用于制造内部异常的公开生产端点，不应为了 curl 测试新增测试后门。
