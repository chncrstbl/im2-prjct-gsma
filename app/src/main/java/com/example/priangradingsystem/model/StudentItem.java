package com.example.priangradingsystem.model;

import android.annotation.SuppressLint;

@SuppressLint("DiscouragedApi")
@SuppressWarnings({"unused", "FieldCanBeLocal", "RedundantSuppression"})
public class StudentItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_STUDENT = 1;
    public static final int TYPE_GENDER_HEADER = 2;

    private String name;
    private String age;
    private String level;
    private String section;
    private String gender;
    private String dob;
    private String parentName;
    private String address;
    private String contact;
    private String photoUri;
    private String performance;
    private String headerTitle;
    private int headerIconResId;
    private final int itemViewType;

    private String firstName;
    private String middleName;
    private String lastName;
    private String extension;

    private String parentFirstName;
    private String parentMiddleName;
    private String parentLastName;
    private String parentExtension;

    public StudentItem(int itemViewType, String name, String age, String level, String performance) {
        this.itemViewType = itemViewType;
        this.name = name;
        this.age = age;
        this.level = level;
        this.performance = performance;
    }

    public StudentItem(int itemViewType, String headerTitle, int headerIconResId) {
        this.itemViewType = itemViewType;
        this.headerTitle = headerTitle;
        this.headerIconResId = headerIconResId;
    }

    public int getItemViewType() { return itemViewType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public String getPerformance() { return performance; }
    public String getHeaderTitle() { return headerTitle; }
    public int getHeaderIconResId() { return headerIconResId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }

    public String getParentFirstName() { return parentFirstName; }
    public void setParentFirstName(String parentFirstName) { this.parentFirstName = parentFirstName; }

    public String getParentMiddleName() { return parentMiddleName; }
    public void setParentMiddleName(String parentMiddleName) { this.parentMiddleName = parentMiddleName; }

    public String getParentLastName() { return parentLastName; }
    public void setParentLastName(String parentLastName) { this.parentLastName = parentLastName; }

    public String getParentExtension() { return parentExtension; }
    public void setParentExtension(String parentExtension) { this.parentExtension = parentExtension; }
}