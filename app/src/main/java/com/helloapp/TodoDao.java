package com.helloapp;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the todos table.
 */
public class TodoDao {

    private final SQLiteDatabase db;

    TodoDao(SQLiteDatabase db) {
        this.db = db;
    }

    /** Returns all todos ordered by id descending (newest first). */
    public List<TodoEntity> getAll() {
        List<TodoEntity> list = new ArrayList<>();
        Cursor c = db.query("todos", null, null, null, null, null, "id DESC");
        while (c.moveToNext()) {
            list.add(cursorToTodo(c));
        }
        c.close();
        return list;
    }

    /** Inserts a todo and returns its new row id. */
    public long insert(TodoEntity todo) {
        ContentValues cv = new ContentValues();
        cv.put("text", todo.text);
        cv.put("done", todo.done ? 1 : 0);
        return db.insert("todos", null, cv);
    }

    /** Updates an existing todo. */
    public void update(TodoEntity todo) {
        ContentValues cv = new ContentValues();
        cv.put("text", todo.text);
        cv.put("done", todo.done ? 1 : 0);
        db.update("todos", cv, "id=?", new String[]{String.valueOf(todo.id)});
    }

    /** Deletes a todo. */
    public void delete(TodoEntity todo) {
        deleteById(todo.id);
    }

    /** Deletes a todo by id. */
    public void deleteById(long id) {
        db.delete("todos", "id=?", new String[]{String.valueOf(id)});
    }

    private TodoEntity cursorToTodo(Cursor c) {
        TodoEntity t = new TodoEntity();
        t.id = c.getLong(c.getColumnIndexOrThrow("id"));
        t.text = c.getString(c.getColumnIndexOrThrow("text"));
        t.done = c.getInt(c.getColumnIndexOrThrow("done")) == 1;
        return t;
    }
}
