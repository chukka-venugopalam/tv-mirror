package com.helloapp;

/**
 * Plain POJO for the notes table. No Room annotations.
 */
public class NoteEntity {

    /** Auto-generated primary key. -1 if not yet persisted. */
    public long id = -1;

    /** Note title. */
    public String title;

    /** Freeform body text. */
    public String body;

    /** Timestamp when the note was last modified (millis since epoch). */
    public long updatedAt;
}
