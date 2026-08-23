package com.helloapp;

/**
 * Plain POJO for the todos table. No Room annotations.
 */
public class TodoEntity {

    /** Auto-generated primary key. -1 if not yet persisted. */
    public long id = -1;

    /** Task text. */
    public String text;

    /** Whether the task is marked done. */
    public boolean done;
}
