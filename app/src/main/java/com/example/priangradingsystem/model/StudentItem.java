package com.example.priangradingsystem.model;

public class StudentItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_STUDENT = 1;

    private final int type;
    private String headerTitle;
    private int headerIconResId = 0;
    private String name;
    private String age;
    private String level;
    private String section = "";
    private String performance = "Good";
    private String photoUri;

    public StudentItem(int type, String headerTitle, int headerIconResId) {
        this.type = type;
        this.headerTitle = headerTitle;
        this.headerIconResId = headerIconResId;
    }

    public StudentItem(int type, String name, String age, String level, String performance) {
        this.type = type;
        this.name = name;
        this.age = age;
        this.level = level;
        this.performance = performance;
    }

    public int getItemViewType() {
        return type;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public int getHeaderIconResId() {
        return headerIconResId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public String getLevel() {
        return level;
    }

    public String getPerformance() {
        return performance;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(String photoUri) {
        this.photoUri = photoUri;
    }
}
