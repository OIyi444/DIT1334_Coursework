package com.name.ccf.Data.Entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "help")
public class Help {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;
    public String userid;
    public String helptext;
    public String imageUri;

    public Help(String username, String userid, String helptext, String imageUri) {
        this.username = username;
        this.userid = userid;
        this.helptext = helptext;
        this.imageUri = imageUri;
    }
}
