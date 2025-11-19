package com.name.ccf.UI.Main;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout; // 保留：用于获取那个框框
import android.widget.TextView;     // 保留：用于设置文字
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.name.ccf.Data.Repository.UserRepository;
import com.name.ccf.R;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.User;
import com.name.ccf.Utils.PasswordUtility;

import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class RegisterFragment extends Fragment {

    // --- ⬇️ 变量定义 ⬇️ ---
    private LinearLayout dropdownRoleArea;
    private TextView textRole;
    // --- ⬆️ 结束 ⬆️ ---

    private EditText editFirstName, editLastName, editStudentId;
    private EditText editPassword, editConfirmPassword;
    private ImageView iconShowPassword, iconShowConfirmPassword;
    private Button buttonConfirm;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private static final Pattern NAME_INVALID_CHARS_PATTERN = Pattern.compile("[^a-zA-Z ]");

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // --- ⬇️ 绑定视图并设置固定文字 ⬇️ ---
        dropdownRoleArea = view.findViewById(R.id.dropdown_role_area);
        textRole = view.findViewById(R.id.text_role);

        // 直接显示 "Student"，且不设置点击事件
        textRole.setText("Student");
        // --- ⬆️ 结束 ⬆️ ---

        editFirstName = view.findViewById(R.id.editFirstName);
        editLastName = view.findViewById(R.id.editLastName);
        editStudentId = view.findViewById(R.id.editStudentId);
        editPassword = view.findViewById(R.id.editPassword);
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword);
        iconShowPassword = view.findViewById(R.id.iconShowPassword);
        iconShowConfirmPassword = view.findViewById(R.id.iconShowConfirmPassword);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);

        setupPasswordToggle();

        TextWatcher uppercaseWatcher = new TextWatcher() {
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
        editStudentId.addTextChangedListener(uppercaseWatcher);

        TextWatcher titleCaseWatcher = new TextWatcher() {
            private boolean isEditing = false;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isEditing) return;
                isEditing = true;
                String original = s.toString();
                String formatted = toTitleCase(original);
                if (!original.equals(formatted)) {
                    s.replace(0, s.length(), formatted);
                }
                isEditing = false;
            }
        };
        editFirstName.addTextChangedListener(titleCaseWatcher);
        editLastName.addTextChangedListener(titleCaseWatcher);

        buttonConfirm.setOnClickListener(v -> registerUser());
        return view;
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
                titleCase.append(c);
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
                titleCase.append(c);
            } else {
                titleCase.append(Character.toLowerCase(c));
            }
        }
        return titleCase.toString();
    }

    private void setupPasswordToggle() {
        iconShowPassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                editPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                iconShowPassword.setImageResource(R.drawable.ic_eyeopen);
            } else {
                editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                iconShowPassword.setImageResource(R.drawable.ic_eyeclose);
            }
            editPassword.setSelection(editPassword.getText().length());
        });
        iconShowConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                editConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                iconShowConfirmPassword.setImageResource(R.drawable.ic_eyeopen);
            } else {
                editConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                iconShowConfirmPassword.setImageResource(R.drawable.ic_eyeclose);
            }
            editConfirmPassword.setSelection(editConfirmPassword.getText().length());
        });
    }

    private void registerUser() {
        editFirstName.setError(null);
        editLastName.setError(null);
        editStudentId.setError(null);
        editPassword.setError(null);
        editConfirmPassword.setError(null);

        // --- ⬇️ 修改：不需要选择，直接硬编码为 "student" ⬇️ ---
        String userType = "student";
        // --- ⬆️ 结束 ⬆️ ---

        String firstName = editFirstName.getText().toString().trim();
        String lastName = editLastName.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();

        EditText currentIdField = editStudentId;
        String id = currentIdField.getText().toString().trim();

        boolean isValid = true;

        if (containsProfanity(firstName)) {
            editFirstName.setError(getString(R.string.error_profanity_not_allowed));
            editFirstName.setText("");
            isValid = false;
        } else if (firstName.isEmpty()) {
            editFirstName.setError(getString(R.string.error_first_name_required));
            isValid = false;
        } else if (NAME_INVALID_CHARS_PATTERN.matcher(firstName).find()) {
            editFirstName.setError(getString(R.string.error_name_invalid_chars));
            isValid = false;
        }

        if (containsProfanity(lastName)) {
            editLastName.setError(getString(R.string.error_profanity_not_allowed));
            editLastName.setText("");
            isValid = false;
        } else if (lastName.isEmpty()) {
            editLastName.setError(getString(R.string.error_last_name_required));
            isValid = false;
        } else if (NAME_INVALID_CHARS_PATTERN.matcher(lastName).find()) {
            editLastName.setError(getString(R.string.error_name_invalid_chars));
            isValid = false;
        }

        if (id.isEmpty()) {
            currentIdField.setError(getString(R.string.error_id_required));
            isValid = false;
        } else if (!id.matches(ID_REGEX)) {
            currentIdField.setError(getString(R.string.error_id_format));
            isValid = false;
        }

        if (password.isEmpty()) {
            editPassword.setError(getString(R.string.error_password_required));
            isValid = false;
        } else if (!PASSWORD_PATTERN.matcher(password).matches()) {
            editPassword.setError(getString(R.string.toast_password_invalid));
            isValid = false;
        }

        if (confirmPassword.isEmpty()) {
            editConfirmPassword.setError(getString(R.string.error_confirm_password_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            if (isValid) {
                editConfirmPassword.setError(getString(R.string.toast_password_no_match));
                isValid = false;
            }
        }

        if (!isValid) {
            return;
        }

        final String finalUserType = userType;
        final String finalName = firstName + " " + lastName;
        final String finalPassword = password;
        final String finalId = id;

        AppDatabase db = AppDatabase.getInstance(requireContext());
        UserRepository userRepo = new UserRepository();

        Executors.newSingleThreadExecutor().execute(() -> {
            User existing = db.userDao().findByUserTypeAndUserid(finalUserType, finalId);

            if (existing != null) {
                requireActivity().runOnUiThread(() -> {
                    currentIdField.setError(getString(R.string.error_id_exists, finalUserType));
                });
                return;
            }

            String hashedPassword = PasswordUtility.hashPassword(finalPassword);
            User user = new User(finalName, hashedPassword, finalUserType, finalId);
            db.userDao().insert(user);
            userRepo.uploadUser(user);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), getString(R.string.toast_register_success, finalUserType), Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            });
        });
    }
}