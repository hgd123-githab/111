package com.example.shareplatform.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.concurrent.TimeUnit;

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

    // 回调接口：通知外部刷新列表点赞数
    public interface OnLikeStatusChangeListener {
        void onLikeUpdated(int position);
    }

    public ShareAdapter(Activity activity, List<Share> shares, int currentUserId, OnLikeStatusChangeListener listener) {
        this.mActivity = activity;
        this.shares = shares != null ? shares : new ArrayList<>();
        this.currentUserId = currentUserId;
        this.likeStatusChangeListener = listener;
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

        public ShareViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            // 绑定控件
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

        // 绑定数据到UI
        public void bind(Share share, int position) {
            currentShareId = share.getSid();
            currentPosition = position;

            // 基础数据绑定
            tvUsername.setText(share.getName());
            tvContent.setText(share.getContent());
            tvTime.setText(share.getCreate_time());
            btnLike.setText(String.valueOf(share.getLikeCount()));

            // 图片列表绑定
            imageAdapter = new ImageAdapter(mActivity, share.getImages());
            rvImages.setAdapter(imageAdapter);

            // 初始化：从后端获取当前用户的点赞状态（关键，确保状态正确）
            checkLikeStatus();
        }

        // 核心：根据点赞状态切换操作
        private void toggleLike() {
            if (isLiked) {
                cancelLike(); // 已点赞 → 执行取消点赞
            } else {
                doLike();     // 未点赞 → 执行点赞
            }
        }

        // 1. 检查点赞状态：从后端同步isLiked值
        private void checkLikeStatus() {
            ApiService apiService = ApiClient.getApiService();
            apiService.checkLike(currentUserId, currentShareId).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            // 解析后端返回的 {"is_liked":true/false}（与Flask接口返回一致）
                            String result = response.body().string();
                            JSONObject jsonResult = new JSONObject(result);
                            // 关键：精准获取is_liked字段，避免解析错误
                            isLiked = jsonResult.getBoolean("is_liked");
                            updateLikeButtonUI(); // 同步按钮UI
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        isRequesting = false; // 无论成功失败，重置请求状态
                        if (response.errorBody() != null) {
                            response.errorBody().close(); // 关闭错误流，避免资源泄漏
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

        // 2. 点赞操作：调用后端/like接口
        private void doLike() {
            ApiService apiService = ApiClient.getApiService();
            JSONObject json = new JSONObject();
            try {
                // 构造请求参数（与Flask接口要求的uid、sid一致）
                json.put("uid", currentUserId);
                json.put("sid", currentShareId);
                RequestBody body = RequestBody.create(JSON, json.toString());

                apiService.likeShare(body).enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        try {
                            if (response.code() == 409) {
                                // 重复点赞（后端返回409）：同步状态为已点赞
                                isLiked = true;
                                updateLikeButtonUI();
                            } else if (response.isSuccessful() && response.body() != null) {
                                // 点赞成功：更新本地数据和UI
                                LikeResponse likeResponse = response.body();
                                int newLikeCount = likeResponse.getLike_count();
                                shares.get(currentPosition).setLikeCount(newLikeCount);
                                isLiked = true;
                                updateLikeButtonUI();
                                // 通知外部刷新列表（如需要）
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

        // 3. 取消点赞操作：调用后端/unlike接口（核心功能）
        private void cancelLike() {
            ApiService apiService = ApiClient.getApiService();
            JSONObject json = new JSONObject();
            try {
                // 构造请求参数（与Flask接口要求的uid、sid完全一致）
                json.put("uid", currentUserId);
                json.put("sid", currentShareId);
                RequestBody body = RequestBody.create(JSON, json.toString());

                // 调用取消点赞接口（与ApiService定义的unlikeShare一致）
                apiService.unlikeShare(body).enqueue(new Callback<LikeResponse>() {
                    @Override
                    public void onResponse(Call<LikeResponse> call, Response<LikeResponse> response) {
                        try {
                            if (response.code() == 400) {
                                // 未点赞却取消（后端返回400）：同步状态为未点赞
                                isLiked = false;
                                updateLikeButtonUI();
                            } else if (response.isSuccessful() && response.body() != null) {
                                // 取消点赞成功：更新本地数据和UI（与Flask返回的like_count同步）
                                LikeResponse likeResponse = response.body();
                                int newLikeCount = likeResponse.getLike_count();
                                // 关键：更新当前分享的点赞数，确保UI显示正确
                                shares.get(currentPosition).setLikeCount(newLikeCount);
                                isLiked = false; // 标记为未点赞
                                updateLikeButtonUI(); // 刷新按钮为灰色边框
                                // 通知外部刷新列表（如需要）
                                if (likeStatusChangeListener != null) {
                                    likeStatusChangeListener.onLikeUpdated(currentPosition);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            isRequesting = false;
                            if (response.errorBody() != null) {
                                response.errorBody().close(); // 关闭错误流，避免资源泄漏
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

        // 4. 更新点赞按钮UI：已点赞（红色实心）/未点赞（灰色边框）
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
            // 同步显示最新点赞数（从本地shares列表获取）
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