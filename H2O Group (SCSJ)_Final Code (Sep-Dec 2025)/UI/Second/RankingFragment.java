package com.name.ccf.UI.Second;

// (All your original imports remain unchanged)
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
// (Imports remain unchanged)
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout; // (Import is necessary)
import com.google.android.material.button.MaterialButton;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.Data.Repository.FeedbackRepository;
import com.name.ccf.R;
import com.name.ccf.UI.Main.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/*
 * (Original comments about guest restrictions, etc.)
 */
public class RankingFragment extends Fragment {

    // (Your original variables)
    private LinearLayout rankingList;
    private ImageButton btnRefresh;
    private TextView textCategory, textTag;

    // --- ⬇️ GEMINI FIX: Correctly referencing XML IDs ---
    private ConstraintLayout rootLayout; // This is 'main_content'
    private View scrollView; // This is 'scrollView_ranking'
    private View refreshGroup; // This is 'refresh_group'
    // --- ⬆️ END OF FIX ---

    private AppDatabase db;
    private List<Feedback> allFeedback = new ArrayList<>();

    // (R.string loading variables)
    private String[] categories;
    private String[] tags;

    private String userType = "guest";
    private LinearLayout filterSection;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ranking, container, false);
    }

    // --- ⬇️ GEMINI FIX: MODIFIED onViewCreated ⬇️ ---
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // (Finding all views based on your provided XML)
        rankingList = view.findViewById(R.id.ranking_list);
        btnRefresh = view.findViewById(R.id.btn_refresh_ranking);
        textCategory = view.findViewById(R.id.text_category);
        textTag = view.findViewById(R.id.text_tag);
        filterSection = view.findViewById(R.id.filter_row);
        refreshGroup = view.findViewById(R.id.refresh_group); // Found the refresh group

        // (Using the correct IDs from your XML)
        rootLayout = view.findViewById(R.id.main_content);
        scrollView = view.findViewById(R.id.scrollView_ranking);

        db = AppDatabase.getInstance(requireContext());

        loadUserDataFromActivity();
        setupSpinners(view);
        textCategory.setText(categories[0]);
        textTag.setText(tags[0]);
        setupRefreshButton();

        if (userType.equals("guest")) {
            applyFiltersAndRanking(); // This will now correctly show the prompt
        } else {
            loadFeedbackFromLocalDb();
        }
    }
    // --- ⬆️ END OF FIX ⬆️ ---

    // (loadUserDataFromActivity method remains unchanged)
    private void loadUserDataFromActivity() {
        if (getActivity() instanceof SecondaryActivity) {
            SecondaryActivity activity = (SecondaryActivity) getActivity();
            userType = activity.getUserType();
            if (userType == null || userType.isEmpty()) userType = "guest";
        } else {
            userType = "guest";
        }
    }

    // (setupSpinners method remains unchanged)
    // (It already correctly hides the filter row and refresh button for guests)
    private void setupSpinners(View rootView) {
        categories = new String[] {
                getString(R.string.cat_all),
                getString(R.string.cat_rice_noodles),
                getString(R.string.cat_western),
                getString(R.string.cat_malay),
                getString(R.string.cat_chinese),
                getString(R.string.cat_indian),
                getString(R.string.cat_snacks),
                getString(R.string.cat_desserts),
                getString(R.string.cat_beverages),
                getString(R.string.cat_vegetarian),
                getString(R.string.cat_others)
        };
        tags = new String[] {
                getString(R.string.tag_all),
                getString(R.string.tag_healthy),
                getString(R.string.tag_spicy),
                getString(R.string.tag_oily),
                getString(R.string.tag_salty),
                getString(R.string.tag_sweet),
                getString(R.string.tag_crispy)
        };

        LinearLayout categoryArea = rootView.findViewById(R.id.dropdown_category_area);
        LinearLayout tagArea = rootView.findViewById(R.id.dropdown_tag_area);

        categoryArea.setOnClickListener(v -> {
            if (getContext() == null) return;
            PopupMenu popup = new PopupMenu(requireContext(), categoryArea);
            for (String category : categories) {
                popup.getMenu().add(category);
            }
            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();
                textCategory.setText(selected);
                applyFiltersAndRanking();
                return true;
            });
            popup.show();
        });

        tagArea.setOnClickListener(v -> {
            if (getContext() == null) return;
            PopupMenu popup = new PopupMenu(requireContext(), tagArea);
            for (String tag : tags) {
                popup.getMenu().add(tag);
            }
            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();
                textTag.setText(selected);
                applyFiltersAndRanking();
                return true;
            });
            popup.show();
        });

        if (userType.equals("guest")) {
            if (filterSection != null) {
                filterSection.setVisibility(View.GONE);
            }
            btnRefresh.setVisibility(View.GONE);
            // (Also hide the 'refresh' text)
            if (refreshGroup != null) {
                refreshGroup.setVisibility(View.GONE);
            }
        }
    }

    // (setupRefreshButton method remains unchanged)
    private void setupRefreshButton() {
        btnRefresh.setOnClickListener(v -> {
            AlphaAnimation anim = new AlphaAnimation(1.0f, 0.3f);
            anim.setDuration(150);
            anim.setRepeatMode(AlphaAnimation.REVERSE);
            anim.setRepeatCount(1);
            btnRefresh.startAnimation(anim);
            refreshDataFromFirebase();
        });
    }

    // (refreshDataFromFirebase method remains unchanged)
    private void refreshDataFromFirebase() {
        if (userType.equals("guest") || getContext() == null) {
            return;
        }
        FeedbackRepository repo = new FeedbackRepository();
        btnRefresh.setEnabled(false);
        Toast.makeText(requireContext(), "Refreshing...", Toast.LENGTH_SHORT).show();

        repo.fetchAllFeedback(firebaseFeedbackList -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                if (db == null) db = AppDatabase.getInstance(requireContext());
                db.feedbackDao().deleteAll();
                db.feedbackDao().insertAll(firebaseFeedbackList);
                allFeedback = firebaseFeedbackList;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        applyFiltersAndRanking();
                        Toast.makeText(getContext(), "Refresh complete", Toast.LENGTH_SHORT).show();
                        btnRefresh.setEnabled(true);
                    });
                } else {
                    btnRefresh.setEnabled(true);
                }
            });
        });
    }


    // (loadFeedbackFromLocalDb method remains unchanged)
    private void loadFeedbackFromLocalDb() {
        new Thread(() -> {
            if (db == null) db = AppDatabase.getInstance(requireContext());
            allFeedback = db.feedbackDao().getAllFeedback();
            if (getActivity() != null) {
                getActivity().runOnUiThread(this::applyFiltersAndRanking);
            }
        }).start();
    }


    // --- ⬇️ GEMINI FIX: MODIFIED applyFiltersAndRanking ⬇️ ---
    private void applyFiltersAndRanking() {

        // --- (This is the new Guest Logic block, using correct IDs) ---
        if (userType.equals("guest")) {
            if (getContext() == null || getActivity() == null || rootLayout == null) return;

            // 1. Hide the ranking list's ScrollView (not just the list)
            if (scrollView != null) {
                scrollView.setVisibility(View.GONE);
            }

            // (The filter/refresh rows are already hidden by setupSpinners)

            // 2. Inflate the prompt (using rootLayout)
            View loginPrompt = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_login_prompt, rootLayout, false);

            // 3. Center the prompt in the ConstraintLayout ('main_content')
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            params.setMargins(16,16,16,16);
            loginPrompt.setLayoutParams(params);

            // 4. Set the button to call the dialog
            Button btnLogin = loginPrompt.findViewById(R.id.btn_login_prompt);
            btnLogin.setOnClickListener(v -> {
                showLoginRegisterDialog(); // Call the options dialog
            });

            // 5. Add the prompt to the root layout
            rootLayout.addView(loginPrompt);
            return; // Stop here for guests
        }
        // --- (End of new Guest Logic) ---

        // (This is your original logic for logged-in users, unchanged)

        // Ensure list is visible (in case they logged in and came back)
        if (scrollView != null) {
            scrollView.setVisibility(View.VISIBLE);
        }
        rankingList.removeAllViews(); // Clear previous rankings

        String selectedCategory = textCategory.getText().toString();
        String selectedTag = textTag.getText().toString();

        List<Feedback> filtered = new ArrayList<>();
        for (Feedback fb : allFeedback) {
            boolean matchCategory = selectedCategory.equals(getString(R.string.cat_all)) || (fb.category != null && fb.category.equalsIgnoreCase(selectedCategory));
            boolean matchTag = selectedTag.equals(getString(R.string.tag_all)) || (fb.tag != null && fb.tag.equalsIgnoreCase(selectedTag));
            if (matchCategory && matchTag) filtered.add(fb);
        }

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

        int rank = 1;
        for (DishAvg d : dishAvgList) {
            View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_ranking, rankingList, false);
            TextView tvRank = item.findViewById(R.id.rank);
            TextView tvDish = item.findViewById(R.id.dish_name);
            TextView tvAvg = item.findViewById(R.id.avg_rating);
            RatingBar ratingBar = item.findViewById(R.id.rating_bar);
            tvRank.setText("#" + rank);
            tvDish.setText(d.dishName);
            tvAvg.setText(getString(R.string.ranking_review_count, d.avgRating, d.count));
            ratingBar.setRating(d.avgRating);
            rankingList.addView(item);
            rank++;
        }
    }
    // --- ⬆️ END OF FIX ⬆️ ---

    // (Your inner DishAvg class remains unchanged)
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

    // (The showLoginRegisterDialog method remains unchanged)
    private void showLoginRegisterDialog() {
        if (getContext() == null || getActivity() == null) return;
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View view = inflater.inflate(R.layout.design_dialog_select, null);
        TextView dialogTitle = view.findViewById(R.id.profile_option_title);
        MaterialButton btnLogin = view.findViewById(R.id.button_avatar);
        MaterialButton btnRegister = view.findViewById(R.id.button_name);
        MaterialButton btnId = view.findViewById(R.id.button_id);
        MaterialButton btnCancel = view.findViewById(R.id.button_cancel);
        dialogTitle.setText(R.string.guest_title);
        btnLogin.setText(R.string.login_title);
        btnRegister.setText(R.string.register_title);
        btnId.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(true)
                .create();
        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.putExtra("openFragment", "login");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            dialog.dismiss();
        });
        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MainActivity.class);
            intent.putExtra("openFragment", "register");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            dialog.dismiss();
        });
        dialog.show();
    }
}