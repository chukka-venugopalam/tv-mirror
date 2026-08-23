package com.helloapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Manages the local SQLite database for Alarms, Todos, and Notes.
 * Replaces the previous Room-based implementation.
 */
public class AssistantDB extends SQLiteOpenHelper {

    private static final String DB_NAME = "assistant.db";
    private static final int DB_VERSION = 1;

    // Singleton
    private static volatile AssistantDB INSTANCE;

    // DAO instances (lazy-initialized after DB is writable)
    private AlarmDao alarmDao;
    private TodoDao todoDao;
    private NoteDao noteDao;

    public static AssistantDB getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AssistantDB.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AssistantDB(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private AssistantDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE alarms (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "hour INTEGER NOT NULL, " +
                "minute INTEGER NOT NULL, " +
                "label TEXT, " +
                "enabled INTEGER NOT NULL DEFAULT 1)");

        db.execSQL("CREATE TABLE todos (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "text TEXT NOT NULL, " +
                "done INTEGER NOT NULL DEFAULT 0)");

        db.execSQL("CREATE TABLE notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "body TEXT, " +
                "updatedAt INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future migrations go here.
    }

    public synchronized AlarmDao alarmDao() {
        if (alarmDao == null) {
            alarmDao = new AlarmDao(getWritableDatabase());
        }
        return alarmDao;
    }

    public synchronized TodoDao todoDao() {
        if (todoDao == null) {
            todoDao = new TodoDao(getWritableDatabase());
        }
        return todoDao;
    }

    public synchronized NoteDao noteDao() {
        if (noteDao == null) {
            noteDao = new NoteDao(getWritableDatabase());
        }
        return noteDao;
    }
}
