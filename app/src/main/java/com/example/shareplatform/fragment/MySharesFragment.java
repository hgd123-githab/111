package com.example.shareplatform.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareplatform.R;
import com.example.shareplatform.activity.LoginActivity;
import com.example.shareplatform.adapter.ShareAdapter;
import com.example.shareplatform.model.Share;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.AuthViewModel;
import com.example.shareplatform.viewmodel.ShareViewModel;

import java.util.List;

public class MySharesFragment extends Fragment {
    private RecyclerView recyclerView;
    private ShareAdapter shareAdapter;
    private ProgressBar progressBar;
    private ShareViewModel shareViewModel;
    private AuthViewModel authViewModel;
    private Button logoutBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_shares, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        setupRecyclerView();
        loadData();
        setupObservers();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        progressBar = view.findViewById(R.id.progress_bar);
        logoutBtn = view.findViewById(R.id.btn_logout);
        logoutBtn.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        shareAdapter = new ShareAdapter();
        recyclerView.setAdapter(shareAdapter);
    }

    private void loadData() {
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        shareViewModel = new ViewModelProvider(this).get(ShareViewModel.class);

        int userId = authViewModel.getUserId();
        if (userId != -1) {
            shareViewModel.getMyShares(userId); // 加载当前用户的分享
        } else {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupObservers() {
        shareViewModel.getMySharesLiveData().observe(getViewLifecycleOwner(), resource -> {
            if (resource instanceof Resource.Loading) {
                showLoading();
            } else if (resource instanceof Resource.Success) {
                hideLoading();
                shareAdapter.setData(((Resource.Success<List<Share>>) resource).getData());
            } else if (resource instanceof Resource.Error) {
                hideLoading();
                Toast.makeText(requireContext(), ((Resource.Error<List<Share>>) resource).getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
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