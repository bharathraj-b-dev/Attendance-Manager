package com.bharath.attendancemanager.ui;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.calculator.AttendanceCalculator;
import com.bharath.attendancemanager.data.LocalStorage;
import com.bharath.attendancemanager.model.AttendanceSession;
import com.bharath.attendancemanager.model.SubjectAttendance;
import com.bharath.attendancemanager.ui.adapter.SessionAdapter;
import com.bharath.attendancemanager.ui.adapter.UpcomingSessionAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class SubjectDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_NAME = "extra_subject_name";

    private SubjectAttendance subject;
    private double targetPercentage;

    private TextView tvDetailTitle;
    private TextView tvDetailSubjectName;
    private TextView tvDetailStatusBadge;
    private TextView tvDetailPercentage;
    private TextView tvDetailPresent;
    private TextView tvDetailAbsent;
    private TextView tvDetailUpcoming;
    private TextView tvDetailBunkAdvice;

    private LinearLayout layoutComponentToggle;
    private MaterialButton btnComponentOverall;
    private MaterialButton btnComponentTheory;
    private MaterialButton btnComponentPractical;

    private MaterialButton btnTabMarked;
    private MaterialButton btnTabUpcoming;
    private TextView tvSessionSectionLabel;
    private RecyclerView rvDetailSessions;
    private TextView tvEmptySessions;
    private MaterialButton btnOpenCalculator;

    private boolean isShowingMarkedTab = true;
    private String selectedComponent = "Overall"; // Default selection is always Overall

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subject_details);

        LocalStorage storage = LocalStorage.getInstance(this);
        targetPercentage = storage.getTargetPercentage();

        String subjectName = getIntent().getStringExtra(EXTRA_SUBJECT_NAME);
        if (subjectName != null) {
            subject = storage.getSubjectByName(subjectName);
        }

        selectedComponent = "Overall"; // Always Overall by default on open

        initViews();
        setupWindowInsets();
        setupListeners();
        bindSubjectData();
    }

    private void setupWindowInsets() {
        View topBar = findViewById(R.id.topAppBar);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalStorage storage = LocalStorage.getInstance(this);
        targetPercentage = storage.getTargetPercentage();
        if (subject != null) {
            subject = storage.getSubjectByName(subject.getSubjectName());
        }
        selectedComponent = "Overall"; // Always Overall on resume/open
        bindSubjectData();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBackDetails);
        btnBack.setOnClickListener(v -> finish());

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailSubjectName = findViewById(R.id.tvDetailSubjectName);
        tvDetailStatusBadge = findViewById(R.id.tvDetailStatusBadge);
        tvDetailPercentage = findViewById(R.id.tvDetailPercentage);
        tvDetailPresent = findViewById(R.id.tvDetailPresent);
        tvDetailAbsent = findViewById(R.id.tvDetailAbsent);
        tvDetailUpcoming = findViewById(R.id.tvDetailUpcoming);
        tvDetailBunkAdvice = findViewById(R.id.tvDetailBunkAdvice);

        layoutComponentToggle = findViewById(R.id.layoutComponentToggle);
        btnComponentOverall = findViewById(R.id.btnComponentOverall);
        btnComponentTheory = findViewById(R.id.btnComponentTheory);
        btnComponentPractical = findViewById(R.id.btnComponentPractical);

        btnTabMarked = findViewById(R.id.btnTabMarked);
        btnTabUpcoming = findViewById(R.id.btnTabUpcoming);
        tvSessionSectionLabel = findViewById(R.id.tvSessionSectionLabel);
        rvDetailSessions = findViewById(R.id.rvDetailSessions);
        tvEmptySessions = findViewById(R.id.tvEmptySessions);
        btnOpenCalculator = findViewById(R.id.btnOpenCalculator);

        rvDetailSessions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnTabMarked.setOnClickListener(v -> {
            isShowingMarkedTab = true;
            updateTabState();
            renderSessions();
        });

        btnTabUpcoming.setOnClickListener(v -> {
            isShowingMarkedTab = false;
            updateTabState();
            renderSessions();
        });

        btnComponentOverall.setOnClickListener(v -> {
            selectedComponent = "Overall";
            updateComponentToggleState();
            bindSubjectData();
        });

        btnComponentTheory.setOnClickListener(v -> {
            if (subject != null && subject.hasComponents() && !subject.getComponents().isEmpty()) {
                selectedComponent = subject.getComponents().get(0).getComponentName();
                updateComponentToggleState();
                bindSubjectData();
            }
        });

        btnComponentPractical.setOnClickListener(v -> {
            if (subject != null && subject.hasComponents() && subject.getComponents().size() > 1) {
                selectedComponent = subject.getComponents().get(1).getComponentName();
                updateComponentToggleState();
                bindSubjectData();
            }
        });

        btnOpenCalculator.setOnClickListener(v -> {
            if (subject != null) {
                Intent intent = new Intent(this, CalculatorActivity.class);
                intent.putExtra(CalculatorActivity.EXTRA_SUBJECT_NAME, subject.getSubjectName());
                startActivity(intent);
            }
        });
    }

    private void bindSubjectData() {
        if (subject == null) {
            tvDetailTitle.setText("Subject Details");
            tvDetailSubjectName.setText("Subject not found");
            return;
        }

        tvDetailTitle.setText(subject.getSubjectName());
        tvDetailSubjectName.setText(subject.getSubjectName());

        // Stats to display: Combined (Overall) vs Component-specific
        int present = subject.getPresentCount();
        int absent = subject.getAbsentCount();
        int upcoming = subject.getUpcomingCount();
        double percentage = subject.getPercentage();

        if (subject.hasComponents() && !selectedComponent.equalsIgnoreCase("Overall")) {
            SubjectAttendance comp = null;
            for (SubjectAttendance c : subject.getComponents()) {
                if (c.getComponentName().equalsIgnoreCase(selectedComponent)) {
                    comp = c;
                    break;
                }
            }
            if (comp != null) {
                present = comp.getPresentCount();
                absent = comp.getAbsentCount();
                upcoming = comp.getUpcomingCount();
                percentage = comp.getPercentage();
            }
        }

        tvDetailPercentage.setText(String.format(Locale.US, "%.2f%%", percentage));
        tvDetailPresent.setText(String.valueOf(present));
        tvDetailAbsent.setText(String.valueOf(absent));
        tvDetailUpcoming.setText(String.valueOf(upcoming));

        // Status badge
        AttendanceCalculator.StatusCategory category = AttendanceCalculator.getStatusCategory(
                percentage, targetPercentage);
        tvDetailStatusBadge.setText(category.getLabel());
        tvDetailStatusBadge.setTextColor(ContextCompat.getColor(this, category.getTextColorRes()));
        tvDetailStatusBadge.setBackgroundResource(category.getBadgeDrawableRes());

        // Bunk advice
        String advice = AttendanceCalculator.getBunkAdvice(present, absent, targetPercentage);
        tvDetailBunkAdvice.setText(advice);

        // Component toggle visibility and button visibility based on available components
        if (subject.hasComponents()) {
            layoutComponentToggle.setVisibility(View.VISIBLE);
            List<SubjectAttendance> comps = subject.getComponents();
            if (comps.size() > 0) {
                btnComponentTheory.setVisibility(View.VISIBLE);
                btnComponentTheory.setText(comps.get(0).getComponentName());
            } else {
                btnComponentTheory.setVisibility(View.GONE);
            }
            if (comps.size() > 1) {
                btnComponentPractical.setVisibility(View.VISIBLE);
                btnComponentPractical.setText(comps.get(1).getComponentName());
            } else {
                btnComponentPractical.setVisibility(View.GONE);
            }
            updateComponentToggleState();
        } else {
            layoutComponentToggle.setVisibility(View.GONE);
        }

        // Update tab buttons text with counts
        List<AttendanceSession> markedList = getFilteredMarkedSessions();
        List<AttendanceSession> upcomingList = getFilteredUpcomingSessions();
        btnTabMarked.setText(String.format(Locale.US, "Marked Records (%d)", markedList.size()));
        btnTabUpcoming.setText(String.format(Locale.US, "Upcoming Classes (%d)", upcomingList.size()));

        updateTabState();
        renderSessions();
    }

    private void updateComponentToggleState() {
        if (selectedComponent.equalsIgnoreCase("Overall")) {
            setActiveButton(btnComponentOverall);
            setInactiveButton(btnComponentTheory);
            setInactiveButton(btnComponentPractical);
        } else {
            setInactiveButton(btnComponentOverall);
            if (subject != null && subject.hasComponents() && !subject.getComponents().isEmpty()) {
                if (subject.getComponents().get(0).getComponentName().equalsIgnoreCase(selectedComponent)) {
                    setActiveButton(btnComponentTheory);
                    setInactiveButton(btnComponentPractical);
                } else {
                    setInactiveButton(btnComponentTheory);
                    setActiveButton(btnComponentPractical);
                }
            }
        }
    }

    private void setActiveButton(MaterialButton btn) {
        if (btn == null || btn.getVisibility() != View.VISIBLE) return;
        btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
        btn.setTextColor(ContextCompat.getColor(this, R.color.text_inverse));
        btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
    }

    private void setInactiveButton(MaterialButton btn) {
        if (btn == null || btn.getVisibility() != View.VISIBLE) return;
        btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent)));
        btn.setTextColor(ContextCompat.getColor(this, R.color.primary));
        btn.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
    }

    private List<AttendanceSession> getFilteredMarkedSessions() {
        if (subject == null) return new ArrayList<>();
        List<AttendanceSession> filtered = new ArrayList<>();

        if (selectedComponent.equalsIgnoreCase("Overall")) {
            filtered.addAll(subject.getMarkedSessions());
        } else {
            for (AttendanceSession s : subject.getMarkedSessions()) {
                if (s.getComponentName() != null && s.getComponentName().equalsIgnoreCase(selectedComponent)) {
                    filtered.add(s);
                }
            }
        }

        // Sort chronologically by date and time
        sortSessionsChronologically(filtered);
        return filtered;
    }

    private List<AttendanceSession> getFilteredUpcomingSessions() {
        if (subject == null) return new ArrayList<>();
        List<AttendanceSession> filtered = new ArrayList<>();

        if (selectedComponent.equalsIgnoreCase("Overall")) {
            filtered.addAll(subject.getUpcomingSessions());
        } else {
            for (AttendanceSession s : subject.getUpcomingSessions()) {
                if (s.getComponentName() != null && s.getComponentName().equalsIgnoreCase(selectedComponent)) {
                    filtered.add(s);
                }
            }
        }

        // Sort chronologically by date and time
        sortSessionsChronologically(filtered);
        return filtered;
    }

    private void sortSessionsChronologically(List<AttendanceSession> sessions) {
        if (sessions == null) return;
        Collections.sort(sessions, (s1, s2) -> {
            long t1 = LocalStorage.parseSessionDateTime(s1.getDate(), s1.getTime());
            long t2 = LocalStorage.parseSessionDateTime(s2.getDate(), s2.getTime());
            if (t1 == 0L) return 1;
            if (t2 == 0L) return -1;
            return Long.compare(t1, t2);
        });
    }

    private void updateTabState() {
        if (isShowingMarkedTab) {
            btnTabMarked.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            btnTabMarked.setTextColor(ContextCompat.getColor(this, R.color.text_inverse));

            btnTabUpcoming.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent)));
            btnTabUpcoming.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            tvSessionSectionLabel.setText(R.string.marked_attendance_title);
        } else {
            btnTabUpcoming.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            btnTabUpcoming.setTextColor(ContextCompat.getColor(this, R.color.text_inverse));

            btnTabMarked.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent)));
            btnTabMarked.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

            tvSessionSectionLabel.setText(R.string.upcoming_sessions_title);
        }
    }

    private void renderSessions() {
        if (subject == null) return;

        if (isShowingMarkedTab) {
            List<AttendanceSession> marked = getFilteredMarkedSessions();
            if (marked.isEmpty()) {
                rvDetailSessions.setVisibility(View.GONE);
                tvEmptySessions.setVisibility(View.VISIBLE);
                tvEmptySessions.setText("No individual marked attendance records found.");
            } else {
                rvDetailSessions.setVisibility(View.VISIBLE);
                tvEmptySessions.setVisibility(View.GONE);
                SessionAdapter adapter = new SessionAdapter(this, marked);
                rvDetailSessions.setAdapter(adapter);
            }
        } else {
            List<AttendanceSession> upcoming = getFilteredUpcomingSessions();
            if (upcoming.isEmpty()) {
                rvDetailSessions.setVisibility(View.GONE);
                tvEmptySessions.setVisibility(View.VISIBLE);
                tvEmptySessions.setText("No upcoming or unmarked sessions found.");
            } else {
                rvDetailSessions.setVisibility(View.VISIBLE);
                tvEmptySessions.setVisibility(View.GONE);
                UpcomingSessionAdapter adapter = new UpcomingSessionAdapter(this, upcoming, (session, newStatus) -> {
                    LocalStorage.getInstance(this).markSessionLocally(session.getSubjectName().isEmpty() ? subject.getSubjectName() : session.getSubjectName(), session.getComponentName(), session.getDate(), session.getTime(), newStatus);
                    subject = LocalStorage.getInstance(this).getSubjectByName(subject.getSubjectName());
                    bindSubjectData();
                });
                rvDetailSessions.setAdapter(adapter);
            }
        }
    }
}