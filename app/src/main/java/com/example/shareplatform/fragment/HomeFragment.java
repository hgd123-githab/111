package com.example.shareplatform.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shareplatform.R;
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
    private int currentUserId; // 当前登录用户ID

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 加载碎片布局（确保包含recycler_view和progress_bar）
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 初始化ViewModel：获取用户ID和分享数据
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        shareViewModel = new ViewModelProvider(this).get(ShareViewModel.class);
        currentUserId = authViewModel.getUserId(); // 从AuthViewModel获取登录用户ID

        // 校验用户登录状态（避免uid=-1）
        if (currentUserId == -1) {
            showSnackbar("请先登录后操作");
            return;
        }

        // 2. 初始化视图和适配器
        initViews(view);
        initShareAdapter();

        // 3. 加载分享数据并观察变化
        loadShareData();
        observeShareData();
    }

    // 初始化视图：绑定RecyclerView和ProgressBar
    private void initViews(View view) {
        rvShares = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        // 设置RecyclerView布局管理器（垂直列表）
        rvShares.setLayoutManager(new LinearLayoutManager(requireContext()));
        // 优化RecyclerView性能（固定高度）
        rvShares.setHasFixedSize(true);
    }

    // 初始化ShareAdapter：传入Activity和回调
    private void initShareAdapter() {
        shareAdapter = new ShareAdapter(
                requireActivity(), // 传入Fragment依附的Activity（关键，避免资源异常）
                null, // 初始空数据
                currentUserId, // 当前登录用户ID
                position -> {
                    // 点赞状态更新回调：刷新当前列表项
                    shareAdapter.notifyItemChanged(position);
                    // 可选：重新加载所有分享（确保与后端数据完全同步）
                    loadShareData();
                }
        );
        rvShares.setAdapter(shareAdapter);
    }

    // 加载分享数据：调用ViewModel获取所有用户的分享
    private void loadShareData() {
        progressBar.setVisibility(View.VISIBLE); // 显示加载框
        shareViewModel.getShares(); // 触发数据加载
    }

    // 观察分享数据变化：更新适配器和UI
    private void observeShareData() {
        shareViewModel.getSharesLiveData().observe(getViewLifecycleOwner(), new Observer<Resource<List<Share>>>() {
            @Override
            public void onChanged(Resource<List<Share>> resource) {
                progressBar.setVisibility(View.GONE); // 隐藏加载框

                if (resource instanceof Resource.Success) {
                    // 数据加载成功：更新适配器数据
                    List<Share> shares = ((Resource.Success<List<Share>>) resource).getData();
                    shareAdapter.setData(shares);
                } else if (resource instanceof Resource.Error) {
                    // 数据加载失败：提示用户
                    String errorMsg = ((Resource.Error<List<Share>>) resource).getMessage();
                    showSnackbar("加载分享失败：" + errorMsg);
                }
            }
        });
    }

    // 显示Snackbar提示（统一提示方式）
    private void showSnackbar(String message) {
        if (getView() != null && requireActivity() != null && !requireActivity().isFinishing()) {
            Snackbar.make(
                    getView(), // 依附于碎片根视图
                    message,
                    Snackbar.LENGTH_SHORT
            ).show();
        }
    }
}