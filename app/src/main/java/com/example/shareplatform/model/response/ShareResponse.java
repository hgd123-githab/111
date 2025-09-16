package com.example.shareplatform.model.response;

import java.util.List;

public class ShareResponse {
    private String msg;
    private int share_id;
    private List<String> image_urls;

    public ShareResponse(String msg, int share_id, List<String> image_urls) {
        this.msg = msg;
        this.share_id = share_id;
        this.image_urls = image_urls;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }


    public int getShare_id() {
        return share_id;
    }

    public void setShare_id(int share_id) {
        this.share_id = share_id;
    }

    public List<String> getImage_urls() {
        return image_urls;
    }

    public void setImage_urls(List<String> image_urls) {
        this.image_urls = image_urls;
    }
}