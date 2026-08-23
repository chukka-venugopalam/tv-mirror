package com.helloapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alarms")
public class AlarmEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Hour of day (0-23). */
    public int hour;

    /** Minute of hour (0-59). */
    public int minute;

    /** User-facing label, e.g. "Wake up". */
    public String label;

    /** Whether this alarm is enabled. */
    public boolean enabled;
}
