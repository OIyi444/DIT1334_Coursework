package com.name.ccf.UI.Second;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.name.ccf.UI.Main.MainActivity;
import com.name.ccf.R;

public class SecondaryActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private ImageView imgUser;

    private String username = "Guest";
    private String userType = "guest"; // "student" 或 "admin"

    private long lastBackPressedTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btn_menu);
        imgUser = findViewById(R.id.img_user);

        Intent intent = getIntent();
        if (intent != null) {
            String name = intent.getStringExtra("username");
            String type = intent.getStringExtra("userType");
            if (name != null) username = name;
            if (type != null) userType = type;
        }

        // 根据用户类型加载菜单
        if (userType.equalsIgnoreCase("admin")) {
            navigationView.inflateMenu(R.menu.admin_drawer_menu);
        } else {
            navigationView.inflateMenu(R.menu.student_drawer_menu);
        }

        navigationView.setNavigationItemSelectedListener(this);

        btnMenu.setOnClickListener(v -> toggleDrawer());
        imgUser.setOnClickListener(v ->
                Toast.makeText(this, "Profile: " + username, Toast.LENGTH_SHORT).show()
        );

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (id == R.id.nav_ranking) {
            selectedFragment = new RankingFragment();
        } else if (id == R.id.nav_feedback) {
            selectedFragment = new FeedbackFragment();
        } else if (id == R.id.nav_chart) {
            selectedFragment = new ChartFragment();
        } else if (id == R.id.nav_logout) {
            showLogoutDialog();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (selectedFragment != null) loadFragment(selectedFragment);
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to log out?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> logoutUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logoutUser() {
        // ✅ 清除登录状态
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // ✅ 回到主界面的 SelectionFragment
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("showFragment", "selection"); // 用这个标志告诉MainActivity显示SelectionFragment
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastBackPressedTime < BACK_PRESS_INTERVAL) {
            finishAffinity();
        } else {
            lastBackPressedTime = now;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
    }

    public void setUserInfo(String username, String userType) {
        this.username = username;
        this.userType = userType;
    }

    public String getUsername() {
        return username;
    }

    public String getUserType() {
        return userType;
    }
}
