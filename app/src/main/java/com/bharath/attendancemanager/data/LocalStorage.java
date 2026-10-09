package com.bharath.attendancemanager.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import com.bharath.attendancemanager.calculator.AttendanceCalculator;
import com.bharath.attendancemanager.model.AttendanceSession;
import com.bharath.attendancemanager.model.AttendanceStatus;
import com.bharath.attendancemanager.model.OverallAttendance;
import com.bharath.attendancemanager.model.SubjectAttendance;
import com.bharath.attendancemanager.notification.ClassReminderScheduler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocalStorage {
    private static final String PREFS_NAME = "attendance_manager_prefs";

    private static final String KEY_HAS_OPENED = "hasOpenedDigiCampus";
    private static final String KEY_LAST_URL = "lastUrl";
    private static final String KEY_TARGET_PERCENTAGE = "targetPercentage";
    private static final String KEY_SUBJECTS_JSON = "subjectsJson";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notificationsEnabled";
    private static final String KEY_HAS_REQUESTED_NOTIF_PERM = "hasRequestedNotifPerm";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_LAST_IMPORT_TIME = "lastImportTime";
    private static final String KEY_NIGHT_MODE = "nightMode";

    public static final String DEFAULT_DIGICAMPUS_URL = "https://rajalakshmi.digiicampus.com/";

    private static LocalStorage instance;
    private final SharedPreferences prefs;
    private final Context context;

    private LocalStorage(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized LocalStorage getInstance(Context context) {
        if (instance == null) {
            instance = new LocalStorage(context);
        }
        return instance;
    }

    public boolean hasOpenedDigiCampus() {
        return prefs.getBoolean(KEY_HAS_OPENED, false);
    }

    public void setHasOpenedDigiCampus(boolean hasOpened) {
        prefs.edit().putBoolean(KEY_HAS_OPENED, hasOpened).apply();
    }

    public String getLastUrl() {
        return prefs.getString(KEY_LAST_URL, DEFAULT_DIGICAMPUS_URL);
    }

    public void setLastUrl(String url) {
        if (url != null && !url.isEmpty() && !url.equals("about:blank")) {
            prefs.edit().putString(KEY_LAST_URL, url).apply();
        }
    }

    public double getTargetPercentage() {
        return (double) prefs.getFloat(KEY_TARGET_PERCENTAGE, (float) AttendanceCalculator.DEFAULT_TARGET_PERCENTAGE);
    }

    public void setTargetPercentage(double target) {
        prefs.edit().putFloat(KEY_TARGET_PERCENTAGE, (float) target).apply();
    }

    public boolean areNotificationsEnabled() {
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply();
    }

    public boolean hasRequestedNotificationPermission() {
        return prefs.getBoolean(KEY_HAS_REQUESTED_NOTIF_PERM, false);
    }

    public void setRequestedNotificationPermission(boolean requested) {
        prefs.edit().putBoolean(KEY_HAS_REQUESTED_NOTIF_PERM, requested).apply();
    }

    public int getNightMode() {
        return prefs.getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setNightMode(int mode) {
        prefs.edit().putInt(KEY_NIGHT_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Bharath");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public long getLastImportTime() {
        return prefs.getLong(KEY_LAST_IMPORT_TIME, 0L);
    }

    public void setLastImportTime(long timestamp) {
        prefs.edit().putLong(KEY_LAST_IMPORT_TIME, timestamp).apply();
    }

    public synchronized List<SubjectAttendance> getSubjects() {
        List<SubjectAttendance> list = new ArrayList<>();
        String jsonStr = prefs.getString(KEY_SUBJECTS_JSON, null);
        if (jsonStr == null || jsonStr.trim().isEmpty()) {
            return list;
        }

        try {
            JSONArray arr = new JSONArray(jsonStr);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    SubjectAttendance s = SubjectAttendance.fromJson(obj);
                    if (s != null) {
                        list.add(s);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized void saveSubjects(List<SubjectAttendance> subjects) {
        if (subjects == null) return;
        JSONArray arr = new JSONArray();
        for (SubjectAttendance s : subjects) {
            arr.put(s.toJson());
        }
        prefs.edit().putString(KEY_SUBJECTS_JSON, arr.toString()).apply();

        // Reschedule class reminder alarms whenever the subject list is updated
        ClassReminderScheduler.scheduleAllReminders(context, subjects);
    }

    public synchronized SubjectAttendance getSubjectByName(String name) {
        if (name == null) return null;
        List<SubjectAttendance> list = getSubjects();
        for (SubjectAttendance s : list) {
            if (s.getSubjectName().equalsIgnoreCase(name.trim())) {
                return s;
            }
        }
        return null;
    }

    /**
     * Marks a session locally for a subject/component.
     */
    public synchronized boolean markSessionLocally(String subjectName, String componentName, String date, String time, AttendanceStatus status) {
        if (subjectName == null) return false;
        List<SubjectAttendance> list = getSubjects();
        boolean updated = false;

        for (int i = 0; i < list.size(); i++) {
            SubjectAttendance s = list.get(i);
            if (s.getSubjectName().equalsIgnoreCase(subjectName.trim())) {
                updated = s.markSessionLocally(componentName, date, time, status);
                if (updated) {
                    list.set(i, s);
                    saveSubjects(list);
                    break;
                }
            }
        }
        return updated;
    }

    public synchronized boolean markSessionLocally(String subjectName, String date, String time, AttendanceStatus status) {
        return markSessionLocally(subjectName, "", date, time, status);
    }

    /**
     * Parses a session date and time into a millisecond timestamp.
     */
    public static long parseSessionDateTime(String dateStr, String timeStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return 0L;
        String combined = dateStr.trim();
        if (timeStr != null && !timeStr.trim().isEmpty()) {
            combined += " " + timeStr.trim();
        }
        String[] formats = {
                "dd MMM yyyy hh:mm a", "dd MMM yyyy HH:mm",
                "dd-MM-yyyy hh:mm a", "dd-MM-yyyy HH:mm",
                "dd/MM/yyyy hh:mm a", "dd/MM/yyyy HH:mm",
                "yyyy-MM-dd HH:mm", "dd MMM yyyy", "dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd"
        };
        for (String fmt : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(fmt, Locale.US);
                sdf.setLenient(false);
                Date parsed = sdf.parse(combined);
                if (parsed != null) return parsed.getTime();
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }

    /**
     * Returns the earliest FUTURE unresolved class across ALL subjects.
     */
    public synchronized AttendanceSession getGlobalNextClass() {
        List<AttendanceSession> allUnresolved = getAllPendingSessions();
        if (allUnresolved.isEmpty()) return null;

        long now = System.currentTimeMillis();
        List<AttendanceSession> futureClasses = new ArrayList<>();

        for (AttendanceSession s : allUnresolved) {
            long classTime = parseSessionDateTime(s.getDate(), s.getTime());
            // Future classes (or no parseable time, treated as upcoming)
            if (classTime == 0L || classTime >= now) {
                futureClasses.add(s);
            }
        }

        if (futureClasses.isEmpty()) return null;

        // Sort chronologically by date/time
        Collections.sort(futureClasses, (s1, s2) -> {
            long t1 = parseSessionDateTime(s1.getDate(), s1.getTime());
            long t2 = parseSessionDateTime(s2.getDate(), s2.getTime());
            if (t1 == 0L) return 1;
            if (t2 == 0L) return -1;
            return Long.compare(t1, t2);
        });

        return futureClasses.get(0);
    }

    /**
     * Returns all PASSED unresolved classes across ALL subjects (PENDING / ATTENDANCE UPDATE REQUIRED).
     */
    public synchronized List<AttendanceSession> getGlobalPendingSessions() {
        List<AttendanceSession> allUnresolved = getAllPendingSessions();
        if (allUnresolved.isEmpty()) return new ArrayList<>();

        long now = System.currentTimeMillis();
        List<AttendanceSession> passedPending = new ArrayList<>();

        for (AttendanceSession s : allUnresolved) {
            long classTime = parseSessionDateTime(s.getDate(), s.getTime());
            // Passed classes that were never marked
            if (classTime > 0L && classTime < now) {
                passedPending.add(s);
            }
        }

        // Sort chronologically (earliest overdue first)
        Collections.sort(passedPending, (s1, s2) -> {
            long t1 = parseSessionDateTime(s1.getDate(), s1.getTime());
            long t2 = parseSessionDateTime(s2.getDate(), s2.getTime());
            return Long.compare(t1, t2);
        });

        return passedPending;
    }

    /**
     * Returns all unresolved (UNMARKED) sessions across all subjects.
     */
    public synchronized List<AttendanceSession> getAllPendingSessions() {
        List<AttendanceSession> pending = new ArrayList<>();
        List<SubjectAttendance> list = getSubjects();
        for (SubjectAttendance s : list) {
            for (AttendanceSession session : s.getPendingSessions()) {
                if (session.getSubjectName() == null || session.getSubjectName().isEmpty()) {
                    session.setSubjectName(s.getSubjectName());
                }
                pending.add(session);
            }
        }
        return pending;
    }

    /**
     * Resets all user local decisions back to UNMARKED.
     */
    public synchronized void resetLocalDecisions() {
        List<SubjectAttendance> list = getSubjects();
        for (SubjectAttendance s : list) {
            resetSubjectSessions(s);
            if (s.hasComponents()) {
                for (SubjectAttendance c : s.getComponents()) {
                    resetSubjectSessions(c);
                }
            }
        }
        saveSubjects(list);
    }

    private void resetSubjectSessions(SubjectAttendance s) {
        List<AttendanceSession> upcoming = s.getUpcomingSessions();
        if (upcoming != null) {
            for (AttendanceSession session : upcoming) {
                session.setStatus(AttendanceStatus.UNMARKED);
                session.setSource(AttendanceSession.SOURCE_DIGICAMPUS);
            }
        }
    }

    /**
     * Updates or adds a subject/component. Supports merging Theory and Practical under parent subject.
     */
    public synchronized void updateOrAddSubject(SubjectAttendance updated) {
        if (updated == null || updated.getSubjectName().trim().isEmpty()) return;

        List<SubjectAttendance> list = getSubjects();
        String parentName = updated.getSubjectName().trim();
        String compName = updated.getComponentName().trim();

        SubjectAttendance parent = null;
        int parentIndex = -1;

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getSubjectName().equalsIgnoreCase(parentName)) {
                parent = list.get(i);
                parentIndex = i;
                break;
            }
        }

        if (parent == null) {
            if (!compName.isEmpty()) {
                // Create parent subject with this component
                parent = new SubjectAttendance(parentName, 0, 0, 0.0);
                parent.addOrUpdateComponent(updated);
            } else {
                parent = updated;
            }
            list.add(parent);
        } else {
            if (!compName.isEmpty()) {
                parent.addOrUpdateComponent(updated);
            } else {
                // Update base DigiCampus counts directly on parent
                if (updated.getDigiCampusPresent() > 0 || updated.getDigiCampusAbsent() > 0) {
                    parent.setDigiCampusPresent(updated.getDigiCampusPresent());
                    parent.setDigiCampusAbsent(updated.getDigiCampusAbsent());
                    parent.setScheduledCount(updated.getScheduledCount());
                    parent.setPercentage(updated.getPercentage());
                }
                if (!updated.getMarkedSessions().isEmpty()) {
                    parent.setMarkedSessions(updated.getMarkedSessions());
                }
                if (!updated.getUpcomingSessions().isEmpty()) {
                    reconcileUpcomingSessions(parent, updated.getUpcomingSessions());
                }
            }
            list.set(parentIndex, parent);
        }

        saveSubjects(list);
    }

    private void reconcileUpcomingSessions(SubjectAttendance existing, List<AttendanceSession> newlyImported) {
        List<AttendanceSession> existingUpcoming = existing.getUpcomingSessions();
        if (existingUpcoming == null || existingUpcoming.isEmpty()) {
            existing.setUpcomingSessions(newlyImported);
            return;
        }

        List<AttendanceSession> merged = new ArrayList<>();
        for (AttendanceSession imported : newlyImported) {
            boolean matched = false;
            for (AttendanceSession ex : existingUpcoming) {
                boolean sameComp = (imported.getComponentName() == null || imported.getComponentName().isEmpty())
                        || imported.getComponentName().equalsIgnoreCase(ex.getComponentName());
                boolean sameDate = (imported.getDate() == null || imported.getDate().isEmpty())
                        || imported.getDate().equalsIgnoreCase(ex.getDate());
                boolean sameTime = (imported.getTime() == null || imported.getTime().isEmpty())
                        || imported.getTime().equalsIgnoreCase(ex.getTime());

                if (sameComp && sameDate && sameTime) {
                    matched = true;
                    // If user previously marked this class locally, preserve user's decision!
                    if (ex.isUserMarked()) {
                        merged.add(ex);
                    } else {
                        merged.add(imported);
                    }
                    break;
                }
            }
            if (!matched) {
                merged.add(imported);
            }
        }
        existing.setUpcomingSessions(merged);
    }

    public OverallAttendance getOverallAttendance() {
        return OverallAttendance.calculate(getSubjects(), getTargetPercentage());
    }

    public void clearSubjectsOnly() {
        prefs.edit().remove(KEY_SUBJECTS_JSON).apply();
    }

    public void clearAllData() {
        prefs.edit()
                .remove(KEY_SUBJECTS_JSON)
                .remove(KEY_HAS_OPENED)
                .remove(KEY_LAST_URL)
                .remove(KEY_LAST_IMPORT_TIME)
                .remove(KEY_NIGHT_MODE)
                .apply();
    }
}