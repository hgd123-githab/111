package com.example.shareplatform.model.request;

import java.util.List;

public class PersonshowRequest {
    private String username;       // 用户名（对应 tv_username）
    private String content;        // 内容（对应 tv_content）
    private String time;           // 时间（对应 tv_time）
    private int likeCount;         // 点赞数（对应 tv_like_count）
    private boolean isLiked;       // 是否已点赞（控制 iv_like 图标切换）
    private List<String> imageUrls;// 图片列表（对应 image_recycler_view）
    private int avatarResId;       // 用户头像（对应 iv_user_avatar，也可用网络地址）

    // 构造方法
    public PersonshowRequest(String username, String content, String time, int likeCount, boolean isLiked, List<String> imageUrls, int avatarResId) {
        this.username = username;
        this.content = content;
        this.time = time;
        this.likeCount = likeCount;
        this.isLiked = isLiked;
        this.imageUrls = imageUrls;
        this.avatarResId = avatarResId;
    }

    // Getter 和 Setter（必须，用于 RecyclerView 适配器获取数据）
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public int getAvatarResId() { return avatarResId; }
    public void setAvatarResId(int avatarResId) { this.avatarResId = avatarResId; }
}
