package com.example.shareplatform.model.request;

public class RegisterRequest {
    private String pho;
    private String pwd;
    private String name;

    public RegisterRequest(String pho, String pwd, String name) {
        this.pho = pho;
        this.pwd = pwd;
        this.name = name;
    }

    public String getPho() {
        return pho;
    }

    public void setPho(String pho) {
        this.pho = pho;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}