package com.helloapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TodoDao {

    @Query("SELECT * FROM todos ORDER BY id DESC")
    List<TodoEntity> getAll();

    @Insert
    long insert(TodoEntity todo);

    @Update
    void update(TodoEntity todo);

    @Delete
    void delete(TodoEntity todo);

    @Query("DELETE FROM todos WHERE id = :id")
    void deleteById(long id);
}
