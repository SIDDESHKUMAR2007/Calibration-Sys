package com.calibratesys.repository;

import com.calibratesys.entity.Laboratory;
import com.calibratesys.entity.enums.LabStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LaboratoryRepository extends JpaRepository<Laboratory, Long> {

    Optional<Laboratory> findByLabCode(String labCode);

    boolean existsByLabCode(String labCode);

    boolean existsByLabCodeAndIdNot(String labCode, Long id);

    boolean existsByCollegeId(Long collegeId);

    List<Laboratory> findByCollegeId(Long collegeId);

    List<Laboratory> findByDepartment(String department);

    List<Laboratory> findByStatus(LabStatus status);

    List<Laboratory> findByCollegeIdAndStatus(Long collegeId, LabStatus status);

    List<Laboratory> findByIdIn(java.util.Collection<Long> ids);

    long countByStatus(LabStatus status);

    long countByStatusAndIdIn(LabStatus status, java.util.Collection<Long> ids);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM Laboratory l JOIN FETCH l.college c WHERE " +
           "(:collegeId IS NULL OR l.college.id = :collegeId) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:department IS NULL OR LOWER(l.department) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
           "(:search IS NULL OR " +
           " LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.labCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (l.location IS NOT NULL AND LOWER(l.location) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY l.name ASC")
    List<Laboratory> findWithAdvancedFilters(
            @org.springframework.data.repository.query.Param("collegeId") Long collegeId,
            @org.springframework.data.repository.query.Param("status") LabStatus status,
            @org.springframework.data.repository.query.Param("department") String department,
            @org.springframework.data.repository.query.Param("search") String search
    );

    @org.springframework.data.jpa.repository.Query("SELECT l FROM Laboratory l JOIN FETCH l.college c WHERE " +
           "l.id IN :laboratoryIds AND " +
           "(:collegeId IS NULL OR l.college.id = :collegeId) AND " +
           "(:status IS NULL OR l.status = :status) AND " +
           "(:department IS NULL OR LOWER(l.department) LIKE LOWER(CONCAT('%', :department, '%'))) AND " +
           "(:search IS NULL OR " +
           " LOWER(l.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.labCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(l.department) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (l.location IS NOT NULL AND LOWER(l.location) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY l.name ASC")
    List<Laboratory> findWithAdvancedFiltersAndLaboratoryIds(
            @org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
            @org.springframework.data.repository.query.Param("collegeId") Long collegeId,
            @org.springframework.data.repository.query.Param("status") LabStatus status,
            @org.springframework.data.repository.query.Param("department") String department,
            @org.springframework.data.repository.query.Param("search") String search
    );

    @org.springframework.data.jpa.repository.Query("SELECT l FROM Laboratory l JOIN FETCH l.college c WHERE " +
           "(LOWER(l.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(l.labCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(l.department) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (l.location IS NOT NULL AND LOWER(l.location) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY l.name ASC")
    List<Laboratory> searchGlobal(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM Laboratory l JOIN FETCH l.college c WHERE " +
           "l.id IN :laboratoryIds AND " +
           "(LOWER(l.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(l.labCode) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(l.department) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (l.location IS NOT NULL AND LOWER(l.location) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY l.name ASC")
    List<Laboratory> searchGlobalWithLaboratoryIds(@org.springframework.data.repository.query.Param("q") String q,
                                                   @org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
                                                   org.springframework.data.domain.Pageable pageable);
}
