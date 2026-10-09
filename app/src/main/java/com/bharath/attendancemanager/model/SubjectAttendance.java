package com.bharath.attendancemanager.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubjectAttendance implements Serializable {
    private String subjectName;
    private String componentName; // e.g. "Theory", "Practical", ""
    private int presentCount;
    private int absentCount;
    private int totalSessions;
    private double percentage;

    // Child components if this subject acts as a parent aggregating Theory + Practical
    private List<SubjectAttendance> components;

    // Detailed sessions
    private List<AttendanceSession> markedSessions;
    private List<AttendanceSession> upcomingSessions;

    public SubjectAttendance() {
        this.subjectName = "";
        this.componentName = "";
        this.presentCount = 0;
        this.absentCount = 0;
        this.totalSessions = 0;
        this.percentage = 0.0;
        this.components = new ArrayList<>();
        this.markedSessions = new ArrayList<>();
        this.upcomingSessions = new ArrayList<>();
    }

    public SubjectAttendance(String subjectName, int presentCount, int absentCount, double percentage) {
        this(subjectName, "", presentCount, absentCount, presentCount + absentCount, percentage);
    }

    public SubjectAttendance(String subjectName, String componentName, int presentCount, int absentCount, int totalSessions, double percentage) {
        this.subjectName = subjectName != null ? subjectName : "";
        this.componentName = componentName != null ? componentName : "";
        this.presentCount = Math.max(0, presentCount);
        this.absentCount = Math.max(0, absentCount);
        this.totalSessions = Math.max(totalSessions, this.presentCount + this.absentCount);
        if (this.totalSessions > 0) {
            this.percentage = Math.round(((double) this.presentCount / this.totalSessions) * 10000.0) / 100.0;
        } else {
            this.percentage = Math.max(0.0, Math.min(100.0, percentage));
        }
        this.components = new ArrayList<>();
        this.markedSessions = new ArrayList<>();
        this.upcomingSessions = new ArrayList<>();
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName != null ? subjectName : "";
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName != null ? componentName : "";
    }

    public String getFullDisplayTitle() {
        if (componentName != null && !componentName.trim().isEmpty()) {
            return subjectName + " (" + componentName + ")";
        }
        return subjectName;
    }

    public boolean hasComponents() {
        return components != null && !components.isEmpty();
    }

    public List<SubjectAttendance> getComponents() {
        if (components == null) {
            components = new ArrayList<>();
        }
        return components;
    }

    public void setComponents(List<SubjectAttendance> components) {
        this.components = components != null ? components : new ArrayList<>();
        reconcileComponents();
    }

    public void addComponent(SubjectAttendance component) {
        if (component == null) return;
        if (this.components == null) {
            this.components = new ArrayList<>();
        }
        boolean found = false;
        for (int i = 0; i < this.components.size(); i++) {
            if (this.components.get(i).getComponentName().equalsIgnoreCase(component.getComponentName())) {
                this.components.set(i, component);
                found = true;
                break;
            }
        }
        if (!found) {
            this.components.add(component);
        }
        reconcileComponents();
    }

    public void addOrUpdateComponent(SubjectAttendance component) {
        addComponent(component);
    }

    public int getDigiCampusPresent() {
        return presentCount;
    }

    public void setDigiCampusPresent(int present) {
        this.presentCount = Math.max(0, present);
        reconcileComponents();
    }

    public int getDigiCampusAbsent() {
        return absentCount;
    }

    public void setDigiCampusAbsent(int absent) {
        this.absentCount = Math.max(0, absent);
        reconcileComponents();
    }

    public void setScheduledCount(int total) {
        this.totalSessions = Math.max(0, total);
    }

    public void setPercentage(double pct) {
        this.percentage = Math.max(0.0, Math.min(100.0, pct));
    }

    /**
     * Reconciles parent subject's stats from child components (Theory + Practical).
     */
    public void reconcileComponents() {
        if (hasComponents()) {
            int totalPres = 0;
            int totalAbs = 0;
            int totalSch = 0;
            List<AttendanceSession> mergedMarked = new ArrayList<>();
            List<AttendanceSession> mergedUpcoming = new ArrayList<>();

            for (SubjectAttendance comp : components) {
                totalPres += comp.getPresentCount();
                totalAbs += comp.getAbsentCount();
                totalSch += comp.getScheduledCount();
                if (comp.markedSessions != null) {
                    mergedMarked.addAll(comp.markedSessions);
                }
                if (comp.upcomingSessions != null) {
                    mergedUpcoming.addAll(comp.upcomingSessions);
                }
            }

            this.presentCount = totalPres;
            this.absentCount = totalAbs;
            this.totalSessions = totalSch;
            if (this.totalSessions > 0) {
                this.percentage = Math.round(((double) this.presentCount / this.totalSessions) * 10000.0) / 100.0;
            } else {
                this.percentage = 0.0;
            }
            this.markedSessions = mergedMarked;
            this.upcomingSessions = mergedUpcoming;
        }
    }

    public int getPresentCount() {
        if (hasComponents()) {
            int sum = 0;
            for (SubjectAttendance c : components) {
                sum += c.getPresentCount();
            }
            return sum;
        }
        return presentCount;
    }

    public int getAbsentCount() {
        if (hasComponents()) {
            int sum = 0;
            for (SubjectAttendance c : components) {
                sum += c.getAbsentCount();
            }
            return sum;
        }
        return absentCount;
    }

    public int getScheduledCount() {
        if (hasComponents()) {
            int sum = 0;
            for (SubjectAttendance c : components) {
                sum += c.getScheduledCount();
            }
            return sum;
        }
        return Math.max(totalSessions, presentCount + absentCount);
    }

    public double getPercentage() {
        if (hasComponents()) {
            int p = getPresentCount();
            int t = getScheduledCount();
            if (t > 0) {
                return Math.round(((double) p / t) * 10000.0) / 100.0;
            }
            return 0.0;
        }
        int total = getScheduledCount();
        if (total > 0) {
            return Math.round(((double) getPresentCount() / total) * 10000.0) / 100.0;
        }
        return percentage;
    }

    public List<AttendanceSession> getMarkedSessions() {
        if (hasComponents()) {
            List<AttendanceSession> allMarked = new ArrayList<>();
            for (SubjectAttendance c : components) {
                allMarked.addAll(c.getMarkedSessions());
            }
            return allMarked;
        }
        if (markedSessions == null) {
            markedSessions = new ArrayList<>();
        }
        return markedSessions;
    }

    public void setMarkedSessions(List<AttendanceSession> markedSessions) {
        this.markedSessions = markedSessions != null ? markedSessions : new ArrayList<>();
    }

    public List<AttendanceSession> getUpcomingSessions() {
        if (hasComponents()) {
            List<AttendanceSession> allUpcoming = new ArrayList<>();
            for (SubjectAttendance c : components) {
                allUpcoming.addAll(c.getUpcomingSessions());
            }
            return allUpcoming;
        }
        if (upcomingSessions == null) {
            upcomingSessions = new ArrayList<>();
        }
        return upcomingSessions;
    }

    public void setUpcomingSessions(List<AttendanceSession> upcomingSessions) {
        this.upcomingSessions = upcomingSessions != null ? upcomingSessions : new ArrayList<>();
        if (this.upcomingSessions != null) {
            for (AttendanceSession s : this.upcomingSessions) {
                if (s.getSubjectName() == null || s.getSubjectName().isEmpty()) {
                    s.setSubjectName(this.subjectName);
                }
                if (s.getComponentName() == null || s.getComponentName().isEmpty()) {
                    s.setComponentName(this.componentName);
                }
            }
        }
    }

    public List<AttendanceSession> getPendingSessions() {
        List<AttendanceSession> pending = new ArrayList<>();
        for (AttendanceSession s : getUpcomingSessions()) {
            if (s.getStatus() == AttendanceStatus.UNMARKED) {
                pending.add(s);
            }
        }
        return pending;
    }

    public int getTotalMarkedCount() {
        return getPresentCount() + getAbsentCount();
    }

    public int getUpcomingCount() {
        return getUpcomingSessions().size();
    }

    public int getPendingCount() {
        return getPendingSessions().size();
    }

    public boolean markSessionLocally(String targetComponent, String date, String time, AttendanceStatus newStatus) {
        if (hasComponents()) {
            boolean marked = false;
            for (SubjectAttendance c : components) {
                if (targetComponent == null || targetComponent.isEmpty() || c.getComponentName().equalsIgnoreCase(targetComponent.trim())) {
                    if (c.markSessionLocally(targetComponent, date, time, newStatus)) {
                        marked = true;
                    }
                }
            }
            reconcileComponents();
            return marked;
        }

        if (newStatus == null) return false;

        AttendanceSession targetSession = null;
        if (upcomingSessions != null) {
            for (AttendanceSession s : upcomingSessions) {
                boolean dateMatch = (date == null || date.isEmpty()) || s.getDate().equalsIgnoreCase(date.trim());
                boolean timeMatch = (time == null || time.isEmpty()) || s.getTime().equalsIgnoreCase(time.trim());
                boolean compMatch = (targetComponent == null || targetComponent.isEmpty()) || s.getComponentName().equalsIgnoreCase(targetComponent.trim());
                if (dateMatch && timeMatch && compMatch) {
                    targetSession = s;
                    break;
                }
            }
        }

        if (targetSession != null) {
            upcomingSessions.remove(targetSession);
            AttendanceStatus oldStatus = targetSession.getStatus();
            targetSession.setStatus(newStatus);
            targetSession.setSource(AttendanceSession.SOURCE_USER);

            if (oldStatus == AttendanceStatus.PRESENT) {
                presentCount = Math.max(0, presentCount - 1);
            } else if (oldStatus == AttendanceStatus.ABSENT) {
                absentCount = Math.max(0, absentCount - 1);
            }

            if (newStatus == AttendanceStatus.PRESENT) {
                presentCount++;
            } else if (newStatus == AttendanceStatus.ABSENT) {
                absentCount++;
            }
            totalSessions = Math.max(totalSessions, presentCount + absentCount);

            if (markedSessions == null) {
                markedSessions = new ArrayList<>();
            }
            markedSessions.add(targetSession);
            return true;
        }

        // If not found in upcomingSessions, check markedSessions
        if (markedSessions != null) {
            for (AttendanceSession s : markedSessions) {
                boolean dateMatch = (date == null || date.isEmpty()) || s.getDate().equalsIgnoreCase(date.trim());
                boolean timeMatch = (time == null || time.isEmpty()) || s.getTime().equalsIgnoreCase(time.trim());
                boolean compMatch = (targetComponent == null || targetComponent.isEmpty()) || s.getComponentName().equalsIgnoreCase(targetComponent.trim());
                if (dateMatch && timeMatch && compMatch) {
                    AttendanceStatus oldStatus = s.getStatus();
                    s.setStatus(newStatus);
                    s.setSource(AttendanceSession.SOURCE_USER);

                    if (oldStatus == AttendanceStatus.PRESENT) {
                        presentCount = Math.max(0, presentCount - 1);
                    } else if (oldStatus == AttendanceStatus.ABSENT) {
                        absentCount = Math.max(0, absentCount - 1);
                    }

                    if (newStatus == AttendanceStatus.PRESENT) {
                        presentCount++;
                    } else if (newStatus == AttendanceStatus.ABSENT) {
                        absentCount++;
                    }
                    totalSessions = Math.max(totalSessions, presentCount + absentCount);
                    return true;
                }
            }
        }

        // Otherwise create new marked session
        AttendanceSession newSession = new AttendanceSession(this.subjectName, targetComponent != null ? targetComponent : this.componentName, date, time, newStatus, AttendanceSession.SOURCE_USER, "User Marked");
        if (newStatus == AttendanceStatus.PRESENT) {
            presentCount++;
        } else if (newStatus == AttendanceStatus.ABSENT) {
            absentCount++;
        }
        totalSessions = Math.max(totalSessions, presentCount + absentCount);

        if (markedSessions == null) {
            markedSessions = new ArrayList<>();
        }
        markedSessions.add(newSession);
        return true;
    }

    public String getFormattedPercentage() {
        return String.format(Locale.US, "%.2f%%", getPercentage());
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("subjectName", subjectName);
            obj.put("componentName", componentName);
            obj.put("presentCount", presentCount);
            obj.put("absentCount", absentCount);
            obj.put("totalSessions", totalSessions);
            obj.put("percentage", percentage);

            JSONArray compsArr = new JSONArray();
            if (components != null) {
                for (SubjectAttendance c : components) {
                    compsArr.put(c.toJson());
                }
            }
            obj.put("components", compsArr);

            JSONArray markedArr = new JSONArray();
            if (markedSessions != null) {
                for (AttendanceSession s : markedSessions) {
                    markedArr.put(s.toJson());
                }
            }
            obj.put("markedSessions", markedArr);

            JSONArray upcomingArr = new JSONArray();
            if (upcomingSessions != null) {
                for (AttendanceSession s : upcomingSessions) {
                    upcomingArr.put(s.toJson());
                }
            }
            obj.put("upcomingSessions", upcomingArr);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static SubjectAttendance fromJson(JSONObject obj) {
        if (obj == null) return null;
        String subjectName = obj.optString("subjectName", "");
        String componentName = obj.optString("componentName", "");
        int present = obj.optInt("presentCount", 0);
        int absent = obj.optInt("absentCount", 0);
        int total = obj.optInt("totalSessions", present + absent);
        double pct = obj.optDouble("percentage", 0.0);

        SubjectAttendance sub = new SubjectAttendance(subjectName, componentName, present, absent, total, pct);

        JSONArray compsArr = obj.optJSONArray("components");
        if (compsArr != null) {
            List<SubjectAttendance> compsList = new ArrayList<>();
            for (int i = 0; i < compsArr.length(); i++) {
                SubjectAttendance c = SubjectAttendance.fromJson(compsArr.optJSONObject(i));
                if (c != null) {
                    compsList.add(c);
                }
            }
            sub.setComponents(compsList);
        }

        JSONArray markedArr = obj.optJSONArray("markedSessions");
        if (markedArr != null) {
            List<AttendanceSession> markedList = new ArrayList<>();
            for (int i = 0; i < markedArr.length(); i++) {
                AttendanceSession s = AttendanceSession.fromJson(markedArr.optJSONObject(i));
                if (s != null) {
                    markedList.add(s);
                }
            }
            sub.setMarkedSessions(markedList);
        }

        JSONArray upcomingArr = obj.optJSONArray("upcomingSessions");
        if (upcomingArr != null) {
            List<AttendanceSession> upcomingList = new ArrayList<>();
            for (int i = 0; i < upcomingArr.length(); i++) {
                AttendanceSession s = AttendanceSession.fromJson(upcomingArr.optJSONObject(i));
                if (s != null) {
                    upcomingList.add(s);
                }
            }
            sub.setUpcomingSessions(upcomingList);
        }

        sub.reconcileComponents();
        return sub;
    }
}