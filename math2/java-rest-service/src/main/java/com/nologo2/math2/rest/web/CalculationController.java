package com.nologo2.math2.rest.web;

import com.nologo2.math2.Number;
import com.nologo2.math2.rest.config.Math2Properties;
import com.nologo2.math2.rest.service.CalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calculate")
@Tag(name = "Calculation")
public class CalculationController {
    private final CalculationService service;
    private final Math2Properties properties;

    public CalculationController(CalculationService service, Math2Properties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping
    @Operation(summary = "Calculate one equation")
    public CalculationResponse calculate(
            @RequestParam("equation") String equation,
            @RequestParam(name = "precision", required = false) Integer precision) {
        return new CalculationResponse(service.calculate(equation, precision(precision)).toString());
    }

    @GetMapping("/batch")
    @Operation(summary = "Calculate a comma-separated batch of equations")
    public BatchCalculationResponse calculateBatch(
            @RequestParam("equations") String equations,
            @RequestParam(name = "precision", required = false) Integer precision) {
        List<String> parsed = EquationBatchParser.split(equations);
        if (parsed.size() > properties.getRequest().getMaxBatchSize()) {
            throw new IllegalArgumentException("Batch exceeds configured maximum size");
        }
        int calculationPrecision = precision(precision);
        return new BatchCalculationResponse(parsed.stream()
                .map(value -> service.calculate(value, calculationPrecision).toString())
                .toList());
    }

    private static int precision(Integer requested) {
        return requested == null ? Number.getMaxLength() : requested;
    }
}
