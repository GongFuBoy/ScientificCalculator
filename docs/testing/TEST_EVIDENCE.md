# 测试与验收证据

> 本文件只记录已实际执行的命令结果；时间以 Asia/Shanghai 为准。

## 构建环境

```text
JAVA_HOME=/opt/homebrew/opt/openjdk@17
Java: 17.0.20
Maven Wrapper: Apache Maven 3.9.9
Spring Boot: 3.3.13
```

## 自动化测试

执行：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 \
PATH=/opt/homebrew/opt/openjdk@17/bin:$PATH \
HTTP_PROXY=http://127.0.0.1:7897 \
HTTPS_PROXY=http://127.0.0.1:7897 \
ALL_PROXY=socks5h://127.0.0.1:7897 \
./mvnw clean verify
```

结果：`BUILD SUCCESS`，最终验证 `Tests run: 48, Failures: 0, Errors: 0, Skipped: 0`。

覆盖范围包括：表达式优先级/函数/角度/限制，历史容量/顺序/并发/快照，服务失败原子性，HTTP 成功与错误契约，真实 Spring 上下文链路。

## JAR

产物：`target/scientific-calculator-1.0.0.jar`

SHA-256：

```bash
shasum -a 256 target/scientific-calculator-1.0.0.jar
# a8bfdbaf4a7cbf0ee18bd1d48951cc4c78dade702ae5a95a63ede5511a2ab4a8  target/scientific-calculator-1.0.0.jar
```

## 独立进程 HTTP 验收

启动：

```bash
java -jar target/scientific-calculator-1.0.0.jar
```

进程使用 `--server.port=18080` 启动，日志显示 `Tomcat started on port 18080`。按顺序执行请求，真实结果如下：

```bash
curl -i http://localhost:18080/health
curl -i -X POST http://localhost:18080/api/v1/calculations -H 'Content-Type: application/json' -d '{"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE"}'
curl -i 'http://localhost:18080/api/v1/calculations?limit=10'
curl -i -X POST http://localhost:18080/api/v1/calculations -H 'Content-Type: application/json' -d '{"expression":"1/0"}'
curl -i -X DELETE http://localhost:18080/api/v1/calculations
```

```text
GET /health
HTTP/1.1 200
{"status":"UP"}

POST /api/v1/calculations  {"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE"}
HTTP/1.1 200
{"id":2,"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE","result":5.0,"createdAt":"2026-08-13T08:31:41.227496Z"}

GET /api/v1/calculations?limit=10
HTTP/1.1 200
[{"id":2,"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE","result":5.0,"createdAt":"2026-08-13T08:31:41.227496Z"},{"id":1,"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE","result":5.0,"createdAt":"2026-08-13T08:31:29.146883Z"}]

POST /api/v1/calculations  {"expression":"1/0"}
HTTP/1.1 422
{"code":"DIVISION_BY_ZERO","message":"Division by zero","path":"/api/v1/calculations","timestamp":"2026-08-13T08:31:41.334211Z"}

DELETE /api/v1/calculations
HTTP/1.1 200
{"cleared":true}

GET /api/v1/calculations?limit=10
HTTP/1.1 200
[]
```
