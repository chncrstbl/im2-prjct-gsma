package com.example.priangradingsystem.model;

public class Teacher {
    private int teacherId;
    private String fullName;
    private String username;
    private String passwordHash;
    private String email;
    private String presetCode;

    public Teacher() {
    }

    public Teacher(int teacherId, String fullName, String username, String passwordHash) {
        this.teacherId = teacherId;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Teacher(int teacherId, String fullName, String username, String passwordHash, String email, String presetCode) {
        this.teacherId = teacherId;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.presetCode = presetCode;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPresetCode() {
        return presetCode;
    }

    public void setPresetCode(String presetCode) {
        this.presetCode = presetCode;
    }
}
