package com.name.ccf.UI.Second;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.R;

import java.util.List;

public class FeedbackAdapter extends RecyclerView.Adapter<FeedbackAdapter.ViewHolder> {

    private List<Feedback> feedbackList;

    public FeedbackAdapter(List<Feedback> feedbackList) {
        this.feedbackList = feedbackList;
    }

    public void setFeedbackList(List<Feedback> list) {
        this.feedbackList = list;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedbackAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedbackAdapter.ViewHolder holder, int position) {
        Feedback fb = feedbackList.get(position);

        holder.tvUsername.setText(fb.dishname); // 如果有 username 字段可替换
        holder.tvTitle.setText(fb.dishname);
        holder.tvContent.setText(fb.feedbackText);
        holder.tvCategory.setText(fb.category);
        holder.tvTag.setText(fb.tag);
        holder.ratingBar.setRating(fb.rating);

        if (fb.imageUri != null && !fb.imageUri.isEmpty()) {
            holder.imgDish.setVisibility(View.VISIBLE);
            holder.imgDish.setImageURI(Uri.parse(fb.imageUri));
        } else {
            holder.imgDish.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return feedbackList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvTitle, tvContent, tvCategory, tvTag;
        RatingBar ratingBar;
        ImageView imgDish;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvContent = itemView.findViewById(R.id.tv_content);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvTag = itemView.findViewById(R.id.tv_tag);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            imgDish = itemView.findViewById(R.id.img_dish);
        }
    }
}
