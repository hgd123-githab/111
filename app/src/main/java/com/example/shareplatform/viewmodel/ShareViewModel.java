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
    // 新增：避免重复加载分享列表的标记
    private boolean isLoadingShares = false;

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

    // 1. 分享功能：优化图片处理异常，避免崩溃
    public void share(String content, List<Uri> imageUris) {
        if (shareLiveData.getValue() instanceof Resource.Loading) {
            return; // 正在分享中，忽略后续请求
        }
        shareLiveData.setValue(new Resource.Loading<>(null));

        AuthViewModel authViewModel = new AuthViewModel(getApplication());
        int userId = authViewModel.getUserId();
        if (userId == -1) {
            shareLiveData.setValue(new Resource.Error<>("用户未登录", null));
            return;
        }

        // 校验参数：避免空内容或空图片列表
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
                // 校验文件大小：避免过大文件导致上传失败
                if (file.length() > 10 * 1024 * 1024) { // 限制10MB以内
                    shareLiveData.setValue(new Resource.Error<>("图片过大（单个图片不超过10MB）", null));
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
                String errorMsg = t.getMessage() != null ? t.getMessage() : "网络连接超时";
                shareLiveData.setValue(new Resource.Error<>("网络错误: " + errorMsg, null));
            }
        });
    }

    // 2. 获取分享列表：新增防抖标记，避免重复加载
    public void getShares() {
        if (isLoadingShares) {
            return; // 正在加载中，忽略后续请求
        }
        isLoadingShares = true;
        sharesLiveData.setValue(new Resource.Loading<>(null));

        apiService.getShares().enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                isLoadingShares = false; // 加载结束，重置标记
                if (response.isSuccessful() && response.body() != null) {
                    sharesLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    String errorMsg = response.message() != null ? response.message() : "未知错误";
                    sharesLiveData.setValue(new Resource.Error<>("获取分享列表失败: " + errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                isLoadingShares = false; // 加载失败，重置标记
                String errorMsg = t.getMessage() != null ? t.getMessage() : "网络连接超时";
                sharesLiveData.setValue(new Resource.Error<>("网络错误: " + errorMsg, null));
            }
        });
    }

    // 3. 获取我的分享：同列表逻辑，添加防抖
    public void getMyShares(int userId) {
        if (mySharesLiveData.getValue() instanceof Resource.Loading) {
            return;
        }
        mySharesLiveData.setValue(new Resource.Loading<>(null));

        apiService.getMyShares(userId).enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mySharesLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    String errorMsg = response.message() != null ? response.message() : "未知错误";
                    mySharesLiveData.setValue(new Resource.Error<>("获取我的分享失败: " + errorMsg, null));
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                String errorMsg = t.getMessage() != null ? t.getMessage() : "网络连接超时";
                mySharesLiveData.setValue(new Resource.Error<>("网络错误: " + errorMsg, null));
            }
        });
    }

    // 4. URI转File：优化异常处理，避免文件操作崩溃
    private File uriToFile(Uri uri) throws IOException {
        ContentResolver contentResolver = getApplication().getContentResolver();
        // 优化：用应用私有目录存储临时文件，避免外部存储权限问题
        File tempDir = new File(getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs(); // 确保目录存在
        }
        File file = new File(tempDir, "img_" + System.currentTimeMillis() + ".jpg");

        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {

            if (inputStream == null) {
                throw new IOException("无法打开图片流，请检查图片权限");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            // 强制刷新流，确保文件写入完成
            outputStream.flush();
        }
        return file;
    }

    // 废弃：getFilePathFromUri 方法（改用 uriToFile，避免ContentProvider查询异常）
    @Deprecated
    private String getFilePathFromUri(Uri uri) {
        return null;
    }

}