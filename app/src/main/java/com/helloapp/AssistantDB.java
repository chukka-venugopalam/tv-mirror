package com.helloapp;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {AlarmEntity.class, TodoEntity.class, NoteEntity.class}, version = 1)
public abstract class AssistantDB extends RoomDatabase {

    private static volatile AssistantDB INSTANCE;

    public abstract AlarmDao alarmDao();
    public abstract TodoDao todoDao();
    public abstract NoteDao noteDao();

    public static AssistantDB getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AssistantDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AssistantDB.class,
                            "assistant.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
