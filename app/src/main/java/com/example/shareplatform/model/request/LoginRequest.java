package com.example.shareplatform.model.request;

public class LoginRequest {
    private String pho;
    private String pwd;

    public LoginRequest(String pho, String pwd) {
        this.pho = pho;
        this.pwd = pwd;
    }

    // Getters and Setters
    public String getPho() { return pho; }
    public void setPho(String pho) { this.pho = pho; }

    public String getPwd() { return pwd; }
    public void setPwd(String pwd) { this.pwd = pwd; }
}