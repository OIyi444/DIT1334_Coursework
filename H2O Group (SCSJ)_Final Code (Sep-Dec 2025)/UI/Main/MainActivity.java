package com.name.ccf.UI.Main;

import android.os.Bundle;
import android.util.Log;
import android.content.Context; // ⬇️ Change: Added Context import
import android.content.SharedPreferences; // ⬇️ Change: Added SharedPreferences import
import java.util.Locale; // ⬇️ Change: Added Locale import
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.name.ccf.R;
// --- GEMINI FIX: Import Database, Repository, and Executor ---
import com.name.ccf.Data.Database.AppDatabase;
import com.name.ccf.Data.Repository.UserRepository;
import java.util.concurrent.Executors;
// --- End of GEMINI FIX ---

import com.name.ccf.UI.Main.RegisterFragment;
import com.name.ccf.UI.Main.LoginFragment;
import com.name.ccf.UI.Main.SelectionFragment;

/*
 * ==========================================================
 * ⭐️ What & Why: Changes from the previous version ⭐️
 * ==========================================================
 * * 1.  (Original Comment) ... (Animation fix) ...
 * *
 * * 2.  (NEW CHANGE) Added 'applySavedLanguage()' and 'attachBaseContext()':
 * * Why? (This is the fix for your "Language Switch" bug).
 * * As your "Launcher" Activity, this file *must* load the saved
 * * language *before* 'onCreate' (before the UI is shown).
 * * 'attachBaseContext' runs first, it calls 'applySavedLanguage',
 * * which reads the "AppSettings" file (just like SettingFragment)
 * * and forces the app to use the correct strings.xml file
 * * from the very beginning.
 */

public class MainActivity extends AppCompatActivity {

    // (Your variables remain unchanged)
    private long lastBackPressedTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000;

    // ⬇️ Change: Added these two methods to fix the language Bug ⬇️
    /**
     * This method runs *before* onCreate.
     * It calls applySavedLanguage to get the "new" context
     * with the correct language, and applies it.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(applySavedLanguage(newBase));
    }

    /**
     * This method reads the saved language from "AppSettings"
     * (the *same* file SettingFragment uses) and returns
     * a new Context configured with that language.
     */
    private Context applySavedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", MODE_PRIVATE);
        String langCode = prefs.getString("AppLanguage", "en"); // Default English
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);

        android.content.res.Configuration config = new android.content.res.Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
    // ⬆️ End of change ⬆️


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // (We "removed" the applySavedLanguage() call from here)
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // --- GEMINI FIX: Sync data from Firebase when app starts ---
        // We do this here, at the very beginning, to ensure
        // the local database is up-to-date before any fragment loads.
        syncUserDataFromFirebase();
        // --- End of GEMINI FIX ---


        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SplashScreenFragment())
                    .commit();
        }

        // (Your original comment, translated)
        // Handle navigation parameters
        if (getIntent() != null) {
            String openFragment = getIntent().getStringExtra("openFragment");
            String showFragment = getIntent().getStringExtra("showFragment"); // ✅ Added

            if ("login".equals(openFragment)) {
                replaceFragment(new LoginFragment(), false);
            } else if ("register".equals(openFragment)) {
                replaceFragment(new RegisterFragment(), false);
            }
            // ✅ Added: Show SelectionFragment after logout from SecondaryActivity
            else if ("selection".equals(showFragment)) {
                replaceFragment(new SelectionFragment(), false);
            }
        }
    }


    // --- GEMINI FIX: Added this new method to sync data ---
    /**
     * Implements the "Firebase as Single Source of Truth" strategy.
     * Fetches ALL users from Firebase and uses that list
     * to completely overwrite the local Room database.
     */
    private void syncUserDataFromFirebase() {
        AppDatabase db = AppDatabase.getInstance(this);
        UserRepository userRepo = new UserRepository();

        // Run network and DB operations on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {

            // 1. Fetch all users from Firebase
            // (The callback 'onFetched' runs on the Main Thread)
            userRepo.fetchUsers(firebaseUserList -> {

                // 2. Once fetched, we must use *another* background thread
                //    to perform database I/O (delete and insert)
                Executors.newSingleThreadExecutor().execute(() -> {

                    // 3. Delete all local users
                    db.userDao().deleteAll();

                    // 4. Insert the fresh list from Firebase
                    db.userDao().insertAll(firebaseUserList);

                    Log.d("MainActivity_Sync", "Successfully synced " + firebaseUserList.size() + " users from Firebase to Room.");
                });
            });

            // NOTE: You would do the exact same thing for Feedback data here.
            // 1. Update FeedbackDao (add deleteAll, insertAll)
            // 2. Call feedbackRepo.fetchAllFeedback(...)
            // 3. In the callback, delete and insert all feedback.
        });
    }
    // --- End of GEMINI FIX ---


    /*
     * (This is your original comment)
     * ==========================================================
     * CHANGE 1: Fix page transition animation (MainActivity)
     * ==========================================================
     */
    public void replaceFragment(Fragment fragment, boolean addToBackStack) {
        if (addToBackStack) {
            getSupportFragmentManager().beginTransaction()
                    // ✅ (Your original animation fix)
                    .setCustomAnimations(
                            R.anim.smooth_fade_in,
                            R.anim.smooth_fade_out,
                            R.anim.smooth_fade_in,
                            R.anim.smooth_fade_out
                    )
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            getSupportFragmentManager().beginTransaction()
                    // ✅ (Your original animation fix)
                    .setCustomAnimations(
                            R.anim.smooth_fade_in,
                            R.anim.smooth_fade_out,
                            R.anim.smooth_fade_in,
                            R.anim.smooth_fade_out
                    )
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    // (Your onBackPressed method remains unchanged)
    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (current instanceof SelectionFragment) {
            long now = System.currentTimeMillis();
            if (now - lastBackPressedTime < BACK_PRESS_INTERVAL) {
                finishAffinity();
            } else {
                lastBackPressedTime = now;
                // ⬅️ (Using R.string for language)
                Toast.makeText(this, R.string.press_back_exit, Toast.LENGTH_SHORT).show();
            }
        }
        else if (current instanceof LoginFragment || current instanceof RegisterFragment) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SelectionFragment())
                    .commit();
        }
        else {
            super.onBackPressed();
        }
    }
}