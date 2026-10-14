package com.calibratesys.service;

import com.calibratesys.dto.CalibrationScheduleRequestDto;
import com.calibratesys.dto.CalibrationScheduleResponseDto;
import com.calibratesys.dto.CalibrationScheduleUpdateRequestDto;
import com.calibratesys.entity.enums.SchedulePriority;
import com.calibratesys.entity.enums.ScheduleStatus;

import java.time.LocalDate;
import java.util.List;

public interface CalibrationScheduleService {

    CalibrationScheduleResponseDto createSchedule(CalibrationScheduleRequestDto requestDto);

    CalibrationScheduleResponseDto updateSchedule(Long id, CalibrationScheduleUpdateRequestDto updateDto);

    CalibrationScheduleResponseDto cancelSchedule(Long id);

    void deleteSchedule(Long id);

    CalibrationScheduleResponseDto getScheduleById(Long id);

    List<CalibrationScheduleResponseDto> getSchedules(Long equipmentId, Long laboratoryId,
                                                      ScheduleStatus status, SchedulePriority priority,
                                                      LocalDate fromDate, LocalDate toDate);

    List<CalibrationScheduleResponseDto> getSchedulesByEquipmentId(Long equipmentId);

    List<CalibrationScheduleResponseDto> getUpcomingSchedules();

    List<CalibrationScheduleResponseDto> getDueSchedules();

    List<CalibrationScheduleResponseDto> getOverdueSchedules();

    void refreshActiveScheduleStatuses();

    com.calibratesys.dto.PageResponseDto<CalibrationScheduleResponseDto> searchSchedules(
            Long equipmentId,
            Long laboratoryId,
            String assetTag,
            String status,
            String priority,
            LocalDate scheduledStartDate,
            LocalDate scheduledEndDate,
            LocalDate dueStartDate,
            LocalDate dueEndDate,
            String search,
            String sortBy,
            String sortDirection,
            int page,
            int size
    );
}
