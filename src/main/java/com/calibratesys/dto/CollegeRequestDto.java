package com.calibratesys.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating or updating a College.
 */
public class CollegeRequestDto {

    @NotBlank(message = "College code is required")
    @Size(min = 2, max = 50, message = "College code must be between 2 and 50 characters")
    private String code;

    @NotBlank(message = "College name is required")
    @Size(min = 2, max = 150, message = "College name must be between 2 and 150 characters")
    private String name;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Email(message = "Contact email must be a valid email address")
    @Size(max = 100, message = "Contact email cannot exceed 100 characters")
    private String contactEmail;

    @Pattern(regexp = "^$|^[+0-9\\s-]{7,25}$", message = "Contact phone must be a valid phone number (7-25 digits, spaces, hyphens, or leading +)")
    @Size(max = 25, message = "Contact phone cannot exceed 25 characters")
    private String contactPhone;

    public CollegeRequestDto() {
    }

    public CollegeRequestDto(String code, String name, String address, String contactEmail, String contactPhone) {
        this.code = code;
        this.name = name;
        this.address = address;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
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
}
