package com.calibratesys.service.impl;

import com.calibratesys.dto.GlobalSearchResultItemDto;
import com.calibratesys.dto.GlobalSearchResponseDto;
import com.calibratesys.entity.*;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.*;
import com.calibratesys.security.DataScopeService;
import com.calibratesys.security.ScopedContext;
import com.calibratesys.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SearchServiceImpl implements SearchService {

    private final EquipmentRepository equipmentRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final StaffRepository staffRepository;
    private final CalibrationScheduleRepository calibrationScheduleRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;
    private final CalibrationDocumentRepository calibrationDocumentRepository;
    private final DataScopeService dataScopeService;
    private final Clock clock;

    @Autowired
    public SearchServiceImpl(EquipmentRepository equipmentRepository,
                             LaboratoryRepository laboratoryRepository,
                             StaffRepository staffRepository,
                             CalibrationScheduleRepository calibrationScheduleRepository,
                             CalibrationRecordRepository calibrationRecordRepository,
                             CalibrationDocumentRepository calibrationDocumentRepository,
                             DataScopeService dataScopeService,
                             Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.staffRepository = staffRepository;
        this.calibrationScheduleRepository = calibrationScheduleRepository;
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.calibrationDocumentRepository = calibrationDocumentRepository;
        this.dataScopeService = dataScopeService;
        this.clock = clock;
    }

    @Override
    public GlobalSearchResponseDto searchGlobal(String query) {
        if (!StringUtils.hasText(query) || query.trim().isEmpty()) {
            throw new ApiException("Search query parameter 'q' is required and cannot be blank", HttpStatus.BAD_REQUEST);
        }

        String q = query.trim();
        if (q.length() > 200) {
            throw new ApiException("Search query parameter 'q' cannot exceed 200 characters", HttpStatus.BAD_REQUEST);
        }

        ScopedContext scope = dataScopeService.resolveScope(null);
        Pageable limit10 = PageRequest.of(0, 10);
        LocalDate today = LocalDate.now(clock);

        // 1. Equipment
        List<GlobalSearchResultItemDto> eqItems = Collections.emptyList();
        if (!scope.isEmptyScope()) {
            List<Equipment> equipmentList = scope.laboratoryIds() != null
                    ? equipmentRepository.searchGlobalWithLaboratoryIds(q, scope.laboratoryIds(), limit10)
                    : equipmentRepository.searchGlobal(q, limit10);

            eqItems = equipmentList.stream().map(e -> {
                String sub = "Asset: " + e.getAssetTag() + (e.getModelNumber() != null ? " | Model: " + e.getModelNumber() : "");
                String labName = e.getLaboratory() != null ? e.getLaboratory().getName() : null;
                Long labId = e.getLaboratory() != null ? e.getLaboratory().getId() : null;
                GlobalSearchResultItemDto item = new GlobalSearchResultItemDto(
                        "EQUIPMENT", e.getId(), e.getName(), sub, labName, e.getStatus() != null ? e.getStatus().name() : null
                );
                item.setLaboratoryId(labId);
                item.setTargetUrl("/equipment?id=" + e.getId());
                return item;
            }).collect(Collectors.toList());
        }

        // 2. Laboratories
        List<GlobalSearchResultItemDto> labItems = Collections.emptyList();
        if (!scope.isEmptyScope()) {
            List<Laboratory> labList = scope.laboratoryIds() != null
                    ? laboratoryRepository.searchGlobalWithLaboratoryIds(q, scope.laboratoryIds(), limit10)
                    : laboratoryRepository.searchGlobal(q, limit10);

            labItems = labList.stream().map(l -> {
                String sub = "Code: " + l.getLabCode() + " | Dept: " + l.getDepartment();
                GlobalSearchResultItemDto item = new GlobalSearchResultItemDto(
                        "LABORATORY", l.getId(), l.getName(), sub, l.getName(), l.getStatus() != null ? l.getStatus().name() : null
                );
                item.setLaboratoryId(l.getId());
                item.setTargetUrl("/laboratories?id=" + l.getId());
                return item;
            }).collect(Collectors.toList());
        }

        // 3. Staff (directory-wide)
        List<Staff> staffList = staffRepository.searchGlobal(q, limit10);
        List<GlobalSearchResultItemDto> staffItems = staffList.stream().map(s -> {
            String sub = "ID: " + s.getStaffId() + (s.getDesignation() != null ? " | " + s.getDesignation() : "");
            GlobalSearchResultItemDto item = new GlobalSearchResultItemDto(
                    "STAFF", s.getId(), s.getFullName(), sub, null, s.getStatus() != null ? s.getStatus().name() : null
            );
            item.setTargetUrl("/staff?id=" + s.getId());
            return item;
        }).collect(Collectors.toList());

        // 4. Calibration Schedules
        List<GlobalSearchResultItemDto> schedItems = Collections.emptyList();
        if (!scope.isEmptyScope()) {
            List<CalibrationSchedule> schedList = scope.laboratoryIds() != null
                    ? calibrationScheduleRepository.searchGlobalWithLaboratoryIds(q, scope.laboratoryIds(), limit10)
                    : calibrationScheduleRepository.searchGlobal(q, limit10);

            schedItems = schedList.stream().map(s -> {
                Equipment eq = s.getEquipment();
                String title = eq != null ? eq.getName() : "Schedule #" + s.getId();
                String sub = "Due: " + s.getDueDate() + " | Priority: " + s.getPriority();
                String labName = eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getName() : null;
                Long labId = eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getId() : null;
                GlobalSearchResultItemDto item = new GlobalSearchResultItemDto(
                        "CALIBRATION_SCHEDULE", s.getId(), title, sub, labName, s.getStatus() != null ? s.getStatus().name() : null
                );
                item.setLaboratoryId(labId);
                item.setTargetUrl("/schedules?id=" + s.getId());
                return item;
            }).collect(Collectors.toList());
        }

        // 5. Calibration Records
        List<GlobalSearchResultItemDto> recItems = Collections.emptyList();
        if (!scope.isEmptyScope()) {
            List<CalibrationRecord> recList = scope.laboratoryIds() != null
                    ? calibrationRecordRepository.searchGlobalWithLaboratoryIds(q, scope.laboratoryIds(), limit10)
                    : calibrationRecordRepository.searchGlobal(q, limit10);

            recItems = recList.stream().map(r -> {
                Equipment eq = r.getEquipment();
                String title = eq != null ? eq.getName() : "Record #" + r.getId();
                String by = r.getCalibratorName() != null ? r.getCalibratorName() : (r.getCalibratedByType() != null ? r.getCalibratedByType().name() : "");
                String sub = "Calibrated: " + r.getCalibrationDate() + " | By: " + by;
                String labName = eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getName() : null;
                Long labId = eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getId() : null;
                GlobalSearchResultItemDto item = new GlobalSearchResultItemDto(
                        "CALIBRATION_RECORD", r.getId(), title, sub, labName, r.getResult() != null ? r.getResult().name() : null
                );
                item.setLaboratoryId(labId);
                item.setTargetUrl("/records?id=" + r.getId());
                return item;
            }).collect(Collectors.toList());
        }

        // 6. Calibration Documents
        List<GlobalSearchResultItemDto> docItems = Collections.emptyList();
        if (!scope.isEmptyScope()) {
            List<CalibrationDocument> docList = scope.laboratoryIds() != null
                    ? calibrationDocumentRepository.searchGlobalWithLaboratoryIds(q, scope.laboratoryIds(), limit10)
                    : calibrationDocumentRepository.searchGlobal(q, limit10);

            docItems = docList.stream().map(d -> {
                Equipment eq = d.getEquipment();
                String sub = (d.getCertificateNumber() != null ? "Cert: " + d.getCertificateNumber() + " | " : "") +
                        (eq != null ? eq.getName() : "");
                String labName = eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getName() : null;
                Long labId = eq != null && eq.getLaboratory() != null ? eq.getLaboratory().getId() : null;
                String validity = (d.getValidUntil() != null && d.getValidUntil().isBefore(today)) ? "EXPIRED" : "VALID";
                GlobalSearchResultItemDto item = new GlobalSearchResultItemDto(
                        "CALIBRATION_DOCUMENT", d.getId(), d.getDocumentName(), sub, labName, validity
                );
                item.setLaboratoryId(labId);
                item.setTargetUrl("/documents?id=" + d.getId());
                return item;
            }).collect(Collectors.toList());
        }

        // Aggregate counts & combined results
        int totalMatches = eqItems.size() + labItems.size() + staffItems.size() +
                schedItems.size() + recItems.size() + docItems.size();

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("EQUIPMENT", eqItems.size());
        counts.put("LABORATORY", labItems.size());
        counts.put("STAFF", staffItems.size());
        counts.put("CALIBRATION_SCHEDULE", schedItems.size());
        counts.put("CALIBRATION_RECORD", recItems.size());
        counts.put("CALIBRATION_DOCUMENT", docItems.size());

        List<GlobalSearchResultItemDto> allResults = new ArrayList<>();
        allResults.addAll(eqItems);
        allResults.addAll(labItems);
        allResults.addAll(staffItems);
        allResults.addAll(schedItems);
        allResults.addAll(recItems);
        allResults.addAll(docItems);

        List<GlobalSearchResultItemDto> cappedResults = allResults.stream().limit(50).collect(Collectors.toList());

        return new GlobalSearchResponseDto(
                q,
                totalMatches,
                counts,
                cappedResults,
                eqItems,
                labItems,
                staffItems,
                schedItems,
                recItems,
                docItems
        );
    }
}
