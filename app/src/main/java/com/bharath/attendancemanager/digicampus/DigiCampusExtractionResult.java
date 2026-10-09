package com.bharath.attendancemanager.digicampus;

import com.bharath.attendancemanager.model.SubjectAttendance;

import java.util.ArrayList;
import java.util.List;

public class DigiCampusExtractionResult {
    public enum ResultType {
        MAIN,
        INDIVIDUAL,
        NOT_ATTENDANCE_PAGE,
        ERROR
    }

    private final ResultType type;
    private final List<SubjectAttendance> subjects;
    private final SubjectAttendance individualSubject;
    private final List<String> errors;
    private final String message;

    private DigiCampusExtractionResult(ResultType type, List<SubjectAttendance> subjects,
                                      SubjectAttendance individualSubject, List<String> errors, String message) {
        this.type = type;
        this.subjects = subjects != null ? subjects : new ArrayList<>();
        this.individualSubject = individualSubject;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.message = message != null ? message : "";
    }

    public static DigiCampusExtractionResult forMain(List<SubjectAttendance> subjects, List<String> errors) {
        return new DigiCampusExtractionResult(ResultType.MAIN, subjects, null, errors, "Main attendance page imported successfully.");
    }

    public static DigiCampusExtractionResult forIndividual(SubjectAttendance subject) {
        return new DigiCampusExtractionResult(ResultType.INDIVIDUAL, null, subject, null, "Individual subject attendance imported successfully.");
    }

    public static DigiCampusExtractionResult forNotAttendancePage(String message) {
        return new DigiCampusExtractionResult(ResultType.NOT_ATTENDANCE_PAGE, null, null, null, message);
    }

    public static DigiCampusExtractionResult forError(String errorMessage) {
        return new DigiCampusExtractionResult(ResultType.ERROR, null, null, null, errorMessage);
    }

    public ResultType getType() {
        return type;
    }

    public List<SubjectAttendance> getSubjects() {
        return subjects;
    }

    public SubjectAttendance getIndividualSubject() {
        return individualSubject;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return type == ResultType.MAIN || type == ResultType.INDIVIDUAL;
    }
}
