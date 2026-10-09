# AttendanceManager 🎓📅

**AttendanceManager** is a comprehensive personal attendance tracker application for Android designed to help students track, manage, and optimize their attendance across subjects and components (Theory & Practical) with seamless integration into college attendance portals like DigiCampus.

---

## 🚀 Key Features

1. **Global Next-Class Tracking:** Scans all subjects and components to identify and highlight the earliest unresolved future class globally.
2. **Theory & Practical Component Architecture:** Separates and aggregates course components under parent subjects, allowing independent attendance tracking for Theory and Practical sessions.
3. **Interactive Notification Actions:** Direct action buttons (`[ ✓ ATTEND ] [ ✕ BUNK ] [ ◉ OD ]`) in class reminder notifications for background attendance updates without opening the app.
4. **Centralized Attendance Target:** Single source of truth for attendance targets (e.g., 75%), dynamically controlling status classifications, bunk advice, filters, and What-If projections.
5. **What-If Calculator:** Simulate future attendance scenarios to determine required consecutive attendance or safe bunk limits.
6. **DigiCampus Portal Integration:** Built-in WebView importer to sync attendance records, subject stats, and session schedules directly from college portals.
7. **Dark / Light / System Mode Persistence:** Seamless appearance preferences supporting Light, Dark, and System Default themes persisted locally.
8. **Robust Local State Management:** Local session resolution (`Attendance Update Required` → `Marked Records`) preventing duplicate records and preserving user decisions.

---

## 🛠️ Tech Stack

* **Language:** Java / Kotlin
* **UI Framework:** Material 3 (Material Components), ConstraintLayout, RecyclerView, NestedScrollView
* **Architecture:** Local-first, Single Source of Truth (`LocalStorage`), BroadcastReceivers for background notifications
* **Build System:** Gradle (Kotlin DSL)

---

## 📦 Building & Running

1. Open the project in **Android Studio**.
2. Run Gradle sync.
3. Build the debug APK (`./gradlew assembleDebug`) or generate a signed release APK (`Build → Generate Signed Bundle / APK`).
