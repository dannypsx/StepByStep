package com.stepbystep.ui.model;

import java.io.Serializable;

/** Presentation data for one saved activity. Replace fixtures with SQLite rows. */
public final class ActivityRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    public final long id, createdAt;
    public final int steps, shakes;
    public final double seconds;

    public ActivityRecord(long id, long createdAt, int steps, int shakes, double seconds) {
        this.id = id;
        this.createdAt = createdAt;
        this.steps = steps;
        this.shakes = shakes;
        this.seconds = seconds;
    }
}
