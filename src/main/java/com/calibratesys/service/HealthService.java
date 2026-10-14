package com.calibratesys.service;

import com.calibratesys.dto.HealthResponseDto;

/**
 * Service interface for application and database health checks.
 */
public interface HealthService {

    /**
     * Checks the operational status of the application and its MySQL database connection.
     *
     * @return HealthResponseDto containing the system status and diagnostic details.
     */
    HealthResponseDto checkHealth();
}
