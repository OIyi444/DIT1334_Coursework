package com.name.ccf.Data.Dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy; // Import
import androidx.room.Query;

import com.name.ccf.Data.Entity.Feedback;

import java.util.List;

@Dao
public interface FeedbackDao {

    // --- GEMINI FIX: Added OnConflictStrategy for stability ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Feedback feedback);

    // --- GEMINI FIX: Added for Firebase full sync ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Feedback> feedbackList);

    // --- GEMINI FIX: Added for Firebase full sync ---
    @Query("DELETE FROM feedback")
    void deleteAll();


    // (Original methods below)
    @Query("SELECT * FROM feedback ORDER BY timestamp DESC")
    List<Feedback> getAllFeedback();

    @Query("DELETE FROM feedback WHERE timestamp < :timeLimit")
    void deleteOldFeedback(long timeLimit);

    // This is your filter method
    @Query("SELECT * FROM feedback " +
            "WHERE (:category = 'All' OR category = :category) " +
            "AND (:rating = -1 OR rating = :rating) " +
            "ORDER BY timestamp DESC")
    List<Feedback> getFilteredFeedback(String category, float rating);
}