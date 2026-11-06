package com.stepbystep.ui.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * Counts phone shakes from the accelerometer. No runtime permission is required for this sensor.
 * A shake is a reading whose total acceleration exceeds THRESHOLD_G, with at least MIN_GAP_MS
 * between two counted shakes so one physical shake is not counted several times.
 */
public final class ShakeDetector implements SensorEventListener {
    public interface Listener { void onShake(); }

    private static final float THRESHOLD_G = 2.5f;
    private static final long MIN_GAP_MS = 400;

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
        float gx = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gy = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gz = event.values[2] / SensorManager.GRAVITY_EARTH;
        double gForce = Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (gForce < THRESHOLD_G) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastShakeAt < MIN_GAP_MS) return;
        lastShakeAt = now;
        listener.onShake();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
