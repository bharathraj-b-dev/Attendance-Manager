package com.bharath.attendancemanager.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.calculator.AttendanceCalculator;
import com.bharath.attendancemanager.data.LocalStorage;
import com.bharath.attendancemanager.model.SubjectAttendance;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalculatorActivity extends AppCompatActivity {

    public static final String EXTRA_SUBJECT_NAME = "extra_subject_name";

    private LocalStorage storage;
    private List<SubjectAttendance> subjects;
    private SubjectAttendance currentSubject;
    private double targetPercentage;

    private Spinner spinnerCalcSubject;
    private TextView tvCalcCurrentPercentage;
    private TextView tvCalcCurrentRatio;

    private MaterialButtonToggleGroup toggleScenario;
    private MaterialButton btnScenarioAttend;
    private MaterialButton btnScenarioMiss;

    private MaterialButton btnDecrement;
    private MaterialButton btnIncrement;
    private TextView tvClassCount;

    private MaterialButton chipPlus1;
    private MaterialButton chipPlus2;
    private MaterialButton chipPlus3;
    private MaterialButton chipPlus5;

    private TextView tvProjectedPercentage;
    private TextView tvPercentageDelta;
    private TextView tvProjectedBadge;
    private TextView tvProjectedRatio;
    private TextView tvCalcMathematicalGuidance;

    private boolean isAttend = true;
    private int classCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        storage = LocalStorage.getInstance(this);
        targetPercentage = storage.getTargetPercentage();
        subjects = storage.getSubjects();

        initViews();
        setupWindowInsets();
        setupSpinner();
        setupListeners();
        recalculate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        targetPercentage = storage.getTargetPercentage();
        subjects = storage.getSubjects();
        if (currentSubject != null) {
            currentSubject = storage.getSubjectByName(currentSubject.getSubjectName());
        }
        updateCurrentSubjectViews();
        recalculate();
    }

    private void setupWindowInsets() {
        View topBar = findViewById(R.id.calcTopBar);
        if (topBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btnBackCalc);
        btnBack.setOnClickListener(v -> finish());

        spinnerCalcSubject = findViewById(R.id.spinnerCalcSubject);
        tvCalcCurrentPercentage = findViewById(R.id.tvCalcCurrentPercentage);
        tvCalcCurrentRatio = findViewById(R.id.tvCalcCurrentRatio);

        toggleScenario = findViewById(R.id.toggleScenario);
        btnScenarioAttend = findViewById(R.id.btnScenarioAttend);
        btnScenarioMiss = findViewById(R.id.btnScenarioMiss);

        btnDecrement = findViewById(R.id.btnDecrement);
        btnIncrement = findViewById(R.id.btnIncrement);
        tvClassCount = findViewById(R.id.tvClassCount);

        chipPlus1 = findViewById(R.id.chipPlus1);
        chipPlus2 = findViewById(R.id.chipPlus2);
        chipPlus3 = findViewById(R.id.chipPlus3);
        chipPlus5 = findViewById(R.id.chipPlus5);

        tvProjectedPercentage = findViewById(R.id.tvProjectedPercentage);
        tvPercentageDelta = findViewById(R.id.tvPercentageDelta);
        tvProjectedBadge = findViewById(R.id.tvProjectedBadge);
        tvProjectedRatio = findViewById(R.id.tvProjectedRatio);
        tvCalcMathematicalGuidance = findViewById(R.id.tvCalcMathematicalGuidance);

        tvClassCount.setText(String.valueOf(classCount));
    }

    private void setupSpinner() {
        List<String> names = new ArrayList<>();
        if (subjects.isEmpty()) {
            names.add("No Subjects Available");
        } else {
            for (SubjectAttendance s : subjects) {
                names.add(s.getFullDisplayTitle());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names);
        spinnerCalcSubject.setAdapter(adapter);

        String preselected = getIntent().getStringExtra(EXTRA_SUBJECT_NAME);
        if (preselected != null && !subjects.isEmpty()) {
            for (int i = 0; i < subjects.size(); i++) {
                if (subjects.get(i).getSubjectName().equalsIgnoreCase(preselected.trim())) {
                    spinnerCalcSubject.setSelection(i);
                    currentSubject = subjects.get(i);
                    break;
                }
            }
        }

        if (currentSubject == null && !subjects.isEmpty()) {
            currentSubject = subjects.get(0);
        }

        spinnerCalcSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!subjects.isEmpty() && position < subjects.size()) {
                    currentSubject = subjects.get(position);
                    updateCurrentSubjectViews();
                    recalculate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        updateCurrentSubjectViews();
    }

    private void updateCurrentSubjectViews() {
        if (currentSubject == null) {
            tvCalcCurrentPercentage.setText("—");
            tvCalcCurrentRatio.setText("—");
            return;
        }

        tvCalcCurrentPercentage.setText(String.format(Locale.US, "%.2f%%", currentSubject.getPercentage()));
        tvCalcCurrentRatio.setText(String.format(Locale.US, "%d / %d", currentSubject.getPresentCount(), currentSubject.getScheduledCount()));
    }

    private void setupListeners() {
        toggleScenario.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                isAttend = (checkedId == R.id.btnScenarioAttend);
                recalculate();
            }
        });

        btnDecrement.setOnClickListener(v -> {
            if (classCount > 1) {
                classCount--;
                tvClassCount.setText(String.valueOf(classCount));
                recalculate();
            }
        });

        btnIncrement.setOnClickListener(v -> {
            classCount++;
            tvClassCount.setText(String.valueOf(classCount));
            recalculate();
        });

        chipPlus1.setOnClickListener(v -> addClasses(1));
        chipPlus2.setOnClickListener(v -> addClasses(2));
        chipPlus3.setOnClickListener(v -> addClasses(3));
        chipPlus5.setOnClickListener(v -> addClasses(5));
    }

    private void addClasses(int extra) {
        classCount = Math.max(1, classCount + extra);
        tvClassCount.setText(String.valueOf(classCount));
        recalculate();
    }

    private void recalculate() {
        if (currentSubject == null) return;

        int pres = currentSubject.getPresentCount();
        int abs = currentSubject.getAbsentCount();
        double currentPct = currentSubject.getPercentage();

        double projectedPct = AttendanceCalculator.simulateScenario(pres, abs, classCount, isAttend);
        double delta = projectedPct - currentPct;

        // Big projected percentage
        tvProjectedPercentage.setText(String.format(Locale.US, "%.2f%%", projectedPct));

        // Delta indicator
        if (delta >= 0) {
            tvPercentageDelta.setText(String.format(Locale.US, "+%.2f%%", delta));
            tvPercentageDelta.setBackgroundResource(R.drawable.bg_badge_safe);
            tvPercentageDelta.setTextColor(ContextCompat.getColor(this, R.color.status_safe_text));
        } else {
            tvPercentageDelta.setText(String.format(Locale.US, "%.2f%%", delta));
            tvPercentageDelta.setBackgroundResource(R.drawable.bg_badge_critical);
            tvPercentageDelta.setTextColor(ContextCompat.getColor(this, R.color.status_critical_text));
        }

        // Projected ratio
        int newPres = isAttend ? (pres + classCount) : pres;
        int newTotal = pres + abs + classCount;
        tvProjectedRatio.setText(String.format(Locale.US, "Projected: %d / %d classes attended", newPres, newTotal));

        // Projected status badge using single source of truth target
        AttendanceCalculator.StatusCategory projectedCat = AttendanceCalculator.getStatusCategory(projectedPct, targetPercentage);
        tvProjectedBadge.setText(projectedCat.getLabel());
        tvProjectedBadge.setTextColor(ContextCompat.getColor(this, projectedCat.getTextColorRes()));
        tvProjectedBadge.setBackgroundResource(projectedCat.getBadgeDrawableRes());

        // Mathematical explanation & bunk limit insight
        int newAbs = isAttend ? abs : (abs + classCount);
        StringBuilder explanation = new StringBuilder();
        if (isAttend) {
            explanation.append(String.format(Locale.US, "Attending %d class%s brings attendance to %.2f%% (+%.2f%%).\n\n",
                    classCount, classCount == 1 ? "" : "es", projectedPct, Math.abs(delta)));
        } else {
            explanation.append(String.format(Locale.US, "Missing %d class%s drops attendance to %.2f%% (%.2f%%).\n\n",
                    classCount, classCount == 1 ? "" : "es", projectedPct, delta));
        }

        String advice = AttendanceCalculator.getWhatIfGuidance(newPres, newAbs, targetPercentage);
        explanation.append("💡 ").append(advice);

        tvCalcMathematicalGuidance.setText(explanation.toString());
    }
}