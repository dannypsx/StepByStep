package com.stepbystep.ui.demo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.stepbystep.ui.ActivityUiActions;
import com.stepbystep.ui.db.ActivityDatabaseHelper;
import com.stepbystep.ui.model.ActivityRecord;
import com.stepbystep.ui.model.ActivityUiState;
import static com.stepbystep.ui.model.ActivityUiState.Phase.*;

/**
 * Controller managing activity recording, shake counting, and SQLite database persistence.
 * Note: Steps are bypassed with 0 on save until the step-counting feature is mapped.
 */
public final class UiPreviewController implements ActivityUiActions {
    public interface Listener { void onStateChanged(ActivityUiState state); }
    private final ActivityUiState state;
    private final Listener listener;
    private final ActivityDatabaseHelper dbHelper;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastTick = SystemClock.elapsedRealtime();
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

    public UiPreviewController(Context context, ActivityUiState restored, boolean autoStart, Listener listener) {
        this.listener = listener;
        this.dbHelper = ActivityDatabaseHelper.getInstance(context);
        this.state = restored == null ? initFromDb(autoStart) : restored;
    }

    private ActivityUiState initFromDb(boolean autoStart) {
        ActivityUiState s = new ActivityUiState();
        s.autoStart = autoStart;
        s.phase = autoStart ? RECORDING : IDLE;
        s.steps = 0;
        s.shakes = 0;
        s.seconds = 0;
        s.history.addAll(dbHelper.getAllRecords());
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
        // TODO(step-integration): replace 0 with state.steps when step sensor is mapped.
        int stepsToSave = 0;
        long createdAt = System.currentTimeMillis();
        long newId = dbHelper.insertRecord(createdAt, stepsToSave, state.shakes, state.seconds);
        if (newId == -1) return;
        state.steps = stepsToSave;
        ActivityRecord record = new ActivityRecord(newId, createdAt, stepsToSave, state.shakes, state.seconds);
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
            dbHelper.deleteRecord(id);
            if (state.savedRecordId == id && state.phase == SAVED) newSession();
            else emit();
            return;
        }
    }
    @Override public void undoDelete() {
        if (state.deletedRecord == null || SystemClock.elapsedRealtime() >= state.undoExpiresAt) return;
        dbHelper.insertWithId(state.deletedRecord);
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
