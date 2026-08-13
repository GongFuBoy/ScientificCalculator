# 科学计算器实施计划

> **致执行代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 子技能，按任务逐项实施本计划。步骤使用复选框（`- [ ]`）跟踪。

**目标：** 构建并验证一个 Java 17/Spring Boot 3 HTTP 科学计算器，提供安全的表达式求值和有界的内存计算历史。

**架构：** 使用小型 Spring Boot 单体应用，暴露计算、历史、清空历史和健康检查接口。`CalculatorService` 协调纯 Java 白名单递归下降求值器与线程安全的有界历史存储；全局异常处理器将稳定的领域错误映射为 JSON 响应。不使用数据库、缓存、脚本引擎、反射或外部服务。

**技术栈：** Java 17、Spring Boot 3.3.13、Maven、JUnit 5、Spring MVC MockMvc、JDK 标准并发/数学 API。

---

## 文件清单

第一版只创建运行所需的文件：

- `pom.xml` — Spring Boot 3.3.13 父 POM、Java 17 编译设置、Web/测试依赖和可执行 JAR 插件。
- `.mvn/wrapper/maven-wrapper.properties`、`mvnw`、`mvnw.cmd` — 固定 Maven 3.9.9 的 Wrapper，保证构建可复现。
- `src/main/java/com/example/scientificcalculator/ScientificCalculatorApplication.java` — Spring Boot 启动类，并在类型创建后显式声明 Bean。
- `src/main/java/com/example/scientificcalculator/calculation/AngleUnit.java` — 固定的 `RADIAN`/`DEGREE` 枚举。
- `src/main/java/com/example/scientificcalculator/calculation/CalculationRecord.java` — 不可变历史/结果记录。
- `src/main/java/com/example/scientificcalculator/calculation/CalculationRequest.java` — HTTP 请求 DTO，角度单位先接收字符串以便返回稳定校验错误。
- `src/main/java/com/example/scientificcalculator/calculation/CalculationController.java` — 计算、历史和清空接口。
- `src/main/java/com/example/scientificcalculator/calculation/CalculatorService.java` — 计算、保存、列表、查询和清空编排。
- `src/main/java/com/example/scientificcalculator/calculation/HistoryNotFoundException.java` — 明确表示 `404 HISTORY_NOT_FOUND` 的异常。
- `src/main/java/com/example/scientificcalculator/calculation/CalculationHistory.java` — 容量 1000 的有界线程安全内存存储。
- `src/main/java/com/example/scientificcalculator/expression/ExpressionException.java` — 求值器异常及稳定错误码。
- `src/main/java/com/example/scientificcalculator/expression/ExpressionEvaluator.java` — 词法分析、递归下降解析、函数白名单及有限值/定义域检查。
- `src/main/java/com/example/scientificcalculator/api/ApiExceptionHandler.java` — 应用异常和 Spring MVC 异常的 JSON 映射。
- `src/main/java/com/example/scientificcalculator/api/HealthController.java` — `GET /health`。
- `src/test/java/com/example/scientificcalculator/expression/ExpressionEvaluatorTest.java` — 解析器/求值器行为与限制测试。
- `src/test/java/com/example/scientificcalculator/calculation/CalculationHistoryTest.java` — 历史顺序、容量、快照和并发测试。
- `src/test/java/com/example/scientificcalculator/calculation/CalculatorServiceTest.java` — 先计算后保存及失败原子性测试。
- `src/test/java/com/example/scientificcalculator/calculation/CalculationControllerTest.java` — MockMvc HTTP 契约测试。
- `src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java` — 真实上下文的 POST/列表集成测试。
- `README.md` — 构建、启动、API、语法和已知限制说明。
- `docs/AI_DELIVERY.md` — 实际 AI 提示/输出、人工检查、修正和证据。
- `docs/TEST_EVIDENCE.md` — 最终命令输出、测试摘要、JAR 校验和及 curl 证据。

## 任务 1：初始化 Maven 应用

**文件：**
- 创建：`pom.xml`
- 创建：`.mvn/wrapper/maven-wrapper.properties`
- 创建：`mvnw`
- 创建：`mvnw.cmd`
- 创建：`src/main/java/com/example/scientificcalculator/ScientificCalculatorApplication.java`
- 创建：`src/main/java/com/example/scientificcalculator/api/HealthController.java`
- 测试：`src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java`

- [ ] **步骤 1：编写失败的上下文测试**

```java
@SpringBootTest
class ScientificCalculatorApplicationTest {
    @Test
    void springContextStarts() {
    }
}
```

- [ ] **步骤 2：运行测试并确认预期的项目缺失失败**

运行：`mvn test -Dtest=ScientificCalculatorApplicationTest`

预期：Maven 报告不存在 `pom.xml`。

- [ ] **步骤 3：添加最小 Maven/Spring Boot 文件**

使用 Spring Boot `3.3.13`、`spring-boot-starter-web` 和 `spring-boot-starter-test`；将 `java.version` 设置为 `17`；配置 `spring-boot-maven-plugin`，不增加额外运行时依赖。添加固定到 Maven `3.9.9` 的标准 Maven Wrapper。由于请求契约很小且可以直接校验，不添加校验或工具类依赖。

```java
@SpringBootApplication
public class ScientificCalculatorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScientificCalculatorApplication.class, args);
    }
}
```

```java
@RestController
class HealthController {
    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
```

- [ ] **步骤 4：运行上下文测试**

运行：`./mvnw test -Dtest=ScientificCalculatorApplicationTest`

预期：测试通过，Spring 成功创建应用上下文。

- [ ] **步骤 5：提交初始化代码**

```bash
git add pom.xml .mvn mvnw mvnw.cmd src/main src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java
git commit -m "build: bootstrap spring boot calculator service"
```

## 任务 2：以测试驱动方式实现表达式求值器

**文件：**
- 创建：`src/main/java/com/example/scientificcalculator/expression/ExpressionException.java`
- 创建：`src/main/java/com/example/scientificcalculator/expression/ExpressionEvaluator.java`
- 测试：`src/test/java/com/example/scientificcalculator/expression/ExpressionEvaluatorTest.java`

- [ ] **步骤 1：编写首批失败测试**

```java
class ExpressionEvaluatorTest {
    private final ExpressionEvaluator evaluator = new ExpressionEvaluator();

    @Test
    void respectsPrecedenceAndParentheses() {
        assertEquals(7, evaluator.evaluate("1 + 2 * 3", AngleUnit.RADIAN), 1e-12);
        assertEquals(9, evaluator.evaluate("(1 + 2) * 3", AngleUnit.RADIAN), 1e-12);
    }

    @Test
    void supportsRightAssociativePowerAndUnarySigns() {
        assertEquals(512, evaluator.evaluate("2^3^2", AngleUnit.RADIAN), 1e-12);
        assertEquals(-4, evaluator.evaluate("-2^2", AngleUnit.RADIAN), 1e-12);
        assertEquals(0.25, evaluator.evaluate("2^-2", AngleUnit.RADIAN), 1e-12);
    }
}
```

- [ ] **步骤 2：运行测试并确认因求值器类型不存在而失败**

运行：`./mvnw test -Dtest=ExpressionEvaluatorTest`

预期：编译失败，提示缺少求值器和 `AngleUnit`。

- [ ] **步骤 3：以最小实现完成词法分析和解析**

创建只包含 `RADIAN` 和 `DEGREE` 值的 `AngleUnit`。

为输入实现私有游标，并提供以下方法：`parseAdditive`、`parseMultiplicative`、`parseUnary`、`parsePower` 和 `parsePrimary`。`parsePower` 必须解析一个可选的 `^`，后接 `parseUnary`，从而保证幂运算右结合并支持负指数。调用 `Double.parseDouble` 前，先显式解析十进制和科学计数法数字；只接受 ASCII 运算符、括号、仅用于拒绝的逗号，以及小写标识符。根表达式解析完成后，拒绝尾随输入。

公开求值器契约必须是：

```java
public double evaluate(String expression, AngleUnit angleUnit)
```

每个数字、标识符、运算符和括号计为一个 Token；空白和 EOF 不计数。拒绝第 501 个 Token。每进入一个括号表达式或函数调用就增加嵌套深度，在递归前拒绝第 101 层，并始终在 `finally` 中递减。

使用 `ExpressionException` 错误码：`EXPRESSION_SYNTAX_ERROR`、`EXPRESSION_LIMIT_EXCEEDED`、`DIVISION_BY_ZERO`、`DOMAIN_ERROR` 和 `NON_FINITE_RESULT`。

- [ ] **步骤 4：补齐求值器行为测试**

增加以下参数化成功矩阵，并对失败场景断言异常错误码：

```java
@ParameterizedTest
@CsvSource({
    "'1 + 2 * 3', 7.0, RADIAN",
    "'(1 + 2) * 3', 9.0, RADIAN",
    "'.5 + 1.5e2', 150.5, RADIAN",
    "'2^3^2', 512.0, RADIAN",
    "'-2^2', -4.0, RADIAN",
    "'2^-2', 0.25, RADIAN",
    "'sqrt(16) + ln(e)', 5.0, RADIAN",
    "'sin(pi / 2)', 1.0, RADIAN",
    "'sin(90)', 1.0, DEGREE",
    "'abs(-3) + log(100)', 5.0, RADIAN"
})
void evaluatesSupportedExpressions(String expression, double expected, AngleUnit unit) {
    assertEquals(expected, evaluator.evaluate(expression, unit), 1e-10);
}

@ParameterizedTest
@ValueSource(strings = {"1 +", "1; system()", "foo(1)", "2pi", "2(3+4)", "1 + 2 abc", "1e", "1..2"})
void rejectsInvalidSyntax(String expression) {
    assertEquals("EXPRESSION_SYNTAX_ERROR",
        assertThrows(ExpressionException.class, () -> evaluator.evaluate(expression, AngleUnit.RADIAN)).code());
}
```

分别增加 `1/0`、`1%0`（`DIVISION_BY_ZERO`），`sqrt(-1)`、`ln(0)`（`DOMAIN_ERROR`），`exp(1000)`（`NON_FINITE_RESULT`），表达式长度 1001、超过 500 个 Token、超过 100 层嵌套括号（`EXPRESSION_LIMIT_EXCEEDED`）的测试。

- [ ] **步骤 5：运行求值器测试套件并确认通过**

运行：`./mvnw test -Dtest=ExpressionEvaluatorTest`

预期：所有求值器测试通过，无警告或错误。

- [ ] **步骤 6：提交求值器**

```bash
git add src/main/java/com/example/scientificcalculator/expression src/test/java/com/example/scientificcalculator/expression src/main/java/com/example/scientificcalculator/calculation/AngleUnit.java
git commit -m "feat: add safe scientific expression evaluator"
```

## 任务 3：增加有界内存历史

**文件：**
- 创建：`src/main/java/com/example/scientificcalculator/calculation/CalculationRecord.java`
- 创建：`src/main/java/com/example/scientificcalculator/calculation/CalculationHistory.java`
- 测试：`src/test/java/com/example/scientificcalculator/calculation/CalculationHistoryTest.java`

- [ ] **步骤 1：编写失败的历史测试**

```java
class CalculationHistoryTest {
    @Test
    void keepsNewestRecordsAndDoesNotExceedCapacity() {
        CalculationHistory history = new CalculationHistory(2);
        history.add("1", AngleUnit.RADIAN, 1);
        history.add("2", AngleUnit.RADIAN, 2);
        history.add("3", AngleUnit.RADIAN, 3);

        assertEquals(List.of("3", "2"),
                history.list(10).stream().map(CalculationRecord::expression).toList());
    }

    @Test
    void concurrentWritesProduceUniqueIdsWithinCapacity() throws Exception {
        CalculationHistory history = new CalculationHistory(1000);
        int writers = 8;
        int writesPerWriter = 100;
        ExecutorService pool = Executors.newFixedThreadPool(writers);
        CountDownLatch ready = new CountDownLatch(writers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<List<Long>>> futures = new ArrayList<>();
        for (int i = 0; i < writers; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                start.await(2, TimeUnit.SECONDS);
                List<Long> ids = new ArrayList<>();
                for (int j = 0; j < writesPerWriter; j++) {
                    ids.add(history.add("1+1", AngleUnit.RADIAN, 2.0).id());
                }
                return ids;
            }));
        }
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();
        Set<Long> ids = new HashSet<>();
        for (Future<List<Long>> future : futures) {
            ids.addAll(future.get(5, TimeUnit.SECONDS));
        }
        pool.shutdownNow();
        assertEquals(writers * writesPerWriter, ids.size());
        assertTrue(history.list(1000).size() <= 1000);
    }
}
```

- [ ] **步骤 2：运行历史测试并确认失败**

运行：`./mvnw test -Dtest=CalculationHistoryTest`

预期：因记录/存储类型缺失而编译失败。

- [ ] **步骤 3：实现最小线程安全存储**

使用 `long nextId`、实例字段 `capacity` 和由同一把锁保护的 `Deque<CalculationRecord>`。在一个同步区段内执行 `++nextId`、使用 `Instant.now()` 创建不可变记录、插入队首，并在 `size() > capacity` 时从队尾淘汰，确保 ID 顺序与插入顺序一致。`list(limit)` 返回新的不可变列表；`find(id)` 返回 `Optional`；`clear()` 清空队列但不重置计数器。构造函数拒绝小于 1 的容量，生产应用 Bean 使用容量 1000。

存储契约必须是：

```java
public CalculationRecord add(String expression, AngleUnit angleUnit, double result)
public List<CalculationRecord> list(int limit)
public Optional<CalculationRecord> find(long id)
public void clear()
```

- [ ] **步骤 4：增加顺序、限制、查询、清空、快照和并发断言**

验证不存在的 ID 返回空值；清空后下一个 ID 仍大于所有历史 ID；返回列表拒绝修改且不改变存储。HTTP `limit` 校验仅在任务 4 中完成。

- [ ] **步骤 5：运行历史测试套件**

运行：`./mvnw test -Dtest=CalculationHistoryTest`

预期：所有历史测试通过。

- [ ] **步骤 6：提交历史存储**

```bash
git add src/main/java/com/example/scientificcalculator/calculation src/test/java/com/example/scientificcalculator/calculation/CalculationHistoryTest.java
git commit -m "feat: add bounded in-memory calculation history"
```

## 任务 4：接入服务、DTO、控制器和错误响应

**文件：**
- 创建：`src/main/java/com/example/scientificcalculator/calculation/CalculationRequest.java`
- 创建：`src/main/java/com/example/scientificcalculator/calculation/CalculatorService.java`
- 创建：`src/main/java/com/example/scientificcalculator/calculation/HistoryNotFoundException.java`
- 创建：`src/main/java/com/example/scientificcalculator/calculation/CalculationController.java`
- 创建：`src/main/java/com/example/scientificcalculator/api/ApiExceptionHandler.java`
- 修改：`src/main/java/com/example/scientificcalculator/ScientificCalculatorApplication.java`，显式暴露 `CalculationHistory(1000)` 和 `ExpressionEvaluator` Spring Bean。
- 测试：`src/test/java/com/example/scientificcalculator/calculation/CalculationControllerTest.java`
- 测试：`src/test/java/com/example/scientificcalculator/calculation/CalculatorServiceTest.java`

- [ ] **步骤 1：编写失败的 MockMvc 契约测试**

先编写聚焦于服务失败原子性的测试：

```java
@Test
void failedEvaluationDoesNotWriteHistory() {
    CalculationHistory history = new CalculationHistory(1000);
    CalculatorService service = new CalculatorService(new ExpressionEvaluator(), history);

    assertThrows(ExpressionException.class,
            () -> service.calculate("1/0", AngleUnit.RADIAN));
    assertTrue(history.list(100).isEmpty());
}
```

再编写 HTTP 契约测试：

```java
class CalculationControllerTest {
    private CalculationHistory history;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        history = new CalculationHistory(1000);
        CalculatorService service = new CalculatorService(new ExpressionEvaluator(), history);
        mvc = MockMvcBuilders.standaloneSetup(new CalculationController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void calculatesAndStoresWithDefaultRadianUnit() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expression\":\"1+2*3\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value(7.0))
            .andExpect(jsonPath("$.angleUnit").value("RADIAN"));
    }
}
```

- [ ] **步骤 2：运行 Web 测试并确认失败**

运行：`./mvnw test -Dtest=CalculationControllerTest`

预期：因控制器、服务或 DTO 不存在而编译失败或路由失败。

- [ ] **步骤 3：实现请求到结果的流程**

将 `CalculationRequest` 定义为包含 `String expression` 和可空 `String angleUnit` 的 record。缺失或空白单位规范化为 `RADIAN`；其他值使用 `AngleUnit.valueOf` 解析，非法值映射为 `400 INVALID_ARGUMENT`。在 HTTP 边界校验表达式非空且长度不超过 1000。仅在控制器中校验历史 `limit` 为 1–100；存储作为内部组件，只接收已校验值。`CalculatorService.calculate` 先求值，成功后才写入历史。计算、列表、单条记录、健康检查和清空历史均返回 `200 OK`；清空返回 `{"cleared":true}`。

服务契约必须是：

```java
public CalculationRecord calculate(String expression, AngleUnit angleUnit)
public List<CalculationRecord> history(int limit)
public CalculationRecord history(long id)
public void clearHistory()
```

记录不存在时，`history(long)` 抛出 `HistoryNotFoundException(id)`。

求值器和历史类创建后，添加以下 Bean：

```java
@Bean
ExpressionEvaluator expressionEvaluator() {
    return new ExpressionEvaluator();
}

@Bean
CalculationHistory calculationHistory() {
    return new CalculationHistory(1000);
}
```

- [ ] **步骤 4：实现全局错误映射器**

将求值器/领域错误映射到文档规定的状态码和错误码；历史记录缺失映射为 `404 HISTORY_NOT_FOUND`。显式处理 `MethodArgumentTypeMismatchException`（`400 INVALID_ARGUMENT`）、`HttpMessageNotReadableException`（`400 INVALID_ARGUMENT`）和 `HttpMediaTypeNotSupportedException`（`415 UNSUPPORTED_MEDIA_TYPE`）。最后增加 `Exception` 兜底处理，返回 `500 INTERNAL_ERROR`，不包含异常类名或堆栈。每个错误都包含 `code`、`message`、`path` 和 `timestamp`。

- [ ] **步骤 5：扩展 MockMvc 测试**

覆盖 DEGREE 计算、历史顺序和 limit、单条记录成功/404、清空响应及后续空列表、空白表达式（`INVALID_ARGUMENT`）、格式错误的 JSON、非法角度单位、非数字/非法 `limit`、求值器 `422` 错误、非法媒体类型、无堆栈详情的通用 `500`、稳定错误结构，以及失败计算不出现在历史中。

- [ ] **步骤 6：运行控制器测试套件**

运行：`./mvnw test -Dtest=CalculationControllerTest`

预期：所有 HTTP 契约测试通过。

- [ ] **步骤 7：提交 HTTP 层**

```bash
git add src/main/java/com/example/scientificcalculator/calculation src/main/java/com/example/scientificcalculator/api src/test/java/com/example/scientificcalculator/calculation/CalculationControllerTest.java
git commit -m "feat: expose calculator and history HTTP APIs"
```

## 任务 5：文档和可执行 JAR 验证

**文件：**
- 创建：`README.md`
- 创建：`docs/AI_DELIVERY.md`
- 创建：`docs/TEST_EVIDENCE.md`
- 修改：`src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java`，通过真实 Spring 上下文调用 `CalculatorService` 并断言保存了一次计算。

- [ ] **步骤 1：根据已冻结需求补充文档**

`README.md` 必须说明 Java 17 前置条件、`./mvnw clean verify`、`java -jar`、所有接口、请求/响应示例、语法、函数白名单、角度单位、错误码、仅内存且重启丢失的行为，以及固定 1000 条容量。

`docs/AI_DELIVERY.md` 必须包含表格，记录本次会话实际使用的提示词、AI 输出摘要、个人验证、个人修正，以及需求、设计、编码、测试和打包阶段的证据路径/命令。

`docs/TEST_EVIDENCE.md` 必须记录真实 Java/Maven 版本、测试摘要、JAR 路径、SHA-256、启动输出，以及执行验证命令后的 curl 响应；命令执行前不得虚构结果。

- [ ] **步骤 2：将上下文测试升级为真实应用集成测试**

为 `ScientificCalculatorApplicationTest` 添加 `@SpringBootTest` 和 `@AutoConfigureMockMvc`。向真实应用上下文 POST `{"expression":"1+2"}`，断言返回 `200` 且结果为 `3.0`，随后 GET 历史并断言记录存在，从而验证 Spring Bean wiring 以及 controller→service→evaluator→history 路径。

- [ ] **步骤 3：运行完整验证命令**

运行：`./mvnw clean verify`

预期：退出码为 0，所有测试通过，并生成 `target/scientific-calculator-*.jar`。

- [ ] **步骤 4：独立验证 JAR**

在独立进程中运行 JAR：

```bash
java -jar target/scientific-calculator-*.jar
```

在另一终端运行：

```bash
curl -i http://localhost:8080/health
curl -i -X POST http://localhost:8080/api/v1/calculations -H 'Content-Type: application/json' -d '{"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE"}'
curl -i 'http://localhost:8080/api/v1/calculations?limit=10'
curl -i -X POST http://localhost:8080/api/v1/calculations -H 'Content-Type: application/json' -d '{"expression":"1/0"}'
curl -i -X DELETE http://localhost:8080/api/v1/calculations
```

预期：健康检查、计算、列表和清空均返回 `200`；计算结果为 `5.0`；除零返回 `422`；清空返回 `{"cleared":true}`。

- [ ] **步骤 5：记录证据和校验和**

运行 `shasum -a 256 target/scientific-calculator-*.jar`，将真实输出、测试摘要和 HTTP 响应复制到 `docs/TEST_EVIDENCE.md`。检查命令输出前不得宣称结果。

- [ ] **步骤 6：提交文档和证据**

```bash
git add README.md docs/AI_DELIVERY.md docs/TEST_EVIDENCE.md src/test
git commit -m "docs: add delivery and verification evidence"
```

## 自查清单

- [ ] 每条已冻结需求都有对应的实现或验证任务。
- [ ] 没有引入数据库、缓存、外部调用、脚本引擎或动态执行路径。
- [ ] 所有成功 HTTP 接口使用 `200 OK`；清空历史返回 `{"cleared":true}`。
- [ ] 确切实现 8 个单参数函数。
- [ ] 历史容量固定为 1000，清空后 ID 仍保持单调递增。
- [ ] 解析器测试保护幂运算右结合和一元符号语义。
- [ ] 错误码及状态映射与 `REQUIREMENTS.md` 和设计规格一致。
- [ ] 最终结论基于最新的 `./mvnw clean verify`、JAR 启动、curl 和校验和证据。
