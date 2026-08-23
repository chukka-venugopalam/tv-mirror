package com.helloapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the alarms table.
 */
public class AlarmDao {

    private final SQLiteDatabase db;

    AlarmDao(SQLiteDatabase db) {
        this.db = db;
    }

    /** Returns all alarms ordered by hour then minute. */
    public List<AlarmEntity> getAll() {
        List<AlarmEntity> list = new ArrayList<>();
        Cursor c = db.query("alarms", null, null, null, null, null, "hour, minute");
        while (c.moveToNext()) {
            list.add(cursorToAlarm(c));
        }
        c.close();
        return list;
    }

    /** Returns a single alarm by id, or null. */
    public AlarmEntity getById(long id) {
        Cursor c = db.query("alarms", null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        AlarmEntity alarm = null;
        if (c.moveToFirst()) {
            alarm = cursorToAlarm(c);
        }
        c.close();
        return alarm;
    }

    /** Returns all enabled alarms. */
    public List<AlarmEntity> getEnabled() {
        List<AlarmEntity> list = new ArrayList<>();
        Cursor c = db.query("alarms", null, "enabled=1", null, null, null, "hour, minute");
        while (c.moveToNext()) {
            list.add(cursorToAlarm(c));
        }
        c.close();
        return list;
    }

    /** Inserts an alarm and returns its new row id. */
    public long insert(AlarmEntity alarm) {
        ContentValues cv = new ContentValues();
        cv.put("hour", alarm.hour);
        cv.put("minute", alarm.minute);
        cv.put("label", alarm.label);
        cv.put("enabled", alarm.enabled ? 1 : 0);
        return db.insert("alarms", null, cv);
    }

    /** Updates an existing alarm. */
    public void update(AlarmEntity alarm) {
        ContentValues cv = new ContentValues();
        cv.put("hour", alarm.hour);
        cv.put("minute", alarm.minute);
        cv.put("label", alarm.label);
        cv.put("enabled", alarm.enabled ? 1 : 0);
        db.update("alarms", cv, "id=?", new String[]{String.valueOf(alarm.id)});
    }

    /** Deletes an alarm. */
    public void delete(AlarmEntity alarm) {
        deleteById(alarm.id);
    }

    /** Deletes an alarm by id. */
    public void deleteById(long id) {
        db.delete("alarms", "id=?", new String[]{String.valueOf(id)});
    }

    private AlarmEntity cursorToAlarm(Cursor c) {
        AlarmEntity a = new AlarmEntity();
        a.id = c.getLong(c.getColumnIndexOrThrow("id"));
        a.hour = c.getInt(c.getColumnIndexOrThrow("hour"));
        a.minute = c.getInt(c.getColumnIndexOrThrow("minute"));
        a.label = c.getString(c.getColumnIndexOrThrow("label"));
        a.enabled = c.getInt(c.getColumnIndexOrThrow("enabled")) == 1;
        return a;
    }
}
