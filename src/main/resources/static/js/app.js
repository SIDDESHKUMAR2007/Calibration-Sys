/**
 * SmartLab: Laboratory Equipment Calibration & Compliance Management System
 * Production Client Application Logic
 */

let authToken = sessionStorage.getItem('jwtToken') || '';
let currentUser = JSON.parse(sessionStorage.getItem('currentUser') || 'null');
let currentRoute = 'dashboard';

// -------------------------------------------------------------
// Toast Notification System
// -------------------------------------------------------------
function showToast(message, type = 'info', title = '') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = '&#8505;';
    if (type === 'success') icon = '&#10003;';
    else if (type === 'error') icon = '&#9888;';
    else if (type === 'warning') icon = '&#9889;';

    let displayTitle = title;
    if (!displayTitle) {
        if (type === 'success') displayTitle = 'Success';
        else if (type === 'error') displayTitle = 'Error';
        else if (type === 'warning') displayTitle = 'Warning';
        else displayTitle = 'Information';
    }

    toast.innerHTML = `
        <span class="toast-icon">${icon}</span>
        <div class="toast-content">
            <div class="toast-title">${escapeHtml(displayTitle)}</div>
            <div class="toast-message">${escapeHtml(message)}</div>
        </div>
        <button type="button" class="toast-close" onclick="this.parentElement.remove()">&times;</button>
    `;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(40px)';
        setTimeout(() => toast.remove(), 250);
    }, 4500);
}

// -------------------------------------------------------------
// Utilities & Human-Friendly Display Formatters
// -------------------------------------------------------------
function escapeHtml(str) {
    if (str === null || str === undefined) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function safeSetJson(data) {
    const jsonViewer = document.getElementById('json-viewer');
    if (jsonViewer) {
        try {
            jsonViewer.textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
        } catch (_) {
            jsonViewer.textContent = String(data);
        }
    }
}

function formatDate(dateStr) {
    if (!dateStr) return '—';
    try {
        const parts = String(dateStr).split('-');
        if (parts.length === 3) {
            const y = Number(parts[0]);
            const m = Number(parts[1]) - 1;
            const d = Number(parts[2]);
            const dateObj = new Date(y, m, d);
            if (!isNaN(dateObj.getTime())) {
                return dateObj.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
            }
        }
        const parsed = new Date(dateStr);
        if (isNaN(parsed.getTime())) return dateStr;
        return parsed.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    } catch (_) {
        return dateStr;
    }
}

function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '—';
    try {
        const d = new Date(dateTimeStr);
        if (isNaN(d.getTime())) return dateTimeStr;
        return d.toLocaleString('en-GB', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: true
        });
    } catch (_) {
        return dateTimeStr;
    }
}

function formatStatusLabel(status) {
    if (!status) return '—';
    const map = {
        'OPERATIONAL': 'Operational',
        'CALIBRATION_DUE': 'Calibration Due',
        'OUT_OF_CALIBRATION': 'Out of Calibration',
        'UNDER_MAINTENANCE': 'Under Maintenance',
        'DECOMMISSIONED': 'Decommissioned',
        'SCHEDULED': 'Scheduled',
        'UPCOMING': 'Upcoming',
        'DUE': 'Due Today',
        'OVERDUE': 'Overdue',
        'COMPLETED': 'Completed',
        'CANCELLED': 'Cancelled',
        'ACTIVE': 'Active',
        'INACTIVE': 'Inactive',
        'VALID': 'Valid',
        'EXPIRING_SOON': 'Expiring Soon',
        'EXPIRED': 'Expired'
    };
    return map[status] || status.replace(/_/g, ' ');
}

function formatStatusBadge(status) {
    if (!status) return '<span class="status-pill status-loading">—</span>';
    const s = String(status).toUpperCase();
    const label = formatStatusLabel(s);
    let cssClass = 'status-loading';
    switch (s) {
        case 'OPERATIONAL':
        case 'ACTIVE':
        case 'COMPLETED':
        case 'VALID':
            cssClass = 'status-online';
            break;
        case 'CALIBRATION_DUE':
        case 'DUE':
        case 'UPCOMING':
        case 'EXPIRING_SOON':
        case 'UNDER_MAINTENANCE':
            cssClass = 'status-warning';
            break;
        case 'OUT_OF_CALIBRATION':
        case 'OVERDUE':
        case 'EXPIRED':
        case 'FAILED':
        case 'DECOMMISSIONED':
        case 'INACTIVE':
            cssClass = 'status-degraded';
            break;
        case 'SCHEDULED':
        case 'CANCELLED':
            cssClass = 'status-neutral';
            break;
    }
    return `<span class="status-pill ${cssClass}">${escapeHtml(label)}</span>`;
}

function formatPriorityBadge(priority) {
    if (!priority) return '—';
    const p = String(priority).toUpperCase();
    const map = {
        'CRITICAL': { label: 'Critical', cls: 'badge-priority-critical' },
        'HIGH': { label: 'High', cls: 'badge-priority-high' },
        'MEDIUM': { label: 'Medium', cls: 'badge-priority-medium' },
        'LOW': { label: 'Low', cls: 'badge-priority-low' }
    };
    const info = map[p] || { label: priority, cls: 'badge-priority-medium' };
    return `<span class="badge-priority ${info.cls}">${escapeHtml(info.label)}</span>`;
}

function formatResultBadge(result) {
    if (!result) return '—';
    const r = String(result).toUpperCase();
    if (r === 'PASSED') {
        return '<span class="badge-result badge-result-passed">Passed</span>';
    } else if (r === 'FAILED') {
        return '<span class="badge-result badge-result-failed">Failed</span>';
    } else if (r.includes('LIMITATION') || r.includes('CONDITION')) {
        return '<span class="badge-result badge-result-passed_with_limitations">Passed (Limitations)</span>';
    }
    return `<span class="badge-result">${escapeHtml(result.replace(/_/g, ' '))}</span>`;
}

function formatRole(role) {
    if (!role) return '—';
    const map = {
        'ADMIN': 'System Administrator',
        'LAB_STAFF': 'Laboratory Staff',
        'STAFF': 'Staff Member',
        'PRIMARY_INCHARGE': 'Primary In-Charge',
        'LAB_TECHNICIAN': 'Lab Technician',
        'CALIBRATION_COORDINATOR': 'Calibration Coordinator',
        'SAFETY_OFFICER': 'Safety Officer'
    };
    return map[role] || role.replace(/_/g, ' ');
}

function formatNotificationType(type) {
    if (!type) return 'General Notification';
    const map = {
        'CALIBRATION_UPCOMING': 'Upcoming Calibration',
        'CALIBRATION_DUE': 'Calibration Due Today',
        'CALIBRATION_OVERDUE': 'Overdue Calibration',
        'CALIBRATION_COMPLETED': 'Calibration Completed',
        'COMPLIANCE_ALERT': 'Compliance Alert',
        'GENERAL_NOTICE': 'Notice'
    };
    return map[type] || type.replace(/_/g, ' ');
}

function formatCalibratorType(type) {
    if (!type) return '—';
    if (type === 'INTERNAL') return 'Internal Staff';
    if (type === 'EXTERNAL_VENDOR') return 'External Vendor';
    return type.replace(/_/g, ' ');
}

function formatNullable(val, fallback = '—') {
    if (val === null || val === undefined || val === '') return fallback;
    return val;
}

// -------------------------------------------------------------
// Navigation & SPA Routing
// -------------------------------------------------------------
function navigateTo(route) {
    if (!authToken) {
        route = 'login';
    }

    // Role-based route protection
    if ((route === 'staff' || route === 'audit' || route === 'colleges' || route === 'diagnostics') && currentUser && currentUser.role !== 'ADMIN') {
        showToast('Access restricted to Administrators', 'warning', 'Permission Required');
        route = 'dashboard';
    }

    currentRoute = route;
    if (route !== 'login') {
        window.location.hash = '#/' + route;
    }

    // Hide all views
    document.querySelectorAll('.route-view').forEach(view => {
        view.classList.remove('active');
    });

    // Update active nav item
    document.querySelectorAll('.nav-item').forEach(item => {
        if (item.getAttribute('data-route') === route) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Show selected view
    const targetView = document.getElementById(`view-${route}`);
    if (targetView) {
        targetView.classList.add('active');
    }

    closeSidebar();
    closeUserDropdown();

    // Trigger data loading for view
    if (authToken) {
        switch (route) {
            case 'dashboard':
                loadDashboard();
                break;
            case 'laboratories':
                loadLaboratories();
                break;
            case 'staff':
                loadStaff();
                populateAssignLabDropdown();
                populateAssignStaffDropdown();
                break;
            case 'equipment':
                loadEquipment();
                populateEquipmentLabDropdown();
                break;
            case 'schedules':
                loadSchedules();
                populateScheduleLabDropdown();
                populateScheduleEquipmentDropdown();
                break;
            case 'records':
                loadRecords();
                populateRecordDropdowns();
                break;
            case 'documents':
                loadDocuments();
                populateDocDropdowns();
                break;
            case 'notifications':
                loadNotifications();
                break;
            case 'reports':
                initReportsUI();
                populateReportLabFilter();
                loadCurrentReport();
                break;
            case 'audit':
                loadAuditLogs();
                break;
            case 'colleges':
                loadColleges();
                break;
            case 'diagnostics':
                checkHealth();
                break;
        }
    }
}

// -------------------------------------------------------------
// Sidebar, User Menu & Password Helpers
// -------------------------------------------------------------
function toggleSidebar() {
    const sidebar = document.getElementById('app-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (sidebar) sidebar.classList.toggle('open');
    if (backdrop) backdrop.classList.toggle('open');
}

function closeSidebar() {
    const sidebar = document.getElementById('app-sidebar');
    const backdrop = document.getElementById('sidebar-backdrop');
    if (sidebar) sidebar.classList.remove('open');
    if (backdrop) backdrop.classList.remove('open');
}

function toggleUserDropdown() {
    const menu = document.getElementById('user-dropdown-menu');
    if (menu) {
        menu.style.display = menu.style.display === 'none' ? 'block' : 'none';
    }
}

function closeUserDropdown() {
    const menu = document.getElementById('user-dropdown-menu');
    if (menu) menu.style.display = 'none';
}

function togglePasswordVisibility() {
    const pwdInput = document.getElementById('auth-password');
    const btn = document.getElementById('toggle-password-btn');
    if (pwdInput) {
        if (pwdInput.type === 'password') {
            pwdInput.type = 'text';
            if (btn) btn.innerHTML = '&#128064;';
        } else {
            pwdInput.type = 'password';
            if (btn) btn.innerHTML = '&#128065;';
        }
    }
}

// -------------------------------------------------------------
// Modal Open / Close Helpers
// -------------------------------------------------------------
function openLabModal(lab) {
    resetLabForm();
    const title = document.getElementById('lab-modal-title');
    if (lab) {
        if (title) title.textContent = 'Edit Laboratory';
        editLaboratory(lab);
    } else {
        if (title) title.textContent = 'Add Laboratory';
    }
    const modal = document.getElementById('lab-modal');
    if (modal) modal.style.display = 'flex';
}
function closeLabModal() {
    const modal = document.getElementById('lab-modal');
    if (modal) modal.style.display = 'none';
}

function openStaffModal(staff) {
    resetStaffForm();
    const title = document.getElementById('staff-modal-title');
    if (staff) {
        if (title) title.textContent = 'Edit Staff Member';
        editStaff(staff);
    } else {
        if (title) title.textContent = 'Register New Staff';
    }
    const modal = document.getElementById('staff-modal');
    if (modal) modal.style.display = 'flex';
}
function closeStaffModal() {
    const modal = document.getElementById('staff-modal');
    if (modal) modal.style.display = 'none';
}

async function openEquipmentModal(eq) {
    resetEquipmentForm();
    if (!labsCache || labsCache.length === 0) {
        await loadLaboratories();
    } else {
        populateEquipmentLabDropdown();
    }
    const title = document.getElementById('equipment-modal-title');
    if (eq) {
        if (title) title.textContent = 'Edit Equipment';
        await editEquipment(eq);
    } else {
        if (title) title.textContent = 'Register Equipment';
    }
    const modal = document.getElementById('equipment-modal');
    if (modal) modal.style.display = 'flex';
}
function closeEquipmentModal() {
    const modal = document.getElementById('equipment-modal');
    if (modal) modal.style.display = 'none';
}

async function openScheduleModal(sch) {
    resetScheduleForm();
    if (!labsCache || labsCache.length === 0) {
        await loadLaboratories();
    } else {
        populateScheduleLabDropdown();
    }
    if (!equipmentCache || equipmentCache.length === 0) {
        await loadEquipment();
    } else {
        populateScheduleEquipmentDropdown();
    }
    const title = document.getElementById('schedule-modal-title');
    if (sch) {
        if (title) title.textContent = 'Edit Calibration Schedule';
        await editSchedule(sch);
    } else {
        if (title) title.textContent = 'Schedule Calibration';
    }
    const modal = document.getElementById('schedule-modal');
    if (modal) modal.style.display = 'flex';
}
function closeScheduleModal() {
    const modal = document.getElementById('schedule-modal');
    if (modal) modal.style.display = 'none';
}

async function openRecordModal() {
    resetRecordForm();
    if (!equipmentCache || equipmentCache.length === 0) {
        await loadEquipment();
    } else {
        populateRecordDropdowns();
    }
    const modal = document.getElementById('record-modal');
    if (modal) modal.style.display = 'flex';
}
function closeRecordModal() {
    const modal = document.getElementById('record-modal');
    if (modal) modal.style.display = 'none';
}

async function openDocumentModal() {
    resetDocForm();
    if (!equipmentCache || equipmentCache.length === 0) {
        await loadEquipment();
    } else {
        populateDocDropdowns();
    }
    const modal = document.getElementById('document-modal');
    if (modal) modal.style.display = 'flex';
}
function closeDocumentModal() {
    const modal = document.getElementById('document-modal');
    if (modal) modal.style.display = 'none';
}

function openCollegeModal(col) {
    resetCollegeForm();
    const title = document.getElementById('college-modal-title');
    if (col) {
        if (title) title.textContent = 'Edit College';
        editCollege(col);
    } else {
        if (title) title.textContent = 'Add College';
    }
    const modal = document.getElementById('college-modal');
    if (modal) modal.style.display = 'flex';
}
function closeCollegeModal() {
    const modal = document.getElementById('college-modal');
    if (modal) modal.style.display = 'none';
}

// -------------------------------------------------------------
// Health Monitoring
// -------------------------------------------------------------
async function checkHealth() {
    const headerPill = document.getElementById('header-status-pill');
    const backendDot = document.getElementById('backend-status-indicator');
    const backendText = document.getElementById('backend-status-text');
    const backendMeta = document.getElementById('backend-app-name');
    
    const dbDot = document.getElementById('db-status-indicator');
    const dbText = document.getElementById('db-status-text');
    const dbMeta = document.getElementById('db-details-text');
    
    const latencyText = document.getElementById('api-latency-text');
    const pingTime = document.getElementById('last-ping-time');
    const jsonViewer = document.getElementById('json-viewer');

    if (headerPill) {
        headerPill.className = 'status-pill status-loading';
        headerPill.textContent = 'Checking...';
    }
    if (backendDot) backendDot.className = 'indicator-dot loading';
    if (dbDot) dbDot.className = 'indicator-dot loading';

    const startTime = performance.now();

    try {
        const response = await fetch('/api/health');
        const elapsed = Math.round(performance.now() - startTime);
        if (latencyText) latencyText.textContent = `${elapsed} ms`;
        if (pingTime) pingTime.textContent = `Last checked: ${new Date().toLocaleTimeString()}`;

        if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);

        const data = await response.json();
        if (jsonViewer) jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (data.status === 'UP') {
            if (backendDot) backendDot.className = 'indicator-dot online';
            if (backendText) backendText.textContent = 'Operational';
        } else {
            if (backendDot) backendDot.className = 'indicator-dot loading';
            if (backendText) backendText.textContent = data.status || 'Degraded';
        }
        if (backendMeta) backendMeta.textContent = data.application || 'SmartLab Core';

        if (data.databaseStatus === 'CONNECTED') {
            if (dbDot) dbDot.className = 'indicator-dot online';
            if (dbText) dbText.textContent = 'Connected';
            const dbProduct = data.details?.databaseProduct || 'MySQL';
            const dbVersion = data.details?.databaseVersion || '';
            if (dbMeta) dbMeta.textContent = `${dbProduct} ${dbVersion}`.trim();
        } else {
            if (dbDot) dbDot.className = 'indicator-dot offline';
            if (dbText) dbText.textContent = data.databaseStatus || 'Disconnected';
            if (dbMeta) dbMeta.textContent = data.details?.databaseError || 'Check DB configuration';
        }

        if (data.status === 'UP' && data.databaseStatus === 'CONNECTED') {
            if (headerPill) {
                headerPill.className = 'status-pill status-online';
                headerPill.textContent = 'System Healthy';
            }
        } else {
            if (headerPill) {
                headerPill.className = 'status-pill status-degraded';
                headerPill.textContent = 'System Degraded';
            }
        }
    } catch (err) {
        const elapsed = Math.round(performance.now() - startTime);
        if (latencyText) latencyText.textContent = `${elapsed} ms`;
        if (pingTime) pingTime.textContent = `Last checked: ${new Date().toLocaleTimeString()}`;

        if (headerPill) {
            headerPill.className = 'status-pill status-offline';
            headerPill.textContent = 'Backend Offline';
        }
        if (backendDot) backendDot.className = 'indicator-dot offline';
        if (backendText) backendText.textContent = 'Offline';
        if (dbDot) dbDot.className = 'indicator-dot offline';
        if (dbText) dbText.textContent = 'Unknown';
    }
}

// -------------------------------------------------------------
// Authentication
// -------------------------------------------------------------
async function login() {
    const identifierInput = document.getElementById('auth-identifier');
    const passwordInput = document.getElementById('auth-password');
    const feedback = document.getElementById('auth-feedback');
    const errAlert = document.getElementById('login-error-alert');
    const errText = document.getElementById('login-error-text');
    const btnText = document.getElementById('login-btn-text');
    const spinner = document.getElementById('login-spinner');
    const jsonViewer = document.getElementById('json-viewer');

    if (errAlert) errAlert.style.display = 'none';

    const identifier = identifierInput ? identifierInput.value.trim() : '';
    const password = passwordInput ? passwordInput.value : '';

    if (!identifier || !password) {
        if (errText) errText.textContent = 'Please enter email/staff ID and password';
        if (errAlert) errAlert.style.display = 'flex';
        showToast('Please enter both email/staff ID and password', 'warning', 'Required Fields');
        return;
    }

    if (btnText) btnText.textContent = 'Authenticating...';
    if (spinner) spinner.style.display = 'inline-block';

    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ identifier, password })
        });

        const data = await res.json();
        if (jsonViewer) jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) {
            throw new Error(data.message || `Login failed (HTTP ${res.status})`);
        }

        authToken = data.token;
        currentUser = data;
        sessionStorage.setItem('jwtToken', authToken);
        sessionStorage.setItem('currentUser', JSON.stringify(currentUser));

        showToast(`Welcome back, ${data.fullName || 'User'}!`, 'success', 'Signed In');
        updateAuthUI();
        navigateTo('dashboard');
    } catch (err) {
        if (errText) errText.textContent = err.message || 'Login failed';
        if (errAlert) errAlert.style.display = 'flex';
        showToast(err.message || 'Authentication error', 'error', 'Login Failed');
    } finally {
        if (btnText) btnText.textContent = 'Sign In';
        if (spinner) spinner.style.display = 'none';
    }
}

function logout() {
    authToken = '';
    currentUser = null;
    sessionStorage.removeItem('jwtToken');
    sessionStorage.removeItem('currentUser');
    updateAuthUI();
    showToast('You have been signed out successfully.', 'info', 'Signed Out');
    resetDashboardUI();

    const collegeTb = document.getElementById('college-table-body');
    if (collegeTb) collegeTb.innerHTML = '<tr><td colspan="6" class="text-center text-muted p-4">Authenticate to load colleges</td></tr>';
    const labTb = document.getElementById('lab-table-body');
    if (labTb) labTb.innerHTML = '<tr><td colspan="7" class="text-center text-muted p-4">Authenticate to load laboratories</td></tr>';
    const staffTb = document.getElementById('staff-table-body');
    if (staffTb) staffTb.innerHTML = '<tr><td colspan="8" class="text-center text-muted p-4">Authenticate to load staff</td></tr>';
    const assignTb = document.getElementById('assignment-table-body');
    if (assignTb) assignTb.innerHTML = '<tr><td colspan="6" class="text-center text-muted p-4">Select a laboratory above to view assignments</td></tr>';
    const equipTb = document.getElementById('equipment-table-body');
    if (equipTb) equipTb.innerHTML = '<tr><td colspan="8" class="text-center text-muted p-4">Authenticate to load equipment</td></tr>';
    const schedTb = document.getElementById('schedule-table-body');
    if (schedTb) schedTb.innerHTML = '<tr><td colspan="7" class="text-center text-muted p-4">Authenticate to load calibration schedules</td></tr>';
    const recTb = document.getElementById('record-table-body');
    if (recTb) recTb.innerHTML = '<tr><td colspan="9" class="text-center text-muted p-4">Authenticate to load calibration records</td></tr>';
    const docTb = document.getElementById('document-table-body');
    if (docTb) docTb.innerHTML = '<tr><td colspan="8" class="text-center text-muted p-4">Authenticate to load documents</td></tr>';
    const auditTable = document.getElementById('audit-logs-table-body');
    if (auditTable) auditTable.innerHTML = '<tr><td colspan="7" class="text-center text-muted p-4">Log in as Administrator to view the audit trail.</td></tr>';
    const notifContainer = document.getElementById('notifications-list-container');
    if (notifContainer) notifContainer.innerHTML = '<div class="empty-state-sm">Authenticate to load notifications</div>';
    const badge = document.getElementById('notif-badge');
    if (badge) {
        badge.textContent = '0';
        badge.style.display = 'none';
    }
}

function updateAuthUI() {
    const loginView = document.getElementById('login-view');
    const appShell = document.getElementById('app-shell');
    const pill = document.getElementById('auth-status-pill');

    if (authToken && currentUser) {
        if (loginView) loginView.style.display = 'none';
        if (appShell) appShell.style.display = 'flex';

        if (pill) {
            pill.className = 'status-pill status-online';
            pill.textContent = `${currentUser.role}: ${currentUser.staffId}`;
        }

        const nameEl = document.getElementById('header-user-name');
        const roleEl = document.getElementById('header-user-role');
        const avatarEl = document.getElementById('header-user-avatar');
        const dropName = document.getElementById('dropdown-full-name');
        const dropEmail = document.getElementById('dropdown-email');
        const dropMeta = document.getElementById('dropdown-meta-info');

        const fullName = currentUser.fullName || currentUser.username || 'User';
        const role = currentUser.role || 'LAB_STAFF';
        const email = currentUser.email || `${currentUser.username || 'user'}@calibratesys.edu`;

        if (nameEl) nameEl.textContent = fullName;
        if (roleEl) {
            roleEl.textContent = role;
            roleEl.className = `user-role-badge ${role === 'ADMIN' ? 'badge-admin' : 'badge-staff'}`;
        }
        if (avatarEl) {
            const parts = fullName.split(' ');
            const initials = parts.length > 1 ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase() : fullName.substring(0, 2).toUpperCase();
            avatarEl.textContent = initials;
        }
        if (dropName) dropName.textContent = fullName;
        if (dropEmail) dropEmail.textContent = email;
        if (dropMeta) dropMeta.textContent = `Staff ID: ${currentUser.staffId} | Role: ${role}`;

        const isAdmin = role === 'ADMIN';
        document.querySelectorAll('.admin-only').forEach(el => {
            el.style.display = isAdmin ? '' : 'none';
        });

        const welcomeMsg = document.getElementById('dashboard-welcome-msg');
        if (welcomeMsg) {
            welcomeMsg.textContent = `Welcome back, ${fullName}. Here is the real-time calibration compliance overview for your assigned laboratories.`;
        }
        updateUnreadCount();
    } else {
        if (loginView) loginView.style.display = 'flex';
        if (appShell) appShell.style.display = 'none';
        if (pill) {
            pill.className = 'status-pill status-loading';
            pill.textContent = 'Not Authenticated';
        }
        document.querySelectorAll('.admin-only').forEach(el => {
            el.style.display = 'none';
        });
    }
}

function getAuthHeaders() {
    const headers = { 'Content-Type': 'application/json' };
    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }
    return headers;
}

function setFormSubmitting(formOrButton, isSubmitting, loadingText = 'Processing...') {
    if (!formOrButton) return;
    const btn = formOrButton.tagName === 'BUTTON' 
        ? formOrButton 
        : formOrButton.querySelector('button[type="submit"]');
    if (!btn) return;

    btn.disabled = isSubmitting;
    if (isSubmitting) {
        btn.dataset.origHtml = btn.innerHTML;
        btn.textContent = loadingText;
    } else if (btn.dataset.origHtml) {
        btn.innerHTML = btn.dataset.origHtml;
    }
}

// -------------------------------------------------------------
// College Management
// -------------------------------------------------------------
let collegesCache = [];

async function loadColleges() {
    if (!authToken) return;
    const tbody = document.getElementById('college-table-body');
    const select = document.getElementById('lab-college-select');
    const filterSelect = document.getElementById('filter-college');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch('/api/colleges', { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to fetch colleges (${res.status})`);

        collegesCache = data;
        tbody.innerHTML = '';
        select.innerHTML = '<option value="">-- Select College --</option>';
        filterSelect.innerHTML = '<option value="">All Colleges</option>';

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No colleges found. Create one above!</td></tr>';
            return;
        }

        data.forEach(c => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code>#${c.id}</code></td>
                <td><strong>${escapeHtml(c.code)}</strong></td>
                <td>${escapeHtml(c.name)}</td>
                <td>${escapeHtml(c.contactEmail || '—')}</td>
                <td>${c.totalLaboratories || 0}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick='editCollege(${JSON.stringify(c)})'>Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteCollege(${c.id})">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);

            const opt = document.createElement('option');
            opt.value = c.id;
            opt.textContent = `${c.code} - ${c.name}`;
            select.appendChild(opt);

            const filterOpt = document.createElement('option');
            filterOpt.value = c.id;
            filterOpt.textContent = `${c.code} - ${c.name}`;
            filterSelect.appendChild(filterOpt);
        });
    } catch (err) {
        showFeedback(document.getElementById('college-feedback'), err.message, false);
    }
}

async function saveCollege(event) {
    event.preventDefault();
    const id = document.getElementById('college-id').value;
    const code = document.getElementById('college-code').value.trim();
    const name = document.getElementById('college-name').value.trim();
    const address = document.getElementById('college-address').value.trim();
    const contactEmail = document.getElementById('college-email').value.trim();
    const contactPhone = document.getElementById('college-phone').value.trim();
    const feedback = document.getElementById('college-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    const payload = { code, name, address, contactEmail, contactPhone };
    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/colleges/${id}` : '/api/colleges';

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Operation failed (${res.status})`);

        showFeedback(feedback, `College ${id ? 'updated' : 'created'} successfully!`, true);
        resetCollegeForm();
        closeCollegeModal();
        loadColleges();
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function editCollege(c) {
    document.getElementById('college-id').value = c.id;
    document.getElementById('college-code').value = c.code || '';
    document.getElementById('college-name').value = c.name || '';
    document.getElementById('college-address').value = c.address || '';
    document.getElementById('college-email').value = c.contactEmail || '';
    document.getElementById('college-phone').value = c.contactPhone || '';
    document.getElementById('btn-college-save').textContent = 'Update College';
    const modal = document.getElementById('college-modal');
    if (modal) {
        document.getElementById('college-modal-title').textContent = 'Edit College';
        modal.style.display = 'flex';
    }
}

function resetCollegeForm() {
    document.getElementById('college-id').value = '';
    document.getElementById('college-form').reset();
    document.getElementById('btn-college-save').textContent = 'Save College';
}

async function deleteCollege(id) {
    if (!confirm(`Are you sure you want to delete College #${id}?`)) return;
    const feedback = document.getElementById('college-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/colleges/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.status === 204) {
            jsonViewer.textContent = JSON.stringify({ message: `College #${id} deleted successfully (204 No Content)` }, null, 2);
            showFeedback(feedback, `College #${id} deleted successfully`, true);
            loadColleges();
            loadLaboratories();
            return;
        }

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        throw new Error(data.message || `Delete failed (${res.status})`);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

// -------------------------------------------------------------
// Laboratory Management
// -------------------------------------------------------------
async function loadLaboratories() {
    if (!authToken) return;
    const tbody = document.getElementById('lab-table-body');
    const collegeId = document.getElementById('filter-college').value;
    const status = document.getElementById('filter-status').value;
    const jsonViewer = document.getElementById('json-viewer');

    let url = '/api/laboratories?';
    if (collegeId) url += `collegeId=${encodeURIComponent(collegeId)}&`;
    if (status) url += `status=${encodeURIComponent(status)}&`;

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to fetch laboratories (${res.status})`);

        labsCache = data;
        populateAssignLabDropdown();
        populateEquipmentLabDropdown();
        populateScheduleLabDropdown();
        populateRecordDropdowns();
        populateReportLabFilter();

        tbody.innerHTML = '';
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No laboratories found matching criteria.</td></tr>';
            return;
        }

        data.forEach(l => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code>#${l.id}</code></td>
                <td><strong>${escapeHtml(l.labCode)}</strong></td>
                <td>${escapeHtml(l.name)} <br><small class="stat-meta">${escapeHtml(l.department)}</small></td>
                <td>${escapeHtml(l.collegeCode || '—')}</td>
                <td>${formatStatusBadge(l.status)}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick='editLaboratory(${JSON.stringify(l)})'>Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteLaboratory(${l.id})">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showFeedback(document.getElementById('lab-feedback'), err.message, false);
    }
}

async function saveLaboratory(event) {
    event.preventDefault();
    const id = document.getElementById('lab-id').value;
    const collegeId = document.getElementById('lab-college-select').value;
    const labCode = document.getElementById('lab-code').value.trim();
    const name = document.getElementById('lab-name').value.trim();
    const department = document.getElementById('lab-department').value.trim();
    const location = document.getElementById('lab-location').value.trim();
    const status = document.getElementById('lab-status').value;
    const feedback = document.getElementById('lab-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    const payload = {
        collegeId: parseInt(collegeId, 10),
        labCode,
        name,
        department,
        location,
        status
    };

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/laboratories/${id}` : '/api/laboratories';

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Operation failed (${res.status})`);

        showFeedback(feedback, `Laboratory ${id ? 'updated' : 'created'} successfully!`, true);
        resetLabForm();
        closeLabModal();
        loadLaboratories();
        loadColleges(); // update lab count
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function editLaboratory(l) {
    document.getElementById('lab-id').value = l.id;
    document.getElementById('lab-college-select').value = l.collegeId;
    document.getElementById('lab-code').value = l.labCode || '';
    document.getElementById('lab-name').value = l.name || '';
    document.getElementById('lab-department').value = l.department || '';
    document.getElementById('lab-location').value = l.location || '';
    document.getElementById('lab-status').value = l.status || 'ACTIVE';
    document.getElementById('btn-lab-save').textContent = 'Update Laboratory';
    const modal = document.getElementById('lab-modal');
    if (modal) {
        document.getElementById('lab-modal-title').textContent = 'Edit Laboratory';
        modal.style.display = 'flex';
    }
}

function resetLabForm() {
    document.getElementById('lab-id').value = '';
    document.getElementById('lab-form').reset();
    document.getElementById('btn-lab-save').textContent = 'Save Laboratory';
}

async function deleteLaboratory(id) {
    if (!confirm(`Are you sure you want to delete Laboratory #${id}?`)) return;
    const feedback = document.getElementById('lab-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/laboratories/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.status === 204) {
            jsonViewer.textContent = JSON.stringify({ message: `Laboratory #${id} deleted successfully (204 No Content)` }, null, 2);
            showFeedback(feedback, `Laboratory #${id} deleted successfully`, true);
            loadLaboratories();
            loadColleges();
            return;
        }

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        throw new Error(data.message || `Delete failed (${res.status})`);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

// -------------------------------------------------------------
// Step 5: Staff Management
// -------------------------------------------------------------
let staffCache = [];
let labsCache = [];
let isEditingAssignment = false;

async function loadStaff() {
    if (!authToken) return;
    const tbody = document.getElementById('staff-table-body');
    const dept = document.getElementById('filter-staff-dept')?.value.trim();
    const status = document.getElementById('filter-staff-status')?.value;
    const jsonViewer = document.getElementById('json-viewer');

    let url = '/api/staff?';
    if (dept) url += `department=${encodeURIComponent(dept)}&`;
    if (status) url += `status=${encodeURIComponent(status)}&`;

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to fetch staff (${res.status})`);

        staffCache = data;
        populateAssignStaffDropdown();

        tbody.innerHTML = '';
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">No staff found matching criteria.</td></tr>';
            return;
        }

        data.forEach(s => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code>${escapeHtml(s.staffId)}</code></td>
                <td><strong>${escapeHtml(s.fullName)}</strong><br><small class="stat-meta">${escapeHtml(s.designation || '—')}</small></td>
                <td>${escapeHtml(s.email)}</td>
                <td>${escapeHtml(s.department || '—')}</td>
                <td><span class="badge-role">${formatRole(s.systemRole)}</span></td>
                <td>${formatStatusBadge(s.status)}</td>
                <td><button class="btn btn-secondary btn-sm" onclick="viewStaffLabs(${s.id}, '${escapeHtml(s.fullName)}')">${s.assignedLaboratoriesCount || 0} Labs</button></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick='editStaff(${JSON.stringify(s)})'>Edit</button>
                    <button class="btn btn-secondary btn-sm" onclick="toggleStaffStatus(${s.id}, '${s.status}')">${s.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteStaff(${s.id})">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showFeedback(document.getElementById('staff-feedback'), err.message, false);
    }
}

async function saveStaff(event) {
    event.preventDefault();
    const dbId = document.getElementById('staff-db-id').value;
    const staffId = document.getElementById('staff-code').value.trim();
    const firstName = document.getElementById('staff-firstname').value.trim();
    const lastName = document.getElementById('staff-lastname').value.trim();
    const email = document.getElementById('staff-email').value.trim();
    const phoneNumber = document.getElementById('staff-phone').value.trim();
    const password = document.getElementById('staff-password').value;
    const designation = document.getElementById('staff-designation').value.trim();
    const department = document.getElementById('staff-department').value.trim();
    const systemRole = document.getElementById('staff-system-role').value;
    const status = document.getElementById('staff-status').value;
    const feedback = document.getElementById('staff-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    const payload = {
        staffId,
        firstName,
        lastName,
        email,
        phoneNumber: phoneNumber || null,
        designation,
        department,
        systemRole,
        status,
        password: password || null
    };

    const method = dbId ? 'PUT' : 'POST';
    const url = dbId ? `/api/staff/${dbId}` : '/api/staff';

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Operation failed (${res.status})`);

        showFeedback(feedback, `Staff ${dbId ? 'updated' : 'created'} successfully!`, true);
        resetStaffForm();
        closeStaffModal();
        loadStaff();
        if (document.getElementById('assign-lab-select') && document.getElementById('assign-lab-select').value) {
            loadSelectedLabAssignments();
        }
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function editStaff(s) {
    document.getElementById('staff-db-id').value = s.id;
    document.getElementById('staff-code').value = s.staffId || '';
    document.getElementById('staff-firstname').value = s.firstName || '';
    document.getElementById('staff-lastname').value = s.lastName || '';
    document.getElementById('staff-email').value = s.email || '';
    document.getElementById('staff-phone').value = s.phoneNumber || '';
    document.getElementById('staff-password').value = '';
    document.getElementById('staff-designation').value = s.designation || '';
    document.getElementById('staff-department').value = s.department || '';
    document.getElementById('staff-system-role').value = s.systemRole || 'LAB_STAFF';
    document.getElementById('staff-status').value = s.status || 'ACTIVE';
    document.getElementById('btn-staff-save').textContent = 'Update Staff';
    const pwdGroup = document.getElementById('staff-password-group');
    if (pwdGroup) pwdGroup.style.display = 'none';
    const modal = document.getElementById('staff-modal');
    if (modal) {
        document.getElementById('staff-modal-title').textContent = 'Edit Staff Member';
        modal.style.display = 'flex';
    }
}

async function toggleStaffStatus(id, currentStatus) {
    const newStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const staff = staffCache.find(s => s.id === id);
    if (!staff) return;

    if (!confirm(`Are you sure you want to mark ${staff.fullName} as ${newStatus}?`)) return;
    const feedback = document.getElementById('staff-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    const payload = {
        staffId: staff.staffId,
        firstName: staff.firstName,
        lastName: staff.lastName,
        email: staff.email,
        phoneNumber: staff.phoneNumber,
        designation: staff.designation,
        department: staff.department,
        systemRole: staff.systemRole,
        status: newStatus,
        password: null
    };

    try {
        const res = await fetch(`/api/staff/${id}`, {
            method: 'PUT',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });
        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        if (!res.ok) throw new Error(data.message || `Status update failed (${res.status})`);

        showFeedback(feedback, `Staff status changed to ${newStatus}`, true);
        loadStaff();
        if (document.getElementById('assign-lab-select').value) {
            loadSelectedLabAssignments();
        }
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

async function deleteStaff(id) {
    if (!confirm(`Are you sure you want to delete Staff record #${id}? (Note: Deletion will fail if assignments exist).`)) return;
    const feedback = document.getElementById('staff-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/staff/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.status === 204) {
            jsonViewer.textContent = JSON.stringify({ message: `Staff #${id} deleted successfully (204 No Content)` }, null, 2);
            showFeedback(feedback, `Staff #${id} deleted successfully`, true);
            loadStaff();
            return;
        }

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        throw new Error(data.message || `Delete failed (${res.status})`);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function resetStaffForm() {
    document.getElementById('staff-db-id').value = '';
    document.getElementById('staff-form').reset();
    document.getElementById('btn-staff-save').textContent = 'Save Staff';
}

// -------------------------------------------------------------
// Step 5: Laboratory Staff Assignment Management
// -------------------------------------------------------------
function populateAssignLabDropdown() {
    const select = document.getElementById('assign-lab-select');
    if (!select) return;
    const currentVal = select.value;
    select.innerHTML = '<option value="">-- Choose Laboratory to Manage Assignments --</option>';

    labsCache.forEach(l => {
        const opt = document.createElement('option');
        opt.value = l.id;
        opt.textContent = `${l.labCode} - ${l.name} (${l.department})`;
        select.appendChild(opt);
    });

    if (currentVal && labsCache.some(l => String(l.id) === String(currentVal))) {
        select.value = currentVal;
    }
}

function populateAssignStaffDropdown() {
    const select = document.getElementById('assign-staff-select');
    if (!select) return;
    const currentVal = select.value;
    select.innerHTML = '<option value="">-- Select Staff Member --</option>';

    staffCache.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.id;
        opt.textContent = `${s.staffId} - ${s.fullName} [${s.department}] (${s.status})`;
        select.appendChild(opt);
    });

    if (currentVal) {
        select.value = currentVal;
    }
}

function onAssignLabSelected() {
    resetAssignmentForm();
    loadSelectedLabAssignments();
}

async function loadSelectedLabAssignments() {
    const labId = document.getElementById('assign-lab-select').value;
    const tbody = document.getElementById('assignment-table-body');
    const feedback = document.getElementById('assignment-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    if (!labId) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">Select a laboratory above to view assignments</td></tr>';
        return;
    }

    try {
        const res = await fetch(`/api/laboratories/${labId}/staff`, { headers: getAuthHeaders() });
        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Failed to load assignments (${res.status})`);

        tbody.innerHTML = '';
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No staff assigned to this laboratory yet.</td></tr>';
            return;
        }

        data.forEach(a => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${escapeHtml(a.staffEmployeeId)}</strong> - ${escapeHtml(a.staffName)}</td>
                <td>${escapeHtml(a.staffEmail)}</td>
                <td><span class="badge-role">${escapeHtml(a.roleInLab)}</span></td>
                <td>${a.primaryContact ? '<span class="badge-primary-contact">&#9733; Primary Contact</span>' : '<span style="color:var(--text-muted);">No</span>'}</td>
                <td><span class="status-pill ${a.assignmentStatus === 'ACTIVE' ? 'status-online' : 'status-degraded'}">${a.assignmentStatus}</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick='editAssignment(${a.staffId}, "${a.roleInLab}", ${a.primaryContact}, "${a.assignmentStatus}")'>Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="removeAssignment(${labId}, ${a.staffId})">Remove</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

async function saveAssignment(event) {
    event.preventDefault();
    const labId = document.getElementById('assign-lab-select').value;
    const staffId = document.getElementById('assign-staff-select').value;
    const roleInLab = document.getElementById('assign-role-select').value;
    const isPrimaryContact = document.getElementById('assign-primary-contact').checked;
    const status = document.getElementById('assign-status-select').value;
    const feedback = document.getElementById('assignment-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    if (!labId) {
        showFeedback(feedback, 'Please select a laboratory first', false);
        return;
    }
    if (!staffId) {
        showFeedback(feedback, 'Please select a staff member', false);
        return;
    }

    const payload = {
        staffId: parseInt(staffId, 10),
        roleInLab,
        isPrimaryContact,
        status
    };

    const method = isEditingAssignment ? 'PUT' : 'POST';
    const url = isEditingAssignment 
        ? `/api/laboratories/${labId}/staff/${staffId}`
        : `/api/laboratories/${labId}/staff`;

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Assignment failed (${res.status})`);

        showFeedback(feedback, `Assignment ${isEditingAssignment ? 'updated' : 'created'} successfully!`, true);
        resetAssignmentForm();
        loadSelectedLabAssignments();
        loadStaff(); // update assigned labs count
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function editAssignment(staffId, roleInLab, isPrimaryContact, status) {
    isEditingAssignment = true;
    const staffSelect = document.getElementById('assign-staff-select');
    staffSelect.value = staffId;
    staffSelect.disabled = true;

    document.getElementById('assign-role-select').value = roleInLab;
    document.getElementById('assign-primary-contact').checked = Boolean(isPrimaryContact);
    document.getElementById('assign-status-select').value = status || 'ACTIVE';
    document.getElementById('btn-assign-save').textContent = 'Update Assignment';
}

async function removeAssignment(labId, staffId) {
    if (!confirm(`Are you sure you want to remove staff member #${staffId} from this laboratory?`)) return;
    const feedback = document.getElementById('assignment-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/laboratories/${labId}/staff/${staffId}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.status === 204) {
            jsonViewer.textContent = JSON.stringify({ message: `Assignment removed (204 No Content)` }, null, 2);
            showFeedback(feedback, `Assignment removed successfully`, true);
            loadSelectedLabAssignments();
            loadStaff();
            return;
        }

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        throw new Error(data.message || `Removal failed (${res.status})`);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function resetAssignmentForm() {
    isEditingAssignment = false;
    const staffSelect = document.getElementById('assign-staff-select');
    staffSelect.disabled = false;
    document.getElementById('assignment-form').reset();
    document.getElementById('btn-assign-save').textContent = 'Assign / Update Staff';
}

// -------------------------------------------------------------
// Step 5: Staff Assigned Laboratories Modal
// -------------------------------------------------------------
async function viewStaffLabs(staffId, staffName) {
    const modal = document.getElementById('staff-labs-modal');
    const title = document.getElementById('staff-labs-title');
    const tbody = document.getElementById('staff-labs-table-body');
    const jsonViewer = document.getElementById('json-viewer');

    title.textContent = `Laboratories Assigned to ${staffName}`;
    tbody.innerHTML = '<tr><td colspan="6" class="text-center">Loading assigned laboratories...</td></tr>';
    modal.style.display = 'flex';

    try {
        const res = await fetch(`/api/staff/${staffId}/laboratories`, { headers: getAuthHeaders() });
        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Failed to fetch staff laboratories (${res.status})`);

        tbody.innerHTML = '';
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No laboratories assigned to this staff member.</td></tr>';
            return;
        }

        data.forEach(a => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${escapeHtml(a.laboratoryCode)}</strong></td>
                <td>${escapeHtml(a.laboratoryName)}</td>
                <td><span class="badge-role">${escapeHtml(a.roleInLab)}</span></td>
                <td>${a.primaryContact ? '<span class="badge-primary-contact">&#9733; Primary Contact</span>' : '<span style="color:var(--text-muted);">No</span>'}</td>
                <td><span class="status-pill ${a.assignmentStatus === 'ACTIVE' ? 'status-online' : 'status-degraded'}">${a.assignmentStatus}</span></td>
                <td>${a.assignedAt ? new Date(a.assignedAt).toLocaleDateString() : '-'}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center" style="color:var(--danger);">${escapeHtml(err.message)}</td></tr>`;
    }
}

function closeStaffLabsModal() {
    document.getElementById('staff-labs-modal').style.display = 'none';
}

// -------------------------------------------------------------
// Step 6: Equipment Management
// -------------------------------------------------------------
let equipmentCache = [];

function populateEquipmentLabDropdown() {
    const formSelect = document.getElementById('equipment-lab-select');
    const filterSelect = document.getElementById('filter-equipment-lab');
    if (!formSelect || !filterSelect) return;

    const currentFormVal = formSelect.value;
    const currentFilterVal = filterSelect.value;

    formSelect.innerHTML = '<option value="">-- Select Laboratory --</option>';
    filterSelect.innerHTML = '<option value="">All Laboratories</option>';

    labsCache.forEach(l => {
        const opt1 = document.createElement('option');
        opt1.value = l.id;
        opt1.textContent = `${l.labCode} - ${l.name} (${l.department})`;
        formSelect.appendChild(opt1);

        const opt2 = document.createElement('option');
        opt2.value = l.id;
        opt2.textContent = `${l.labCode} - ${l.name}`;
        filterSelect.appendChild(opt2);
    });

    if (currentFormVal && labsCache.some(l => String(l.id) === String(currentFormVal))) {
        formSelect.value = currentFormVal;
    }
    if (currentFilterVal && labsCache.some(l => String(l.id) === String(currentFilterVal))) {
        filterSelect.value = currentFilterVal;
    }
}

async function loadEquipment() {
    if (!authToken) return;
    const tbody = document.getElementById('equipment-table-body');
    const labId = document.getElementById('filter-equipment-lab')?.value;
    const status = document.getElementById('filter-equipment-status')?.value;
    const search = document.getElementById('filter-equipment-search')?.value.trim();
    const jsonViewer = document.getElementById('json-viewer');

    let url = '/api/equipment?';
    if (labId) url += `laboratoryId=${encodeURIComponent(labId)}&`;
    if (status) url += `status=${encodeURIComponent(status)}&`;
    if (search) url += `search=${encodeURIComponent(search)}&`;

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Failed to fetch equipment (${res.status})`);

        equipmentCache = data;
        populateScheduleEquipmentDropdown();
        populateRecordDropdowns();
        populateDocDropdowns();
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">No equipment found matching criteria.</td></tr>';
            return;
        }

        data.forEach(eq => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code>${escapeHtml(eq.assetTag)}</code></td>
                <td><strong>${escapeHtml(eq.name)}</strong> <br><small class="stat-meta">${escapeHtml(eq.modelNumber || eq.model || '—')}</small></td>
                <td>${escapeHtml(eq.manufacturer || '—')}</td>
                <td>${escapeHtml(eq.serialNumber || '—')}</td>
                <td><strong>${escapeHtml(eq.laboratoryCode || '')}</strong> <br><small class="stat-meta">${escapeHtml(eq.laboratoryName || '')}</small></td>
                <td>${eq.calibrationFrequencyDays || 90} days</td>
                <td>${formatStatusBadge(eq.status)}</td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick='editEquipment(${JSON.stringify(eq)})'>Edit</button>
                    <button class="btn btn-danger btn-sm" onclick="deleteEquipment(${eq.id}, '${escapeHtml(eq.assetTag)}')">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showFeedback(document.getElementById('equipment-feedback'), err.message, false);
    }
}

async function saveEquipment(event) {
    event.preventDefault();
    const id = document.getElementById('equipment-id').value;
    const laboratoryIdVal = document.getElementById('equipment-lab-select').value;
    const assetTag = document.getElementById('equipment-asset-tag').value.trim();
    const serialNumber = document.getElementById('equipment-serial').value.trim();
    const name = document.getElementById('equipment-name').value.trim();
    const model = document.getElementById('equipment-model').value.trim();
    const manufacturer = document.getElementById('equipment-manufacturer').value.trim();
    const purchaseDate = document.getElementById('equipment-purchase-date').value;
    const installationDate = document.getElementById('equipment-installation-date').value;
    const frequencyVal = document.getElementById('equipment-frequency').value;
    const status = document.getElementById('equipment-status').value;
    const feedback = document.getElementById('equipment-feedback');

    if (!laboratoryIdVal) {
        showFeedback(feedback, 'Please select a laboratory', false);
        return;
    }

    const payload = {
        laboratoryId: Number(laboratoryIdVal),
        assetTag,
        serialNumber: serialNumber || null,
        name,
        modelNumber: model || null,
        manufacturer: manufacturer || null,
        purchaseDate: purchaseDate || null,
        installationDate: installationDate || null,
        calibrationFrequencyDays: Number(frequencyVal),
        status
    };

    const isEdit = Boolean(id);
    const url = isEdit ? `/api/equipment/${id}` : '/api/equipment';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Equipment save failed (${res.status})`);

        showFeedback(feedback, `Equipment "${data.name}" (${data.assetTag}) saved successfully!`, true);
        resetEquipmentForm();
        closeEquipmentModal();
        loadEquipment();
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

async function editEquipment(eq) {
    if (!labsCache || labsCache.length === 0) {
        await loadLaboratories();
    } else {
        populateEquipmentLabDropdown();
    }
    document.getElementById('equipment-id').value = eq.id;
    document.getElementById('equipment-lab-select').value = eq.laboratoryId;
    document.getElementById('equipment-asset-tag').value = eq.assetTag;
    document.getElementById('equipment-serial').value = eq.serialNumber || '';
    document.getElementById('equipment-name').value = eq.name;
    document.getElementById('equipment-model').value = eq.modelNumber || eq.model || '';
    document.getElementById('equipment-manufacturer').value = eq.manufacturer || '';
    document.getElementById('equipment-purchase-date').value = eq.purchaseDate || '';
    document.getElementById('equipment-installation-date').value = eq.installationDate || '';
    document.getElementById('equipment-frequency').value = eq.calibrationFrequencyDays || 90;
    document.getElementById('equipment-status').value = eq.status || 'OPERATIONAL';
    document.getElementById('btn-equipment-save').textContent = 'Update Equipment';
    const modal = document.getElementById('equipment-modal');
    if (modal) {
        document.getElementById('equipment-modal-title').textContent = 'Edit Equipment';
        modal.style.display = 'flex';
    }
}

function resetEquipmentForm() {
    document.getElementById('equipment-id').value = '';
    document.getElementById('equipment-form').reset();
    document.getElementById('equipment-frequency').value = 90;
    document.getElementById('equipment-status').value = 'OPERATIONAL';
    document.getElementById('btn-equipment-save').textContent = 'Save Equipment';
}

async function deleteEquipment(id, assetTag) {
    if (!confirm(`Are you sure you want to delete equipment "${assetTag}" (ID #${id})?`)) return;
    const feedback = document.getElementById('equipment-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/equipment/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.status === 204) {
            jsonViewer.textContent = JSON.stringify({ message: `Equipment #${id} (${assetTag}) deleted successfully (204 No Content)` }, null, 2);
            showFeedback(feedback, `Equipment "${assetTag}" deleted successfully`, true);
            loadEquipment();
            return;
        }

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        throw new Error(data.message || `Delete failed (${res.status})`);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

// -------------------------------------------------------------
// Step 7: Calibration Scheduling & Due-Date Management
// -------------------------------------------------------------
let schedulesCache = [];

function populateScheduleEquipmentDropdown() {
    const formSelect = document.getElementById('schedule-equipment-select');
    const filterSelect = document.getElementById('filter-schedule-equipment');
    if (!formSelect || !filterSelect) return;

    const currentFormVal = formSelect.value;
    const currentFilterVal = filterSelect.value;

    formSelect.innerHTML = '<option value="">-- Select Equipment --</option>';
    filterSelect.innerHTML = '<option value="">All Equipment</option>';

    equipmentCache.forEach(eq => {
        const label = `${eq.assetTag} - ${eq.name} (${eq.laboratoryCode || 'Lab'})`;

        const opt1 = document.createElement('option');
        opt1.value = eq.id;
        opt1.textContent = label;
        formSelect.appendChild(opt1);

        const opt2 = document.createElement('option');
        opt2.value = eq.id;
        opt2.textContent = `${eq.assetTag} - ${eq.name}`;
        filterSelect.appendChild(opt2);
    });

    if (currentFormVal && equipmentCache.some(e => String(e.id) === String(currentFormVal))) {
        formSelect.value = currentFormVal;
    }
    if (currentFilterVal && equipmentCache.some(e => String(e.id) === String(currentFilterVal))) {
        filterSelect.value = currentFilterVal;
    }
}

function populateScheduleLabDropdown() {
    const filterSelect = document.getElementById('filter-schedule-lab');
    if (!filterSelect) return;

    const currentVal = filterSelect.value;
    filterSelect.innerHTML = '<option value="">All Laboratories</option>';

    labsCache.forEach(l => {
        const opt = document.createElement('option');
        opt.value = l.id;
        opt.textContent = `${l.labCode} - ${l.name}`;
        filterSelect.appendChild(opt);
    });

    if (currentVal && labsCache.some(l => String(l.id) === String(currentVal))) {
        filterSelect.value = currentVal;
    }
}

function filterSchedulesByQuickCategory(category, btnEl) {
    const statusSelect = document.getElementById('filter-schedule-status');
    if (category === 'ALL') {
        if (statusSelect) statusSelect.value = '';
    } else {
        if (statusSelect) statusSelect.value = category;
    }
    if (btnEl) {
        document.querySelectorAll('.tab-btn').forEach(b => {
            const oc = b.getAttribute('onclick') || '';
            if (oc.includes('filterSchedulesByQuickCategory')) {
                b.classList.remove('active');
            }
        });
        btnEl.classList.add('active');
    }
    loadSchedules();
}

async function loadSchedules() {
    if (!authToken) return;
    const tbody = document.getElementById('schedule-table-body');
    const labId = document.getElementById('filter-schedule-lab')?.value;
    const equipmentId = document.getElementById('filter-schedule-equipment')?.value;
    const status = document.getElementById('filter-schedule-status')?.value;
    const priority = document.getElementById('filter-schedule-priority')?.value;
    const fromDate = document.getElementById('filter-schedule-from')?.value;
    const toDate = document.getElementById('filter-schedule-to')?.value;

    let url = '/api/calibration-schedules?';
    if (labId) url += `laboratoryId=${encodeURIComponent(labId)}&`;
    if (equipmentId) url += `equipmentId=${encodeURIComponent(equipmentId)}&`;
    if (status) url += `status=${encodeURIComponent(status)}&`;
    if (priority) url += `priority=${encodeURIComponent(priority)}&`;
    if (fromDate) url += `fromDate=${encodeURIComponent(fromDate)}&`;
    if (toDate) url += `toDate=${encodeURIComponent(toDate)}&`;

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to fetch schedules (${res.status})`);

        schedulesCache = data;
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center">No calibration schedules found matching criteria.</td></tr>';
            return;
        }

        data.forEach(s => {
            const tr = document.createElement('tr');
            const canCancel = s.status !== 'CANCELLED' && s.status !== 'COMPLETED';

            tr.innerHTML = `
                <td><code>#${s.id}</code></td>
                <td><strong>${escapeHtml(s.assetTag || '—')}</strong><br><small class="stat-meta">${escapeHtml(s.equipmentName || '—')}</small></td>
                <td><strong>${escapeHtml(s.laboratoryCode || '—')}</strong><br><small class="stat-meta">${escapeHtml(s.laboratoryName || '')}</small></td>
                <td>${formatDate(s.scheduledDate)}</td>
                <td><strong>${formatDate(s.dueDate)}</strong></td>
                <td>${formatStatusBadge(s.status)}</td>
                <td>${formatPriorityBadge(s.priority)}</td>
                <td><small>${escapeHtml(s.remarks || '—')}</small></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick='editSchedule(${JSON.stringify(s)})'>Edit</button>
                    ${canCancel ? `<button class="btn btn-secondary btn-sm" onclick="cancelSchedule(${s.id})">Cancel</button>` : ''}
                    <button class="btn btn-danger btn-sm" onclick="deleteSchedule(${s.id})">Delete</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showFeedback(document.getElementById('schedule-feedback'), err.message, false);
    }
}

async function saveSchedule(event) {
    event.preventDefault();
    const id = document.getElementById('schedule-id').value;
    const equipmentIdVal = document.getElementById('schedule-equipment-select').value;
    const scheduledDate = document.getElementById('schedule-date').value;
    const dueDate = document.getElementById('schedule-due-date').value;
    const priority = document.getElementById('schedule-priority').value;
    const statusVal = document.getElementById('schedule-status-select')?.value;
    const remarks = document.getElementById('schedule-remarks').value.trim();
    const feedback = document.getElementById('schedule-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    if (!equipmentIdVal) {
        showFeedback(feedback, 'Please select an equipment item', false);
        return;
    }

    if (dueDate < scheduledDate) {
        showFeedback(feedback, 'Due date cannot be before scheduled date', false);
        return;
    }

    const isEdit = Boolean(id);
    const url = isEdit ? `/api/calibration-schedules/${id}` : '/api/calibration-schedules';
    const method = isEdit ? 'PUT' : 'POST';

    const payload = isEdit ? {
        scheduledDate,
        dueDate,
        priority,
        status: statusVal || null,
        remarks: remarks || null
    } : {
        equipmentId: Number(equipmentIdVal),
        scheduledDate,
        dueDate,
        priority,
        remarks: remarks || null
    };

    try {
        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Schedule save failed (${res.status})`);

        showFeedback(feedback, `Calibration schedule #${data.id} (${data.status}) saved successfully!`, true);
        resetScheduleForm();
        closeScheduleModal();
        loadSchedules();
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

async function editSchedule(s) {
    if (!equipmentCache || equipmentCache.length === 0) {
        await loadEquipment();
    } else {
        populateScheduleEquipmentDropdown();
    }
    document.getElementById('schedule-id').value = s.id;
    document.getElementById('schedule-equipment-select').value = s.equipmentId;
    document.getElementById('schedule-equipment-select').disabled = true;
    document.getElementById('schedule-date').value = s.scheduledDate;
    document.getElementById('schedule-due-date').value = s.dueDate;
    document.getElementById('schedule-priority').value = s.priority;

    const statusGroup = document.getElementById('schedule-status-group');
    if (statusGroup) {
        statusGroup.style.display = 'block';
        document.getElementById('schedule-status-select').value = s.status;
    }

    document.getElementById('schedule-remarks').value = s.remarks || '';
    document.getElementById('btn-schedule-save').textContent = 'Update Schedule';
    const modal = document.getElementById('schedule-modal');
    if (modal) {
        document.getElementById('schedule-modal-title').textContent = 'Edit Calibration Schedule';
        modal.style.display = 'flex';
    }
}

function resetScheduleForm() {
    document.getElementById('schedule-id').value = '';
    const equipSelect = document.getElementById('schedule-equipment-select');
    if (equipSelect) equipSelect.disabled = false;

    const statusGroup = document.getElementById('schedule-status-group');
    if (statusGroup) statusGroup.style.display = 'none';

    document.getElementById('schedule-form').reset();
    document.getElementById('schedule-priority').value = 'MEDIUM';
    document.getElementById('btn-schedule-save').textContent = 'Create Schedule';
}

async function cancelSchedule(id) {
    if (!confirm(`Are you sure you want to cancel Calibration Schedule #${id}?`)) return;
    const feedback = document.getElementById('schedule-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/calibration-schedules/${id}/cancel`, {
            method: 'PATCH',
            headers: getAuthHeaders()
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Cancellation failed (${res.status})`);

        showFeedback(feedback, `Calibration Schedule #${id} has been CANCELLED`, true);
        loadSchedules();
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

async function deleteSchedule(id) {
    if (!confirm(`Are you sure you want to delete Calibration Schedule #${id}?`)) return;
    const feedback = document.getElementById('schedule-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/calibration-schedules/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.status === 204) {
            jsonViewer.textContent = JSON.stringify({ message: `Calibration Schedule #${id} deleted successfully (204 No Content)` }, null, 2);
            showFeedback(feedback, `Calibration Schedule #${id} deleted successfully`, true);
            loadSchedules();
            return;
        }

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        throw new Error(data.message || `Delete failed (${res.status})`);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

// -------------------------------------------------------------
// Step 8: Calibration Records & History
// -------------------------------------------------------------
let recordsCache = [];

function populateRecordDropdowns() {
    const formEquip = document.getElementById('record-equipment-select');
    const filterEquip = document.getElementById('filter-record-equipment');
    const filterLab = document.getElementById('filter-record-lab');

    if (formEquip && filterEquip) {
        const curFormVal = formEquip.value;
        const curFilterVal = filterEquip.value;

        formEquip.innerHTML = '<option value="">-- Select Equipment --</option>';
        filterEquip.innerHTML = '<option value="">All Equipment</option>';

        equipmentCache.forEach(eq => {
            const label = `${eq.assetTag} - ${eq.name} (${eq.laboratoryCode || 'Lab'})`;

            const opt1 = document.createElement('option');
            opt1.value = eq.id;
            opt1.textContent = label;
            formEquip.appendChild(opt1);

            const opt2 = document.createElement('option');
            opt2.value = eq.id;
            opt2.textContent = `${eq.assetTag} - ${eq.name}`;
            filterEquip.appendChild(opt2);
        });

        if (curFormVal && equipmentCache.some(e => String(e.id) === String(curFormVal))) {
            formEquip.value = curFormVal;
        }
        if (curFilterVal && equipmentCache.some(e => String(e.id) === String(curFilterVal))) {
            filterEquip.value = curFilterVal;
        }
    }

    if (filterLab) {
        const curLabVal = filterLab.value;
        filterLab.innerHTML = '<option value="">All Laboratories</option>';

        labsCache.forEach(l => {
            const opt = document.createElement('option');
            opt.value = l.id;
            opt.textContent = `${l.labCode} - ${l.name}`;
            filterLab.appendChild(opt);
        });

        if (curLabVal && labsCache.some(l => String(l.id) === String(curLabVal))) {
            filterLab.value = curLabVal;
        }
    }
}

function onRecordEquipmentChange() {
    const equipSelect = document.getElementById('record-equipment-select');
    const schedSelect = document.getElementById('record-schedule-select');
    const calDateInput = document.getElementById('record-calibration-date');
    const nextDueInput = document.getElementById('record-next-due-date');

    if (!equipSelect || !schedSelect) return;

    const equipId = equipSelect.value;
    schedSelect.innerHTML = '<option value="">-- Optional Associated Schedule --</option>';

    if (!equipId) return;

    // Filter schedules for this equipment that are not cancelled
    const matchingSchedules = schedulesCache.filter(s => String(s.equipmentId) === String(equipId) && s.status !== 'CANCELLED');
    matchingSchedules.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.id;
        opt.textContent = `Schedule #${s.id} (Due: ${s.dueDate}, ${s.status}, ${s.priority})`;
        schedSelect.appendChild(opt);
    });

    // Suggest default calibration date as today if empty
    if (calDateInput && !calDateInput.value) {
        calDateInput.value = new Date().toISOString().split('T')[0];
    }

    // Suggest next due date based on equipment's calibration frequency
    const eq = equipmentCache.find(e => String(e.id) === String(equipId));
    if (eq && calDateInput && nextDueInput && calDateInput.value) {
        const baseDate = new Date(calDateInput.value);
        const freqDays = eq.calibrationFrequencyDays || 90;
        baseDate.setDate(baseDate.getDate() + freqDays);
        nextDueInput.value = baseDate.toISOString().split('T')[0];
    }
}

async function loadRecords() {
    if (!authToken) return;
    const tbody = document.getElementById('record-table-body');
    const labId = document.getElementById('filter-record-lab')?.value;
    const equipmentId = document.getElementById('filter-record-equipment')?.value;
    const result = document.getElementById('filter-record-result')?.value;
    const type = document.getElementById('filter-record-type')?.value;
    const fromDate = document.getElementById('filter-record-from')?.value;
    const toDate = document.getElementById('filter-record-to')?.value;
    const jsonViewer = document.getElementById('json-viewer');

    let url = '/api/calibration-records?';
    if (labId) url += `laboratoryId=${encodeURIComponent(labId)}&`;
    if (equipmentId) url += `equipmentId=${encodeURIComponent(equipmentId)}&`;
    if (result) url += `result=${encodeURIComponent(result)}&`;
    if (type) url += `calibratedByType=${encodeURIComponent(type)}&`;
    if (fromDate) url += `fromDate=${encodeURIComponent(fromDate)}&`;
    if (toDate) url += `toDate=${encodeURIComponent(toDate)}&`;

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to fetch calibration records (${res.status})`);

        recordsCache = data;
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="10" class="text-center">No calibration records found matching criteria.</td></tr>';
            return;
        }

        data.forEach(r => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><code>#${r.id}</code></td>
                <td><strong>${escapeHtml(r.assetTag || '—')}</strong><br><small class="stat-meta">${escapeHtml(r.equipmentName || '—')}</small></td>
                <td><strong>${escapeHtml(r.laboratoryCode || '—')}</strong><br><small class="stat-meta">${escapeHtml(r.laboratoryName || '')}</small></td>
                <td>${formatDate(r.calibrationDate)}</td>
                <td><strong>${formatDate(r.nextDueDate)}</strong></td>
                <td><span class="badge-calib-type">${formatCalibratorType(r.calibratedByType)}</span></td>
                <td>${escapeHtml(r.calibratorName || '—')}</td>
                <td>${formatResultBadge(r.result)}</td>
                <td>${escapeHtml(r.standardsUsed || '—')}<br><small class="stat-meta">${escapeHtml(r.uncertaintyMeasurement || '')}</small></td>
                <td><small>${escapeHtml(r.notes || '—')}</small></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showFeedback(document.getElementById('record-feedback'), err.message, false);
    }
}

async function saveRecord(event) {
    event.preventDefault();
    const equipmentIdVal = document.getElementById('record-equipment-select').value;
    const scheduleIdVal = document.getElementById('record-schedule-select').value;
    const calibrationDate = document.getElementById('record-calibration-date').value;
    const nextDueDate = document.getElementById('record-next-due-date').value;
    const calibratedByType = document.getElementById('record-type').value;
    const calibratorName = document.getElementById('record-calibrator-name').value.trim();
    const result = document.getElementById('record-result').value;
    const uncertaintyMeasurement = document.getElementById('record-uncertainty').value.trim();
    const standardsUsed = document.getElementById('record-standards').value.trim();
    const notes = document.getElementById('record-notes').value.trim();
    const feedback = document.getElementById('record-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    if (!equipmentIdVal) {
        showFeedback(feedback, 'Please select an equipment item', false);
        return;
    }

    if (nextDueDate < calibrationDate) {
        showFeedback(feedback, 'Next due date cannot be before calibration date', false);
        return;
    }

    const payload = {
        equipmentId: Number(equipmentIdVal),
        scheduleId: scheduleIdVal ? Number(scheduleIdVal) : null,
        calibrationDate,
        nextDueDate,
        calibratedByType,
        calibratorName,
        result,
        uncertaintyMeasurement: uncertaintyMeasurement || null,
        standardsUsed: standardsUsed || null,
        notes: notes || null
    };

    try {
        const res = await fetch('/api/calibration-records', {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(payload)
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Record creation failed (${res.status})`);

        showFeedback(feedback, `Calibration record #${data.id} (${data.result}) saved successfully!`, true);
        resetRecordForm();
        closeRecordModal();
        loadRecords();
        loadSchedules();
        loadEquipment();
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function resetRecordForm() {
    document.getElementById('record-form').reset();
    document.getElementById('record-schedule-select').innerHTML = '<option value="">-- Optional Associated Schedule --</option>';
    document.getElementById('record-type').value = 'INTERNAL';
    document.getElementById('record-result').value = 'PASSED';
}

// -------------------------------------------------------------
// Step 9: Calibration Certificates and Document Management
// -------------------------------------------------------------
let documentsCache = [];

function populateDocDropdowns() {
    const equipSelect = document.getElementById('doc-equipment-id');
    const filterEquipSelect = document.getElementById('filter-doc-equipment');
    if (!equipSelect || !filterEquipSelect) return;

    const currentEquipVal = equipSelect.value;
    const currentFilterVal = filterEquipSelect.value;

    equipSelect.innerHTML = '<option value="">-- Select Equipment (Required) * --</option>';
    filterEquipSelect.innerHTML = '<option value="">All Equipment</option>';

    equipmentCache.forEach(eq => {
        const opt = document.createElement('option');
        opt.value = eq.id;
        opt.textContent = `${eq.assetTag} - ${eq.name} (${eq.laboratoryCode || ''})`;
        equipSelect.appendChild(opt);

        const filterOpt = document.createElement('option');
        filterOpt.value = eq.id;
        filterOpt.textContent = `${eq.assetTag} - ${eq.name}`;
        filterEquipSelect.appendChild(filterOpt);
    });

    if (currentEquipVal) equipSelect.value = currentEquipVal;
    if (currentFilterVal) filterEquipSelect.value = currentFilterVal;
}

async function onDocEquipmentChange() {
    const equipId = document.getElementById('doc-equipment-id').value;
    const recordSelect = document.getElementById('doc-record-id');
    if (!recordSelect) return;

    recordSelect.innerHTML = '<option value="">-- Optional Calibration Record --</option>';
    if (!equipId) return;

    try {
        const res = await fetch(`/api/equipment/${equipId}/calibration-records`, { headers: getAuthHeaders() });
        if (res.ok) {
            const records = await res.json();
            records.forEach(r => {
                const opt = document.createElement('option');
                opt.value = r.id;
                opt.textContent = `Record #${r.id} (${r.calibrationDate} - ${r.result})`;
                recordSelect.appendChild(opt);
            });
        }
    } catch (err) {
        console.error('Error loading equipment calibration records:', err);
    }
}

async function loadDocuments() {
    if (!authToken) return;
    const tbody = document.getElementById('document-table-body');
    const equipId = document.getElementById('filter-doc-equipment')?.value;
    const certNo = document.getElementById('filter-doc-cert-no')?.value?.trim();
    const issueStart = document.getElementById('filter-doc-issue-start')?.value;
    const issueEnd = document.getElementById('filter-doc-issue-end')?.value;
    const jsonViewer = document.getElementById('json-viewer');

    let url = '/api/calibration-documents?';
    if (equipId) url += `equipmentId=${encodeURIComponent(equipId)}&`;
    if (certNo) url += `certificateNumber=${encodeURIComponent(certNo)}&`;
    if (issueStart) url += `issueStartDate=${encodeURIComponent(issueStart)}&`;
    if (issueEnd) url += `issueEndDate=${encodeURIComponent(issueEnd)}&`;

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to fetch documents (${res.status})`);

        documentsCache = data;
        tbody.innerHTML = '';

        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="12" class="text-center">No calibration documents found matching criteria.</td></tr>';
            return;
        }

        const isAdmin = currentUser?.role === 'ADMIN';

        data.forEach(d => {
            const tr = document.createElement('tr');
            const sizeFormatted = d.fileSizeBytes ? formatFileSize(d.fileSizeBytes) : '—';

            let deleteButton = '';
            if (isAdmin) {
                if (d.calibrationRecordId) {
                    deleteButton = `<button class="btn btn-sm btn-secondary" disabled title="Immutable: Preserved for calibration audit trail">Locked</button>`;
                } else {
                    deleteButton = `<button class="btn btn-sm btn-danger" onclick="deleteDocument(${d.id})">Delete</button>`;
                }
            }

            tr.innerHTML = `
                <td><code>#${d.id}</code></td>
                <td><strong>${escapeHtml(d.equipmentCode || '—')}</strong><br><small class="stat-meta">${escapeHtml(d.equipmentName || '—')}</small></td>
                <td>${escapeHtml(d.laboratoryName || '—')}</td>
                <td>${d.calibrationRecordId ? `<code>Record #${d.calibrationRecordId}</code><br><small class="stat-meta">${formatDate(d.calibrationDate)}</small>` : '<span class="text-muted">None (General)</span>'}</td>
                <td><strong>${escapeHtml(d.documentName)}</strong></td>
                <td><code>${escapeHtml(d.certificateNumber || '—')}</code></td>
                <td><span class="badge-file-type">${escapeHtml(d.fileType)}</span><br><small class="stat-meta">${sizeFormatted}</small></td>
                <td>${formatDate(d.issueDate)}</td>
                <td><strong>${formatDate(d.validUntil)}</strong></td>
                <td>${formatStatusBadge(d.validityStatus)}</td>
                <td><small>${formatDateTime(d.uploadedAt)}</small></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="downloadDocument(${d.id})">Download</button>
                    ${deleteButton}
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showFeedback(document.getElementById('doc-feedback'), err.message, false);
    }
}

async function uploadDocument(event) {
    event.preventDefault();
    const equipId = document.getElementById('doc-equipment-id').value;
    const recordId = document.getElementById('doc-record-id').value;
    const docName = document.getElementById('doc-name').value.trim();
    const certNo = document.getElementById('doc-cert-no').value.trim();
    const issueDate = document.getElementById('doc-issue-date').value;
    const validUntil = document.getElementById('doc-valid-until').value;
    const fileInput = document.getElementById('doc-file');
    const feedback = document.getElementById('doc-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    if (!equipId) {
        showFeedback(feedback, 'Please select an equipment item', false);
        return;
    }

    if (!docName) {
        showFeedback(feedback, 'Document name is required', false);
        return;
    }

    if (!fileInput.files || fileInput.files.length === 0) {
        showFeedback(feedback, 'Please select a file to upload', false);
        return;
    }

    const file = fileInput.files[0];
    if (file.size > 10 * 1024 * 1024) {
        showFeedback(feedback, 'File exceeds maximum allowed size of 10MB', false);
        return;
    }

    if (issueDate && validUntil && validUntil < issueDate) {
        showFeedback(feedback, 'Validity end date cannot be before issue date', false);
        return;
    }

    const formData = new FormData();
    formData.append('equipmentId', equipId);
    if (recordId) formData.append('calibrationRecordId', recordId);
    formData.append('documentName', docName);
    if (certNo) formData.append('certificateNumber', certNo);
    if (issueDate) formData.append('issueDate', issueDate);
    if (validUntil) formData.append('validUntil', validUntil);
    formData.append('file', file);

    try {
        const headers = {};
        if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

        const res = await fetch('/api/calibration-documents', {
            method: 'POST',
            headers,
            body: formData
        });

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Upload failed (${res.status})`);

        showFeedback(feedback, `Document "${data.documentName}" uploaded successfully!`, true);
        resetDocForm();
        closeDocumentModal();
        loadDocuments();
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

async function downloadDocument(id) {
    try {
        const headers = {};
        if (authToken) headers['Authorization'] = `Bearer ${authToken}`;

        const res = await fetch(`/api/calibration-documents/${id}/download`, { headers });
        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            throw new Error(errData.message || `Download failed (${res.status})`);
        }

        const disposition = res.headers.get('Content-Disposition') || '';
        let filename = 'document';
        const match = disposition.match(/filename="?([^";]+)"?/);
        if (match && match[1]) filename = match[1];

        const blob = await res.blob();
        const blobUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = blobUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(blobUrl);
    } catch (err) {
        showFeedback(document.getElementById('doc-feedback'), err.message, false);
    }
}

async function deleteDocument(id) {
    if (!confirm(`Are you sure you want to delete document #${id}? This will remove the file from storage.`)) return;
    const feedback = document.getElementById('doc-feedback');
    const jsonViewer = document.getElementById('json-viewer');

    try {
        const res = await fetch(`/api/calibration-documents/${id}`, {
            method: 'DELETE',
            headers: getAuthHeaders()
        });

        if (res.status === 204) {
            jsonViewer.textContent = JSON.stringify({ message: `Document #${id} deleted (204 No Content)` }, null, 2);
            showFeedback(feedback, `Document #${id} deleted successfully`, true);
            loadDocuments();
            return;
        }

        const data = await res.json();
        jsonViewer.textContent = JSON.stringify(data, null, 2);
        throw new Error(data.message || `Deletion failed (${res.status})`);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function resetDocForm() {
    document.getElementById('document-form').reset();
    document.getElementById('doc-record-id').innerHTML = '<option value="">-- Optional Calibration Record --</option>';
}

function formatFileSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    else if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    else return (bytes / 1048576).toFixed(2) + ' MB';
}

// -------------------------------------------------------------
// Step 10: In-App Notifications and Calibration Alert Management
// -------------------------------------------------------------
let notificationsCache = [];

function scrollToNotifications() {
    const section = document.getElementById('notifications-section');
    if (section) {
        section.scrollIntoView({ behavior: 'smooth' });
    }
    loadNotifications();
}

async function updateUnreadCount() {
    if (!authToken) return;
    try {
        const res = await fetch('/api/notifications/unread-count', { headers: getAuthHeaders() });
        if (res.ok) {
            const data = await res.json();
            const badge = document.getElementById('notif-badge');
            if (badge) {
                if (data.unreadCount > 0) {
                    badge.textContent = data.unreadCount > 99 ? '99+' : data.unreadCount;
                    badge.style.display = 'inline-block';
                } else {
                    badge.textContent = '0';
                    badge.style.display = 'none';
                }
            }
        }
    } catch (err) {
        console.error('Failed to update unread count:', err);
    }
}

async function loadNotifications() {
    if (!authToken) return;
    const container = document.getElementById('notifications-list-container');
    const type = document.getElementById('filter-notif-type')?.value;
    const priority = document.getElementById('filter-notif-priority')?.value;
    const isRead = document.getElementById('filter-notif-status')?.value;
    const jsonViewer = document.getElementById('json-viewer');

    let url = '/api/notifications?';
    if (type) url += `type=${encodeURIComponent(type)}&`;
    if (priority) url += `priority=${encodeURIComponent(priority)}&`;
    if (isRead !== '') url += `isRead=${encodeURIComponent(isRead)}&`;

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to fetch notifications (${res.status})`);

        notificationsCache = data;
        updateUnreadCount();
        container.innerHTML = '';

        if (data.length === 0) {
            container.innerHTML = '<div class="text-center" style="padding: 2rem; color: var(--text-muted);"><p>No notifications found matching criteria.</p></div>';
            return;
        }

        data.forEach(n => {
            const div = document.createElement('div');
            div.className = `notif-item ${n.read ? 'read' : 'unread'}`;
            const readStatusBadge = n.read 
                ? '<span class="badge-read">READ</span>' 
                : '<span class="badge-unread">UNREAD</span>';

            const markReadBtn = !n.read 
                ? `<button class="btn btn-sm btn-secondary" onclick="markNotificationRead(${n.id})">Mark as Read</button>` 
                : '';

            div.innerHTML = `
                <div style="flex: 1;">
                    <div class="notif-title-row">
                        ${formatPriorityBadge(n.priority)}
                        <span class="badge-file-type">${formatNotificationType(n.notificationType)}</span>
                        ${readStatusBadge}
                        <span class="notif-title">${escapeHtml(n.title)}</span>
                    </div>
                    <div style="font-size: 0.85rem; color: var(--text-primary); margin: 0.35rem 0;">
                        ${escapeHtml(n.message)}
                    </div>
                    <div class="notif-meta">
                        <strong>Equipment:</strong> ${escapeHtml(n.equipmentName || '—')} (${escapeHtml(n.equipmentCode || '—')}) &bull; 
                        <strong>Laboratory:</strong> ${escapeHtml(n.laboratoryName || '—')} &bull; 
                        ${n.scheduledDueDate ? `<strong>Due Date:</strong> ${formatDate(n.scheduledDueDate)} &bull; ` : ''}
                        <strong>Received:</strong> ${formatDateTime(n.createdAt)}
                        ${n.readAt ? ` &bull; <strong>Read:</strong> ${formatDateTime(n.readAt)}` : ''}
                    </div>
                </div>
                <div style="margin-left: 1rem; display: flex; align-items: center;">
                    ${markReadBtn}
                </div>
            `;
            container.appendChild(div);
        });
    } catch (err) {
        showFeedback(document.getElementById('notif-feedback'), err.message, false);
    }
}

async function markNotificationRead(id) {
    try {
        const res = await fetch(`/api/notifications/${id}/read`, {
            method: 'PATCH',
            headers: getAuthHeaders()
        });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to mark notification read (${res.status})`);

        showToast('Notification marked as read', 'info', 'Updated');
        loadNotifications();
        updateUnreadCount();
    } catch (err) {
        showFeedback(document.getElementById('notif-feedback'), err.message, false);
    }
}

async function markAllNotificationsAsRead() {
    try {
        const res = await fetch('/api/notifications/read-all', {
            method: 'PATCH',
            headers: getAuthHeaders()
        });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Failed to mark all read (${res.status})`);

        showToast(`${data.updatedCount} notification(s) marked as read`, 'success', 'All Read');
        showFeedback(document.getElementById('notif-feedback'), `${data.updatedCount} notification(s) marked as read`, true);
        loadNotifications();
        updateUnreadCount();
    } catch (err) {
        showFeedback(document.getElementById('notif-feedback'), err.message, false);
    }
}

async function triggerCalibrationAlerts() {
    const feedback = document.getElementById('notif-feedback');
    showToast('Evaluating upcoming and overdue calibration schedules...', 'info', 'Evaluating Alerts');

    try {
        const res = await fetch('/api/notifications/generate-calibration-alerts', {
            method: 'POST',
            headers: getAuthHeaders()
        });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) throw new Error(data.message || `Alert generation failed (${res.status})`);

        const msg = `Alert Evaluation Complete: ${data.notificationsCreated} created, ${data.notificationsSkippedAsDuplicates} skipped (Upcoming: ${data.upcomingSchedulesProcessed}, Due: ${data.dueSchedulesProcessed}, Overdue: ${data.overdueSchedulesProcessed})`;
        showToast(msg, 'success', 'Alerts Generated');
        showFeedback(feedback, msg, true);
        loadNotifications();
        updateUnreadCount();
        loadDashboard();
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

// -------------------------------------------------------------
// Step 11: Compliance Dashboard & Monitoring
// -------------------------------------------------------------
let currentDashboardLabFilter = '';
let dashboardLaboratoriesCache = [];

async function loadDashboard() {
    if (!authToken) return;

    try {
        let url = '/api/dashboard';
        if (currentDashboardLabFilter) {
            url += `?laboratoryId=${encodeURIComponent(currentDashboardLabFilter)}`;
        }

        const res = await fetch(url, {
            headers: getAuthHeaders()
        });

        const data = await res.json();
        document.getElementById('json-viewer').textContent = JSON.stringify(data, null, 2);

        if (!res.ok) throw new Error(data.message || `Failed to load dashboard (${res.status})`);

        // 1. Update Scope Indicator
        const scopePill = document.getElementById('dashboard-scope-pill');
        const scopeText = document.getElementById('dashboard-scope-text');
        const isAdmin = currentUser && currentUser.role === 'ADMIN';

        if (scopePill && scopeText) {
            if (isAdmin) {
                if (currentDashboardLabFilter) {
                    scopePill.className = 'scope-pill admin';
                    scopePill.textContent = 'FILTERED LAB';
                    const activeLab = data.laboratories && data.laboratories.find(l => String(l.laboratoryId) === String(currentDashboardLabFilter));
                    scopeText.textContent = activeLab ? `${activeLab.laboratoryName} (${activeLab.laboratoryCode})` : `Laboratory #${currentDashboardLabFilter}`;
                } else {
                    scopePill.className = 'scope-pill admin';
                    scopePill.textContent = 'ORGANIZATION-WIDE';
                    scopeText.textContent = 'All Laboratories across Colleges';
                }
            } else {
                scopePill.className = 'scope-pill staff';
                scopePill.textContent = 'ASSIGNED LABS';
                if (data.laboratories && data.laboratories.length > 0) {
                    scopeText.textContent = `${data.laboratories.length} active assignment(s): ${data.laboratories.map(l => l.laboratoryCode).join(', ')}`;
                } else {
                    scopeText.textContent = 'No active laboratory assignments';
                }
            }
        }

        // 2. Populate Lab Filter dropdown (preserve selection)
        dashboardLaboratoriesCache = data.laboratories || [];
        populateDashboardLabDropdown(dashboardLaboratoriesCache);

        // 3. Populate KPI summary cards
        const s = data.summary || {};
        document.getElementById('kpi-labs').textContent = s.totalLaboratories ?? 0;
        document.getElementById('kpi-active-labs').textContent = s.activeLaboratories ?? 0;
        document.getElementById('kpi-equipment').textContent = s.totalEquipment ?? 0;
        document.getElementById('kpi-operational').textContent = s.operationalEquipment ?? 0;
        document.getElementById('kpi-maintenance').textContent = s.equipmentUnderMaintenance ?? 0;
        document.getElementById('kpi-upcoming').textContent = s.upcomingSchedules ?? 0;
        document.getElementById('kpi-due').textContent = s.dueSchedules ?? 0;
        document.getElementById('kpi-overdue').textContent = s.overdueSchedules ?? 0;
        document.getElementById('kpi-certs').textContent = s.recordsWithDocuments ?? 0;

        // 4. Populate Attention Required Columns
        renderAttentionList('dash-overdue-list', data.overdueCalibrations || [], 'overdue', 'No overdue calibrations.');
        renderAttentionList('dash-due-list', data.dueCalibrations || [], 'due', 'No calibrations due today.');
        renderAttentionList('dash-upcoming-list', data.upcomingCalibrations || [], 'upcoming', 'No upcoming calibrations.');

        document.getElementById('attention-overdue-count').textContent = (data.overdueCalibrations || []).length;
        document.getElementById('attention-due-count').textContent = (data.dueCalibrations || []).length;
        document.getElementById('attention-upcoming-count').textContent = (data.upcomingCalibrations || []).length;

        // 5. Populate Laboratory Overview Table
        const cbOverdue = document.getElementById('cb-overdue-only');
        if (cbOverdue && cbOverdue.checked) {
            loadDashboardLaboratories();
        } else {
            renderLaboratoryOverviewTable(data.laboratories || []);
        }

        // 6. Populate Notifications Widget
        const notifBadge = document.getElementById('dash-notif-count-badge');
        if (notifBadge) {
            notifBadge.textContent = data.unreadNotificationCount ?? 0;
        }

        const notifList = document.getElementById('dash-notif-widget-list');
        if (notifList) {
            if (!data.recentNotifications || data.recentNotifications.length === 0) {
                notifList.innerHTML = '<div class="text-center" style="padding: 1rem; color: var(--text-muted);">No unread alerts.</div>';
            } else {
                notifList.innerHTML = '';
                data.recentNotifications.forEach(n => {
                    const div = document.createElement('div');
                    div.className = 'notif-item unread';
                    div.style.padding = '0.5rem 0.65rem';
                    div.style.marginBottom = '0.35rem';
                    div.innerHTML = `
                        <div style="font-weight: 600; font-size: 0.8rem; color: var(--text-primary);">${escapeHtml(n.title)}</div>
                        <div style="font-size: 0.725rem; color: var(--text-secondary);">${escapeHtml(n.message)}</div>
                        <div style="font-size: 0.7rem; color: var(--text-muted); margin-top: 0.2rem;">${escapeHtml(n.laboratoryName || '')} &bull; ${escapeHtml(n.priority || '')}</div>
                    `;
                    notifList.appendChild(div);
                });
            }
        }

        // 7. Populate Recent Calibration Activity Table
        renderRecentCalibrationsTable(data.recentCalibrations || []);

    } catch (err) {
        console.error('Failed to load dashboard:', err);
    }
}

function populateDashboardLabDropdown(laboratories) {
    const select = document.getElementById('dashboard-lab-filter');
    if (!select) return;

    const currentVal = select.value;
    select.innerHTML = '<option value="">All Scoped Laboratories</option>';

    laboratories.forEach(l => {
        const opt = document.createElement('option');
        opt.value = l.laboratoryId;
        opt.textContent = `${l.laboratoryCode} - ${l.laboratoryName}`;
        select.appendChild(opt);
    });

    if (currentVal && laboratories.some(l => String(l.laboratoryId) === String(currentVal))) {
        select.value = currentVal;
    }
}

function onDashboardLabFilterChange() {
    const select = document.getElementById('dashboard-lab-filter');
    currentDashboardLabFilter = select ? select.value : '';
    loadDashboard();
}

function renderAttentionList(containerId, items, type, emptyMessage) {
    const container = document.getElementById(containerId);
    if (!container) return;

    if (!items || items.length === 0) {
        container.innerHTML = `<div class="text-center" style="padding: 1rem; color: var(--text-muted); font-size: 0.8rem;">${emptyMessage}</div>`;
        return;
    }

    container.innerHTML = '';
    items.forEach(item => {
        const div = document.createElement('div');
        div.className = `attention-item ${type}`;

        let urgencyTag = '';
        if (type === 'overdue') {
            urgencyTag = `<span class="badge-priority-critical">${item.daysOverdue}d OVERDUE</span>`;
        } else if (type === 'due') {
            urgencyTag = `<span class="badge-priority-medium">DUE TODAY</span>`;
        } else {
            urgencyTag = `<span class="badge-unread">In ${item.daysUntilDue}d</span>`;
        }

        div.innerHTML = `
            <div class="attention-title">
                <span>${escapeHtml(item.equipmentName)}</span>
                ${urgencyTag}
            </div>
            <div class="attention-details">
                <div>Tag: <strong>${escapeHtml(item.assetTag)}</strong> &bull; Lab: ${escapeHtml(item.laboratoryCode || item.laboratoryName || 'N/A')}</div>
                <div>Due Date: <strong>${escapeHtml(item.dueDate || '')}</strong> &bull; Priority: ${escapeHtml(item.priority || '')}</div>
                ${item.responsibleStaffName ? `<div>Responsible: ${escapeHtml(item.responsibleStaffName)} (${escapeHtml(item.responsibleStaffRole || 'Staff')})</div>` : ''}
            </div>
        `;
        container.appendChild(div);
    });
}

function renderLaboratoryOverviewTable(laboratories) {
    const tbody = document.getElementById('dash-lab-table-body');
    if (!tbody) return;

    if (!laboratories || laboratories.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">No active laboratory assignments available.</td></tr>';
        return;
    }

    tbody.innerHTML = '';
    laboratories.forEach(lab => {
        const tr = document.createElement('tr');
        if (lab.overdueCount > 0) {
            tr.style.backgroundColor = '#fff5f5';
        }

        const overdueBadge = lab.overdueCount > 0
            ? `<span class="badge-priority-critical">${lab.overdueCount} OVERDUE</span>`
            : `<span style="color: var(--text-muted);">0</span>`;

        const dueBadge = lab.dueCount > 0
            ? `<span class="badge-priority-medium">${lab.dueCount}</span>`
            : `<span style="color: var(--text-muted);">0</span>`;

        tr.innerHTML = `
            <td><strong>${escapeHtml(lab.laboratoryName)}</strong></td>
            <td><code>${escapeHtml(lab.laboratoryCode)}</code></td>
            <td>${lab.totalEquipment}</td>
            <td><span style="color: var(--success); font-weight: 600;">${lab.operationalEquipment}</span></td>
            <td>${lab.upcomingCalibrationCount}</td>
            <td>${dueBadge}</td>
            <td>${overdueBadge}</td>
        `;
        tbody.appendChild(tr);
    });
}

async function loadDashboardLaboratories() {
    if (!authToken) return;

    const cbOverdue = document.getElementById('cb-overdue-only');
    const overdueOnly = cbOverdue ? cbOverdue.checked : false;

    let url = `/api/dashboard/laboratories?overdueOnly=${overdueOnly}`;
    if (currentDashboardLabFilter) {
        url += `&laboratoryId=${encodeURIComponent(currentDashboardLabFilter)}`;
    }

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || 'Failed to load laboratory summaries');
        renderLaboratoryOverviewTable(data);
    } catch (err) {
        console.error('Failed to filter laboratories:', err);
    }
}

function renderRecentCalibrationsTable(records) {
    const tbody = document.getElementById('dash-recent-table-body');
    if (!tbody) return;

    if (!records || records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center">No calibration activity available.</td></tr>';
        return;
    }

    tbody.innerHTML = '';
    records.forEach(r => {
        const tr = document.createElement('tr');
        const certBadge = r.hasCertificate
            ? `<span class="badge-cert-yes">&#10003; ${r.documentCount} Doc(s)</span>`
            : `<span class="badge-cert-no">None</span>`;

        let resultBadgeClass = 'badge-priority-low';
        if (r.result === 'PASSED') resultBadgeClass = 'badge-priority-medium';
        if (r.result === 'FAILED') resultBadgeClass = 'badge-priority-critical';

        tr.innerHTML = `
            <td>#${r.recordId}</td>
            <td><strong>${escapeHtml(r.equipmentName || 'N/A')}</strong></td>
            <td><code>${escapeHtml(r.assetTag || 'N/A')}</code></td>
            <td>${escapeHtml(r.laboratoryName || 'N/A')}</td>
            <td>${escapeHtml(r.calibrationDate || '')}</td>
            <td>${escapeHtml(r.nextDueDate || '')}</td>
            <td><span class="${resultBadgeClass}">${escapeHtml(r.result || '')}</span></td>
            <td>${escapeHtml(r.calibratorName || '')}</td>
            <td>${certBadge}</td>
        `;
        tbody.appendChild(tr);
    });
}

function resetDashboardUI() {
    currentDashboardLabFilter = '';
    const ids = ['kpi-labs', 'kpi-active-labs', 'kpi-equipment', 'kpi-operational', 'kpi-maintenance', 'kpi-upcoming', 'kpi-due', 'kpi-overdue', 'kpi-certs', 'attention-overdue-count', 'attention-due-count', 'attention-upcoming-count'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.textContent = '0';
    });

    const overdueList = document.getElementById('dash-overdue-list');
    if (overdueList) overdueList.innerHTML = '<div class="text-center" style="padding: 1rem; color: var(--text-muted); font-size: 0.8rem;">No overdue calibrations.</div>';

    const dueList = document.getElementById('dash-due-list');
    if (dueList) dueList.innerHTML = '<div class="text-center" style="padding: 1rem; color: var(--text-muted); font-size: 0.8rem;">No calibrations due today.</div>';

    const upcomingList = document.getElementById('dash-upcoming-list');
    if (upcomingList) upcomingList.innerHTML = '<div class="text-center" style="padding: 1rem; color: var(--text-muted); font-size: 0.8rem;">No upcoming calibrations.</div>';

    const labTable = document.getElementById('dash-lab-table-body');
    if (labTable) labTable.innerHTML = '<tr><td colspan="7" class="text-center">Authenticate to load laboratories</td></tr>';

    const notifWidget = document.getElementById('dash-notif-widget-list');
    if (notifWidget) notifWidget.innerHTML = '<div class="text-center" style="padding: 1rem; color: var(--text-muted);">No unread alerts.</div>';

    const recentTable = document.getElementById('dash-recent-table-body');
    if (recentTable) recentTable.innerHTML = '<tr><td colspan="9" class="text-center">No calibration activity available.</td></tr>';

    const filterSelect = document.getElementById('dashboard-lab-filter');
    if (filterSelect) filterSelect.innerHTML = '<option value="">All Scoped Laboratories</option>';
}

// -------------------------------------------------------------
// Utilities
// -------------------------------------------------------------
function showFeedback(element, message, isSuccess) {
    if (message) {
        showToast(message, isSuccess ? 'success' : 'error');
    }
    if (!element) return;
    element.className = isSuccess ? 'feedback-msg feedback-success' : 'feedback-msg feedback-error';
    element.textContent = message;
    element.style.display = 'block';
    setTimeout(() => {
        element.style.display = 'none';
    }, 6000);
}

// -------------------------------------------------------------
// Step 12: Audit Trail & Activity Logging Module
// -------------------------------------------------------------
let auditLogsCurrentPage = 0;
const auditLogsPageSize = 15;
let auditLogsTotalPages = 1;
let currentAuditLogsCache = [];

async function loadAuditLogs() {
    if (!authToken || !currentUser || currentUser.role !== 'ADMIN') return;

    const action = document.getElementById('filter-audit-action')?.value || '';
    const entityType = document.getElementById('filter-audit-entity-type')?.value || '';
    const fromDate = document.getElementById('filter-audit-from')?.value || '';
    const toDate = document.getElementById('filter-audit-to')?.value || '';

    const params = new URLSearchParams();
    params.append('page', auditLogsCurrentPage);
    params.append('size', auditLogsPageSize);
    if (action) params.append('action', action);
    if (entityType) params.append('entityType', entityType);
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);

    try {
        const res = await fetch(`/api/audit-logs?${params.toString()}`, {
            headers: { 'Authorization': `Bearer ${authToken}` }
        });

        if (!res.ok) {
            const errData = await res.json().catch(() => ({}));
            showFeedback(document.getElementById('audit-feedback'), errData.message || 'Failed to load audit logs', false);
            return;
        }

        const data = await res.json();
        const logs = data.content || [];
        currentAuditLogsCache = logs;
        auditLogsTotalPages = data.totalPages || 1;

        const tableBody = document.getElementById('audit-logs-table-body');
        if (!tableBody) return;

        if (logs.length === 0) {
            tableBody.innerHTML = '<tr><td colspan="7" class="text-center" style="padding: 1.5rem; color: var(--text-muted);">No audit log events match the current criteria.</td></tr>';
        } else {
            tableBody.innerHTML = logs.map((log, index) => {
                let badgeClass = 'badge-audit-auth';
                const act = log.action || '';
                if (act.includes('CREATED') || act.includes('ASSIGNED')) badgeClass = 'badge-audit-create';
                else if (act.includes('UPDATED')) badgeClass = 'badge-audit-update';
                else if (act.includes('DELETED') || act.includes('DEACTIVATED') || act.includes('UNASSIGNED') || act.includes('CANCELLED')) badgeClass = 'badge-audit-delete';
                else if (act.includes('TRIGGERED') || act.includes('FAILURE')) badgeClass = 'badge-audit-alert';

                const formattedTime = formatDateTime(log.createdAt);
                const actorDisplay = log.actorStaffName
                    ? `${escapeHtml(log.actorStaffName)} <span style="color:var(--text-muted); font-size:0.75rem;">(${escapeHtml(log.actorStaffEmail || log.actorIdentifier || '')})</span>`
                    : escapeHtml(log.actorIdentifier || 'System / Anonymous');

                const entityDisplay = log.entityType
                    ? `<span class="badge-audit-entity">${escapeHtml(log.entityType)}</span> ${log.entityId ? '<code>#' + log.entityId + '</code>' : ''}`
                    : '—';

                const hasDiff = (log.oldValues && log.oldValues.trim() !== '') || (log.newValues && log.newValues.trim() !== '');
                const diffBtn = hasDiff
                    ? `<button class="btn btn-sm btn-secondary" onclick="showAuditDiff(${index})">View Diff</button>`
                    : `<span style="color:var(--text-muted); font-size:0.8rem;">None</span>`;

                return `
                    <tr>
                        <td style="white-space:nowrap; font-size:0.82rem; color:var(--text-secondary);">${formattedTime}</td>
                        <td style="font-size:0.85rem;">${actorDisplay}</td>
                        <td><span class="badge-audit-action ${badgeClass}">${escapeHtml(log.action)}</span></td>
                        <td>${entityDisplay}</td>
                        <td style="font-size:0.85rem;">${escapeHtml(log.description || '—')}</td>
                        <td style="font-size:0.82rem; font-family:var(--font-mono, monospace);">${escapeHtml(log.ipAddress || '—')}</td>
                        <td>${diffBtn}</td>
                    </tr>
                `;
            }).join('');
        }

        // Pagination info
        const pageInfo = document.getElementById('audit-page-info');
        if (pageInfo) {
            pageInfo.textContent = `Page ${auditLogsCurrentPage + 1} of ${Math.max(1, auditLogsTotalPages)} (${data.totalElements || 0} total records)`;
        }
        const btnPrev = document.getElementById('btn-audit-prev');
        if (btnPrev) btnPrev.disabled = (auditLogsCurrentPage <= 0);
        const btnNext = document.getElementById('btn-audit-next');
        if (btnNext) btnNext.disabled = (auditLogsCurrentPage >= auditLogsTotalPages - 1);

    } catch (err) {
        showFeedback(document.getElementById('audit-feedback'), err.message, false);
    }
}

function changeAuditPage(delta) {
    const newPage = auditLogsCurrentPage + delta;
    if (newPage >= 0 && newPage < auditLogsTotalPages) {
        auditLogsCurrentPage = newPage;
        loadAuditLogs();
    }
}

function resetAuditFilters() {
    const actFilter = document.getElementById('filter-audit-action');
    if (actFilter) actFilter.value = '';
    const entFilter = document.getElementById('filter-audit-entity-type');
    if (entFilter) entFilter.value = '';
    const fromFilter = document.getElementById('filter-audit-from');
    if (fromFilter) fromFilter.value = '';
    const toFilter = document.getElementById('filter-audit-to');
    if (toFilter) toFilter.value = '';
    auditLogsCurrentPage = 0;
    loadAuditLogs();
}

function showAuditDiff(index) {
    const log = currentAuditLogsCache[index];
    if (!log) return;

    const modalTitle = document.getElementById('audit-diff-modal-title');
    if (modalTitle) modalTitle.textContent = `Audit Event #${log.id}: ${log.action}`;

    const modalMeta = document.getElementById('audit-modal-meta');
    if (modalMeta) {
        modalMeta.innerHTML = `
            <div><strong>Entity:</strong> ${escapeHtml(log.entityType || '-')} ${log.entityId ? '#' + log.entityId : ''}</div>
            <div><strong>Actor:</strong> ${escapeHtml(log.actorStaffName || log.actorIdentifier || 'System')} (${escapeHtml(log.actorStaffEmail || log.actorIdentifier || '')})</div>
            <div><strong>Timestamp:</strong> ${log.createdAt ? new Date(log.createdAt).toLocaleString() : '-'} | <strong>IP:</strong> ${escapeHtml(log.ipAddress || 'unknown')}</div>
            <div style="margin-top:0.25rem;"><strong>Description:</strong> ${escapeHtml(log.description || '-')}</div>
        `;
    }

    const oldEl = document.getElementById('audit-modal-old-values');
    if (oldEl) {
        if (!log.oldValues || log.oldValues.trim() === '') {
            oldEl.textContent = 'None (Entity was newly created or previous state not applicable)';
        } else {
            try {
                oldEl.textContent = JSON.stringify(JSON.parse(log.oldValues), null, 2);
            } catch (e) {
                oldEl.textContent = log.oldValues;
            }
        }
    }

    const newEl = document.getElementById('audit-modal-new-values');
    if (newEl) {
        if (!log.newValues || log.newValues.trim() === '') {
            newEl.textContent = 'None (Entity was deleted or no new state retained)';
        } else {
            try {
                newEl.textContent = JSON.stringify(JSON.parse(log.newValues), null, 2);
            } catch (e) {
                newEl.textContent = log.newValues;
            }
        }
    }

    const modal = document.getElementById('audit-diff-modal');
    if (modal) modal.style.display = 'flex';
}

function closeAuditDiffModal() {
    const modal = document.getElementById('audit-diff-modal');
    if (modal) modal.style.display = 'none';
}

// =============================================================
// Step 13: Global Search, Debouncing, Dropdown & Navigation
// =============================================================
let globalSearchDebounceTimer = null;
let globalSearchResultsData = null;

function onGlobalSearchInput(event) {
    const q = event.target.value.trim();
    const dropdown = document.getElementById('global-search-dropdown');
    const clearBtn = document.getElementById('global-search-clear');
    const spinner = document.getElementById('global-search-spinner');

    if (globalSearchDebounceTimer) {
        clearTimeout(globalSearchDebounceTimer);
    }

    if (!q || q.length < 2) {
        if (dropdown) dropdown.style.display = 'none';
        if (clearBtn) clearBtn.style.display = q.length > 0 ? 'inline-block' : 'none';
        if (spinner) spinner.style.display = 'none';
        return;
    }

    if (clearBtn) clearBtn.style.display = 'inline-block';

    globalSearchDebounceTimer = setTimeout(() => {
        executeGlobalSearch(q);
    }, 300);
}

function onGlobalSearchKeyDown(event) {
    const dropdown = document.getElementById('global-search-dropdown');
    if (event.key === 'Escape') {
        if (dropdown) dropdown.style.display = 'none';
    } else if (event.key === 'Enter') {
        event.preventDefault();
        const input = document.getElementById('global-search-input');
        if (input && input.value.trim().length >= 2) {
            executeGlobalSearch(input.value.trim());
        }
    }
}

function clearGlobalSearch() {
    const input = document.getElementById('global-search-input');
    const dropdown = document.getElementById('global-search-dropdown');
    const clearBtn = document.getElementById('global-search-clear');
    const spinner = document.getElementById('global-search-spinner');

    if (input) input.value = '';
    if (dropdown) dropdown.style.display = 'none';
    if (clearBtn) clearBtn.style.display = 'none';
    if (spinner) spinner.style.display = 'none';
}

async function executeGlobalSearch(query) {
    if (!authToken) {
        alert('Please login to use global search.');
        return;
    }

    const spinner = document.getElementById('global-search-spinner');
    const dropdown = document.getElementById('global-search-dropdown');
    const resultsContainer = document.getElementById('global-search-results');

    if (spinner) spinner.style.display = 'inline-block';

    try {
        const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`, {
            headers: getAuthHeaders()
        });
        const data = await res.json();

        if (!res.ok) {
            throw new Error(data.message || `Search failed (${res.status})`);
        }

        globalSearchResultsData = data;
        renderGlobalSearchDropdown(data, query);
        if (dropdown) dropdown.style.display = 'block';
    } catch (err) {
        console.error('Global search error:', err);
        if (resultsContainer) {
            resultsContainer.innerHTML = `<div class="search-empty-state" style="color:var(--danger-color);">${escapeHtml(err.message)}</div>`;
        }
        if (dropdown) dropdown.style.display = 'block';
    } finally {
        if (spinner) spinner.style.display = 'none';
    }
}

function renderGlobalSearchDropdown(data, query) {
    const container = document.getElementById('global-search-results');
    if (!container) return;

    if (!data || data.totalMatches === 0) {
        container.innerHTML = `<div class="search-empty-state">No matching items found for "<strong>${escapeHtml(query)}</strong>"</div>`;
        return;
    }

    let html = '';

    const categories = [
        { key: 'equipment', title: 'Equipment', icon: '&#128300;', targetSection: 'equipment' },
        { key: 'laboratories', title: 'Laboratories', icon: '&#127979;', targetSection: 'laboratories' },
        { key: 'staff', title: 'Staff Directory', icon: '&#128100;', targetSection: 'staff' },
        { key: 'calibrationSchedules', title: 'Calibration Schedules', icon: '&#128197;', targetSection: 'schedules' },
        { key: 'calibrationRecords', title: 'Calibration Records', icon: '&#128221;', targetSection: 'records' },
        { key: 'calibrationDocuments', title: 'Calibration Documents', icon: '&#128196;', targetSection: 'documents' }
    ];

    categories.forEach(cat => {
        const items = data[cat.key];
        if (items && items.length > 0) {
            html += `
                <div class="search-category-header">
                    <span>${cat.icon} ${cat.title}</span>
                    <span class="badge" style="background:#cbd5e1; color:#1e293b; padding:1px 6px; border-radius:10px; font-size:0.7rem;">${items.length}</span>
                </div>
            `;
            items.forEach(item => {
                const statusBadge = item.status ? `<span class="search-item-badge status-pill ${getSearchStatusClass(item.status)}">${escapeHtml(item.status)}</span>` : '';
                const labBadge = item.laboratory ? `<span style="font-size:0.7rem; color:#64748b;">${escapeHtml(item.laboratory)}</span>` : '';
                html += `
                    <div class="search-item" onclick="onSelectGlobalSearchResult('${cat.targetSection}', ${item.id || item.entityId})">
                        <div class="search-item-main">
                            <div class="search-item-title">${escapeHtml(item.title)}</div>
                            <div class="search-item-sub">${escapeHtml(item.subtitle || '')}</div>
                        </div>
                        <div class="search-item-meta">
                            ${statusBadge}
                            ${labBadge}
                        </div>
                    </div>
                `;
            });
        }
    });

    html += `<div class="search-dropdown-footer">Found ${data.totalMatches} matches (Press Esc to close)</div>`;
    container.innerHTML = html;
}

function getSearchStatusClass(status) {
    if (!status) return 'status-unknown';
    switch (status) {
        case 'OPERATIONAL':
        case 'ACTIVE':
        case 'PASSED':
        case 'COMPLETED':
        case 'VALID':
            return 'status-online';
        case 'OVERDUE':
        case 'FAILED':
        case 'OUT_OF_CALIBRATION':
        case 'EXPIRED':
            return 'status-degraded';
        default:
            return 'status-loading';
    }
}

function onSelectGlobalSearchResult(targetSection, id) {
    const dropdown = document.getElementById('global-search-dropdown');
    if (dropdown) dropdown.style.display = 'none';

    if (targetSection) {
        navigateTo(targetSection);
    }

    setTimeout(() => {
        let targetEl = null;
        switch (targetSection) {
            case 'equipment':
                targetEl = document.getElementById('filter-equipment-search') || document.getElementById('equipment-table-body');
                if (targetEl && targetEl.tagName === 'INPUT') {
                    targetEl.focus();
                }
                break;
            case 'laboratories':
                targetEl = document.getElementById('lab-table-body');
                break;
            case 'staff':
                targetEl = document.getElementById('staff-table-body');
                break;
            case 'schedules':
                targetEl = document.getElementById('schedule-table-body');
                break;
            case 'records':
                targetEl = document.getElementById('record-table-body');
                break;
            case 'documents':
                targetEl = document.getElementById('document-table-body');
                break;
        }
        if (targetEl) {
            targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }, 150);
}

// Close search dropdown on click outside
document.addEventListener('click', (event) => {
    const container = document.querySelector('.header-search-container');
    const dropdown = document.getElementById('global-search-dropdown');
    if (container && dropdown && !container.contains(event.target)) {
        dropdown.style.display = 'none';
    }
});

// -------------------------------------------------------------
// Step 14: Reports, Export & Compliance Documentation Module
// -------------------------------------------------------------
let currentReportCategory = 'calibration';
let currentReportType = 'calibration-status';
let reportCurrentPage = 0;
let reportTotalPages = 1;

function initReportsUI() {
    populateReportLabFilter();
    updateReportFilterVisibility(currentReportType);
    loadCurrentReport();
}

function populateReportLabFilter() {
    const labSelect = document.getElementById('report-filter-lab');
    if (!labSelect) return;

    const currentVal = labSelect.value;
    labSelect.innerHTML = '<option value="">All Permitted Labs</option>';

    const sourceLabs = (typeof labsCache !== 'undefined' && Array.isArray(labsCache) && labsCache.length > 0)
        ? labsCache
        : ((typeof dashboardLaboratoriesCache !== 'undefined' && Array.isArray(dashboardLaboratoriesCache) && dashboardLaboratoriesCache.length > 0)
            ? dashboardLaboratoriesCache
            : ((typeof laboratoriesCache !== 'undefined' && Array.isArray(laboratoriesCache) && laboratoriesCache.length > 0) ? laboratoriesCache : []));

    if (sourceLabs.length > 0) {
        sourceLabs.forEach(lab => {
            const opt = document.createElement('option');
            opt.value = lab.id;
            opt.textContent = `${lab.name} (${lab.labCode})`;
            labSelect.appendChild(opt);
        });
    }
    labSelect.value = currentVal;
}

function switchReportCategory(category) {
    currentReportCategory = category;

    // Toggle tab active classes
    document.querySelectorAll('.report-tab-btn').forEach(btn => btn.classList.remove('active'));
    const activeTab = document.getElementById(`tab-cat-${category}`);
    if (activeTab) activeTab.classList.add('active');

    // Toggle container visibility
    ['calibration', 'inventory', 'compliance'].forEach(cat => {
        const el = document.getElementById(`report-cards-${cat}`);
        if (el) el.style.display = (cat === category) ? 'grid' : 'none';
    });

    // Pick first report card in active category
    const activeContainer = document.getElementById(`report-cards-${category}`);
    if (activeContainer) {
        const firstCard = activeContainer.querySelector('.report-selection-card');
        if (firstCard) {
            const reportType = firstCard.getAttribute('data-report-type');
            selectReportType(reportType, firstCard);
        }
    }
}

function selectReportType(reportType, cardEl) {
    currentReportType = reportType;
    const norm = (reportType || '').toLowerCase().replace(/_/g, '-');

    // Highlight card
    document.querySelectorAll('.report-cat-card, .report-selection-card').forEach(card => card.classList.remove('active'));
    if (cardEl) {
        cardEl.classList.add('active');
    } else {
        document.querySelectorAll('.report-cat-card, .report-selection-card').forEach(card => {
            const attr = (card.getAttribute('data-report-type') || '').toLowerCase().replace(/_/g, '-');
            const oc = (card.getAttribute('onclick') || '').toLowerCase().replace(/_/g, '-');
            if (attr === norm || oc.includes(norm)) {
                card.classList.add('active');
            }
        });
    }

    updateReportFilterVisibility(reportType);
    reportCurrentPage = 0;
    loadCurrentReport();
}

function updateReportFilterVisibility(reportType) {
    const groupLab = document.getElementById('filter-group-lab');
    const groupFrom = document.getElementById('filter-group-date-from');
    const groupTo = document.getElementById('filter-group-date-to');
    const groupStatus = document.getElementById('filter-group-equip-status');
    const groupPriority = document.getElementById('filter-group-priority');
    const groupResult = document.getElementById('filter-group-result');
    const groupCalibrator = document.getElementById('filter-group-calibrator-type');

    // Reset visibility
    if (groupLab) groupLab.style.display = 'flex';
    if (groupFrom) groupFrom.style.display = 'flex';
    if (groupTo) groupTo.style.display = 'flex';
    if (groupStatus) groupStatus.style.display = 'none';
    if (groupPriority) groupPriority.style.display = 'none';
    if (groupResult) groupResult.style.display = 'none';
    if (groupCalibrator) groupCalibrator.style.display = 'none';

    const norm = (reportType || '').toLowerCase().replace(/_/g, '-');

    switch (norm) {
        case 'calibration-status':
            if (groupStatus) groupStatus.style.display = 'flex';
            if (groupPriority) groupPriority.style.display = 'flex';
            if (groupResult) groupResult.style.display = 'flex';
            break;
        case 'upcoming-calibrations':
        case 'overdue-calibrations':
            if (groupPriority) groupPriority.style.display = 'flex';
            break;
        case 'due-calibrations':
            if (groupPriority) groupPriority.style.display = 'flex';
            if (groupFrom) groupFrom.style.display = 'none';
            if (groupTo) groupTo.style.display = 'none';
            break;
        case 'calibration-history':
            if (groupResult) groupResult.style.display = 'flex';
            if (groupCalibrator) groupCalibrator.style.display = 'flex';
            break;
        case 'equipment':
        case 'equipment-inventory':
            if (groupStatus) groupStatus.style.display = 'flex';
            break;
        case 'laboratories':
        case 'laboratory-summary':
        case 'staff-assignments':
        case 'compliance-attention':
        case 'expired-certificates':
        case 'failed-calibrations':
        case 'out-of-calibration':
            if (groupFrom) groupFrom.style.display = 'none';
            if (groupTo) groupTo.style.display = 'none';
            break;
    }
}

function buildReportQueryParams() {
    const params = new URLSearchParams();

    const lab = document.getElementById('report-filter-lab') ? document.getElementById('report-filter-lab').value : '';
    const from = document.getElementById('report-filter-from') ? document.getElementById('report-filter-from').value : '';
    const to = document.getElementById('report-filter-to') ? document.getElementById('report-filter-to').value : '';
    const status = document.getElementById('report-filter-equip-status') ? document.getElementById('report-filter-equip-status').value : '';
    const priority = document.getElementById('report-filter-priority') ? document.getElementById('report-filter-priority').value : '';
    const result = document.getElementById('report-filter-result') ? document.getElementById('report-filter-result').value : '';
    const calibrator = document.getElementById('report-filter-calibrator-type') ? document.getElementById('report-filter-calibrator-type').value : '';

    if (lab) params.append('laboratoryId', lab);
    if (from) params.append('fromDate', from);
    if (to) params.append('toDate', to);
    if (status) params.append('equipmentStatus', status);
    if (priority) params.append('priority', priority);
    if (result) params.append('calibrationResult', result);
    if (calibrator) params.append('calibratorType', calibrator);

    const norm = (currentReportType || '').toLowerCase().replace(/_/g, '-');
    if (norm === 'expired-certificates') {
        params.append('category', 'EXPIRED_CERTIFICATE');
    } else if (norm === 'failed-calibrations') {
        params.append('category', 'FAILED_CALIBRATION');
    } else if (norm === 'out-of-calibration') {
        params.append('category', 'OUT_OF_CALIBRATION');
    }

    return params;
}

function resolveReportEndpoint(reportType) {
    const norm = (reportType || '').toLowerCase().replace(/_/g, '-');
    switch (norm) {
        case 'calibration-status': return '/api/reports/calibration-status';
        case 'upcoming-calibrations': return '/api/reports/upcoming-calibrations';
        case 'due-calibrations': return '/api/reports/due-calibrations';
        case 'overdue-calibrations': return '/api/reports/overdue-calibrations';
        case 'calibration-history': return '/api/reports/calibration-history';
        case 'equipment':
        case 'equipment-inventory':
            return '/api/reports/equipment';
        case 'laboratories':
        case 'laboratory-summary':
            return '/api/reports/laboratories';
        case 'staff-assignments':
            return '/api/reports/staff-assignments';
        case 'compliance-attention':
        case 'expired-certificates':
        case 'failed-calibrations':
        case 'out-of-calibration':
            return '/api/reports/compliance-attention';
        default: return '/api/reports/calibration-status';
    }
}

async function loadCurrentReport() {
    if (!authToken) return;

    const feedback = document.getElementById('report-feedback');
    const from = document.getElementById('report-filter-from') ? document.getElementById('report-filter-from').value : '';
    const to = document.getElementById('report-filter-to') ? document.getElementById('report-filter-to').value : '';
    if (from && to && from > to) {
        showFeedback(feedback, 'From Date cannot be after To Date', false);
        return;
    }

    try {
        const endpoint = resolveReportEndpoint(currentReportType);
        const params = buildReportQueryParams();
        params.append('page', reportCurrentPage);
        params.append('size', 20);

        const url = `${endpoint}?${params.toString()}`;
        const res = await fetch(url, { headers: getAuthHeaders() });
        const data = await res.json();
        safeSetJson(data);

        if (!res.ok) {
            throw new Error(data.message || `Failed to fetch report (${res.status})`);
        }

        renderReportTable(currentReportType, data);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    }
}

function renderReportTable(reportType, pageData) {
    const headEl = document.getElementById('report-table-head');
    const bodyEl = document.getElementById('report-table-body');
    const paginationBar = document.getElementById('report-pagination-bar');
    const paginationInfo = document.getElementById('report-pagination-info');
    const pageCurrent = document.getElementById('report-page-current');
    const btnPrev = document.getElementById('btn-report-prev');
    const btnNext = document.getElementById('btn-report-next');

    const content = pageData.content || [];
    const totalElements = pageData.totalElements || 0;
    reportTotalPages = pageData.totalPages || 1;

    // Update pagination controls
    if (paginationBar) paginationBar.style.display = totalElements > 0 ? 'flex' : 'none';
    if (paginationInfo) paginationInfo.textContent = `Showing ${content.length} of ${totalElements} record(s)`;
    if (pageCurrent) pageCurrent.textContent = `Page ${reportCurrentPage + 1} of ${Math.max(1, reportTotalPages)}`;
    if (btnPrev) btnPrev.disabled = (reportCurrentPage <= 0);
    if (btnNext) btnNext.disabled = (reportCurrentPage >= reportTotalPages - 1);

    if (content.length === 0) {
        if (headEl) headEl.innerHTML = '';
        if (bodyEl) {
            bodyEl.innerHTML = `
                <tr>
                    <td colspan="10" class="text-center empty-filter-state" style="padding: 2rem; color: var(--text-muted);">
                        <p>No records found for the selected filters.</p>
                    </td>
                </tr>
            `;
        }
        return;
    }

    let headersHtml = '';
    let rowsHtml = '';
    const norm = (reportType || '').toLowerCase().replace(/_/g, '-');

    switch (norm) {
        case 'calibration-status':
            headersHtml = `
                <tr>
                    <th>Equipment</th>
                    <th>Asset Tag</th>
                    <th>Laboratory</th>
                    <th>Equipment Status</th>
                    <th>Latest Calibration</th>
                    <th>Next Due Date</th>
                    <th>Latest Result</th>
                    <th>Priority</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><strong>${escapeHtml(item.equipmentName || '—')}</strong></td>
                    <td><code>${escapeHtml(item.assetTag || '—')}</code></td>
                    <td>${escapeHtml(item.laboratoryName || '—')}</td>
                    <td>${formatStatusBadge(item.equipmentStatus)}</td>
                    <td>${formatDate(item.latestCalibrationDate)}</td>
                    <td><strong>${formatDate(item.nextDueDate)}</strong></td>
                    <td>${formatResultBadge(item.latestCalibrationResult)}</td>
                    <td>${formatPriorityBadge(item.calibrationPriority)}</td>
                </tr>
            `).join('');
            break;

        case 'upcoming-calibrations':
        case 'due-calibrations':
            headersHtml = `
                <tr>
                    <th>Equipment</th>
                    <th>Asset Tag</th>
                    <th>Laboratory</th>
                    <th>Scheduled Date</th>
                    <th>Due Date</th>
                    <th>Priority</th>
                    <th>Status</th>
                    <th>Responsible Staff</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><strong>${escapeHtml(item.equipmentName || '—')}</strong></td>
                    <td><code>${escapeHtml(item.assetTag || '—')}</code></td>
                    <td>${escapeHtml(item.laboratoryName || '—')}</td>
                    <td>${formatDate(item.scheduledDate)}</td>
                    <td><strong>${formatDate(item.dueDate)}</strong></td>
                    <td>${formatPriorityBadge(item.priority)}</td>
                    <td>${formatStatusBadge(item.scheduleStatus)}</td>
                    <td>${escapeHtml(item.responsibleStaff || 'Unassigned')}</td>
                </tr>
            `).join('');
            break;

        case 'overdue-calibrations':
            headersHtml = `
                <tr>
                    <th>Equipment</th>
                    <th>Asset Tag</th>
                    <th>Laboratory</th>
                    <th>Scheduled Date</th>
                    <th>Due Date</th>
                    <th>Days Overdue</th>
                    <th>Priority</th>
                    <th>Responsible Staff</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><strong>${escapeHtml(item.equipmentName || '—')}</strong></td>
                    <td><code>${escapeHtml(item.assetTag || '—')}</code></td>
                    <td>${escapeHtml(item.laboratoryName || '—')}</td>
                    <td>${formatDate(item.scheduledDate)}</td>
                    <td><strong style="color:var(--danger, #dc2626);">${formatDate(item.dueDate)}</strong></td>
                    <td><span class="badge-priority badge-priority-critical">+${item.daysOverdue} days</span></td>
                    <td>${formatPriorityBadge(item.priority)}</td>
                    <td>${escapeHtml(item.responsibleStaff || 'Unassigned')}</td>
                </tr>
            `).join('');
            break;

        case 'calibration-history':
            headersHtml = `
                <tr>
                    <th>Equipment</th>
                    <th>Asset Tag</th>
                    <th>Laboratory</th>
                    <th>Calibration Date</th>
                    <th>Next Due Date</th>
                    <th>Result</th>
                    <th>Calibrator Type</th>
                    <th>Calibrator Name</th>
                    <th>Standards Used</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><strong>${escapeHtml(item.equipmentName || '—')}</strong></td>
                    <td><code>${escapeHtml(item.assetTag || '—')}</code></td>
                    <td>${escapeHtml(item.laboratoryName || '—')}</td>
                    <td>${formatDate(item.calibrationDate)}</td>
                    <td><strong>${formatDate(item.nextDueDate)}</strong></td>
                    <td>${formatResultBadge(item.calibrationResult || item.result)}</td>
                    <td><span class="badge-calib-type">${formatCalibratorType(item.calibratorType || item.calibratedByType)}</span></td>
                    <td>${escapeHtml(item.calibratorName || '—')}</td>
                    <td>${escapeHtml(item.standardsUsed || '—')}</td>
                </tr>
            `).join('');
            break;

        case 'equipment':
        case 'equipment-inventory':
            headersHtml = `
                <tr>
                    <th>Asset Tag</th>
                    <th>Serial Number</th>
                    <th>Equipment Name</th>
                    <th>Model</th>
                    <th>Manufacturer</th>
                    <th>Laboratory</th>
                    <th>Installation Date</th>
                    <th>Freq (Days)</th>
                    <th>Status</th>
                    <th>Next Due Date</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><code>${escapeHtml(item.assetTag || '—')}</code></td>
                    <td>${escapeHtml(item.serialNumber || '—')}</td>
                    <td><strong>${escapeHtml(item.equipmentName || '—')}</strong></td>
                    <td>${escapeHtml(item.modelNumber || '—')}</td>
                    <td>${escapeHtml(item.manufacturer || '—')}</td>
                    <td>${escapeHtml(item.laboratoryName || '—')}</td>
                    <td>${formatDate(item.installationDate)}</td>
                    <td>${item.calibrationFrequencyDays || '—'} days</td>
                    <td>${formatStatusBadge(item.currentStatus || item.status)}</td>
                    <td><strong>${formatDate(item.nextDueDate)}</strong></td>
                </tr>
            `).join('');
            break;

        case 'laboratories':
        case 'laboratory-summary':
            headersHtml = `
                <tr>
                    <th>Laboratory Name</th>
                    <th>Code</th>
                    <th>College</th>
                    <th>Department</th>
                    <th>Location</th>
                    <th>Status</th>
                    <th>Total Equipment</th>
                    <th>Active Staff</th>
                    <th>Attention Required</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><strong>${escapeHtml(item.laboratoryName || '—')}</strong></td>
                    <td><code>${escapeHtml(item.laboratoryCode || '—')}</code></td>
                    <td>${escapeHtml(item.collegeName || '—')}</td>
                    <td>${escapeHtml(item.department || '—')}</td>
                    <td>${escapeHtml(item.location || '—')}</td>
                    <td>${formatStatusBadge(item.laboratoryStatus)}</td>
                    <td>${item.totalEquipmentCount || 0}</td>
                    <td>${item.activeStaffCount || 0}</td>
                    <td>
                        ${(item.calibrationAttentionCount || 0) > 0
                            ? `<span class="badge-priority badge-priority-critical">${item.calibrationAttentionCount} items</span>`
                            : `<span style="color:#16a34a; font-weight:600;">&#10003; Compliant</span>`}
                    </td>
                </tr>
            `).join('');
            break;

        case 'staff-assignments':
            headersHtml = `
                <tr>
                    <th>Staff Name</th>
                    <th>Staff ID</th>
                    <th>Email</th>
                    <th>Designation</th>
                    <th>Laboratory</th>
                    <th>Role in Lab</th>
                    <th>Primary Contact</th>
                    <th>Status</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><strong>${escapeHtml(item.staffName || '—')}</strong></td>
                    <td><code>${escapeHtml(item.staffId || '—')}</code></td>
                    <td>${escapeHtml(item.email || '—')}</td>
                    <td>${escapeHtml(item.designation || '—')}</td>
                    <td>${escapeHtml(item.laboratoryName || '—')} (${escapeHtml(item.laboratoryCode || '')})</td>
                    <td><span class="badge-role">${formatRole(item.roleInLab)}</span></td>
                    <td>${item.isPrimaryContact ? '<span class="badge-primary-contact">&#9733; PRIMARY</span>' : '—'}</td>
                    <td>${formatStatusBadge(item.assignmentStatus)}</td>
                </tr>
            `).join('');
            break;

        case 'compliance-attention':
        case 'expired-certificates':
        case 'failed-calibrations':
        case 'out-of-calibration':
            headersHtml = `
                <tr>
                    <th>Category</th>
                    <th>Severity</th>
                    <th>Equipment</th>
                    <th>Asset Tag</th>
                    <th>Laboratory</th>
                    <th>Details</th>
                    <th>Action Required</th>
                    <th>Relevant Date</th>
                </tr>
            `;
            rowsHtml = content.map(item => `
                <tr>
                    <td><span class="badge-role">${escapeHtml(item.category ? item.category.replace(/_/g, ' ') : '—')}</span></td>
                    <td>${formatPriorityBadge(item.severity)}</td>
                    <td><strong>${escapeHtml(item.equipmentName || '—')}</strong></td>
                    <td><code>${escapeHtml(item.assetTag || '—')}</code></td>
                    <td>${escapeHtml(item.laboratoryName || '—')}</td>
                    <td>${escapeHtml(item.details || '—')}</td>
                    <td><strong style="color:#b91c1c;">${escapeHtml(item.actionRequired || '—')}</strong></td>
                    <td>${formatDate(item.relevantDate)}</td>
                </tr>
            `).join('');
            break;
    }

    if (headEl) headEl.innerHTML = headersHtml;
    if (bodyEl) bodyEl.innerHTML = rowsHtml;
}

function applyReportFilters() {
    reportCurrentPage = 0;
    loadCurrentReport();
}

function resetReportFilters() {
    const lab = document.getElementById('report-filter-lab');
    const from = document.getElementById('report-filter-from');
    const to = document.getElementById('report-filter-to');
    const status = document.getElementById('report-filter-equip-status');
    const priority = document.getElementById('report-filter-priority');
    const result = document.getElementById('report-filter-result');
    const calibrator = document.getElementById('report-filter-calibrator-type');

    if (lab) lab.value = '';
    if (from) from.value = '';
    if (to) to.value = '';
    if (status) status.value = '';
    if (priority) priority.value = '';
    if (result) result.value = '';
    if (calibrator) calibrator.value = '';

    reportCurrentPage = 0;
    loadCurrentReport();
}

function changeReportPage(delta) {
    const newPage = reportCurrentPage + delta;
    if (newPage >= 0 && newPage < reportTotalPages) {
        reportCurrentPage = newPage;
        loadCurrentReport();
    }
}

async function exportCurrentReportCsv() {
    if (!authToken) return;

    const feedback = document.getElementById('report-feedback');
    const from = document.getElementById('report-filter-from') ? document.getElementById('report-filter-from').value : '';
    const to = document.getElementById('report-filter-to') ? document.getElementById('report-filter-to').value : '';
    if (from && to && from > to) {
        showFeedback(feedback, 'From Date cannot be after To Date', false);
        return;
    }

    const exportBtn = document.getElementById('btn-export-report');
    setFormSubmitting(exportBtn, true, 'Exporting CSV...');
    try {
        const baseEndpoint = resolveReportEndpoint(currentReportType);
        const exportEndpoint = `${baseEndpoint}/export`;
        const params = buildReportQueryParams();
        params.append('format', 'csv');

        const exportUrl = `${exportEndpoint}?${params.toString()}`;

        const res = await fetch(exportUrl, {
            headers: {
                'Authorization': `Bearer ${authToken}`
            }
        });

        if (!res.ok) {
            let errorMsg = `Export failed (${res.status})`;
            try {
                const errData = await res.json();
                if (errData.message) errorMsg = errData.message;
            } catch (_) {}
            throw new Error(errorMsg);
        }

        const blob = await res.blob();
        let filename = `${currentReportType}-report-${new Date().toISOString().split('T')[0]}.csv`;

        const disposition = res.headers.get('Content-Disposition');
        if (disposition && disposition.indexOf('filename=') !== -1) {
            const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(disposition);
            if (matches != null && matches[1]) {
                filename = matches[1].replace(/['"]/g, '');
            }
        }

        const downloadUrl = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = downloadUrl;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(downloadUrl);
        a.remove();

        showFeedback(feedback, `Report exported successfully as ${filename}`, true);
    } catch (err) {
        showFeedback(feedback, err.message, false);
    } finally {
        setFormSubmitting(exportBtn, false);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    checkHealth();
    updateAuthUI();
    if (authToken) {
        const hash = window.location.hash ? window.location.hash.replace(/^#\/?/, '') : 'dashboard';
        navigateTo(hash || 'dashboard');
        updateUnreadCount();
    }
});

// Hash routing change listener
window.addEventListener('hashchange', () => {
    if (!authToken) return;
    const route = window.location.hash.replace(/^#\/?/, '') || 'dashboard';
    if (route !== currentRoute) {
        navigateTo(route);
    }
});

// Global click handler for modal backdrop & user dropdown
window.addEventListener('click', (e) => {
    // Backdrop close for modals
    if (e.target && e.target.classList.contains('modal-backdrop')) {
        e.target.style.display = 'none';
    }
    // Close user dropdown when clicking outside
    const dropdown = document.getElementById('user-dropdown-menu');
    const toggleBtn = document.getElementById('user-profile-toggle');
    if (dropdown && dropdown.style.display === 'block' && toggleBtn && !toggleBtn.contains(e.target) && !dropdown.contains(e.target)) {
        dropdown.style.display = 'none';
    }
});

// Escape key listener
window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        document.querySelectorAll('.modal-backdrop').forEach(m => {
            m.style.display = 'none';
        });
        closeUserDropdown();
        closeSidebar();
    }
});

