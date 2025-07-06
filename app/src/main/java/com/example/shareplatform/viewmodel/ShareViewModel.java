package com.example.shareplatform.viewmodel;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
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

    public void share(String content, List<Uri> imageUris) {
        shareLiveData.setValue(new Resource.Loading<>(null));

        AuthViewModel authViewModel = new AuthViewModel(getApplication());
        int userId = authViewModel.getUserId();
        if (userId == -1) {
            shareLiveData.setValue(new Resource.Error<>("用户未登录", null));
            return;
        }

        RequestBody userIdBody = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(userId));
        RequestBody contentBody = RequestBody.create(MediaType.parse("text/plain"), content);

        List<MultipartBody.Part> imageParts = new ArrayList<>();
        for (Uri uri : imageUris) {
            try {
                File file = uriToFile(uri);
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
                    shareLiveData.setValue(new Resource.Error<>("分享失败: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<ShareResponse> call, Throwable t) {
                shareLiveData.setValue(new Resource.Error<>("网络错误: " + t.getMessage(), null));
            }
        });
    }

    public void getShares() {
        sharesLiveData.setValue(new Resource.Loading<>(null));
        apiService.getShares().enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sharesLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    sharesLiveData.setValue(new Resource.Error<>("获取分享列表失败: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                sharesLiveData.setValue(new Resource.Error<>("网络错误: " + t.getMessage(), null));
            }
        });
    }

    public void getMyShares(int userId) {
        mySharesLiveData.setValue(new Resource.Loading<>(null));
        apiService.getMyShares(userId).enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mySharesLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    mySharesLiveData.setValue(new Resource.Error<>("获取我的分享失败: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                mySharesLiveData.setValue(new Resource.Error<>("网络错误: " + t.getMessage(), null));
            }
        });
    }

    private File uriToFile(Uri uri) throws IOException {
        ContentResolver contentResolver = getApplication().getContentResolver();
        File file = new File(getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "temp_" + System.currentTimeMillis() + ".jpg");

        try (InputStream inputStream = contentResolver.openInputStream(uri);
             OutputStream outputStream = new FileOutputStream(file)) {

            if (inputStream == null) {
                throw new IOException("无法打开输入流");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return file;
    }

    private String getFilePathFromUri(Uri uri) {
        String filePath = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getApplication().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    if (columnIndex != -1) {
                        filePath = cursor.getString(columnIndex);
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if (uri.getScheme().equals("file")) {
            filePath = uri.getPath();
        }
        return filePath;
    }
}