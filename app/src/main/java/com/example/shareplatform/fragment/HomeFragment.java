package com.example.shareplatform.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareplatform.R;
import com.example.shareplatform.activity.Show;
import com.example.shareplatform.adapter.ShareAdapter;
import com.example.shareplatform.model.Share;
import com.example.shareplatform.util.Resource;
import com.example.shareplatform.viewmodel.ShareViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private RecyclerView recyclerView;
    private ShareAdapter shareAdapter;
    private ProgressBar progressBar;
    private ShareViewModel shareViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
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
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        shareAdapter = new ShareAdapter();
        //更改
        // 设置item点击事件
        shareAdapter.setOnShareItemClickListener(share -> {
            // 跳转到详情页ShowActivity
            Intent intent = new Intent(requireContext(), Show.class);
            // 传递分享数据到详情页
            intent.putExtra("share_id", share.getSid());
            intent.putExtra("content", share.getContent());
            intent.putExtra("uid", share.getUid());
            intent.putExtra("create_time", share.getCreate_time());
            // 传递图片列表
            intent.putStringArrayListExtra("images", new ArrayList<>(share.getImages()));
            startActivity(intent);
        });
        //更改
        recyclerView.setAdapter(shareAdapter);
    }

    private void loadData() {
        shareViewModel = new ViewModelProvider(this).get(ShareViewModel.class);
        shareViewModel.getShares(); // 加载**所有用户**的分享
    }

    private void setupObservers() {
        shareViewModel.getSharesLiveData().observe(getViewLifecycleOwner(), resource -> {
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

}