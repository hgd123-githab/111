package com.example.shareplatform.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.shareplatform.R;
import com.example.shareplatform.network.ApiClient;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<String> imageUrls;
    private Context context;
    private static final String BASE_URL = "http://10.34.48.10:5190"; // 替换为实际服务器地址

    public ImageAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        this.context = null; // 由onCreateViewHolder设置
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        holder.bind(imageUrl);
    }

    @Override
    public int getItemCount() {
        return imageUrls != null ? imageUrls.size() : 0;
    }

    public class ImageViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_image);
        }

        public void bind(String imageUrl) {
            // 处理相对URL（Flask返回的是/image/uid/filename）
            if (imageUrl.startsWith("/image")) {
                imageUrl = BASE_URL + imageUrl; // 拼接完整URL
            }

            Log.d("ImageAdapter", "加载图片: " + imageUrl);

            // 使用Glide加载图片
            Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_background)
                    .into(imageView);

            /* 备用方案：手动加载图片（Glide失败时使用）
            ApiClient.getApiService().getImage(uid, filename).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            byte[] bytes = response.body().bytes();
                            final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                            if (itemView.getContext() instanceof Activity) {
                                ((Activity) itemView.getContext()).runOnUiThread(() -> {
                                    imageView.setImageBitmap(bitmap);
                                });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    t.printStackTrace();
                }
            });
            */
        }
    }
}