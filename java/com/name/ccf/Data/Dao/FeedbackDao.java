package com.name.ccf.Data.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.name.ccf.Data.Entity.Feedback;

import java.util.List;

@Dao
public interface FeedbackDao {

    @Insert
    void insert(Feedback feedback);

    @Query("SELECT * FROM feedback ORDER BY timestamp DESC")
    List<Feedback> getAllFeedback();

    @Query("DELETE FROM feedback WHERE timestamp < :timeLimit")
    void deleteOldFeedback(long timeLimit);

    // 新增：按 category 和 rating 筛选
    @Query("SELECT * FROM feedback " +
            "WHERE (:category = 'All' OR category = :category) " +
            "AND (:rating = -1 OR rating = :rating) " +
            "ORDER BY timestamp DESC")
    List<Feedback> getFilteredFeedback(String category, float rating);
}

