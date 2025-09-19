package com.example.shareplatform.network;

import android.util.Log;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.GzipSource;
import okio.Okio;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
<<<<<<< HEAD
    private static final String BASE_URL = "http://10.34.86.144:5190"; // 关键：设置Flask服务器地址
    private static final int TIMEOUT = 30; // 超时时间（秒）
    private static Retrofit retrofit = null;
    private static OkHttpClient client = null;
=======
    private static final String BASE_URL = "http://10.34.2.227:5190/";
    private static ApiService apiService;
>>>>>>> b4455be20792b16ee6381fb281a2b5bf92670a3b

    public static ApiService getApiService() {
        if (apiService == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    // 1. 修复日志拦截器：读取响应后不破坏原始流
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        // 打印请求日志
                        Log.d("ApiClient", "Request: " + request.method() + " " + request.url());

                        Response response = chain.proceed(request);
                        // 关键：复制响应体，避免原始流被消耗
                        ResponseBody responseBody = response.body();
                        String responseString = "";
                        if (responseBody != null) {
                            // 处理gzip压缩响应（若有）
                            BufferedSource source = responseBody.source();
                            source.request(Long.MAX_VALUE); // 读取全部数据
                            Buffer buffer = source.buffer();
                            if ("gzip".equalsIgnoreCase(response.headers().get("Content-Encoding"))) {
                                try (GzipSource gzippedSource = new GzipSource(buffer.clone())) {
                                    responseString = Okio.buffer(gzippedSource).readString(
                                            java.nio.charset.StandardCharsets.UTF_8
                                    );
                                }
                            } else {
                                responseString = buffer.clone().readString(
                                        java.nio.charset.StandardCharsets.UTF_8
                                );
                            }
                            // 重新构建响应体（核心：否则后续解析无数据）
                            ResponseBody newResponseBody = ResponseBody.create(
                                    responseBody.contentType(),
                                    responseString.getBytes()
                            );
                            // 返回新的响应，保留原始流
                            return response.newBuilder()
                                    .body(newResponseBody)
                                    .build();
                        }
                        return response;
                    })
                    // 2. 添加超时拦截器，避免网络阻塞
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create()) // 确保Gson解析器存在
                    .build();

            apiService = retrofit.create(ApiService.class);
        }
        return apiService;
    }
}