package com.calibratesys.dto;

public class MarkAllReadResponseDto {

    private int updatedCount;
    private String message;

    public MarkAllReadResponseDto() {
    }

    public MarkAllReadResponseDto(int updatedCount, String message) {
        this.updatedCount = updatedCount;
        this.message = message;
    }

    public int getUpdatedCount() {
        return updatedCount;
    }

    public void setUpdatedCount(int updatedCount) {
        this.updatedCount = updatedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
