package com.calibratesys.util;

import com.calibratesys.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Utility for validating pagination parameters across all API endpoints.
 * Enforces:
 *   - page >= 0
 *   - size >= 1
 *   - size <= MAX_PAGE_SIZE (100)
 */
public final class PaginationValidator {

    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_SIZE = 20;

    private PaginationValidator() {
    }

    /**
     * Validates page index and page size parameters.
     * Throws ApiException (400 Bad Request) if any constraint is violated.
     */
    public static void validate(int page, int size) {
        if (page < 0) {
            throw new ApiException("Page index must not be less than zero", HttpStatus.BAD_REQUEST);
        }
        if (size < 1) {
            throw new ApiException("Page size must not be less than one", HttpStatus.BAD_REQUEST);
        }
        if (size > MAX_PAGE_SIZE) {
            throw new ApiException("Page size cannot exceed " + MAX_PAGE_SIZE + " items per page", HttpStatus.BAD_REQUEST);
        }
    }
}
