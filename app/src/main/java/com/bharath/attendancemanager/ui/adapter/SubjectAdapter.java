package com.bharath.attendancemanager.ui.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.calculator.AttendanceCalculator;
import com.bharath.attendancemanager.model.SubjectAttendance;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder> {

    public interface OnSubjectActionListener {
        void onViewDetails(SubjectAttendance subject);
        void onCalculate(SubjectAttendance subject);
    }

    private final Context context;
    private final List<SubjectAttendance> subjects = new ArrayList<>();
    private final double targetPercentage;
    private final OnSubjectActionListener listener;

    public SubjectAdapter(Context context, double targetPercentage, OnSubjectActionListener listener) {
        this.context = context;
        this.targetPercentage = targetPercentage;
        this.listener = listener;
    }

    public void updateData(List<SubjectAttendance> newSubjects) {
        subjects.clear();
        if (newSubjects != null) {
            subjects.addAll(newSubjects);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SubjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_subject_card, parent, false);
        return new SubjectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubjectViewHolder holder, int position) {
        SubjectAttendance subject = subjects.get(position);
        holder.bind(subject, targetPercentage, listener);
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    static class SubjectViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSubjectName;
        private final TextView tvPercentage;
        private final TextView tvRatio;
        private final TextView tvBunkSummary;
        private final TextView tvComponentsSummary;
        private final TextView tvSessionsSummary;
        private final ProgressBar progressBar;
        private final Button btnViewDetails;
        private final Button btnCalculate;

        public SubjectViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubjectName = itemView.findViewById(R.id.tvSubjectName);
            tvPercentage = itemView.findViewById(R.id.tvPercentage);
            tvRatio = itemView.findViewById(R.id.tvRatio);
            tvBunkSummary = itemView.findViewById(R.id.tvBunkSummary);
            tvComponentsSummary = itemView.findViewById(R.id.tvComponentsSummary);
            tvSessionsSummary = itemView.findViewById(R.id.tvSessionsSummary);
            progressBar = itemView.findViewById(R.id.progressBarAttendance);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnCalculate = itemView.findViewById(R.id.btnCalculate);
        }

        public void bind(SubjectAttendance subject, double target, OnSubjectActionListener listener) {
            Context ctx = itemView.getContext();
            tvSubjectName.setText(subject.getSubjectName());
            tvPercentage.setText(String.format(Locale.US, "%.2f%%", subject.getPercentage()));
            tvRatio.setText(String.format(Locale.US, "%d / %d attended", subject.getPresentCount(), subject.getScheduledCount()));

            if (subject.hasComponents()) {
                StringBuilder compsSb = new StringBuilder();
                for (int i = 0; i < subject.getComponents().size(); i++) {
                    SubjectAttendance c = subject.getComponents().get(i);
                    if (i > 0) compsSb.append("  •  ");
                    compsSb.append(c.getComponentName().isEmpty() ? "Component" : c.getComponentName())
                            .append(": ")
                            .append(c.getPresentCount()).append("/").append(c.getScheduledCount())
                            .append(" (").append(String.format(Locale.US, "%.2f%%", c.getPercentage())).append(")");
                }
                tvComponentsSummary.setVisibility(View.VISIBLE);
                tvComponentsSummary.setText(compsSb.toString());
            } else {
                tvComponentsSummary.setVisibility(View.GONE);
            }

            int markedCount = subject.getTotalMarkedCount();
            int upcomingCount = subject.getUpcomingCount();
            int pendingCount = subject.getPendingCount();

            if (pendingCount > 0) {
                tvSessionsSummary.setText(String.format(Locale.US, "Marked: %d  |  Pending: %d", markedCount, pendingCount));
            } else if (upcomingCount > 0) {
                tvSessionsSummary.setText(String.format(Locale.US, "Marked: %d  |  Upcoming: %d", markedCount, upcomingCount));
            } else {
                tvSessionsSummary.setText(String.format(Locale.US, "Present: %d  |  Absent: %d", subject.getPresentCount(), subject.getAbsentCount()));
            }

            // Progress bar
            int progress = (int) Math.round(subject.getPercentage());
            progressBar.setProgress(Math.min(100, Math.max(0, progress)));

            // Color coding based on status category
            AttendanceCalculator.StatusCategory category = AttendanceCalculator.getStatusCategory(subject.getPercentage(), target);

            int progressColor = ContextCompat.getColor(ctx,
                    category == AttendanceCalculator.StatusCategory.SAFE ? R.color.status_safe :
                    category == AttendanceCalculator.StatusCategory.WARNING ? R.color.status_warning :
                    R.color.status_critical);
            progressBar.setProgressTintList(ColorStateList.valueOf(progressColor));

            // Bunk / Catchup summary advice
            String bunkAdvice = AttendanceCalculator.getBunkAdvice(subject.getPresentCount(), subject.getAbsentCount(), target);
            tvBunkSummary.setText(bunkAdvice);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetails(subject);
            });

            btnViewDetails.setOnClickListener(v -> {
                if (listener != null) listener.onViewDetails(subject);
            });

            btnCalculate.setOnClickListener(v -> {
                if (listener != null) listener.onCalculate(subject);
            });
        }
    }
}
