package com.helloapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TodosActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private TodoListAdapter adapter;
    private List<TodoEntity> todos = new ArrayList<>();
    private ListView listView;
    private EditText inputField;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todos);

        listView = findViewById(R.id.todo_list);
        inputField = findViewById(R.id.todo_input);
        Button addBtn = findViewById(R.id.btn_add_todo);
        emptyText = findViewById(R.id.empty_text);

        adapter = new TodoListAdapter();
        listView.setAdapter(adapter);

        addBtn.setOnClickListener(v -> addTodo());

        // Also add on keyboard "done" action.
        inputField.setOnEditorActionListener((v, actionId, event) -> {
            addTodo();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodos();
    }

    private void addTodo() {
        String text = inputField.getText().toString().trim();
        if (text.isEmpty()) return;

        TodoEntity todo = new TodoEntity();
        todo.text = text;
        todo.done = false;

        executor.execute(() -> {
            AssistantDB.getInstance(this).todoDao().insert(todo);
            runOnUiThread(() -> {
                inputField.setText("");
                loadTodos();
            });
        });
    }

    private void loadTodos() {
        executor.execute(() -> {
            List<TodoEntity> list = AssistantDB.getInstance(this).todoDao().getAll();
            runOnUiThread(() -> {
                todos = list;
                adapter.notifyDataSetChanged();
                emptyText.setVisibility(todos.isEmpty() ? View.VISIBLE : View.GONE);
                listView.setVisibility(todos.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private class TodoListAdapter extends BaseAdapter {

        @Override
        public int getCount() { return todos.size(); }

        @Override
        public TodoEntity getItem(int pos) { return todos.get(pos); }

        @Override
        public long getItemId(int pos) { return todos.get(pos).id; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(TodosActivity.this)
                        .inflate(R.layout.todo_item, parent, false);
            }

            TodoEntity todo = todos.get(pos);

            CheckBox checkBox = convertView.findViewById(R.id.todo_checkbox);
            TextView textText = convertView.findViewById(R.id.todo_text);
            ImageButton deleteBtn = convertView.findViewById(R.id.todo_delete);

            // Prevent listener fire during bind
            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(todo.done);
            textText.setText(todo.text);

            // Strike through if done
            if (todo.done) {
                textText.setPaintFlags(textText.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                textText.setPaintFlags(textText.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            }

            checkBox.setOnCheckedChangeListener((btn, checked) -> {
                todo.done = checked;
                executor.execute(() ->
                        AssistantDB.getInstance(TodosActivity.this).todoDao().update(todo));
                // Rebind to update strike-through
                notifyDataSetChanged();
            });

            deleteBtn.setOnClickListener(v -> {
                executor.execute(() -> {
                    AssistantDB.getInstance(TodosActivity.this).todoDao().delete(todo);
                    runOnUiThread(() -> {
                        todos.remove(pos);
                        notifyDataSetChanged();
                        emptyText.setVisibility(todos.isEmpty() ? View.VISIBLE : View.GONE);
                        listView.setVisibility(todos.isEmpty() ? View.GONE : View.VISIBLE);
                    });
                });
            });

            return convertView;
        }
    }
}
