package com.name.ccf.Data.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.name.ccf.Data.Entity.User;
import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void insert(User user);

    // 注册时用：用户名+类型+学号判断是否存在，大小写不敏感
    @Query("SELECT * FROM users WHERE username = :username AND usertype = :usertype AND userid = :userid COLLATE NOCASE LIMIT 1")
    User findByUsernameUserTypeAndUserid(String username, String usertype, String userid);

    // 登录时用：类型+学号，大小写不敏感
    @Query("SELECT * FROM users WHERE usertype = :usertype AND userid = :userid COLLATE NOCASE LIMIT 1")
    User findByUserTypeAndUserid(String usertype, String userid);

    @Query("SELECT * FROM users")
    List<User> getAll();

    @Update
    void update(User user);
}
