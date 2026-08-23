package com.helloapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour, minute")
    List<AlarmEntity> getAll();

    @Query("SELECT * FROM alarms WHERE id = :id")
    AlarmEntity getById(long id);

    @Query("SELECT * FROM alarms WHERE enabled = 1")
    List<AlarmEntity> getEnabled();

    @Insert
    long insert(AlarmEntity alarm);

    @Update
    void update(AlarmEntity alarm);

    @Delete
    void delete(AlarmEntity alarm);

    @Query("DELETE FROM alarms WHERE id = :id")
    void deleteById(long id);
}
