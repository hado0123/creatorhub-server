package com.creatorhub.dto;

import java.util.List;
import java.util.Map;

public record DerivativesCheckResponse(
        boolean ready,
        Map<String, Long> derivedSizeByKey,
        List<String> missingKeys
) {
    public static DerivativesCheckResponse notReady(Map<String, Long> derivedSizeByKey, List<String> missingKeys) {
        return new DerivativesCheckResponse(
                false,
                derivedSizeByKey,
                missingKeys
        );
    }
    public static DerivativesCheckResponse ready(Map<String, Long> derivedSizeByKey) {
        return new DerivativesCheckResponse(
                true,
                derivedSizeByKey,
                null
        );
    }
}