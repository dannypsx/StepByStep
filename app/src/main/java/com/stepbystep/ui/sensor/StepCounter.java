package com.stepbystep.ui.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Reads walking steps from the hardware step counter (Sensor.TYPE_STEP_COUNTER).
 * The sensor reports the total number of steps since the last reboot, not per session, so this
 * class keeps the previous total and only reports the difference (new steps) to the listener.
 * Android 10+ needs the ACTIVITY_RECOGNITION runtime permission before start() delivers events.
 */
public final class StepCounter implements SensorEventListener {
    public interface Listener { void onSteps(int newSteps); }

    private final SensorManager manager;
    private final Sensor stepSensor;
    private final Listener listener;
    private float lastTotal = -1;
    private boolean running;

    public StepCounter(Context context, Listener listener) {
        this.manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.stepSensor = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        this.listener = listener;
    }

    /** False when the device has no step counter sensor. */
    public boolean isAvailable() { return stepSensor != null; }

    public void start() {
        if (running || stepSensor == null) return;
        // The first event after registering is only a baseline, so steps taken while the app
        // was not listening are never added to the current session.
        lastTotal = -1;
        running = manager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        if (!running) return;
        manager.unregisterListener(this);
        running = false;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float total = event.values[0];
        if (lastTotal >= 0 && total > lastTotal) listener.onSteps((int) (total - lastTotal));
        lastTotal = total;
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
