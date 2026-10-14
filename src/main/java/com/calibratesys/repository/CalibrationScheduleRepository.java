package com.calibratesys.repository;

import com.calibratesys.entity.CalibrationSchedule;
import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface CalibrationScheduleRepository extends JpaRepository<CalibrationSchedule, Long> {

    List<CalibrationSchedule> findByEquipmentId(Long equipmentId);

    List<CalibrationSchedule> findByEquipmentIdOrderByDueDateAsc(Long equipmentId);

    List<CalibrationSchedule> findByStatus(ScheduleStatus status);

    List<CalibrationSchedule> findByDueDateBeforeAndStatusNot(LocalDate dueDate, ScheduleStatus status);

    List<CalibrationSchedule> findByDueDateBetweenAndStatus(LocalDate start, LocalDate end, ScheduleStatus status);

    List<CalibrationSchedule> findByEquipmentLaboratoryIdAndStatus(Long laboratoryId, ScheduleStatus status);

    List<CalibrationSchedule> findByEquipmentLaboratoryIdOrderByDueDateAsc(Long laboratoryId);

    List<CalibrationSchedule> findByStatusNotIn(Collection<ScheduleStatus> statuses);

    boolean existsByEquipmentId(Long equipmentId);

    @Query("SELECT s FROM CalibrationSchedule s JOIN s.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:priority IS NULL OR s.priority = :priority) AND " +
           "(:fromDate IS NULL OR s.dueDate >= :fromDate) AND " +
           "(:toDate IS NULL OR s.dueDate <= :toDate) " +
           "ORDER BY s.dueDate ASC")
    List<CalibrationSchedule> findWithFilters(
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("status") ScheduleStatus status,
            @Param("priority") SchedulePriority priority,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT s.status, COUNT(s) FROM CalibrationSchedule s GROUP BY s.status")
    List<Object[]> countSchedulesByStatus();

    @Query("SELECT s.status, COUNT(s) FROM CalibrationSchedule s WHERE s.equipment.laboratory.id IN :laboratoryIds GROUP BY s.status")
    List<Object[]> countSchedulesByStatusForLaboratoryIds(@Param("laboratoryIds") Collection<Long> laboratoryIds);

    @Query("SELECT s.equipment.laboratory.id, s.status, COUNT(s) FROM CalibrationSchedule s GROUP BY s.equipment.laboratory.id, s.status")
    List<Object[]> countSchedulesPerLaboratoryAndStatus();

    @Query("SELECT s.equipment.laboratory.id, s.status, COUNT(s) FROM CalibrationSchedule s WHERE s.equipment.laboratory.id IN :laboratoryIds GROUP BY s.equipment.laboratory.id, s.status")
    List<Object[]> countSchedulesPerLaboratoryAndStatusForLaboratoryIds(@Param("laboratoryIds") Collection<Long> laboratoryIds);

    @Query("SELECT s FROM CalibrationSchedule s JOIN FETCH s.equipment e JOIN FETCH e.laboratory lab WHERE s.status = :status ORDER BY s.dueDate ASC")
    List<CalibrationSchedule> findByStatusWithEquipmentAndLaboratory(@Param("status") ScheduleStatus status);

    @Query("SELECT s FROM CalibrationSchedule s JOIN FETCH s.equipment e JOIN FETCH e.laboratory lab WHERE s.status = :status AND e.laboratory.id IN :laboratoryIds ORDER BY s.dueDate ASC")
    List<CalibrationSchedule> findByStatusAndLaboratoryIdInWithEquipmentAndLaboratory(@Param("status") ScheduleStatus status, @Param("laboratoryIds") Collection<Long> laboratoryIds);

    @Query(value = "SELECT s FROM CalibrationSchedule s JOIN s.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:assetTag IS NULL OR LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :assetTag, '%'))) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:priority IS NULL OR s.priority = :priority) AND " +
           "(:scheduledFrom IS NULL OR s.scheduledDate >= :scheduledFrom) AND " +
           "(:scheduledTo IS NULL OR s.scheduledDate <= :scheduledTo) AND " +
           "(:dueFrom IS NULL OR s.dueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR s.dueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (s.remarks IS NOT NULL AND LOWER(s.remarks) LIKE LOWER(CONCAT('%', :search, '%'))))",
           countQuery = "SELECT COUNT(s) FROM CalibrationSchedule s JOIN s.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:assetTag IS NULL OR LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :assetTag, '%'))) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:priority IS NULL OR s.priority = :priority) AND " +
           "(:scheduledFrom IS NULL OR s.scheduledDate >= :scheduledFrom) AND " +
           "(:scheduledTo IS NULL OR s.scheduledDate <= :scheduledTo) AND " +
           "(:dueFrom IS NULL OR s.dueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR s.dueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (s.remarks IS NOT NULL AND LOWER(s.remarks) LIKE LOWER(CONCAT('%', :search, '%'))))")
    org.springframework.data.domain.Page<CalibrationSchedule> findWithAdvancedFilters(
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("assetTag") String assetTag,
            @Param("status") ScheduleStatus status,
            @Param("priority") SchedulePriority priority,
            @Param("scheduledFrom") LocalDate scheduledFrom,
            @Param("scheduledTo") LocalDate scheduledTo,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(value = "SELECT s FROM CalibrationSchedule s JOIN s.equipment e WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:assetTag IS NULL OR LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :assetTag, '%'))) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:priority IS NULL OR s.priority = :priority) AND " +
           "(:scheduledFrom IS NULL OR s.scheduledDate >= :scheduledFrom) AND " +
           "(:scheduledTo IS NULL OR s.scheduledDate <= :scheduledTo) AND " +
           "(:dueFrom IS NULL OR s.dueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR s.dueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (s.remarks IS NOT NULL AND LOWER(s.remarks) LIKE LOWER(CONCAT('%', :search, '%'))))",
           countQuery = "SELECT COUNT(s) FROM CalibrationSchedule s JOIN s.equipment e WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:assetTag IS NULL OR LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :assetTag, '%'))) AND " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:priority IS NULL OR s.priority = :priority) AND " +
           "(:scheduledFrom IS NULL OR s.scheduledDate >= :scheduledFrom) AND " +
           "(:scheduledTo IS NULL OR s.scheduledDate <= :scheduledTo) AND " +
           "(:dueFrom IS NULL OR s.dueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR s.dueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (s.remarks IS NOT NULL AND LOWER(s.remarks) LIKE LOWER(CONCAT('%', :search, '%'))))")
    org.springframework.data.domain.Page<CalibrationSchedule> findWithAdvancedFiltersAndLaboratoryIds(
            @Param("laboratoryIds") Collection<Long> laboratoryIds,
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("assetTag") String assetTag,
            @Param("status") ScheduleStatus status,
            @Param("priority") SchedulePriority priority,
            @Param("scheduledFrom") LocalDate scheduledFrom,
            @Param("scheduledTo") LocalDate scheduledTo,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT s FROM CalibrationSchedule s JOIN FETCH s.equipment e JOIN FETCH e.laboratory lab WHERE " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (s.remarks IS NOT NULL AND LOWER(s.remarks) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY s.dueDate ASC")
    List<CalibrationSchedule> searchGlobal(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT s FROM CalibrationSchedule s JOIN FETCH s.equipment e JOIN FETCH e.laboratory lab WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (s.remarks IS NOT NULL AND LOWER(s.remarks) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY s.dueDate ASC")
    List<CalibrationSchedule> searchGlobalWithLaboratoryIds(@Param("q") String q,
                                                            @Param("laboratoryIds") Collection<Long> laboratoryIds,
                                                            org.springframework.data.domain.Pageable pageable);
}
