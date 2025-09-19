package com.example.shareplatform.model.response;

import com.google.gson.annotations.SerializedName;

public class AvatarUploadResponse {
    private String msg;

    @SerializedName("avatar_url") // 匹配后端返回的字段名
    private String avatarUrl;

    // Getters and Setters
    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}