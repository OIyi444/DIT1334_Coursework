package com.name.ccf.UI.Main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
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

import com.name.ccf.R;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.User;
import com.name.ccf.UI.Second.SecondaryActivity;

import java.util.concurrent.Executors;

public class LoginFragment extends Fragment {

    private LinearLayout layoutStudent, layoutAdmin;
    private EditText editStudentId, editStudentPassword, editAdminId, editAdminPassword;
    private Button buttonStudent, buttonAdmin, buttonLogin;
    private CheckBox checkSavePassword;
    private boolean isStudentMode = true;
    private ImageView iconShowStudentPassword, iconShowAdminPassword;
    private TextView textForgotPassword;
    private SharedPreferences sharedPreferences;

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
        setupForgotPassword(); // ✅ 集成 Room 重置密码
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
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Reset Password");

            LinearLayout layout = new LinearLayout(requireContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 10);

            EditText inputId = new EditText(requireContext());
            inputId.setHint(isStudentMode ? "Student ID" : "Admin ID");
            inputId.setInputType(InputType.TYPE_CLASS_TEXT);
            layout.addView(inputId);

            EditText inputNewPassword = new EditText(requireContext());
            inputNewPassword.setHint("New Password");
            inputNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            layout.addView(inputNewPassword);

            builder.setView(layout);

            builder.setPositiveButton("Reset", (dialog, which) -> {
                String id = inputId.getText().toString().trim();
                String newPassword = inputNewPassword.getText().toString().trim();

                if (id.isEmpty() || newPassword.isEmpty()) {
                    Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                AppDatabase db = AppDatabase.getInstance(requireContext());

                Executors.newSingleThreadExecutor().execute(() -> {
                    User user = db.userDao().findByUserTypeAndUserid(isStudentMode ? "student" : "admin", id);
                    if (user != null) {
                        user.password = newPassword;
                        db.userDao().update(user);

                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Password reset successfully!", Toast.LENGTH_SHORT).show());
                    } else {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "No such user found", Toast.LENGTH_SHORT).show());
                    }
                });
            });

            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
            builder.show();
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

        // 显示学生登录区
        layoutStudent.setVisibility(View.VISIBLE);
        layoutAdmin.setVisibility(View.GONE);

        // 切换按钮选中状态
        buttonStudent.setSelected(true);
        buttonAdmin.setSelected(false);

        // 更新视觉效果
        buttonStudent.setTextColor(getResources().getColor(android.R.color.white));
        buttonAdmin.setTextColor(getResources().getColor(R.color.black));
    }

    private void switchToAdminMode() {
        isStudentMode = false;

        // 显示管理员登录区
        layoutStudent.setVisibility(View.GONE);
        layoutAdmin.setVisibility(View.VISIBLE);

        // 切换按钮选中状态
        buttonStudent.setSelected(false);
        buttonAdmin.setSelected(true);

        // 更新视觉效果
        buttonAdmin.setTextColor(getResources().getColor(android.R.color.white));
        buttonStudent.setTextColor(getResources().getColor(R.color.black));
    }


    private void loginUser() {
        String id, password, userType;

        if (isStudentMode) {
            id = editStudentId.getText().toString().trim().toUpperCase(); // ✅ 转大写
            password = editStudentPassword.getText().toString().trim();
            userType = "student";
        } else {
            id = editAdminId.getText().toString().trim().toUpperCase(); // ✅ 转大写
            password = editAdminPassword.getText().toString().trim();
            userType = "admin";
        }

        if (id.isEmpty() || password.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        AppDatabase db = AppDatabase.getInstance(requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            User user = db.userDao().findByUserTypeAndUserid(userType, id); // 这里就能匹配大小写统一的 ID

            requireActivity().runOnUiThread(() -> {
                if (user == null) {
                    Toast.makeText(getActivity(), "No such " + userType + " account", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.equals(user.password)) {
                    Toast.makeText(getActivity(), "Welcome " + userType + "!", Toast.LENGTH_SHORT).show();
                    saveCredentials(id, password);

                    Intent intent = new Intent(requireContext(), SecondaryActivity.class);
                    intent.putExtra("username", user.username != null ? user.username : "");
                    intent.putExtra("userType", userType != null ? userType : "");
                    startActivity(intent);
                    requireActivity().finish();
                } else {
                    Toast.makeText(getActivity(), "Incorrect password", Toast.LENGTH_SHORT).show();
                }
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
