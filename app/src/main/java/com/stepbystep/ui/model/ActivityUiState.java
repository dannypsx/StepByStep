package com.stepbystep.ui.model;

import java.io.Serializable;
import java.util.ArrayList;

/** The screen only renders this state. Sensor/SQLite code belongs outside the views. */
public final class ActivityUiState implements Serializable {
    private static final long serialVersionUID = 1L;
    public enum Phase { IDLE, RECORDING, PAUSED, REVIEW, SAVED }
    public Phase phase = Phase.RECORDING;
    public int steps = 0, shakes = 0, goal = 100;
    public double seconds = 0;
    public boolean autoStart = true;
    public ArrayList<ActivityRecord> history = new ArrayList<>();
    public long historyVersion = 0, savedRecordId = -1;
    public ActivityRecord deletedRecord;
    public int deletedIndex;
    public long undoExpiresAt;
}
