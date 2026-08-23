package com.helloapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "todos")
public class TodoEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Task text. */
    public String text;

    /** Whether the task is marked done. */
    public boolean done;
}
