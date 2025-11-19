package com.name.ccf.UI.Second;

// --- ⬇️ GEMINI FIX: Added ALL necessary imports ---
import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView; // (Import ImageView)
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat; // (Import ContextCompat)
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide; // (Import Glide)
import com.google.android.material.button.MaterialButton;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.Help;
// --- ⬇️ GEMINI FIX: Using YOUR spelling "HelpRespository" ---
import com.name.ccf.Data.Repository.HelpRespository;
// --- ⬆️ END OF FIX ---
import java.util.concurrent.Executors;

import com.name.ccf.R;
import com.name.ccf.UI.Main.MainActivity;

import java.util.ArrayList;

/*
 * (Original comments about guest restrictions, etc.)
 */
public class ContactUsFragment extends Fragment {

    // (Original variables)
    private EditText etStudentName, etStudentId, etFeedback;
    private Button btnSubmit;
    private ImageButton btnMic;

    // --- ⬇️ GEMINI FIX: Added variables for Image Upload ---
    private ImageView imgDishPhoto;
    private TextView textUploadHint;
    private Uri dishPhotoUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    // --- ⬆️ END OF FIX ---

    private ActivityResultLauncher<Intent> speechLauncher;
    private AppDatabase db;

    // --- ⬇️ GEMINI FIX: Using YOUR spelling "HelpRespository" ---
    private HelpRespository helpRepository;
    // --- ⬆️ END OF FIX ---

    private String username = "Guest";
    private String userType = "guest";
    private String studentId = "N/A";


    // --- ⬇️ GEMINI FIX: Added profanity blocklist ---
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


    // --- ⬇️ GEMINI FIX: Added onCreate to register permission launcher ---
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Launcher for Runtime Permission
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission IS granted. Now we can open the gallery.
                        imagePickerLauncher.launch("image/*");
                    } else {
                        // Permission DENIED. Show a toast.
                        Toast.makeText(getContext(), "Permission denied. Cannot pick image.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    // --- ⬆️ END OF FIX ---


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // 1. Get user data
        if (getActivity() instanceof SecondaryActivity) {
            SecondaryActivity activity = (SecondaryActivity) getActivity();
            username = activity.getUsername();
            userType = activity.getUserType();
            studentId = activity.getStudentId();
        }
        if (userType == null || userType.isEmpty()) {
            userType = "guest";
        }

        // 2. If Guest, inflate the 'item_login_prompt' layout and stop
        if ("guest".equals(userType)) {
            View loginPromptView = inflater.inflate(R.layout.item_login_prompt, container, false);

            Button btnLogin = loginPromptView.findViewById(R.id.btn_login_prompt);
            btnLogin.setOnClickListener(v -> {
                showLoginRegisterDialog();
            });

            return loginPromptView;
        }

        // 3. If user is *NOT* Guest, load the main fragment
        View view = inflater.inflate(R.layout.fragment_contact_us, container, false);

        // (Find views)
        etStudentName = view.findViewById(R.id.etStudentName);
        etStudentId = view.findViewById(R.id.etStudentId);
        etFeedback = view.findViewById(R.id.etFeedback);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnMic = view.findViewById(R.id.btnMic);

        // (Find new image views)
        imgDishPhoto = view.findViewById(R.id.img_dish_photo);
        textUploadHint = view.findViewById(R.id.text_upload_hint);

        // (Initialize DB/Repo and pre-fill fields)
        db = AppDatabase.getInstance(requireContext());
        helpRepository = new HelpRespository(); // (Using YOUR spelling)

        etStudentName.setText(username);
        etStudentId.setText(studentId);

        // (Register image picker and mic launchers)
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        dishPhotoUri = uri; // This is the local content:// URI
                        Glide.with(this).load(uri).into(imgDishPhoto);
                        textUploadHint.setVisibility(View.GONE);
                    }
                });

        imgDishPhoto.setOnClickListener(v -> checkAndRequestPermission());

        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (matches != null && !matches.isEmpty()) {
                            etFeedback.setText(matches.get(0));
                            etFeedback.setSelection(etFeedback.getText().length());
                        }
                    }
                });

        btnSubmit.setOnClickListener(v -> submitFeedback());
        btnMic.setOnClickListener(v -> startVoiceInput());

        return view;
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


    // (startVoiceInput method remains unchanged)
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.contact_voice_prompt));
        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), R.string.toast_voice_not_supported, Toast.LENGTH_SHORT).show();
        }
    }

    // --- ⬇️ GEMINI FIX: MODIFIED submitFeedback to include Profanity Check ⬇️ ---
    private void submitFeedback() {

        String studentName = etStudentName.getText().toString().trim();
        String studentId = etStudentId.getText().toString().trim();
        String feedback = etFeedback.getText().toString().trim();
        boolean hasError = false;

        // --- 1. [NEW] Profanity Check ---
        if (containsProfanity(studentName)) {
            etStudentName.setError(getString(R.string.error_profanity_not_allowed));
            etStudentName.setText(""); // Clear the text
            if (!hasError) etStudentName.requestFocus();
            hasError = true;
        } else if (studentName.isEmpty()) {
            etStudentName.setError(getString(R.string.error_name_required));
            if (!hasError) etStudentName.requestFocus();
            hasError = true;
        } else {
            etStudentName.setError(null);
        }

        if (containsProfanity(feedback)) {
            etFeedback.setError(getString(R.string.error_profanity_not_allowed));
            etFeedback.setText(""); // Clear the text
            if (!hasError) etFeedback.requestFocus();
            hasError = true;
        } else if (feedback.isEmpty()) {
            etFeedback.setError(getString(R.string.error_feedback_text_required));
            if (!hasError) etFeedback.requestFocus();
            hasError = true;
        } else {
            etFeedback.setError(null);
        }

        // (Student ID check remains unchanged)
        if (studentId.isEmpty()) {
            etStudentId.setError(getString(R.string.error_id_required));
            if (!hasError) etStudentId.requestFocus();
            hasError = true;
        } else {
            etStudentId.setError(null);
        }
        // --- ⬆️ END OF NEW VALIDATION ⬆️ ---


        if (hasError) return;

        // (Rest of the submit logic remains unchanged)
        btnSubmit.setEnabled(false);
        Toast.makeText(requireContext(), "Submitting...", Toast.LENGTH_SHORT).show();

        if (dishPhotoUri != null) {
            helpRepository.uploadImageAndGetUrl(dishPhotoUri, new HelpRespository.OnImageUrlReadyListener() {
                @Override
                public void onUrlReady(String httpsUrl) {
                    saveHelpToDatabase(feedback, httpsUrl);
                }
                @Override
                public void onUploadFailed(Exception e) {
                    Log.e("ContactUsFragment", "Image upload failed", e);
                    Toast.makeText(getContext(), "Image upload failed. Submitting without image.", Toast.LENGTH_LONG).show();
                    saveHelpToDatabase(feedback, "");
                }
            });
        } else {
            saveHelpToDatabase(feedback, "");
        }
    }

    // (saveHelpToDatabase method remains unchanged)
    private void saveHelpToDatabase(String helpText, String imageUriStr) {

        Help helpRequest = new Help(
                this.username,
                this.studentId,
                helpText,
                imageUriStr
        );

        Executors.newSingleThreadExecutor().execute(() -> {
            if (db == null) db = AppDatabase.getInstance(requireContext());
            db.helpDAO().insert(helpRequest);

            if (helpRepository == null) helpRepository = new HelpRespository();
            helpRepository.uploadHelpRequest(helpRequest);
        });

        Toast.makeText(getContext(), R.string.toast_contact_submitted, Toast.LENGTH_LONG).show();
        resetForm();
    }

    // (resetForm method remains unchanged)
    private void resetForm() {
        if (getContext() == null) return;

        etFeedback.setText("");
        dishPhotoUri = null;

        Glide.with(this).clear(imgDishPhoto);
        textUploadHint.setVisibility(View.VISIBLE);

        etFeedback.setError(null);
        btnSubmit.setEnabled(true);
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