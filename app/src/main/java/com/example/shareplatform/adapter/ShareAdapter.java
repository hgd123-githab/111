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
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
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
    private static final String TAG = "ShareAdapter";
    private List<Share> shares = new ArrayList<>();
    private int currentUserId;
    private OnLikeStatusChangeListener likeStatusChangeListener;
    private OnShareDeleteListener deleteListener;
    private Activity mActivity;
    private boolean isMySharePage;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private long lastClickTime = 0;
    private static final long CLICK_INTERVAL = 800;
    private String currentUserAvatarUrl;
    private static final String BASE_URL = "http://10.34.2.227:5190/";

    // 回调接口
    public interface OnLikeStatusChangeListener {
        void onLikeUpdated(int position);
    }

    public interface OnShareDeleteListener {
        void onShareDeleted(int shareId);
    }

    // 构造函数
    public ShareAdapter(Activity activity, List<Share> shares, int currentUserId,
                        OnLikeStatusChangeListener likeListener, OnShareDeleteListener deleteListener,
                        boolean isMySharePage) {
        this.mActivity = activity;
        this.shares = shares != null ? shares : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.likeStatusChangeListener = likeListener;
        this.deleteListener = deleteListener;
        this.isMySharePage = isMySharePage;
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
        if (position >= shares.size()) return;
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

    // 更新当前用户头像
    public void updateCurrentUserAvatar(String newAvatarUrl) {
        this.currentUserAvatarUrl = newAvatarUrl;
        for (int i = 0; i < shares.size(); i++) {
            if (shares.get(i).getUid() == currentUserId) {
                notifyItemChanged(i);
            }
        }
    }

    // 分享项 ViewHolder
    public class ShareViewHolder extends RecyclerView.ViewHolder {
        private TextView tvUsername;
        private TextView tvContent;
        private RecyclerView rvImages;
        private TextView tvTime;
        private Button btnLike;
        private Button btnDelete;
        private TextView tvNoImage;
        private ImageAdapter imageAdapter;
        private boolean isLiked = false;
        private int currentShareId;
        private int currentPosition;
        private boolean isRequesting = false;
        private ImageView ivDynamicUserAvatar;

        public ShareViewHolder(View itemView) {
            super(itemView);
            // 绑定控件
            ivDynamicUserAvatar = itemView.findViewById(R.id.iv_dynamic_avatar);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvContent = itemView.findViewById(R.id.tv_content);
            rvImages = itemView.findViewById(R.id.image_recycler_view);
            tvNoImage = itemView.findViewById(R.id.tv_no_image);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnLike = itemView.findViewById(R.id.btn_like);
            btnDelete = itemView.findViewById(R.id.btn_delete);

            // 图片列表布局
            rvImages.setLayoutManager(new LinearLayoutManager(
                    mActivity, LinearLayoutManager.HORIZONTAL, false
            ));

            // 点赞按钮逻辑
            btnLike.setOnClickListener(v -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastClickTime < CLICK_INTERVAL || isRequesting) return;
                lastClickTime = currentTime;
                isRequesting = true;
                toggleLike();
            });

            // 删除按钮逻辑：防重复点击
            btnDelete.setOnClickListener(v -> {
                currentPosition = getAdapterPosition();
                if (currentPosition == RecyclerView.NO_POSITION || currentPosition >= shares.size()) return;
                if (isRequesting) return;

                currentShareId = shares.get(currentPosition).getSid();
                isRequesting = true;
                deleteShareFromServer();
            });
        }

        public void bind(Share share, int position) {
            currentShareId = share.getSid();
            currentPosition = position;

            // 基础数据绑定
            tvUsername.setText(share.getName() != null ? share.getName() : "未知用户");
            tvContent.setText(share.getContent() != null ? share.getContent() : "");
            tvTime.setText(share.getCreate_time() != null ? share.getCreate_time() : "");
            btnLike.setText(String.valueOf(share.getLikeCount()));

            // 处理图片/无图片布局
            List<String> images = share.getImages();
            if (images != null && !images.isEmpty()) {
                rvImages.setVisibility(View.VISIBLE);
                tvNoImage.setVisibility(View.GONE);
                imageAdapter = new ImageAdapter(mActivity, images);
                rvImages.setAdapter(imageAdapter);
            } else {
                rvImages.setVisibility(View.GONE);
                tvNoImage.setVisibility(View.VISIBLE);
            }

            // 条件显示删除按钮（仅我的分享+当前用户发布）
            if (isMySharePage && share.getUid() == currentUserId) {
                btnDelete.setVisibility(View.VISIBLE);
            } else {
                btnDelete.setVisibility(View.GONE);
            }

            // 加载动态头像（带超时）
            final String loadUrl = share.getUid() == currentUserId ? currentUserAvatarUrl :
                    (share.getAvatarUrl() != null && !share.getAvatarUrl().startsWith("http")
                            ? "http://10.34.2.227:5190" + (share.getAvatarUrl().startsWith("/") ? share.getAvatarUrl() : "/" + share.getAvatarUrl())
                            : (share.getAvatarUrl() != null ? share.getAvatarUrl() : ""));
            loadDynamicAvatar(loadUrl);

            // 检查点赞状态
            checkLikeStatus();
        }

        // 加载头像（防网络阻塞）
        private void loadDynamicAvatar(final String loadUrl) {
            if (ivDynamicUserAvatar == null || mActivity.isFinishing()) return;

            RequestOptions options = new RequestOptions()
                    .circleCrop()
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .timeout(15000);

            Glide.with(mActivity)
                    .load(loadUrl)
                    .apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "加载头像失败：" + loadUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(ivDynamicUserAvatar);
        }

        // #################### 关键修复：删除请求逻辑 ####################
        // 找到deleteShareFromServer方法，修改如下：
        private void deleteShareFromServer() {
            if (mActivity.isFinishing()) {
                isRequesting = false;
                return;
            }

            ApiService apiService = ApiClient.getApiService();
            // 关键修复：传入Content-Type为application/json
            Call<ResponseBody> call = apiService.deleteShare(
                    currentShareId,
                    currentUserId,
                    "application/json"  // 新增：指定请求格式
            );
            Log.d(TAG, "发送删除请求：sid=" + currentShareId + ", uid=" + currentUserId);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    isRequesting = false;
                    Log.d(TAG, "删除接口响应：code=" + response.code());

                    try {
                        if (response.isSuccessful()) {
                            // 本地列表删除
                            shares.remove(currentPosition);
                            notifyItemRemoved(currentPosition);
                            notifyItemRangeChanged(currentPosition, shares.size());
                            // 通知主页面
                            if (deleteListener != null) {
                                deleteListener.onShareDeleted(currentShareId);
                            }
                            Snackbar.make(itemView, "删除成功", Snackbar.LENGTH_SHORT).show();
                        } else {
                            String errorMsg = "删除失败（" + response.code() + "）";
                            if (response.errorBody() != null) {
                                String errorBody = response.errorBody().string();
                                Log.e(TAG, "删除错误体：" + errorBody);
                                try {
                                    // 仅当错误体是JSON格式时才解析
                                    if (errorBody.startsWith("{")) {
                                        JSONObject errorJson = new JSONObject(errorBody);
                                        errorMsg = errorJson.getString("msg");
                                    }
                                } catch (JSONException e) {
                                    Log.e(TAG, "解析错误信息失败", e);
                                }
                            }
                            Snackbar.make(itemView, errorMsg, Snackbar.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "处理删除响应失败", e);
                        Snackbar.make(itemView, "删除失败：解析错误", Snackbar.LENGTH_SHORT).show();
                    } finally {
                        if (response.errorBody() != null) {
                            try {
                                response.errorBody().close();
                            } catch (Exception e) {
                                Log.e(TAG, "关闭响应流失败", e);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    isRequesting = false;
                    Log.e(TAG, "删除请求失败", t);
                    Snackbar.make(itemView, "删除失败：网络错误", Snackbar.LENGTH_SHORT).show();
                }
            });
        }


        // 点赞/取消点赞逻辑（保持不变）
        private void toggleLike() {
            if (isLiked) cancelLike();
            else doLike();
        }

        private void checkLikeStatus() {
            ApiService apiService = ApiClient.getApiService();
            apiService.checkLike(currentUserId, currentShareId).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            JSONObject json = new JSONObject(response.body().string());
                            isLiked = json.getBoolean("is_liked");
                            updateLikeButtonUI();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        isRequesting = false;
                        if (response.errorBody() != null) {
                            try {
                                response.errorBody().close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
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
                                int newLikeCount = response.body().getLike_count();
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
                                int newLikeCount = response.body().getLike_count();
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

        private void updateLikeButtonUI() {
            Drawable drawable;
            int textColor;
            if (isLiked) {
                drawable = ContextCompat.getDrawable(mActivity, R.drawable.baseline_favorite_24);
                textColor = ContextCompat.getColor(mActivity, android.R.color.holo_red_dark);
            } else {
                drawable = ContextCompat.getDrawable(mActivity, R.drawable.baseline_favorite_border_24);
                textColor = ContextCompat.getColor(mActivity, android.R.color.darker_gray);
            }
            if (drawable != null) {
                drawable.setColorFilter(textColor, PorterDuff.Mode.SRC_IN);
                btnLike.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }
            btnLike.setTextColor(textColor);
            btnLike.setText(String.valueOf(shares.get(currentPosition).getLikeCount()));
        }
    }

    // 图片适配器（保持不变）
    private static class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
        private Activity activity;
        private List<String> imageUrls = new ArrayList<>();

        public ImageAdapter(Activity activity, List<String> imageUrls) {
            this.activity = activity;
            this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            if (position >= imageUrls.size()) return;
            final String imageUrl = !imageUrls.get(position).startsWith("http")
                    ? "http://10.34.2.227:5190" + imageUrls.get(position)
                    : imageUrls.get(position);

            RequestOptions options = new RequestOptions()
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.baseline_broken_image_24)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .timeout(15000);

            Glide.with(activity)
                    .load(imageUrl)
                    .apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e(TAG, "加载图片失败：" + imageUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            return false;
                        }
                    })
                    .into(holder.ivImage);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;

            public ImageViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.iv_image);
            }
        }
    }
}