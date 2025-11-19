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

// --- GEMINI FIX: Import Glide ---
import com.bumptech.glide.Glide;
// --- End of GEMINI FIX ---

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

        // --- GEMINI FIX 1: Set the correct field (username) ---
        holder.tvUsername.setText(fb.username);
        // --- End of GEMINI FIX 1 ---

        holder.tvTitle.setText(fb.dishname);
        holder.tvContent.setText(fb.feedbackText);
        holder.tvCategory.setText(fb.category);
        holder.tvTag.setText(fb.tag);
        holder.ratingBar.setRating(fb.rating);

        // --- GEMINI FIX 2: Use Glide to safely load images ---
        if (fb.imageUri != null && !fb.imageUri.isEmpty()) {
            holder.imgDish.setVisibility(View.VISIBLE);

            // Use Glide to load the image URI
            // Glide can handle 'content://', 'http://', 'file://' etc. safely.
            Glide.with(holder.itemView.getContext())
                    .load(Uri.parse(fb.imageUri))
                    .placeholder(R.drawable.ic_default_user) // Optional: Add a placeholder
                    .error(R.drawable.ic_eyeclose)          // Optional: Add an error image
                    .into(holder.imgDish);

        } else {
            holder.imgDish.setVisibility(View.GONE);
        }
        // --- End of GEMINI FIX 2 ---
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