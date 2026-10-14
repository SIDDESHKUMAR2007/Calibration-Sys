package com.calibratesys.repository;

import com.calibratesys.entity.CalibrationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalibrationDocumentRepository extends JpaRepository<CalibrationDocument, Long> {

    List<CalibrationDocument> findByEquipmentId(Long equipmentId);

    List<CalibrationDocument> findByEquipmentIdOrderByUploadedAtDesc(Long equipmentId);

    List<CalibrationDocument> findByCalibrationRecordId(Long calibrationRecordId);

    List<CalibrationDocument> findByCalibrationRecordIdOrderByUploadedAtDesc(Long calibrationRecordId);

    Optional<CalibrationDocument> findByCertificateNumber(String certificateNumber);

    boolean existsByEquipmentId(Long equipmentId);

    @Query("SELECT d FROM CalibrationDocument d WHERE " +
           "(:equipmentId IS NULL OR d.equipment.id = :equipmentId) AND " +
           "(:calibrationRecordId IS NULL OR (d.calibrationRecord IS NOT NULL AND d.calibrationRecord.id = :calibrationRecordId)) AND " +
           "(:certificateNumber IS NULL OR LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :certificateNumber, '%'))) AND " +
           "(:issueStartDate IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate >= :issueStartDate)) AND " +
           "(:issueEndDate IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate <= :issueEndDate)) AND " +
           "(:validStartDate IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil >= :validStartDate)) AND " +
           "(:validEndDate IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil <= :validEndDate)) " +
           "ORDER BY d.uploadedAt DESC")
    List<CalibrationDocument> findWithFilters(
            @Param("equipmentId") Long equipmentId,
            @Param("calibrationRecordId") Long calibrationRecordId,
            @Param("certificateNumber") String certificateNumber,
            @Param("issueStartDate") LocalDate issueStartDate,
            @Param("issueEndDate") LocalDate issueEndDate,
            @Param("validStartDate") LocalDate validStartDate,
            @Param("validEndDate") LocalDate validEndDate
    );

    @Query("SELECT d.calibrationRecord.id, COUNT(d) FROM CalibrationDocument d WHERE d.calibrationRecord.id IN :recordIds GROUP BY d.calibrationRecord.id")
    List<Object[]> countDocumentsPerRecordId(@Param("recordIds") java.util.Collection<Long> recordIds);

    @Query("SELECT COUNT(DISTINCT d.calibrationRecord.id) FROM CalibrationDocument d WHERE d.calibrationRecord IS NOT NULL")
    long countDistinctRecordsWithDocuments();

    @Query("SELECT COUNT(DISTINCT d.calibrationRecord.id) FROM CalibrationDocument d WHERE d.calibrationRecord IS NOT NULL AND d.equipment.laboratory.id IN :laboratoryIds")
    long countDistinctRecordsWithDocumentsForLaboratoryIds(@Param("laboratoryIds") java.util.Collection<Long> laboratoryIds);

    @Query(value = "SELECT d FROM CalibrationDocument d JOIN d.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:recordId IS NULL OR (d.calibrationRecord IS NOT NULL AND d.calibrationRecord.id = :recordId)) AND " +
           "(:certNum IS NULL OR LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :certNum, '%'))) AND " +
           "(:docName IS NULL OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :docName, '%'))) AND " +
           "(:fileType IS NULL OR LOWER(d.fileType) LIKE LOWER(CONCAT('%', :fileType, '%'))) AND " +
           "(:issueFrom IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate >= :issueFrom)) AND " +
           "(:issueTo IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate <= :issueTo)) AND " +
           "(:validFrom IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil >= :validFrom)) AND " +
           "(:validTo IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil <= :validTo)) AND " +
           "(:search IS NULL OR " +
           " LOWER(d.documentName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (d.certificateNumber IS NOT NULL AND LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(d) FROM CalibrationDocument d JOIN d.equipment e WHERE " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:recordId IS NULL OR (d.calibrationRecord IS NOT NULL AND d.calibrationRecord.id = :recordId)) AND " +
           "(:certNum IS NULL OR LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :certNum, '%'))) AND " +
           "(:docName IS NULL OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :docName, '%'))) AND " +
           "(:fileType IS NULL OR LOWER(d.fileType) LIKE LOWER(CONCAT('%', :fileType, '%'))) AND " +
           "(:issueFrom IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate >= :issueFrom)) AND " +
           "(:issueTo IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate <= :issueTo)) AND " +
           "(:validFrom IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil >= :validFrom)) AND " +
           "(:validTo IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil <= :validTo)) AND " +
           "(:search IS NULL OR " +
           " LOWER(d.documentName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (d.certificateNumber IS NOT NULL AND LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<CalibrationDocument> findWithAdvancedFilters(
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("recordId") Long recordId,
            @Param("certNum") String certNum,
            @Param("docName") String docName,
            @Param("fileType") String fileType,
            @Param("issueFrom") LocalDate issueFrom,
            @Param("issueTo") LocalDate issueTo,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @Query(value = "SELECT d FROM CalibrationDocument d JOIN d.equipment e WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:recordId IS NULL OR (d.calibrationRecord IS NOT NULL AND d.calibrationRecord.id = :recordId)) AND " +
           "(:certNum IS NULL OR LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :certNum, '%'))) AND " +
           "(:docName IS NULL OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :docName, '%'))) AND " +
           "(:fileType IS NULL OR LOWER(d.fileType) LIKE LOWER(CONCAT('%', :fileType, '%'))) AND " +
           "(:issueFrom IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate >= :issueFrom)) AND " +
           "(:issueTo IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate <= :issueTo)) AND " +
           "(:validFrom IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil >= :validFrom)) AND " +
           "(:validTo IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil <= :validTo)) AND " +
           "(:search IS NULL OR " +
           " LOWER(d.documentName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (d.certificateNumber IS NOT NULL AND LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')))",
           countQuery = "SELECT COUNT(d) FROM CalibrationDocument d JOIN d.equipment e WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(:equipmentId IS NULL OR e.id = :equipmentId) AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:recordId IS NULL OR (d.calibrationRecord IS NOT NULL AND d.calibrationRecord.id = :recordId)) AND " +
           "(:certNum IS NULL OR LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :certNum, '%'))) AND " +
           "(:docName IS NULL OR LOWER(d.documentName) LIKE LOWER(CONCAT('%', :docName, '%'))) AND " +
           "(:fileType IS NULL OR LOWER(d.fileType) LIKE LOWER(CONCAT('%', :fileType, '%'))) AND " +
           "(:issueFrom IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate >= :issueFrom)) AND " +
           "(:issueTo IS NULL OR (d.issueDate IS NOT NULL AND d.issueDate <= :issueTo)) AND " +
           "(:validFrom IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil >= :validFrom)) AND " +
           "(:validTo IS NULL OR (d.validUntil IS NOT NULL AND d.validUntil <= :validTo)) AND " +
           "(:search IS NULL OR " +
           " LOWER(d.documentName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (d.certificateNumber IS NOT NULL AND LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')))")
    org.springframework.data.domain.Page<CalibrationDocument> findWithAdvancedFiltersAndLaboratoryIds(
            @Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
            @Param("equipmentId") Long equipmentId,
            @Param("laboratoryId") Long laboratoryId,
            @Param("recordId") Long recordId,
            @Param("certNum") String certNum,
            @Param("docName") String docName,
            @Param("fileType") String fileType,
            @Param("issueFrom") LocalDate issueFrom,
            @Param("issueTo") LocalDate issueTo,
            @Param("validFrom") LocalDate validFrom,
            @Param("validTo") LocalDate validTo,
            @Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT d FROM CalibrationDocument d JOIN FETCH d.equipment e JOIN FETCH e.laboratory lab WHERE " +
           "(LOWER(d.documentName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (d.certificateNumber IS NOT NULL AND LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY d.uploadedAt DESC")
    List<CalibrationDocument> searchGlobal(@Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT d FROM CalibrationDocument d JOIN FETCH d.equipment e JOIN FETCH e.laboratory lab WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(LOWER(d.documentName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (d.certificateNumber IS NOT NULL AND LOWER(d.certificateNumber) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY d.uploadedAt DESC")
    List<CalibrationDocument> searchGlobalWithLaboratoryIds(@Param("q") String q,
                                                            @Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
                                                            org.springframework.data.domain.Pageable pageable);
}
