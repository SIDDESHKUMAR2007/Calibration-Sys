# Smart Laboratory Equipment Calibration and Compliance Management System

A robust, enterprise-grade Spring Boot application designed to manage equipment inventories, calibration schedules, immutable calibration histories, certificate documents, multi-laboratory scoping, role-based access control, compliance monitoring, and automated audit trailing for collegiate and institutional laboratories.

---

## 1. System Requirements

- **Java Development Kit (JDK):** Version 21 (LTS) or higher
- **Build Tool:** Apache Maven 3.9+ (or included Maven Wrapper `./mvnw` / `mvnw.cmd`)
- **Database:** MySQL Server 8.0+
- **Operating System:** Windows, Linux, or macOS

---

## 2. Architecture & Modules

The system follows a strict layered architecture:
`DTO` ↔ `Controller` ↔ `Service` (Business Rules & Scoping) ↔ `Repository` ↔ `MySQL Database`

### Core Modules:
- **Authentication & Security:** Stateless JWT tokens, BCrypt password hashing, Spring Security 6 filter chain, dynamic CORS, HTTP security headers (`X-Content-Type-Options: nosniff`, `X-Frame-Options: SAMEORIGIN`).
- **Organization Hierarchy:** Colleges → Multi-Laboratory scoping → Staff assignments (`LaboratoryStaff` with role assignments like `PRIMARY_INCHARGE`, `LAB_TECHNICIAN`, `SAFETY_OFFICER`, etc.).
- **Equipment Inventory:** Asset tracking, serial numbers, manufacturers, operational statuses, calibration frequency intervals.
- **Calibration Scheduling:** Proactive scheduling, deterministic date calculations using an injectable `Clock`, status computation (`SCHEDULED`, `UPCOMING`, `DUE`, `OVERDUE`, `COMPLETED`, `CANCELLED`).
- **Calibration Records:** Immutable event history recording actual calibration events, results (`PASSED`, `FAILED`), technicians, and standards used.
- **Certificate & Document Management:** Secure upload and storage of calibration certificates (PDF/PNG/JPEG), MIME and magic-number validation, directory traversal prevention (`UUID` storage filenames), sanitized download headers.
- **In-App Notifications:** Real-time event notifications targeted to laboratory-assigned staff for overdue/upcoming calibrations.
- **Compliance Dashboard:** Aggregated metrics, calibration compliance percentages, equipment operational ratios, recent activity feed.
- **Immutable Audit Trail:** Append-only activity log tracking who did what, on which entity, when, outcome, and sanitized before/after state diffs (with automatic password and sensitive token masking).
- **Advanced Search & Global Search:** Multi-entity unified search across equipment, laboratories, staff, schedules, records, and documents with laboratory scoping.
- **Reporting & CSV Export:** Compliance summaries, overdue lists, laboratory audit packs, CSV exports with formula injection mitigations (`=` / `@` / `+` / `-` escaping).

---

## 3. Configuration & Profiles

Configuration is split into two environments:

### A. Development (`application.properties`)
- Default active profile.
- Schema auto-update (`spring.jpa.hibernate.ddl-auto=update`).
- Pre-provisions a default administrator on initial boot if not present.
- Configured for local testing against MySQL database `calibrate_sys_db`.

### B. Production (`application-prod.properties`)
- Activated using `--spring.profiles.active=prod` or `SPRING_PROFILES_ACTIVE=prod`.
- Schema validation only (`spring.jpa.hibernate.ddl-auto=validate`) — preventing accidental schema drops or alterations.
- Dev admin auto-provisioning **disabled** (`app.security.init-admin.enabled=false`).
- Enforces strict external configuration via environment variables.
- Requires high-entropy JWT secret (minimum 32 characters / 256 bits) with fail-fast application startup checks.

### Environment Variables Template (`.env.example`)
Refer to [.env.example](file:///.env.example) for all supported environment variables:
```bash
# Database Credentials
DB_HOST=localhost
DB_PORT=3306
DB_NAME=calibrate_sys_db
DB_USERNAME=calib_admin
DB_PASSWORD=YourStrongDatabasePasswordHere

# Security & JWT
JWT_SECRET=production-grade-random-secret-key-at-least-256-bits-long-change-me
JWT_EXPIRATION_MS=86400000

# CORS Allowed Origins
APP_CORS_ALLOWED_ORIGINS=https://calibrate.yourinstitution.edu

# Document Storage
DOCUMENT_STORAGE_PATH=/var/calibratesys/uploads/calibration-documents
DOCUMENTS_MAX_FILE_SIZE=10485760
```

---

## 4. Running the Application

### Running in Development Mode
```powershell
# Using the Maven Wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Using Linux/macOS
./mvnw spring-boot:run
```

Access the application in your browser at:
`http://localhost:8080/`

### Running in Production Mode
```bash
java -jar -Dspring.profiles.active=prod target/calibrate-sys-0.0.1-SNAPSHOT.jar
```

---

## 5. Development Administrator Provisioning

When running with the default development profile (`app.security.init-admin.enabled=true`), the system automatically provisions an administrator account on first boot:
- **Email:** `admin@calibratesys.edu`
- **Password:** `Admin@12345`
- **Staff ID:** `ADMIN-001`
- **Role:** `ADMIN`

> [!WARNING]
> This account provisioning is automatically disabled in the `prod` profile (`app.security.init-admin.enabled=false`). For production systems, provision initial administrative credentials securely through administrative SQL scripts with BCrypt hashed passwords.

---

## 6. Running Tests

The test suite covers full integration testing, entity relationships, security filters, role authorization, IDOR boundaries, audit logging, CSV formula escaping, and input validation:

```powershell
# Run the complete integration test suite
.\mvnw.cmd test

# Run a specific test class
.\mvnw.cmd test -Dtest=SecurityHardeningTest
```

**Test Verification Status:**
- Total Tests: 211
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: `BUILD SUCCESS` (Executable archive: `target/calibrate-sys-0.0.1-SNAPSHOT.jar`)

---

## 7. Production Deployment Guidelines

1. **Database Schema Migration:**
   - Use Flyway or Liquibase or execute verified schema DDL scripts prior to starting the production application.
   - Set `HIBERNATE_DDL_AUTO=validate` (default in `prod` profile).

2. **TLS / Reverse Proxy Configuration:**
   - Deploy behind a reverse proxy (e.g., NGINX, Apache, or AWS ALB) terminating HTTPS.
   - Configure `server.forward-headers-strategy=native` if behind a proxy.

3. **CORS Restrictions:**
   - Supply the exact production domain(s) in `APP_CORS_ALLOWED_ORIGINS` (e.g. `https://calibrate.yourcollege.edu`).

4. **File System Permissions:**
   - Ensure the user running the Spring Boot process has read/write permissions to `DOCUMENT_STORAGE_PATH` (e.g., `/var/calibratesys/uploads/calibration-documents`).
   - Store document uploads outside the web server's static root to prevent direct web execution.

5. **JVM Flags:**
   - Recommended flags: `-Xms1g -Xmx2g -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError`

---

## 8. Backup & Recovery Documentation

### A. Database Backup
Perform regular automated backups using `mysqldump`:
```bash
mysqldump -u calib_admin -p --single-transaction --quick --routines --triggers calibrate_sys_db | gzip > /backups/db/calibrate_sys_$(date +%Y%m%d_%H%M%S).sql.gz
```
To restore:
```bash
gunzip < /backups/db/calibrate_sys_20261014_120000.sql.gz | mysql -u calib_admin -p calibrate_sys_db
```

### B. Uploaded Documents Backup
Calibration certificates stored under `DOCUMENT_STORAGE_PATH` must be backed up concurrently with database snapshots:
```bash
tar -czf /backups/docs/calibration_documents_$(date +%Y%m%d_%H%M%S).tar.gz /var/calibratesys/uploads/calibration-documents/
```

### C. Configuration & Key Backup
- Back up the environment file (`.env`) containing `JWT_SECRET` and database credentials in an encrypted secrets vault.

---

## 9. Security Verification Checklist

- [x] Passwords hashed with BCrypt (strength 10).
- [x] Stateless JWT authentication with signed claims.
- [x] Fail-fast JWT secret validation in production (< 32 chars rejected).
- [x] Role separation: `ADMIN` vs `LAB_STAFF` system roles.
- [x] Multi-laboratory data scoping: `LAB_STAFF` can only access assigned laboratories.
- [x] Cross-laboratory IDOR protection across equipment, schedules, records, and documents.
- [x] File upload security: MIME verification, magic byte validation, UUID storage naming, path traversal prevention (`normalize()`).
- [x] Audit trail immutability: Append-only table without update or delete operations; sensitive credential masking.
- [x] Failed login attempt recording in audit logs without leaking plaintext passwords.
- [x] Pagination bounds enforcement (`page >= 0`, `size` between 1 and 100).
- [x] CSV formula injection protection (escapes `=`, `@`, `+`, `-`).
- [x] HTTP security headers: `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`.
- [x] Sanitized API error responses: stack traces and internal SQL suppressed from client responses.
- [x] Actuator / health endpoint sanitization: credentials and database connection parameters stripped.

---

## 10. API Inventory

| Module | Method | Endpoint | Authorization | Description |
|---|---|---|---|---|
| **Auth** | `POST` | `/api/auth/login` | Public | Authenticates credentials and issues signed JWT |
| | `GET` | `/api/auth/me` | Authenticated | Returns current authenticated staff details and lab assignments |
| **Colleges** | `GET` | `/api/colleges` | Authenticated | Lists all colleges (paginated, sortable) |
| | `GET` | `/api/colleges/{id}` | Authenticated | Retrieves college details |
| | `POST` | `/api/colleges` | `ADMIN` | Creates new college |
| | `PUT` | `/api/colleges/{id}` | `ADMIN` | Updates college details |
| | `DELETE` | `/api/colleges/{id}` | `ADMIN` | Soft-deletes / deletes college |
| **Laboratories** | `GET` | `/api/laboratories` | Authenticated | Lists laboratories (scoped to assigned labs for `LAB_STAFF`) |
| | `GET` | `/api/laboratories/{id}` | Authenticated | Retrieves lab details (scoped) |
| | `POST` | `/api/laboratories` | `ADMIN` | Creates new laboratory |
| | `PUT` | `/api/laboratories/{id}` | `ADMIN` | Updates laboratory metadata |
| | `DELETE` | `/api/laboratories/{id}` | `ADMIN` | Deletes laboratory |
| **Staff & Assignments** | `GET` | `/api/staff` | `ADMIN` | Lists staff members |
| | `POST` | `/api/staff` | `ADMIN` | Registers new staff user |
| | `GET` | `/api/staff/{id}` | `ADMIN` or Self | Retrieves staff record |
| | `PUT` | `/api/staff/{id}` | `ADMIN` | Updates staff profile |
| | `POST` | `/api/laboratory-staff` | `ADMIN` | Assigns staff member to laboratory with specific role |
| | `GET` | `/api/laboratory-staff/laboratory/{labId}` | Authenticated | Retrieves staff assigned to a specific laboratory |
| | `DELETE` | `/api/laboratory-staff/{id}` | `ADMIN` | Revokes staff laboratory assignment |
| **Equipment** | `GET` | `/api/equipment` | Authenticated | Lists equipment (scoped to assigned labs for `LAB_STAFF`) |
| | `GET` | `/api/equipment/{id}` | Authenticated | Retrieves equipment details (scoped) |
| | `POST` | `/api/equipment` | `ADMIN` | Registers new equipment |
| | `PUT` | `/api/equipment/{id}` | `ADMIN` | Updates equipment metadata |
| | `DELETE` | `/api/equipment/{id}` | `ADMIN` | Deletes equipment |
| **Schedules** | `GET` | `/api/calibration-schedules` | Authenticated | Lists calibration schedules (scoped) |
| | `GET` | `/api/calibration-schedules/{id}` | Authenticated | Retrieves calibration schedule details |
| | `POST` | `/api/calibration-schedules` | `ADMIN` | Plans proactive calibration schedule |
| | `PUT` | `/api/calibration-schedules/{id}` | `ADMIN` | Updates calibration schedule |
| **Records** | `GET` | `/api/calibration-records` | Authenticated | Lists historical calibration records (scoped) |
| | `GET` | `/api/calibration-records/{id}` | Authenticated | Retrieves calibration record details |
| | `POST` | `/api/calibration-records` | Authenticated | Records calibration event; updates schedule to `COMPLETED` |
| **Documents** | `POST` | `/api/calibration-documents/upload` | Authenticated | Uploads calibration certificate document (multipart) |
| | `GET` | `/api/calibration-documents/{id}` | Authenticated | Retrieves document metadata |
| | `GET` | `/api/calibration-documents/{id}/download` | Authenticated | Downloads stored certificate file |
| | `DELETE` | `/api/calibration-documents/{id}` | Authenticated | Deletes document (blocked if linked to calibration record) |
| **Notifications** | `GET` | `/api/notifications` | Authenticated | Lists user's notifications (scoped strictly to self) |
| | `PATCH` | `/api/notifications/{id}/read` | Authenticated | Marks specific notification as read |
| | `PATCH` | `/api/notifications/read-all` | Authenticated | Marks all unread notifications as read |
| | `POST` | `/api/notifications/generate-calibration-alerts` | `ADMIN` | Triggers alert generation for upcoming/due/overdue calibrations |
| **Dashboard** | `GET` | `/api/dashboard` | Authenticated | Retrieves aggregated compliance metrics (scoped) |
| **Search** | `GET` | `/api/search/global` | Authenticated | Unified global search across all entities (scoped) |
| | `GET` | `/api/equipment/search` | Authenticated | Filtered equipment search |
| **Reports** | `GET` | `/api/reports/summary` | Authenticated | Returns structured compliance summary |
| | `GET` | `/api/reports/export/csv` | Authenticated | Streams sanitized CSV report |
| **Audit Logs** | `GET` | `/api/audit-logs` | `ADMIN` | Queries immutable audit log entries |
| **Health** | `GET` | `/api/health` | Public | System status and database connectivity check |

---

## 11. Final Feature Matrix

| Feature Module | Functional Capabilities | Authorization Rule | Scope Enforcement |
|---|---|---|---|
| **Multi-Tenancy Scoping** | Multi-college, multi-laboratory support | RBAC (`ADMIN`, `LAB_STAFF`) | `LAB_STAFF` queries filtered by assigned laboratories; IDOR protected |
| **Staff Lab Assignments** | Many-to-many relationship with laboratory roles | `ADMIN` assigns | Roles: `PRIMARY_INCHARGE`, `CALIBRATION_COORDINATOR`, `LAB_TECHNICIAN`, `SAFETY_OFFICER`, `STAFF` |
| **Equipment Lifecycle** | Model, serial, calibration frequency, status | `ADMIN` write, Staff read | Filtered by laboratory assignment |
| **Deterministic Scheduling** | Statuses: `SCHEDULED`, `UPCOMING`, `DUE`, `OVERDUE`, `COMPLETED`, `CANCELLED` | `ADMIN` write, Staff read | Date calculations via injectable clock; auto-advances on completion |
| **Immutable Records** | Calibration pass/fail, standards, results | `ADMIN` + assigned staff | Prevents alteration after submission |
| **Certificate Vault** | File validation (PDF/PNG/JPEG), magic bytes | Authenticated scoped | UUID storage filenames; deletion blocked if linked to active record |
| **Alert Resolution** | Smart recipient routing (Primary In-charge → Coordinator → Active Staff) | Automated trigger | Strict recipient isolation (Staff A cannot read Staff B alerts) |
| **Compliance Dashboard** | Operational ratios, compliance %, due counters | Scoped by role | `ADMIN` sees institution; `LAB_STAFF` sees assigned labs |
| **Audit History** | Append-only logging with before/after state diffs | `ADMIN` read-only | Sensitive parameters (passwords, tokens) masked |
| **Advanced Search** | Unified multi-entity query with pagination | Scoped by role | Filters out unassigned laboratory entities for `LAB_STAFF` |
| **Export & Reporting** | Summary reports, Overdue listings, CSV exports | Scoped by role | CSV formula injection protection (`=`, `@`, `+`, `-` sanitization) |

---

## 12. 10–15 Minute Demonstration Workflow

Follow these steps to demonstrate the end-to-end functionality of the system:

1. **System Health Check (1 min):**
   - Access `http://localhost:8080/api/health` in browser or curl.
   - Confirm status: `UP` and database status: `CONNECTED`.
2. **Administrator Sign-In (1 min):**
   - Navigate to `http://localhost:8080/`.
   - Log in with default admin credentials (`admin@calibratesys.edu` / `Admin@12345`).
   - Observe JWT token saved in session storage and dashboard loaded.
3. **Establish Organization Hierarchy (2 mins):**
   - Navigate to **Laboratories** and verify existing laboratories or create "Biotechnology Research Lab".
   - Navigate to **Staff** and create a staff user: `tech@calibratesys.edu` (Role: `LAB_STAFF`).
   - Assign the staff user to "Biotechnology Research Lab" with role `PRIMARY_INCHARGE`.
4. **Register Equipment & Schedule Calibration (2 mins):**
   - In **Equipment**, add "Autoclave Unit #4" (Serial: `AC-9081`, Calibration Frequency: `180` days).
   - In **Schedules**, create a calibration schedule for this equipment with due date in 5 days (Status: `UPCOMING`).
5. **Run Notification Alert Engine (1 min):**
   - Trigger alert generation (`POST /api/notifications/generate-calibration-alerts`).
   - Observe in-app notification successfully routed to the `PRIMARY_INCHARGE` of the laboratory.
6. **Execute Calibration Record & Upload Certificate (3 mins):**
   - Navigate to **Calibration Records** and record calibration event: Result `PASSED`, notes `Calibration within tolerance`.
   - Observe the linked calibration schedule automatically transitions to `COMPLETED`.
   - Upload calibration certificate PDF (`/api/calibration-documents/upload`) and link to record.
   - Download certificate and verify file integrity.
7. **Verify Scoping & Role Isolation (2 mins):**
   - Log out of administrator account.
   - Log in as `tech@calibratesys.edu`.
   - Verify that the staff user only sees "Biotechnology Research Lab" and its associated equipment, schedules, and notifications.
   - Attempt to access an unassigned laboratory URL or API — observe `403 Forbidden`.
8. **View Compliance Dashboard & Export Report (2 mins):**
   - Log back in as `ADMIN`.
   - Review **Compliance Dashboard** metrics (Total Labs, Equipment, Operational %, Overdue counts).
   - Navigate to **Reports**, view compliance audit breakdown, and click **Export CSV**.
   - Inspect the downloaded CSV to confirm data completeness and formula escaping.
9. **Inspect Immutable Audit Trail (1 min):**
   - Navigate to **Audit Logs**.
   - Verify comprehensive historical records of equipment creation, calibration completion, document upload, and login actions.

---

## 13. Demo Account Safety & Production Hardening

> [!CAUTION]
> The default development administrator account (`admin@calibratesys.edu` / `Admin@12345`) is provided **strictly for development and local testing**.
> 
> In production environments:
> 1. Set `SPRING_PROFILES_ACTIVE=prod`.
> 2. The initialization property `app.security.init-admin.enabled` is set to `false` by default in the `prod` profile, preventing automatic creation of default accounts.
> 3. Provide a cryptographically random `JWT_SECRET` (at least 256 bits). If the secret is missing or less than 32 characters, the application will fail to start.
> 4. Change all default database passwords and restrict database access to the application host.

---

## 14. Known Limitations & Architectural Notes

1. **In-App Notifications Only:**
   - Notifications are currently implemented as in-app notifications stored in the database. External email (SMTP) and SMS gateways are purposefully not implemented in this phase.
2. **Local / Shared Disk Document Storage:**
   - Calibration certificates are stored on the local or mounted file system. For horizontally scaled cloud environments, configure a shared NFS mount or consider an S3/Blob storage adapter in future iterations.
3. **Single Monolith Architecture:**
   - The application is intentionally structured as a modular Spring Boot monolith with clean domain separation rather than distributed microservices to ensure transactional integrity and operational simplicity.

