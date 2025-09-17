package com.example.shareplatform.network;

import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "http://10.34.86.144:5190";
    private static final int TIMEOUT = 60;
    private static Retrofit retrofit = null;
    private static OkHttpClient client = null;

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);

            // 修复1：响应校验拦截器 - 读取响应体后必须关闭原始响应流
            Interceptor responseCheckInterceptor = chain -> {
                Request request = chain.request();
                Response originalResponse = chain.proceed(request);
                ResponseBody originalBody = originalResponse.body();

                if (!originalResponse.isSuccessful() || originalBody == null) {
                    originalResponse.close(); // 关键：请求失败时关闭原始响应
                    throw new IOException("请求失败，状态码：" + originalResponse.code());
                }

                // 读取响应体并重新构建新响应（原始响应需关闭）
                String responseContent = originalBody.string();
                originalResponse.close(); // 关键：读取完成后关闭原始响应流

                // 构建新的响应体，避免流被消耗
                ResponseBody newBody = ResponseBody.create(originalBody.contentType(), responseContent);
                return originalResponse.newBuilder()
                        .body(newBody)
                        .build();
            };

            // 修复2：重试拦截器 - 简化逻辑，避免过度重试加剧冲突
            Interceptor retryInterceptor = chain -> {
                Request request = chain.request();
                Response response = null;
                int retryCount = 0;
                while (retryCount < 1) { // 重试次数减少为1次，避免并发堆积
                    try {
                        response = chain.proceed(request);
                        if (response.isSuccessful()) {
                            break;
                        } else {
                            response.close(); // 重试前关闭失败的响应
                        }
                    } catch (IOException e) {
                        retryCount++;
                        if (retryCount >= 1) {
                            throw e;
                        }
                    }
                }
                return response;
            };

            // 构建OkHttpClient：拦截器顺序（重试→响应校验→日志）
            client = new OkHttpClient.Builder()
                    .connectTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT, java.util.concurrent.TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(retryInterceptor)          // 重试拦截器（先）
                    .addInterceptor(responseCheckInterceptor)  // 响应校验（中）
                    .addInterceptor(loggingInterceptor)        // 日志（后）
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}