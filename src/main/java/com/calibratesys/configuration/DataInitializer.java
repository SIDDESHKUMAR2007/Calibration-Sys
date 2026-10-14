package com.calibratesys.configuration;

import com.calibratesys.entity.Staff;
import com.calibratesys.entity.enums.StaffStatus;
import com.calibratesys.entity.enums.SystemRole;
import com.calibratesys.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Development initialization component for provisioning an initial administrator account safely.
 * Controlled via application properties (app.security.init-admin.*).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.init-admin.enabled:true}")
    private boolean initAdminEnabled;

    @Value("${app.security.init-admin.email:admin@calibratesys.edu}")
    private String adminEmail;

    @Value("${app.security.init-admin.password:Admin@12345}")
    private String adminPassword;

    @Value("${app.security.init-admin.staff-id:ADMIN-001}")
    private String adminStaffId;

    @Value("${app.security.init-admin.first-name:System}")
    private String adminFirstName;

    @Value("${app.security.init-admin.last-name:Admin}")
    private String adminLastName;

    @Value("${app.security.init-admin.designation:System Administrator}")
    private String adminDesignation;

    @Value("${app.security.init-admin.department:Administration}")
    private String adminDepartment;

    @Autowired
    public DataInitializer(StaffRepository staffRepository, PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!initAdminEnabled) {
            log.info("Initial admin provisioning is disabled by configuration");
            return;
        }

        boolean exists = staffRepository.existsByEmail(adminEmail) || staffRepository.existsByStaffId(adminStaffId);

        if (!exists) {
            Staff admin = new Staff(
                    adminStaffId,
                    adminFirstName,
                    adminLastName,
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    "555-0199",
                    adminDesignation,
                    adminDepartment,
                    SystemRole.ADMIN,
                    StaffStatus.ACTIVE
            );
            staffRepository.save(admin);
            log.info("Initial administrator account provisioned: staffId={}, email={}, role=ADMIN", adminStaffId, adminEmail);
        } else {
            log.debug("Administrator account already exists for email: {}", adminEmail);
        }
    }
}
