package com.name.ccf.Data.Dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.name.ccf.Data.Entity.Help;
import java.util.List;

/**
 * Data Access Object (DAO) for the Help table.
 * Defines database operations for Help entities.
 */
@Dao
public interface HelpDAO {

    /**
     * Inserts a new help request into the database.
     * If a conflict occurs (e.g., same ID), it replaces the old entry.
     * @param help The Help object to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Help help);

    /**
     * Fetches all help requests from the database.
     * @return A list of all Help objects.
     */
    @Query("SELECT * FROM help")
    List<Help> getAll();

    /**
     * Finds all help requests submitted by a specific user.
     * @param userId The ID (userid) of the user to search for.
     * @return A list of Help objects matching the user ID.
     */
    @Query("SELECT * FROM help WHERE userid = :userId")
    List<Help> findByUserId(String userId);

    /**
     * Deletes a specific help request from the database.
     * @param help The Help object to delete.
     */
    @Delete
    void delete(Help help);

    /**
     * Deletes all entries from the help table.
     */
    @Query("DELETE FROM help")
    void deleteAll();
}