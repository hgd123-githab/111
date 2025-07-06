package com.example.shareplatform.util;

public abstract class Resource<T> {
    public static class Loading<T> extends Resource<T> {
        private T data;

        public Loading(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }
    }

    public static class Success<T> extends Resource<T> {
        private T data;

        public Success(T data) {
            this.data = data;
        }

        public T getData() {
            return data;
        }
    }

    public static class Error<T> extends Resource<T> {
        private String message;
        private T data;

        public Error(String message, T data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() {
            return message;
        }

        public T getData() {
            return data;
        }
    }
}