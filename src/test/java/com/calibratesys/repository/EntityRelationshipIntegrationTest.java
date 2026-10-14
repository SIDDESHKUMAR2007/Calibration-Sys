package com.calibratesys.repository;

import com.calibratesys.entity.*;
import com.calibratesys.entity.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EntityRelationshipIntegrationTest {

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private LaboratoryRepository laboratoryRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private LaboratoryStaffRepository laboratoryStaffRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private CalibrationScheduleRepository calibrationScheduleRepository;

    @Autowired
    private CalibrationRecordRepository calibrationRecordRepository;

    @Autowired
    private CalibrationDocumentRepository calibrationDocumentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Verify full entity relationship lifecycle: College -> Lab -> Staff (M:N) -> Equipment -> Schedule -> Record -> Doc -> Notification")
    void testFullEntityRelationshipLifecycle() {
        // 1. College
        College college = new College("ENG-COL-01", "Engineering Institute of Technology", "Main Campus", "info@engcol.edu", "555-0100");
        college = collegeRepository.save(college);
        assertNotNull(college.getId());

        // 2. Laboratory
        Laboratory lab = new Laboratory(college, "LAB-CHEM-101", "Advanced Analytical Chemistry Lab", "Chemical Engineering", "Block B, Room 204", LabStatus.ACTIVE);
        lab = laboratoryRepository.save(lab);
        assertNotNull(lab.getId());
        assertEquals(college.getId(), lab.getCollege().getId());

        // 3. Staff members
        Staff staff1 = new Staff("STF-001", "Alice", "Smith", "alice.smith@engcol.edu", "555-0101", "Lab Superintendent", "Chemical Engineering", StaffStatus.ACTIVE);
        Staff staff2 = new Staff("STF-002", "Bob", "Jones", "bob.jones@engcol.edu", "555-0102", "Senior Calibration Technician", "Instrumentation", StaffStatus.ACTIVE);
        staff1 = staffRepository.save(staff1);
        staff2 = staffRepository.save(staff2);
        assertNotNull(staff1.getId());
        assertNotNull(staff2.getId());

        // 4. Many-to-Many via LaboratoryStaff junction table
        LaboratoryStaff assignment1 = new LaboratoryStaff(lab, staff1, StaffLabRole.PRIMARY_INCHARGE, true);
        LaboratoryStaff assignment2 = new LaboratoryStaff(lab, staff2, StaffLabRole.LAB_TECHNICIAN, false);
        laboratoryStaffRepository.save(assignment1);
        laboratoryStaffRepository.save(assignment2);

        List<LaboratoryStaff> labStaffList = laboratoryStaffRepository.findByLaboratoryId(lab.getId());
        assertEquals(2, labStaffList.size());

        List<LaboratoryStaff> primaryInCharge = laboratoryStaffRepository.findByLaboratoryIdAndIsPrimaryContactTrue(lab.getId());
        assertEquals(1, primaryInCharge.size());
        assertEquals(staff1.getId(), primaryInCharge.get(0).getStaff().getId());

        // 5. Equipment in Laboratory
        Equipment equipment = new Equipment(
                lab,
                "EQ-SPECTRO-001",
                "SN-998822",
                "UV-Vis Spectrophotometer",
                "UV-2600i",
                "Shimadzu",
                LocalDate.now().minusYears(1),
                LocalDate.now().minusMonths(11),
                180,
                EquipmentStatus.OPERATIONAL
        );
        equipment = equipmentRepository.save(equipment);
        assertNotNull(equipment.getId());
        assertEquals(lab.getId(), equipment.getLaboratory().getId());

        // 6. Calibration Schedule (upcoming / future)
        LocalDate dueDate = LocalDate.now().plusDays(14);
        CalibrationSchedule schedule = new CalibrationSchedule(
                equipment,
                LocalDate.now().plusDays(10),
                dueDate,
                ScheduleStatus.UPCOMING,
                SchedulePriority.HIGH,
                "Semi-annual photometer calibration"
        );
        schedule = calibrationScheduleRepository.save(schedule);
        assertNotNull(schedule.getId());
        assertEquals(equipment.getId(), schedule.getEquipment().getId());

        // 7. Calibration Record (completed calibration history)
        CalibrationRecord record = new CalibrationRecord(
                equipment,
                schedule,
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusDays(14),
                CalibratedByType.EXTERNAL_VENDOR,
                "Precision Metrology Labs Inc.",
                CalibrationResult.PASSED,
                "±0.002 Abs",
                "Holmium Oxide Filter Master Standard",
                "Equipment calibrated and verified within tolerance."
        );
        record = calibrationRecordRepository.save(record);
        assertNotNull(record.getId());
        assertEquals(equipment.getId(), record.getEquipment().getId());

        // 8. Calibration Document / Certificate
        CalibrationDocument doc = new CalibrationDocument(
                equipment,
                record,
                "UV-Vis_Calibration_Certificate_2026.pdf",
                "CERT-NABL-2026-00451",
                "/uploads/certificates/cert_00451.pdf",
                "application/pdf",
                2048576L,
                LocalDate.now().minusMonths(6),
                LocalDate.now().plusDays(14)
        );
        doc = calibrationDocumentRepository.save(doc);
        assertNotNull(doc.getId());
        assertEquals(equipment.getId(), doc.getEquipment().getId());
        assertEquals(record.getId(), doc.getCalibrationRecord().getId());

        // 9. Notification (for responsible staff regarding upcoming calibration)
        Notification notification = new Notification(
                staff1,
                lab,
                equipment,
                schedule,
                "Calibration Due Soon: UV-Vis Spectrophotometer",
                "Equipment EQ-SPECTRO-001 in lab LAB-CHEM-101 is due for calibration on " + dueDate,
                NotificationType.CALIBRATION_UPCOMING,
                NotificationPriority.HIGH
        );
        notification = notificationRepository.save(notification);
        assertNotNull(notification.getId());
        assertEquals(staff1.getId(), notification.getRecipientStaff().getId());
        assertFalse(notification.isRead());

        // Verify retrieval by recipient staff
        List<Notification> unreadNotifs = notificationRepository.findByRecipientStaffIdAndIsReadFalseOrderByCreatedAtDesc(staff1.getId());
        assertEquals(1, unreadNotifs.size());
        assertEquals("Calibration Due Soon: UV-Vis Spectrophotometer", unreadNotifs.get(0).getTitle());
    }
}
