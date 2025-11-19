package com.name.ccf.UI.Second;

// (All your original imports remain unchanged)
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.Data.Repository.FeedbackRepository;
import com.name.ccf.R;
import com.name.ccf.UI.Main.MainActivity;
import com.name.ccf.UI.Second.HomeFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/*
 * (Original comments about Guest Restrictions, etc.)
 */
public class FeedbackFragment extends Fragment {

    private FeedbackRepository feedbackRepository;
    // (All variables declarations remain unchanged)
    private ImageView imgDishPhoto;
    private TextView textUploadHint;
    private Spinner spinnerCategory, spinnerTag;
    private RatingBar ratingBar;
    private EditText etDishName, etFeedbackText;
    private Button btnSubmit;
    private ImageButton btnMic;

    private ConstraintLayout rootLayout;
    private ScrollView scrollView;

    private Uri dishPhotoUri = null; // This is the 'content://' URI

    // (All launchers remain unchanged)
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;

    private AppDatabase db;
    private final Map<String, Integer> categoryMap = new LinkedHashMap<>();
    private final Map<String, Integer> tagMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Integer>> categoryToTagMap = new HashMap<>();

    private String username = "Guest";
    private String userType = "guest";


    // --- ⬇️ GEMINI FIX: Added profanity blocklist ---
    // (This is a BASIC filter. A real multi-language filter is very complex)
    // (This is a basic filter. A real multi-language filter is very complex)
    private static final String[] PROFANITY_BLOCKLIST = {
            // English
            "fuck", "shit", "bitch", "cunt", "asshole", "piss", "dick", "pussy",
            // Malay
            "puki", "pukimak", "babi", "sial", "bodoh", "gampang", "pantat",
            // Chinese (Hokkien/Cantonese/Mandarin Slang)
            "cibai", "kanina", "lanjiao", "cb", "knn", "ccb", "nabei", "fuck",
            // Other Slang
            "wtf", "lmfao", "stfu"
            // Add any other words you want to block
    };

    /**
     * [NEW] Checks if a text contains any blocked words.
     * This is a simple 'contains' check and can be bypassed (e.g., "f*ck").
     */
    private boolean containsProfanity(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lowerCaseText = text.toLowerCase().replace(" ", ""); // Remove spaces
        for (String blockedWord : PROFANITY_BLOCKLIST) {
            if (lowerCaseText.contains(blockedWord)) {
                return true; // Found a bad word
            }
        }
        return false; // Text is clean
    }
    // --- ⬆️ END OF FIX ---


    // (onCreate method remains unchanged)
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launcher for Runtime Permission
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        imagePickerLauncher.launch("image/*");
                    } else {
                        Toast.makeText(getContext(), "Permission denied. Cannot pick image.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_feedback, container, false);

        // (All findViewById calls remain unchanged from your provided code)
        imgDishPhoto = root.findViewById(R.id.img_dish_photo);
        textUploadHint = root.findViewById(R.id.text_upload_hint);
        spinnerCategory = root.findViewById(R.id.spinner_category);
        spinnerTag = root.findViewById(R.id.spinner_tag);
        ratingBar = root.findViewById(R.id.ratingBar);
        etDishName = root.findViewById(R.id.et_dish_name);
        etFeedbackText = root.findViewById(R.id.et_feedback_text);
        btnSubmit = root.findViewById(R.id.btn_submit);
        btnMic = root.findViewById(R.id.btn_mic);
        feedbackRepository = new FeedbackRepository();
        rootLayout = root.findViewById(R.id.feedback_page);
        scrollView = root.findViewById(R.id.scrollView);
        db = AppDatabase.getInstance(requireContext());

        if (getActivity() instanceof SecondaryActivity) {
            SecondaryActivity activity = (SecondaryActivity) getActivity();
            username = activity.getUsername();
            userType = activity.getUserType();
        }

        // (Guest logic remains unchanged from your provided code)
        if ("guest".equals(userType)) {
            scrollView.setVisibility(View.GONE);
            View loginPrompt = inflater.inflate(R.layout.item_login_prompt, rootLayout, false);
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
            Button btnLogin = loginPrompt.findViewById(R.id.btn_login_prompt);
            btnLogin.setOnClickListener(v -> showLoginRegisterDialog());
            rootLayout.addView(loginPrompt);
        }
        else {
            // (Logged-in user logic)
            btnSubmit.setOnClickListener(v -> handleSubmit());

            imagePickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            dishPhotoUri = uri;
                            Glide.with(this).load(uri).into(imgDishPhoto);
                            textUploadHint.setVisibility(View.GONE);
                        }
                    });

            imgDishPhoto.setOnClickListener(v -> {
                checkAndRequestPermission();
            });

            speechLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                            ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            if (matches != null && !matches.isEmpty()) {
                                etFeedbackText.setText(matches.get(0));
                            }
                        }
                    });
            btnMic.setOnClickListener(v -> startVoiceInput());

            setupCategoryTagData();
            setupCategorySpinner();
        }

        return root;
    }


    // (checkAndRequestPermission method remains unchanged)
    private void checkAndRequestPermission() {
        if (getContext() == null) return;
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*");
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }


    // (setupCategoryTagData... etc. all remain unchanged)
    private void setupCategoryTagData() {
        categoryMap.clear();
        categoryMap.put("All Categories", R.string.cat_all);
        categoryMap.put("Rice / Noodles", R.string.cat_rice_noodles);
        categoryMap.put("Western", R.string.cat_western);
        categoryMap.put("Malay", R.string.cat_malay);
        categoryMap.put("Chinese", R.string.cat_chinese);
        categoryMap.put("Indian / Mamak", R.string.cat_indian);
        categoryMap.put("Snacks / Bakery", R.string.cat_snacks);
        categoryMap.put("Desserts", R.string.cat_desserts);
        categoryMap.put("Beverages", R.string.cat_beverages);
        categoryMap.put("Vegetarian", R.string.cat_vegetarian);
        categoryMap.put("Others", R.string.cat_others);
        tagMap.clear();
        tagMap.put("General", R.string.tag_general);
        tagMap.put("Healthy", R.string.tag_healthy);
        tagMap.put("Spicy", R.string.tag_spicy);
        tagMap.put("Oily", R.string.tag_oily);
        tagMap.put("Salty", R.string.tag_salty);
        tagMap.put("Soup", R.string.tag_soup);
        tagMap.put("Grilled", R.string.tag_grilled);
        tagMap.put("Fried", R.string.tag_fried);
        tagMap.put("Creamy", R.string.tag_creamy);
        tagMap.put("Sweet", R.string.tag_sweet);
        tagMap.put("Rendang", R.string.tag_rendang);
        tagMap.put("Stir-fried", R.string.tag_stir_fried);
        tagMap.put("Steamed", R.string.tag_steamed);
        tagMap.put("Curry", R.string.tag_curry);
        tagMap.put("Roti", R.string.tag_roti);
        tagMap.put("Crispy", R.string.tag_crispy);
        tagMap.put("Baked", R.string.tag_baked);
        tagMap.put("Cold", R.string.tag_cold);
        tagMap.put("Hot", R.string.tag_hot);
        tagMap.put("Icy", R.string.tag_icy);
        tagMap.put("Refreshing", R.string.tag_refreshing);
        tagMap.put("Light", R.string.tag_light);
        categoryToTagMap.clear();
        categoryToTagMap.put("All Categories", createTagSubMap("General"));
        categoryToTagMap.put("Rice / Noodles", createTagSubMap("Healthy", "Spicy", "Oily", "Salty", "Soup"));
        categoryToTagMap.put("Western", createTagSubMap("Grilled", "Fried", "Creamy", "Healthy"));
        categoryToTagMap.put("Malay", createTagSubMap("Spicy", "Sweet", "Oily", "Rendang"));
        categoryToTagMap.put("Chinese", createTagSubMap("Stir-fried", "Steamed", "Salty", "Oily"));
        categoryToTagMap.put("Indian / Mamak", createTagSubMap("Spicy", "Curry", "Oily", "Fried", "Roti"));
        categoryToTagMap.put("Snacks / Bakery", createTagSubMap("Sweet", "Salty", "Crispy", "Baked"));
        categoryToTagMap.put("Desserts", createTagSubMap("Sweet", "Cold", "Hot", "Icy"));
        categoryToTagMap.put("Beverages", createTagSubMap("Sweet", "Cold", "Hot", "Refreshing"));
        categoryToTagMap.put("Vegetarian", createTagSubMap("Healthy", "Light", "Steamed", "Fried"));
        categoryToTagMap.put("Others", createTagSubMap("General"));
    }
    private Map<String, Integer> createTagSubMap(String... tags) {
        Map<String, Integer> subMap = new LinkedHashMap<>();
        for (String tagKey : tags) {
            if (tagMap.containsKey(tagKey)) {
                subMap.put(tagKey, tagMap.get(tagKey));
            }
        }
        return subMap;
    }
    private void setupCategorySpinner() {
        List<String> translatedCategories = new ArrayList<>();
        for (Integer stringId : categoryMap.values()) {
            translatedCategories.add(getString(stringId));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_selected, translatedCategories);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCategory.setAdapter(adapter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedEnglishKey = (new ArrayList<>(categoryMap.keySet())).get(position);
                updateTagSpinner(selectedEnglishKey);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                String defaultKey = (new ArrayList<>(categoryMap.keySet())).get(0);
                updateTagSpinner(defaultKey);
            }
        });
        String defaultKey = (new ArrayList<>(categoryMap.keySet())).get(0);
        updateTagSpinner(defaultKey);
    }
    private void updateTagSpinner(String categoryEnglishKey) {
        Map<String, Integer> tagsForCategory = categoryToTagMap.getOrDefault(
                categoryEnglishKey,
                createTagSubMap(getString(R.string.tag_general))
        );
        List<String> translatedTags = new ArrayList<>();
        for (Integer stringId : tagsForCategory.values()) {
            translatedTags.add(getString(stringId));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_selected, translatedTags);
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerTag.setAdapter(adapter);
    }

    // --- ⬇️ GEMINI FIX: MODIFIED handleSubmit ⬇️ ---
    private void handleSubmit() {
        // (1. Get all inputs - unchanged)
        String dish = etDishName.getText().toString().trim();
        String translatedCategory = spinnerCategory.getSelectedItem().toString();
        String translatedTag = spinnerTag.getSelectedItem().toString();
        String feedbackText = etFeedbackText.getText().toString().trim();
        float rating = ratingBar.getRating();
        boolean hasError = false;

        // --- 2. [NEW] Profanity Check ---
        // (This check now runs first, as requested)
        if (containsProfanity(dish)) {
            // (You MUST add R.string.error_profanity_not_allowed to your strings.xml)
            etDishName.setError(getString(R.string.error_profanity_not_allowed));
            etDishName.setText(""); // Clear the text as requested
            if (!hasError) etDishName.requestFocus();
            hasError = true;
        } else if (dish.isEmpty()) { // (Check for empty *after* profanity)
            etDishName.setError(getString(R.string.error_dish_name_required));
            if (!hasError) etDishName.requestFocus();
            hasError = true;
        } else {
            etDishName.setError(null);
        }

        if (containsProfanity(feedbackText)) {
            // (You MUST add R.string.error_profanity_not_allowed to your strings.xml)
            etFeedbackText.setError(getString(R.string.error_profanity_not_allowed));
            etFeedbackText.setText(""); // Clear the text as requested
            if (!hasError) etFeedbackText.requestFocus();
            hasError = true;
        } else if (feedbackText.isEmpty()) { // (Check for empty *after* profanity)
            etFeedbackText.setError(getString(R.string.error_feedback_text_required));
            if (!hasError) etFeedbackText.requestFocus();
            hasError = true;
        } else {
            etFeedbackText.setError(null);
        }
        // --- ⬆️ END OF NEW VALIDATION ⬆️ ---


        // (3. Validation for Rating - unchanged)
        if (rating == 0.0f) {
            Toast.makeText(requireContext(), R.string.error_rating_required, Toast.LENGTH_SHORT).show();
            if (!hasError) ratingBar.requestFocus();
            hasError = true;
        }

        // (4. Validation for Category - unchanged)
        if (translatedCategory.equals(getString(R.string.cat_all))) {
            Toast.makeText(requireContext(), R.string.error_category_required, Toast.LENGTH_SHORT).show();
            if (!hasError) spinnerCategory.requestFocus();
            hasError = true;
        }

        if (hasError) {
            return; // Stop if any validation failed
        }

        // (Rest of the submit logic remains unchanged)
        btnSubmit.setEnabled(false);
        Toast.makeText(requireContext(), "Submitting...", Toast.LENGTH_SHORT).show();

        if (dishPhotoUri != null) {
            feedbackRepository.uploadImageAndGetUrl(dishPhotoUri, new FeedbackRepository.OnImageUrlReadyListener() {
                @Override
                public void onUrlReady(String httpsUrl) {
                    saveFeedbackToDatabase(dish, translatedCategory, translatedTag, feedbackText, rating, httpsUrl);
                }

                @Override
                public void onUploadFailed(Exception e) {
                    Log.e("FeedbackFragment", "Image upload failed", e);
                    Toast.makeText(getContext(), "Image upload failed. Submitting without image.", Toast.LENGTH_LONG).show();
                    saveFeedbackToDatabase(dish, translatedCategory, translatedTag, feedbackText, rating, "");
                }
            });
        } else {
            saveFeedbackToDatabase(dish, translatedCategory, translatedTag, feedbackText, rating, "");
        }
    }
    // --- ⬆️ END OF FIX ⬆️ ---

    // (saveFeedbackToDatabase method remains unchanged)
    private void saveFeedbackToDatabase(String dish, String translatedCategory, String translatedTag,
                                        String feedbackText, float rating, String imageUriStr) {

        String categoryKey = getKeyFromValue(categoryMap, translatedCategory, "Others");
        String tagKey = getKeyFromValue(tagMap, translatedTag, "General");

        Feedback feedback = new Feedback(
                username, dish, rating, categoryKey, tagKey,
                imageUriStr,
                feedbackText, System.currentTimeMillis()
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            if (db == null) db = AppDatabase.getInstance(requireContext());
            db.feedbackDao().insert(feedback);
            feedbackRepository.uploadFeedback(feedback);
        });

        Toast.makeText(requireContext(), R.string.toast_feedback_submitted, Toast.LENGTH_LONG).show();
        resetForm();

        // (Navigate back to HomeFragment - unchanged)
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    // (resetForm method remains unchanged)
    private void resetForm() {
        if (getContext() == null) return;

        etDishName.setText("");
        etFeedbackText.setText("");
        ratingBar.setRating(0);
        spinnerCategory.setSelection(0);
        spinnerTag.setSelection(0);
        dishPhotoUri = null;

        Glide.with(this).clear(imgDishPhoto);
        textUploadHint.setVisibility(View.VISIBLE);

        etDishName.setError(null);
        etFeedbackText.setError(null);
        btnSubmit.setEnabled(true);
    }

    // (getKeyFromValue method remains unchanged)
    private String getKeyFromValue(Map<String, Integer> map, String translatedValue, String defaultKey) {
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (getString(entry.getValue()).equals(translatedValue)) {
                return entry.getKey();
            }
        }
        return defaultKey;
    }

    // (startVoiceInput method remains unchanged)
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.feedback_voice_prompt));
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), R.string.toast_voice_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    // (showLoginRegisterDialog method remains unchanged)
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