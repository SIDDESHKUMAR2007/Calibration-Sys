package com.calibratesys.controller;

import com.calibratesys.dto.GlobalSearchResponseDto;
import com.calibratesys.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for Unified Global Search operations across the system.
 * Accessible to all authenticated users (ADMIN, LAB_STAFF) with active data scoping.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Unified cross-entity search endpoint.
     * Examples:
     *   GET /api/search?q=microscope
     *   GET /api/search?q=AST-2024
     */
    @GetMapping
    public ResponseEntity<GlobalSearchResponseDto> searchGlobal(@RequestParam(name = "q", required = false) String query) {
        GlobalSearchResponseDto response = searchService.searchGlobal(query);
        return ResponseEntity.ok(response);
    }
}
