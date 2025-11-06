package com.name.ccf.Data.Database;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.name.ccf.Data.Dao.FeedbackDao;
import com.name.ccf.Data.Dao.UserDao;
import com.name.ccf.Data.Entity.Feedback;
import com.name.ccf.Data.Entity.User;

@Database(entities = {User.class, Feedback.class}, version = 4) // 升到 4
public abstract class AppDatabase extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract FeedbackDao feedbackDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {

                    // Migration 3 -> 4
                    Migration MIGRATION_3_4 = new Migration(3, 4) {
                        @Override
                        public void migrate(@NonNull SupportSQLiteDatabase database) {
                            // Feedback 表，确保所有列存在且有默认值
                            database.execSQL(
                                    "CREATE TABLE IF NOT EXISTS `feedback_new` (" +
                                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                            "`username` TEXT DEFAULT 'unknown', " +
                                            "`dishname` TEXT DEFAULT '', " +
                                            "`rating` REAL NOT NULL DEFAULT 0, " +
                                            "`category` TEXT DEFAULT '', " +
                                            "`tag` TEXT DEFAULT '', " +
                                            "`imageUri` TEXT DEFAULT '', " +
                                            "`feedbackText` TEXT DEFAULT '', " +
                                            "`timestamp` INTEGER NOT NULL DEFAULT 0)"
                            );

                            // 迁移旧数据到新表
                            database.execSQL(
                                    "INSERT INTO feedback_new (id, username, dishname, rating, category, tag, imageUri, feedbackText, timestamp) " +
                                            "SELECT id, username, dishname, rating, category, tag, imageUri, feedbackText, timestamp FROM feedback"
                            );

                            // 删除旧表并重命名
                            database.execSQL("DROP TABLE feedback");
                            database.execSQL("ALTER TABLE feedback_new RENAME TO feedback");

                            // User 表，如果有缺失列，可以添加默认值（这里假设都已经存在，不需要 ALTER）
                        }
                    };

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "ccf_db")
                            .addMigrations(MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
