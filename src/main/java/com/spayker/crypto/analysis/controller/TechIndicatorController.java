package com.spayker.crypto.analysis.controller;

import com.spayker.crypto.analysis.dto.indicator.IndicatorMetaData;
import com.spayker.crypto.analysis.dto.indicator.IndicatorRequest;
import com.spayker.crypto.analysis.dto.indicator.IndicatorResponse;
import com.spayker.crypto.analysis.dto.indicator.TimeFrame;
import com.spayker.crypto.analysis.service.TechIndicatorManager;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("v1")
@RequiredArgsConstructor
public class TechIndicatorController {

    private final TechIndicatorManager techIndicatorManager;

    @GetMapping("/tech/indicators")
    public ResponseEntity<List<IndicatorMetaData>> fetchAvailableIndicators() {
        log.debug("Received fetch all available indications request");
        return ResponseEntity.ok(techIndicatorManager.getAvailableIndications());
    }

    @GetMapping("/{symbol}/tech/indicators/{indicatorName}")
    public ResponseEntity<IndicatorResponse> fetchIndicatorDataByName(
            @PathVariable @Pattern(regexp = "[a-zA-Z]{1,10}") String symbol,
            @PathVariable String indicatorName,
            @RequestParam String timeFrame) {
        log.debug("Received fetch tech {} indication request for {}", indicatorName, symbol);
        IndicatorRequest request = new IndicatorRequest(indicatorName,
                symbol.toLowerCase(),
                TimeFrame.valueOf(timeFrame.toUpperCase()));
        return ResponseEntity.ok(techIndicatorManager.getTechIndicatorData(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{symbol}/tech/indicators/{indicatorName}")
    public ResponseEntity<Void> addIndicatorBySymbol(@PathVariable @Pattern(regexp = "[a-zA-Z]{1,10}") String symbol,
                                                    @PathVariable String indicatorName,
                                                    @RequestParam String timeFrame) {
        log.info("Received add new tech {} indication request for {}", indicatorName, symbol);
        techIndicatorManager.addIndicatorBySymbol(IndicatorRequest.builder()
                .symbol(symbol.toLowerCase())
                .name(indicatorName)
                .timeFrame(TimeFrame.valueOf(timeFrame.toUpperCase()))
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}