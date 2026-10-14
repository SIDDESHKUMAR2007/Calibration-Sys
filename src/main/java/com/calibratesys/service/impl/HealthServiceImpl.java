package com.calibratesys.service.impl;

import com.calibratesys.dto.HealthResponseDto;
import com.calibratesys.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementation of HealthService verifying system state and MySQL connectivity.
 */
@Service
public class HealthServiceImpl implements HealthService {

    private final DataSource dataSource;

    @Value("${spring.application.name:calibrate-sys}")
    private String applicationName;

    @Autowired
    public HealthServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public HealthResponseDto checkHealth() {
        String dbStatus = "UNKNOWN";
        Map<String, Object> details = new LinkedHashMap<>();

        details.put("environment", "local/development");
        details.put("javaVersion", System.getProperty("java.version"));
        details.put("osName", System.getProperty("os.name"));

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(2);
            if (valid) {
                dbStatus = "CONNECTED";
                DatabaseMetaData metaData = connection.getMetaData();
                details.put("databaseProduct", metaData.getDatabaseProductName());
                details.put("databaseVersion", metaData.getDatabaseProductVersion());
                String rawUrl = metaData.getURL();
                String sanitizedUrl = rawUrl != null ? rawUrl.split("\\?")[0] : "configured";
                details.put("databaseUrl", sanitizedUrl);
            } else {
                dbStatus = "DISCONNECTED";
                details.put("databaseError", "Connection validation returned false");
            }
        } catch (Exception ex) {
            dbStatus = "DISCONNECTED";
            details.put("databaseError", "Database connectivity unavailable");
        }

        String overallStatus = "CONNECTED".equalsIgnoreCase(dbStatus) ? "UP" : "DEGRADED";

        return new HealthResponseDto(
                overallStatus,
                "Smart Laboratory Equipment Calibration and Compliance Management System (" + applicationName + ")",
                "1.0.0-SNAPSHOT",
                LocalDateTime.now(),
                dbStatus,
                details
        );
    }
}
