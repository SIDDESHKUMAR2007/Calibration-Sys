package com.calibratesys.service.impl;

import com.calibratesys.entity.*;
import com.calibratesys.entity.enums.*;
import com.calibratesys.exception.ApiException;
import com.calibratesys.repository.*;
import com.calibratesys.service.DemoDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class DemoDataServiceImpl implements DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final CollegeRepository collegeRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final StaffRepository staffRepository;
    private final LaboratoryStaffRepository laboratoryStaffRepository;
    private final EquipmentRepository equipmentRepository;
    private final CalibrationScheduleRepository calibrationScheduleRepository;
    private final CalibrationRecordRepository calibrationRecordRepository;
    private final CalibrationDocumentRepository calibrationDocumentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${app.demo-data.enabled:true}")
    private boolean demoDataEnabled;

    @Value("${app.documents.storage-path:uploads/calibration-documents}")
    private String storagePathStr;

    @Autowired
    public DemoDataServiceImpl(JdbcTemplate jdbcTemplate,
                               CollegeRepository collegeRepository,
                               LaboratoryRepository laboratoryRepository,
                               StaffRepository staffRepository,
                               LaboratoryStaffRepository laboratoryStaffRepository,
                               EquipmentRepository equipmentRepository,
                               CalibrationScheduleRepository calibrationScheduleRepository,
                               CalibrationRecordRepository calibrationRecordRepository,
                               CalibrationDocumentRepository calibrationDocumentRepository,
                               NotificationRepository notificationRepository,
                               AuditLogRepository auditLogRepository,
                               PasswordEncoder passwordEncoder,
                               Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.collegeRepository = collegeRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.staffRepository = staffRepository;
        this.laboratoryStaffRepository = laboratoryStaffRepository;
        this.equipmentRepository = equipmentRepository;
        this.calibrationScheduleRepository = calibrationScheduleRepository;
        this.calibrationRecordRepository = calibrationRecordRepository;
        this.calibrationDocumentRepository = calibrationDocumentRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Map<String, Long> resetDemoData() {
        if (!demoDataEnabled) {
            throw new ApiException("Demo data reset is disabled by configuration (app.demo-data.enabled=false).", HttpStatus.FORBIDDEN);
        }

        log.info("Starting demonstration data reset to compact college-demo dataset...");

        // 1. Truncate tables cleanly with foreign key checks temporarily disabled
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE audit_logs");
        jdbcTemplate.execute("TRUNCATE TABLE calibration_documents");
        jdbcTemplate.execute("TRUNCATE TABLE calibration_records");
        jdbcTemplate.execute("TRUNCATE TABLE calibration_schedules");
        jdbcTemplate.execute("TRUNCATE TABLE notifications");
        jdbcTemplate.execute("TRUNCATE TABLE laboratory_staff");
        jdbcTemplate.execute("TRUNCATE TABLE equipment");
        jdbcTemplate.execute("TRUNCATE TABLE laboratories");
        jdbcTemplate.execute("TRUNCATE TABLE colleges");
        jdbcTemplate.execute("TRUNCATE TABLE staff");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");

        LocalDate today = LocalDate.now(clock);

        // 2. Populate College (exactly 1)
        College college = new College(
                "COL-001",
                "ABC Engineering College",
                "Block 1, Campus Road, Tech City",
                "contact@abc.edu",
                "+91-9876543210"
        );
        college = collegeRepository.save(college);

        // 3. Populate Laboratories (exactly 2)
        Laboratory labPhy = new Laboratory(
                college,
                "LAB-PHY-01",
                "Physics Laboratory",
                "Physics",
                "Block A - Room 101",
                LabStatus.ACTIVE
        );
        labPhy = laboratoryRepository.save(labPhy);

        Laboratory labEle = new Laboratory(
                college,
                "LAB-ELE-01",
                "Electronics Laboratory",
                "Electronics",
                "Block B - Room 201",
                LabStatus.ACTIVE
        );
        labEle = laboratoryRepository.save(labEle);

        // 4. Populate Staff (1 ADMIN + 4 Staff)
        Staff admin = new Staff(
                "ADMIN-001",
                "System",
                "Admin",
                "admin@calibratesys.edu",
                passwordEncoder.encode("Admin@12345"),
                "555-0199",
                "System Administrator",
                "Administration",
                SystemRole.ADMIN,
                StaffStatus.ACTIVE
        );
        admin = staffRepository.save(admin);

        Staff staff1 = new Staff(
                "STF-001",
                "Arun",
                "Kumar",
                "arun.kumar@calibratesys.edu",
                passwordEncoder.encode("Staff@12345"),
                "+91-9876543211",
                "Laboratory Incharge",
                "Physics",
                SystemRole.LAB_STAFF,
                StaffStatus.ACTIVE
        );
        staff1 = staffRepository.save(staff1);

        Staff staff2 = new Staff(
                "STF-002",
                "Priya",
                "Nair",
                "priya.nair@calibratesys.edu",
                passwordEncoder.encode("Staff@12345"),
                "+91-9876543212",
                "Lab Technician",
                "Physics",
                SystemRole.LAB_STAFF,
                StaffStatus.ACTIVE
        );
        staff2 = staffRepository.save(staff2);

        Staff staff3 = new Staff(
                "STF-003",
                "Rahul",
                "Kumar",
                "rahul.kumar@calibratesys.edu",
                passwordEncoder.encode("Staff@12345"),
                "+91-9876543213",
                "Calibration Coordinator",
                "Electronics",
                SystemRole.LAB_STAFF,
                StaffStatus.ACTIVE
        );
        staff3 = staffRepository.save(staff3);

        Staff staff4 = new Staff(
                "STF-004",
                "Anita",
                "Joseph",
                "anita.joseph@calibratesys.edu",
                passwordEncoder.encode("Staff@12345"),
                "+91-9876543214",
                "Lab Technician",
                "Electronics",
                SystemRole.LAB_STAFF,
                StaffStatus.ACTIVE
        );
        staff4 = staffRepository.save(staff4);

        // 5. Multi-Lab Assignments (Physics: STF-001, STF-002; Electronics: STF-003, STF-004, STF-002)
        LaboratoryStaff assignPhy1 = new LaboratoryStaff(labPhy, staff1, StaffLabRole.PRIMARY_INCHARGE, true);
        assignPhy1 = laboratoryStaffRepository.save(assignPhy1);

        LaboratoryStaff assignPhy2 = new LaboratoryStaff(labPhy, staff2, StaffLabRole.LAB_TECHNICIAN, false);
        assignPhy2 = laboratoryStaffRepository.save(assignPhy2);

        LaboratoryStaff assignEle1 = new LaboratoryStaff(labEle, staff3, StaffLabRole.CALIBRATION_COORDINATOR, true);
        assignEle1 = laboratoryStaffRepository.save(assignEle1);

        LaboratoryStaff assignEle2 = new LaboratoryStaff(labEle, staff4, StaffLabRole.LAB_TECHNICIAN, false);
        assignEle2 = laboratoryStaffRepository.save(assignEle2);

        // Cross-assignment: STF-002 belongs to both Physics and Electronics!
        LaboratoryStaff assignEleCross = new LaboratoryStaff(labEle, staff2, StaffLabRole.LAB_TECHNICIAN, false);
        assignEleCross = laboratoryStaffRepository.save(assignEleCross);

        // 6. Populate Equipment (exactly 6: 3 in Physics, 3 in Electronics)
        Equipment eqPhy1 = new Equipment(
                labPhy, "PHY-EQP-001", "SN-TEK-8812", "Digital Oscilloscope", "TBS1102B", "Tektronix",
                today.minusYears(2), today.minusYears(2), 180, EquipmentStatus.OPERATIONAL
        );
        eqPhy1 = equipmentRepository.save(eqPhy1);

        Equipment eqPhy2 = new Equipment(
                labPhy, "PHY-EQP-002", "SN-FLK-4421", "Digital Multimeter", "117", "Fluke",
                today.minusYears(1), today.minusYears(1), 365, EquipmentStatus.OPERATIONAL
        );
        eqPhy2 = equipmentRepository.save(eqPhy2);

        Equipment eqPhy3 = new Equipment(
                labPhy, "PHY-EQP-003", "SN-SHM-9931", "Electronic Balance", "BL-220H", "Shimadzu",
                today.minusYears(3), today.minusYears(3), 90, EquipmentStatus.CALIBRATION_DUE
        );
        eqPhy3 = equipmentRepository.save(eqPhy3);

        Equipment eqEle1 = new Equipment(
                labEle, "ELE-EQP-001", "SN-KEY-1104", "Function Generator", "33500B", "Keysight",
                today.minusYears(2), today.minusYears(2), 180, EquipmentStatus.OPERATIONAL
        );
        eqEle1 = equipmentRepository.save(eqEle1);

        Equipment eqEle2 = new Equipment(
                labEle, "ELE-EQP-002", "SN-RGL-6652", "Digital Storage Oscilloscope", "DS1054Z", "Rigol",
                today.minusYears(2), today.minusYears(2), 90, EquipmentStatus.OUT_OF_CALIBRATION
        );
        eqEle2 = equipmentRepository.save(eqEle2);

        Equipment eqEle3 = new Equipment(
                labEle, "ELE-EQP-003", "SN-KEY-7719", "DC Power Supply", "E3631A", "Keysight",
                today.minusYears(1), today.minusYears(1), 365, EquipmentStatus.UNDER_MAINTENANCE
        );
        eqEle3 = equipmentRepository.save(eqEle3);

        // 7. Populate Calibration Schedules (6 schedules: 2 UPCOMING, 1 DUE, 1 OVERDUE, 2 COMPLETED)
        CalibrationSchedule sch1 = new CalibrationSchedule(
                eqPhy1, today.plusDays(15), today.plusDays(15),
                ScheduleStatus.UPCOMING, SchedulePriority.MEDIUM,
                "Semi-annual oscilloscope calibration check"
        );
        sch1 = calibrationScheduleRepository.save(sch1);

        CalibrationSchedule sch2 = new CalibrationSchedule(
                eqEle1, today.plusDays(25), today.plusDays(25),
                ScheduleStatus.UPCOMING, SchedulePriority.LOW,
                "Routine waveform generator calibration check"
        );
        sch2 = calibrationScheduleRepository.save(sch2);

        CalibrationSchedule sch3 = new CalibrationSchedule(
                eqPhy3, today, today,
                ScheduleStatus.DUE, SchedulePriority.HIGH,
                "High precision weight standard recalibration due today"
        );
        sch3 = calibrationScheduleRepository.save(sch3);

        CalibrationSchedule sch4 = new CalibrationSchedule(
                eqEle2, today.minusDays(10), today.minusDays(5),
                ScheduleStatus.OVERDUE, SchedulePriority.CRITICAL,
                "Overdue calibration - instrument quarantined until serviced"
        );
        sch4 = calibrationScheduleRepository.save(sch4);

        CalibrationSchedule sch5 = new CalibrationSchedule(
                eqPhy2, today.minusMonths(2), today.minusMonths(2),
                ScheduleStatus.COMPLETED, SchedulePriority.MEDIUM,
                "Annual multimeter calibration completed successfully"
        );
        sch5 = calibrationScheduleRepository.save(sch5);

        CalibrationSchedule sch6 = new CalibrationSchedule(
                eqEle3, today.minusMonths(4), today.minusMonths(4),
                ScheduleStatus.COMPLETED, SchedulePriority.MEDIUM,
                "Power supply calibration completed by internal technician"
        );
        sch6 = calibrationScheduleRepository.save(sch6);

        // 8. Populate Calibration Records (4 records)
        CalibrationRecord rec1 = new CalibrationRecord(
                eqPhy2, sch5, today.minusMonths(2), today.minusMonths(2).plusDays(365),
                CalibratedByType.INTERNAL, "Dr. Arun Kumar",
                CalibrationResult.PASSED, "± 0.05%",
                "NIST Traceable Voltage Standard VS-101",
                "Annual calibration completed within specification."
        );
        rec1 = calibrationRecordRepository.save(rec1);

        CalibrationRecord rec2 = new CalibrationRecord(
                eqPhy1, null, today.minusMonths(5), today.plusDays(15),
                CalibratedByType.EXTERNAL_VENDOR, "Tektronix Authorized Service Center",
                CalibrationResult.PASSED, "± 0.1% bandwidth accuracy",
                "ISO/IEC 17025 Reference Waveform Calibrator",
                "Channel 1 and 2 bandwidth and rise time verified."
        );
        rec2 = calibrationRecordRepository.save(rec2);

        CalibrationRecord rec3 = new CalibrationRecord(
                eqEle1, null, today.minusMonths(5), today.plusDays(25),
                CalibratedByType.EXTERNAL_VENDOR, "Keysight Calibration Labs",
                CalibrationResult.PASSED_WITH_CONDITIONS, "± 1 ppm frequency stability",
                "Rubidium Atomic Frequency Standard",
                "Slight drift observed at 30MHz; compensated within tolerance."
        );
        rec3 = calibrationRecordRepository.save(rec3);

        CalibrationRecord rec4 = new CalibrationRecord(
                eqEle3, sch6, today.minusMonths(4), today.minusMonths(4).plusDays(365),
                CalibratedByType.INTERNAL, "Rahul Kumar",
                CalibrationResult.PASSED, "± 0.02 V / ± 0.01 A",
                "Fluke 8846A 6.5 Digit Precision Multimeter",
                "Voltage and current limit calibration verified."
        );
        rec4 = calibrationRecordRepository.save(rec4);

        // 9. Manage Physical Files and Populate Calibration Documents (3 records)
        byte[] validPdfBytes = ("%PDF-1.4\n" +
                "1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n" +
                "2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n" +
                "3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj\n" +
                "xref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000102 00000 n \n" +
                "trailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF\n").getBytes(StandardCharsets.UTF_8);

        List<String> validFiles = List.of("CAL-CERT-001.pdf", "CAL-CERT-002.pdf", "CAL-CERT-003.pdf");
        Path storageDir = Paths.get(storagePathStr).toAbsolutePath().normalize();

        try {
            Files.createDirectories(storageDir);

            // Clean orphaned development files
            try (Stream<Path> stream = Files.list(storageDir)) {
                stream.forEach(file -> {
                    String fileName = file.getFileName().toString();
                    if (!validFiles.contains(fileName)) {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException e) {
                            log.warn("Could not delete orphaned file: {}", file, e);
                        }
                    }
                });
            }

            // Write valid PDF certificate files
            for (String certFile : validFiles) {
                Path target = storageDir.resolve(certFile);
                Files.write(target, validPdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Error managing certificate storage during reset: {}", e.getMessage(), e);
        }

        CalibrationDocument doc1 = new CalibrationDocument(
                eqPhy2, rec1, "Multimeter Calibration Certificate", "CAL-CERT-001",
                "CAL-CERT-001.pdf", "application/pdf", (long) validPdfBytes.length,
                today.minusMonths(2), today.minusMonths(2).plusDays(365)
        );
        doc1 = calibrationDocumentRepository.save(doc1);

        CalibrationDocument doc2 = new CalibrationDocument(
                eqPhy1, rec2, "Oscilloscope NIST Calibration Certificate", "CAL-CERT-002",
                "CAL-CERT-002.pdf", "application/pdf", (long) validPdfBytes.length,
                today.minusMonths(5), today.plusDays(15)
        );
        doc2 = calibrationDocumentRepository.save(doc2);

        CalibrationDocument doc3 = new CalibrationDocument(
                eqEle1, rec3, "Function Generator ISO Calibration Certificate", "CAL-CERT-003",
                "CAL-CERT-003.pdf", "application/pdf", (long) validPdfBytes.length,
                today.minusMonths(5), today.plusDays(25)
        );
        doc3 = calibrationDocumentRepository.save(doc3);

        // 10. Populate Notifications (5 records)
        Notification n1 = new Notification(
                staff3, labEle, eqEle2, sch4,
                "Calibration Overdue Alert",
                "Digital Storage Oscilloscope (ELE-EQP-002) is overdue for calibration since 5 days ago. Immediate servicing required.",
                NotificationType.CALIBRATION_OVERDUE, NotificationPriority.CRITICAL
        );
        notificationRepository.save(n1);

        Notification n2 = new Notification(
                staff1, labPhy, eqPhy3, sch3,
                "Calibration Due Today",
                "Electronic Balance (PHY-EQP-003) is due for calibration today. Please verify analytical calibration standard.",
                NotificationType.CALIBRATION_DUE, NotificationPriority.HIGH
        );
        notificationRepository.save(n2);

        Notification n3 = new Notification(
                staff2, labPhy, eqPhy1, sch1,
                "Upcoming Calibration Reminder",
                "Digital Oscilloscope (PHY-EQP-001) scheduled calibration is approaching in 15 days.",
                NotificationType.CALIBRATION_UPCOMING, NotificationPriority.MEDIUM
        );
        notificationRepository.save(n3);

        Notification n4 = new Notification(
                staff3, labEle, null, null,
                "Laboratory Compliance Review",
                "Laboratory compliance rating requires review due to 1 overdue instrument in Electronics Laboratory.",
                NotificationType.COMPLIANCE_ALERT, NotificationPriority.HIGH
        );
        notificationRepository.save(n4);

        Notification n5 = new Notification(
                staff1, labPhy, null, null,
                "Annual Laboratory Audit Scheduled",
                "Annual academic laboratory quality compliance inspection will be conducted next month for Physics Laboratory.",
                NotificationType.GENERAL_NOTICE, NotificationPriority.LOW
        );
        notificationRepository.save(n5);

        // 11. Populate Audit Logs (11 authentic records)
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.LOGIN_SUCCESS, AuditEntityType.AUTHENTICATION, admin.getId(), "Administrator authenticated successfully", null, null, "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.COLLEGE_CREATED, AuditEntityType.COLLEGE, college.getId(), "College 'ABC Engineering College' (COL-001) registered", null, "{\"code\":\"COL-001\"}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.LABORATORY_CREATED, AuditEntityType.LABORATORY, labPhy.getId(), "Laboratory 'Physics Laboratory' (LAB-PHY-01) created", null, "{\"code\":\"LAB-PHY-01\"}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.LABORATORY_CREATED, AuditEntityType.LABORATORY, labEle.getId(), "Laboratory 'Electronics Laboratory' (LAB-ELE-01) created", null, "{\"code\":\"LAB-ELE-01\"}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.STAFF_CREATED, AuditEntityType.STAFF, staff1.getId(), "Staff member 'Dr. Arun Kumar' (STF-001) registered", null, "{\"staffId\":\"STF-001\"}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.STAFF_ASSIGNED_TO_LAB, AuditEntityType.LABORATORY_STAFF, assignPhy1.getId(), "Staff STF-001 assigned as PRIMARY_INCHARGE to LAB-PHY-01", null, "{\"lab\":\"LAB-PHY-01\"}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.STAFF_ASSIGNED_TO_LAB, AuditEntityType.LABORATORY_STAFF, assignEleCross.getId(), "Staff STF-002 assigned as LAB_TECHNICIAN to LAB-ELE-01 (Multi-lab)", null, "{\"lab\":\"LAB-ELE-01\"}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.EQUIPMENT_CREATED, AuditEntityType.EQUIPMENT, eqPhy1.getId(), "Equipment 'Digital Oscilloscope' (PHY-EQP-001) registered", null, "{\"assetTag\":\"PHY-EQP-001\"}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.CALIBRATION_SCHEDULE_CREATED, AuditEntityType.CALIBRATION_SCHEDULE, sch1.getId(), "Calibration schedule created for PHY-EQP-001", null, "{\"scheduleId\":" + sch1.getId() + "}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.CALIBRATION_RECORD_CREATED, AuditEntityType.CALIBRATION_RECORD, rec1.getId(), "Calibration record logged for PHY-EQP-002 with result PASSED", null, "{\"recordId\":" + rec1.getId() + "}", "127.0.0.1"));
        auditLogRepository.save(new AuditLog(admin, admin.getEmail(), AuditAction.CALIBRATION_DOCUMENT_UPLOADED, AuditEntityType.CALIBRATION_DOCUMENT, doc1.getId(), "Calibration document 'CAL-CERT-001.pdf' uploaded", null, "{\"docId\":" + doc1.getId() + "}", "127.0.0.1"));

        log.info("Demonstration dataset reset complete.");
        return getEntityCounts();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getEntityCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("colleges", collegeRepository.count());
        counts.put("laboratories", laboratoryRepository.count());
        counts.put("staff", staffRepository.count());
        counts.put("laboratory_staff", laboratoryStaffRepository.count());
        counts.put("equipment", equipmentRepository.count());
        counts.put("calibration_schedules", calibrationScheduleRepository.count());
        counts.put("calibration_records", calibrationRecordRepository.count());
        counts.put("calibration_documents", calibrationDocumentRepository.count());
        counts.put("notifications", notificationRepository.count());
        counts.put("audit_logs", auditLogRepository.count());
        return counts;
    }
}
