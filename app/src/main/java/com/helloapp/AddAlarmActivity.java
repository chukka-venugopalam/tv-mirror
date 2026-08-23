package com.helloapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddAlarmActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_alarm);

        TimePicker timePicker = findViewById(R.id.time_picker);
        EditText labelInput = findViewById(R.id.alarm_label_input);
        Button saveBtn = findViewById(R.id.btn_save_alarm);

        // Use 24-hour view for simplicity.
        timePicker.setIs24HourView(true);

        saveBtn.setOnClickListener(v -> {
            int hour = timePicker.getHour();
            int minute = timePicker.getMinute();
            String label = labelInput.getText().toString().trim();
            if (label.isEmpty()) {
                label = "Alarm";
            }

            AlarmEntity alarm = new AlarmEntity();
            alarm.hour = hour;
            alarm.minute = minute;
            alarm.label = label;
            alarm.enabled = true;

            executor.execute(() -> {
                long id = AssistantDB.getInstance(this).alarmDao().insert(alarm);
                alarm.id = id;
                AlarmScheduler.scheduleNext(this, alarm);
                runOnUiThread(() -> {
                    Toast.makeText(this,
                            String.format("Alarm set for %02d:%02d", hour, minute),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }
}
