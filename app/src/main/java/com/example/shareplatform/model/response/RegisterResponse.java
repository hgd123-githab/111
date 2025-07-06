package com.example.shareplatform.model.response;

public class RegisterResponse {
    private String msg;
    private RegisterData data;

    public RegisterResponse(String msg, RegisterData data) {
        this.msg = msg;
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public RegisterData getData() {
        return data;
    }

    public void setData(RegisterData data) {
        this.data = data;
    }

    // 将内部类改为public访问权限
    public static class RegisterData {
        private int user_id;
        private String phone;
        private String username;

        public RegisterData(int user_id, String phone, String username) {
            this.user_id = user_id;
            this.phone = phone;
            this.username = username;
        }

        // 确保getter方法为public
        public int getUser_id() {
            return user_id;
        }

        public void setUser_id(int user_id) {
            this.user_id = user_id;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}