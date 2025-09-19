package com.example.shareplatform.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareplatform.R;
import com.example.shareplatform.model.Share;

import java.util.ArrayList;
import java.util.List;



// ShareAdapter.java - 分享列表适配器
public class ShareAdapter extends RecyclerView.Adapter<ShareAdapter.ShareViewHolder> {
    private List<Share> shares;

//更改
private OnShareItemClickListener listener;
    public interface OnShareItemClickListener {
        void onShareItemClick(Share share);
    }
    public void setOnShareItemClickListener(OnShareItemClickListener listener) {
        this.listener = listener;
    }

//更改
    public ShareAdapter(List<Share> shares) {
        this.shares = shares;
    }
    public ShareAdapter() {
        this.shares = new ArrayList<>();
    }
    @Override
    public ShareViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_share, parent, false);
        return new ShareViewHolder(view);
    }


    @Override
    public void onBindViewHolder(ShareViewHolder holder, int position) {
        Share share = shares.get(position);
       holder.bind(share);
        //更改
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareItemClick(share);
            }
        });
        //更改
    }

    @Override
    public int getItemCount() {
        return shares != null ? shares.size() : 0;
    }

    public void setData(List<Share> newShares) {
        this.shares = newShares;
        notifyDataSetChanged();
    }

    public class ShareViewHolder extends RecyclerView.ViewHolder {
        private TextView usernameTv;
        private TextView contentTv;
        private RecyclerView imageRecyclerView;
        private TextView timeTv;

        public ShareViewHolder(View itemView) {
            super(itemView);
            usernameTv = itemView.findViewById(R.id.tv_username);
            contentTv = itemView.findViewById(R.id.tv_content);
            imageRecyclerView = itemView.findViewById(R.id.image_recycler_view);
            timeTv = itemView.findViewById(R.id.tv_time);
        }

        public void bind(Share share) {
            usernameTv.setText(share.getName());
            contentTv.setText(share.getContent());
            timeTv.setText(share.getCreate_time());

            ImageAdapter imageAdapter = new ImageAdapter(share.getImages());
            imageRecyclerView.setLayoutManager(new LinearLayoutManager(
                    itemView.getContext(), LinearLayoutManager.HORIZONTAL, false
            ));
            imageRecyclerView.setAdapter(imageAdapter);
        }
    }
}