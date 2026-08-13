# Scientific Calculator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and verify a Java 17/Spring Boot 3 HTTP scientific calculator with safe expression evaluation and a bounded in-memory calculation history.

**Architecture:** A small Spring Boot monolith exposes calculation, history, clear-history, and health endpoints. `CalculatorService` coordinates a pure-Java whitelist recursive-descent evaluator and a thread-safe bounded history store; a global exception handler maps stable domain errors to JSON responses. No database, cache, script engine, reflection, or external service is used.

**Tech Stack:** Java 17, Spring Boot 3.3.13, Maven, JUnit 5, Spring MVC MockMvc, standard JDK concurrency/math APIs.

---

## File map

Create only the files needed for the first runnable version:

- `pom.xml` — Spring Boot 3.3.13 parent, Java 17 compiler settings, web/test dependencies, executable-JAR plugin.
- `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd` — Maven 3.9.9 wrapper for reproducible builds.
- `src/main/java/com/example/scientificcalculator/ScientificCalculatorApplication.java` — Spring Boot entry point and explicit beans added after their types exist.
- `src/main/java/com/example/scientificcalculator/calculation/AngleUnit.java` — fixed `RADIAN`/`DEGREE` enum.
- `src/main/java/com/example/scientificcalculator/calculation/CalculationRecord.java` — immutable history/result record.
- `src/main/java/com/example/scientificcalculator/calculation/CalculationRequest.java` — HTTP request DTO with string angle-unit input for stable validation errors.
- `src/main/java/com/example/scientificcalculator/calculation/CalculationController.java` — calculation/history/clear endpoints.
- `src/main/java/com/example/scientificcalculator/calculation/CalculatorService.java` — calculate, store, list, find, clear orchestration.
- `src/main/java/com/example/scientificcalculator/calculation/HistoryNotFoundException.java` — explicit `404 HISTORY_NOT_FOUND` signal.
- `src/main/java/com/example/scientificcalculator/calculation/CalculationHistory.java` — bounded thread-safe in-memory store with capacity 1000.
- `src/main/java/com/example/scientificcalculator/expression/ExpressionException.java` — typed evaluator errors and stable error codes.
- `src/main/java/com/example/scientificcalculator/expression/ExpressionEvaluator.java` — tokenizer, recursive-descent parser, function whitelist, finite/domain checks.
- `src/main/java/com/example/scientificcalculator/api/ApiExceptionHandler.java` — JSON error mapping for application and Spring MVC errors.
- `src/main/java/com/example/scientificcalculator/api/HealthController.java` — `GET /health`.
- `src/test/java/com/example/scientificcalculator/expression/ExpressionEvaluatorTest.java` — parser/evaluator behavior and limits.
- `src/test/java/com/example/scientificcalculator/calculation/CalculationHistoryTest.java` — history ordering, capacity, snapshot, and concurrency.
- `src/test/java/com/example/scientificcalculator/calculation/CalculatorServiceTest.java` — evaluate-before-store and failure atomicity.
- `src/test/java/com/example/scientificcalculator/calculation/CalculationControllerTest.java` — MockMvc HTTP contract tests.
- `src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java` — real-context POST/list integration test.
- `README.md` — build/start/API/grammar/known-limit documentation.
- `docs/AI_DELIVERY.md` — actual AI prompts/outputs, personal checks, corrections, and evidence.
- `docs/TEST_EVIDENCE.md` — final command outputs, test summary, JAR checksum, and curl evidence.

## Task 1: Bootstrap the Maven application

**Files:**
- Create: `pom.xml`
- Create: `.mvn/wrapper/maven-wrapper.properties`
- Create: `mvnw`
- Create: `mvnw.cmd`
- Create: `src/main/java/com/example/scientificcalculator/ScientificCalculatorApplication.java`
- Create: `src/main/java/com/example/scientificcalculator/api/HealthController.java`
- Test: `src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java`

- [ ] **Step 1: Write the failing context test**

```java
@SpringBootTest
class ScientificCalculatorApplicationTest {
    @Test
    void springContextStarts() {
    }
}
```

- [ ] **Step 2: Run the test and confirm the expected missing-project failure**

Run: `mvn test -Dtest=ScientificCalculatorApplicationTest`

Expected: Maven reports that no `pom.xml` exists.

- [ ] **Step 3: Add the minimal Maven/Spring Boot files**

Use Spring Boot `3.3.13`, `spring-boot-starter-web`, and `spring-boot-starter-test`; set `java.version` to `17`; configure `spring-boot-maven-plugin` without extra runtime dependencies. Add the standard Maven Wrapper pinned to Maven `3.9.9`. Do not add validation or utility dependencies because the small request contract is validated directly.

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

- [ ] **Step 4: Run the context test**

Run: `./mvnw test -Dtest=ScientificCalculatorApplicationTest`

Expected: the test passes and Spring creates the application context.

- [ ] **Step 5: Commit the bootstrap**

```bash
git add pom.xml .mvn mvnw mvnw.cmd src/main src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java
git commit -m "build: bootstrap spring boot calculator service"
```

## Task 2: Implement the expression evaluator test-first

**Files:**
- Create: `src/main/java/com/example/scientificcalculator/expression/ExpressionException.java`
- Create: `src/main/java/com/example/scientificcalculator/expression/ExpressionEvaluator.java`
- Test: `src/test/java/com/example/scientificcalculator/expression/ExpressionEvaluatorTest.java`

- [ ] **Step 1: Write the first failing tests**

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

- [ ] **Step 2: Run the tests and confirm they fail because evaluator types do not exist**

Run: `./mvnw test -Dtest=ExpressionEvaluatorTest`

Expected: compilation failure mentioning the missing evaluator and `AngleUnit`.

- [ ] **Step 3: Implement the tokenizer and parser minimally**

Create `AngleUnit` with exactly `RADIAN` and `DEGREE` values. 

Implement a private cursor over the input with these methods: `parseAdditive`, `parseMultiplicative`, `parseUnary`, `parsePower`, and `parsePrimary`. `parsePower` must parse one optional `^` followed by `parseUnary` so powers are right associative and negative exponents work. Tokenize decimal/scientific numbers explicitly before calling `Double.parseDouble`; accept only ASCII operators, parentheses, commas only for rejection, and lowercase identifiers. Reject trailing input after the root expression.

The public evaluator contract is exactly:

```java
public double evaluate(String expression, AngleUnit angleUnit)
```

Count every number, identifier, operator, and parenthesis as one token; whitespace and EOF do not count. Reject the 501st token. Increment nesting depth for each parenthesized expression and function call, reject depth 101 before recursing, and always decrement in `finally`.

Use `ExpressionException` codes for `EXPRESSION_SYNTAX_ERROR`, `EXPRESSION_LIMIT_EXCEEDED`, `DIVISION_BY_ZERO`, `DOMAIN_ERROR`, and `NON_FINITE_RESULT`.

- [ ] **Step 4: Add the complete evaluator behavior tests**

Add this parameterized success matrix and assert exception error codes for failures:

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

Add individual tests for `1/0`, `1%0` (`DIVISION_BY_ZERO`), `sqrt(-1)`, `ln(0)` (`DOMAIN_ERROR`), `exp(1000)` (`NON_FINITE_RESULT`), expression length 1001, more than 500 tokens, and more than 100 nested parentheses (`EXPRESSION_LIMIT_EXCEEDED`).

- [ ] **Step 5: Run the evaluator suite to verify green**

Run: `./mvnw test -Dtest=ExpressionEvaluatorTest`

Expected: all evaluator tests pass with no warnings or errors.

- [ ] **Step 6: Commit the evaluator**

```bash
git add src/main/java/com/example/scientificcalculator/expression src/test/java/com/example/scientificcalculator/expression src/main/java/com/example/scientificcalculator/calculation/AngleUnit.java
git commit -m "feat: add safe scientific expression evaluator"
```

## Task 3: Add bounded in-memory history

**Files:**
- Create: `src/main/java/com/example/scientificcalculator/calculation/CalculationRecord.java`
- Create: `src/main/java/com/example/scientificcalculator/calculation/CalculationHistory.java`
- Test: `src/test/java/com/example/scientificcalculator/calculation/CalculationHistoryTest.java`

- [ ] **Step 1: Write failing history tests**

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

- [ ] **Step 2: Run the history tests and confirm they fail**

Run: `./mvnw test -Dtest=CalculationHistoryTest`

Expected: compilation failure because record/store types are missing.

- [ ] **Step 3: Implement the minimal thread-safe store**

Use a `long nextId`, an instance `capacity`, and a `Deque<CalculationRecord>` guarded by the same lock. Allocate `++nextId`, create the immutable record with `Instant.now()`, add it to the front, and evict from the tail while `size() > capacity` inside one synchronized section so ID order and insertion order cannot diverge. `list(limit)` returns a new immutable list; `find(id)` returns an `Optional`; `clear()` empties the deque without resetting the counter. Reject capacities below 1 in the constructor, and construct the production application bean with capacity 1000.

The store contracts are exactly:

```java
public CalculationRecord add(String expression, AngleUnit angleUnit, double result)
public List<CalculationRecord> list(int limit)
public Optional<CalculationRecord> find(long id)
public void clear()
```

- [ ] **Step 4: Add ordering, limit, find, clear, snapshot, and concurrency assertions**

Verify missing IDs return empty, clear leaves the next ID greater than all previous IDs, and the returned list rejects mutation without changing the store. HTTP `limit` validation belongs only to Task 4.

- [ ] **Step 5: Run the history suite**

Run: `./mvnw test -Dtest=CalculationHistoryTest`

Expected: all history tests pass.

- [ ] **Step 6: Commit the history store**

```bash
git add src/main/java/com/example/scientificcalculator/calculation src/test/java/com/example/scientificcalculator/calculation/CalculationHistoryTest.java
git commit -m "feat: add bounded in-memory calculation history"
```

## Task 4: Wire service, DTOs, controller, and error responses

**Files:**
- Create: `src/main/java/com/example/scientificcalculator/calculation/CalculationRequest.java`
- Create: `src/main/java/com/example/scientificcalculator/calculation/CalculatorService.java`
- Create: `src/main/java/com/example/scientificcalculator/calculation/HistoryNotFoundException.java`
- Create: `src/main/java/com/example/scientificcalculator/calculation/CalculationController.java`
- Create: `src/main/java/com/example/scientificcalculator/api/ApiExceptionHandler.java`
- Modify: `src/main/java/com/example/scientificcalculator/ScientificCalculatorApplication.java` to expose `CalculationHistory(1000)` and `ExpressionEvaluator` as explicit Spring beans.
- Test: `src/test/java/com/example/scientificcalculator/calculation/CalculationControllerTest.java`
- Test: `src/test/java/com/example/scientificcalculator/calculation/CalculatorServiceTest.java`

- [ ] **Step 1: Write failing MockMvc contract tests**

First write a focused service failure-atomicity test:

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

Then write the HTTP contract test:

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

- [ ] **Step 2: Run the web test and confirm it fails**

Run: `./mvnw test -Dtest=CalculationControllerTest`

Expected: compilation or request-routing failure because controller/service/DTOs do not exist.

- [ ] **Step 3: Implement request/result flow**

Make `CalculationRequest` a record with `String expression` and nullable `String angleUnit`. Normalize a missing or blank unit to `RADIAN`; parse other values with `AngleUnit.valueOf` and map invalid values to `400 INVALID_ARGUMENT`. Validate nonblank expression and max length 1000 at the HTTP boundary. Validate history `limit` only in the controller as 1–100; the store remains an internal component receiving already-valid values. `CalculatorService.calculate` evaluates first and adds history only after success. Return `200 OK` for calculation, list, single-record, health, and clear-history; clear returns `{"cleared":true}`.

Use these exact service contracts:

```java
public CalculationRecord calculate(String expression, AngleUnit angleUnit)
public List<CalculationRecord> history(int limit)
public CalculationRecord history(long id)
public void clearHistory()
```

`history(long)` throws `HistoryNotFoundException(id)` when no record exists.

Add these beans after the evaluator and history classes exist:

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

- [ ] **Step 4: Implement the global error mapper**

Map evaluator/domain errors to their documented status/code pairs and history misses to `404 HISTORY_NOT_FOUND`. Add explicit handlers for `MethodArgumentTypeMismatchException` (`400 INVALID_ARGUMENT`), `HttpMessageNotReadableException` (`400 INVALID_ARGUMENT`), and `HttpMediaTypeNotSupportedException` (`415 UNSUPPORTED_MEDIA_TYPE`). Add a final `Exception` handler returning `500 INTERNAL_ERROR` without exception class names or stack traces. Every error includes `code`, `message`, `path`, and `timestamp`.

- [ ] **Step 5: Expand MockMvc tests**

Cover DEGREE calculation, history ordering and limit, single-record success/404, clear response and empty follow-up list, blank expression (`INVALID_ARGUMENT`), malformed JSON, invalid angle unit, nonnumeric/invalid `limit`, evaluator `422` errors, invalid media type, generic `500` without stack details, stable error shape, and failed calculations not appearing in history.

- [ ] **Step 6: Run the controller suite**

Run: `./mvnw test -Dtest=CalculationControllerTest`

Expected: all HTTP contract tests pass.

- [ ] **Step 7: Commit the HTTP layer**

```bash
git add src/main/java/com/example/scientificcalculator/calculation src/main/java/com/example/scientificcalculator/api src/test/java/com/example/scientificcalculator/calculation/CalculationControllerTest.java
git commit -m "feat: expose calculator and history HTTP APIs"
```

## Task 5: Documentation and runnable-jar verification

**Files:**
- Create: `README.md`
- Create: `docs/AI_DELIVERY.md`
- Create: `docs/TEST_EVIDENCE.md`
- Modify: `src/test/java/com/example/scientificcalculator/ScientificCalculatorApplicationTest.java` to call `CalculatorService` through the real Spring context and assert one calculation is stored.

- [ ] **Step 1: Add documentation from the frozen requirements**

`README.md` must document Java 17 prerequisites, `./mvnw clean verify`, `java -jar`, all endpoints, request/response examples, grammar, function whitelist, angle units, error codes, memory-only restart behavior, and the fixed 1000-record capacity.

`docs/AI_DELIVERY.md` must contain a table with the actual prompts used in this session, AI output summary, personal verification, personal corrections, and evidence paths/commands for requirements, design, coding, testing, and packaging.

`docs/TEST_EVIDENCE.md` must record the real Java/Maven versions, test summary, JAR path, SHA-256, startup output, and curl responses after the verification commands run; do not invent results before execution.

- [ ] **Step 2: Upgrade the context test to a real application integration test**

Annotate `ScientificCalculatorApplicationTest` with `@SpringBootTest` and `@AutoConfigureMockMvc`. POST `{"expression":"1+2"}` to the real application context, assert `200` and result `3.0`, then GET history and assert the record is present. This proves Spring bean wiring and the controller→service→evaluator→history path.

- [ ] **Step 3: Run the complete verification command**

Run: `./mvnw clean verify`

Expected: exit code 0, all tests pass, and `target/scientific-calculator-*.jar` exists.

- [ ] **Step 4: Verify the JAR independently**

Run the JAR in a separate process:

```bash
java -jar target/scientific-calculator-*.jar
```

From another shell, run:

```bash
curl -i http://localhost:8080/health
curl -i -X POST http://localhost:8080/api/v1/calculations -H 'Content-Type: application/json' -d '{"expression":"sin(90)+sqrt(16)","angleUnit":"DEGREE"}'
curl -i 'http://localhost:8080/api/v1/calculations?limit=10'
curl -i -X POST http://localhost:8080/api/v1/calculations -H 'Content-Type: application/json' -d '{"expression":"1/0"}'
curl -i -X DELETE http://localhost:8080/api/v1/calculations
```

Expected: health/calculation/list/clear return `200`; calculation result is `5.0`; division by zero returns `422`; clear returns `{"cleared":true}`.

- [ ] **Step 5: Record evidence and checksum**

Run `shasum -a 256 target/scientific-calculator-*.jar` and copy the actual output, test summary, and HTTP responses into `docs/TEST_EVIDENCE.md`. Do not claim a result until the command output has been checked.

- [ ] **Step 6: Commit documentation and evidence**

```bash
git add README.md docs/AI_DELIVERY.md docs/TEST_EVIDENCE.md src/test
git commit -m "docs: add delivery and verification evidence"
```

## Self-review checklist

- [ ] Every frozen requirement has an implementation or verification task.
- [ ] No task introduces a database, cache, external call, script engine, or dynamic execution path.
- [ ] All successful HTTP endpoints use `200 OK`; clear-history returns `{"cleared":true}`.
- [ ] Exactly eight single-argument functions are implemented.
- [ ] History capacity is fixed at 1000 and IDs remain monotonic across clear.
- [ ] Parser tests protect right-associative power and unary-sign semantics.
- [ ] Error codes and status mappings match `REQUIREMENTS.md` and the design spec.
- [ ] The final claim is based on fresh `./mvnw clean verify`, JAR startup, curl, and checksum evidence.
