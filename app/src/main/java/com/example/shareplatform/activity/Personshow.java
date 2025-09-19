package com.example.shareplatform.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
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
import com.bumptech.glide.request.RequestOptions;
import com.example.shareplatform.R;
import com.example.shareplatform.adapter.ShareAdapter;
import com.example.shareplatform.model.Share;
import com.example.shareplatform.network.ApiClient;
import com.example.shareplatform.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Personshow extends AppCompatActivity {
    private static final String TAG = "Personshow";
    private int userId;
    private ShareAdapter shareAdapter;
    private RecyclerView recyclerView;
    private ImageView userAvatar;
    private android.os.IBinder mWindowToken;
    private List<Share> userShareList = new ArrayList<>();
    public static final String ACTION_SHARE_DELETED = "com.example.shareplatform.ACTION_SHARE_DELETED";
    public static final String EXTRA_DELETED_SHARE_ID = "deleted_share_id";

    // 头像更新广播接收器
    private BroadcastReceiver avatarUpdatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isFinishing()) return;
            if (intent.getAction().equals(PersonalInformation.ACTION_AVATAR_UPDATED)) {
                String avatarUrl = intent.getStringExtra("avatar_url");
                if (avatarUrl != null && userAvatar != null) {
                    RequestOptions options = new RequestOptions()
                            .circleCrop()
                            .placeholder(R.drawable.baseline_person_24)
                            .error(R.drawable.baseline_person_24)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .timeout(15000);

                    Glide.with(Personshow.this)
                            .load(avatarUrl)
                            .apply(options)
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
        userAvatar = findViewById(R.id.iv_user_avatar);

        // 1. 获取用户ID
        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "获取用户ID失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. 初始化Adapter（传递删除回调）
        shareAdapter = new ShareAdapter(
                this,
                userShareList,
                userId,
                position -> shareAdapter.notifyItemChanged(position),
                // 删除回调：重新加载数据，确保与后端同步
                deletedShareId -> {
                    Log.d(TAG, "收到删除通知：sid=" + deletedShareId);
                    loadUserShares();
                },
                true // 标记为“我的分享”页面
        );
        recyclerView.setAdapter(shareAdapter);

        // 3. 加载我的分享数据
        loadUserShares();

        // 4. 注册广播
        LocalBroadcastManager.getInstance(this).registerReceiver(
                avatarUpdatedReceiver,
                new IntentFilter(PersonalInformation.ACTION_AVATAR_UPDATED)
        );
        loadUserAvatar();
    }

    // 加载用户头像
    private void loadUserAvatar() {
        if (userId == -1 || userAvatar == null || isFinishing()) return;
        String avatarUrl = "http://10.34.2.227:5190/user/avatar/" + userId;

        RequestOptions options = new RequestOptions()
                .circleCrop()
                .placeholder(R.drawable.baseline_person_24)
                .error(R.drawable.baseline_person_24)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .timeout(15000);

        Glide.with(this)
                .load(avatarUrl)
                .apply(options)
                .into(userAvatar);
    }

    // 加载我的分享数据（带日志）
    private void loadUserShares() {
        ApiService apiService = ApiClient.getApiService();
        Call<List<Share>> call = apiService.getMyShares(userId);
        Log.d(TAG, "加载我的分享：uid=" + userId);

        call.enqueue(new Callback<List<Share>>() {
            @Override
            public void onResponse(Call<List<Share>> call, Response<List<Share>> response) {
                if (isFinishing()) return;
                Log.d(TAG, "加载分享响应：code=" + response.code());

                if (response.isSuccessful()) {
                    userShareList = response.body() != null ? response.body() : new ArrayList<>();
                    Log.d(TAG, "加载到分享数量：" + userShareList.size());
                    shareAdapter.setData(userShareList);
                } else {
                    String errorMsg = "获取分享失败：" + response.code();
                    Toast.makeText(Personshow.this, errorMsg, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<Share>> call, Throwable t) {
                if (isFinishing()) return;
                String errorMsg = "加载分享网络失败：" + t.getMessage();
                Toast.makeText(Personshow.this, errorMsg, Toast.LENGTH_SHORT).show();
                Log.e(TAG, errorMsg, t);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(avatarUpdatedReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mWindowToken != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mWindowToken, 0);
        }
    }
}