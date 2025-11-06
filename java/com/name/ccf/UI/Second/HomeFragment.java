package com.name.ccf.UI.Second;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.R;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private LinearLayout llFeedbackList;
    private Spinner categorySpinner, ratingSpinner;
    private List<Feedback> allFeedback = new ArrayList<>();
    private AppDatabase db;
    private ImageButton btnRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        llFeedbackList = view.findViewById(R.id.ll_feedback_list);
        categorySpinner = view.findViewById(R.id.text_filter);
        ratingSpinner = view.findViewById(R.id.rating_filter);
        btnRefresh = view.findViewById(R.id.btn_refresh);

        db = AppDatabase.getInstance(requireContext());

        setupSpinners(view);
        setupRefreshButton();
        loadFeedback();

        return view;
    }

    private void setupSpinners(View rootView) {
        String[] categories = {"All", "Rice", "Noodle", "Roti", "Snacks"};
        String[] ratings = {"All", "1", "2", "3", "4", "5"};

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        categorySpinner.setAdapter(catAdapter);

        ArrayAdapter<String> rateAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_dropdown_item, ratings);
        ratingSpinner.setAdapter(rateAdapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        categorySpinner.setOnItemSelectedListener(listener);
        ratingSpinner.setOnItemSelectedListener(listener);

        rootView.findViewById(R.id.dropdown_area).setOnClickListener(v -> categorySpinner.performClick());
        rootView.findViewById(R.id.dropdown_rating_area).setOnClickListener(v -> ratingSpinner.performClick());
    }

    private void setupRefreshButton() {
        btnRefresh.setOnClickListener(v -> {
            // 点击按钮时增加小动画
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.3f);
            anim.setDuration(150);
            anim.setRepeatMode(AlphaAnimation.REVERSE);
            anim.setRepeatCount(1);
            btnRefresh.startAnimation(anim);

            // 拉取数据并刷新列表
            loadFeedbackWithFeedback();
        });
    }

    private void loadFeedbackWithFeedback() {
        new Thread(() -> {
            allFeedback = db.feedbackDao().getAllFeedback();
            requireActivity().runOnUiThread(() -> {
                applyFilters();
                // 刷新完成后给用户提示
                Toast.makeText(requireContext(), "Refresh complete", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void loadFeedback() {
        new Thread(() -> {
            allFeedback = db.feedbackDao().getAllFeedback();
            requireActivity().runOnUiThread(this::applyFilters);
        }).start();
    }

    private void applyFilters() {
        String selectedCategory = categorySpinner.getSelectedItem().toString();
        String selectedRating = ratingSpinner.getSelectedItem().toString();

        llFeedbackList.removeAllViews();

        for (Feedback fb : allFeedback) {
            boolean matchCategory = selectedCategory.equals("All") || fb.category.equalsIgnoreCase(selectedCategory);
            boolean matchRating = selectedRating.equals("All") || fb.rating == Float.parseFloat(selectedRating);

            if (matchCategory && matchRating) {
                View card = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_feedback, llFeedbackList, false);

                TextView tvUsername = card.findViewById(R.id.tv_username);
                TextView tvTitle = card.findViewById(R.id.tv_title);
                TextView tvContent = card.findViewById(R.id.tv_content);
                TextView tvCategory = card.findViewById(R.id.tv_category);
                TextView tvTag = card.findViewById(R.id.tv_tag);
                RatingBar ratingBar = card.findViewById(R.id.rating_bar);
                ImageView imgDish = card.findViewById(R.id.img_dish);

                tvUsername.setText(fb.username);
                tvTitle.setText(fb.dishname);
                tvContent.setText(fb.feedbackText);
                tvCategory.setText(fb.category);
                tvTag.setText(fb.tag);
                ratingBar.setRating(fb.rating);

                if (fb.imageUri != null && !fb.imageUri.isEmpty()) {
                    imgDish.setVisibility(View.VISIBLE);
                    imgDish.setImageURI(android.net.Uri.parse(fb.imageUri));
                }
                else {
                    imgDish.setVisibility(View.GONE);
                }

                llFeedbackList.addView(card);
            }
        }
    }
}
