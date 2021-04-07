package com.arruler;

import android.os.Build;
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

public class Measurement extends AppCompatActivity implements Scene.OnUpdateListener {

    private static String TAG = Measurement.class.getSimpleName();

    private ArFragment arFragment = null;
    private TextView distanceModeTextView = null;
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
    private HashMap<String, Anchor> midAnchors = new HashMap<String, Anchor>();
    private HashMap<String, AnchorNode> midAnchorNodes = new HashMap<String, AnchorNode>(   );
    private List<List<Node>> fromGroundNodes = new ArrayList<List<Node>>();

    private TextView[][] multipleDistances = new TextView[Constants.maxNumMultiplePoints][Constants.maxNumMultiplePoints];

    private String initCM;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_measurement);
        String[] distanceModeArray = getResources().getStringArray(R.array.distance_mode);
        Collections.addAll(distanceModeArrayList, distanceModeArray);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        distanceModeTextView = findViewById(R.id.distance_view);
        multipleDistanceTableLayout = findViewById(R.id.multiple_distance_table);

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
        distanceModeSpinner = findViewById(R.id.distance_mode_spinner);
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
                ViewGroup.LayoutParams layoutParams = multipleDistanceTableLayout.getLayoutParams();
                if (distanceMode.equals(distanceModeArrayList.get(2))){
                    layoutParams.height = Constants.multipleDistanceTableHeight;
                    multipleDistanceTableLayout.setLayoutParams(layoutParams);
                    initDistanceTable();
                }
                else {
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
                if (multipleDistances[i][j] != null)
                    multipleDistances[i][j].setText((i == j) ? "-" : initCM);
            }
        }
        fromGroundNodes.clear();
    }

    private void tapDistanceFromGround(HitResult hitResult) {
        clearAllAnchors();
        Anchor anchor = hitResult.createAnchor();
        placedAnchors.add(anchor);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setSmoothed(true);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        placedAnchorNodes.add(anchorNode);

        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        transformableNode.getRotationController().setEnabled(false);
        transformableNode.getScaleController().setEnabled(false);
        transformableNode.getTranslationController().setEnabled(true);
        transformableNode.setRenderable(transformableNode.getRenderable());
        transformableNode.setParent(anchorNode);

        Node node = new Node();
        node.setParent(transformableNode);
        node.setWorldPosition(new Vector3(
                anchorNode.getWorldPosition().x,
                anchorNode.getWorldPosition().y,
                anchorNode.getWorldPosition().z
        ));
        node.setRenderable(distanceCardViewRenderable);

        Node arrow1UpNode = new Node();
        arrow1UpNode.setParent(node);
        arrow1UpNode.setWorldPosition(new Vector3(
                node.getWorldPosition().x,
                node.getWorldPosition().y + 0.1f,
                node.getWorldPosition().z
        ));
        arrow1UpNode.setRenderable(arrow1UpRenderable);
        arrow1UpNode.setOnTapListener(((hitTestResult, motionEvent) -> {
            node.setWorldPosition(new Vector3(
                    node.getWorldPosition().x,
                    node.getWorldPosition().y + 0.01f,
                    node.getWorldPosition().z
            ));
        })
        );

        Node arrow1DownNode = new Node();
        arrow1DownNode.setParent(node);
        arrow1DownNode.setWorldPosition(new Vector3(
                node.getWorldPosition().x,
                node.getWorldPosition().y + 0.08f,
                node.getWorldPosition().z
        ));
        arrow1DownNode.setRenderable(arrow1UpRenderable);
        arrow1DownNode.setOnTapListener(((hitTestResult, motionEvent) -> {
                    node.setWorldPosition(new Vector3(
                            node.getWorldPosition().x,
                            node.getWorldPosition().y - 0.01f,
                            node.getWorldPosition().z
                    ));
                })
        );

        Node arrow10UpNode = new Node();
        arrow10UpNode.setParent(node);
        arrow10UpNode.setWorldPosition(new Vector3(
                node.getWorldPosition().x,
                node.getWorldPosition().y + 0.08f,
                node.getWorldPosition().z
        ));
        arrow10UpNode.setRenderable(arrow1UpRenderable);
        arrow10UpNode.setOnTapListener(((hitTestResult, motionEvent) -> {
                    node.setWorldPosition(new Vector3(
                            node.getWorldPosition().x,
                            node.getWorldPosition().y - 0.01f,
                            node.getWorldPosition().z
                    ));
                })
        );

        Node arrow10DownNode = new Node();
        arrow10DownNode.setParent(node);
        arrow10DownNode.setWorldPosition(new Vector3(
                node.getWorldPosition().x,
                node.getWorldPosition().y + 0.08f,
                node.getWorldPosition().z
        ));
        arrow10DownNode.setRenderable(arrow1UpRenderable);
        arrow10DownNode.setOnTapListener(((hitTestResult, motionEvent) -> {
                    node.setWorldPosition(new Vector3(
                            node.getWorldPosition().x,
                            node.getWorldPosition().y - 0.01f,
                            node.getWorldPosition().z
                    ));
                })
        );

        fromGroundNodes.add(Arrays.asList(node, arrow1UpNode, arrow1DownNode, arrow10UpNode, arrow10DownNode));

        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
    }

    private void placeAnchor(HitResult hitResult, Renderable renderable) {
        Anchor anchor = hitResult.createAnchor();
        placedAnchors.add(anchor);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setSmoothed(true);
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        placedAnchorNodes.add(anchorNode);

        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem()) {{
            getRotationController().setEnabled(false);
            getScaleController().setEnabled(false);
            getTranslationController().setEnabled(true);
            setRenderable(renderable);
            setParent(anchorNode);
        }};

        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    private void placeMidAnchor(Pose pose, Renderable renderable) {
        String midKey = String.format("%d_%d", 0, 1);
        Anchor anchor = Objects.requireNonNull(arFragment.getArSceneView().getSession()).
                createAnchor(pose);
        midAnchors.put(midKey, anchor);

        AnchorNode anchorNode = new AnchorNode();
        anchorNode.setSmoothed(true);
        anchorNode.setParent(arFragment.getArSceneView().getScene());
        midAnchorNodes.put(midKey, anchorNode);

        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem()) {{
            this.getRotationController().setEnabled(false);
            this.getScaleController().setEnabled(false);
            this.getTranslationController().setEnabled(true);
            this.setRenderable(renderable);
            setParent(anchorNode);
        }};
        arFragment.getArSceneView().getScene().addOnUpdateListener(this);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
    }

    private void tapDistanceOf2Points(HitResult hitResult) {
        if (placedAnchorNodes.size() == 0) {
            placeAnchor(hitResult, cubeRenderable);
        }
        else if (placedAnchorNodes.size() == 1) {
            placeAnchor(hitResult, cubeRenderable);
            Vector3 worldPosition0 = placedAnchorNodes.get(0).getWorldPosition();
            Vector3 worldPosition1 = placedAnchorNodes.get(1).getWorldPosition();
            float[] midPosition = {
                    (worldPosition0.x + worldPosition1.x) / 2,
                    (worldPosition0.y + worldPosition1.y) / 2,
                    (worldPosition0.z + worldPosition1.z) / 2
            };
            float[] quaternion = {0.0f,0.0f,0.0f,0.0f};
            Pose pose = new Pose(midPosition, quaternion);

            placeMidAnchor(pose, distanceCardViewRenderable);
        }
        else {
            clearAllAnchors();
            placeAnchor(hitResult, cubeRenderable);
        }
    }

    private void tapDistanceOfMultiplePoints(HitResult hitResult) {
        if (placedAnchorNodes.size() >= Constants.maxNumMultiplePoints) {
            clearAllAnchors();
        }
        ViewRenderable
                .builder()
                .setView(this, R.layout.point_text_layout)
                .build()
                .thenAccept(material -> {
                    material.setShadowReceiver(false);
                    material.setShadowCaster(false);
                    pointTextView = (TextView)material.getView();
                    pointTextView.setText(String.valueOf(placedAnchors.size()));
                    placeAnchor(hitResult, material);
                })
                .exceptionally(throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage()).setTitle("Error");
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    return null;
                });
        Log.i(TAG, String.format("Number of anchors: %d", placedAnchorNodes.size()));
    }

    private void measureDistanceFromGround() {
        if (fromGroundNodes.size() == 0) return;
        for (List<Node> node: fromGroundNodes) {
            TextView textView = ((LinearLayout)distanceCardViewRenderable.getView()).
                    findViewById(R.id.distanceCard);
            double distanceMeter = node.get(0).getWorldPosition().y + 1.0f;
            textView.setText(makeDistanceTextWithCM(distanceMeter));
        }
    }

    private void measureDistanceFromCamera() {
        Frame frame = arFragment.getArSceneView().getArFrame();
        if (placedAnchorNodes.size() >= 1) {
            double distanceMeter = calculateDistanceMeter(
                    placedAnchorNodes.get(0).getWorldPosition(),
                    Objects.requireNonNull(frame).getCamera().getPose());
            measureDistanceOf2Points(distanceMeter);
        }
    }

    private void measureDistanceOf2Points() {
        if (placedAnchorNodes.size() == 2) {
            double distanceMeter = calculateDistanceMeter(
                    placedAnchorNodes.get(0).getWorldPosition(),
                    placedAnchorNodes.get(1).getWorldPosition()
            );
            measureDistanceOf2Points(distanceMeter);
        }
    }

    private void measureDistanceOf2Points(double distanceMeter) {
        String distanceTextWithCM = makeDistanceTextWithCM(distanceMeter);
        TextView textView = ((LinearLayout)distanceCardViewRenderable.getView()).findViewById(R.id.distanceCard);
        textView.setText(distanceTextWithCM);
        Log.d(TAG, String.format("Distance: %s", distanceTextWithCM));
    }

    private void measureMultipleDistances() {
        if (placedAnchorNodes.size() > 1) {
            for (int i = 0; i < placedAnchorNodes.size(); ++i) {
                for (int j = i + 1; j < placedAnchorNodes.size(); ++j) {
                    double distanceMeter = calculateDistanceMeter(
                            placedAnchorNodes.get(i).getWorldPosition(),
                            placedAnchorNodes.get(j).getWorldPosition());
                    //double distanceCM = changeUnit(distanceMeter, "cm");
                    String distanceCMFloor = makeDistanceTextWithCM(distanceMeter);
                    multipleDistances[i][j].setText(distanceCMFloor);
                    multipleDistances[j][i].setText(distanceCMFloor);
                }
            }
        }
    }

    private String makeDistanceTextWithCM(double distanceMeter) {
        double distanceCM = changeUnit(distanceMeter, "cm");
        return String.format("%.2f cm", distanceCM);
    }

    private double calculateDistanceMeter(float x, float y, float z) {
        return Math.sqrt(Math.pow(x,2) + Math.pow(y,2) + Math.pow(z,2));
    }

    private double calculateDistanceMeter(Pose objPose0, Pose objPose1) {
        return calculateDistanceMeter(
                objPose0.tx() - objPose1.tx(),
                objPose0.ty() - objPose1.ty(),
                objPose0.tz() - objPose1.tz());
    }

    private double calculateDistanceMeter(Vector3 objPose0, Pose objPose1) {
        return calculateDistanceMeter(
                objPose0.x - objPose1.tx(),
                objPose0.y - objPose1.ty(),
                objPose0.z - objPose1.tz());
    }

    private double calculateDistanceMeter(Vector3 objPose0, Vector3 objPose1) {
        return calculateDistanceMeter(
                objPose0.x - objPose1.x,
                objPose0.y - objPose1.y,
                objPose0.z - objPose1.z);
    }

    double changeUnit(double distanceMeter, String unit) {
        switch (unit) {
            case "cm": return distanceMeter * 100;
            case "mm": return distanceMeter * 1000;
            default: return distanceMeter;
        }
    }

    void toastMode() {
        String msg = "Unknown";
        if (distanceMode.equals(distanceModeArrayList.get(0))) {
            msg = "Find plane and tap somewhere";
        } else if (distanceMode.equals(distanceModeArrayList.get(1))) {
            msg = "Find plane and tap 2 points";
        } else if (distanceMode.equals(distanceModeArrayList.get(2))) {
            msg = "Find plane and tap multiple points";
        } else if (distanceMode.equals(distanceModeArrayList.get(3))) {
            msg = "Find plane and tap a point";
        }
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    boolean checkIsSupportedDeviceOrFinish(Activity activity) {
        ActivityManager systemService =
                (ActivityManager)Objects.requireNonNull(activity
                        .getSystemService(Context.ACTIVITY_SERVICE));
        String openGLVersionString = systemService.getDeviceConfigurationInfo().getGlEsVersion();

        if (Double.parseDouble(openGLVersionString) < Constants.MIN_OPENGL_VERSION) {
            String msg = String.format("Sceneform requires OpenGL ES %f later", Constants.MIN_OPENGL_VERSION);
            Log.e(TAG, msg);
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }

    @SuppressLint("SetTextI18n") @Override
    public void onUpdate(FrameTime frameTime) {
        if (distanceMode.equals(distanceModeArrayList.get(1))) {
            measureDistanceOf2Points();
        } else if (distanceMode.equals(distanceModeArrayList.get(2))) {
            measureMultipleDistances();
        } else if (distanceMode.equals(distanceModeArrayList.get(3))) {
            measureDistanceFromGround();
        } else {
            measureDistanceFromCamera();
        }
    }
}