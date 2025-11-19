package com.name.ccf.Data.Database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.name.ccf.Data.Dao.FeedbackDao;
import com.name.ccf.Data.Dao.UserDao;
import com.name.ccf.Data.Dao.HelpDAO; // (Import remains correct)
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.Data.Entity.User;
import com.name.ccf.Data.Entity.Help; // (Import remains correct)

// --- ⬇️ GEMINI FIX: MUST update version from 5 to 6 ---
@Database(entities = {User.class, Feedback.class, Help.class}, version = 6) // <-- 1. 升级到 version = 6
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract FeedbackDao feedbackDao();
    public abstract HelpDAO helpDAO(); // (This remains correct)

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ccf_db")
                            // (This will delete all old data and recreate the
                            // database with the new 'help.imageUri' column)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}