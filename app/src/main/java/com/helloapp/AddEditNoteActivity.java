package com.helloapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddEditNoteActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private long noteId = -1;
    private EditText titleInput;
    private EditText bodyInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_note);

        titleInput = findViewById(R.id.note_title_input);
        bodyInput = findViewById(R.id.note_body_input);
        Button saveBtn = findViewById(R.id.btn_save_note);

        noteId = getIntent().getLongExtra("note_id", -1);

        if (noteId != -1) {
            // Edit mode — load existing note.
            ((Button) findViewById(R.id.btn_save_note)).setText("Update Note");
            executor.execute(() -> {
                NoteEntity note = AssistantDB.getInstance(this).noteDao().getById(noteId);
                if (note != null) {
                    runOnUiThread(() -> {
                        titleInput.setText(note.title);
                        bodyInput.setText(note.body);
                    });
                }
            });
        }

        saveBtn.setOnClickListener(v -> saveNote());
    }

    private void saveNote() {
        String title = titleInput.getText().toString().trim();
        String body = bodyInput.getText().toString();

        if (title.isEmpty() && body.isEmpty()) {
            Toast.makeText(this, "Please enter a title or some text", Toast.LENGTH_SHORT).show();
            return;
        }

        if (noteId != -1) {
            // Update existing note
            executor.execute(() -> {
                NoteEntity note = AssistantDB.getInstance(this).noteDao().getById(noteId);
                if (note != null) {
                    note.title = title;
                    note.body = body;
                    note.updatedAt = System.currentTimeMillis();
                    AssistantDB.getInstance(this).noteDao().update(note);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "Note updated", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        } else {
            // Create new note
            NoteEntity note = new NoteEntity();
            note.title = title;
            note.body = body;
            note.updatedAt = System.currentTimeMillis();

            executor.execute(() -> {
                AssistantDB.getInstance(this).noteDao().insert(note);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        }
    }
}
