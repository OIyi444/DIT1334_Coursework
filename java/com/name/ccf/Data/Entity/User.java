package com.name.ccf.Data.Entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;
    public String password;
    public String usertype;
    public String userid; // 学号或管理员编号

    public User(String username, String password, String usertype, String userid) {
        this.username = username;
        this.password = password;
        this.usertype = usertype;
        this.userid = userid;
    }
}
