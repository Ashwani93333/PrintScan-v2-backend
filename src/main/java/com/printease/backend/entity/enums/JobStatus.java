package com.printease.backend.entity.enums;

import java.util.Set;

public enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    CANCELLED;

    /**
     * Returns true if transitioning from this status to the given target is allowed.
     * Valid transitions:
     *   PENDING -> PROCESSING, PENDING -> CANCELLED
     *   PROCESSING -> COMPLETED, PROCESSING -> CANCELLED
     */
    public boolean canTransitionTo(JobStatus target) {
        return switch (this) {
            case PENDING -> target == PROCESSING || target == COMPLETED || target == CANCELLED;
            case PROCESSING -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
