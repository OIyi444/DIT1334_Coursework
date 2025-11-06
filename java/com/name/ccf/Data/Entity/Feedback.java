package com.name.ccf.Data.Entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "feedback")
public class Feedback {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String username;
    public String dishname;
    public float rating;
    public String category;
    public String tag;
    public String imageUri;
    public String feedbackText;
    public long timestamp;

    public Feedback(String username,String dishname, float rating, String category,
                    String tag, String imageUri, String feedbackText, long timestamp) {
        this.username = username;
        this.dishname = dishname;
        this.rating = rating;
        this.category = category;
        this.tag = tag;
        this.imageUri = imageUri;
        this.feedbackText = feedbackText;
        this.timestamp = timestamp;
    }
}
