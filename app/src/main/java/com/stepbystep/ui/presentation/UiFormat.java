package com.stepbystep.ui.presentation;

import com.stepbystep.ui.model.ActivityRecord;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Presentation-only formatting and badge rules, shared by every screen. */
public final class UiFormat {
    private UiFormat() {}
    public static String number(int n) { return NumberFormat.getIntegerInstance(Locale.US).format(n); }
    public static String duration(double seconds) {
        long s = Math.max(0, (long) seconds);
        return s >= 3600 ? String.format(Locale.US, "%02d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
                : String.format(Locale.US, "%02d:%02d", s / 60, s % 60);
    }
    public static String rate(int n, double seconds) { return String.format(Locale.US, "%.1f", seconds > 0 ? n * 60.0 / seconds : 0); }
    public static String date(long time) { return new SimpleDateFormat("d MMM yyyy · HH:mm", new Locale("th", "TH")).format(new Date(time)) + " น."; }
    public static int totalSteps(List<ActivityRecord> h) { int n = 0; for (ActivityRecord r : h) n += r.steps; return n; }
    public static int totalShakes(List<ActivityRecord> h) { int n = 0; for (ActivityRecord r : h) n += r.shakes; return n; }
    public static int bestSteps(List<ActivityRecord> h) { int n = 0; for (ActivityRecord r : h) n = Math.max(n, r.steps); return n; }
    public static int bestShakes(List<ActivityRecord> h) { int n = 0; for (ActivityRecord r : h) n = Math.max(n, r.shakes); return n; }
    /** Newest-first list: compare to older remaining records, strict greater-than, non-zero. */
    public static boolean[] records(List<ActivityRecord> h, long id) {
        for (int i = 0; i < h.size(); i++) {
            ActivityRecord r = h.get(i);
            if (r.id != id) continue;
            int stepMax = 0, shakeMax = 0;
            for (int j = i + 1; j < h.size(); j++) { stepMax = Math.max(stepMax, h.get(j).steps); shakeMax = Math.max(shakeMax, h.get(j).shakes); }
            return new boolean[]{r.steps > stepMax && r.steps > 0, r.shakes > shakeMax && r.shakes > 0};
        }
        return new boolean[]{false, false};
    }
}
