package com.stepbystep.ui;

/** Integration contract: replace UiPreviewController with your real controller. */
public interface ActivityUiActions {
    void start();
    void pause();
    void resume();
    void finish();
    void save();
    void newSession();
    void discard();
    void delete(long recordId);
    void undoDelete();
    void setGoal(int goal);
    void setAutoStart(boolean enabled);
}
