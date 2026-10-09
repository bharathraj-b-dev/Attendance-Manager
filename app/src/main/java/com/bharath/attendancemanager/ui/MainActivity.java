package com.bharath.attendancemanager.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bharath.attendancemanager.R;
import com.bharath.attendancemanager.calculator.AttendanceCalculator;
import com.bharath.attendancemanager.data.LocalStorage;
import com.bharath.attendancemanager.digicampus.DigiCampusExtractionResult;
import com.bharath.attendancemanager.digicampus.DigiCampusImporter;
import com.bharath.attendancemanager.digicampus.DigiCampusWebViewManager;
import com.bharath.attendancemanager.model.AttendanceSession;
import com.bharath.attendancemanager.model.AttendanceStatus;
import com.bharath.attendancemanager.model.OverallAttendance;
import com.bharath.attendancemanager.model.SubjectAttendance;
import com.bharath.attendancemanager.notification.ClassReminderScheduler;
import com.bharath.attendancemanager.notification.NotificationHelper;
import com.bharath.attendancemanager.ui.adapter.SubjectAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SubjectAdapter.OnSubjectActionListener {

    private static final String KEY_CURRENT_TAB = "key_current_tab";

    private LocalStorage storage;
    private DigiCampusWebViewManager webViewManager;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    // Root containers
    private View layoutWelcome;
    private View layoutMainApp;
    private BottomNavigationView bottomNavigation;

    // View tabs
    private View viewDashboard;
    private View viewSubjects;
    private View viewImport;
    private View viewSettings;

    // Dashboard / Home views
    private TextView tvGreeting;
    private TextView tvOverallPercentage;
    private TextView tvOverallStatus;
    private TextView tvOverallPresent;
    private TextView tvOverallAbsent;
    private TextView tvOverallMarked;
    private TextView tvOverallAdvice;

    // Next Class Card
    private MaterialCardView cardNextClass;
    private TextView tvNextClassBadge;
    private TextView tvNextSubjectName;
    private TextView tvNextClassTime;
    private TextView tvNextCurrentPct;
    private Button btnNextAttend;
    private Button btnNextBunk;
    private Button btnNextOd;

    // Pending Attention Card on Home
    private MaterialCardView cardPendingAttention;
    private TextView tvPendingCountBadge;
    private TextView tvPendingSubjectName;
    private TextView tvPendingClassTime;
    private Button btnPendingAttend;
    private Button btnPendingBunk;
    private Button btnPendingOd;

    private View layoutEmptyDashboard;

    // Subjects tab views
    private MaterialCardView cardSubjectsNeedsAttention;
    private TextView tvSubjectsAttentionDetails;
    private Button btnSubjectsAttentionAttend;
    private Button btnSubjectsAttentionBunk;
    private Button btnSubjectsAttentionOd;
    private TextView tvSubjectsSubtitle;
    private ChipGroup chipGroupFilter;
    private RecyclerView rvAllSubjects;
    private SubjectAdapter allSubjectsAdapter;
    private int currentFilterMode = 0; // 0=All, 1=Safe, 2=Warning, 3=Critical

    // Import / WebView tab views
    private WebView webViewDigiCampus;
    private ProgressBar webProgressBar;
    private TextView tvWebAddress;
    private ImageView ivBannerStatusIcon;
    private TextView tvBannerStatusText;
    private Button btnImportAttendance;

    // Settings tab views
    private TextView tvTargetDisplay;
    private Slider sliderTarget;
    private EditText etStudentName;
    private MaterialSwitch switchNotifications;
    private TextView tvNotificationPermissionStatus;
    private Button btnOpenNotificationSettings;
    private MaterialButtonToggleGroup toggleThemeMode;
    private Button btnReloadDigiCampus;
    private Button btnResetDecisions;
    private Button btnClearData;

    private int activeTabId = R.id.nav_home;
    private AttendanceSession currentNextSession;
    private AttendanceSession currentPendingSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        storage = LocalStorage.getInstance(this);
        AppCompatDelegate.setDefaultNightMode(storage.getNightMode());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationHelper.initChannels(this);

        setupNotificationPermissionLauncher();

        initViews();
        setupWindowInsets();
        setupWebView();
        setupNavigation();
        setupBackButtonHandling();
        setupDashboard();
        setupSubjectsTab();
        setupSettingsTab();

        handleStartupFlow(savedInstanceState);
    }

    private void setupWindowInsets() {
        if (layoutMainApp != null) {
            ViewCompat.setOnApplyWindowInsetsListener(layoutMainApp, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), 0);
                return insets;
            });
        }
    }

    private void setupNotificationPermissionLauncher() {
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    storage.setRequestedNotificationPermission(true);
                    storage.setNotificationsEnabled(isGranted);
                    updateNotificationSettingsUi();
                    if (isGranted) {
                        Toast.makeText(this, "Class reminders enabled ✓", Toast.LENGTH_SHORT).show();
                        List<SubjectAttendance> subjects = storage.getSubjects();
                        if (!subjects.isEmpty()) {
                            ClassReminderScheduler.scheduleAllReminders(this, subjects);
                        }
                    } else {
                        Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initViews() {
        layoutWelcome = findViewById(R.id.layoutWelcome);
        layoutMainApp = findViewById(R.id.layoutMainApp);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        viewDashboard = findViewById(R.id.viewDashboard);
        viewSubjects = findViewById(R.id.viewSubjects);
        viewImport = findViewById(R.id.viewImport);
        viewSettings = findViewById(R.id.viewSettings);

        // Welcome screen button
        Button btnConnectDigiCampus = findViewById(R.id.btnConnectDigiCampus);
        btnConnectDigiCampus.setOnClickListener(v -> {
            storage.setHasOpenedDigiCampus(true);
            showMainApp();
            switchTab(R.id.nav_import);
            webViewManager.loadUrl(LocalStorage.DEFAULT_DIGICAMPUS_URL);
        });

        // Dashboard views
        tvGreeting = findViewById(R.id.tvGreeting);
        tvOverallPercentage = findViewById(R.id.tvOverallPercentage);
        tvOverallStatus = findViewById(R.id.tvOverallStatus);
        tvOverallPresent = findViewById(R.id.tvOverallPresent);
        tvOverallAbsent = findViewById(R.id.tvOverallAbsent);
        tvOverallMarked = findViewById(R.id.tvOverallMarked);
        tvOverallAdvice = findViewById(R.id.tvOverallAdvice);

        // Next Class Card
        cardNextClass = findViewById(R.id.cardNextClass);
        tvNextClassBadge = findViewById(R.id.tvNextClassBadge);
        tvNextSubjectName = findViewById(R.id.tvNextSubjectName);
        tvNextClassTime = findViewById(R.id.tvNextClassTime);
        tvNextCurrentPct = findViewById(R.id.tvNextCurrentPct);
        btnNextAttend = findViewById(R.id.btnNextAttend);
        btnNextBunk = findViewById(R.id.btnNextBunk);
        btnNextOd = findViewById(R.id.btnNextOd);

        // Pending Attention Card on Home
        cardPendingAttention = findViewById(R.id.cardPendingAttention);
        tvPendingCountBadge = findViewById(R.id.tvPendingCountBadge);
        tvPendingSubjectName = findViewById(R.id.tvPendingSubjectName);
        tvPendingClassTime = findViewById(R.id.tvPendingClassTime);
        btnPendingAttend = findViewById(R.id.btnPendingAttend);
        btnPendingBunk = findViewById(R.id.btnPendingBunk);
        btnPendingOd = findViewById(R.id.btnPendingOd);

        layoutEmptyDashboard = findViewById(R.id.layoutEmptyDashboard);

        Button btnEmptyImport = findViewById(R.id.btnEmptyImport);
        btnEmptyImport.setOnClickListener(v -> switchTab(R.id.nav_import));

        // Subjects tab views
        cardSubjectsNeedsAttention = findViewById(R.id.cardSubjectsNeedsAttention);
        tvSubjectsAttentionDetails = findViewById(R.id.tvSubjectsAttentionDetails);
        btnSubjectsAttentionAttend = findViewById(R.id.btnSubjectsAttentionAttend);
        btnSubjectsAttentionBunk = findViewById(R.id.btnSubjectsAttentionBunk);
        btnSubjectsAttentionOd = findViewById(R.id.btnSubjectsAttentionOd);
        tvSubjectsSubtitle = findViewById(R.id.tvSubjectsSubtitle);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        rvAllSubjects = findViewById(R.id.rvAllSubjects);

        // Import tab views
        webViewDigiCampus = findViewById(R.id.webViewDigiCampus);
        webProgressBar = findViewById(R.id.webProgressBar);
        tvWebAddress = findViewById(R.id.tvWebAddress);
        ivBannerStatusIcon = findViewById(R.id.ivBannerStatusIcon);
        tvBannerStatusText = findViewById(R.id.tvBannerStatusText);
        btnImportAttendance = findViewById(R.id.btnImportAttendance);

        ImageButton btnWebBack = findViewById(R.id.btnWebBack);
        btnWebBack.setOnClickListener(v -> {
            if (webViewManager.canGoBack()) {
                webViewManager.goBack();
            }
        });

        ImageButton btnWebReload = findViewById(R.id.btnWebReload);
        btnWebReload.setOnClickListener(v -> webViewManager.reload());

        ImageButton btnWebHome = findViewById(R.id.btnWebHome);
        btnWebHome.setOnClickListener(v -> webViewManager.loadUrl(LocalStorage.DEFAULT_DIGICAMPUS_URL));

        btnImportAttendance.setOnClickListener(v -> performAttendanceImport());

        // Settings tab views
        tvTargetDisplay = findViewById(R.id.tvTargetDisplay);
        sliderTarget = findViewById(R.id.sliderTarget);
        etStudentName = findViewById(R.id.etStudentName);
        switchNotifications = findViewById(R.id.switchNotifications);
        tvNotificationPermissionStatus = findViewById(R.id.tvNotificationPermissionStatus);
        btnOpenNotificationSettings = findViewById(R.id.btnOpenNotificationSettings);
        toggleThemeMode = findViewById(R.id.toggleThemeMode);
        btnReloadDigiCampus = findViewById(R.id.btnReloadDigiCampus);
        btnResetDecisions = findViewById(R.id.btnResetDecisions);
        btnClearData = findViewById(R.id.btnClearData);

        if (btnOpenNotificationSettings != null) {
            btnOpenNotificationSettings.setOnClickListener(v -> NotificationHelper.openNotificationSettings(this));
        }

        // Bind Next Class action buttons
        btnNextAttend.setOnClickListener(v -> markCurrentNextSession(AttendanceStatus.PRESENT));
        btnNextBunk.setOnClickListener(v -> markCurrentNextSession(AttendanceStatus.ABSENT));
        btnNextOd.setOnClickListener(v -> markCurrentNextSession(AttendanceStatus.ON_DUTY));

        // Bind Pending Attention action buttons
        btnPendingAttend.setOnClickListener(v -> markCurrentPendingSession(AttendanceStatus.PRESENT));
        btnPendingBunk.setOnClickListener(v -> markCurrentPendingSession(AttendanceStatus.ABSENT));
        btnPendingOd.setOnClickListener(v -> markCurrentPendingSession(AttendanceStatus.ON_DUTY));

        // Bind Subjects Needs Attention action buttons
        btnSubjectsAttentionAttend.setOnClickListener(v -> markCurrentPendingSession(AttendanceStatus.PRESENT));
        btnSubjectsAttentionBunk.setOnClickListener(v -> markCurrentPendingSession(AttendanceStatus.ABSENT));
        btnSubjectsAttentionOd.setOnClickListener(v -> markCurrentPendingSession(AttendanceStatus.ON_DUTY));
    }

    private void markCurrentNextSession(AttendanceStatus status) {
        if (currentNextSession != null && currentNextSession.getSubjectName() != null) {
            storage.markSessionLocally(currentNextSession.getSubjectName(), currentNextSession.getComponentName(), currentNextSession.getDate(), currentNextSession.getTime(), status);
            refreshAllData();
            Toast.makeText(this, String.format(Locale.US, "Marked %s as %s", currentNextSession.getFullSubjectTitle(), status.getDisplayName()), Toast.LENGTH_SHORT).show();
        }
    }

    private void markCurrentPendingSession(AttendanceStatus status) {
        if (currentPendingSession != null && currentPendingSession.getSubjectName() != null) {
            storage.markSessionLocally(currentPendingSession.getSubjectName(), currentPendingSession.getComponentName(), currentPendingSession.getDate(), currentPendingSession.getTime(), status);
            refreshAllData();
            Toast.makeText(this, String.format(Locale.US, "Marked %s as %s", currentPendingSession.getFullSubjectTitle(), status.getDisplayName()), Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================================
    // STARTUP & STATE RESTORATION
    // ============================================================

    private void handleStartupFlow(Bundle savedInstanceState) {
        boolean hasOpenedBefore = storage.hasOpenedDigiCampus();

        if (savedInstanceState != null) {
            boolean restored = webViewManager.restoreState(savedInstanceState);
            activeTabId = savedInstanceState.getInt(KEY_CURRENT_TAB, R.id.nav_home);
            if (restored || hasOpenedBefore) {
                showMainApp();
                switchTab(activeTabId);
                return;
            }
        }

        if (hasOpenedBefore) {
            showMainApp();
            webViewManager.loadInitialOrLastUrl();
            switchTab(R.id.nav_home);
        } else {
            showWelcomeScreen();
        }
    }

    private void showWelcomeScreen() {
        layoutWelcome.setVisibility(View.VISIBLE);
        layoutMainApp.setVisibility(View.GONE);
    }

    private void showMainApp() {
        layoutWelcome.setVisibility(View.GONE);
        layoutMainApp.setVisibility(View.VISIBLE);
        refreshAllData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotificationSettingsUi();
        refreshAllData();
    }

    // ============================================================
    // WEBVIEW SETUP
    // ============================================================

    private void setupWebView() {
        webViewManager = new DigiCampusWebViewManager(webViewDigiCampus, storage);
        webViewManager.setPageListener(new DigiCampusWebViewManager.PageListener() {
            @Override
            public void onPageStarted(String url) {
                webProgressBar.setVisibility(View.VISIBLE);
                tvWebAddress.setText(url != null ? url : "");
            }

            @Override
            public void onPageFinished(String url) {
                webProgressBar.setVisibility(View.GONE);
                tvWebAddress.setText(url != null ? url : "");
                checkForAttendancePageDetection();
            }

            @Override
            public void onProgressChanged(int progress) {
                webProgressBar.setProgress(progress);
                if (progress >= 100) {
                    webProgressBar.setVisibility(View.GONE);
                } else {
                    webProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void checkForAttendancePageDetection() {
        String currentUrl = tvWebAddress.getText().toString().toLowerCase(Locale.ROOT);
        if (currentUrl.contains("attendance")) {
            tvBannerStatusText.setText("✓ Attendance page detected! Tap 'Import Attendance' to sync.");
            tvBannerStatusText.setTextColor(ContextCompat.getColor(this, R.color.status_safe_text));
            ivBannerStatusIcon.setImageResource(R.drawable.ic_check_circle);
            ivBannerStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_safe));
        } else {
            tvBannerStatusText.setText("Navigate to Academics → Attendance Records, then tap Import below.");
            tvBannerStatusText.setTextColor(ContextCompat.getColor(this, R.color.primary_dark));
            ivBannerStatusIcon.setImageResource(R.drawable.ic_nav_import);
            ivBannerStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.primary));
        }
    }

    // ============================================================
    // NAVIGATION & BACK BUTTON
    // ============================================================

    private void setupNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            showTabContent(item.getItemId());
            return true;
        });
    }

    private void switchTab(int tabId) {
        if (bottomNavigation.getSelectedItemId() != tabId) {
            bottomNavigation.setSelectedItemId(tabId);
        } else {
            showTabContent(tabId);
        }
    }

    private void showTabContent(int tabId) {
        activeTabId = tabId;

        viewDashboard.setVisibility(tabId == R.id.nav_home ? View.VISIBLE : View.GONE);
        viewSubjects.setVisibility(tabId == R.id.nav_subjects ? View.VISIBLE : View.GONE);
        viewImport.setVisibility(tabId == R.id.nav_import ? View.VISIBLE : View.GONE);
        viewSettings.setVisibility(tabId == R.id.nav_settings ? View.VISIBLE : View.GONE);

        if (tabId == R.id.nav_home || tabId == R.id.nav_subjects) {
            refreshAllData();
        }
    }

    private void setupBackButtonHandling() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (activeTabId == R.id.nav_import) {
                    if (webViewManager.canGoBack()) {
                        webViewManager.goBack();
                        return;
                    }
                    switchTab(R.id.nav_home);
                    return;
                }

                if (activeTabId != R.id.nav_home) {
                    switchTab(R.id.nav_home);
                    return;
                }

                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    // ============================================================
    // DASHBOARD & SUBJECTS SETUP
    // ============================================================

    private void setupDashboard() {
        // Home screen focuses strictly on Overall Attendance, Next Class, & Pending Attention
    }

    private void setupSubjectsTab() {
        rvAllSubjects.setLayoutManager(new LinearLayoutManager(this));
        allSubjectsAdapter = new SubjectAdapter(this, storage.getTargetPercentage(), this);
        rvAllSubjects.setAdapter(allSubjectsAdapter);

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipSafe)) {
                currentFilterMode = 1;
            } else if (checkedIds.contains(R.id.chipWarning)) {
                currentFilterMode = 2;
            } else if (checkedIds.contains(R.id.chipCritical)) {
                currentFilterMode = 3;
            } else {
                currentFilterMode = 0;
            }
            filterAndDisplaySubjects();
        });
    }

    private void setupSettingsTab() {
        double currentTarget = storage.getTargetPercentage();
        sliderTarget.setValue((float) currentTarget);
        tvTargetDisplay.setText(String.format(Locale.US, "%.0f%%", currentTarget));

        sliderTarget.addOnChangeListener((slider, value, fromUser) -> {
            storage.setTargetPercentage(value);
            tvTargetDisplay.setText(String.format(Locale.US, "%.0f%%", value));
            refreshAllData();
        });

        etStudentName.setText(storage.getUserName());
        etStudentName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                storage.setUserName(s.toString().trim());
                updateGreeting();
            }
        });

        updateNotificationSettingsUi();

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                storage.setNotificationsEnabled(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !NotificationHelper.hasNotificationPermission(this)) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    updateNotificationSettingsUi();
                    NotificationHelper.openNotificationSettings(this);
                } else {
                    updateNotificationSettingsUi();
                    List<SubjectAttendance> subjects = storage.getSubjects();
                    if (!subjects.isEmpty()) {
                        ClassReminderScheduler.scheduleAllReminders(this, subjects);
                    }
                }
            } else {
                storage.setNotificationsEnabled(false);
                updateNotificationSettingsUi();
                ClassReminderScheduler.cancelAllReminders(this);
            }
        });

        // Setup Dark Mode toggle
        if (toggleThemeMode != null) {
            int currentMode = storage.getNightMode();
            if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) {
                toggleThemeMode.check(R.id.btnThemeLight);
            } else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                toggleThemeMode.check(R.id.btnThemeDark);
            } else {
                toggleThemeMode.check(R.id.btnThemeSystem);
            }

            toggleThemeMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (isChecked) {
                    int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    if (checkedId == R.id.btnThemeLight) {
                        mode = AppCompatDelegate.MODE_NIGHT_NO;
                    } else if (checkedId == R.id.btnThemeDark) {
                        mode = AppCompatDelegate.MODE_NIGHT_YES;
                    }
                    storage.setNightMode(mode);
                }
            });
        }

        btnReloadDigiCampus.setOnClickListener(v -> {
            webViewManager.loadUrl(LocalStorage.DEFAULT_DIGICAMPUS_URL);
            switchTab(R.id.nav_import);
            Toast.makeText(this, "Reloading DigiCampus login page...", Toast.LENGTH_SHORT).show();
        });

        if (btnResetDecisions != null) {
            btnResetDecisions.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Reset Local Decisions")
                        .setMessage("Revert all locally marked attendance decisions back to unmarked?")
                        .setPositiveButton("Reset", (dialog, which) -> {
                            storage.resetLocalDecisions();
                            refreshAllData();
                            Toast.makeText(this, "Local attendance decisions reset.", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        btnClearData.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reset Attendance Data")
                    .setMessage("Are you sure you want to delete all imported subjects and session records? This cannot be undone.")
                    .setPositiveButton("Reset", (dialog, which) -> {
                        storage.clearSubjectsOnly();
                        refreshAllData();
                        Toast.makeText(this, "All attendance data cleared.", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void updateNotificationSettingsUi() {
        if (switchNotifications == null) return;

        boolean systemAllowed = NotificationHelper.isSystemNotificationEnabled(this);
        boolean userPrefOn = storage.areNotificationsEnabled();

        switchNotifications.setChecked(userPrefOn && systemAllowed);

        if (!systemAllowed) {
            if (tvNotificationPermissionStatus != null) {
                tvNotificationPermissionStatus.setVisibility(View.VISIBLE);
            }
            if (btnOpenNotificationSettings != null) {
                btnOpenNotificationSettings.setVisibility(View.VISIBLE);
            }
        } else {
            if (tvNotificationPermissionStatus != null) {
                tvNotificationPermissionStatus.setVisibility(View.GONE);
            }
            if (btnOpenNotificationSettings != null) {
                btnOpenNotificationSettings.setVisibility(View.GONE);
            }
        }
    }

    // ============================================================
    // DATA BINDING & REFRESH
    // ============================================================

    private void refreshAllData() {
        updateGreeting();

        List<SubjectAttendance> subjects = storage.getSubjects();
        double target = storage.getTargetPercentage();

        if (subjects.isEmpty()) {
            layoutEmptyDashboard.setVisibility(View.VISIBLE);
            rvAllSubjects.setVisibility(View.GONE);
            cardNextClass.setVisibility(View.GONE);
            cardPendingAttention.setVisibility(View.GONE);
            if (cardSubjectsNeedsAttention != null) cardSubjectsNeedsAttention.setVisibility(View.GONE);

            tvOverallPercentage.setText("0.0%");
            tvOverallStatus.setText("No Data");
            tvOverallPresent.setText("0");
            tvOverallAbsent.setText("0");
            tvOverallMarked.setText("0");
            tvOverallAdvice.setText("Optionally import attendance from DigiCampus or manage locally.");
            tvSubjectsSubtitle.setText("0 subjects");
            return;
        }

        layoutEmptyDashboard.setVisibility(View.GONE);
            rvAllSubjects.setVisibility(View.VISIBLE);

        OverallAttendance overall = OverallAttendance.calculate(subjects, target);
        tvOverallPercentage.setText(overall.getFormattedPercentage());
        tvOverallPresent.setText(String.valueOf(overall.getTotalPresent()));
        tvOverallAbsent.setText(String.valueOf(overall.getTotalAbsent()));
        tvOverallMarked.setText(String.valueOf(overall.getTotalMarked()));

        AttendanceCalculator.StatusCategory overallCategory = AttendanceCalculator.getStatusCategory(
                overall.getOverallPercentage(), target);
        tvOverallStatus.setText(overallCategory.getLabel());
        tvOverallStatus.setTextColor(ContextCompat.getColor(this, overallCategory.getTextColorRes()));
        tvOverallStatus.setBackgroundResource(overallCategory.getBadgeDrawableRes());

        tvOverallAdvice.setText(String.format(Locale.US, "✓ %d safe  •  ⚠️ %d warning  •  🚨 %d critical",
                overall.getSafeSubjectsCount(), overall.getWarningSubjectsCount(), overall.getCriticalSubjectsCount()));

        tvSubjectsSubtitle.setText(String.format(Locale.US, "%d subjects", subjects.size()));

        TextView tvSubjectsTargetExplanation = findViewById(R.id.tvSubjectsTargetExplanation);
        if (tvSubjectsTargetExplanation != null) {
            tvSubjectsTargetExplanation.setText(String.format(Locale.US, "Your attendance target: %.0f%%", target));
        }

        // Bind GLOBAL Next Class card and Pending Attention card across ALL subjects
        bindNextAndPendingClasses();

        filterAndDisplaySubjects();
    }

    private void bindNextAndPendingClasses() {
        // 1. Calculate GLOBAL Next Class (earliest future unresolved session across ALL subjects)
        currentNextSession = storage.getGlobalNextClass();

        if (currentNextSession != null) {
            cardNextClass.setVisibility(View.VISIBLE);
            tvNextClassBadge.setText("Upcoming");
            tvNextClassBadge.setBackgroundResource(R.drawable.bg_badge_unmarked);
            tvNextClassBadge.setTextColor(ContextCompat.getColor(this, R.color.status_unmarked_text));

            tvNextSubjectName.setText(currentNextSession.getFullSubjectTitle());
            String timeDisplay = (currentNextSession.getDate().isEmpty() ? "" : currentNextSession.getDate() + " • ")
                    + (currentNextSession.getTime().isEmpty() ? "Scheduled Class" : currentNextSession.getTime());
            tvNextClassTime.setText(timeDisplay);

            SubjectAttendance nextSubject = storage.getSubjectByName(currentNextSession.getSubjectName());
            if (nextSubject != null) {
                tvNextCurrentPct.setText(String.format(Locale.US, "Current attendance: %.2f%% (%d/%d)",
                        nextSubject.getPercentage(), nextSubject.getPresentCount(), nextSubject.getScheduledCount()));
            } else {
                tvNextCurrentPct.setText("Attendance update required");
            }

            btnNextAttend.setVisibility(View.VISIBLE);
            btnNextBunk.setVisibility(View.VISIBLE);
            btnNextOd.setVisibility(View.VISIBLE);
        } else {
            cardNextClass.setVisibility(View.VISIBLE);
            tvNextClassBadge.setText("All Resolved");
            tvNextClassBadge.setBackgroundResource(R.drawable.bg_badge_safe);
            tvNextClassBadge.setTextColor(ContextCompat.getColor(this, R.color.status_safe_text));
            tvNextSubjectName.setText("All upcoming classes resolved 🎉");
            tvNextClassTime.setText("No pending future classes left to mark.");
            tvNextCurrentPct.setText("Your overall attendance is up to date!");
            btnNextAttend.setVisibility(View.GONE);
            btnNextBunk.setVisibility(View.GONE);
            btnNextOd.setVisibility(View.GONE);
        }

        // 2. Calculate GLOBAL Pending Classes (passed unresolved sessions across ALL subjects)
        List<AttendanceSession> globalPending = storage.getGlobalPendingSessions();

        if (!globalPending.isEmpty()) {
            currentPendingSession = globalPending.get(0);
            cardPendingAttention.setVisibility(View.VISIBLE);
            tvPendingCountBadge.setText(String.format(Locale.US, "%d pending", globalPending.size()));
            tvPendingSubjectName.setText(currentPendingSession.getFullSubjectTitle());
            String pendingTime = (currentPendingSession.getDate().isEmpty() ? "" : currentPendingSession.getDate() + " • ")
                    + (currentPendingSession.getTime().isEmpty() ? "Scheduled" : currentPendingSession.getTime());
            tvPendingClassTime.setText(pendingTime + " — Attendance not recorded");

            if (cardSubjectsNeedsAttention != null) {
                cardSubjectsNeedsAttention.setVisibility(View.VISIBLE);
                tvSubjectsAttentionDetails.setText(String.format(Locale.US, "%d class%s need attention: %s • %s",
                        globalPending.size(), globalPending.size() == 1 ? "" : "es",
                        currentPendingSession.getFullSubjectTitle(), pendingTime));
            }
        } else {
            cardPendingAttention.setVisibility(View.GONE);
            if (cardSubjectsNeedsAttention != null) {
                cardSubjectsNeedsAttention.setVisibility(View.GONE);
            }
            currentPendingSession = null;
        }
    }

    private void filterAndDisplaySubjects() {
        List<SubjectAttendance> all = storage.getSubjects();
        double target = storage.getTargetPercentage();

        if (currentFilterMode == 0) {
            allSubjectsAdapter.updateData(all);
            return;
        }

        List<SubjectAttendance> filtered = new ArrayList<>();
        for (SubjectAttendance s : all) {
            AttendanceCalculator.StatusCategory cat = AttendanceCalculator.getStatusCategory(s.getPercentage(), target);
            if (currentFilterMode == 1 && cat == AttendanceCalculator.StatusCategory.SAFE) {
                filtered.add(s);
            } else if (currentFilterMode == 2 && cat == AttendanceCalculator.StatusCategory.WARNING) {
                filtered.add(s);
            } else if (currentFilterMode == 3 && cat == AttendanceCalculator.StatusCategory.CRITICAL) {
                filtered.add(s);
            }
        }
        allSubjectsAdapter.updateData(filtered);
    }

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greetingPrefix;
        if (hour < 12) {
            greetingPrefix = "Good morning";
        } else if (hour < 17) {
            greetingPrefix = "Good afternoon";
        } else {
            greetingPrefix = "Good evening";
        }
        String name = storage.getUserName();
        tvGreeting.setText(String.format(Locale.US, "%s, %s 👋", greetingPrefix, name.isEmpty() ? "Student" : name));
    }

    // ============================================================
    // ATTENDANCE IMPORT FLOW
    // ============================================================

    private void performAttendanceImport() {
        Toast.makeText(this, "Scanning DigiCampus attendance page...", Toast.LENGTH_SHORT).show();

        DigiCampusImporter.extractAttendance(webViewDigiCampus, result -> {
            if (result == null) {
                showDialog("Import Notice", "Unable to read page. Please make sure the DigiCampus page has finished loading.");
                return;
            }

            if (result.getType() == DigiCampusExtractionResult.ResultType.MAIN) {
                List<SubjectAttendance> imported = result.getSubjects();
                for (SubjectAttendance s : imported) {
                    storage.updateOrAddSubject(s);
                }
                storage.setLastImportTime(System.currentTimeMillis());
                refreshAllData();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        storage.areNotificationsEnabled() &&
                        !storage.hasRequestedNotificationPermission() &&
                        !NotificationHelper.hasNotificationPermission(this)) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    NotificationHelper.showImportSuccess(this, imported.size());
                    NotificationHelper.checkAllSubjectsAndNotify(this, imported, storage.getTargetPercentage());
                }

                String message = String.format(Locale.US,
                        "Successfully imported %d subjects!\n\nOverall Attendance: %.2f%%\n\nTap 'View Dashboard' to explore predictions.",
                        imported.size(), storage.getOverallAttendance().getOverallPercentage());

                new AlertDialog.Builder(this)
                        .setTitle("Attendance Synced ✓")
                        .setMessage(message)
                        .setPositiveButton("View Dashboard", (dialog, which) -> switchTab(R.id.nav_home))
                        .setNegativeButton("Stay Here", null)
                        .show();

            } else if (result.getType() == DigiCampusExtractionResult.ResultType.INDIVIDUAL) {
                SubjectAttendance single = result.getIndividualSubject();
                storage.updateOrAddSubject(single);
                storage.setLastImportTime(System.currentTimeMillis());
                refreshAllData();

                int marked = single.getMarkedSessions().size();
                int upcoming = single.getUpcomingCount();

                String message = String.format(Locale.US,
                        "Subject: %s\nPresent: %d | Absent: %d\nMarked records: %d\nUpcoming classes: %d\n\nSubject records updated successfully!",
                        single.getFullDisplayTitle(), single.getPresentCount(), single.getAbsentCount(), marked, upcoming);

                new AlertDialog.Builder(this)
                        .setTitle("Subject Synced ✓")
                        .setMessage(message)
                        .setPositiveButton("View Subject", (dialog, which) -> {
                            Intent intent = new Intent(this, SubjectDetailsActivity.class);
                            intent.putExtra(SubjectDetailsActivity.EXTRA_SUBJECT_NAME, single.getSubjectName());
                            startActivity(intent);
                        })
                        .setNegativeButton("OK", null)
                        .show();

            } else if (result.getType() == DigiCampusExtractionResult.ResultType.NOT_ATTENDANCE_PAGE) {
                showDialog("Attendance Page Not Found",
                        "Please log in and open your main Attendance Records page first.\n\nPath in DigiCampus:\nAcademics → Attendance Records");
            } else {
                showDialog("Import Error", result.getMessage());
            }
        });
    }

    private void showDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    // ============================================================
    // SUBJECT CARD ACTIONS
    // ============================================================

    @Override
    public void onViewDetails(SubjectAttendance subject) {
        Intent intent = new Intent(this, SubjectDetailsActivity.class);
        intent.putExtra(SubjectDetailsActivity.EXTRA_SUBJECT_NAME, subject.getSubjectName());
        startActivity(intent);
    }

    @Override
    public void onCalculate(SubjectAttendance subject) {
        Intent intent = new Intent(this, CalculatorActivity.class);
        intent.putExtra(CalculatorActivity.EXTRA_SUBJECT_NAME, subject.getSubjectName());
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (webViewManager != null) {
            webViewManager.saveState(outState);
        }
        outState.putInt(KEY_CURRENT_TAB, activeTabId);
        super.onSaveInstanceState(outState);
    }
}