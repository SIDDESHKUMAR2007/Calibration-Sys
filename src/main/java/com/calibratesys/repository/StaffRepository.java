package com.calibratesys.repository;

import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByEmail(String email);

    Optional<Staff> findByStaffId(String staffId);

    Optional<Staff> findByEmailOrStaffId(String email, String staffId);

    List<Staff> findByStatus(StaffStatus status);

    List<Staff> findBySystemRole(SystemRole systemRole);

    List<Staff> findByDepartment(String department);

    boolean existsByEmail(String email);

    boolean existsByStaffId(String staffId);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByStaffIdAndIdNot(String staffId, Long id);

    List<Staff> findByDepartmentAndStatus(String department, StaffStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT s FROM Staff s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:systemRole IS NULL OR s.systemRole = :systemRole) AND " +
           "(:department IS NULL OR LOWER(s.department) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
           "(:laboratoryId IS NULL OR s.id IN (SELECT ls.staff.id FROM LaboratoryStaff ls WHERE ls.laboratory.id = :laboratoryId AND (:roleInLab IS NULL OR ls.roleInLab = :roleInLab))) AND " +
           "(:search IS NULL OR " +
           " LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.staffId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (s.designation IS NOT NULL AND LOWER(s.designation) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (s.department IS NOT NULL AND LOWER(s.department) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY s.firstName ASC, s.lastName ASC")
    List<Staff> findWithAdvancedFilters(
            @org.springframework.data.repository.query.Param("status") StaffStatus status,
            @org.springframework.data.repository.query.Param("systemRole") SystemRole systemRole,
            @org.springframework.data.repository.query.Param("department") String department,
            @org.springframework.data.repository.query.Param("laboratoryId") Long laboratoryId,
            @org.springframework.data.repository.query.Param("roleInLab") com.calibratesys.entity.enums.StaffLabRole roleInLab,
            @org.springframework.data.repository.query.Param("search") String search
    );

    @org.springframework.data.jpa.repository.Query("SELECT s FROM Staff s WHERE " +
           "(LOWER(s.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(s.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(s.staffId) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(s.email) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (s.designation IS NOT NULL AND LOWER(s.designation) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (s.department IS NOT NULL AND LOWER(s.department) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY s.firstName ASC, s.lastName ASC")
    List<Staff> searchGlobal(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);
}
