package com.campus.freshman.common;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(String code, String message, Map<String, String> details, LocalDateTime timestamp) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Map.of(), LocalDateTime.now());
    }
}
