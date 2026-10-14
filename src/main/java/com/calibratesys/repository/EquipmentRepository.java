package com.calibratesys.repository;

import com.calibratesys.entity.Equipment;
import com.calibratesys.entity.enums.EquipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    Optional<Equipment> findByAssetTag(String assetTag);

    List<Equipment> findByLaboratoryId(Long laboratoryId);

    List<Equipment> findByStatus(EquipmentStatus status);

    List<Equipment> findByLaboratoryIdAndStatus(Long laboratoryId, EquipmentStatus status);

    boolean existsByAssetTag(String assetTag);

    boolean existsByAssetTagAndIdNot(String assetTag, Long id);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsBySerialNumberAndIdNot(String serialNumber, Long id);

    Optional<Equipment> findBySerialNumber(String serialNumber);

    boolean existsByLaboratoryId(Long laboratoryId);

    List<Equipment> findByNameContainingIgnoreCaseOrAssetTagContainingIgnoreCaseOrSerialNumberContainingIgnoreCase(
            String name, String assetTag, String serialNumber
    );

    @org.springframework.data.jpa.repository.Query("SELECT e.status, COUNT(e) FROM Equipment e GROUP BY e.status")
    List<Object[]> countEquipmentByStatus();

    @org.springframework.data.jpa.repository.Query("SELECT e.status, COUNT(e) FROM Equipment e WHERE e.laboratory.id IN :laboratoryIds GROUP BY e.status")
    List<Object[]> countEquipmentByStatusForLaboratoryIds(@org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds);

    @org.springframework.data.jpa.repository.Query("SELECT e.laboratory.id, COUNT(e) FROM Equipment e GROUP BY e.laboratory.id")
    List<Object[]> countEquipmentGroupedByLaboratory();

    @org.springframework.data.jpa.repository.Query("SELECT e.laboratory.id, COUNT(e) FROM Equipment e WHERE e.laboratory.id IN :laboratoryIds GROUP BY e.laboratory.id")
    List<Object[]> countEquipmentGroupedByLaboratoryForLaboratoryIds(@org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds);

    @org.springframework.data.jpa.repository.Query("SELECT e.laboratory.id, COUNT(e) FROM Equipment e WHERE e.status = :status GROUP BY e.laboratory.id")
    List<Object[]> countEquipmentByStatusGroupedByLaboratory(@org.springframework.data.repository.query.Param("status") EquipmentStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT e.laboratory.id, COUNT(e) FROM Equipment e WHERE e.laboratory.id IN :laboratoryIds AND e.status = :status GROUP BY e.laboratory.id")
    List<Object[]> countEquipmentByStatusGroupedByLaboratoryForLaboratoryIds(@org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds, @org.springframework.data.repository.query.Param("status") EquipmentStatus status);

    long countByLaboratoryIdIn(java.util.Collection<Long> laboratoryIds);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Equipment e WHERE " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:manufacturer IS NULL OR LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :manufacturer, '%'))) AND " +
           "(:modelNumber IS NULL OR LOWER(e.modelNumber) LIKE LOWER(CONCAT('%', :modelNumber, '%'))) AND " +
           "(:frequency IS NULL OR e.calibrationFrequencyDays = :frequency) AND " +
           "(:purchaseFrom IS NULL OR (e.purchaseDate IS NOT NULL AND e.purchaseDate >= :purchaseFrom)) AND " +
           "(:purchaseTo IS NULL OR (e.purchaseDate IS NOT NULL AND e.purchaseDate <= :purchaseTo)) AND " +
           "(:installFrom IS NULL OR (e.installationDate IS NOT NULL AND e.installationDate >= :installFrom)) AND " +
           "(:installTo IS NULL OR (e.installationDate IS NOT NULL AND e.installationDate <= :installTo)) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (e.serialNumber IS NOT NULL AND LOWER(e.serialNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (e.modelNumber IS NOT NULL AND LOWER(e.modelNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (e.manufacturer IS NOT NULL AND LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :search, '%'))))")
    org.springframework.data.domain.Page<Equipment> findWithAdvancedFilters(
            @org.springframework.data.repository.query.Param("laboratoryId") Long laboratoryId,
            @org.springframework.data.repository.query.Param("status") EquipmentStatus status,
            @org.springframework.data.repository.query.Param("manufacturer") String manufacturer,
            @org.springframework.data.repository.query.Param("modelNumber") String modelNumber,
            @org.springframework.data.repository.query.Param("frequency") Integer frequency,
            @org.springframework.data.repository.query.Param("purchaseFrom") java.time.LocalDate purchaseFrom,
            @org.springframework.data.repository.query.Param("purchaseTo") java.time.LocalDate purchaseTo,
            @org.springframework.data.repository.query.Param("installFrom") java.time.LocalDate installFrom,
            @org.springframework.data.repository.query.Param("installTo") java.time.LocalDate installTo,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Equipment e WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(:laboratoryId IS NULL OR e.laboratory.id = :laboratoryId) AND " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:manufacturer IS NULL OR LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :manufacturer, '%'))) AND " +
           "(:modelNumber IS NULL OR LOWER(e.modelNumber) LIKE LOWER(CONCAT('%', :modelNumber, '%'))) AND " +
           "(:frequency IS NULL OR e.calibrationFrequencyDays = :frequency) AND " +
           "(:purchaseFrom IS NULL OR (e.purchaseDate IS NOT NULL AND e.purchaseDate >= :purchaseFrom)) AND " +
           "(:purchaseTo IS NULL OR (e.purchaseDate IS NOT NULL AND e.purchaseDate <= :purchaseTo)) AND " +
           "(:installFrom IS NULL OR (e.installationDate IS NOT NULL AND e.installationDate >= :installFrom)) AND " +
           "(:installTo IS NULL OR (e.installationDate IS NOT NULL AND e.installationDate <= :installTo)) AND " +
           "(:search IS NULL OR " +
           " LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " (e.serialNumber IS NOT NULL AND LOWER(e.serialNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (e.modelNumber IS NOT NULL AND LOWER(e.modelNumber) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           " (e.manufacturer IS NOT NULL AND LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :search, '%'))))")
    org.springframework.data.domain.Page<Equipment> findWithAdvancedFiltersAndLaboratoryIds(
            @org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
            @org.springframework.data.repository.query.Param("laboratoryId") Long laboratoryId,
            @org.springframework.data.repository.query.Param("status") EquipmentStatus status,
            @org.springframework.data.repository.query.Param("manufacturer") String manufacturer,
            @org.springframework.data.repository.query.Param("modelNumber") String modelNumber,
            @org.springframework.data.repository.query.Param("frequency") Integer frequency,
            @org.springframework.data.repository.query.Param("purchaseFrom") java.time.LocalDate purchaseFrom,
            @org.springframework.data.repository.query.Param("purchaseTo") java.time.LocalDate purchaseTo,
            @org.springframework.data.repository.query.Param("installFrom") java.time.LocalDate installFrom,
            @org.springframework.data.repository.query.Param("installTo") java.time.LocalDate installTo,
            @org.springframework.data.repository.query.Param("search") String search,
            org.springframework.data.domain.Pageable pageable
    );

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Equipment e JOIN FETCH e.laboratory lab WHERE " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (e.serialNumber IS NOT NULL AND LOWER(e.serialNumber) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (e.modelNumber IS NOT NULL AND LOWER(e.modelNumber) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (e.manufacturer IS NOT NULL AND LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY e.name ASC")
    List<Equipment> searchGlobal(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Equipment e JOIN FETCH e.laboratory lab WHERE " +
           "e.laboratory.id IN :laboratoryIds AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " LOWER(e.assetTag) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           " (e.serialNumber IS NOT NULL AND LOWER(e.serialNumber) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (e.modelNumber IS NOT NULL AND LOWER(e.modelNumber) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (e.manufacturer IS NOT NULL AND LOWER(e.manufacturer) LIKE LOWER(CONCAT('%', :q, '%')))) " +
           "ORDER BY e.name ASC")
    List<Equipment> searchGlobalWithLaboratoryIds(@org.springframework.data.repository.query.Param("q") String q,
                                                  @org.springframework.data.repository.query.Param("laboratoryIds") java.util.Collection<Long> laboratoryIds,
                                                  org.springframework.data.domain.Pageable pageable);
}
