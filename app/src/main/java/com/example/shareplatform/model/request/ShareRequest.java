package com.example.shareplatform.model.request;

public class ShareRequest {
    private String uid;
    private String content;

    public ShareRequest(String uid, String content) {
        this.uid = uid;
        this.content = content;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}