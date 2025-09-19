package com.example.shareplatform.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
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
import com.bumptech.glide.request.RequestOptions;
import com.example.shareplatform.R;
import com.example.shareplatform.activity.PersonalInformation;
import com.example.shareplatform.activity.Personshow;
import com.example.shareplatform.adapter.ShareAdapter;
import com.example.shareplatform.model.Share;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.AuthViewModel;
import com.example.shareplatform.viewmodel.ShareViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";
    private RecyclerView rvShares;
    private ShareAdapter shareAdapter;
    private ProgressBar progressBar;
    private ShareViewModel shareViewModel;
    private AuthViewModel authViewModel;
    private int currentUserId;
    private ImageView userAvatar;
    private static final String BASE_URL = "http://10.34.2.227:5190/";

    // 广播接收器
    private BroadcastReceiver avatarUpdatedReceiver;
    private BroadcastReceiver shareDeletedReceiver;
    private boolean isAvatarReceiverRegistered = false;
    private boolean isShareDeletedReceiverRegistered = false;

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
        initReceivers();
        checkLoginStatus();
        initShareAdapter();
        loadShareData();
        observeShareData();
        registerReceivers();
        loadInitialAvatar();
    }

    private void initViews(View view) {
        rvShares = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        userAvatar = view.findViewById(R.id.iv_home_avatar);
        rvShares.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvShares.setHasFixedSize(true);
    }

    private void initViewModels() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        shareViewModel = new ViewModelProvider(this).get(ShareViewModel.class);
    }

    private void initReceivers() {
        // 头像更新广播
        avatarUpdatedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getActivity() == null || getActivity().isFinishing()) return;
                if (intent.getAction().equals(PersonalInformation.ACTION_AVATAR_UPDATED)) {
                    String avatarUrl = intent.getStringExtra("avatar_url");
                    if (avatarUrl != null) {
                        if (userAvatar != null) {
                            RequestOptions options = new RequestOptions()
                                    .circleCrop()
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .timeout(15000);

                            Glide.with(requireContext())
                                    .load(avatarUrl)
                                    .apply(options)
                                    .into(userAvatar);
                        }
                        if (shareAdapter != null) {
                            shareAdapter.updateCurrentUserAvatar(avatarUrl);
                        }
                    }
                }
            }
        };

        // 动态删除广播：增加日志
        shareDeletedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (getActivity() == null || getActivity().isFinishing()) return;
                if (intent.getAction().equals(Personshow.ACTION_SHARE_DELETED)) {
                    int deletedSid = intent.getIntExtra(Personshow.EXTRA_DELETED_SHARE_ID, -1);
                    Log.d(TAG, "收到删除广播，刷新主页面：sid=" + deletedSid);
                    loadShareData();
                }
            }
        };
    }

    private void checkLoginStatus() {
        currentUserId = authViewModel.getUserId();
        if (currentUserId == -1) {
            showSnackbar("请先登录");
        }
    }

    private void initShareAdapter() {
        shareAdapter = new ShareAdapter(
                requireActivity(),
                new ArrayList<>(),
                currentUserId,
                position -> shareAdapter.notifyItemChanged(position),
                null,
                false
        );
        rvShares.setAdapter(shareAdapter);
    }

    // 加载主页面数据增加日志
    private void loadShareData() {
        if (currentUserId == -1 || getActivity() == null || getActivity().isFinishing()) return;
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "加载主页面分享数据");
        shareViewModel.getShares();
    }

    private void observeShareData() {
        shareViewModel.getSharesLiveData().observe(getViewLifecycleOwner(), new Observer<Resource<List<Share>>>() {
            @Override
            public void onChanged(Resource<List<Share>> resource) {
                if (getActivity() == null || getActivity().isFinishing()) return;
                progressBar.setVisibility(View.GONE);
                if (resource instanceof Resource.Success) {
                    List<Share> data = ((Resource.Success<List<Share>>) resource).getData() != null
                            ? ((Resource.Success<List<Share>>) resource).getData()
                            : new ArrayList<>();
                    Log.d(TAG, "主页面加载到分享数量：" + data.size());
                    shareAdapter.setData(data);
                } else if (resource instanceof Resource.Error) {
                    String errorMsg = ((Resource.Error<List<Share>>) resource).getMessage();
                    Log.e(TAG, "主页面加载失败：" + errorMsg);
                    showSnackbar("加载失败: " + errorMsg);
                }
            }
        });
    }

    private void registerReceivers() {
        if (getActivity() == null) return;
        if (!isAvatarReceiverRegistered) {
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                    avatarUpdatedReceiver,
                    new IntentFilter(PersonalInformation.ACTION_AVATAR_UPDATED)
            );
            isAvatarReceiverRegistered = true;
        }
        if (!isShareDeletedReceiverRegistered) {
            LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                    shareDeletedReceiver,
                    new IntentFilter(Personshow.ACTION_SHARE_DELETED)
            );
            isShareDeletedReceiverRegistered = true;
        }
    }

    private void loadInitialAvatar() {
        if (currentUserId == -1 || userAvatar == null || getActivity() == null || getActivity().isFinishing()) return;
        String avatarUrl = BASE_URL + "user/avatar/" + currentUserId;

        RequestOptions options = new RequestOptions()
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .timeout(15000);

        Glide.with(requireContext())
                .load(avatarUrl)
                .apply(options)
                .into(userAvatar);
    }

    private void showSnackbar(String message) {
        if (getView() == null || getActivity() == null || getActivity().isFinishing()) return;
        Snackbar.make(getView(), message, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() == null) return;
        if (isAvatarReceiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(avatarUpdatedReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isAvatarReceiverRegistered = false;
        }
        if (isShareDeletedReceiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(shareDeletedReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isShareDeletedReceiverRegistered = false;
        }
    }
}