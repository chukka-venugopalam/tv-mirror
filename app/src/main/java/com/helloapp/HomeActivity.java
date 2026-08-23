package com.helloapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class HomeActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        requestNotificationPermission();

        Button alarmsBtn = findViewById(R.id.btn_alarms);
        Button todosBtn = findViewById(R.id.btn_todos);
        Button notesBtn = findViewById(R.id.btn_notes);
        Button tvRemoteBtn = findViewById(R.id.btn_tv_remote);

        alarmsBtn.setOnClickListener(v ->
                startActivity(new Intent(this, AlarmsActivity.class)));
        todosBtn.setOnClickListener(v ->
                startActivity(new Intent(this, TodosActivity.class)));
        notesBtn.setOnClickListener(v ->
                startActivity(new Intent(this, NotesActivity.class)));
        tvRemoteBtn.setOnClickListener(v ->
                startActivity(new Intent(this, MainActivity.class)));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }
}
