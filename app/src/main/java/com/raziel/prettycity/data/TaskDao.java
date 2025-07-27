package com.raziel.prettycity.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.raziel.prettycity.data.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("SELECT * FROM tasks WHERE deleted=0")
    LiveData<List<Task>> getAll();

    @Query("SELECT * FROM tasks WHERE deleted=0 AND id = :taskId")
    LiveData<Task> getById(int taskId);

    @Query("SELECT * FROM tasks WHERE deleted=0 ORDER BY id DESC")
    LiveData<List<Task>> getAllTasks();

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Task task);

    @Query("SELECT * FROM tasks WHERE synced = 0")
    List<Task> getUnsyncedTasks();

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("UPDATE tasks SET deleted = 1, synced = 0 WHERE id = :id")
    void markAsDeleted(int id);
}
