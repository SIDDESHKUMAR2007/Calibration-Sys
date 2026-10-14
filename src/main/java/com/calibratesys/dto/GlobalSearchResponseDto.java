package com.calibratesys.dto;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GlobalSearchResponseDto {

    private String query;
    private int totalMatches;
    private Map<String, Integer> countsByEntityType;
    private List<GlobalSearchResultItemDto> results;

    private List<GlobalSearchResultItemDto> equipment;
    private List<GlobalSearchResultItemDto> laboratories;
    private List<GlobalSearchResultItemDto> staff;
    private List<GlobalSearchResultItemDto> calibrationSchedules;
    private List<GlobalSearchResultItemDto> calibrationRecords;
    private List<GlobalSearchResultItemDto> calibrationDocuments;

    public GlobalSearchResponseDto() {
        this.results = Collections.emptyList();
        this.countsByEntityType = Collections.emptyMap();
        this.equipment = Collections.emptyList();
        this.laboratories = Collections.emptyList();
        this.staff = Collections.emptyList();
        this.calibrationSchedules = Collections.emptyList();
        this.calibrationRecords = Collections.emptyList();
        this.calibrationDocuments = Collections.emptyList();
    }

    public GlobalSearchResponseDto(String query, int totalMatches,
                                   Map<String, Integer> countsByEntityType,
                                   List<GlobalSearchResultItemDto> results) {
        this.query = query;
        this.totalMatches = totalMatches;
        this.countsByEntityType = countsByEntityType != null ? countsByEntityType : Collections.emptyMap();
        this.results = results != null ? results : Collections.emptyList();
        this.equipment = Collections.emptyList();
        this.laboratories = Collections.emptyList();
        this.staff = Collections.emptyList();
        this.calibrationSchedules = Collections.emptyList();
        this.calibrationRecords = Collections.emptyList();
        this.calibrationDocuments = Collections.emptyList();
    }

    public GlobalSearchResponseDto(String query,
                                   int totalMatches,
                                   Map<String, Integer> countsByEntityType,
                                   List<GlobalSearchResultItemDto> results,
                                   List<GlobalSearchResultItemDto> equipment,
                                   List<GlobalSearchResultItemDto> laboratories,
                                   List<GlobalSearchResultItemDto> staff,
                                   List<GlobalSearchResultItemDto> calibrationSchedules,
                                   List<GlobalSearchResultItemDto> calibrationRecords,
                                   List<GlobalSearchResultItemDto> calibrationDocuments) {
        this.query = query;
        this.totalMatches = totalMatches;
        this.countsByEntityType = countsByEntityType != null ? countsByEntityType : Collections.emptyMap();
        this.results = results != null ? results : Collections.emptyList();
        this.equipment = equipment != null ? equipment : Collections.emptyList();
        this.laboratories = laboratories != null ? laboratories : Collections.emptyList();
        this.staff = staff != null ? staff : Collections.emptyList();
        this.calibrationSchedules = calibrationSchedules != null ? calibrationSchedules : Collections.emptyList();
        this.calibrationRecords = calibrationRecords != null ? calibrationRecords : Collections.emptyList();
        this.calibrationDocuments = calibrationDocuments != null ? calibrationDocuments : Collections.emptyList();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public Map<String, Integer> getCountsByEntityType() {
        return countsByEntityType;
    }

    public void setCountsByEntityType(Map<String, Integer> countsByEntityType) {
        this.countsByEntityType = countsByEntityType;
    }

    public List<GlobalSearchResultItemDto> getResults() {
        return results;
    }

    public void setResults(List<GlobalSearchResultItemDto> results) {
        this.results = results;
    }

    public List<GlobalSearchResultItemDto> getEquipment() {
        return equipment;
    }

    public void setEquipment(List<GlobalSearchResultItemDto> equipment) {
        this.equipment = equipment;
    }

    public List<GlobalSearchResultItemDto> getLaboratories() {
        return laboratories;
    }

    public void setLaboratories(List<GlobalSearchResultItemDto> laboratories) {
        this.laboratories = laboratories;
    }

    public List<GlobalSearchResultItemDto> getStaff() {
        return staff;
    }

    public void setStaff(List<GlobalSearchResultItemDto> staff) {
        this.staff = staff;
    }

    public List<GlobalSearchResultItemDto> getCalibrationSchedules() {
        return calibrationSchedules;
    }

    public void setCalibrationSchedules(List<GlobalSearchResultItemDto> calibrationSchedules) {
        this.calibrationSchedules = calibrationSchedules;
    }

    public List<GlobalSearchResultItemDto> getCalibrationRecords() {
        return calibrationRecords;
    }

    public void setCalibrationRecords(List<GlobalSearchResultItemDto> calibrationRecords) {
        this.calibrationRecords = calibrationRecords;
    }

    public List<GlobalSearchResultItemDto> getCalibrationDocuments() {
        return calibrationDocuments;
    }

    public void setCalibrationDocuments(List<GlobalSearchResultItemDto> calibrationDocuments) {
        this.calibrationDocuments = calibrationDocuments;
    }
}
