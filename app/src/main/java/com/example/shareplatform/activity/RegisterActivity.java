package com.example.shareplatform.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shareplatform.R;
import com.example.shareplatform.model.response.RegisterResponse;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {
    private EditText phoneEt, passwordEt, nameEt;
    private Button registerBtn;
    private TextView loginTv;
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;
    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        setupObservers();
        setupClickListeners();
    }

    private void initViews() {
        phoneEt = findViewById(R.id.et_phone);
        passwordEt = findViewById(R.id.et_password);
        nameEt = findViewById(R.id.et_name);
        registerBtn = findViewById(R.id.btn_register);
        loginTv = findViewById(R.id.tv_login);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupObservers() {
        authViewModel.getRegisterLiveData().observe(this, new Observer<Resource<RegisterResponse>>() {
            @Override
            public void onChanged(Resource<RegisterResponse> resource) {
                if (resource instanceof Resource.Loading) {
                    showLoading();
                } else if (resource instanceof Resource.Success) {
                    hideLoading();
                    Toast.makeText(RegisterActivity.this, "注册成功", Toast.LENGTH_SHORT).show();
                    navigateToLoginActivity();
                } else if (resource instanceof Resource.Error) {
                    hideLoading();
                    Toast.makeText(RegisterActivity.this, ((Resource.Error<RegisterResponse>) resource).getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupClickListeners() {
        registerBtn.setOnClickListener(v -> {
            String phone = phoneEt.getText().toString().trim();
            String password = passwordEt.getText().toString().trim();
            String name = nameEt.getText().toString().trim();

            if (TextUtils.isEmpty(phone) || TextUtils.isEmpty(password) || TextUtils.isEmpty(name)) {
                Toast.makeText(this, "所有字段均为必填", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!phone.matches(PHONE_PATTERN)) {
                Toast.makeText(this, "请输入正确的手机号格式", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(phone, password, name);
        });

        loginTv.setOnClickListener(v -> navigateToLoginActivity());
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        registerBtn.setEnabled(false);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        registerBtn.setEnabled(true);
    }
}