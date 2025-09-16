package com.example.shareplatform.model.response;

public class LoginResponse {
    private String msg;
    private int uid;

    public LoginResponse(String msg, int uid) {
        this.msg = msg;
        this.uid = uid;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

}