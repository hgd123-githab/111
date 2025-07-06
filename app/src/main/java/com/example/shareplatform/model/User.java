package com.example.shareplatform.model;

public class User {
    private int uid;
    private String pho;
    private String name;

    public User(int uid, String pho, String name) {
        this.uid = uid;
        this.pho = pho;
        this.name = name;
    }

    // Getters and Setters
    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }
    public String getPho() { return pho; }
    public void setPho(String pho) { this.pho = pho; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}