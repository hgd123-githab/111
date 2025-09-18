package com.example.shareplatform.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.shareplatform.R;
import com.example.shareplatform.adapter.ShareAdapter;
import com.example.shareplatform.model.Share;
import com.example.shareplatform.network.ApiClient;
import com.example.shareplatform.network.ApiService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Personshow extends AppCompatActivity {
    private int userId;
    private ShareAdapter shareAdapter;
    private RecyclerView recyclerView;
    private ImageView userAvatar;
    private android.os.IBinder mWindowToken;

    // 广播接收器（使用统一的Action常量）
    private BroadcastReceiver avatarUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PersonalInformation.ACTION_AVATAR_UPDATED)) {
                String avatarUrl = intent.getStringExtra("avatar_url");
                if (avatarUrl != null && userAvatar != null) {
                    Glide.with(Personshow.this)
                            .load(avatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.baseline_person_24)
                            .error(R.drawable.baseline_person_24)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(userAvatar);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_personshow);

        mWindowToken = getWindow().getDecorView().getWindowToken();
        recyclerView = findViewById(R.id.recyclerView_shares);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAvatar = findViewById(R.id.iv_user_avatar); // 确保布局ID与代码一致

        // 获取用户ID
        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "获取用户ID失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化适配器
        shareAdapter = new ShareAdapter(this, null, userId, position -> {
            shareAdapter.notifyItemChanged(position);
        });
        recyclerView.setAdapter(shareAdapter);

        // 加载数据
        loadUserShares();

        // 注册广播（使用统一Action）
        LocalBroadcastManager.getInstance(this).registerReceiver(
                avatarUpdatedReceiver,
                new IntentFilter(PersonalInformation.ACTION_AVATAR_UPDATED)
        );

        // 首次加载头像
        loadUserAvatar();
    }

    // 加载用户头像（补充方法）
    private void loadUserAvatar() {
        if (userId == -1 || userAvatar == null) return;
        String avatarUrl = "http://10.34.2.227:5190/user/avatar/" + userId;
        Glide.with(this)
                .load(avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(userAvatar);
    }

    private void loadUserShares() {
        ApiService apiService = ApiClient.getApiService();
        Call<List<Share>> call = apiService.getMyShares(userId);
        call.enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                if (response.isSuccessful()) {
                    List<Share> userShares = response.body();
                    if (userShares != null) {
                        shareAdapter.setData(userShares);
                    } else {
                        Toast.makeText(Personshow.this, "没有获取到分享数据", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(Personshow.this, "获取分享数据失败：" + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                Toast.makeText(Personshow.this, "网络请求失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 释放资源（解决输入通道警告）
    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(avatarUpdatedReceiver);
        if (mWindowToken != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mWindowToken, 0);
        }
    }
}