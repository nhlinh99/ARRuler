package com.arruler;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ArcoreMeasurement extends AppCompatActivity {

    private String TAG = "ArcoreMeasurement";
    private List<String> buttonArrayList = new ArrayList<>();
    private Button toMeasurement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arcore_measurement);

        String[] buttonArray = getResources().getStringArray(R.array.arcore_measurement_buttons);

        Collections.addAll(buttonArrayList, buttonArray);

        toMeasurement = findViewById(R.id.to_measurement);
        toMeasurement.setText(buttonArrayList.get(0));
        toMeasurement.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(getApplication(), Measurement.class);
                startActivity(intent);
            }
        });


    }
}