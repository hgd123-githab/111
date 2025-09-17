package com.example.shareplatform.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Share {
    private int sid;
    private int uid;
    private String content;
    private String create_time;
    private List<String> images;
    private  String name;
    // 关键：添加@SerializedName注解，映射后端的like_count字段
    @SerializedName("like_count")
    private int likeCount;

    // 空构造函数（必须保留，Gson解析需要）

    public Share() {}
    // 带参构造函数（可选，按需保留）
    public Share(int sid, int uid, String content, String create_time, List<String> images, int likeCount) {
        this.sid = sid;
        this.uid = uid;
        this.content = content;
        this.create_time = create_time;
        this.images = images;
        this.likeCount = likeCount; // 补充likeCount参数
        this.name = name;
    }




    // 新增点赞数的getter和setter方法
    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}