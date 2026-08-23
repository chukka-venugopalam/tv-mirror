package com.helloapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the notes table.
 */
public class NoteDao {

    private final SQLiteDatabase db;

    NoteDao(SQLiteDatabase db) {
        this.db = db;
    }

    /** Returns all notes ordered by updatedAt descending (newest first). */
    public List<NoteEntity> getAll() {
        List<NoteEntity> list = new ArrayList<>();
        Cursor c = db.query("notes", null, null, null, null, null, "updatedAt DESC");
        while (c.moveToNext()) {
            list.add(cursorToNote(c));
        }
        c.close();
        return list;
    }

    /** Returns a single note by id, or null. */
    public NoteEntity getById(long id) {
        Cursor c = db.query("notes", null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        NoteEntity note = null;
        if (c.moveToFirst()) {
            note = cursorToNote(c);
        }
        c.close();
        return note;
    }

    /** Inserts a note and returns its new row id. */
    public long insert(NoteEntity note) {
        ContentValues cv = new ContentValues();
        cv.put("title", note.title);
        cv.put("body", note.body);
        cv.put("updatedAt", note.updatedAt);
        return db.insert("notes", null, cv);
    }

    /** Updates an existing note. */
    public void update(NoteEntity note) {
        ContentValues cv = new ContentValues();
        cv.put("title", note.title);
        cv.put("body", note.body);
        cv.put("updatedAt", note.updatedAt);
        db.update("notes", cv, "id=?", new String[]{String.valueOf(note.id)});
    }

    /** Deletes a note. */
    public void delete(NoteEntity note) {
        deleteById(note.id);
    }

    /** Deletes a note by id. */
    public void deleteById(long id) {
        db.delete("notes", "id=?", new String[]{String.valueOf(id)});
    }

    private NoteEntity cursorToNote(Cursor c) {
        NoteEntity n = new NoteEntity();
        n.id = c.getLong(c.getColumnIndexOrThrow("id"));
        n.title = c.getString(c.getColumnIndexOrThrow("title"));
        n.body = c.getString(c.getColumnIndexOrThrow("body"));
        n.updatedAt = c.getLong(c.getColumnIndexOrThrow("updatedAt"));
        return n;
    }
}
