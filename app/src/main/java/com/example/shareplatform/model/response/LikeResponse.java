package com.example.shareplatform.model.response;

// 点赞/取消点赞的响应模型（对应后端返回的JSON结构）
public class LikeResponse {
    private int like_count; // 后端返回的点赞数字段（下划线命名）
    private String msg;      // 后端返回的提示信息

    // 空构造函数（Gson解析必须）
    public LikeResponse() {}

    // Getter方法（关键：用于获取解析后的字段值）
    public int getLike_count() {
        return like_count;
    }

    public String getMsg() {
        return msg;
    }

    // Setter方法（可选，Gson解析可无，但建议保留）
    public void setLike_count(int like_count) {
        this.like_count = like_count;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}