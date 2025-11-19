package com.name.ccf.UI.Main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.name.ccf.R;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.User;
import com.name.ccf.UI.Second.SecondaryActivity;
import com.name.ccf.Data.Repository.UserRepository;
import com.name.ccf.Utils.PasswordUtility;

import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class LoginFragment extends Fragment {

    // --- ⬇️ UPDATED: Admin Credentials (Hardcoded) ⬇️ ---
    private static final String ADMIN_1_ID = "ADMN1234567";
    private static final String ADMIN_1_PASS = "Czx@1234567";

    private static final String ADMIN_2_ID = "ADMN7654321";
    private static final String ADMIN_2_PASS = "Tcy@7654321";
    // --- ⬆️ END ⬆️ ---

    private LinearLayout layoutStudent, layoutAdmin;
    private EditText editStudentId, editStudentPassword, editAdminId, editAdminPassword;
    private Button buttonStudent, buttonAdmin, buttonLogin;
    private CheckBox checkSavePassword;
    private boolean isStudentMode = true;
    private ImageView iconShowStudentPassword, iconShowAdminPassword;
    private TextView textForgotPassword;
    private SharedPreferences sharedPreferences;

    private static final String ID_REGEX = "(SCSJ|SCKL|SCKD|SCPG)\\d{7}";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^" +
                    "(?=.*[a-z])" +
                    "(?=.*[A-Z])" +
                    "(?=.*\\d)" +
                    "(?=.*[^A-Za-z0-9])" +
                    ".{8,26}$"
    );

    private static final String[] PROFANITY_BLOCKLIST = {
            "fuck", "shit", "bitch", "cunt", "asshole", "piss", "dick", "pussy",
            "puki", "pukimak", "babi", "sial", "bodoh", "gampang", "pantat",
            "cibai", "kanina", "lanjiao", "cb", "knn", "ccb", "nabei", "fuck",
            "wtf", "lmfao", "stfu"
    };

    private boolean containsProfanity(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lowerCaseText = text.toLowerCase().replace(" ", "");
        for (String blockedWord : PROFANITY_BLOCKLIST) {
            if (lowerCaseText.contains(blockedWord)) {
                return true;
            }
        }
        return false;
    }

    private final TextWatcher uppercaseWatcher = new TextWatcher() {
        private boolean isEditing = false;
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override
        public void afterTextChanged(Editable s) {
            if (isEditing) {
                return;
            }
            isEditing = true;
            String original = s.toString();
            String upper = original.toUpperCase();

            if (!original.equals(upper)) {
                s.replace(0, s.length(), upper);
            }
            isEditing = false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        layoutStudent = view.findViewById(R.id.layoutStudent);
        layoutAdmin = view.findViewById(R.id.layoutAdmin);
        editStudentId = view.findViewById(R.id.editStudentId);
        editStudentPassword = view.findViewById(R.id.editStudentPassword);
        editAdminId = view.findViewById(R.id.editAdminId);
        editAdminPassword = view.findViewById(R.id.editAdminPassword);
        buttonStudent = view.findViewById(R.id.buttonStudent);
        buttonAdmin = view.findViewById(R.id.buttonAdmin);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        checkSavePassword = view.findViewById(R.id.checkSavePassword);
        iconShowStudentPassword = view.findViewById(R.id.iconShowStudentPassword);
        iconShowAdminPassword = view.findViewById(R.id.iconShowAdminPassword);
        textForgotPassword = view.findViewById(R.id.textForgotPassword);

        sharedPreferences = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        loadSavedCredentials();

        buttonStudent.setOnClickListener(v -> switchToStudentMode());
        buttonAdmin.setOnClickListener(v -> switchToAdminMode());
        buttonLogin.setOnClickListener(v -> loginUser());
        setupPasswordToggle();
        setupForgotPassword();

        editStudentId.addTextChangedListener(uppercaseWatcher);
        editAdminId.addTextChangedListener(uppercaseWatcher);

        return view;
    }

    private void setupPasswordToggle() {
        iconShowStudentPassword.setOnClickListener(v ->
                togglePasswordVisibility(editStudentPassword, iconShowStudentPassword));
        iconShowAdminPassword.setOnClickListener(v ->
                togglePasswordVisibility(editAdminPassword, iconShowAdminPassword));
    }

    private void togglePasswordVisibility(EditText editText, ImageView icon) {
        if (editText.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            icon.setImageResource(R.drawable.ic_eyeopen);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            icon.setImageResource(R.drawable.ic_eyeclose);
        }
        editText.setSelection(editText.getText().length());
    }

    private void setupForgotPassword() {
        textForgotPassword.setOnClickListener(v -> {
            if (getContext() == null) return;

            View view = LayoutInflater.from(requireContext())
                    .inflate(R.layout.design_dialog_input, null);

            TextView dialogTitle = view.findViewById(R.id.dialog_title);
            EditText inputId = view.findViewById(R.id.dialog_input_1);
            EditText inputNewPassword = view.findViewById(R.id.dialog_input_2);
            EditText inputExtra = view.findViewById(R.id.dialog_input_3);
            MaterialButton btnCancel = view.findViewById(R.id.button_cancel);
            MaterialButton btnConfirm = view.findViewById(R.id.button_confirm);

            dialogTitle.setText(R.string.login_reset_title);
            inputId.setHint(isStudentMode ? getString(R.string.login_hint_student_id)
                    : getString(R.string.login_hint_admin_id));
            inputId.setInputType(InputType.TYPE_CLASS_TEXT);

            inputId.addTextChangedListener(uppercaseWatcher);

            inputNewPassword.setHint(R.string.login_reset_hint_new_password);
            inputNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            inputNewPassword.setVisibility(View.VISIBLE);

            inputExtra.setVisibility(View.GONE);

            AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                    .setView(view)
                    .setCancelable(true)
                    .create();

            btnConfirm.setOnClickListener(v1 -> {
                inputId.setError(null);
                inputNewPassword.setError(null);

                String id = inputId.getText().toString().trim();
                String newPassword = inputNewPassword.getText().toString().trim();
                boolean hasError = false;

                if (id.isEmpty()) {
                    inputId.setError(getString(R.string.error_id_required));
                    hasError = true;
                } else if (!id.matches(ID_REGEX)) {
                    inputId.setError(getString(R.string.error_id_format));
                    hasError = true;
                }

                if (containsProfanity(newPassword)) {
                    inputNewPassword.setError(getString(R.string.error_profanity_not_allowed));
                    inputNewPassword.setText("");
                    hasError = true;
                } else if (newPassword.isEmpty()) {
                    inputNewPassword.setError(getString(R.string.error_password_required));
                    hasError = true;
                }
                else if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
                    inputNewPassword.setError(getString(R.string.toast_password_invalid));
                    hasError = true;
                }

                if (hasError) {
                    Toast.makeText(getActivity(), R.string.toast_fill_all_fields, Toast.LENGTH_SHORT).show();
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(requireContext());
                Executors.newSingleThreadExecutor().execute(() -> {
                    User user = db.userDao().findByUserTypeAndUserid(isStudentMode ? "student" : "admin", id);

                    if (user != null) {
                        String hashedNewPassword = PasswordUtility.hashPassword(newPassword);
                        user.password = hashedNewPassword;
                        db.userDao().update(user);
                        new UserRepository().uploadUser(user);

                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), R.string.toast_password_reset_success, Toast.LENGTH_SHORT).show());
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), R.string.toast_user_not_found_simple, Toast.LENGTH_SHORT).show());
                    }
                });

                dialog.dismiss();
            });

            btnCancel.setOnClickListener(v1 -> dialog.dismiss());
            dialog.show();
        });
    }

    private void loadSavedCredentials() {
        boolean savedAsStudent = sharedPreferences.getBoolean("isStudentMode", true);
        isStudentMode = savedAsStudent;
        switchToMode(savedAsStudent);
        if (savedAsStudent) {
            editStudentId.setText(sharedPreferences.getString("studentId", ""));
            editStudentPassword.setText(sharedPreferences.getString("studentPassword", ""));
        } else {
            editAdminId.setText(sharedPreferences.getString("adminId", ""));
            editAdminPassword.setText(sharedPreferences.getString("adminPassword", ""));
        }
        checkSavePassword.setChecked(sharedPreferences.getBoolean("savePassword", false));
    }
    private void switchToMode(boolean studentMode) {
        if (studentMode) switchToStudentMode();
        else switchToAdminMode();
    }
    private void switchToStudentMode() {
        isStudentMode = true;
        layoutStudent.setVisibility(View.VISIBLE);
        layoutAdmin.setVisibility(View.GONE);
        buttonStudent.setSelected(true);
        buttonAdmin.setSelected(false);
        buttonStudent.setTextColor(getResources().getColor(android.R.color.white));
        buttonAdmin.setTextColor(getResources().getColor(R.color.black));
    }
    private void switchToAdminMode() {
        isStudentMode = false;
        layoutStudent.setVisibility(View.GONE);
        layoutAdmin.setVisibility(View.VISIBLE);
        buttonStudent.setSelected(false);
        buttonAdmin.setSelected(true);
        buttonAdmin.setTextColor(getResources().getColor(android.R.color.white));
        buttonStudent.setTextColor(getResources().getColor(R.color.black));
    }

    private void loginUser() {

        final String userType;
        final EditText currentIdField;
        final EditText currentPasswordField;
        if (isStudentMode) {
            currentIdField = editStudentId;
            currentPasswordField = editStudentPassword;
            userType = "student";
        } else {
            currentIdField = editAdminId;
            currentPasswordField = editAdminPassword;
            userType = "admin";
        }
        currentIdField.setError(null);
        currentPasswordField.setError(null);

        String id = currentIdField.getText().toString().trim().toUpperCase();
        String password = currentPasswordField.getText().toString().trim();

        boolean hasError = false;
        if (id.isEmpty()) {
            currentIdField.setError(getString(R.string.error_id_required));
            hasError = true;
        }
        if (password.isEmpty()) {
            currentIdField.setError(getString(R.string.error_password_required));
            hasError = true;
        }
        if (hasError) return;

        // --- ⬇️ MODIFIED LOGIC: Check hardcoded Admins with new IDs ⬇️ ---
        if (!isStudentMode) {
            boolean isAdmin1 = id.equals(ADMIN_1_ID) && password.equals(ADMIN_1_PASS);
            boolean isAdmin2 = id.equals(ADMIN_2_ID) && password.equals(ADMIN_2_PASS);

            if (isAdmin1 || isAdmin2) {
                // Login Success Toast
                Toast.makeText(getActivity(), getString(R.string.toast_welcome, "Admin"), Toast.LENGTH_SHORT).show();
                saveCredentials(id, password);

                // Navigate to next activity
                Intent intent = new Intent(requireContext(), SecondaryActivity.class);
                // Distinguish which admin is logging in
                String adminName = isAdmin1 ? "Admin 1 (Czx)" : "Admin 2 (Tcy)";

                intent.putExtra("username", adminName);
                intent.putExtra("userType", "admin");
                intent.putExtra("STUDENT_ID", id);
                startActivity(intent);
                requireActivity().finish();
                return; // ⚠️ Return immediately on match, do not check database
            }
        }
        // --- ⬆️ END ⬆️ ---

        // Standard database query
        AppDatabase db = AppDatabase.getInstance(requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            User localUser = db.userDao().findByUserTypeAndUserid(userType, id);

            if (getActivity() == null) return;

            getActivity().runOnUiThread(() -> {
                if (localUser == null) {
                    currentIdField.setError(getString(R.string.error_no_account, userType));
                    return;
                }

                if (!PasswordUtility.verifyPassword(password, localUser.password)) {
                    currentPasswordField.setError(getString(R.string.toast_password_incorrect));
                    return;
                }

                Toast.makeText(getActivity(), getString(R.string.toast_welcome, userType), Toast.LENGTH_SHORT).show();

                saveCredentials(id, password);

                Intent intent = new Intent(requireContext(), SecondaryActivity.class);
                intent.putExtra("username", localUser.username);
                intent.putExtra("userType", localUser.usertype);
                intent.putExtra("STUDENT_ID", localUser.userid);
                startActivity(intent);
                requireActivity().finish();
            });
        });
    }

    private void saveCredentials(String id, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("savePassword", checkSavePassword.isChecked());
        editor.putBoolean("isStudentMode", isStudentMode);

        if (checkSavePassword.isChecked()) {
            if (isStudentMode) {
                editor.putString("studentId", id);
                editor.putString("studentPassword", password);
            } else {
                editor.putString("adminId", id);
                editor.putString("adminPassword", password);
            }
        } else {
            editor.remove("studentId");
            editor.remove("studentPassword");
            editor.remove("adminId");
            editor.remove("adminPassword");
        }
        editor.apply();
    }
}