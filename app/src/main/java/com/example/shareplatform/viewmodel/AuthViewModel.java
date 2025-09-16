package com.example.shareplatform.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.shareplatform.model.response.LoginResponse;
import com.example.shareplatform.model.response.RegisterResponse;
import com.example.shareplatform.model.request.LoginRequest;
import com.example.shareplatform.model.request.RegisterRequest;
import com.example.shareplatform.network.ApiClient;
import com.example.shareplatform.network.ApiService;
import com.example.shareplatform.util.Resource;

import okhttp3.FormBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {
    private ApiService apiService;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_UID = "user_id";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private MutableLiveData<Resource<LoginResponse>> loginLiveData;
    private MutableLiveData<Resource<RegisterResponse>> registerLiveData;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        apiService = ApiClient.getApiService();
        sharedPreferences = application.getSharedPreferences(PREF_NAME, Application.MODE_PRIVATE);
        loginLiveData = new MutableLiveData<>();
        registerLiveData = new MutableLiveData<>();
    }


    // 登录相关
    public LiveData<Resource<LoginResponse>> getLoginLiveData() {
        return loginLiveData;
    }

    public void login(String phone, String password) {
        loginLiveData.setValue(new Resource.Loading<>(null));
        LoginRequest loginRequest = new LoginRequest(phone, password);
        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveUserId(response.body().getUid());
                    saveLoginStatus(true);
                    loginLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    loginLiveData.setValue(new Resource.Error<>("登录失败: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                loginLiveData.setValue(new Resource.Error<>("网络错误: " + t.getMessage(), null));
            }
        });
    }

    // 注册相关
    public LiveData<Resource<RegisterResponse>> getRegisterLiveData() {
        return registerLiveData;
    }

    public void register(String phone, String password, String name) {
        registerLiveData.setValue(new Resource.Loading<>(null));
        RegisterRequest registerRequest = new RegisterRequest(phone, password, name);
        apiService.register(registerRequest).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    saveUserId(response.body().getData().getUser_id());
                    saveLoginStatus(true);
                    registerLiveData.setValue(new Resource.Success<>(response.body()));
                } else {
                    registerLiveData.setValue(new Resource.Error<>("注册失败: " + response.message(), null));
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                registerLiveData.setValue(new Resource.Error<>("网络错误: " + t.getMessage(), null));
            }
        });
    }

    // 退出登录
    public void logout() {
        saveLoginStatus(false);
        sharedPreferences.edit().remove(KEY_UID).apply();
    }

    public int getUserId() {
        return sharedPreferences.getInt(KEY_UID, -1);
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    private void saveUserId(int userId) {
        sharedPreferences.edit().putInt(KEY_UID, userId).apply();
    }

    private void saveLoginStatus(boolean isLoggedIn) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }
}