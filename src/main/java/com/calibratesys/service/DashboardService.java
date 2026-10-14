package com.calibratesys.service;

import com.calibratesys.dto.*;

import java.util.List;

public interface DashboardService {

    DashboardSummaryDto getSummary(Long laboratoryId);

    CalibrationStatusSummaryDto getCalibrationStatusSummary(Long laboratoryId);

    EquipmentStatusSummaryDto getEquipmentStatusSummary(Long laboratoryId);

    List<DashboardCalibrationItemDto> getUpcomingCalibrations(Long laboratoryId);

    List<DashboardCalibrationItemDto> getDueCalibrations(Long laboratoryId);

    List<DashboardCalibrationItemDto> getOverdueCalibrations(Long laboratoryId);

    List<RecentCalibrationDto> getRecentCalibrations(Long laboratoryId, int limit);

    List<LaboratoryDashboardSummaryDto> getLaboratorySummaries(Long laboratoryId, boolean overdueOnly);

    DashboardResponseDto getDashboard(Long laboratoryId);
}
