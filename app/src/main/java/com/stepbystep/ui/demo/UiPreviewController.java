package com.stepbystep.ui.demo;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.stepbystep.ui.ActivityUiActions;
import com.stepbystep.ui.model.ActivityRecord;
import com.stepbystep.ui.model.ActivityUiState;
import static com.stepbystep.ui.model.ActivityUiState.Phase.*;
import java.util.Calendar;

/**
 * UI DEMO ONLY: in-memory fixture data and simulated counts.
 * No SensorManager, runtime permissions, SQLite, networking or background tracking.
 * TODO(integration): implement ActivityUiActions and push ActivityUiState to the UI.
 */
public final class UiPreviewController implements ActivityUiActions {
    public interface Listener { void onStateChanged(ActivityUiState state); }
    private final ActivityUiState state;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastTick = SystemClock.elapsedRealtime();
    private long nextId;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            tick();
            if (state.deletedRecord != null && SystemClock.elapsedRealtime() >= state.undoExpiresAt) {
                state.deletedRecord = null;
            }
            emit();
            handler.postDelayed(this, 1000);
        }
    };

    public UiPreviewController(ActivityUiState restored, boolean autoStart, Listener listener) {
        this.listener = listener;
        this.state = restored == null ? fixtures(autoStart) : restored;
        nextId = 1;
        for (ActivityRecord record : state.history) nextId = Math.max(nextId, record.id + 1);
    }

    private static ActivityUiState fixtures(boolean autoStart) {
        ActivityUiState s = new ActivityUiState();
        s.autoStart = autoStart;
        if (!autoStart) { s.phase = IDLE; s.steps = s.shakes = 0; s.seconds = 0; }
        Calendar date = Calendar.getInstance();
        date.set(2026, Calendar.OCTOBER, 29, 18, 57, 40);
        date.set(Calendar.MILLISECOND, 0);
        long time = date.getTimeInMillis();
        s.history.add(new ActivityRecord(4, time, 12, 7, 13));
        s.history.add(new ActivityRecord(3, time - 40000, 0, 6, 16));
        s.history.add(new ActivityRecord(2, time - 100000, 0, 10, 29));
        s.history.add(new ActivityRecord(1, time - 4600000, 1, 0, 8));
        return s;
    }

    public ActivityUiState snapshot() { tick(); return state; }
    public void attach() { lastTick = SystemClock.elapsedRealtime(); handler.removeCallbacks(ticker); handler.post(ticker); }
    public void detach() { if (state.phase == RECORDING) pause(); handler.removeCallbacks(ticker); }
    private void tick() {
        long now = SystemClock.elapsedRealtime();
        if (state.phase == RECORDING) state.seconds += (now - lastTick) / 1000.0;
        lastTick = now;
    }
    private void emit() { listener.onStateChanged(state); }
    private void change(ActivityUiState.Phase phase) { tick(); state.phase = phase; emit(); }
    @Override public void start() { if (state.phase == IDLE) change(RECORDING); }
    @Override public void pause() { if (state.phase == RECORDING) change(PAUSED); }
    @Override public void resume() { if (state.phase == PAUSED || state.phase == REVIEW) change(RECORDING); }
    @Override public void finish() { if (state.phase == PAUSED) change(REVIEW); }
    @Override public void save() {
        if (state.phase != REVIEW) return;
        ActivityRecord record = new ActivityRecord(nextId++, System.currentTimeMillis(), state.steps, state.shakes, state.seconds);
        state.history.add(0, record);
        state.savedRecordId = record.id;
        state.historyVersion++;
        change(SAVED);
    }
    @Override public void newSession() {
        tick();
        state.phase = IDLE;
        state.steps = state.shakes = 0;
        state.seconds = 0;
        state.savedRecordId = -1;
        emit();
    }
    @Override public void discard() { if (state.phase == REVIEW) newSession(); }
    @Override public void delete(long id) {
        for (int index = 0; index < state.history.size(); index++) {
            if (state.history.get(index).id != id) continue;
            state.deletedRecord = state.history.remove(index);
            state.deletedIndex = index;
            state.undoExpiresAt = SystemClock.elapsedRealtime() + 8000;
            state.historyVersion++;
            if (state.savedRecordId == id && state.phase == SAVED) newSession();
            else emit();
            return;
        }
    }
    @Override public void undoDelete() {
        if (state.deletedRecord == null || SystemClock.elapsedRealtime() >= state.undoExpiresAt) return;
        state.history.add(Math.min(state.deletedIndex, state.history.size()), state.deletedRecord);
        state.deletedRecord = null;
        state.historyVersion++;
        emit();
    }
    @Override public void setGoal(int value) { if (value >= 1 && value <= 100000) { state.goal = value; emit(); } }
    @Override public void setAutoStart(boolean enabled) { state.autoStart = enabled; emit(); }
    public void addDemoSteps(int value) { if (state.phase == RECORDING) { tick(); state.steps += value; emit(); } }
    public void addDemoShake() { if (state.phase == RECORDING) { tick(); state.shakes++; emit(); } }
    public void showIdlePreview() { newSession(); }
}
