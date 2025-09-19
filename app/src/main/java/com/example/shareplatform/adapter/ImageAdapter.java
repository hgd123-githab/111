package com.example.shareplatform.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.shareplatform.R;
import com.example.shareplatform.activity.ImagePreviewActivity;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private List<String> imageUrls;
    private Context context;
<<<<<<< HEAD
    private static final String BASE_URL = "http://10.34.86.144:5190"; // 替换为实际服务器地址
=======
    private RequestOptions glideOptions = new RequestOptions()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .override(300, 300);
>>>>>>> b4455be20792b16ee6381fb281a2b5bf92670a3b

    public ImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls != null ? imageUrls : List.of();
    }

    public ImageAdapter(List<String> imageUrls) {
        this.context = null;
        this.imageUrls = imageUrls != null ? imageUrls : List.of();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context parentContext = parent.getContext();
        View view = LayoutInflater.from(parentContext).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        Context loadContext = context != null ? context : holder.itemView.getContext();
        String fullUrl = "http://10.34.2.227:5190" + imageUrl;

        Glide.with(loadContext)
                .load(fullUrl)
                .apply(glideOptions)
                .into(holder.ivImage);

        // 添加点击事件
        holder.ivImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(loadContext, ImagePreviewActivity.class);
                intent.putExtra("IMAGE_URL", fullUrl);
                loadContext.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }
}