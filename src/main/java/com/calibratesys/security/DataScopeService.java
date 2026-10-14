package com.calibratesys.security;

public interface DataScopeService {

    /**
     * Resolve the data scope for the currently authenticated user,
     * validating any specifically requested laboratory ID.
     *
     * @param requestedLaboratoryId optional laboratory ID filter from client
     * @return ScopedContext containing permitted laboratory IDs and metadata
     */
    ScopedContext resolveScope(Long requestedLaboratoryId);

    /**
     * Resolve the general data scope for the currently authenticated user without a specific lab filter.
     */
    ScopedContext resolveScope();
}
