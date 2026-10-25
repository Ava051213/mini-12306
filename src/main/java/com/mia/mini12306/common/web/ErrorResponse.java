package com.mia.mini12306.common.web;

// Share one response contract across all API errors.
public record ErrorResponse(String message) {
}
