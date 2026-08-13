package com.example.scientificcalculator.calculation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.scientificcalculator.api.ApiExceptionHandler;
import com.example.scientificcalculator.expression.ExpressionEvaluator;

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

    @Test
    void calculatesTrigonometricFunctionInDegrees() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"sin(90)\",\"angleUnit\":\"DEGREE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(1.0))
                .andExpect(jsonPath("$.angleUnit").value("DEGREE"));
    }

    @Test
    void listsNewestHistoryWithLimit() throws Exception {
        calculate("1+1");
        calculate("2+2");
        calculate("3+3");

        mvc.perform(get("/api/v1/calculations").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].expression").value("3+3"))
                .andExpect(jsonPath("$[1].expression").value("2+2"));
    }

    @Test
    void getsOneHistoryRecordAndReturnsNotFoundForMissingId() throws Exception {
        CalculationRecord record = history.add("1+2", AngleUnit.RADIAN, 3.0);

        mvc.perform(get("/api/v1/calculations/{id}", record.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expression").value("1+2"));

        mvc.perform(get("/api/v1/calculations/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HISTORY_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/v1/calculations/999"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void clearsHistoryWithOkResponse() throws Exception {
        calculate("1+1");

        mvc.perform(delete("/api/v1/calculations"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"cleared\":true}"));

        mvc.perform(get("/api/v1/calculations"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void rejectsBlankExpressionAndInvalidAngleUnit() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"1+1\",\"angleUnit\":\"degree\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void rejectsMalformedJsonAndUnsupportedMediaTypeAsJson() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("1+1"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void rejectsNullJsonRequest() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void rejectsInvalidLimitValues() throws Exception {
        for (String limit : new String[] {"0", "101", "abc"}) {
            mvc.perform(get("/api/v1/calculations").param("limit", limit))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        }
    }

    @Test
    void mapsCalculationErrorsAndDoesNotStoreFailures() throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"sqrt(-1)\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("DOMAIN_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/v1/calculations"))
                .andExpect(jsonPath("$.timestamp").exists());

        mvc.perform(get("/api/v1/calculations"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void genericErrorDoesNotExposeExceptionDetails() throws Exception {
        MockMvc failingMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        failingMvc.perform(get("/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("RuntimeException"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret detail"))));
    }

    private void calculate(String expression) throws Exception {
        mvc.perform(post("/api/v1/calculations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"" + expression + "\"}"))
                .andExpect(status().isOk());
    }

    @RestController
    static class FailingController {
        @GetMapping("/failure")
        void fail() {
            throw new RuntimeException("secret detail");
        }
    }
}
