package com.calibratesys.service;

import com.calibratesys.dto.GlobalSearchResponseDto;

public interface SearchService {

    /**
     * Unified cross-entity global search across equipment, schedules, records,
     * documents, laboratories, and staff, respecting active laboratory data scoping.
     *
     * @param query Search query string
     * @return GlobalSearchResponseDto containing categorized matches
     */
    GlobalSearchResponseDto searchGlobal(String query);
}
