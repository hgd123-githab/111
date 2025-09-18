package com.example.shareplatform.model.request;

// 基础响应模型（根据后端实际返回调整）
public class BaseResponse {
    private boolean success; // 后端返回的成功标记（如：true/false）
    private String msg;      // 提示信息（如："上传成功"）

    // Getter & Setter
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
}