package com.helloapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotesActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private NoteListAdapter adapter;
    private List<NoteEntity> notes = new ArrayList<>();
    private ListView listView;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        listView = findViewById(R.id.note_list);
        emptyText = findViewById(R.id.empty_text);
        adapter = new NoteListAdapter();
        listView.setAdapter(adapter);

        findViewById(R.id.btn_add_note).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditNoteActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }

    private void loadNotes() {
        executor.execute(() -> {
            List<NoteEntity> list = AssistantDB.getInstance(this).noteDao().getAll();
            runOnUiThread(() -> {
                notes = list;
                adapter.notifyDataSetChanged();
                emptyText.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
                listView.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private class NoteListAdapter extends BaseAdapter {

        @Override
        public int getCount() { return notes.size(); }

        @Override
        public NoteEntity getItem(int pos) { return notes.get(pos); }

        @Override
        public long getItemId(int pos) { return notes.get(pos).id; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(NotesActivity.this)
                        .inflate(R.layout.note_item, parent, false);
            }

            NoteEntity note = notes.get(pos);

            TextView titleText = convertView.findViewById(R.id.note_title);
            TextView previewText = convertView.findViewById(R.id.note_preview);
            TextView dateText = convertView.findViewById(R.id.note_date);
            ImageButton deleteBtn = convertView.findViewById(R.id.note_delete);

            titleText.setText(note.title != null && !note.title.isEmpty() ? note.title : "(Untitled)");
            String body = note.body != null ? note.body : "";
            previewText.setText(body.length() > 80 ? body.substring(0, 80) + "..." : body);
            dateText.setText(new java.text.SimpleDateFormat("MMM d, yyyy HH:mm",
                    java.util.Locale.getDefault()).format(new java.util.Date(note.updatedAt)));

            // Tap to edit
            convertView.setOnClickListener(v -> {
                Intent intent = new Intent(NotesActivity.this, AddEditNoteActivity.class);
                intent.putExtra("note_id", note.id);
                startActivity(intent);
            });

            deleteBtn.setOnClickListener(v -> {
                executor.execute(() -> {
                    AssistantDB.getInstance(NotesActivity.this).noteDao().delete(note);
                    runOnUiThread(() -> {
                        notes.remove(pos);
                        notifyDataSetChanged();
                        emptyText.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
                        listView.setVisibility(notes.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                });
            });

            return convertView;
        }
    }
}
