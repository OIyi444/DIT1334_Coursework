package com.name.ccf.UI.Second;

// (All your original imports remain unchanged)
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
import android.widget.Button;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.Data.Repository.FeedbackRepository;
import com.name.ccf.R;
import com.name.ccf.UI.Main.MainActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/*
 * (Original comments about guest restrictions, etc.)
 */
public class HomeFragment extends Fragment {

    // (All your variables remain unchanged)
    private LinearLayout llFeedbackList;
    private Spinner spinnerCategory;
    private Spinner spinnerSortBy;
    private SearchView searchView;
    private List<Feedback> allFeedback = new ArrayList<>();
    private AppDatabase db;
    private ImageButton btnRefresh;
    private TextView tvUserStatus;

    private String userType = "guest";

    private final Map<String, String> categoryTranslationMap = new HashMap<>();
    private final Map<String, String> tagTranslationMap = new HashMap<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        llFeedbackList = view.findViewById(R.id.ll_feedback_list);
        searchView = view.findViewById(R.id.search_view);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerSortBy = view.findViewById(R.id.spinner_sort_by);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        tvUserStatus = view.findViewById(R.id.tv_user_status);
        db = AppDatabase.getInstance(requireContext());
        loadUserDataFromActivity();

        setupSpinners(view);
        setupSearchView();
        setupRefreshButton();
        setupTranslationMaps();
        loadFeedbackFromLocalDb();
    }

    // (loadUserDataFromActivity method remains unchanged)
    private void loadUserDataFromActivity() {
        if (getActivity() instanceof SecondaryActivity) {
            SecondaryActivity activity = (SecondaryActivity) getActivity();
            String username = activity.getUsername();
            userType = activity.getUserType();
            if (username == null || username.isEmpty()) username = "Guest";
            if (userType == null || userType.isEmpty()) userType = "guest";
        }
        else {
            userType = "guest";
        }
    }

    // --- ⬇️ GEMINI FIX: MODIFIED setupSpinners ⬇️ ---
    private void setupSpinners(View rootView) {
        String[] categories = {
                getString(R.string.cat_all), getString(R.string.cat_rice_noodles),
                getString(R.string.cat_western), getString(R.string.cat_malay),
                getString(R.string.cat_chinese), getString(R.string.cat_indian),
                getString(R.string.cat_snacks), getString(R.string.cat_desserts),
                getString(R.string.cat_beverages), getString(R.string.cat_vegetarian),
                getString(R.string.cat_others)
        };
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item_selected, categories);
        catAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCategory.setAdapter(catAdapter);
        String[] sortOptions = {
                getString(R.string.sort_date_newest), getString(R.string.sort_rating_highest),
                getString(R.string.sort_rating_lowest), getString(R.string.filter_3_days),
                getString(R.string.filter_7_days), getString(R.string.filter_1_month)
        };
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(
                requireContext(), R.layout.spinner_item_selected, sortOptions);
        sortAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerSortBy.setAdapter(sortAdapter);

        if (userType.equals("guest")) {
            spinnerCategory.setEnabled(false);
            spinnerSortBy.setEnabled(false);

            // --- ⚠️ 已修正 (Was R.id.category_filter_row) ---
            rootView.findViewById(R.id.dropdown_area_category).setOnClickListener(v -> showLoginRegisterDialog());
            rootView.findViewById(R.id.dropdown_area_sort).setOnClickListener(v -> showLoginRegisterDialog());
        }
        else {
            AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    applyFilters();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };
            spinnerCategory.setOnItemSelectedListener(listener);
            spinnerSortBy.setOnItemSelectedListener(listener);

            // --- ⚠️ 已修正 (Was R.id.category_filter_row) ---
            rootView.findViewById(R.id.dropdown_area_category).setOnClickListener(v -> spinnerCategory.performClick());
            rootView.findViewById(R.id.dropdown_area_sort).setOnClickListener(v -> spinnerSortBy.performClick());
        }
    }
    // --- ⬆️ END OF FIX ⬆️ ---

    // (setupTranslationMaps method remains unchanged)
    private void setupTranslationMaps() {
        categoryTranslationMap.put("All Categories", getString(R.string.cat_all));
        categoryTranslationMap.put("Rice / Noodles", getString(R.string.cat_rice_noodles));
        categoryTranslationMap.put("Western", getString(R.string.cat_western));
        categoryTranslationMap.put("Malay", getString(R.string.cat_malay));
        categoryTranslationMap.put("Chinese", getString(R.string.cat_chinese));
        categoryTranslationMap.put("Indian / Mamak", getString(R.string.cat_indian));
        categoryTranslationMap.put("Snacks / Bakery", getString(R.string.cat_snacks));
        categoryTranslationMap.put("Desserts", getString(R.string.cat_desserts));
        categoryTranslationMap.put("Beverages", getString(R.string.cat_beverages));
        categoryTranslationMap.put("Vegetarian", getString(R.string.cat_vegetarian));
        categoryTranslationMap.put("Others", getString(R.string.cat_others));
        tagTranslationMap.put("All Tags", getString(R.string.tag_all));
        tagTranslationMap.put("General", getString(R.string.tag_general));
        tagTranslationMap.put("Healthy", getString(R.string.tag_healthy));
        tagTranslationMap.put("Spicy", getString(R.string.tag_spicy));
        tagTranslationMap.put("Oily", getString(R.string.tag_oily));
        tagTranslationMap.put("Salty", getString(R.string.tag_salty));
        tagTranslationMap.put("Sweet", getString(R.string.tag_sweet));
        tagTranslationMap.put("Crispy", getString(R.string.tag_crispy));
        tagTranslationMap.put("Soup", getString(R.string.tag_soup));
        tagTranslationMap.put("Grilled", getString(R.string.tag_grilled));
        tagTranslationMap.put("Fried", getString(R.string.tag_fried));
        tagTranslationMap.put("Creamy", getString(R.string.tag_creamy));
        tagTranslationMap.put("Rendang", getString(R.string.tag_rendang));
        tagTranslationMap.put("Stir-fried", getString(R.string.tag_stir_fried));
        tagTranslationMap.put("Steamed", getString(R.string.tag_steamed));
        tagTranslationMap.put("Curry", getString(R.string.tag_curry));
        tagTranslationMap.put("Roti", getString(R.string.tag_roti));
        tagTranslationMap.put("Baked", getString(R.string.tag_baked));
        tagTranslationMap.put("Cold", getString(R.string.tag_cold));
        tagTranslationMap.put("Hot", getString(R.string.tag_hot));
        tagTranslationMap.put("Icy", getString(R.string.tag_icy));
        tagTranslationMap.put("Refreshing", getString(R.string.tag_refreshing));
        tagTranslationMap.put("Light", getString(R.string.tag_light));
    }

    // --- ⬇️ GEMINI FIX: MODIFIED setupSearchView ⬇️ ---
    private void setupSearchView() {
        if (userType.equals("guest")) {
            searchView.setOnSearchClickListener(v -> {
                // (Changed to call the new dialog)
                showLoginRegisterDialog();
            });
            searchView.setFocusable(false);
            searchView.setFocusableInTouchMode(false);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) { return true; }
                @Override public boolean onQueryTextChange(String newText) { return true; }
            });
        }
        else {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    applyFilters();
                    return false;
                }
                @Override
                public boolean onQueryTextChange(String newText) {
                    applyFilters();
                    return true;
                }
            });
            searchView.setFocusable(true);
            searchView.setFocusableInTouchMode(true);
            searchView.setOnSearchClickListener(null);
        }
    }
    // --- ⬆️ END OF FIX ⬆️ ---

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
                        applyFilters();
                        Toast.makeText(requireContext(), "Refresh complete", Toast.LENGTH_SHORT).show();
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
                getActivity().runOnUiThread(this::applyFilters);
            }
        }).start();
    }

    // --- ⬇️ GEMINI FIX: MODIFIED applyFilters ⬇️ ---
    private void applyFilters() {
        String searchQuery = searchView.getQuery().toString().toLowerCase().trim();
        String selectedCategory = getString(R.string.cat_all);
        if (spinnerCategory.getSelectedItem() != null) {
            selectedCategory = spinnerCategory.getSelectedItem().toString();
        }
        String selectedSortOrFilter = getString(R.string.sort_date_newest);
        if (spinnerSortBy.getSelectedItem() != null) {
            selectedSortOrFilter = spinnerSortBy.getSelectedItem().toString();
        }
        List<Feedback> filteredList = new ArrayList<>();
        long now = System.currentTimeMillis();
        long timeCutoff = 0;
        if (selectedSortOrFilter.equals(getString(R.string.filter_3_days))) {
            timeCutoff = now - TimeUnit.DAYS.toMillis(3);
        } else if (selectedSortOrFilter.equals(getString(R.string.filter_7_days))) {
            timeCutoff = now - TimeUnit.DAYS.toMillis(7);
        } else if (selectedSortOrFilter.equals(getString(R.string.filter_1_month))) {
            timeCutoff = now - TimeUnit.DAYS.toMillis(30);
        }
        for (Feedback fb : allFeedback) {
            boolean matchSearch = searchQuery.isEmpty() ||
                    (fb.dishname != null && fb.dishname.toLowerCase().contains(searchQuery)) ||
                    (fb.category != null && fb.category.toLowerCase().contains(searchQuery));
            boolean matchCategory = selectedCategory.equals(getString(R.string.cat_all)) ||
                    (fb.category != null && categoryTranslationMap.getOrDefault(fb.category, fb.category).equals(selectedCategory));
            boolean matchDateFilter = true;
            if (timeCutoff > 0) {
                matchDateFilter = fb.timestamp >= timeCutoff;
            }
            if (matchSearch && matchCategory && matchDateFilter) {
                filteredList.add(fb);
            }
        }
        if (selectedSortOrFilter.equals(getString(R.string.sort_rating_highest))) {
            Collections.sort(filteredList, (fb1, fb2) -> Float.compare(fb2.rating, fb1.rating));
        } else if (selectedSortOrFilter.equals(getString(R.string.sort_rating_lowest))) {
            Collections.sort(filteredList, (fb1, fb2) -> Float.compare(fb1.rating, fb2.rating));
        } else {
            Collections.sort(filteredList, (fb1, fb2) -> Long.compare(fb2.timestamp, fb1.timestamp));
        }
        llFeedbackList.removeAllViews();
        boolean isGuest = userType.equals("guest");
        final int GUEST_LIMIT = 3;
        int itemsAdded = 0;
        for (Feedback fb : filteredList) {
            if (isGuest && itemsAdded >= GUEST_LIMIT) {
                break;
            }
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
            String dbCategoryKey = fb.category;
            String dbTagKey = fb.tag;
            String translatedCategory = categoryTranslationMap.getOrDefault(dbCategoryKey, dbCategoryKey);
            String translatedTag = tagTranslationMap.getOrDefault(dbTagKey, dbTagKey);
            tvCategory.setText(translatedCategory);
            tvTag.setText(translatedTag);
            ratingBar.setRating(fb.rating);
            if (fb.imageUri != null && !fb.imageUri.isEmpty()) {
                imgDish.setVisibility(View.VISIBLE);
                Glide.with(requireContext())
                        .load(android.net.Uri.parse(fb.imageUri))
                        .into(imgDish);
            }
            else {
                imgDish.setVisibility(View.GONE);
            }
            llFeedbackList.addView(card);
            itemsAdded++;
        }
        if (isGuest && filteredList.size() > GUEST_LIMIT) {
            if (getContext() == null || getActivity() == null) return;
            View loginPrompt = LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_login_prompt, llFeedbackList, false);
            Button btnLogin = loginPrompt.findViewById(R.id.btn_login_prompt);

            // --- (The click listener now calls the new dialog) ---
            btnLogin.setOnClickListener(v -> {
                showLoginRegisterDialog(); // Use the new dialog
            });

            llFeedbackList.addView(loginPrompt);
        }
    }
    // --- ⬆️ END OF FIX ⬆️ ---


    // --- ⬇️ GEMINI FIX: This method is REPLACED ⬇️ ---
    // (The old showLoginPromptDialog is removed)
    /**
     * Displays a dialog using the layout you provided (assumed to be 'design_dialog_options.xml').
     * It re-uses 'button_avatar' for Login and 'button_name' for Register.
     * It hides the other buttons as requested.
     */
    private void showLoginRegisterDialog() {
        if (getContext() == null || getActivity() == null) return;

        // 1. Inflate the layout you provided
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        // !!! ATTENTION !!!
        // I am ASSUMING the file is named 'design_dialog_select.xml' based on your XML.
        // If your file has a different name, change it here.
        View view = inflater.inflate(R.layout.design_dialog_select, null);

        // 2. Find views based on the IDs from your XML
        TextView dialogTitle = view.findViewById(R.id.profile_option_title);
        MaterialButton btnLogin = view.findViewById(R.id.button_avatar); // Reusing 'button_avatar'
        MaterialButton btnRegister = view.findViewById(R.id.button_name); // Reusing 'button_name'
        MaterialButton btnId = view.findViewById(R.id.button_id);
        MaterialButton btnCancel = view.findViewById(R.id.button_cancel);

        // 3. Set content
        dialogTitle.setText(R.string.guest_title); // "Login or Register"
        btnLogin.setText(R.string.login_title);     // "Login"
        btnRegister.setText(R.string.register_title); // "Register"

        // 4. Hide the bottom two buttons (as requested)
        btnId.setVisibility(View.GONE);
        btnCancel.setVisibility(View.GONE);

        // 5. Create dialog (cancellable as requested)
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(true) // Allow back button dismiss
                .create();

        // 6. Set click listeners
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

        // 7. Show
        dialog.show();
    }
    // --- ⬆️ (END OF FIX) ⬆️ ---
}