package com.bharath.attendancemanager.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.model.AttendanceSession;
import com.bharath.attendancemanager.model.AttendanceStatus;

import java.util.ArrayList;
import java.util.List;

public class UpcomingSessionAdapter extends RecyclerView.Adapter<UpcomingSessionAdapter.UpcomingViewHolder> {

    public interface OnUpcomingSessionActionListener {
        void onSessionMark(AttendanceSession session, AttendanceStatus newStatus);
    }

    private final Context context;
    private final List<AttendanceSession> sessions = new ArrayList<>();
    private final OnUpcomingSessionActionListener listener;

    public UpcomingSessionAdapter(Context context, List<AttendanceSession> initialSessions, OnUpcomingSessionActionListener listener) {
        this.context = context;
        this.listener = listener;
        if (initialSessions != null) {
            this.sessions.addAll(initialSessions);
        }
    }

    public void updateData(List<AttendanceSession> newSessions) {
        sessions.clear();
        if (newSessions != null) {
            sessions.addAll(newSessions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UpcomingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session_record, parent, false);
        return new UpcomingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingViewHolder holder, int position) {
        AttendanceSession session = sessions.get(position);
        holder.bind(session, listener);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class UpcomingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSessionDate;
        private final TextView tvSessionTime;
        private final TextView tvSessionStatus;
        private final LinearLayout layoutActionButtons;
        private final Button btnSessionAttend;
        private final Button btnSessionBunk;
        private final Button btnSessionOd;

        public UpcomingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionDate = itemView.findViewById(R.id.tvSessionDate);
            tvSessionTime = itemView.findViewById(R.id.tvSessionTime);
            tvSessionStatus = itemView.findViewById(R.id.tvSessionStatus);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnSessionAttend = itemView.findViewById(R.id.btnSessionAttend);
            btnSessionBunk = itemView.findViewById(R.id.btnSessionBunk);
            btnSessionOd = itemView.findViewById(R.id.btnSessionOd);
        }

        public void bind(AttendanceSession session, OnUpcomingSessionActionListener listener) {
            Context ctx = itemView.getContext();
            String date = session.getDate();
            String time = session.getTime();
            String comp = session.getComponentName();

            tvSessionDate.setText(date.isEmpty() ? "Upcoming Class" : date);
            if (comp != null && !comp.isEmpty()) {
                tvSessionTime.setText((time.isEmpty() ? "Scheduled" : time) + " • " + comp);
            } else {
                tvSessionTime.setText(time.isEmpty() ? "Scheduled" : time);
            }

            AttendanceStatus status = session.getStatus();
            if (status == null) status = AttendanceStatus.UNMARKED;

            if (session.isUserMarked()) {
                tvSessionStatus.setText(status.getDisplayName() + " (LOCAL)");
            } else {
                tvSessionStatus.setText(status.getDisplayName());
            }
            tvSessionStatus.setTextColor(ContextCompat.getColor(ctx, status.getTextColorRes()));
            tvSessionStatus.setBackgroundResource(status.getBadgeBackgroundRes());

            layoutActionButtons.setVisibility(View.VISIBLE);

            btnSessionAttend.setOnClickListener(v -> {
                if (listener != null) listener.onSessionMark(session, AttendanceStatus.PRESENT);
            });

            btnSessionBunk.setOnClickListener(v -> {
                if (listener != null) listener.onSessionMark(session, AttendanceStatus.ABSENT);
            });

            btnSessionOd.setOnClickListener(v -> {
                if (listener != null) listener.onSessionMark(session, AttendanceStatus.ON_DUTY);
            });
        }
    }
}