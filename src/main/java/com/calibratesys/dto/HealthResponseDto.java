package com.calibratesys.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object representing system health and connectivity status.
 */
public class HealthResponseDto {

    private String status;
    private String application;
    private String version;
    private LocalDateTime timestamp;
    private String databaseStatus;
    private Map<String, Object> details;

    public HealthResponseDto() {
    }

    public HealthResponseDto(String status, String application, String version,
                             LocalDateTime timestamp, String databaseStatus, Map<String, Object> details) {
        this.status = status;
        this.application = application;
        this.version = version;
        this.timestamp = timestamp;
        this.databaseStatus = databaseStatus;
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
