package com.stepbystep.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.stepbystep.ui.demo.UiPreviewController;
import com.stepbystep.ui.model.ActivityRecord;
import com.stepbystep.ui.model.ActivityUiState;
import com.stepbystep.ui.presentation.ActivityHistoryAdapter;
import com.stepbystep.ui.presentation.UiFormat;
import com.stepbystep.ui.sensor.ShakeDetector;
import com.stepbystep.ui.views.ActivityChartView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import static com.stepbystep.ui.model.ActivityUiState.Phase.*;

/** Native view layer. All counts, history and actions currently come from a UI preview controller. */
public final class MainActivity extends Activity {
    private UiPreviewController preview;
    // TODO(integration): replace this with your controller implementing ActivityUiActions.
    private ActivityUiActions actions;
    private ActivityUiState state;
    private ActivityHistoryAdapter historyAdapter, recentAdapter;
    private long renderedHistoryVersion = Long.MIN_VALUE;
    private int page = 0;
    private boolean bindingSettings;
    private ActivityUiState.Phase renderedPhase;
    private Dialog currentSheet;
    private ShakeDetector shakeDetector;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(getColor(R.color.bg));
        getWindow().setNavigationBarColor(getColor(R.color.bg));
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(R.layout.activity_main);
        installInsets();
        ActivityUiState restored = null;
        if (saved != null) {
            page = saved.getInt("ui_page", 0);
            if (Build.VERSION.SDK_INT >= 33) restored = saved.getSerializable("ui_preview", ActivityUiState.class);
            else restored = (ActivityUiState) saved.getSerializable("ui_preview");
        }
        historyAdapter = new ActivityHistoryAdapter(Integer.MAX_VALUE, this::showDetail);
        recentAdapter = new ActivityHistoryAdapter(1, this::showDetail);
        setupList(R.id.rvActivityHistory, historyAdapter);
        setupList(R.id.rvRecentHistory, recentAdapter);
        preview = new UiPreviewController(this, restored, getPreferences(MODE_PRIVATE).getBoolean("ui_auto_start", true), this::renderState);
        actions = preview;
        // Real shake counting: the preview controller only counts while the phase is RECORDING.
        shakeDetector = new ShakeDetector(this, preview::addDemoShake);
        if (!shakeDetector.isAvailable()) message("เครื่องนี้ไม่มีเซนเซอร์ Accelerometer จึงนับการเขย่าไม่ได้");
        click(R.id.navToday, () -> go(0));
        click(R.id.navHistory, () -> go(1));
        click(R.id.navInsights, () -> go(2));
        click(R.id.navSettings, () -> go(3));
        click(R.id.btnSettings, () -> go(3));
        click(R.id.btnAllHistory, () -> go(1));
        click(R.id.btnCumulative, () -> go(2));
        click(R.id.btnGoal, this::showGoal);
        click(R.id.btnSettingsGoal, this::showGoal);
        click(R.id.btnPreview, this::showPreview);
        click(R.id.btnSettingsPreview, this::showPreview);
        click(R.id.btnSensorPreview, () -> showSensor("ask"));
        click(R.id.btnPrimary, this::primaryAction);
        click(R.id.btnEnd, () -> actions.finish());
        click(R.id.btnSummaryBack, () -> { if (state.phase == SAVED) go(1); else actions.resume(); });
        click(R.id.btnDiscard, this::confirmDiscard);
        click(R.id.btnUndo, () -> { actions.undoDelete(); message("คืนกิจกรรมแล้ว ยอดสะสมและสถิติอัปเดตแล้ว"); });
        Switch auto = findViewById(R.id.switchAutoStart);
        auto.setOnCheckedChangeListener((button, checked) -> {
            if (!bindingSettings) {
                actions.setAutoStart(checked);
                getPreferences(MODE_PRIVATE).edit().putBoolean("ui_auto_start", checked).apply();
                message("เปลี่ยนตัวเลือกสำหรับการเปิดแอปครั้งถัดไปแล้ว");
            }
        });
        text(R.id.tvDate, new SimpleDateFormat("d MMM", new Locale("th", "TH")).format(new Date()));
        renderState(preview.snapshot());
    }

    private void installInsets() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            findViewById(R.id.root).setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return WindowInsets.CONSUMED;
            });
        }
    }
    private void setupList(int id, ActivityHistoryAdapter adapter) {
        RecyclerView list = findViewById(id);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setNestedScrollingEnabled(false);
        list.setAdapter(adapter);
        list.setItemAnimator(null);
    }
    private void click(int id, Runnable callback) { findViewById(id).setOnClickListener(v -> callback.run()); }
    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
    private static void text(View root, int id, String value) { ((TextView) root.findViewById(id)).setText(value); }
    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void message(String value) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show(); }

    /** Integration entry point: call on the main thread with state supplied by your real data layer. */
    public void renderState(ActivityUiState value) {
        state = value;
        text(R.id.tvCurrentSteps, String.format(Locale.US, "%02d", state.steps));
        text(R.id.tvCurrentShakes, String.format(Locale.US, "%02d", state.shakes));
        text(R.id.tvStepsPerMin, UiFormat.rate(state.steps, state.seconds));
        text(R.id.tvShakesPerMin, UiFormat.rate(state.shakes, state.seconds) + " ครั้ง");
        text(R.id.tvSessionDuration, UiFormat.duration(state.seconds));
        int percent = Math.round(state.steps * 100f / state.goal);
        text(R.id.btnGoal, "เป้า " + UiFormat.number(state.goal) + " ก้าว ›");
        text(R.id.tvGoalRemaining, state.steps >= state.goal ? "ถึงเป้าหมายแล้ว เก่งมาก!" : "อีก " + UiFormat.number(state.goal - state.steps) + " ก้าว ถึงเป้าหมาย");
        text(R.id.tvGoalPercent, percent + "%");
        ((ProgressBar) findViewById(R.id.goalProgress)).setProgress(Math.min(100, percent));
        ((ProgressBar) findViewById(R.id.goalProgress)).setContentDescription("ความคืบหน้า " + percent + " เปอร์เซ็นต์ ของเป้าหมาย " + state.goal + " ก้าว");
        String sessionLabel;
        switch (state.phase) {
            case RECORDING: sessionLabel = "● กำลังทำกิจกรรม"; break;
            case PAUSED: sessionLabel = "● พักกิจกรรมอยู่"; break;
            case REVIEW: sessionLabel = "จบกิจกรรมแล้ว"; break;
            case SAVED: sessionLabel = "บันทึกแล้ว"; break;
            default: sessionLabel = "พร้อมเริ่มกิจกรรม";
        }
        text(R.id.tvSessionState, sessionLabel);
        ((TextView) findViewById(R.id.tvSessionState)).setTextColor(getColor(state.phase == PAUSED ? R.color.gold : R.color.lime));
        findViewById(R.id.sessionCard).setBackgroundResource(state.phase == PAUSED ? R.drawable.session_paused : R.drawable.session);
        text(R.id.btnSettingsGoal, UiFormat.number(state.goal) + " ก้าว  ›");
        bindingSettings = true;
        ((Switch) findViewById(R.id.switchAutoStart)).setChecked(state.autoStart);
        bindingSettings = false;
        if (renderedHistoryVersion != state.historyVersion) {
            renderedHistoryVersion = state.historyVersion;
            historyAdapter.submitHistory(state.history);
            recentAdapter.submitHistory(state.history);
            String totals = UiFormat.number(UiFormat.totalSteps(state.history)) + " ก้าว · เขย่า " + UiFormat.number(UiFormat.totalShakes(state.history)) + " ครั้ง";
            text(R.id.tvCumulativeSummary, totals);
            text(R.id.tvHistoryTotals, totals);
            text(R.id.tvHistoryCount, state.history.size() + " เซสชัน");
            findViewById(R.id.tvEmptyHistory).setVisibility(state.history.isEmpty() ? View.VISIBLE : View.GONE);
            text(R.id.tvInsightSteps, UiFormat.number(UiFormat.totalSteps(state.history)));
            text(R.id.tvInsightMeta, state.history.size() + " กิจกรรม · เขย่า " + UiFormat.number(UiFormat.totalShakes(state.history)) + " ครั้ง");
            text(R.id.tvBestRecords, "สถิติสูงสุดจากประวัติ\nเดิน " + UiFormat.bestSteps(state.history) + " ก้าว · เขย่า " + UiFormat.bestShakes(state.history) + " ครั้ง");
            ((ActivityChartView) findViewById(R.id.activityChart)).setRecords(state.history);
        }
        boolean summary = page == 0 && (state.phase == REVIEW || state.phase == SAVED);
        int[] pages = {R.id.pageToday, R.id.pageHistory, R.id.pageInsights, R.id.pageSettings};
        for (int i = 0; i < pages.length; i++) findViewById(pages[i]).setVisibility(i == page && !summary ? View.VISIBLE : View.GONE);
        findViewById(R.id.pageSummary).setVisibility(summary ? View.VISIBLE : View.GONE);
        int[] nav = {R.id.navToday, R.id.navHistory, R.id.navInsights, R.id.navSettings};
        for (int i = 0; i < nav.length; i++) findViewById(nav[i]).setSelected(page == i);
        renderControls();
        if (summary) renderSummary();
        boolean undo = state.deletedRecord != null && SystemClock.elapsedRealtime() < state.undoExpiresAt;
        findViewById(R.id.toastBar).setVisibility(undo ? View.VISIBLE : View.GONE);
        if (renderedPhase != state.phase) {
            renderedPhase = state.phase;
            ((ScrollView) findViewById(R.id.mainScroll)).scrollTo(0, 0);
        }
    }

    private void renderControls() {
        boolean active = state.phase == RECORDING || state.phase == PAUSED || state.phase == REVIEW;
        findViewById(R.id.actionDock).setVisibility(page == 0 || active ? View.VISIBLE : View.GONE);
        Button primary = findViewById(R.id.btnPrimary), end = findViewById(R.id.btnEnd);
        end.setVisibility(page == 0 && state.phase == PAUSED ? View.VISIBLE : View.GONE);
        end.setBackgroundResource(R.drawable.button_end);
        end.setTextColor(getColor(R.color.coral));
        primary.setBackgroundResource(R.drawable.button_primary);
        primary.setTextColor(getColor(R.color.lime_ink));
        String label, hint; int icon;
        if (page != 0 && active) {
            label = state.phase == REVIEW ? "กลับไปสรุปกิจกรรม" : "กลับไปกิจกรรม";
            hint = state.phase == RECORDING ? "กิจกรรมของคุณยังจับเวลาอยู่" : "ข้อมูลกิจกรรมยังอยู่ครบ";
            icon = R.drawable.ic_step;
            primary.setBackgroundResource(R.drawable.button_secondary); primary.setTextColor(getColor(R.color.lime));
        } else switch (state.phase) {
            case RECORDING: label="พักกิจกรรม"; hint="พักได้ทุกเมื่อ แล้วกลับมาทำต่อ"; icon=R.drawable.ic_pause; primary.setBackgroundResource(R.drawable.button_pause); break;
            case PAUSED: label="ทำต่อ"; hint="หยุดเวลาแล้ว · ทุกก้าวยังอยู่ครบ"; icon=R.drawable.ic_play; break;
            case REVIEW: label="บันทึกกิจกรรม"; hint="ตรวจสรุปก่อนบันทึก · ยังกลับไปทำต่อได้"; icon=R.drawable.ic_save; break;
            case SAVED: label="กลับหน้าวันนี้"; hint="เก็บกิจกรรมนี้ไว้ในประวัติแล้ว"; icon=R.drawable.ic_home; break;
            default: label="เริ่มกิจกรรม"; hint="พร้อมเมื่อไร กดเริ่มได้เลย"; icon=R.drawable.ic_play;
        }
        primary.setText(label);
        primary.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        primary.setCompoundDrawableTintList(ColorStateList.valueOf(primary.getCurrentTextColor()));
        primary.setCompoundDrawablePadding(dp(7));
        end.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_finish, 0, 0, 0);
        end.setCompoundDrawableTintList(ColorStateList.valueOf(getColor(R.color.coral)));
        text(R.id.tvActionHint, hint);
    }
    private void primaryAction() {
        if (page != 0) { go(0); return; }
        switch (state.phase) {
            case IDLE: actions.start(); break;
            case RECORDING: actions.pause(); break;
            case PAUSED: actions.resume(); break;
            case REVIEW: actions.save(); break;
            case SAVED: actions.newSession(); break;
        }
    }
    private void go(int next) { page = next; renderState(state); ((ScrollView) findViewById(R.id.mainScroll)).scrollTo(0, 0); }
    private void renderSummary() {
        boolean done = state.phase == SAVED;
        text(R.id.tvSummaryEyebrow, done ? "SAVED TO YOUR JOURNAL" : "SESSION COMPLETE");
        text(R.id.tvSummaryTitle, done ? "เก็บก้าวครั้งนี้\nไว้เรียบร้อยแล้ว." : "อีกหนึ่งกิจกรรม\nที่คุณทำเพื่อตัวเอง.");
        text(R.id.tvSummarySubtitle, done ? "กลับมาดูความตั้งใจครั้งนี้ได้ในประวัติ" : "ดูสรุป แล้วเก็บทุกก้าวครั้งนี้ไว้");
        text(R.id.btnSummaryBack, done ? "‹ ดูประวัติกิจกรรม" : "‹ กลับไปทำกิจกรรมต่อ");
        text(R.id.tvSummarySteps, UiFormat.number(state.steps));
        text(R.id.tvSummaryDuration, UiFormat.duration(state.seconds));
        text(R.id.tvSummaryShakes, state.shakes + " ครั้ง");
        text(R.id.tvSummaryRate, UiFormat.rate(state.steps, state.seconds) + " ก้าว/นาที");
        text(R.id.tvSummaryShakeRate, UiFormat.rate(state.shakes, state.seconds) + " ครั้ง/นาที");
        text(R.id.tvSummaryGoal, Math.round(state.steps*100f/state.goal) + "% ของเป้าหมาย " + UiFormat.number(state.goal) + " ก้าว");
        boolean[] flags = done ? UiFormat.records(state.history, state.savedRecordId)
                : new boolean[]{state.steps>0 && state.steps>UiFormat.bestSteps(state.history), state.shakes>0 && state.shakes>UiFormat.bestShakes(state.history)};
        findViewById(R.id.summaryStepBadge).setVisibility(flags[0] ? View.VISIBLE : View.GONE);
        findViewById(R.id.summaryShakeBadge).setVisibility(flags[1] ? View.VISIBLE : View.GONE);
        findViewById(R.id.summaryBadges).setVisibility(flags[0] || flags[1] ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnDiscard).setVisibility(done ? View.GONE : View.VISIBLE);
    }

    private View bottomSheet(int layout) {
        if (currentSheet != null) currentSheet.dismiss();
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View content = getLayoutInflater().inflate(layout, null);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(false); scroll.setClipToPadding(false);
        scroll.addView(content);
        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(.65f);
            window.setGravity(Gravity.BOTTOM);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.show();
        if (window != null) {
            int width = getResources().getDisplayMetrics().widthPixels;
            int max = (int)(getResources().getDisplayMetrics().heightPixels*.86f);
            content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST));
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, Math.min(content.getMeasuredHeight(), max));
        }
        dialog.setCanceledOnTouchOutside(true);
        currentSheet = dialog;
        return content;
    }
    private void closeSheet() { if (currentSheet != null) { currentSheet.dismiss(); currentSheet = null; } }
    private void showDetail(ActivityRecord record) {
        View v = bottomSheet(R.layout.sheet_detail);
        text(v, R.id.tvDetailDate, UiFormat.date(record.createdAt));
        text(v, R.id.tvDetailSteps, UiFormat.number(record.steps));
        text(v, R.id.tvDetailShakes, UiFormat.number(record.shakes));
        text(v, R.id.tvDetailDuration, "ระยะเวลา   " + UiFormat.duration(record.seconds));
        text(v, R.id.tvDetailRate, "จังหวะเดินเฉลี่ย   " + UiFormat.rate(record.steps, record.seconds) + " ก้าว/นาที");
        text(v, R.id.tvDetailShakeRate, "เขย่าเฉลี่ย   " + UiFormat.rate(record.shakes, record.seconds) + " ครั้ง/นาที");
        boolean[] flags = UiFormat.records(state.history, record.id);
        v.findViewById(R.id.detailStepBadge).setVisibility(flags[0] ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.detailShakeBadge).setVisibility(flags[1] ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.detailBadges).setVisibility(flags[0]||flags[1] ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.btnDetailDone).setOnClickListener(button -> closeSheet());
        v.findViewById(R.id.btnDetailDelete).setOnClickListener(button -> confirmDelete(record));
    }
    private void confirmDelete(ActivityRecord record) {
        View v = bottomSheet(R.layout.sheet_confirm);
        text(v, R.id.tvConfirmPreview, UiFormat.date(record.createdAt) + "\n" + record.steps + " ก้าว · เขย่า " + record.shakes + " ครั้ง · " + UiFormat.duration(record.seconds));
        v.findViewById(R.id.btnCancel).setOnClickListener(button -> showDetail(record));
        v.findViewById(R.id.btnConfirm).setOnClickListener(button -> { closeSheet(); actions.delete(record.id); });
    }
    private void confirmDiscard() {
        View v = bottomSheet(R.layout.sheet_confirm);
        text(v, R.id.tvConfirmTitle, "ไม่บันทึกกิจกรรมนี้?");
        text(v, R.id.tvConfirmMessage, "ข้อมูลเซสชันนี้จะถูกล้าง\nกิจกรรมที่เคยบันทึกไว้ยังอยู่ครบ");
        text(v, R.id.tvConfirmPreview, state.steps + " ก้าว · เขย่า " + state.shakes + " ครั้ง · " + UiFormat.duration(state.seconds));
        text(v, R.id.btnCancel, "กลับไปสรุป"); text(v, R.id.btnConfirm, "ไม่บันทึก");
        v.findViewById(R.id.btnCancel).setOnClickListener(button -> closeSheet());
        v.findViewById(R.id.btnConfirm).setOnClickListener(button -> { closeSheet(); actions.discard(); });
    }
    private void showGoal() {
        View v = bottomSheet(R.layout.sheet_goal);
        EditText input = v.findViewById(R.id.etGoal); input.setText(String.valueOf(state.goal)); input.setSelectAllOnFocus(true);
        v.findViewById(R.id.btnGoalCancel).setOnClickListener(button -> closeSheet());
        v.findViewById(R.id.btnGoalSave).setOnClickListener(button -> {
            try {
                int value = Integer.parseInt(input.getText().toString());
                if (value < 1 || value > 100000) throw new NumberFormatException();
                actions.setGoal(value); closeSheet(); message("บันทึกเป้าหมายแล้ว");
            } catch (NumberFormatException ex) { input.setError("กรอกจำนวนเต็ม 1–100,000"); }
        });
    }
    private void showPreview() {
        View v = bottomSheet(R.layout.sheet_preview);
        boolean recording = state.phase == RECORDING;
        for (int id : new int[]{R.id.btnAddStep,R.id.btnAddShake,R.id.btnAddTen}) v.findViewById(id).setEnabled(recording);
        v.findViewById(R.id.btnAddStep).setOnClickListener(button -> preview.addDemoSteps(1));
        v.findViewById(R.id.btnAddTen).setOnClickListener(button -> preview.addDemoSteps(10));
        v.findViewById(R.id.btnAddShake).setOnClickListener(button -> preview.addDemoShake());
        v.findViewById(R.id.btnPreviewDone).setOnClickListener(button -> closeSheet());
    }
    private void showSensor(String mode) {
        View v = bottomSheet(R.layout.sheet_sensor);
        if (mode.equals("denied")) {
            text(v, R.id.tvSensorTitle, "ยังไม่ได้อนุญาตการนับก้าว");
            text(v, R.id.tvSensorMessage, "เปิดสิทธิ์กิจกรรมทางกายในหน้าตั้งค่าของแอปเพื่อใช้งานนับก้าว คุณยังดูประวัติที่บันทึกไว้ได้");
        } else if (mode.equals("missing")) {
            text(v, R.id.tvSensorTitle, "เครื่องนี้ไม่มีเซนเซอร์นับก้าว");
            text(v, R.id.tvSensorMessage, "ยังแสดงประวัติได้ ส่วนการนับก้าวจะใช้งานไม่ได้ หากมี Accelerometer ยังตรวจการเขย่าได้");
        } else if (mode.equals("allowed")) {
            text(v, R.id.tvSensorTitle, "พร้อมสำหรับทุกก้าวแล้ว");
            text(v, R.id.tvSensorMessage, "นี่คือตัวอย่างสถานะอนุญาต เพื่อนสามารถเชื่อมกับผลขอสิทธิ์จริงของ Android ได้");
        }
        v.findViewById(R.id.btnSensorAllow).setVisibility(mode.equals("ask") ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.btnSensorAsk).setOnClickListener(button -> showSensor("ask"));
        v.findViewById(R.id.btnSensorDenied).setOnClickListener(button -> showSensor("denied"));
        v.findViewById(R.id.btnSensorMissing).setOnClickListener(button -> showSensor("missing"));
        v.findViewById(R.id.btnSensorAllow).setOnClickListener(button -> showSensor("allowed"));
        v.findViewById(R.id.btnSensorClose).setOnClickListener(button -> closeSheet());
    }
    @Override protected void onResume() {
        super.onResume();
        if (preview != null) preview.attach();
        if (shakeDetector != null) shakeDetector.start();
    }
    @Override protected void onPause() {
        if (shakeDetector != null) shakeDetector.stop();
        if (preview != null) preview.detach();
        super.onPause();
    }
    @Override protected void onDestroy() { closeSheet(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putInt("ui_page", page); out.putSerializable("ui_preview", preview.snapshot()); super.onSaveInstanceState(out); }
    @Override public void onBackPressed() {
        if (page != 0) go(0);
        else if (state.phase == REVIEW) message("บันทึกกิจกรรม หรือเลือกกลับไปทำต่อจากปุ่มด้านบน");
        else { actions.pause(); super.onBackPressed(); }
    }
}
