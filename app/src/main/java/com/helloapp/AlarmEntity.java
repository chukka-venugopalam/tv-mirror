package com.helloapp;

/**
 * Plain POJO for the alarms table. No Room annotations.
 */
public class AlarmEntity {

    /** Auto-generated primary key. -1 if not yet persisted. */
    public long id = -1;

    /** Hour of day (0-23). */
    public int hour;

    /** Minute of hour (0-59). */
    public int minute;

    /** User-facing label, e.g. "Wake up". */
    public String label;

    /** Whether this alarm is enabled. */
    public boolean enabled;
}
