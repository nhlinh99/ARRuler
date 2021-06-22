package com.arruler;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {
    Button switchToMeasuringRoomActivity;
    Button switchToQuitActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        switchToMeasuringRoomActivity = findViewById(R.id.room_measure_button);
        switchToMeasuringRoomActivity.setOnClickListener(view -> switch_measuring_room_activity());

        switchToQuitActivity = findViewById(R.id.quit_button);
        switchToQuitActivity.setOnClickListener(view -> finish());
    }

    private void switch_measuring_room_activity() {
        Intent switchActivityIntent = new Intent(this, Measuring_room.class);
        startActivity(switchActivityIntent);
    }
}
