package com.calibratesys.repository;

import com.calibratesys.entity.CalibrationRecord;
import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CalibrationRecordRepository extends JpaRepository<CalibrationRecord, Long> {

    List<CalibrationRecord> findByEquipmentIdOrderByCalibrationDateDesc(Long equipmentId);

    List<CalibrationRecord> findByCalibrationScheduleId(Long scheduleId);

    List<CalibrationRecord> findByCalibrationScheduleIdOrderByCalibrationDateDesc(Long scheduleId);

    List<CalibrationRecord> findByResult(CalibrationResult result);

    List<CalibrationRecord> findByCalibrationDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByEquipmentId(Long equipmentId);

    boolean existsByCalibrationScheduleId(Long scheduleId);

    @Query("SELECT r FROM CalibrationRecord r JOIN r.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:result IS NULL OR r.result = :result) AND " +
           "(:calibratedByType IS NULL OR r.calibratedByType = :calibratedByType) AND " +
           "(:fromDate IS NULL OR r.calibrationDate >= :fromDate) AND " +
           "(:toDate IS NULL OR r.calibrationDate <= :toDate) " +
           "ORDER BY r.calibrationDate DESC, r.id DESC")
    List<CalibrationRecord> findWithFilters(
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("result") CalibrationResult result,
            @Param("calibratedByType") CalibratedByType calibratedByType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("SELECT r FROM CalibrationRecord r JOIN FETCH r.equipment e JOIN FETCH e.laboratory lab ORDER BY r.calibrationDate DESC, r.id DESC")
    List<CalibrationRecord> findRecentRecords(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM CalibrationRecord r JOIN FETCH r.equipment e JOIN FETCH e.laboratory lab WHERE e.laboratory.id IN :laboratoryIds ORDER BY r.calibrationDate DESC, r.id DESC")
    List<CalibrationRecord> findRecentRecordsForLaboratoryIds(@Param("laboratoryIds") java.util.Collection<Long> laboratoryIds, org.springframework.data.domain.Pageable pageable);

    long countByEquipmentLaboratoryIdIn(java.util.Collection<Long> laboratoryIds);

    @Query(value = "SELECT r FROM CalibrationRecord r JOIN r.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:result IS NULL OR r.result = :result) AND " +
           "(:calibratedByType IS NULL OR r.calibratedByType = :calibratedByType) AND " +
           "(:calibratorName IS NULL OR LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :calibratorName, '%'))) AND " +
           "(:calFrom IS NULL OR r.calibrationDate >= :calFrom) AND " +
           "(:calTo IS NULL OR r.calibrationDate <= :calTo) AND " +
           "(:dueFrom IS NULL OR r.nextDueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR r.nextDueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (r.calibratorName IS NOT NULL AND LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (r.notes IS NOT NULL AND LOWER(r.notes) LIKE LOWER(CONCAT('%', :search, '%'))))",
           countQuery = "SELECT COUNT(r) FROM CalibrationRecord r JOIN r.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:result IS NULL OR r.result = :result) AND " +
           "(:calibratedByType IS NULL OR r.calibratedByType = :calibratedByType) AND " +
           "(:calibratorName IS NULL OR LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :calibratorName, '%'))) AND " +
           "(:calFrom IS NULL OR r.calibrationDate >= :calFrom) AND " +
           "(:calTo IS NULL OR r.calibrationDate <= :calTo) AND " +
           "(:dueFrom IS NULL OR r.nextDueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR r.nextDueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (r.calibratorName IS NOT NULL AND LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (r.notes IS NOT NULL AND LOWER(r.notes) LIKE LOWER(CONCAT('%', :search, '%'))))")
    org.springframework.data.domain.Page<CalibrationRecord> findWithAdvancedFilters(
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("result") CalibrationResult result,
            @Param("calibratedByType") CalibratedByType calibratedByType,
            @Param("calibratorName") String calibratorName,
            @Param("calFrom") LocalDate calFrom,
            @Param("calTo") LocalDate calTo,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(value = "SELECT r FROM CalibrationRecord r JOIN r.equipment e WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:result IS NULL OR r.result = :result) AND " +
           "(:calibratedByType IS NULL OR r.calibratedByType = :calibratedByType) AND " +
           "(:calibratorName IS NULL OR LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :calibratorName, '%'))) AND " +
           "(:calFrom IS NULL OR r.calibrationDate >= :calFrom) AND " +
           "(:calTo IS NULL OR r.calibrationDate <= :calTo) AND " +
           "(:dueFrom IS NULL OR r.nextDueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR r.nextDueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (r.calibratorName IS NOT NULL AND LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (r.notes IS NOT NULL AND LOWER(r.notes) LIKE LOWER(CONCAT('%', :search, '%'))))",
           countQuery = "SELECT COUNT(r) FROM CalibrationRecord r JOIN r.equipment e WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:result IS NULL OR r.result = :result) AND " +
           "(:calibratedByType IS NULL OR r.calibratedByType = :calibratedByType) AND " +
           "(:calibratorName IS NULL OR LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :calibratorName, '%'))) AND " +
           "(:calFrom IS NULL OR r.calibrationDate >= :calFrom) AND " +
           "(:calTo IS NULL OR r.calibrationDate <= :calTo) AND " +
           "(:dueFrom IS NULL OR r.nextDueDate >= :dueFrom) AND " +
           "(:dueTo IS NULL OR r.nextDueDate <= :dueTo) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (r.calibratorName IS NOT NULL AND LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (r.notes IS NOT NULL AND LOWER(r.notes) LIKE LOWER(CONCAT('%', :search, '%'))))")
    org.springframework.data.domain.Page<CalibrationRecord> findWithAdvancedFiltersAndLaboratoryIds(
            @Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("result") CalibrationResult result,
            @Param("calibratedByType") CalibratedByType calibratedByType,
            @Param("calibratorName") String calibratorName,
            @Param("calFrom") LocalDate calFrom,
            @Param("calTo") LocalDate calTo,
            @Param("dueFrom") LocalDate dueFrom,
            @Param("dueTo") LocalDate dueTo,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT r FROM CalibrationRecord r JOIN FETCH r.equipment e JOIN FETCH e.laboratory lab WHERE " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (r.calibratorName IS NOT NULL AND LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (r.notes IS NOT NULL AND LOWER(r.notes) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY r.calibrationDate DESC, r.id DESC")
    List<CalibrationRecord> searchGlobal(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM CalibrationRecord r JOIN FETCH r.equipment e JOIN FETCH e.laboratory lab WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (r.calibratorName IS NOT NULL AND LOWER(r.calibratorName) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (r.notes IS NOT NULL AND LOWER(r.notes) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY r.calibrationDate DESC, r.id DESC")
    List<CalibrationRecord> searchGlobalWithLaboratoryIds(@Param("q") String q,
                                                          @Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
                                                          org.springframework.data.domain.Pageable pageable);
}
