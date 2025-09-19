package com.example.shareplatform.viewmodel;

import android.app.Application;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.example.shareplatform.model.Share;
import com.example.shareplatform.model.response.ShareResponse;
import com.example.shareplatform.network.ApiClient;
import com.example.shareplatform.network.ApiService;
import com.example.shareplatform.util.Resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareViewModel extends AndroidViewModel {
    private MutableLiveData<Resource<ShareResponse>> shareLiveData;
    private MutableLiveData<Resource<List<Share>>> sharesLiveData;
    private MutableLiveData<Resource<List<Share>>> mySharesLiveData;
    private ApiService apiService;
    private boolean isLoadingShares = false; // 避免重复加载

    public ShareViewModel(@NonNull Application application) {
        super(application);
        shareLiveData = new MutableLiveData<>();
        sharesLiveData = new MutableLiveData<>();
        mySharesLiveData = new MutableLiveData<>();
        apiService = ApiClient.getApiService();
    }

    public MutableLiveData<Resource<ShareResponse>> getShareLiveData() {
        return shareLiveData;
    }

    public MutableLiveData<Resource<List<Share>>> getSharesLiveData() {
        return sharesLiveData;
    }

    public MutableLiveData<Resource<List<Share>>> getMySharesLiveData() {
        return mySharesLiveData;
    }

    // 发布分享
    public void share(String content, List<Uri> imageUris) {
        if (shareLiveData.getValue() instanceof Resource.Loading) return;
        shareLiveData.setValue(new Resource.Loading<>(null));

        AuthViewModel authViewModel = new AuthViewModel(getApplication());
        int userId = authViewModel.getUserId();
        if (userId == -1) {
            shareLiveData.setValue(new Resource.Error<>("用户未登录", null));
            return;
        }

        if (content == null || content.trim().isEmpty()) {
            shareLiveData.setValue(new Resource.Error<>("分享内容不能为空", null));
            return;
        }
        if (imageUris == null || imageUris.isEmpty()) {
            shareLiveData.setValue(new Resource.Error<>("请选择至少一张图片", null));
            return;
        }

        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId));
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content.trim());

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : imageUris) {
            try {
                File file = uriToFile(uri);
                if (file.length() > 10 * 1024 * 1024) {
                    shareLiveData.setValue(new Resource.Error<>("图片过大（单个不超过10MB）", null));
                    return;
                }
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                MultipartBody.Part part = MultipartBody.Part.createFormData("images", file.getName(), requestFile);
                imageParts.add(part);
            } catch (IOException e) {
                shareLiveData.setValue(new Resource.Error<>("处理图片失败: " + e.getMessage(), null));
                return;
            }
        }

        apiService.share(userIdBody, contentBody, imageParts).enqueue(new Callback<ShareResponse>() {
            @Override
            public void onResponse(Call<ShareResponse> call, Response<ShareResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    shareLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    String errorMsg = response.message() != null ? response.message() : "未知错误";
                    shareLiveData.setValue(new Resource.Error<>("分享失败: " + errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<ShareResponse> call, Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "网络超时";
                shareLiveData.setValue(new Resource.Error<>("网络错误: " + errorMsg, null));
            }
        });
    }

    // 获取所有分享（首页用）
    public void getShares() {
        if (isLoadingShares) return;
        isLoadingShares = true;
        sharesLiveData.setValue(new Resource.Loading<>(null));

        apiService.getShares().enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                isLoadingShares = false;
                if (response.isSuccessful() && response.body() != null) {
                    sharesLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    sharesLiveData.setValue(new Resource.Error<>("加载失败: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                isLoadingShares = false;
                sharesLiveData.setValue(new Resource.Error<>("网络错误: " + t.getMessage(), null));
            }
        });
    }

    // 获取我的分享
    public void getMyShares(int userId) {
        if (mySharesLiveData.getValue() instanceof Resource.Loading) return;
        mySharesLiveData.setValue(new Resource.Loading<>(null));

        //  getMyShares 接口（路径为 /user/shares）
        apiService.getMyShares(userId).enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mySharesLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    mySharesLiveData.setValue(new Resource.Error<>("加载我的分享失败: " + response.code(), null));
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                mySharesLiveData.setValue(new Resource.Error<>("网络错误: " + t.getMessage(), null));
            }
        });
    }

    // Uri转File（用于图片上传）
    private File uriToFile(Uri uri) throws IOException {
        ContentResolver contentResolver = getApplication().getContentResolver();
        InputStream inputStream = contentResolver.openInputStream(uri);
        if (inputStream == null) throw new IOException("无法打开图片流");

        File cacheDir = getApplication().getExternalCacheDir();
        if (cacheDir == null) cacheDir = getApplication().getCacheDir();
        File file = new File(cacheDir, System.currentTimeMillis() + ".jpg");

        try (OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            inputStream.close();
        }
        return file;
    }
}