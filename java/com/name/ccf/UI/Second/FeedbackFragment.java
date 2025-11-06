package com.name.ccf.UI.Second;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;

import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.R;
import com.name.ccf.UI.Main.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FeedbackFragment extends Fragment {

    private ImageView imgDishPhoto;
    private TextView textUploadHint;
    private Spinner spinnerCategory, spinnerTag;
    private RatingBar ratingBar;
    private EditText etDishName, etFeedbackText;
    private Button btnSubmit;
    private ImageButton btnMic;

    private Uri dishPhotoUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<Intent> speechLauncher;

    private AppDatabase db;

    private final Map<String, String[]> tagMap = new HashMap<>();

    private String username = "Guest";
    private String userType = "guest";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_feedback, container, false);

        // ===== Find UI elements =====
        imgDishPhoto = root.findViewById(R.id.img_dish_photo);
        textUploadHint = root.findViewById(R.id.text_upload_hint);
        spinnerCategory = root.findViewById(R.id.spinner_category);
        spinnerTag = root.findViewById(R.id.spinner_tag);
        ratingBar = root.findViewById(R.id.ratingBar);
        etDishName = root.findViewById(R.id.et_dish_name);
        etFeedbackText = root.findViewById(R.id.et_feedback_text);
        btnSubmit = root.findViewById(R.id.btn_submit);
        btnMic = root.findViewById(R.id.btn_mic);

        // ===== Database =====
        db = AppDatabase.getInstance(requireContext());

        // ===== Get current user info =====
        if (getActivity() instanceof SecondaryActivity) {
            SecondaryActivity activity = (SecondaryActivity) getActivity();
            username = activity.getUsername();
            userType = activity.getUserType();
        }

        // ===== Guest restrictions =====
        if ("guest".equals(userType)) {
            etDishName.setEnabled(false);
            etFeedbackText.setEnabled(false);
            ratingBar.setIsIndicator(true);
            spinnerCategory.setEnabled(false);
            spinnerTag.setEnabled(false);
            imgDishPhoto.setEnabled(false);
            btnMic.setEnabled(false);

            btnSubmit.setOnClickListener(v -> {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Guest Access")
                        .setMessage("Guest users cannot submit feedback.\nPlease register or login to continue.")
                        .setPositiveButton("Register", (dialog, which) -> {
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            intent.putExtra("openFragment", "register");
                            startActivity(intent);
                            requireActivity().finish(); // 关闭 SecondaryActivity
                        })
                        .setNegativeButton("Login", (dialog, which) -> {
                            Intent intent = new Intent(requireContext(), MainActivity.class);
                            intent.putExtra("openFragment", "login");
                            startActivity(intent);
                            requireActivity().finish(); // 关闭 SecondaryActivity
                        })
                        .setNeutralButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }

        else {
            btnSubmit.setOnClickListener(v -> handleSubmit());
        }

        // ===== Image Picker =====
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        dishPhotoUri = uri;
                        imgDishPhoto.setImageURI(uri);
                        textUploadHint.setVisibility(View.GONE);
                    }
                });
        imgDishPhoto.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        // ===== Voice Input =====
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

        // ===== Category/Tag setup =====
        setupCategoryTagData();
        setupCategorySpinner();

        return root;
    }

    private void setupCategoryTagData() {
        tagMap.put("Rice", new String[]{"Healthy", "Spicy", "Oil", "Salty"});
        tagMap.put("Noodle", new String[]{"Healthy", "Spicy", "Oil", "Salty"});
        tagMap.put("Roti", new String[]{"Healthy", "Spicy", "Oil", "Salty"});
        tagMap.put("Snacks", new String[]{"Sweet", "Crispy"});
        tagMap.put("All", new String[]{"General"});
    }

    private void setupCategorySpinner() {
        String[] categories = {"All", "Rice", "Noodle", "Roti", "Snacks"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateTagSpinner(categories[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTagSpinner(String category) {
        String[] tags = tagMap.getOrDefault(category, new String[]{"General"});
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, tags);
        spinnerTag.setAdapter(adapter);
    }

    private void handleSubmit() {
        String dish = etDishName.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String tag = spinnerTag.getSelectedItem().toString();
        String feedbackText = etFeedbackText.getText().toString().trim();
        float rating = ratingBar.getRating();

        if (dish.isEmpty() || feedbackText.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String imageUriStr = dishPhotoUri != null ? dishPhotoUri.toString() : "";

        Feedback feedback = new Feedback(
                username,
                dish,
                rating,
                category,
                tag,
                imageUriStr,
                feedbackText,
                System.currentTimeMillis()
        );

        // Insert into Room DB
        new Thread(() -> db.feedbackDao().insert(feedback)).start();

        Toast.makeText(requireContext(), "Feedback submitted!", Toast.LENGTH_LONG).show();

        // Reset UI
        etDishName.setText("");
        etFeedbackText.setText("");
        ratingBar.setRating(0);
        spinnerCategory.setSelection(0);
        spinnerTag.setSelection(0);
        dishPhotoUri = null;
        imgDishPhoto.setImageDrawable(null);
        textUploadHint.setVisibility(View.VISIBLE);
    }

    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your feedback");
        try {
            speechLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Voice recognition not available", Toast.LENGTH_SHORT).show();
        }
    }
}
