package com.example.shareplatform.network;

import com.example.shareplatform.model.Share;
import com.example.shareplatform.model.request.LoginRequest;
import com.example.shareplatform.model.request.RegisterRequest;
import com.example.shareplatform.model.response.AvatarUploadResponse;
import com.example.shareplatform.model.response.LikeResponse;
import com.example.shareplatform.model.response.LoginResponse;
import com.example.shareplatform.model.response.RegisterResponse;
import com.example.shareplatform.model.response.ShareResponse;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // 原有接口保持不变...
    @POST("user")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @Multipart
    @POST("share")
    Call<ShareResponse> share(
            @Part("uid") RequestBody uid,
            @Part("content") RequestBody content,
            @Part List<MultipartBody.Part> images
    );

    @GET("shares")
    Call<List<Share>> getShares();

    @GET("user/shares")
    Call<List<Share>> getMyShares(@Query("uid") int uid);

    @GET("image/{uid}/{filename}")
    Call<ResponseBody> getImage(@Path("uid") String uid, @Path("filename") String filename);

    @POST("like")
    Call<LikeResponse> likeShare(@Body RequestBody request);

    @POST("unlike")
    Call<LikeResponse> unlikeShare(@Body RequestBody request);

    @GET("like/check")
    Call<ResponseBody> checkLike(@Query("uid") int uid, @Query("sid") int sid);

    @Multipart
    @POST("/user/avatar")
    Call<AvatarUploadResponse> uploadAvatar(
            @Part("uid") RequestBody uid,
            @Part MultipartBody.Part avatar
    );

    // 关键修复：添加Content-Type头
    @DELETE("share/{sid}")
    Call<ResponseBody> deleteShare(
            @Path("sid") int sid,
            @Query("uid") int uid,
            @Header("Content-Type") String contentType  // 新增：指定JSON类型
    );
}
