package com.stepbystep.ui.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Counts phone shakes from the accelerometer. No runtime permission is required for this sensor.
 * At rest the accelerometer reads only gravity (about 9.81 m/s^2), so a shake is a reading whose
 * total acceleration differs from gravity by more than THRESHOLD, with at least MIN_GAP_MS
 * between two counted shakes so one physical shake is not counted several times.
 */
public final class ShakeDetector implements SensorEventListener {
    public interface Listener { void onShake(); }

    private static final float THRESHOLD = 3.2f; // m/s^2 away from gravity
    private static final long MIN_GAP_MS = 700;

    private final SensorManager manager;
    private final Sensor accelerometer;
    private final Listener listener;
    private long lastShakeAt;
    private boolean running;

    public ShakeDetector(Context context, Listener listener) {
        this.manager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = manager == null ? null : manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.listener = listener;
    }

    /** False when the device has no accelerometer. */
    public boolean isAvailable() { return accelerometer != null; }

    public void start() {
        if (running || accelerometer == null) return;
        running = manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stop() {
        if (!running) return;
        manager.unregisterListener(this);
        running = false;
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double magnitude = Math.sqrt(x * x + y * y + z * z);
        if (Math.abs(magnitude - SensorManager.GRAVITY_EARTH) < THRESHOLD) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastShakeAt < MIN_GAP_MS) return;
        lastShakeAt = now;
        listener.onShake();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
