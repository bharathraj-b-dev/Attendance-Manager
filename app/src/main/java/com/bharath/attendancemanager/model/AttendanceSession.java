package com.bharath.attendancemanager.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class AttendanceSession implements Serializable {
    public static final String SOURCE_DIGICAMPUS = "DIGICAMPUS";
    public static final String SOURCE_USER = "USER";

    private String subjectName;
    private String componentName;
    private String date;
    private String time;
    private AttendanceStatus status;
    private String source;
    private String rawText;

    public AttendanceSession() {
        this.subjectName = "";
        this.componentName = "";
        this.date = "";
        this.time = "";
        this.status = AttendanceStatus.UNMARKED;
        this.source = SOURCE_DIGICAMPUS;
        this.rawText = "";
    }

    public AttendanceSession(String date, String time, AttendanceStatus status, String rawText) {
        this("", "", date, time, status, SOURCE_DIGICAMPUS, rawText);
    }

    public AttendanceSession(String subjectName, String date, String time, AttendanceStatus status, String source, String rawText) {
        this(subjectName, "", date, time, status, source, rawText);
    }

    public AttendanceSession(String subjectName, String componentName, String date, String time, AttendanceStatus status, String source, String rawText) {
        this.subjectName = subjectName != null ? subjectName : "";
        this.componentName = componentName != null ? componentName : "";
        this.date = date != null ? date : "";
        this.time = time != null ? time : "";
        this.status = status != null ? status : AttendanceStatus.UNMARKED;
        this.source = source != null ? source : SOURCE_DIGICAMPUS;
        this.rawText = rawText != null ? rawText : "";
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

    public String getFullSubjectTitle() {
        if (componentName != null && !componentName.trim().isEmpty()) {
            return subjectName + " (" + componentName + ")";
        }
        return subjectName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source != null ? source : SOURCE_DIGICAMPUS;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public boolean isMarked() {
        return status != AttendanceStatus.UNMARKED;
    }

    public boolean isResolved() {
        return status != AttendanceStatus.UNMARKED;
    }

    public boolean isUserMarked() {
        return SOURCE_USER.equalsIgnoreCase(source);
    }

    public String getSessionKey() {
        return (subjectName + "|" + componentName + "|" + date + "|" + time).toLowerCase();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("subjectName", subjectName);
            obj.put("componentName", componentName);
            obj.put("date", date);
            obj.put("time", time);
            obj.put("status", status != null ? status.name() : AttendanceStatus.UNMARKED.name());
            obj.put("source", source);
            obj.put("rawText", rawText);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static AttendanceSession fromJson(JSONObject obj) {
        if (obj == null) return null;
        String subjectName = obj.optString("subjectName", "");
        String componentName = obj.optString("componentName", "");
        String date = obj.optString("date", "");
        String time = obj.optString("time", "");
        String statusStr = obj.optString("status", "UNMARKED");
        AttendanceStatus status;
        try {
            status = AttendanceStatus.valueOf(statusStr);
        } catch (Exception e) {
            status = AttendanceStatus.fromRawString(statusStr);
        }
        String source = obj.optString("source", SOURCE_DIGICAMPUS);
        String rawText = obj.optString("rawText", "");
        return new AttendanceSession(subjectName, componentName, date, time, status, source, rawText);
    }
}