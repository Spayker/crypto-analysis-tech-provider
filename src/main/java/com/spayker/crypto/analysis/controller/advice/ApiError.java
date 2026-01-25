package com.spayker.crypto.analysis.controller.advice;

import java.util.Map;

public record ApiError(
        String code,
        String message,
        Map<String, String> details
) {}