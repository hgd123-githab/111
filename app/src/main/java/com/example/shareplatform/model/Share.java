package com.example.shareplatform.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Share {
    private int sid;
    private int uid;
    private String content;
    private String create_time;
    private List<String> images;
    private String name;

<<<<<<< HEAD
=======
    // 关键修改：使用@SerializedName匹配后端返回的user_avatar字段
    @SerializedName("user_avatar")
    private String avatarUrl;

    @SerializedName("like_count")
    private int likeCount;
>>>>>>> b4455be20792b16ee6381fb281a2b5bf92670a3b

    // 空构造函数（必须保留，Gson解析需要）
    public Share() {}

    // 带参构造函数
    public Share(int sid, int uid, String content, String create_time, List<String> images,
                 int likeCount, String name, String avatarUrl) {
        this.sid = sid;
        this.uid = uid;
        this.content = content;
        this.create_time = create_time;
        this.images = images;
<<<<<<< HEAD
        this.name = name;
=======
        this.likeCount = likeCount;
        this.name = name;
        this.avatarUrl = avatarUrl;
>>>>>>> b4455be20792b16ee6381fb281a2b5bf92670a3b
    }

    // 头像URL的getter和setter方法
    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    // 其他原有getter和setter方法（保持不变）
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
<<<<<<< HEAD
=======

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }
>>>>>>> b4455be20792b16ee6381fb281a2b5bf92670a3b
}