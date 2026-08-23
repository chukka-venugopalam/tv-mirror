package com.helloapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class NoteEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Note title. */
    public String title;

    /** Freeform body text. */
    public String body;

    /** Timestamp when the note was last modified (millis since epoch). */
    public long updatedAt;
}
