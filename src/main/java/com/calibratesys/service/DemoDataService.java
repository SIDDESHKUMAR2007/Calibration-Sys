package com.calibratesys.service;

import java.util.Map;

/**
 * Service for initializing and resetting demonstration data for academic and evaluation purposes.
 * Controlled by configuration property app.demo-data.enabled.
 */
public interface DemoDataService {

    /**
     * Resets the entire application data to a small, clean, college-demonstration-friendly dataset.
     *
     * @return Map containing post-reset record counts for verification.
     */
    Map<String, Long> resetDemoData();

    /**
     * Returns current record counts across all core entities.
     *
     * @return Map of entity names to record counts.
     */
    Map<String, Long> getEntityCounts();
}
