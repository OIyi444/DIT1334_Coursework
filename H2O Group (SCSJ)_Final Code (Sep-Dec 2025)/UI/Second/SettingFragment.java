package com.name.ccf.UI.Second;

// (All your original imports remain unchanged)
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.name.ccf.R;

import java.util.Locale;

/*
 * (Your original imports)
 */
import android.content.ClipData;
import android.content.ClipboardManager;
import android.text.InputType;
import android.widget.EditText;

import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Entity.User;
import java.util.concurrent.Executors;

/*
 * ==========================================================
 * ⭐️ BUG FIX: Added the missing import for "Pattern" ⭐️
 * ==========================================================
 * NOTE:
 * I am re-adding this import because I accidentally deleted it.
 * This is needed for 'PASSWORD_PATTERN' to work.
 */
import java.util.regex.Pattern; // ⬅️ (RE-ADDED)
/*
 * (This is your original comment block)
 * ==========================================================
 * ⭐️ What & Why: Changes from the previous version ⭐️
 * ==========================================================
 * * 1.  (Original Comment) ... (Imports) ...
 * * 2.  (Original Comment) ... (loadUserData) ...
 * * 3.  (Original Comment) ... (setupListeners profile) ...
 * * 4.  (Original Comment) ... (profile methods) ...
 * * 5.  (Original Comment) ... (setupListeners password) ...
 * * 6.  (Original Comment) ... (password methods) ...
 * * 7.  (Original Comment) ... (Refactored Logout Logic) ...
 * * 8.  (Original Comment) ... (showLogoutConfirmationDialog fix) ...
 * *
 * * 9.  (BUG FIX) Re-added 'PASSWORD_PATTERN' variable:
 * * Why? (This is the fix for your "Cannot resolve symbol" error).
 * * I accidentally deleted this variable in the last update.
 * * It is now back, and all password validation logic
 * * will work correctly again.
 */
public class SettingFragment extends Fragment {

    // (All your variable declarations remain unchanged)
    private RelativeLayout settingUserProfileCard, settingLanguage, settingChangePassword, settingPrivacyPolicy;
    private TextView textUserName, textUserId, textSelectedLanguage, settingLogout;
    private ImageView imageAvatar;
    private ActivityResultLauncher<Intent> avatarPickerLauncher;

    // (This is your original Avatar interface)
    public interface OnAvatarChangeListener {
        void onAvatarChanged(Uri newUri);
    }
    private OnAvatarChangeListener mAvatarListener; // Renamed for clarity

    // (This is your original Logout interface)
    public interface OnLogoutListener {
        void onLogoutRequested();
    }
    private OnLogoutListener mLogoutListener;

    private String currentUsername = "Guest";
    private String currentUserId = "N/A";
    private String currentUserType = "guest";

    // ⬇️ 更改: 重新添加了您在 RegisterFragment 中使用的密码验证规则
    /*
     * ==========================================================
     * ⭐️ BUG FIX: Re-Added the missing 'PASSWORD_PATTERN' ⭐️
     * ==========================================================
     * NOTE:
     * This is the fix for the "Cannot resolve symbol 'PASSWORD_PATTERN'" error.
     * I accidentally deleted this. It is now restored.
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])" +  // At least one letter
                    "(?=.*\\d)" +         // At least one number
                    "(?=.*[^A-Za-z0-9])" + // At least one symbol
                    ".{8,12}$"            // Length must be 8 to 12
    );
    // ⬆️ 更改结束 ⬆️

    // (All your methods from onAttach to initAvatarPicker remain unchanged)
    // ... (Omitted for brevity) ...
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnAvatarChangeListener) {
            mAvatarListener = (OnAvatarChangeListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnAvatarChangeListener");
        }
        if (context instanceof OnLogoutListener) {
            mLogoutListener = (OnLogoutListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnLogoutListener");
        }
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_setting, container, false);
        initViews(view);
        loadUserDataFromActivity();
        textUserName.setText(currentUsername);

        // (This is your requested change: "Your ID:")
        textUserId.setText(getString(R.string.setting_label_your_id) + " " + currentUserId);

        initAvatarPicker();
        setupListeners();
        loadSavedLanguageDisplay();
        loadCurrentAvatar();
        return view;
    }
    private void initViews(View view) {
        settingUserProfileCard = view.findViewById(R.id.setting_user_profile_card);
        imageAvatar = view.findViewById(R.id.image_avatar);
        textUserName = view.findViewById(R.id.text_user_name);
        textUserId = view.findViewById(R.id.text_user_id);
        settingLanguage = view.findViewById(R.id.setting_language);
        textSelectedLanguage = view.findViewById(R.id.text_selected_language);
        settingChangePassword = view.findViewById(R.id.setting_change_password);
        settingPrivacyPolicy = view.findViewById(R.id.setting_privacy_policy);
        settingLogout = view.findViewById(R.id.setting_logout);
    }
    private void loadUserDataFromActivity() {
        if (getActivity() instanceof SecondaryActivity) {
            SecondaryActivity activity = (SecondaryActivity) getActivity();
            currentUsername = activity.getUsername();
            currentUserId = activity.getStudentId();
            currentUserType = activity.getUserType();
            if (currentUsername == null || currentUsername.isEmpty()) {
                currentUsername = "Guest";
            }
            if (currentUserId == null || currentUserId.isEmpty()) {
                currentUserId = "N/A";
            }
            if (currentUserType == null || currentUserType.isEmpty()) {
                currentUserType = "guest";
            }
        } else {
            currentUsername = "Guest";
            currentUserId = "N/A";
            currentUserType = "guest";
        }
    }
    private void initAvatarPicker() {
        avatarPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Glide.with(requireContext()).load(imageUri).into(imageAvatar);
                            showToast(getString(R.string.toast_avatar_updated));
                            if (mAvatarListener != null) {
                                mAvatarListener.onAvatarChanged(imageUri);
                            }
                        }
                    }
                });
    }

    // (Your setupListeners method remains unchanged)
    private void setupListeners() {
        if (currentUserType.equals("guest")) {
            settingUserProfileCard.setEnabled(false);
            settingLanguage.setEnabled(false);
            settingChangePassword.setEnabled(false);
            settingPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicyDialog());
            settingLogout.setVisibility(View.GONE);
        }
        else {
            settingUserProfileCard.setOnClickListener(v -> showProfileOptionsDialog());
            settingLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
            settingChangePassword.setOnClickListener(v -> showChangePasswordDialog());
            settingPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicyDialog());
            settingLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
        }
    }

    // (All your "Change Profile/Password" methods remain unchanged)
    // ... (Omitted for brevity) ...
    private void showProfileOptionsDialog() {
        if (getContext() == null) return;

        // 1️⃣ 载入自定义布局
        View view = LayoutInflater.from(getContext()).inflate(R.layout.design_dialog_select, null);

        // 2️⃣ 获取布局控件
        TextView title = view.findViewById(R.id.profile_option_title);
        MaterialButton btnOption1 = view.findViewById(R.id.button_avatar);
        MaterialButton btnOption2 = view.findViewById(R.id.button_name);
        MaterialButton btnOption3 = view.findViewById(R.id.button_id);
        MaterialButton btnCancel = view.findViewById(R.id.button_cancel);

        // 3️⃣ 创建圆角 AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(true)
                .create();

        // 4️⃣ 设置标题
        btnOption1.setText(R.string.setting_profile_opt_avatar);
        btnOption2.setText(R.string.setting_profile_opt_name);
        btnOption3.setText(R.string.setting_profile_opt_id);
        btnCancel.setText(R.string.dialog_cancel);
        title.setText(R.string.setting_profile_options_title);

        // 5️⃣ 根据用户类型动态显示按钮
        btnOption1.setVisibility("guest".equalsIgnoreCase(currentUserType) ? View.GONE : View.VISIBLE);
        btnOption2.setVisibility(View.VISIBLE);
        btnOption3.setVisibility(View.VISIBLE);

        // 6️⃣ 设置按钮点击事件
        btnOption1.setOnClickListener(v -> {
            openGalleryForAvatar();
            dialog.dismiss();
        });

        btnOption2.setOnClickListener(v -> {
            showChangeNameDialog();
            dialog.dismiss();
        });

        btnOption3.setOnClickListener(v -> {
            copyStudentIdToClipboard();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 7️⃣ 显示 Dialog
        dialog.show();
    }




    private void showChangeNameDialog() {
        if (getContext() == null) return;

        // 1️⃣ 载入自定义布局
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.design_dialog_input, null);

        // 2️⃣ 获取布局控件
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        EditText editName = view.findViewById(R.id.dialog_input_1);
        EditText edittext2 = view.findViewById(R.id.dialog_input_2); // 可选，默认为 GONE
        EditText edittext3 = view.findViewById(R.id.dialog_input_3); // 可选，默认为 GONE
        MaterialButton btnConfirm = view.findViewById(R.id.button_confirm);
        MaterialButton btnCancel = view.findViewById(R.id.button_cancel);

        // 3️⃣ 设置标题和初始值
        dialogTitle.setText(R.string.setting_change_name_title);
        editName.setHint(R.string.setting_change_name_hint);
        editName.setText(currentUsername);
        editName.setSelection(currentUsername.length());

        // 4️⃣ 创建 AlertDialog 并套用圆角样式
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(false)
                .create();

        // 5️⃣ 按钮功能
        btnConfirm.setOnClickListener(v -> {
            String newName = editName.getText().toString().trim();
            if (newName.isEmpty()) {
                showToast(getString(R.string.toast_name_empty));
                return;
            }
            if (!newName.equals(currentUsername)) {
                updateUsernameInDatabase(newName);
            }
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 6️⃣ 显示 Dialog
        dialog.show();
    }

    private void updateUsernameInDatabase(String newName) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = db.userDao().findByUserTypeAndUserid(currentUserType, currentUserId);
            if (user != null) {
                user.username = newName;
                db.userDao().update(user);
                SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Activity.MODE_PRIVATE);
                String oldKey = "avatar_uri_" + currentUsername;
                String newKey = "avatar_uri_" + newName;
                String avatarUri = prefs.getString(oldKey, null);
                SharedPreferences.Editor editor = prefs.edit();
                if (avatarUri != null) {
                    editor.putString(newKey, avatarUri);
                    editor.remove(oldKey);
                }
                editor.apply();
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (getActivity() instanceof SecondaryActivity) {
                        ((SecondaryActivity) getActivity()).setUserInfo(newName, currentUserType);
                    }
                    currentUsername = newName;
                    textUserName.setText(currentUsername);
                    showToast(getString(R.string.toast_name_updated));
                });
            } else {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    showToast(getString(R.string.toast_user_not_found));
                });
            }
        });
    }
    private void copyStudentIdToClipboard() {
        if (getContext() == null) return;
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getString(R.string.setting_label_student_id_clipboard), currentUserId);
        clipboard.setPrimaryClip(clip);
        showToast(getString(R.string.toast_id_copied));
    }
    private void showChangePasswordDialog() {
        if (getContext() == null) return;

        // 1️⃣ 载入自定义布局
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.design_dialog_input, null);

        // 2️⃣ 获取布局控件
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        EditText editOldPass = view.findViewById(R.id.dialog_input_1);           // 主输入框
        EditText editNewPass = view.findViewById(R.id.dialog_input_2);    // 额外输入框1
        EditText editConfirmPass = view.findViewById(R.id.dialog_input_3); // 额外输入框2
        MaterialButton btnConfirm = view.findViewById(R.id.button_confirm);
        MaterialButton btnCancel = view.findViewById(R.id.button_cancel);

        // 3️⃣ 设置标题和提示
        dialogTitle.setText(R.string.setting_change_pass_title);

        editOldPass.setHint(R.string.setting_change_pass_hint_old);
        editOldPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        editNewPass.setVisibility(View.VISIBLE);
        editNewPass.setHint(R.string.setting_change_pass_hint_new);
        editNewPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        editConfirmPass.setVisibility(View.VISIBLE);
        editConfirmPass.setHint(R.string.setting_change_pass_hint_confirm);
        editConfirmPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // 4️⃣ 创建圆角 Dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(false)
                .create();

        // 5️⃣ 设置按钮事件
        btnConfirm.setOnClickListener(v -> {
            String oldPass = editOldPass.getText().toString().trim();
            String newPass = editNewPass.getText().toString().trim();
            String confirmPass = editConfirmPass.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                showToast(getString(R.string.toast_fill_all_fields));
                return;
            }

            if (!PASSWORD_PATTERN.matcher(newPass).matches()) {
                showToast(getString(R.string.toast_password_invalid));
                return;
            }

            if (!newPass.equals(confirmPass)) {
                showToast(getString(R.string.toast_password_no_match));
                return;
            }

            updatePasswordInDatabase(oldPass, newPass);
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 6️⃣ 显示 Dialog
        dialog.show();
    }

    private void updatePasswordInDatabase(String oldPass, String newPass) {
        if (getContext() == null) return;
        AppDatabase db = AppDatabase.getInstance(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            User user = db.userDao().findByUserTypeAndUserid(currentUserType, currentUserId);
            if (user == null) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> showToast(getString(R.string.toast_user_not_found)));
                return;
            }
            if (!user.password.equals(oldPass)) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> showToast(getString(R.string.toast_password_incorrect)));
                return;
            }
            user.password = newPass;
            db.userDao().update(user);
            SharedPreferences prefs = requireActivity().getSharedPreferences("LoginPrefs", Activity.MODE_PRIVATE);
            if (prefs.getBoolean("savePassword", false)) {
                String keyToUpdate = currentUserType.equals("student") ? "studentPassword" : "adminPassword";
                prefs.edit().putString(keyToUpdate, newPass).apply();
            }
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                showToast(getString(R.string.toast_password_updated));
            });
        });
    }
    private void openGalleryForAvatar() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        avatarPickerLauncher.launch(intent);
    }
    private void showLanguageSelectionDialog() {
        if (getContext() == null) return;

        final String[] languages = {"Bahasa Melayu", "English", "中文 (简体)"};
        final String[] codes = {"ms", "en", "zh"};

        // 1️⃣ 载入自定义布局
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.design_dialog_select, null);

        // 2️⃣ 获取控件
        TextView dialogTitle = view.findViewById(R.id.profile_option_title);
        MaterialButton btnOption1 = view.findViewById(R.id.button_avatar);
        MaterialButton btnOption2 = view.findViewById(R.id.button_name);
        MaterialButton btnOption3 = view.findViewById(R.id.button_id);

        // 隐藏取消按钮
        view.findViewById(R.id.button_cancel).setVisibility(View.GONE);


        // 3️⃣ 设置标题
        dialogTitle.setText(R.string.setting_language_dialog_title);

        // 4️⃣ 设置语言选项文本
        btnOption1.setText(languages[0]);
        btnOption2.setText(languages[1]);
        btnOption3.setText(languages[2]);

        // 5️⃣ 创建圆角 Dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(true)
                .create();

        // 6️⃣ 按钮点击事件
        btnOption1.setOnClickListener(v -> {
            setAppLocale(codes[0]);
            textSelectedLanguage.setText(languages[0]);
            dialog.dismiss();
        });

        btnOption2.setOnClickListener(v -> {
            setAppLocale(codes[1]);
            textSelectedLanguage.setText(languages[1]);
            dialog.dismiss();
        });

        btnOption3.setOnClickListener(v -> {
            setAppLocale(codes[2]);
            textSelectedLanguage.setText(languages[2]);
            dialog.dismiss();
        });


        // 7️⃣ 显示 Dialog
        dialog.show();
    }

    private void setAppLocale(String langCode) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppSettings", Activity.MODE_PRIVATE);
        prefs.edit().putString("AppLanguage", langCode).apply();
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        requireActivity().getResources().updateConfiguration(
                config,
                requireActivity().getResources().getDisplayMetrics()
        );
        requireActivity().recreate();
    }
    private void loadSavedLanguageDisplay() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppSettings", Activity.MODE_PRIVATE);
        String code = prefs.getString("AppLanguage", "en");
        switch (code) {
            case "ms":
                textSelectedLanguage.setText("Bahasa Melayu");
                break;
            case "zh":
                textSelectedLanguage.setText("中文 (简体)");
                break;
            default:
                textSelectedLanguage.setText("English");
        }
    }
    private void showPrivacyPolicyDialog() {
        if (getContext() == null) return;

        // 1️⃣ 载入自定义布局
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.design_dialog_privacy, null);

        // 2️⃣ 获取控件
        TextView title = view.findViewById(R.id.dialog_title);
        TextView message = view.findViewById(R.id.dialog_message);
        MaterialButton btnClose = view.findViewById(R.id.button_close);

        // 3️⃣ 可选：根据需要动态设置标题或内容
        title.setText(R.string.privacy_title);
        message.setText(R.string.privacy_message);

        // 4️⃣ 创建圆角 Dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(true)
                .create();

        // 5️⃣ 按钮事件
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 6️⃣ 显示 Dialog
        dialog.show();
    }


    /*
     * (This is your original comment, translated)
     * ==========================================================
     * CHANGE 1: Fix Logout Dialog
     * ==========================================================
     */
    private void showLogoutConfirmationDialog() {
        /*
         * (This is your previous comment)
         * ==========================================================
         * ⬇️ 更改: 删除了 "Are you sure?" 弹窗 ⬇️
         * ==========================================================
         */

        // (This is your original "immediate logout" code)
        if (mLogoutListener != null) {
            mLogoutListener.onLogoutRequested();
        }
    }

    /*
     * (This is your previous comment, translated)
     * ==========================================================
     * CHANGE 2: Add Logout Logic
     * ==========================================================
     * NOTE: This method has been "REMOVED" to prevent duplicate code.
     */
    // (REMOVED) private void logoutUser() { ... }


    /*
     * (This is your previous comment, translated)
     * ==========================================================
     * CHANGE 3: Fix loadCurrentAvatar (SettingFragment)
     * ==========================================================
     */
    private void loadCurrentAvatar() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("UserPrefs", Activity.MODE_PRIVATE);
        String uriString = prefs.getString("avatar_uri_" + currentUsername, null);

        if (uriString != null) {
            try {
                Glide.with(requireContext())
                        .load(Uri.parse(uriString))
                        .into(imageAvatar);
            } catch (Exception e) {
                showToast(getString(R.string.toast_avatar_load_fail));
            }
        }
    }

    // ⬇️ 更改: 恢复到您原有的 onDetach ⬇️
    @Override
    public void onDetach() {
        super.onDetach();
        mAvatarListener = null;
        mLogoutListener = null;
    }
    // ⬆️ 更改结束 ⬆️

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}