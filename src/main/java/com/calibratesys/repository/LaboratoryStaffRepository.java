package com.calibratesys.repository;

import com.calibratesys.entity.LaboratoryStaff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaboratoryStaffRepository extends JpaRepository<LaboratoryStaff, Long> {

    List<LaboratoryStaff> findByLaboratoryId(Long laboratoryId);

    List<LaboratoryStaff> findByStaffId(Long staffId);

    List<LaboratoryStaff> findByLaboratoryIdAndIsPrimaryContactTrue(Long laboratoryId);

    List<LaboratoryStaff> findByLaboratoryIdAndStatus(Long laboratoryId, String status);

    Optional<LaboratoryStaff> findByLaboratoryIdAndStaffId(Long laboratoryId, Long staffId);

    boolean existsByLaboratoryIdAndStaffId(Long laboratoryId, Long staffId);

    boolean existsByLaboratoryId(Long laboratoryId);

    boolean existsByStaffId(Long staffId);

    List<LaboratoryStaff> findByLaboratoryIdAndIsPrimaryContactTrueAndStatus(Long laboratoryId, String status);

    @org.springframework.data.jpa.repository.Query("SELECT ls FROM LaboratoryStaff ls JOIN FETCH ls.staff s WHERE ls.laboratory.id = :laboratoryId AND UPPER(ls.status) = 'ACTIVE' AND s.status = com.calibratesys.entity.enums.StaffStatus.ACTIVE")
    List<LaboratoryStaff> findActiveAssignmentsByLaboratoryId(@org.springframework.data.repository.query.Param("laboratoryId") Long laboratoryId);

    @org.springframework.data.jpa.repository.Query("SELECT ls FROM LaboratoryStaff ls JOIN FETCH ls.laboratory lab JOIN FETCH lab.college WHERE ls.staff.id = :staffId AND UPPER(ls.status) = 'ACTIVE' AND ls.staff.status = com.calibratesys.entity.enums.StaffStatus.ACTIVE")
    List<LaboratoryStaff> findActiveAssignmentsByStaffId(@org.springframework.data.repository.query.Param("staffId") Long staffId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(ls) > 0 FROM LaboratoryStaff ls WHERE ls.laboratory.id = :laboratoryId AND ls.staff.id = :staffId AND UPPER(ls.status) = 'ACTIVE' AND ls.staff.status = com.calibratesys.entity.enums.StaffStatus.ACTIVE")
    boolean hasActiveAssignment(@org.springframework.data.repository.query.Param("staffId") Long staffId, @org.springframework.data.repository.query.Param("laboratoryId") Long laboratoryId);

    @org.springframework.data.jpa.repository.Query("SELECT ls.laboratory.id, COUNT(ls) FROM LaboratoryStaff ls WHERE UPPER(ls.status) = 'ACTIVE' AND ls.staff.status = com.calibratesys.entity.enums.StaffStatus.ACTIVE GROUP BY ls.laboratory.id")
    List<Object[]> countActiveStaffPerLaboratory();

    @org.springframework.data.jpa.repository.Query("SELECT ls.laboratory.id, COUNT(ls) FROM LaboratoryStaff ls WHERE ls.laboratory.id IN :laboratoryIds AND UPPER(ls.status) = 'ACTIVE' AND ls.staff.status = com.calibratesys.entity.enums.StaffStatus.ACTIVE GROUP BY ls.laboratory.id")
    List<Object[]> countActiveStaffPerLaboratoryForLaboratoryIds(@org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds);

    @org.springframework.data.jpa.repository.Query(value = "SELECT ls FROM LaboratoryStaff ls JOIN FETCH ls.laboratory lab JOIN FETCH ls.staff s WHERE " +
           "(:laboratoryId IS NULL OR lab.id = :laboratoryId) AND " +
           "(:staffId IS NULL OR s.id = :staffId) AND " +
           "(:roleInLab IS NULL OR ls.roleInLab = :roleInLab) AND " +
           "(:isPrimaryContact IS NULL OR ls.isPrimaryContact = :isPrimaryContact) AND " +
           "(:status IS NULL OR UPPER(ls.status) = UPPER(:status))",
           countQuery = "SELECT COUNT(ls) FROM LaboratoryStaff ls WHERE " +
           "(:laboratoryId IS NULL OR ls.laboratory.id = :laboratoryId) AND " +
           "(:staffId IS NULL OR ls.staff.id = :staffId) AND " +
           "(:roleInLab IS NULL OR ls.roleInLab = :roleInLab) AND " +
           "(:isPrimaryContact IS NULL OR ls.isPrimaryContact = :isPrimaryContact) AND " +
           "(:status IS NULL OR UPPER(ls.status) = UPPER(:status))")
    org.springframework.data.domain.Page<LaboratoryStaff> findAssignmentsWithFilters(
            @org.springframework.data.repository.query.Param("laboratoryId") Long laboratoryId,
            @org.springframework.data.repository.query.Param("staffId") Long staffId,
            @org.springframework.data.repository.query.Param("roleInLab") com.calibratesys.entity.enums.StaffLabRole roleInLab,
            @org.springframework.data.repository.query.Param("isPrimaryContact") Boolean isPrimaryContact,
            @org.springframework.data.repository.query.Param("status") String status,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query(value = "SELECT ls FROM LaboratoryStaff ls JOIN FETCH ls.laboratory lab JOIN FETCH ls.staff s WHERE " +
           "lab.id IN :laboratoryIds AND " +
           "(:laboratoryId IS NULL OR lab.id = :laboratoryId) AND " +
           "(:staffId IS NULL OR s.id = :staffId) AND " +
           "(:roleInLab IS NULL OR ls.roleInLab = :roleInLab) AND " +
           "(:isPrimaryContact IS NULL OR ls.isPrimaryContact = :isPrimaryContact) AND " +
           "(:status IS NULL OR UPPER(ls.status) = UPPER(:status))",
           countQuery = "SELECT COUNT(ls) FROM LaboratoryStaff ls WHERE " +
           "ls.laboratory.id IN :laboratoryIds AND " +
           "(:laboratoryId IS NULL OR ls.laboratory.id = :laboratoryId) AND " +
           "(:staffId IS NULL OR ls.staff.id = :staffId) AND " +
           "(:roleInLab IS NULL OR ls.roleInLab = :roleInLab) AND " +
           "(:isPrimaryContact IS NULL OR ls.isPrimaryContact = :isPrimaryContact) AND " +
           "(:status IS NULL OR UPPER(ls.status) = UPPER(:status))")
    org.springframework.data.domain.Page<LaboratoryStaff> findAssignmentsWithFiltersForLaboratoryIds(
            @org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
            @org.springframework.data.repository.query.Param("laboratoryId") Long laboratoryId,
            @org.springframework.data.repository.query.Param("staffId") Long staffId,
            @org.springframework.data.repository.query.Param("roleInLab") com.calibratesys.entity.enums.StaffLabRole roleInLab,
            @org.springframework.data.repository.query.Param("isPrimaryContact") Boolean isPrimaryContact,
            @org.springframework.data.repository.query.Param("status") String status,
            org.springframework.data.domain.Pageable pageable
    );
}
