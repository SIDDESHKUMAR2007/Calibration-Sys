package com.calibratesys.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating an existing Staff member.
 * Password field is optional; if provided and non-blank, it will be hashed and updated.
 */
public class StaffUpdateRequestDto {

    @NotBlank(message = "Staff ID is required")
    @Size(min = 2, max = 50, message = "Staff ID must be between 2 and 50 characters")
    private String staffId;

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 25, message = "Phone number cannot exceed 25 characters")
    private String phoneNumber;

    @NotBlank(message = "Designation is required")
    @Size(min = 2, max = 100, message = "Designation must be between 2 and 100 characters")
    private String designation;

    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be between 2 and 100 characters")
    private String department;

    private String systemRole;

    private String status;

    private String password;

    public StaffUpdateRequestDto() {
    }

    public StaffUpdateRequestDto(String staffId, String firstName, String lastName, String email,
                                 String phoneNumber, String designation, String department,
                                 String systemRole, String status, String password) {
        this.staffId = staffId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.designation = designation;
        this.department = department;
        this.systemRole = systemRole;
        this.status = status;
        this.password = password;
    }

    public String getStaffId() {
        return staffId;
    }

    public void setStaffId(String staffId) {
        this.staffId = staffId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSystemRole() {
        return systemRole;
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
