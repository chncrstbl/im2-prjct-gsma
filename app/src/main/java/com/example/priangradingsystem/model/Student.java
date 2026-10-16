package com.example.priangradingsystem.model;

import android.net.Uri;
import java.io.Serializable;

@SuppressWarnings("unused")
public class Student implements Serializable {
    private final String name;
    private final String gradeClass;
    private final String age;
    private final String dob;
    private final Uri photoUri;
    private String gradeLevel = "";
    private String section = "";
    private String performance = "";

    public Student(String name, String gradeClass, String age, String dob, Uri photoUri) {
        this.name = name;
        this.gradeClass = gradeClass;
        this.age = age;
        this.dob = dob;
        this.photoUri = photoUri;

        if (gradeClass != null && gradeClass.contains(" - ")) {
            String[] parts = gradeClass.split(" - ");
            this.gradeLevel = parts[0];
            this.section = parts[1];
        } else if (gradeClass != null) {
            this.gradeLevel = gradeClass;
        }
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return name;
    }

    public String getGradeClass() {
        return gradeClass;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public String getSection() {
        return section;
    }

    public String getPerformance() {
        return performance;
    }

    public void setPerformance(String performance) {
        this.performance = performance;
    }

    public String getAge() {
        return age;
    }

    public int getAgeInt() {
        try {
            return Integer.parseInt(age);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getDob() {
        return dob;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }
}
