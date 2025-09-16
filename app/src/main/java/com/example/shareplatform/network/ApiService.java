package com.example.shareplatform.network;

import com.example.shareplatform.model.Share;
import com.example.shareplatform.model.request.LoginRequest;
import com.example.shareplatform.model.request.RegisterRequest;
import com.example.shareplatform.model.response.LoginResponse;
import com.example.shareplatform.model.response.RegisterResponse;
import com.example.shareplatform.model.response.ShareResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // 用户登录接口
    @POST("user")
    Call<LoginResponse> login(@Body LoginRequest request);

    // 用户注册接口
    @POST("register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    // 发布分享接口
    @Multipart
    @POST("share")
    Call<ShareResponse> share(
            @Part("uid") RequestBody uid,
            @Part("content") RequestBody content,
            @Part List<MultipartBody.Part> images
    );


    // 获取所有分享接口

    @GET("shares")
    Call<List<Share>> getShares();

    // 获取我的分享接口
    @GET("shares")
    Call<List<Share>> getMyShares(@Query("uid") int uid);

    // 访问图片接口（新增）
    @GET("image/{uid}/{filename}")
    Call<ResponseBody> getImage(@Path("uid") String uid, @Path("filename") String filename);
}