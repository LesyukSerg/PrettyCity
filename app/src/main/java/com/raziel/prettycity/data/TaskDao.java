package com.raziel.prettycity.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.raziel.prettycity.data.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    void insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("SELECT * FROM tasks ORDER BY priority DESC")
    List<Task> getAll();

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getById(int taskId);

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    LiveData<List<Task>> getAllTasks();

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteById(int id);
}
