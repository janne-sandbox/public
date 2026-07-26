package com.nologo.math2.rest.web;

import com.nologo.math2.Number;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes and configures the default rounding mode used by the {@code round()} function when
 * no explicit mode code is supplied in the expression.
 *
 * <p>Mode codes: 0&nbsp;HALF_EVEN (default), 1&nbsp;HALF_UP, 2&nbsp;HALF_DOWN,
 * 3&nbsp;CEILING, 4&nbsp;FLOOR, 5&nbsp;UP, 6&nbsp;DOWN.</p>
 */
@RestController
@RequestMapping("/api/v1/rounding")
@Tag(name = "Rounding")
public class RoundingController {

    @GetMapping
    @Operation(summary = "Return the current default rounding mode")
    public RoundingResponse getDefaultRoundingMode() {
        int code = Number.getDefaultRoundingModeCode();
        return new RoundingResponse(code, Number.getRoundingModeName(code));
    }

    @PostMapping("/{code}")
    @Operation(summary = "Set the default rounding mode by code (0–6)")
    public RoundingResponse setDefaultRoundingMode(@PathVariable("code") int code) {
        Number.setDefaultRoundingModeCode(code); // throws IllegalArgumentException → 400 via ApiExceptionHandler
        return new RoundingResponse(code, Number.getRoundingModeName(code));
    }
}
