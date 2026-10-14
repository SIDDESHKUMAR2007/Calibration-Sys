package com.calibratesys.dto;

import java.time.LocalDateTime;

/**
 * DTO for returning College representations without exposing JPA entity internals.
 */
public class CollegeResponseDto {

    private Long id;
    private String code;
    private String name;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private int totalLaboratories;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CollegeResponseDto() {
    }

    public CollegeResponseDto(Long id, String code, String name, String address,
                              String contactEmail, String contactPhone, int totalLaboratories,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.address = address;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.totalLaboratories = totalLaboratories;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public int getTotalLaboratories() {
        return totalLaboratories;
    }

    public void setTotalLaboratories(int totalLaboratories) {
        this.totalLaboratories = totalLaboratories;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
