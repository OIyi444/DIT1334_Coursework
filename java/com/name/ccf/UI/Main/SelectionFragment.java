package com.name.ccf.UI.Main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.name.ccf.R;
import com.name.ccf.UI.Second.HomeFragment;
import com.name.ccf.UI.Second.SecondaryActivity;

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

    /** 打开 Login Fragment */
    private void openLoginFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new LoginFragment())
                .commit();
    }

    /** 打开 Register Fragment */
    private void openRegisterFragment() {
        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RegisterFragment())
                .commit();
    }

    /** Guest 临时登录确认 */
    private void showGuestConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Continue as Guest?")
                .setMessage("Guest account is temporary. You won't be able to submit feedback without registering.")
                .setPositiveButton("Yes", (dialog, which) -> loginAsGuest())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** 设置为 Guest 并跳转 HomeFragment */
    private void loginAsGuest() {
        // 创建 Intent 启动 SecondaryActivity
        android.content.Intent intent = new android.content.Intent(
                getActivity(),
                com.name.ccf.UI.Second.SecondaryActivity.class
        );

        // 传递用户信息（可选）
        intent.putExtra("username", "Guest");
        intent.putExtra("userType", "guest");

        // 启动主界面
        startActivity(intent);

        Toast.makeText(getContext(), "Logged in as Guest", Toast.LENGTH_SHORT).show();
    }

}
