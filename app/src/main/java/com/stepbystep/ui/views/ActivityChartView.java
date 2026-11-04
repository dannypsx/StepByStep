package com.stepbystep.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.stepbystep.ui.model.ActivityRecord;
import java.util.ArrayList;
import java.util.List;

/** Native decorative chart, independent of storage and sensors. */
public final class ActivityChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final ArrayList<Integer> values = new ArrayList<>();
    public ActivityChartView(Context c, AttributeSet attrs) { super(c, attrs); }
    public void setRecords(List<ActivityRecord> records) {
        values.clear();
        for (int i = Math.min(records.size(), 7) - 1; i >= 0; i--) values.add(records.get(i).steps);
        setContentDescription("ก้าวเดิน 7 กิจกรรมล่าสุด เรียงจากเก่าไปใหม่ " + values);
        invalidate();
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        paint.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        paint.setTextAlign(Paint.Align.CENTER);
        if (values.isEmpty()) { paint.setColor(Color.rgb(163,178,151)); canvas.drawText("บันทึกกิจกรรมแรกเพื่อดูภาพรวม", getWidth()/2f, getHeight()/2f, paint); return; }
        int max = 1; for (int n : values) max = Math.max(max, n);
        float gap = 12 * density, column = getWidth() / (float) values.size(), base = getHeight() - 27 * density;
        for (int i=0; i<values.size(); i++) {
            float x = column * i + column/2, h = Math.max(3*density, (base-28*density)*values.get(i)/max);
            float half = Math.min(22*density, (column-gap)/2);
            paint.setColor(i==values.size()-1 ? Color.rgb(220,248,139) : Color.rgb(120,147,93));
            rect.set(x-half, base-h, x+half, base); canvas.drawRoundRect(rect, 6*density, 6*density, paint);
            paint.setColor(Color.rgb(190,207,174)); canvas.drawText(String.valueOf(values.get(i)), x, base-h-8*density, paint);
            paint.setColor(Color.rgb(149,162,142)); canvas.drawText(String.valueOf(i+1), x, base+20*density, paint);
        }
    }
}
