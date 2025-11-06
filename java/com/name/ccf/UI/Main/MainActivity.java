package com.name.ccf.UI.Main;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.name.ccf.R;
import com.name.ccf.UI.Main.RegisterFragment;
import com.name.ccf.UI.Main.LoginFragment;
import com.name.ccf.UI.Main.SelectionFragment;

public class MainActivity extends AppCompatActivity {

    private long lastBackPressedTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000; // 2秒内双击退出

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 默认载入 Splash
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SplashScreenFragment())
                    .commit();
        }

        // 处理跳转参数
        if (getIntent() != null) {
            String openFragment = getIntent().getStringExtra("openFragment");
            String showFragment = getIntent().getStringExtra("showFragment"); // ✅ 新增

            if ("login".equals(openFragment)) {
                replaceFragment(new LoginFragment(), false);
            } else if ("register".equals(openFragment)) {
                replaceFragment(new RegisterFragment(), false);
            }
            // ✅ 新增：从 SecondaryActivity logout 后显示 SelectionFragment
            else if ("selection".equals(showFragment)) {
                replaceFragment(new SelectionFragment(), false);
            }
        }
    }

    public void replaceFragment(Fragment fragment, boolean addToBackStack) {
        if (addToBackStack) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        // 如果在 SelectionFragment：两次返回退出
        if (current instanceof SelectionFragment) {
            long now = System.currentTimeMillis();
            if (now - lastBackPressedTime < BACK_PRESS_INTERVAL) {
                finishAffinity(); // 完全退出 App
            } else {
                lastBackPressedTime = now;
            }
        }
        // 如果在 Login 或 RegisterFragment：返回 SelectionFragment
        else if (current instanceof LoginFragment || current instanceof RegisterFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SelectionFragment())
                    .commit();
        }
        // 其他情况用默认逻辑
        else {
            super.onBackPressed();
        }
    }
}