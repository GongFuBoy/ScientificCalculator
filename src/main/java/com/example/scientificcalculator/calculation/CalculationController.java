package com.example.scientificcalculator.calculation;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.scientificcalculator.expression.ExpressionException;

@RestController
@RequestMapping("/api/v1/calculations")
public class CalculationController {
    private final CalculatorService service;

    public CalculationController(CalculatorService service) {
        this.service = service;
    }

    @PostMapping
    public CalculationRecord calculate(@RequestBody CalculationRequest request) {
        if (request.expression() == null || request.expression().isBlank()) {
            throw invalid("Expression must not be blank");
        }
        if (request.expression().length() > 1000) {
            throw new ExpressionException("EXPRESSION_LIMIT_EXCEEDED", "Expression exceeds 1000 characters");
        }
        return service.calculate(request.expression(), parseAngleUnit(request.angleUnit()));
    }

    @GetMapping
    public List<CalculationRecord> history(@RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > 100) {
            throw invalid("Limit must be between 1 and 100");
        }
        return service.history(limit);
    }

    @GetMapping("/{id}")
    public CalculationRecord history(@PathVariable long id) {
        return service.history(id);
    }

    @DeleteMapping
    public Map<String, Boolean> clearHistory() {
        service.clearHistory();
        return Map.of("cleared", true);
    }

    private AngleUnit parseAngleUnit(String value) {
        if (value == null || value.isBlank()) {
            return AngleUnit.RADIAN;
        }
        try {
            return AngleUnit.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw invalid("Angle unit must be RADIAN or DEGREE");
        }
    }

    private ExpressionException invalid(String message) {
        return new ExpressionException("INVALID_ARGUMENT", message);
    }
}
