package com.example.shareplatform.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.shareplatform.R;
import com.example.shareplatform.activity.MainActivity;
import com.example.shareplatform.model.response.LoginResponse;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private EditText etPhone, etPassword;
    private Button btnLogin;
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupObservers();
    }

    private void initViews() {
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "手机号和密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        authViewModel.login(phone, password);
    }

    private void setupObservers() {
        authViewModel.getLoginLiveData().observe(this, new Observer<Resource<LoginResponse>>() {
            @Override
            public void onChanged(Resource<LoginResponse> resource) {
                if (resource instanceof Resource.Loading) {
                    // 显示加载中
                } else if (resource instanceof Resource.Success) {
                    // 登录成功，跳转主页面并清理栈
                    navigateToMainActivity();
                } else if (resource instanceof Resource.Error) {
                    Toast.makeText(LoginActivity.this, ((Resource.Error<LoginResponse>) resource).getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        // 关键：使用FLAG_ACTIVITY_NEW_TASK和FLAG_ACTIVITY_CLEAR_TASK清理栈
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        // 结束当前登录活动
        finish();
    }
}