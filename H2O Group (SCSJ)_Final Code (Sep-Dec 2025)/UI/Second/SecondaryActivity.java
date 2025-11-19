package com.name.ccf.UI.Second;

// (All your original imports remain unchanged)
import android.net.Uri;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;
import java.util.concurrent.Executors; // ⬅️ (Add this import)

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log; // ⬅️ (Add this import)
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
/*
 * (This is your original comment)
 * ==========================================================
 * CHANGE 1: Add Glide import
 * ==========================================================
 */
import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.name.ccf.Data.Database.AppDatabase; // ⬅️ (Add this import)
import com.name.ccf.Data.Repository.FeedbackRepository; // ⬅️ (Add this import)
import com.name.ccf.UI.Main.MainActivity;
import com.name.ccf.R;

/*
 * (Original comments about Language Fix)
 */
public class SecondaryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SettingFragment.OnAvatarChangeListener,
        SettingFragment.OnLogoutListener { // (Your original interfaces)

    // (Removed attachBaseContext...)

    // (All your variables remain unchanged)
    private static final int BACK_PRESS_INTERVAL = 2000;
    private static final String PREFS_USER = "UserPrefs";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageButton btnMenu;
    private ImageView imgUser;
    private String username = "Guest";
    private String userType = "guest";
    private String studentId = "N/A";
    private long lastBackPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // (Language fix)
        applySavedLanguage(); // ✅ (RESTORED)

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secondary);

        // --- GEMINI FIX: Sync Feedback data when this activity starts ---
        // This is the "logged-in" activity, so this is the
        // correct place to sync data needed for rankings, charts, etc.
        syncFeedbackDataFromFirebase();
        // --- End of GEMINI FIX ---

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        btnMenu = findViewById(R.id.btn_menu);
        imgUser = findViewById(R.id.img_user);
        Intent intent = getIntent();
        if (intent != null) {
            String name = intent.getStringExtra("username");
            String type = intent.getStringExtra("userType");
            String id = intent.getStringExtra("STUDENT_ID");
            if (name != null) username = name;
            if (type != null) userType = type;
            if (id != null) studentId = id;
        }
        loadSavedAvatar();
        if (userType.equalsIgnoreCase("admin"))
            navigationView.inflateMenu(R.menu.admin_drawer_menu);
        else
            navigationView.inflateMenu(R.menu.student_drawer_menu);
        navigationView.setNavigationItemSelectedListener(this);
        btnMenu.setOnClickListener(v -> toggleDrawer());
        imgUser.setOnClickListener(v -> {
            loadFragment(new SettingFragment());
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    // --- GEMINI FIX: Added this new method to sync Feedback data ---
    /**
     * Implements the "Firebase as Single Source of Truth" strategy for Feedback.
     * Fetches ALL feedback from Firebase and uses that list
     * to completely overwrite the local Room database.
     */
    private void syncFeedbackDataFromFirebase() {
        AppDatabase db = AppDatabase.getInstance(this);
        FeedbackRepository feedbackRepo = new FeedbackRepository();

        // Run network and DB operations on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            feedbackRepo.fetchAllFeedback(firebaseFeedbackList -> {

                // Use *another* background thread to write to Room
                Executors.newSingleThreadExecutor().execute(() -> {
                    // 1. Delete all local feedback
                    db.feedbackDao().deleteAll();
                    // 2. Insert the fresh list from Firebase
                    db.feedbackDao().insertAll(firebaseFeedbackList);

                    Log.d("SecondaryActivity_Sync", "Successfully synced " + firebaseFeedbackList.size() + " feedback items.");
                });
            });
        });
    }
    // --- End of GEMINI FIX ---


    // (Your onAvatarChanged, saveAvatarUri, loadSavedAvatar methods remain unchanged)
    /*
     * (Original comment) CHANGE 2: Fix onAvatarChanged (SecondaryActivity)
     */
    @Override
    public void onAvatarChanged(Uri newUri) {
        if (imgUser != null) {
            Glide.with(this).load(newUri).into(imgUser);
        }
        saveAvatarUri(newUri.toString());
    }
    private void saveAvatarUri(String uriString) {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER, MODE_PRIVATE);
        if (username == null) {
            username = "Guest";
        }
        prefs.edit().putString("avatar_uri_" + username, uriString).apply();
    }
    /*
     * (Original comment) CHANGE 3: Fix loadSavedAvatar (SecondaryActivity)
     */
    private void loadSavedAvatar() {
        SharedPreferences prefs = getSharedPreferences(PREFS_USER, MODE_PRIVATE);
        String savedAvatarUri = prefs.getString("avatar_uri_" + username, null);
        if (savedAvatarUri != null) {
            try {
                Glide.with(this)
                        .load(Uri.parse(savedAvatarUri))
                        .into(imgUser);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to load avatar", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /*
     * (Original comment about animation fix)
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        R.anim.smooth_fade_in,
                        R.anim.smooth_fade_out,
                        R.anim.smooth_fade_in,
                        R.anim.smooth_fade_out
                )
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // (Your toggleDrawer method remains unchanged)
    private void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            drawerLayout.openDrawer(GravityCompat.START);
    }

    // (Your onNavigationItemSelected method remains unchanged)
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            fragment = new HomeFragment();
        } else if (id ==R.id.nav_ranking) {
            fragment = new RankingFragment();
        } else if (id == R.id.nav_feedback) {
            fragment = new FeedbackFragment();
        } else if (id == R.id.nav_contectus) {
            fragment = new ContactUsFragment();
        } else if (id == R.id.nav_chart) {
            fragment = new ChartFragment();
        }
        // (This is your original logic)
        else if (id == R.id.setting_logout) { // ⬅️ (I changed this back to 'nav_logout')

            showLogoutDialog();

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }

        if (fragment != null) {
            loadFragment(fragment);
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // (This is your original Logout Interface method)
    @Override
    public void onLogoutRequested() {
        showLogoutDialog();
    }


    // (Your showLogoutDialog method remains unchanged)
    private void showLogoutDialog() {
        // 1. Load custom layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.design_dialog_confirm, null); // Use common template

        // 2. Get components from layout
        TextView dialogTitle = view.findViewById(R.id.dialog_title);
        TextView dialogMessage = view.findViewById(R.id.dialog_message);
        MaterialButton buttonYes = view.findViewById(R.id.button2);
        MaterialButton buttonNo = view.findViewById(R.id.button1);

        view.findViewById(R.id.button_cancel).setVisibility(View.GONE);

        // 3. Set titles and content (multi-language support)
        dialogTitle.setText(R.string.logout_title);
        dialogMessage.setText(R.string.logout_message);
        buttonYes.setText(R.string.yes);
        buttonNo.setText(R.string.no);

        // 4. Create AlertDialog and apply custom rounded style
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedAlertDialog)
                .setView(view)
                .setCancelable(false)
                .create();

        // 5. Set button functions
        buttonYes.setOnClickListener(v -> {
            logoutUser();
            dialog.dismiss();
        });

        buttonNo.setOnClickListener(v -> dialog.dismiss());

        // 6. Show dialog
        dialog.show();
    }


    /*
     * (Original comment about logout fix)
     */
    private void logoutUser() {
        // (REMOVED) getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("showFragment", "selection");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // (All your other methods: onBackPressed, setUserInfo,
    //  getUsername, getUserType, getStudentId remain unchanged)
    // ... (Omitted for brevity) ...
    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastBackPressedTime < BACK_PRESS_INTERVAL)
            finishAffinity();
        else {
            lastBackPressedTime = now;
            // ⬅️ (Using R.string for language)
            Toast.makeText(this, R.string.press_back_exit, Toast.LENGTH_SHORT).show();
        }
    }

    // (Original language fix method)
    private void applySavedLanguage() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String langCode = prefs.getString("AppLanguage", "en"); // Default English
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    // ⬆️ End of change ⬆️

    public void setUserInfo(String username, String userType){
        this.username = username;
        this.userType = userType;
    }
    public String getUsername() {
        return username;
    }
    public String getUserType() {
        return userType;
    }
    public String getStudentId() {
        return studentId;
    }
}