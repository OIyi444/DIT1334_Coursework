package com.name.ccf.UI.Main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.name.ccf.R;
import com.name.ccf.UI.Second.HomeFragment;
import com.name.ccf.UI.Second.SecondaryActivity;

/*
 * ==========================================================
 * ⭐️ What & Why: Changes from the previous version ⭐️
 * ==========================================================
 * * 1.  (NEW CHANGE) Externalized all hardcoded Java strings:
 * * Why? (To make this page fully support language switching).
 * * All user-visible strings in this file (like "Continue as Guest?",
 * * "Logged in as Guest") were hardcoded in English.
 * * I have "REPLACED" all of them with 'R.string' references
 * * (e.g., 'R.string.guest_confirm_title', 'R.string.toast_login_guest').
 */

public class SelectionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnLogin = view.findViewById(R.id.btn_login);
        Button btnRegister = view.findViewById(R.id.btn_register);
        Button btnGuest = view.findViewById(R.id.btn_guest);

        btnLogin.setOnClickListener(v -> openLoginFragment());
        btnRegister.setOnClickListener(v -> openRegisterFragment());
        btnGuest.setOnClickListener(v -> showGuestConfirmation());
    }

    /** (您的原有注释) 打开 Login Fragment */
    private void openLoginFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }

    /** (您的原有注释) 打开 Register Fragment */
    private void openRegisterFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RegisterFragment())
                .commit();
    }

    /** (您的原有注释) Guest 临时登录确认 */
    // ⬇️ 更改: 换成了 @string 引用 ⬇️
    private void showGuestConfirmation() {
        if (getContext() == null) return;

        // 1️⃣ 载入自定义布局
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.design_dialog_confirm, null);

        // 2️⃣ 获取控件
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        TextView dialogMessage = view.findViewById(R.id.dialog_message);
        MaterialButton btnCancel = view.findViewById(R.id.button_cancel);
        MaterialButton btnConfirm = view.findViewById(R.id.button2);
        view.findViewById(R.id.button1).setVisibility(View.GONE);


        // 3️⃣ 设置标题和内容
        dialogTitle.setText(R.string.guest_confirm_title);
        dialogMessage.setText(R.string.guest_confirm_message);
        btnCancel.setText(R.string.dialog_cancel);
        btnConfirm.setText(R.string.yes);

        // 4️⃣ 创建圆角 Dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(true)
                .create();

        // 5️⃣ 按钮点击事件
        btnConfirm.setOnClickListener(v -> {
            loginAsGuest();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 6️⃣ 显示 Dialog
        dialog.show();
    }


    /** (您的原有注释) 设置为 Guest 并跳转 HomeFragment */
    // ⬇️ 更改: 换成了 @string 引用 ⬇️
    private void loginAsGuest() {
        android.content.Intent intent = new android.content.Intent(
                getActivity(),
                com.name.ccf.UI.Second.SecondaryActivity.class
        );

        // (旧代码) intent.putExtra("username", "Guest");
        intent.putExtra("username", getString(R.string.guest_username)); // ✅ (新代码)
        intent.putExtra("userType", "guest");

        startActivity(intent);

        // (旧代码) Toast.makeText(getContext(), "Logged in as Guest", ...);
        Toast.makeText(getContext(), R.string.toast_login_guest, Toast.LENGTH_SHORT).show(); // ✅ (新代码)
    }

}