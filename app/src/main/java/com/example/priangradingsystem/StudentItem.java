package com.example.priangradingsystem;

@SuppressWarnings("unused")
public class StudentItem {
    public static final int TYPE_HEADER = 0;
    public static final int TYPE_STUDENT = 1;
    public static final int TYPE_GENDER_HEADER = 2;

    public int itemViewType;
    public String headerTitle;
    public int headerIconResId;

    public String name;
    public String age;
    public String level;
    public String section;
    public String performance;
    public String gender;
    public String dob;
    public String parentName;
    public String address;
    public String contact;
    public String photoUri;

    public StudentItem() {
        this.itemViewType = TYPE_STUDENT;
    }

    public StudentItem(int itemViewType, String headerTitle, int headerIconResId) {
        this.itemViewType = itemViewType;
        this.headerTitle = headerTitle;
        this.headerIconResId = headerIconResId;
    }

    public StudentItem(int itemViewType, String name, String age, String level, String performance) {
        this.itemViewType = itemViewType;
        this.name = name;
        this.age = age;
        this.level = level;
        this.performance = performance;
    }

    public int getItemViewType() { return itemViewType; }
    public String getHeaderTitle() { return headerTitle; }
    public int getHeaderIconResId() { return headerIconResId; }
    public String getName() { return name; }
    public String getAge() { return age; }
    public String getLevel() { return level; }
    public String getSection() { return section; }
    public String getPerformance() { return performance; }
    public String getGender() { return gender; }
    public String getDob() { return dob; }
    public String getParentName() { return parentName; }
    public String getAddress() { return address; }
    public String getContact() { return contact; }
    public String getPhotoUri() { return photoUri; }

    public void setName(String name) { this.name = name; }
    public void setAge(String age) { this.age = age; }
    public void setLevel(String level) { this.level = level; }
    public void setSection(String section) { this.section = section; }
    public void setPerformance(String performance) { this.performance = performance; }
    public void setGender(String gender) { this.gender = gender; }
    public void setDob(String dob) { this.dob = dob; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public void setAddress(String address) { this.address = address; }
    public void setContact(String contact) { this.contact = contact; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
}