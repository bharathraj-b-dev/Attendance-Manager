package com.bharath.attendancemanager.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.model.AttendanceSession;
import com.bharath.attendancemanager.model.AttendanceStatus;

import java.util.ArrayList;
import java.util.List;

public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.SessionViewHolder> {

    private final Context context;
    private final List<AttendanceSession> sessions = new ArrayList<>();

    public SessionAdapter(Context context, List<AttendanceSession> initialSessions) {
        this.context = context;
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
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session_record, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        AttendanceSession session = sessions.get(position);
        holder.bind(session);
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSessionDate;
        private final TextView tvSessionTime;
        private final TextView tvSessionStatus;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSessionDate = itemView.findViewById(R.id.tvSessionDate);
            tvSessionTime = itemView.findViewById(R.id.tvSessionTime);
            tvSessionStatus = itemView.findViewById(R.id.tvSessionStatus);
        }

        public void bind(AttendanceSession session) {
            Context ctx = itemView.getContext();
            String date = session.getDate();
            String time = session.getTime();
            String comp = session.getComponentName();

            tvSessionDate.setText(date.isEmpty() ? "Session Record" : date);
            if (comp != null && !comp.isEmpty()) {
                tvSessionTime.setText((time.isEmpty() ? "—" : time) + " • " + comp);
            } else {
                tvSessionTime.setText(time.isEmpty() ? "—" : time);
            }

            AttendanceStatus status = session.getStatus();
            if (status == null) status = AttendanceStatus.UNMARKED;

            tvSessionStatus.setText(status.getDisplayName());
            tvSessionStatus.setTextColor(ContextCompat.getColor(ctx, status.getTextColorRes()));
            tvSessionStatus.setBackgroundResource(status.getBadgeBackgroundRes());
        }
    }
}
