package com.example.priangradingsystem.model;

import java.io.Serializable;

@SuppressWarnings("unused")
public class ListItem implements Serializable {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_STUDENT = 1;

    private final int type;
    private final String headerTitle;
    private final Student student;

    public ListItem(String headerTitle) {
        this.type = TYPE_HEADER;
        this.headerTitle = headerTitle;
        this.student = null;
    }

    public ListItem(Student student) {
        this.type = TYPE_STUDENT;
        this.headerTitle = null;
        this.student = student;
    }

    public int getType() {
        return type;
    }

    public boolean isHeader() {
        return type == TYPE_HEADER;
    }

    public String getHeaderTitle() {
        return headerTitle;
    }

    public Student getStudent() {
        return student;
    }
}
