package com.name.ccf.UI.Second;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingFragment extends Fragment {

    private LinearLayout rankingList;
    private ImageButton btnRefresh;
    private TextView textCategory, textTag;
    private Spinner spinnerCategory, spinnerTag;

    private AppDatabase db;
    private List<Feedback> allFeedback = new ArrayList<>();

    private final String[] categories = {"All", "Rice", "Noodle", "Roti", "Snacks"};
    private final String[] tags = {"All", "Spicy", "Sweet", "Salty"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_ranking, container, false);

        rankingList = view.findViewById(R.id.ranking_list);
        btnRefresh = view.findViewById(R.id.btn_refresh_ranking);
        textCategory = view.findViewById(R.id.text_category);
        textTag = view.findViewById(R.id.text_tag);

        db = AppDatabase.getInstance(requireContext());

        setupSpinners(view);  // 修正这里传入 view
        setupRefreshButton();
        loadFeedback();

        return view;
    }

    // 设置 Spinner
    private void setupSpinners(View rootView) {
        spinnerCategory = new Spinner(requireContext());
        spinnerTag = new Spinner(requireContext());

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, tags);
        spinnerTag.setAdapter(tagAdapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                textCategory.setText(categories[position]);
                applyFiltersAndRanking();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerTag.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                textTag.setText(tags[position]);
                applyFiltersAndRanking();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // 点击 TextView + ImageView 区域弹出 Spinner
        rootView.findViewById(R.id.dropdown_category_area).setOnClickListener(v -> spinnerCategory.performClick());
        rootView.findViewById(R.id.dropdown_tag_area).setOnClickListener(v -> spinnerTag.performClick());
    }

    private void setupRefreshButton() {
        btnRefresh.setOnClickListener(v -> {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.3f);
            anim.setDuration(150);
            anim.setRepeatMode(AlphaAnimation.REVERSE);
            anim.setRepeatCount(1);
            btnRefresh.startAnimation(anim);

            loadFeedbackWithFeedback();
        });
    }

    private void loadFeedback() {
        new Thread(() -> {
            allFeedback = db.feedbackDao().getAllFeedback();
            requireActivity().runOnUiThread(this::applyFiltersAndRanking);
        }).start();
    }

    private void loadFeedbackWithFeedback() {
        new Thread(() -> {
            allFeedback = db.feedbackDao().getAllFeedback();
            requireActivity().runOnUiThread(() -> {
                applyFiltersAndRanking();
                Toast.makeText(requireContext(), "Refresh complete", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void applyFiltersAndRanking() {
        String selectedCategory = textCategory.getText().toString();
        String selectedTag = textTag.getText().toString();

        // 筛选
        List<Feedback> filtered = new ArrayList<>();
        for (Feedback fb : allFeedback) {
            boolean matchCategory = selectedCategory.equals("All") || fb.category.equalsIgnoreCase(selectedCategory);
            boolean matchTag = selectedTag.equals("All") || fb.tag.equalsIgnoreCase(selectedTag);
            if (matchCategory && matchTag) filtered.add(fb);
        }

        // 计算平均分
        Map<String, List<Float>> dishRatings = new HashMap<>();
        for (Feedback fb : filtered) {
            dishRatings.computeIfAbsent(fb.dishname, k -> new ArrayList<>()).add(fb.rating);
        }

        List<DishAvg> dishAvgList = new ArrayList<>();
        for (Map.Entry<String, List<Float>> entry : dishRatings.entrySet()) {
            String dish = entry.getKey();
            List<Float> ratings = entry.getValue();
            float sum = 0;
            for (float r : ratings) sum += r;
            float avg = sum / ratings.size();
            dishAvgList.add(new DishAvg(dish, avg, ratings.size()));
        }

        dishAvgList.sort((d1, d2) -> Float.compare(d2.avgRating, d1.avgRating));

        rankingList.removeAllViews();
        int rank = 1;
        for (DishAvg d : dishAvgList) {
            View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_ranking, rankingList, false);

            TextView tvRank = item.findViewById(R.id.rank);
            TextView tvDish = item.findViewById(R.id.dish_name);
            TextView tvAvg = item.findViewById(R.id.avg_rating);
            RatingBar ratingBar = item.findViewById(R.id.rating_bar);

            tvRank.setText("#" + rank);
            tvDish.setText(d.dishName);
            tvAvg.setText(String.format("%.1f (%d reviews)", d.avgRating, d.count));
            ratingBar.setRating(d.avgRating);

            rankingList.addView(item);
            rank++;
        }
    }

    private static class DishAvg {
        String dishName;
        float avgRating;
        int count;

        DishAvg(String dishName, float avgRating, int count) {
            this.dishName = dishName;
            this.avgRating = avgRating;
            this.count = count;
        }
    }
}
