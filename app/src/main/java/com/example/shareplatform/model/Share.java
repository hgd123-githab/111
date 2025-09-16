package com.example.shareplatform.model;

import java.util.List;

public class Share {
    private int sid;
    private int uid;
    private String content;
    private String create_time;
    private List<String> images;

    public Share() {

    }

    public Share(int sid, int uid, String content, String create_time, List<String> images) {
        this.sid = sid;
        this.uid = uid;
        this.content = content;
        this.create_time = create_time;
        this.images = images;
    }

    public int getSid() {
        return sid;
    }

    public void setSid(int sid) {
        this.sid = sid;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreate_time() {
        return create_time;
    }

    public void setCreate_time(String create_time) {
        this.create_time = create_time;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }
}