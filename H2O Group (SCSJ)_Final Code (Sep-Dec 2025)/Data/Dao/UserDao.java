package com.name.ccf.Data.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy; // 1. 导入 OnConflictStrategy
import androidx.room.Query;
import androidx.room.Update;
import com.name.ccf.Data.Entity.User;
import java.util.List;

@Dao
public interface UserDao {

flict = OnConflictStrategy.REPLACE)
    void insertAll(List<User> users);

    @Query("DELETE FROM users")
    void deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(User user);

    @Query("SELECT * FROM users WHERE username = :username AND usertype = :usertype AND userid = :userid COLLATE NOCASE LIMIT 1")
    User findByUsernameUserTypeAndUserid(String username, String usertype, String userid);

    // Used for login: find by type + userid, case-insensitive
    @Query("SELECT * FROM users WHERE usertype = :usertype AND userid = :userid COLLATE NOCASE LIMIT 1")
    User findByUserTypeAndUserid(String usertype, String userid);

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Update
    void update(User user);

    // Required for the first step of "Change Password" and "Login" features:
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);
}