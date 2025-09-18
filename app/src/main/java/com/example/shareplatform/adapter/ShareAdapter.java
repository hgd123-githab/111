package com.example.shareplatform.adapter;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.shareplatform.R;
import com.example.shareplatform.model.Share;
import com.example.shareplatform.model.response.LikeResponse;
import com.example.shareplatform.network.ApiClient;
import com.example.shareplatform.network.ApiService;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShareAdapter extends RecyclerView.Adapter<ShareAdapter.ShareViewHolder> {
    private List<Share> shares;
    private int currentUserId;
    private OnLikeStatusChangeListener likeStatusChangeListener;
    private Activity mActivity;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private long lastClickTime = 0;
    private static final long CLICK_INTERVAL = 800; // 防重复点击间隔
    // 新增：存储当前用户最新头像URL，用于列表项刷新
    private String currentUserAvatarUrl;
    private static final String BASE_URL = "http://10.34.2.227:5190/";

    // 回调接口：通知外部刷新列表点赞数
    public interface OnLikeStatusChangeListener {
        void onLikeUpdated(int position);
    }

    public ShareAdapter(Activity activity, List<Share> shares, int currentUserId, OnLikeStatusChangeListener listener) {
        this.mActivity = activity;
        this.shares = shares != null ? shares : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.likeStatusChangeListener = listener;
        // 初始化：加载当前用户默认头像URL
        this.currentUserAvatarUrl = BASE_URL + "user/avatar/" + currentUserId;
    }

    @NonNull
    @Override
    public ShareViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share, parent, false);
        return new ShareViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShareViewHolder holder, int position) {
        Share share = shares.get(position);
        holder.bind(share, position);
    }

    @Override
    public int getItemCount() {
        return shares.size();
    }

    // 外部更新列表数据
    public void setData(List<Share> newShares) {
        this.shares = newShares != null ? newShares : new ArrayList<>();
        notifyDataSetChanged();
    }

    // 新增：外部调用更新当前用户头像URL，并刷新列表中所有当前用户的动态项
    public void updateCurrentUserAvatar(String newAvatarUrl) {
        this.currentUserAvatarUrl = newAvatarUrl;
        // 遍历列表，刷新当前用户发布的动态项头像
        for (int i = 0; i < shares.size(); i++) {
            if (shares.get(i).getUid() == currentUserId) { // 匹配当前用户发布的动态
                notifyItemChanged(i); // 刷新对应位置的列表项
            }
        }
    }

    public class ShareViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername;
        private TextView tvContent;
        private RecyclerView rvImages;
        private TextView tvTime;
        private Button btnLike;
        private ImageAdapter imageAdapter;
        private boolean isLiked = false; // 当前点赞状态（核心标记）
        private int currentShareId;     // 当前分享ID
        private int currentPosition;     // 当前列表位置
        private View itemView;
        private boolean isRequesting = false; // 避免并发请求
        // 新增：动态项中的用户头像控件（昵称左侧）
        private ImageView ivDynamicUserAvatar;

        public ShareViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            // 绑定控件：新增动态头像控件（与item_share.xml中的iv_dynamic_avatar匹配）
            ivDynamicUserAvatar = itemView.findViewById(R.id.iv_dynamic_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvContent = itemView.findViewById(R.id.tv_content);
            rvImages = itemView.findViewById(R.id.image_recycler_view);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnLike = itemView.findViewById(R.id.btn_like);

            // 初始化图片列表（水平布局）
            rvImages.setLayoutManager(new LinearLayoutManager(
                    mActivity, LinearLayoutManager.HORIZONTAL, false
            ));

            // 点赞按钮点击事件：核心逻辑（防抖+状态切换）
            btnLike.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                // 1. 防重复点击：800ms内或请求中，忽略点击
                if (currentTime - lastClickTime < CLICK_INTERVAL || isRequesting) {
                    return;
                }
                lastClickTime = currentTime;
                isRequesting = true;
                // 2. 根据当前状态切换：已点赞→取消，未点赞→点赞
                toggleLike();
            });
        }

        // 绑定数据到UI：新增头像加载逻辑
        public void bind(Share share, int position) {
            currentShareId = share.getSid();
            currentPosition = position;

            // 基础数据绑定
            tvUsername.setText(share.getName());
            tvContent.setText(share.getContent());
            tvTime.setText(share.getCreate_time());
            btnLike.setText(String.valueOf(share.getLikeCount()));

            // 新增：加载动态项中的用户头像（关键）
            loadDynamicItemAvatar(share);

            // 图片列表绑定
            imageAdapter = new ImageAdapter(mActivity, share.getImages());
            rvImages.setAdapter(imageAdapter);

            // 初始化：从后端获取当前用户的点赞状态
            checkLikeStatus();
        }

        // 新增：加载动态项中的用户头像（区分当前用户与其他用户）
        private void loadDynamicItemAvatar(Share share) {
            if (ivDynamicUserAvatar == null) return;

            // 基础URL（与后端服务地址一致）
            String baseUrl = "http://10.34.2.227:5190";

            if (share.getUid() == currentUserId) {
                // 当前用户的动态：加载最新头像
                Glide.with(mActivity)
                        .load(currentUserAvatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.baseline_person_24)
                        .error(R.drawable.baseline_person_24)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(ivDynamicUserAvatar);
            } else {
                // 其他用户的动态：加载对应的头像
                String otherUserAvatarUrl = share.getAvatarUrl();
                Log.d("Avatar", "Other user avatar URL from server: " + otherUserAvatarUrl);

                // 处理URL（后端返回的是相对路径，需要拼接基础URL）
                if (otherUserAvatarUrl != null && !otherUserAvatarUrl.startsWith("http")) {
                    // 拼接完整URL（处理可能的重复斜杠问题）
                    if (otherUserAvatarUrl.startsWith("/")) {
                        otherUserAvatarUrl = baseUrl + otherUserAvatarUrl;
                    } else {
                        otherUserAvatarUrl = baseUrl + "/" + otherUserAvatarUrl;
                    }
                }

                Log.d("Avatar", "Other user avatar full URL: " + otherUserAvatarUrl);

                // 如果URL仍为null，使用默认头像
                if (otherUserAvatarUrl == null) {
                    Glide.with(mActivity)
                            .load(R.drawable.baseline_person_24)
                            .circleCrop()
                            .into(ivDynamicUserAvatar);
                } else {
                    // 加载其他用户头像
                    Glide.with(mActivity)
                            .load(otherUserAvatarUrl)
                            .circleCrop()
                            .placeholder(R.drawable.baseline_person_24)  // 加载中显示默认头像
                            .error(R.drawable.baseline_person_24)        // 加载失败显示默认头像
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)  // 恢复默认缓存策略
                            .skipMemoryCache(false)                         // 恢复内存缓存
                            .into(ivDynamicUserAvatar);
                }
            }
        }
        // 核心：根据点赞状态切换操作（原有逻辑不变）
        private void toggleLike() {
            if (isLiked) {
                cancelLike(); // 已点赞 → 执行取消点赞
            } else {
                doLike();     // 未点赞 → 执行点赞
            }
        }

        // 1. 检查点赞状态：从后端同步isLiked值（原有逻辑不变）
        private void checkLikeStatus() {
            ApiService apiService = ApiClient.getApiService();
            apiService.checkLike(currentUserId, currentShareId).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String result = response.body().string();
                            JSONObject jsonResult = new JSONObject(result);
                            isLiked = jsonResult.getBoolean("is_liked");
                            updateLikeButtonUI();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        isRequesting = false;
                        if (response.errorBody() != null) {
                            response.errorBody().close();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    t.printStackTrace();
                    isRequesting = false;
                }
            });
        }

        // 2. 点赞操作：调用后端/like接口（原有逻辑不变）
        private void doLike() {
            ApiService apiService = ApiClient.getApiService();
            JSONObject json = new JSONObject();
            try {
                json.put("uid", currentUserId);
                json.put("sid", currentShareId);
                RequestBody body = RequestBody.create(JSON, json.toString());

                apiService.likeShare(body).enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        try {
                            if (response.code() == 409) {
                                isLiked = true;
                                updateLikeButtonUI();
                            } else if (response.isSuccessful() && response.body() != null) {
                                LikeResponse likeResponse = response.body();
                                int newLikeCount = likeResponse.getLike_count();
                                shares.get(currentPosition).setLikeCount(newLikeCount);
                                isLiked = true;
                                updateLikeButtonUI();
                                if (likeStatusChangeListener != null) {
                                    likeStatusChangeListener.onLikeUpdated(currentPosition);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            isRequesting = false;
                            if (response.errorBody() != null) {
                                response.errorBody().close();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        t.printStackTrace();
                        isRequesting = false;
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
                isRequesting = false;
            }
        }

        // 3. 取消点赞操作：调用后端/unlike接口（原有逻辑不变）
        private void cancelLike() {
            ApiService apiService = ApiClient.getApiService();
            JSONObject json = new JSONObject();
            try {
                json.put("uid", currentUserId);
                json.put("sid", currentShareId);
                RequestBody body = RequestBody.create(JSON, json.toString());

                apiService.unlikeShare(body).enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        try {
                            if (response.code() == 400) {
                                isLiked = false;
                                updateLikeButtonUI();
                            } else if (response.isSuccessful() && response.body() != null) {
                                LikeResponse likeResponse = response.body();
                                int newLikeCount = likeResponse.getLike_count();
                                shares.get(currentPosition).setLikeCount(newLikeCount);
                                isLiked = false;
                                updateLikeButtonUI();
                                if (likeStatusChangeListener != null) {
                                    likeStatusChangeListener.onLikeUpdated(currentPosition);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            isRequesting = false;
                            if (response.errorBody() != null) {
                                response.errorBody().close();
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<LikeResponse> call, Throwable t) {
                        t.printStackTrace();
                        isRequesting = false;
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
                isRequesting = false;
            }
        }

        // 4. 更新点赞按钮UI（原有逻辑不变）
        private void updateLikeButtonUI() {
            if (isLiked) {
                // 已点赞状态：红色实心爱心 + 红色文字
                Drawable favoriteDrawable = ContextCompat.getDrawable(mActivity, R.drawable.baseline_favorite_24);
                if (favoriteDrawable != null) {
                    favoriteDrawable.setColorFilter(
                            ContextCompat.getColor(mActivity, android.R.color.holo_red_dark),
                            PorterDuff.Mode.SRC_IN
                    );
                    btnLike.setCompoundDrawablesWithIntrinsicBounds(favoriteDrawable, null, null, null);
                }
                btnLike.setTextColor(ContextCompat.getColor(mActivity, android.R.color.holo_red_dark));
            } else {
                // 未点赞状态：灰色边框爱心 + 灰色文字
                Drawable favoriteBorderDrawable = ContextCompat.getDrawable(mActivity, R.drawable.baseline_favorite_border_24);
                if (favoriteBorderDrawable != null) {
                    favoriteBorderDrawable.setColorFilter(
                            ContextCompat.getColor(mActivity, android.R.color.darker_gray),
                            PorterDuff.Mode.SRC_IN
                    );
                    btnLike.setCompoundDrawablesWithIntrinsicBounds(favoriteBorderDrawable, null, null, null);
                }
                btnLike.setTextColor(ContextCompat.getColor(mActivity, android.R.color.darker_gray));
            }
            btnLike.setText(String.valueOf(shares.get(currentPosition).getLikeCount()));
        }

        // 保留Snackbar方法（不调用，避免弹窗）
        private void showSnackbar(String message) {
            if (mActivity == null || mActivity.isFinishing() || mActivity.isDestroyed()) {
                return;
            }
            View rootView = mActivity.findViewById(android.R.id.content);
            if (rootView != null) {
                Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show();
            }
        }
    }
}