package com.calibratesys.service;

import com.calibratesys.dto.CalibrationRecordRequestDto;
import com.calibratesys.dto.CalibrationRecordResponseDto;
import com.calibratesys.entity.enums.CalibratedByType;
import com.calibratesys.entity.enums.CalibrationResult;

import java.time.LocalDate;
import java.util.List;

public interface CalibrationRecordService {

    CalibrationRecordResponseDto createRecord(CalibrationRecordRequestDto requestDto);

    CalibrationRecordResponseDto getRecordById(Long id);

    List<CalibrationRecordResponseDto> getRecords(Long equipmentId, Long laboratoryId,
                                                 CalibrationResult result, CalibratedByType calibratedByType,
                                                 LocalDate fromDate, LocalDate toDate);

    List<CalibrationRecordResponseDto> getRecordsByEquipmentId(Long equipmentId);

    List<CalibrationRecordResponseDto> getRecordsByScheduleId(Long scheduleId);

    com.calibratesys.dto.PageResponseDto<CalibrationRecordResponseDto> searchRecords(
            Long equipmentId,
            Long laboratoryId,
            CalibrationResult result,
            CalibratedByType calibratedByType,
            String calibratorName,
            LocalDate calibrationStartDate,
            LocalDate calibrationEndDate,
            LocalDate dueStartDate,
            LocalDate dueEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    );
}
