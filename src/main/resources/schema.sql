-- ===================================================================
-- Smart Laboratory Equipment Calibration and Compliance Management System
-- Normalized MySQL Relational Schema
-- ===================================================================

-- 1. College Entity
CREATE TABLE IF NOT EXISTS colleges (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    address VARCHAR(255),
    contact_email VARCHAR(100),
    contact_phone VARCHAR(25),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Staff / User Entity
CREATE TABLE IF NOT EXISTS staff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    staff_id VARCHAR(50) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(25),
    designation VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    system_role VARCHAR(30) NOT NULL DEFAULT 'LAB_STAFF',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_staff_email (email),
    INDEX idx_staff_role (system_role),
    INDEX idx_staff_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Laboratory Entity
CREATE TABLE IF NOT EXISTS laboratories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id BIGINT NOT NULL,
    lab_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    department VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_laboratories_college FOREIGN KEY (college_id) REFERENCES colleges (id) ON DELETE RESTRICT,
    INDEX idx_labs_college (college_id),
    INDEX idx_labs_code (lab_code),
    INDEX idx_labs_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Staff-Laboratory Many-to-Many Assignment Junction Table
CREATE TABLE IF NOT EXISTS laboratory_staff (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    laboratory_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    role_in_lab VARCHAR(50) NOT NULL DEFAULT 'STAFF',
    is_primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_staff_laboratory FOREIGN KEY (laboratory_id) REFERENCES laboratories (id) ON DELETE CASCADE,
    CONSTRAINT fk_lab_staff_staff FOREIGN KEY (staff_id) REFERENCES staff (id) ON DELETE CASCADE,
    CONSTRAINT uq_lab_staff UNIQUE (laboratory_id, staff_id),
    INDEX idx_lab_staff_lab (laboratory_id),
    INDEX idx_lab_staff_staff (staff_id),
    INDEX idx_lab_staff_role (role_in_lab)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Equipment Entity
CREATE TABLE IF NOT EXISTS equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    laboratory_id BIGINT NOT NULL,
    asset_tag VARCHAR(100) NOT NULL UNIQUE,
    serial_number VARCHAR(100),
    name VARCHAR(150) NOT NULL,
    model_number VARCHAR(100),
    manufacturer VARCHAR(100),
    purchase_date DATE,
    installation_date DATE,
    calibration_frequency_days INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPERATIONAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_equipment_laboratory FOREIGN KEY (laboratory_id) REFERENCES laboratories (id) ON DELETE RESTRICT,
    INDEX idx_equipment_lab (laboratory_id),
    INDEX idx_equipment_asset (asset_tag),
    INDEX idx_equipment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Calibration Schedule Entity
CREATE TABLE IF NOT EXISTS calibration_schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    scheduled_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    remarks TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_calib_sched_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,
    INDEX idx_calib_sched_equip (equipment_id),
    INDEX idx_calib_sched_due (due_date),
    INDEX idx_calib_sched_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Calibration Record / History Entity
CREATE TABLE IF NOT EXISTS calibration_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    schedule_id BIGINT NULL,
    calibration_date DATE NOT NULL,
    next_due_date DATE NOT NULL,
    calibrated_by_type VARCHAR(30) NOT NULL,
    calibrator_name VARCHAR(150) NOT NULL,
    result VARCHAR(30) NOT NULL,
    uncertainty_measurement VARCHAR(100),
    standards_used VARCHAR(255),
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_calib_rec_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE RESTRICT,
    CONSTRAINT fk_calib_rec_schedule FOREIGN KEY (schedule_id) REFERENCES calibration_schedules (id) ON DELETE SET NULL,
    INDEX idx_calib_rec_equip (equipment_id),
    INDEX idx_calib_rec_date (calibration_date),
    INDEX idx_calib_rec_result (result)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Calibration Certificate / Document Entity
CREATE TABLE IF NOT EXISTS calibration_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    calibration_record_id BIGINT NULL,
    document_name VARCHAR(255) NOT NULL,
    certificate_number VARCHAR(100),
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(50),
    file_size_bytes BIGINT,
    issue_date DATE,
    valid_until DATE,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_calib_doc_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,
    CONSTRAINT fk_calib_doc_record FOREIGN KEY (calibration_record_id) REFERENCES calibration_records (id) ON DELETE SET NULL,
    INDEX idx_calib_doc_equip (equipment_id),
    INDEX idx_calib_doc_record (calibration_record_id),
    INDEX idx_calib_doc_cert_no (certificate_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Notification Entity
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_staff_id BIGINT NOT NULL,
    laboratory_id BIGINT NULL,
    equipment_id BIGINT NULL,
    schedule_id BIGINT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_staff_id) REFERENCES staff (id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_laboratory FOREIGN KEY (laboratory_id) REFERENCES laboratories (id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_equipment FOREIGN KEY (equipment_id) REFERENCES equipment (id) ON DELETE CASCADE,
    CONSTRAINT fk_notif_schedule FOREIGN KEY (schedule_id) REFERENCES calibration_schedules (id) ON DELETE SET NULL,
    INDEX idx_notif_recipient (recipient_staff_id),
    INDEX idx_notif_read (recipient_staff_id, is_read),
    INDEX idx_notif_type (notification_type),
    INDEX idx_notif_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
