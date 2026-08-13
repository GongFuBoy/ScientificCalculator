# Scientific Calculator

Java 17 + Spring Boot 3.3.13 实现的纯内存科学计算器 HTTP 后端。无需数据库、缓存或外部接口，重启后历史记录丢失。

完整项目文档入口：[docs/README.md](docs/README.md)

## 构建与启动

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw clean verify
java -jar target/scientific-calculator-1.0.0.jar
```

服务默认监听 `8080`。

## API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/health` | 健康检查 |
| POST | `/api/v1/calculations` | 计算并保存成功记录 |
| GET | `/api/v1/calculations?limit=20` | 查询最近历史，`limit` 为 1～100 |
| GET | `/api/v1/calculations/{id}` | 查询单条历史 |
| DELETE | `/api/v1/calculations` | 清空历史，返回 `{"cleared":true}` |

计算请求：

```json
{"expression":"sin(pi / 2) + sqrt(16)","angleUnit":"RADIAN"}
```

`angleUnit` 可选值为 `RADIAN` 或 `DEGREE`，缺失时默认为 `RADIAN`。所有成功接口返回 `200 OK`。

## 表达式能力

支持数字、小数、科学计数法、常量 `pi`/`e`、括号、`+ - * / % ^`，幂运算右结合，并支持一元正负号。单参数函数严格限制为：`sin`、`cos`、`tan`、`sqrt`、`abs`、`ln`、`log`、`exp`。

表达式长度最多 1000 字符，最多 500 个 Token，最大嵌套深度 100。结果使用 Java `double`。

## 错误码

错误响应统一包含 `code`、`message`、`path`、`timestamp`，不返回堆栈。常见映射：

- `400`：`INVALID_ARGUMENT`、`EXPRESSION_SYNTAX_ERROR`、`EXPRESSION_LIMIT_EXCEEDED`
- `404`：`HISTORY_NOT_FOUND`
- `415`：`UNSUPPORTED_MEDIA_TYPE`
- `422`：`DIVISION_BY_ZERO`、`DOMAIN_ERROR`、`NON_FINITE_RESULT`
- `500`：`INTERNAL_ERROR`

历史最多保存 1000 条，只保留最新记录；清空后 ID 继续递增且不复用。
