package com.example.shareplatform.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shareplatform.R;
import com.example.shareplatform.model.response.CommentModel;

import java.util.List;


// 2. 评论适配器（将数据与子项布局绑定）
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<CommentModel> commentList; // 存储所有评论的列表

    // 对外提供方法：设置评论数据（可动态更新）
    public void setCommentList(List<CommentModel> commentList) {
        this.commentList = commentList;
        notifyDataSetChanged(); // 数据变化后，刷新列表
    }

    // 3. 创建ViewHolder（绑定子项布局的控件）
    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvCommentUsername; // 评论者用户名
        TextView tvCommentContent;  // 评论文字
        TextView tvCommentTime;     // 评论时间

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);

            // 绑定子项布局的控件ID
            tvCommentUsername = itemView.findViewById(R.id.tv_comment_username);
            tvCommentContent = itemView.findViewById(R.id.tv_comment_content);
            tvCommentTime = itemView.findViewById(R.id.tv_comment_time);
        }
    }

    // 4. 加载子项布局到ViewHolder
    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_item, parent, false); // 加载单个评论布局
        return new CommentViewHolder(view);
    }

    // 5. 将数据绑定到子项控件（每条评论执行一次）
    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentModel comment = commentList.get(position); // 获取当前位置的评论数据
        // 给控件设置数据
        holder.tvCommentUsername.setText(comment.getUsername());
        holder.tvCommentContent.setText(comment.getContent());
        holder.tvCommentTime.setText(comment.getTime());
    }

    // 6. 返回评论总数（RecyclerView据此创建对应数量的子项）
    @Override
    public int getItemCount() {
        return commentList == null ? 0 : commentList.size();
    }
}