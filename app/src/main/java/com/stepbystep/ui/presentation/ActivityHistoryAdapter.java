package com.stepbystep.ui.presentation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.stepbystep.ui.R;
import com.stepbystep.ui.model.ActivityRecord;
import java.util.ArrayList;
import java.util.List;

/** Real RecyclerView/ViewHolder, ready to render data supplied by the integration layer. */
public final class ActivityHistoryAdapter extends RecyclerView.Adapter<ActivityHistoryAdapter.Holder> {
    public interface OnRecordClick { void onClick(ActivityRecord record); }
    private final OnRecordClick listener;
    private final int maxRows;
    private List<ActivityRecord> all = new ArrayList<>();
    public ActivityHistoryAdapter(int maxRows, OnRecordClick listener) { this.maxRows = maxRows; this.listener = listener; setHasStableIds(true); }
    public void submitHistory(List<ActivityRecord> records) {
        all = new ArrayList<>(records);
        // The preview is tiny. Replace with ListAdapter/DiffUtil for a production data source.
        notifyDataSetChanged();
    }
    @Override public long getItemId(int position) { return all.get(position).id; }
    @Override public int getItemCount() { return Math.min(maxRows, all.size()); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        ActivityRecord record = all.get(position);
        holder.date.setText(UiFormat.date(record.createdAt));
        holder.steps.setText(UiFormat.number(record.steps));
        holder.stats.setText("เขย่า " + UiFormat.number(record.shakes) + " ครั้ง · เวลา " + UiFormat.duration(record.seconds));
        holder.rates.setText(UiFormat.rate(record.steps, record.seconds) + " ก้าว/นาที · " + UiFormat.rate(record.shakes, record.seconds) + " เขย่า/นาที");
        boolean[] flags = UiFormat.records(all, record.id);
        holder.stepBadge.setVisibility(flags[0] ? View.VISIBLE : View.GONE);
        holder.shakeBadge.setVisibility(flags[1] ? View.VISIBLE : View.GONE);
        holder.badges.setVisibility(flags[0] || flags[1] ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onClick(record));
        holder.itemView.setContentDescription("ดูรายละเอียด " + UiFormat.date(record.createdAt) + " " + record.steps + " ก้าว เขย่า " + record.shakes + " ครั้ง");
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView date, steps, stats, rates, stepBadge, shakeBadge;
        final View badges;
        Holder(View v) { super(v); date = v.findViewById(R.id.itemDate); steps = v.findViewById(R.id.itemSteps); stats = v.findViewById(R.id.itemStats); rates = v.findViewById(R.id.itemRates); stepBadge = v.findViewById(R.id.badgeStep); shakeBadge = v.findViewById(R.id.badgeShake); badges = v.findViewById(R.id.itemBadges); }
    }
}
