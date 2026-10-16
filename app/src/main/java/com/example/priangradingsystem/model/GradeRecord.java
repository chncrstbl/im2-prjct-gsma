package com.example.priangradingsystem.model;

import java.io.Serializable;

@SuppressWarnings("unused")
public class GradeRecord implements Serializable {

    private String studentId;
    private String subject;
    private double grade;
    private String period;

    public GradeRecord() {
    }

    public GradeRecord(String studentId, String subject, double grade, String period) {
        this.studentId = studentId;
        this.subject = subject;
        this.grade = grade;
        this.period = period;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public double getGrade() {
        return grade;
    }

    public void setGrade(double grade) {
        this.grade = grade;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }
}
