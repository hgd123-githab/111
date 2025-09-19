package com.example.shareplatform.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.shareplatform.R;
import com.example.shareplatform.model.response.AvatarUploadResponse;
import com.example.shareplatform.network.ApiClient;
import com.example.shareplatform.network.ApiService;
import com.example.shareplatform.viewmodel.AuthViewModel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonalInformation extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    // 权限/请求码（与ShareActivity保持一致的风格）
    private static final int PICK_IMAGE_REQUEST = 100;
    private static final int PERMISSION_REQUEST_CODE = 101;
    // 选中的头像Uri
    private Uri selectedAvatarUri;
    // 控件
    private ImageView ivAvatar;
    // 依赖
    private AuthViewModel authViewModel;
    private ApiService apiService;
    private static final String BASE_URL = "http://10.34.2.227:5190/";
    // 统一广播Action
    public static final String ACTION_AVATAR_UPDATED = "com.example.shareplatform.AVATAR_UPDATED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_personal_information);

        // 初始化
        initView();
        initData();
        // 头像点击：触发相册选择
        ivAvatar.setOnClickListener(v -> requestImagePermissionAndPick());
    }

    private void initView() {
        ivAvatar = findViewById(R.id.iv_user_avatar);
        // 初始设置圆形裁剪（匹配头像样式）
        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

        // 检查控件是否初始化成功
        if (ivAvatar == null) {
            Toast.makeText(this, "头像控件未找到，请检查布局文件", Toast.LENGTH_SHORT).show();
        }
    }

    private void initData() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        apiService = ApiClient.getApiService();
        // 加载当前用户头像（首次进入页面时调用）
        loadCurrentAvatar();
    }

    /**
     * 权限请求与相册选择（完全参考ShareActivity的实现）
     */
    private void requestImagePermissionAndPick() {
        // 使用与ShareActivity相同的权限检查逻辑
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{android.Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissions = new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        if (EasyPermissions.hasPermissions(this, permissions)) {
            pickImage();
        } else {
            EasyPermissions.requestPermissions(
                    this,
                    "需要访问存储权限来选择图片",
                    PERMISSION_REQUEST_CODE,
                    permissions
            );
        }
    }

    /**
     * 打开相册（与ShareActivity保持一致）
     */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    /**
     * 处理相册选择结果
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                selectedAvatarUri = imageUri;
                // 本地预览
                Glide.with(this)
                        .load(selectedAvatarUri)
                        .circleCrop()
                        .placeholder(R.drawable.baseline_person_24)
                        .error(R.drawable.baseline_person_24)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(ivAvatar);
                // 上传头像到后端
                uploadAvatarToServer();
            }
        }
    }

    /**
     * 上传头像到后端
     */
    private void uploadAvatarToServer() {
        if (selectedAvatarUri == null) {
            Toast.makeText(this, "请先选择头像图片", Toast.LENGTH_SHORT).show();
            return;
        }

        int userId = authViewModel.getUserId();
        if (userId == -1) {
            Toast.makeText(this, "用户未登录，无法上传头像", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 构建uid请求体
            RequestBody uidBody = RequestBody.create(
                    MediaType.parse("multipart/form-data"),
                    String.valueOf(userId)
            );

            // 构建图片请求体（使用输入流转换方式，兼容所有版本）
            InputStream inputStream = getContentResolver().openInputStream(selectedAvatarUri);
            byte[] imageBytes = inputStreamToByteArray(inputStream);
            String mimeType = getContentResolver().getType(selectedAvatarUri);
            RequestBody avatarBody = RequestBody.create(MediaType.parse(mimeType), imageBytes);

            // 构建Multipart文件部分
            String fileName = "avatar_" + System.currentTimeMillis() + ".jpg";
            MultipartBody.Part avatarPart = MultipartBody.Part.createFormData(
                    "avatar",
                    fileName,
                    avatarBody
            );

            // 发起上传请求
            apiService.uploadAvatar(uidBody, avatarPart).enqueue(new Callback<AvatarUploadResponse>() {
                @Override
                public void onResponse(Call<AvatarUploadResponse> call, Response<AvatarUploadResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        AvatarUploadResponse uploadResponse = response.body();
                        Toast.makeText(PersonalInformation.this, uploadResponse.getMsg(), Toast.LENGTH_SHORT).show();

                        String newAvatarUrl = BASE_URL + uploadResponse.getAvatarUrl().replaceFirst("^/", "");
                        loadAvatarByUrl(newAvatarUrl);

                        // 发送广播更新其他页面
                        Intent intent = new Intent(ACTION_AVATAR_UPDATED);
                        intent.putExtra("avatar_url", newAvatarUrl);
                        LocalBroadcastManager.getInstance(PersonalInformation.this).sendBroadcast(intent);
                    } else {
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                AvatarUploadResponse errorResponse = new com.google.gson.Gson().fromJson(errorJson, AvatarUploadResponse.class);
                                Toast.makeText(PersonalInformation.this, errorResponse.getMsg(), Toast.LENGTH_SHORT).show();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(PersonalInformation.this, "上传失败：" + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<AvatarUploadResponse> call, Throwable t) {
                    Toast.makeText(PersonalInformation.this, "网络错误：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "图片处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 输入流转字节数组
    private byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    /**
     * 加载当前用户头像
     */
    private void loadCurrentAvatar() {
        int userId = authViewModel.getUserId();
        if (userId == -1 || ivAvatar == null) return;

        String avatarUrl = BASE_URL + "user/avatar/" + userId;
        loadAvatarByUrl(avatarUrl);
    }

    /**
     * 统一加载头像方法
     */
    private void loadAvatarByUrl(String avatarUrl) {
        Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivAvatar);
    }

    /**
     * 权限回调（与ShareActivity保持一致）
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            pickImage();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
}