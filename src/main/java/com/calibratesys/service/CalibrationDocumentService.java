package com.calibratesys.service;

import com.calibratesys.dto.CalibrationDocumentResponseDto;
import com.calibratesys.dto.DownloadedDocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface CalibrationDocumentService {

    CalibrationDocumentResponseDto uploadDocument(Long equipmentId, Long calibrationRecordId,
                                                  String documentName, String certificateNumber,
                                                  LocalDate issueDate, LocalDate validUntil,
                                                  MultipartFile file);

    CalibrationDocumentResponseDto getDocumentById(Long id);

    DownloadedDocumentDto downloadDocument(Long id);

    List<CalibrationDocumentResponseDto> getDocumentsByEquipment(Long equipmentId);

    List<CalibrationDocumentResponseDto> getDocumentsByCalibrationRecord(Long calibrationRecordId);

    List<CalibrationDocumentResponseDto> getAllDocumentsWithFilters(Long equipmentId,
                                                                   Long calibrationRecordId,
                                                                   String certificateNumber,
                                                                   LocalDate issueStartDate,
                                                                   LocalDate issueEndDate,
                                                                   LocalDate validStartDate,
                                                                   LocalDate validEndDate);

    void deleteDocument(Long id);

    com.calibratesys.dto.PageResponseDto<CalibrationDocumentResponseDto> searchDocuments(
            Long equipmentId,
            Long laboratoryId,
            Long calibrationRecordId,
            String certificateNumber,
            String documentName,
            String fileType,
            String validity,
            LocalDate issueStartDate,
            LocalDate issueEndDate,
            LocalDate validStartDate,
            LocalDate validEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    );
}
