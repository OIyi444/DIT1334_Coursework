package com.name.ccf.UI.Main;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.name.ccf.R;
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.User;

import java.util.concurrent.Executors;

public class RegisterFragment extends Fragment {

    private Spinner userTypeSpinner;
    private EditText editName, editStudentId, editAdminId, editPassword, editConfirmPassword;
    private ImageView iconShowPassword, iconShowConfirmPassword;
    private LinearLayout layoutStudent, layoutAdmin;
    private Button buttonConfirm;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        userTypeSpinner = view.findViewById(R.id.user_type);
        editName = view.findViewById(R.id.editName);
        editStudentId = view.findViewById(R.id.editStudentId);
        editAdminId = view.findViewById(R.id.editAdminId);
        editPassword = view.findViewById(R.id.editPassword);
        editConfirmPassword = view.findViewById(R.id.editConfirmPassword);
        iconShowPassword = view.findViewById(R.id.iconShowPassword);
        iconShowConfirmPassword = view.findViewById(R.id.iconShowConfirmPassword);
        layoutStudent = view.findViewById(R.id.layoutStudent);
        layoutAdmin = view.findViewById(R.id.layoutAdmin);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);

        setupSpinner();
        setupPasswordToggle();

        buttonConfirm.setOnClickListener(v -> registerUser());

        return view;
    }

    private void setupSpinner() {
        String[] userTypes = {"Student", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, userTypes);
        userTypeSpinner.setAdapter(adapter);

        userTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                String selected = userTypes[position];
                if (selected.equals("Student")) {
                    layoutStudent.setVisibility(View.VISIBLE);
                    layoutAdmin.setVisibility(View.GONE);
                } else {
                    layoutStudent.setVisibility(View.GONE);
                    layoutAdmin.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
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
        String userType = userTypeSpinner.getSelectedItem().toString().toLowerCase();
        String name = editName.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        String confirmPassword = editConfirmPassword.getText().toString().trim();
        String id = userType.equals("student") ? editStudentId.getText().toString().trim()
                : editAdminId.getText().toString().trim();

        if (name.isEmpty() || id.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(getActivity(), "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        // 统一 userid 为大写，避免大小写重复注册
        id = id.toUpperCase();

        // 使变量 final，用于 lambda
        final String finalUserType = userType;
        final String finalName = name;
        final String finalPassword = password;
        final String finalId = id;

        AppDatabase db = AppDatabase.getInstance(requireContext());

        Executors.newSingleThreadExecutor().execute(() -> {
            // 检查同类型用户是否已经有这个 ID
            User existing = db.userDao().findByUserTypeAndUserid(finalUserType, finalId);

            if (existing != null) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "This " + finalUserType + " ID already exists", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // 插入新用户
            User user = new User(finalName, finalPassword, finalUserType, finalId);
            db.userDao().insert(user);

            // 回到主线程显示 Toast 并跳转
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), "Registered as " + finalUserType, Toast.LENGTH_SHORT).show();
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LoginFragment())
                        .commit();
            });
        });
    }

}
