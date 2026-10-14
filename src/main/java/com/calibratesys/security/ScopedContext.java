package com.calibratesys.security;

import com.calibratesys.entity.Staff;

import java.util.Collection;
import java.util.List;

/**
 * Encapsulates the resolved laboratory data scope for the currently authenticated user.
 */
public record ScopedContext(
        Staff staff,
        boolean isAdmin,
        List<Long> laboratoryIds,
        boolean isEmptyScope
) {
    public boolean hasLabAccess(Long labId) {
        if (isAdmin) {
            return laboratoryIds == null || laboratoryIds.contains(labId);
        }
        return !isEmptyScope && laboratoryIds != null && laboratoryIds.contains(labId);
    }

    public boolean appliesToLaboratories() {
        return !isAdmin || (laboratoryIds != null && !laboratoryIds.isEmpty());
    }

    public boolean isUnrestricted() {
        return laboratoryIds == null;
    }

    public boolean isPermitted(Long labId) {
        return hasLabAccess(labId);
    }
}
