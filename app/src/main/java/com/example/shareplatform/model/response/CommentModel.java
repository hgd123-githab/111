package com.example.shareplatform.model.response;

public class CommentModel {
    private String username; // 评论者用户名
    private String content;  // 评论内容
    private String time;     // 评论时间

    // 构造方法（用于创建评论对象）
    public CommentModel(String username, String content, String time) {
        this.username = username;
        this.content = content;
        this.time = time;
    }

    // Getter方法（供适配器获取数据）
    public String getUsername() { return username; }
    public String getContent() { return content; }
    public String getTime() { return time; }
}