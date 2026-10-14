package com.calibratesys.dto;

/**
 * DTO returned upon successful authentication.
 * Omits any sensitive password hashes.
 */
public class LoginResponseDto {

    private String token;
    private String tokenType;
    private long expiresInMs;
    private String staffId;
    private String fullName;
    private String email;
    private String role;

    public LoginResponseDto() {
        this.tokenType = "Bearer";
    }

    public LoginResponseDto(String token, long expiresInMs, String staffId,
                            String fullName, String email, String role) {
        this.token = token;
        this.tokenType = "Bearer";
        this.expiresInMs = expiresInMs;
        this.staffId = staffId;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public void setExpiresInMs(long expiresInMs) {
        this.expiresInMs = expiresInMs;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
