package com.example.shareplatform.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.shareplatform.R;
import com.example.shareplatform.activity.LoginActivity;
import com.example.shareplatform.activity.PersonalInformation;
import com.example.shareplatform.activity.Personshow;
import com.example.shareplatform.viewmodel.AuthViewModel;

public class MySharesFragment extends Fragment {
    private ProgressBar progressBar;
    private AuthViewModel authViewModel;
    private Button logoutBtn;
    private LinearLayout myInfoLayout;
    private LinearLayout myShareLayout;
    private ImageView ivMyPageAvatar;
    private static final String BASE_URL = "http://10.34.2.227:5190/";
    private int currentUserId;

    // 广播接收器（统一Action）
    private BroadcastReceiver avatarUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PersonalInformation.ACTION_AVATAR_UPDATED)) {
                String newAvatarUrl = intent.getStringExtra("avatar_url");
                if (newAvatarUrl != null && ivMyPageAvatar != null) {
                    Glide.with(requireContext())
                            .load(newAvatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.baseline_person_24)
                            .error(R.drawable.baseline_person_24)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(ivMyPageAvatar);
                }
            }
        }
    };

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

        // 注册广播
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(avatarUpdatedReceiver,
                        new IntentFilter(PersonalInformation.ACTION_AVATAR_UPDATED));

        // 首次加载头像
        loadMyPageAvatar();
    }

    private void initViews(View view) {
        progressBar = view.findViewById(R.id.progress_bar);
        logoutBtn = view.findViewById(R.id.btn_logout);
        myInfoLayout = view.findViewById(R.id.ll_my_info);
        myShareLayout = view.findViewById(R.id.ll_my_share);
        ivMyPageAvatar = view.findViewById(R.id.iv_my_avatar); // 确保布局ID匹配

        logoutBtn.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupObservers() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        currentUserId = authViewModel.getUserId();
    }

    private void setupMyInfoClick() {
        myInfoLayout.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PersonalInformation.class);
            startActivity(intent);
        });
    }

    private void setupMyShareClick() {
        myShareLayout.setOnClickListener(v -> {
            int userId = authViewModel.getUserId();
            if (userId == -1) {
                Toast.makeText(requireContext(), "用户ID获取失败", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(requireContext(), Personshow.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });
    }

    private void loadMyPageAvatar() {
        if (currentUserId == -1 || ivMyPageAvatar == null) return;

        String avatarUrl = BASE_URL + "user/avatar/" + currentUserId;
        Glide.with(requireContext())
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(ivMyPageAvatar);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(avatarUpdatedReceiver);
    }
}