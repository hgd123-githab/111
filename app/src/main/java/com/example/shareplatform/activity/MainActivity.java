package com.example.shareplatform.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.shareplatform.R;
import com.example.shareplatform.activity.ShareActivity;
import com.example.shareplatform.fragment.HomeFragment;
import com.example.shareplatform.fragment.MySharesFragment;
import com.example.shareplatform.viewmodel.AuthViewModel;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {
    private AuthViewModel authViewModel;
    private MaterialButton btnHome;
    private MaterialButton btnShare;
    private MaterialButton btnMine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 登录验证
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        if (!authViewModel.isLoggedIn()) {
            navigateToLoginActivity();
            return;
        }

        // 初始化视图
        initViews();
        // 初始显示首页 Fragment
        showFragment(new HomeFragment());
        // 设置按钮点击事件
        setupButtonClickEvents();
    }

    private void initViews() {

        btnHome = findViewById(R.id.btn_home);
        btnShare = findViewById(R.id.fab);
        btnMine = findViewById(R.id.btn_mine);
    }

    // 显示指定 Fragment（替换容器中的 Fragment）
    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    private void setupButtonClickEvents() {
        // 首页按钮：显示首页 Fragment
        btnHome.setOnClickListener(v -> showFragment(new HomeFragment()));

        // 分享按钮：跳转到分享页面
        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ShareActivity.class);
            startActivity(intent);
        });

        // 我的按钮：显示“我的” Fragment
        btnMine.setOnClickListener(v -> showFragment(new MySharesFragment()));
    }

    private void navigateToLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


}