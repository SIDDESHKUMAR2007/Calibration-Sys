package com.calibratesys.util;

import com.calibratesys.exception.ApiException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Utility for whitelisting and validating dynamic sorting parameters.
 */
public final class SortValidator {

    private SortValidator() {
    }

    public static Sort validateAndBuildSort(String sortBy, String sortDirection,
                                            String defaultSortKey, Sort.Direction defaultDirection,
                                            Map<String, String> allowedFieldsMap) {
        String key = sortBy;
        if (key == null || key.trim().isEmpty()) {
            key = defaultSortKey;
        } else {
            key = key.trim();
            if (!allowedFieldsMap.containsKey(key)) {
                throw new ApiException("Invalid sort field: '" + key + "'. Allowed sort fields are: " +
                        String.join(", ", allowedFieldsMap.keySet()), HttpStatus.BAD_REQUEST);
            }
        }

        Sort.Direction direction = defaultDirection;
        if (sortDirection != null && !sortDirection.trim().isEmpty()) {
            String dir = sortDirection.trim().toUpperCase();
            if (!"ASC".equals(dir) && !"DESC".equals(dir)) {
                throw new ApiException("Invalid sort direction: '" + sortDirection + "'. Allowed values are: ASC, DESC", HttpStatus.BAD_REQUEST);
            }
            direction = Sort.Direction.fromString(dir);
        }

        String entityProperty = allowedFieldsMap.get(key);
        return Sort.by(direction, entityProperty);
    }
}
