package com.helloapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AlarmsActivity extends AppCompatActivity {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private AlarmListAdapter adapter;
    private List<AlarmEntity> alarms = new ArrayList<>();
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarms);

        listView = findViewById(R.id.alarm_list);
        adapter = new AlarmListAdapter();
        listView.setAdapter(adapter);

        findViewById(R.id.btn_add_alarm).setOnClickListener(v ->
                startActivity(new Intent(this, AddAlarmActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlarms();
    }

    private void loadAlarms() {
        executor.execute(() -> {
            List<AlarmEntity> list = AssistantDB.getInstance(this).alarmDao().getAll();
            runOnUiThread(() -> {
                alarms = list;
                adapter.notifyDataSetChanged();
            });
        });
    }

    private class AlarmListAdapter extends BaseAdapter {

        @Override
        public int getCount() { return alarms.size(); }

        @Override
        public AlarmEntity getItem(int pos) { return alarms.get(pos); }

        @Override
        public long getItemId(int pos) { return alarms.get(pos).id; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AlarmsActivity.this)
                        .inflate(R.layout.alarm_item, parent, false);
            }

            AlarmEntity alarm = alarms.get(pos);

            TextView timeText = convertView.findViewById(R.id.alarm_time);
            TextView labelText = convertView.findViewById(R.id.alarm_label);
            Switch toggle = convertView.findViewById(R.id.alarm_toggle);
            ImageButton deleteBtn = convertView.findViewById(R.id.alarm_delete);

            timeText.setText(String.format("%02d:%02d", alarm.hour, alarm.minute));
            labelText.setText(alarm.label != null ? alarm.label : "");

            // Prevent listener fire during bind
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(alarm.enabled);
            toggle.setOnCheckedChangeListener((btn, checked) -> {
                alarm.enabled = checked;
                executor.execute(() -> {
                    AssistantDB.getInstance(AlarmsActivity.this).alarmDao().update(alarm);
                    if (checked) {
                        AlarmScheduler.scheduleNext(AlarmsActivity.this, alarm);
                    } else {
                        AlarmScheduler.cancel(AlarmsActivity.this, alarm);
                    }
                });
            });

            deleteBtn.setOnClickListener(v -> {
                executor.execute(() -> {
                    AlarmScheduler.cancel(AlarmsActivity.this, alarm);
                    AssistantDB.getInstance(AlarmsActivity.this).alarmDao().delete(alarm);
                    runOnUiThread(() -> {
                        alarms.remove(pos);
                        notifyDataSetChanged();
                    });
                });
            });

            return convertView;
        }
    }
}
