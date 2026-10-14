package com.calibratesys.dto;

public class GlobalSearchResultItemDto {

    private String entityType;
    private Long entityId;
    private String title;
    private String subtitle;
    private String laboratory;
    private String status;

    private Long id;
    private String targetUrl;
    private Long laboratoryId;
    private String laboratoryName;

    public GlobalSearchResultItemDto() {
    }

    public GlobalSearchResultItemDto(String entityType, Long entityId, String title,
                                     String subtitle, String laboratory, String status) {
        this.entityType = entityType;
        this.entityId = entityId;
        this.id = entityId;
        this.title = title;
        this.subtitle = subtitle;
        this.laboratory = laboratory;
        this.laboratoryName = laboratory;
        this.status = status;
    }

    public Long getId() {
        return id != null ? id : entityId;
    }

    public void setId(Long id) {
        this.id = id;
        this.entityId = id;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Long getLaboratoryId() {
        return laboratoryId;
    }

    public void setLaboratoryId(Long laboratoryId) {
        this.laboratoryId = laboratoryId;
    }

    public String getLaboratoryName() {
        return laboratoryName != null ? laboratoryName : laboratory;
    }

    public void setLaboratoryName(String laboratoryName) {
        this.laboratoryName = laboratoryName;
        this.laboratory = laboratoryName;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getLaboratory() {
        return laboratory;
    }

    public void setLaboratory(String laboratory) {
        this.laboratory = laboratory;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
