package com.arruler;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import android.annotation.SuppressLint;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import com.google.ar.core.*;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.*;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.ar.sceneform.rendering.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.lang.*;

public class Measurement extends AppCompatActivity {

    private TextView mTextView;
    private static float MIN_OPENGL_VERSION = (float) 3.0;
    private static String TAG = Measurement.class.getSimpleName();

    private ArFragment arFragment = new ArFragment();
    private TextView distanceModeTextView;
    private TextView pointTextView;

    private LinearLayout arrow1UpLinearLayout;
    private LinearLayout arrow1DownLinearLayout;
    private ImageView arrow1UpView;
    private ImageView arrow1DownView;
    private Renderable arrow1UpRenderable;
    private Renderable arrow1DownRenderable;

    private LinearLayout arrow10UpLinearLayout;
    private LinearLayout arrow10DownLinearLayout;
    private ImageView arrow10UpView;
    private ImageView arrow10DownView;
    private Renderable arrow10UpRenderable;
    private Renderable arrow10DownRenderable;

    private TableLayout multipleDistanceTableLayout;

    private ModelRenderable cubeRenderable;
    private ViewRenderable distanceCardViewRenderable;
    private Spinner distanceModeSpinner;
    private List<String> distanceModeArrayList = new ArrayList<String>();
    private String distanceMode = "";

    private List<Anchor> placedAnchors = new ArrayList<Anchor>();
    private List<AnchorNode> placedAnchorNodes = new ArrayList<AnchorNode>();
    private HashMap<String, Anchor> midAnchors;
    private HashMap<String, AnchorNode> midAnchorNodes;
    private List<List<Node>> fromGroundNodes = new ArrayList<List<Node>>();

    private List multipleDistances = Arrays.asList(Constants.maxNumMultiplePoints,
            {ArrayList<TextView>(Constants.maxNumMultiplePoints)});


    private String initCM;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        } ;

        setContentView(R.layout.activity_measurement);
        String[] distanceModeArray = getResources().getStringArray(R.array.distance_mode);
        Collections.addAll(distanceModeArrayList, distanceModeArray);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        distanceModeTextView = (TextView) findViewById(R.id.distance_view);
        multipleDistanceTableLayout = (TableLayout) findViewById(R.id.multiple_distance_table);

        initCM = getResources().getString(R.string.initCM);

        configureSpinner();
        initArrowView();
        initRenderable();
        clearButton();

        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            if (cubeRenderable == null || distanceCardViewRenderable == null) return;
            // Creating Anchor.
            if (distanceMode.equals(distanceModeArrayList.get(0))) {
                clearAllAnchors();
                placeAnchor(hitResult, distanceCardViewRenderable);
            }
            else if (distanceMode.equals(distanceModeArrayList.get(1))) {
                tapDistanceOf2Points(hitResult);
            }
            else if (distanceMode.equals(distanceModeArrayList.get(2))) {
                tapDistanceOfMultiplePoints(hitResult);
            }
            else if (distanceMode.equals(distanceModeArrayList.get(3))) {
                tapDistanceFromGround(hitResult);
            }
            else {
                clearAllAnchors();
                placeAnchor(hitResult, distanceCardViewRenderable);
            }

        });

    }

    private void initDistanceTable(){
        for (int i = 0; i < Constants.maxNumMultiplePoints+1; i++){
            TableRow tableRow = new TableRow(this);
            multipleDistanceTableLayout.addView(tableRow,
                    multipleDistanceTableLayout.getWidth(),
                    Constants.multipleDistanceTableHeight / (Constants.maxNumMultiplePoints + 1));
            for (int j = 0; j < Constants.maxNumMultiplePoints+1; j++){
                TextView textView = new TextView(this);
                textView.setTextColor(android.graphics.Color.WHITE);
                if (i==0){
                    if (j==0){
                        textView.setText("cm");
                    }
                    else{
                        textView.setText(String.valueOf(j-1));
                    }
                }
                else{
                    if (j==0){
                        textView.setText(String.valueOf(i-1));
                    }
                    else if(i==j){
                        textView.setText("-");
                        multipleDistances[i-1][j-1] = textView;
                    }
                    else{
                        textView.setText(initCM);
                        multipleDistances[i-1][j-1] = textView;
                    }
                }
                tableRow.addView(textView,
                        tableRow.getLayoutParams().width / (Constants.maxNumMultiplePoints + 1),
                        tableRow.getLayoutParams().height);
            }
        }
    }

    private void initArrowView(){
        arrow1UpLinearLayout = new LinearLayout(this);
        arrow1UpLinearLayout.setOrientation(LinearLayout.VERTICAL);

        arrow1UpLinearLayout.setGravity(Gravity.CENTER);
        arrow1UpView = new ImageView(this);
        arrow1UpView.setImageResource(R.drawable.arrow_1up);
        arrow1UpLinearLayout.addView(arrow1UpView,
                Constants.arrowViewSize,
                Constants.arrowViewSize);

        arrow1DownLinearLayout = new LinearLayout(this);
        arrow1DownLinearLayout.setOrientation(LinearLayout.VERTICAL);
        arrow1DownLinearLayout.setGravity(Gravity.CENTER);
        arrow1DownView = new ImageView(this);
        arrow1DownView.setImageResource(R.drawable.arrow_1down);
        arrow1DownLinearLayout.addView(arrow1DownView,
                Constants.arrowViewSize,
                Constants.arrowViewSize);

        arrow10UpLinearLayout = new LinearLayout(this);
        arrow10UpLinearLayout.setOrientation(LinearLayout.VERTICAL);
        arrow10UpLinearLayout.setGravity(Gravity.CENTER);
        arrow10UpView = new ImageView(this);
        arrow10UpView.setImageResource(R.drawable.arrow_10up);
        arrow10UpLinearLayout.addView(arrow10UpView,
                Constants.arrowViewSize,
                Constants.arrowViewSize);

        arrow10DownLinearLayout = new LinearLayout(this);
        arrow10DownLinearLayout.setOrientation(LinearLayout.VERTICAL);
        arrow10DownLinearLayout.setGravity(Gravity.CENTER);
        arrow10DownView = new ImageView(this);
        arrow10DownView.setImageResource(R.drawable.arrow_10down);
        arrow10DownLinearLayout.addView(arrow10DownView,
                Constants.arrowViewSize,
                Constants.arrowViewSize);
    }

    private void initRenderable() {
        MaterialFactory.makeTransparentWithColor (
                this,
                new Color(android.graphics.Color.RED)
        ).thenAccept ( material -> {
            cubeRenderable = ShapeFactory.makeSphere(
                    0.02f,
                    Vector3.zero(),
                    material);
            cubeRenderable.setShadowCaster(false);
            cubeRenderable.setShadowReceiver(false);
        }).exceptionally (
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Error").setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }
        );

        ViewRenderable
            .builder()
            .setView(this, R.layout.distance_text_layout)
            .build()
            .thenAccept( material -> {
                distanceCardViewRenderable = material;
                distanceCardViewRenderable.setShadowCaster(false);
                distanceCardViewRenderable.setShadowReceiver(false);
            }).exceptionally (
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Error").setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
            });

        ViewRenderable
            .builder()
            .setView(this, arrow1UpLinearLayout)
            .build()
            .thenAccept (material -> {
                arrow1UpRenderable = material;
                arrow1UpRenderable.setShadowCaster(false);
                arrow1UpRenderable.setShadowReceiver(false);
            }).exceptionally (
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Error").setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
            });

        ViewRenderable
            .builder()
            .setView(this, arrow1DownLinearLayout)
            .build()
            .thenAccept (material -> {
                arrow1DownRenderable = material;
                arrow1DownRenderable.setShadowCaster(false);
                arrow1DownRenderable.setShadowReceiver(false);
            }).exceptionally (
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Error").setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
            });

        ViewRenderable
            .builder()
            .setView(this, arrow10UpLinearLayout)
            .build()
            .thenAccept (material -> {
                arrow10UpRenderable = material;
                arrow10UpRenderable.setShadowCaster(false);
                arrow10UpRenderable.setShadowReceiver(false);
            }).exceptionally (
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Error").setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
            });

        ViewRenderable
            .builder()
            .setView(this, arrow10DownLinearLayout)
            .build()
            .thenAccept (material -> {
                arrow10DownRenderable = material;
                arrow10DownRenderable.setShadowCaster(false);
                arrow10DownRenderable.setShadowReceiver(false);
            }).exceptionally (
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("Error").setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
            });
    }

    private void configureSpinner(){
        distanceMode = distanceModeArrayList.get(0);
        distanceModeSpinner = (Spinner) findViewById(R.id.distance_mode_spinner);
        ArrayAdapter distanceModeAdapter = new ArrayAdapter(
                getApplicationContext(),
                android.R.layout.simple_spinner_item,
                distanceModeArrayList
        );
        distanceModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        distanceModeSpinner.setAdapter(distanceModeAdapter);
        distanceModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {
                Spinner spinnerParent = (Spinner) parent;
                distanceMode = (String) spinnerParent.getSelectedItem();
                clearAllAnchors();
                setMode();
                toastMode();
                if (distanceMode.equals(distanceModeArrayList.get(2))){
                    ViewGroup.LayoutParams layoutParams = multipleDistanceTableLayout.getLayoutParams();
                    layoutParams.height = Constants.multipleDistanceTableHeight;
                    multipleDistanceTableLayout.setLayoutParams(layoutParams);
                    initDistanceTable();
                }
                else {
                    ViewGroup.LayoutParams layoutParams = multipleDistanceTableLayout.getLayoutParams();
                    layoutParams.height = 0;
                    multipleDistanceTableLayout.setLayoutParams(layoutParams);
                }
                Log.i(TAG, "Selected arcore focus on ${distanceMode}");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                clearAllAnchors();
                setMode();
                toastMode();
            }
        });
    }

    private void setMode(){
        distanceModeTextView.setText(distanceMode);
    }

    private void clearButton(){
        clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAllAnchors();
            }
        });
    }

    private void clearAllAnchors(){
        placedAnchors.clear();

        for (AnchorNode anchorNode : placedAnchorNodes){
            arFragment.getArSceneView().getScene().removeChild(anchorNode);
            anchorNode.setEnabled(false);
            anchorNode.getAnchor().detach();
            anchorNode.setParent(null);
        }
        placedAnchorNodes.clear();
        midAnchors.clear();
        for (Map.Entry<String, AnchorNode> entry : midAnchorNodes.entrySet()){
            String k = entry.getKey();
            AnchorNode anchorNode = entry.getValue();
            arFragment.getArSceneView().getScene().removeChild(anchorNode);
            anchorNode.setEnabled(false);
            anchorNode.getAnchor().detach();
            anchorNode.setParent(null);
        }

        midAnchorNodes.clear();
        for (int i = 0; i < Constants.maxNumMultiplePoints; i++){
            for (int j = 0; j < Constants.maxNumMultiplePoints; j++){
                if (multipleDistances[i][j] != null)  multipleDistances[i][j].setText((i == j) ? "-" : initCM);
            }
        }
        fromGroundNodes.clear();
    }


    private void toastMode() {
    }


    private void tapDistanceFromGround(HitResult hitResult) {
    }

    private void tapDistanceOfMultiplePoints(HitResult hitResult) {
    }

    private void tapDistanceOf2Points(HitResult hitResult) {
    }

    private void placeAnchor(HitResult hitResult, ViewRenderable distanceCardViewRenderable) {
    }

    private boolean checkIsSupportedDeviceOrFinish(Measurement measurement) {
        return true;
    }
}