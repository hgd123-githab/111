package com.example.shareplatform.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.shareplatform.R;
import com.example.shareplatform.activity.PersonalInformation;
import com.example.shareplatform.adapter.ShareAdapter;
import com.example.shareplatform.model.Share;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.AuthViewModel;
import com.example.shareplatform.viewmodel.ShareViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView rvShares;
    private ShareAdapter shareAdapter;
    private ProgressBar progressBar;
    private ShareViewModel shareViewModel;
    private AuthViewModel authViewModel;
    private int currentUserId;
    private ImageView userAvatar;
    private static final String BASE_URL = "http://10.34.2.227:5190/";

    // 广播接收器（使用统一Action）
    private BroadcastReceiver avatarUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PersonalInformation.ACTION_AVATAR_UPDATED)) {
                String avatarUrl = intent.getStringExtra("avatar_url");
                if (avatarUrl != null) {
                    // 1. 更新顶部个人头像（原有逻辑）
                    if (userAvatar != null) {
                        Glide.with(requireContext())
                                .load(avatarUrl)
                                .circleCrop()
                                .placeholder(R.drawable.baseline_person_24)
                                .error(R.drawable.baseline_person_24)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .into(userAvatar);
                    }
                    // 2. 新增：通知Adapter更新列表中当前用户的动态头像
                    if (shareAdapter != null) {
                        shareAdapter.updateCurrentUserAvatar(avatarUrl);
                    }
                }
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initViewModels();
        checkLoginStatus();
        initShareAdapter();
        loadShareData();
        observeShareData();
        registerAvatarReceiver();
        loadInitialAvatar();
    }

    private void initViews(View view) {
        rvShares = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        userAvatar = view.findViewById(R.id.iv_home_avatar); // 顶部个人头像
        rvShares.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvShares.setHasFixedSize(true);
    }

    private void initViewModels() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        shareViewModel = new ViewModelProvider(this).get(ShareViewModel.class);
    }

    private void checkLoginStatus() {
        currentUserId = authViewModel.getUserId();
        if (currentUserId == -1) {
            showSnackbar("请先登录");
        }
    }

    private void initShareAdapter() {
        // 初始化Adapter时传入当前用户ID，用于区分动态归属
        shareAdapter = new ShareAdapter(
                requireActivity(),
                null,
                currentUserId,
                position -> shareAdapter.notifyItemChanged(position)
        );
        rvShares.setAdapter(shareAdapter);
    }

    private void loadShareData() {
        if (currentUserId != -1) {
            progressBar.setVisibility(View.VISIBLE);
            shareViewModel.getShares();
        }
    }

    private void observeShareData() {
        shareViewModel.getSharesLiveData().observe(getViewLifecycleOwner(), new Observer<Resource<List<Share>>>() {
            @Override
            public void onChanged(Resource<List<Share>> resource) {
                progressBar.setVisibility(View.GONE);
                if (resource instanceof Resource.Success) {
                    shareAdapter.setData(((Resource.Success<List<Share>>) resource).getData());
                } else if (resource instanceof Resource.Error) {
                    showSnackbar("加载失败: " + ((Resource.Error<List<Share>>) resource).getMessage());
                }
            }
        });
    }

    private void registerAvatarReceiver() {
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(avatarUpdatedReceiver,
                        new IntentFilter(PersonalInformation.ACTION_AVATAR_UPDATED));
    }

    private void loadInitialAvatar() {
        if (currentUserId != -1 && userAvatar != null) {
            String avatarUrl = BASE_URL + "user/avatar/" + currentUserId;
            Glide.with(requireContext())
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(userAvatar);
        }
    }

    private void showSnackbar(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(requireContext())
                .unregisterReceiver(avatarUpdatedReceiver);
    }
}