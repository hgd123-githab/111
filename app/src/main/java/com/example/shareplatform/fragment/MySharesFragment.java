package com.example.shareplatform.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.shareplatform.R;
import com.example.shareplatform.activity.LoginActivity;
import com.example.shareplatform.activity.PersonalInformation;
import com.example.shareplatform.activity.Personshow;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.AuthViewModel;

public class MySharesFragment extends Fragment {
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;
    private Button logoutBtn;
    private LinearLayout myInfoLayout;
    private LinearLayout myShareLayout; // 新增：声明"我的分享"控件

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_shares, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupObservers();
        setupMyInfoClick();
        setupMyShareClick();
    }


    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        logoutBtn = view.findViewById(R.id.btn_logout);
        myInfoLayout = view.findViewById(R.id.ll_my_info);
        myShareLayout = view.findViewById(R.id.ll_my_share);

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupObservers() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
    }


    private void setupMyInfoClick() {
        myInfoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PersonalInformation.class);
            startActivity(intent);
        });
    }

    // 新增："我的分享"点击跳转逻辑（跳转到PersonShowActivity）
    private void setupMyShareClick() {
        myShareLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), Personshow.class);
            startActivity(intent);
        });
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("退出确认")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("退出", (dialog, which) -> logout())
                .setNegativeButton("取消", null)
                .show();
    }

    private void logout() {
        authViewModel.logout();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}