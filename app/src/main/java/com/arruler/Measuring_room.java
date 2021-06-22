package com.arruler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.arruler.ViewRender3D.Render_Sketch_Room;
import com.arruler.utils.Constants;
import com.arruler.utils.CustomVisualizer;
import com.google.ar.core.Anchor;
import com.google.ar.core.Config;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.DpToMetersViewSizer;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.PlaneRenderer;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Measuring_room extends AppCompatActivity implements Scene.OnUpdateListener {

    private static final String TAG = Measuring_room.class.getSimpleName();
    private ArFragment arFragment = null; //ArFragment parameters
    private Session session = null;


    private final List<AnchorNode> AnchorNodes = new ArrayList<>(); //list saved the renderable object, in case of delete or draw
    private final List<AnchorNode> LineNodes = new ArrayList<>();
    private final List<AnchorNode> DistanceBetweenAnchorsCards = new ArrayList<>();
    private final List<AnchorNode> DoorNodes = new ArrayList<>();

    private ModelRenderable placedAnchor; //renderable object
    private ModelRenderable LineBetweenAnchor;
    private ModelRenderable RectangleRender;
    private ModelRenderable CubeRender;

    private ViewRenderable ObjectCardViewRenderable;
    private ViewRenderable distanceCardViewRenderable;
    private ViewRenderable RoomCardViewRenderable;

    private AnchorNode middleAnchorNode = null; //change continuously in OnUpdate
    private AnchorNode LineDragAnchorNode = null;

    private final List<AnchorNode> ObjectNodes = new ArrayList<>();

    private List<AnchorNode> middleDoorAnchorNode = null;
    private List<AnchorNode> middleObjectAnchorNode = null;


    private boolean check_choose_plane = false;
    private float height_of_floor;
    private int[] image_dimension = new int[2]; //resolution of mobile phone

    private boolean check_measuring_height_of_room = false; //measuring height boolean
    private boolean check_get_floor_point_to_measure_height = false;

    private boolean check_measuring_door_command = false; //measuring door boolean
    private boolean check_measuring_door_at_bottom = false;
    private boolean check_complete_measuring_door = false;

    private boolean check_measuring_window_command = false; //not use this yet...

    private boolean check_measuring_object_command = false; //measuring object boolean
    private boolean check_measuring_object_first = false;
    private boolean check_measuring_object_second = false;
    private boolean check_measuring_object_third = false;
    private boolean check_complete_measuring_object = false;

    private List<Integer> list_coordinate_plane_for_door; //get the plane of wall for door
    private List<Float> bottom_door_coordinate; //door coordinate
    private List<Float> top_door_coordinate;

    private List<List<Float>> object_coordinates;

    private boolean thread_button = false; //thread happened in button event

    private float height_of_room = 0;
    private final float epsilon = (float) 0.06; // almost equal

    private final ArrayList<List<Float>> room_corner_transfer_to_render = new ArrayList<>();
    private final ArrayList<List<List<Float>>> doors_coordinate_transfer_to_render = new ArrayList<>();
    private final ArrayList<List<Integer>> plane_contains_doors = new ArrayList<>();

    private Button clickButton;
    private Button UndoButton;
    private LinearLayout Measuring_Layout;

    private final Thread thread = new Thread(() -> {
        while (AnchorNodes.size() > 1) {
            try {
                Thread.sleep(1000);

                if (AnchorNodes.size() < 2 || !check_choose_plane || !thread_button) continue;

                for(AnchorNode node:LineNodes) DeleteAnchorNode(node);
                for(AnchorNode node:DoorNodes) DeleteAnchorNode(node);
                LineNodes.clear();

                for (int i = 0; i < AnchorNodes.size(); i++) {
                    AnchorNode linenode = LineBetweenTwoAnchorNodes(AnchorNodes.get(i), AnchorNodes.get((i + 1) % AnchorNodes.size()));
                    LineNodes.add(linenode);
                }

                for (AnchorNode node:DoorNodes) DrawAnchorNode(node);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_measuring_room);
        image_dimension = new int[]{this.getResources().getDisplayMetrics().widthPixels, this.getResources().getDisplayMetrics().widthPixels};

        Measuring_Layout = findViewById(R.id.measuring_layout);

        clickButton = findViewById(R.id.button_measure);

        clickButton.setOnClickListener(v -> {
            thread_button = true;
            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            clickButton.startAnimation(myAnim);

            session = arFragment.getArSceneView().getSession();
            float[] point_to_render;
            Pose pose = null;

            if (!check_measuring_height_of_room){
                if (!check_get_floor_point_to_measure_height){
                    point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                    pose = new Pose(new float[]{point_to_render[0], point_to_render[1], point_to_render[2]}, new float[]{0, 0, 0, 1});

                    check_get_floor_point_to_measure_height = true;

                } else {
                    Vector3 temp = AnchorNodes.get(0).getWorldPosition();
                    List<Float> p = Arrays.asList(temp.x, temp.y, temp.z);
                    point_to_render = convert_2D_to_3D_on_height_from_point(p);
                    height_of_room = Math.abs(point_to_render[1] - height_of_floor);
                    check_measuring_height_of_room = true;

                    DeleteAnchorNode(middleAnchorNode);
                    DeleteAnchorNode(LineDragAnchorNode);

                    DeleteAnchorNode(AnchorNodes.get(0));
                    AnchorNodes.remove(0);
                    thread_button = false;
                    return;
                }

            } else {
                if (check_measuring_door_command) {
                    if (check_measuring_door_at_bottom) {
                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                        List<Float> point = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);

                        for (int i = 0; i < room_corner_transfer_to_render.size(); i++) {
                            int t2 = (i + 1) % room_corner_transfer_to_render.size();
                            List<Integer> list_p = Arrays.asList(i, t2);
                            if (checkPointIsNearTheSegmentedLine(point, list_p)) {
                                point_to_render = GetPointRightAngledWithLine(point, list_p);
                                list_coordinate_plane_for_door = list_p;
                                System.out.println(list_coordinate_plane_for_door);
                                break;
                            }
                        }

                        bottom_door_coordinate = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);
                        check_measuring_door_at_bottom = false;

                        pose = new Pose(point_to_render, new float[]{0, 0, 0, 1});

                    } else {
                        point_to_render = GetPointTopOfTheDoor(list_coordinate_plane_for_door, (float) (image_dimension[0] / 2), (float) image_dimension[1]);
                        pose = new Pose(point_to_render, new float[]{0, 0, 0, 1});
                        top_door_coordinate = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);
                        doors_coordinate_transfer_to_render.add(Arrays.asList(bottom_door_coordinate, top_door_coordinate));
                        plane_contains_doors.add(list_coordinate_plane_for_door);
                        check_complete_measuring_door = true;

                        DoorNodes.addAll(DrawRectangleBlank(bottom_door_coordinate, top_door_coordinate));

                        List<Float> p1 = bottom_door_coordinate;
                        List<Float> p2 = Arrays.asList(top_door_coordinate.get(0), bottom_door_coordinate.get(1), top_door_coordinate.get(2));
                        List<Float> p3 = top_door_coordinate;
                        List<Float> p4 = Arrays.asList(bottom_door_coordinate.get(0), top_door_coordinate.get(1), bottom_door_coordinate.get(2));

                        DoorNodes.add(DrawFillRectangle(Arrays.asList(p1, p2, p3, p4)));
                    }

                } else if (check_measuring_object_command) {
                    if (check_measuring_object_first){
                        check_measuring_object_first = false;
                        object_coordinates = new ArrayList<>();
                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                        pose = new Pose(point_to_render, new float[]{0, 0, 0, 1});

                        Anchor anchor = session.createAnchor(pose);

                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setSmoothed(true);
                        anchorNode.setRenderable(placedAnchor);

                        object_coordinates.add(Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]));
                        ObjectNodes.add(anchorNode);
                        DrawAnchorNode(anchorNode);

                        check_measuring_object_second = true;

                    } else if (check_measuring_object_second){
                        check_measuring_object_second = false;

                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                        pose = new Pose(point_to_render, new float[]{0, 0, 0, 1});

                        Anchor anchor = session.createAnchor(pose);

                        AnchorNode anchorNode = new AnchorNode(anchor);
                        anchorNode.setSmoothed(true);
                        anchorNode.setRenderable(placedAnchor);

                        object_coordinates.add(Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]));
                        ObjectNodes.add(anchorNode);
                        DrawAnchorNode(anchorNode);
                        ObjectNodes.add(LineBetweenTwoAnchorNodes(ObjectNodes.get(0), ObjectNodes.get(1)));


                        ViewRenderable
                                .builder()
                                .setView(this, R.layout.distance_table_room)
                                .setSizer(new DpToMetersViewSizer(420))
                                .build()
                                .thenAccept(material -> {
                                    distanceCardViewRenderable = material;
                                    distanceCardViewRenderable.setShadowCaster(false);
                                    distanceCardViewRenderable.setShadowReceiver(false);
                                }).exceptionally(
                                throwable -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                    builder.setMessage(throwable.getMessage()).setTitle("Error");
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    return null;
                                });

                        AnchorNode distancenode = DrawViewRendererHorizontal(ObjectNodes.get(0), ObjectNodes.get(1), distanceCardViewRenderable, "cm");
                        DrawAnchorNode(distancenode);

                        check_measuring_object_third = true;

                    } else if (check_measuring_object_third){
                        check_measuring_object_third = false;

                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);

                        List<Float> p = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);

                        point_to_render = GetPointIsParalelWithLineToFormRectangleAndRightAngledWithThatLine(p);

                        object_coordinates.add(Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]));

                        float p4_x = object_coordinates.get(0).get(0) + object_coordinates.get(2).get(0) - object_coordinates.get(1).get(0);
                        float p4_y = object_coordinates.get(0).get(1) + object_coordinates.get(2).get(1) - object_coordinates.get(1).get(1);
                        float p4_z = object_coordinates.get(0).get(2) + object_coordinates.get(2).get(2) - object_coordinates.get(1).get(2);

                        object_coordinates.add(Arrays.asList(p4_x, p4_y, p4_z));

                        for (AnchorNode node:ObjectNodes) DeleteAnchorNode(node);
                        ObjectNodes.clear();

                        ObjectNodes.addAll(DrawRectangleBlank(object_coordinates.get(0), object_coordinates.get(1), object_coordinates.get(2), object_coordinates.get(3)));

                        ViewRenderable
                                .builder()
                                .setView(this, R.layout.distance_table_room)
                                .setSizer(new DpToMetersViewSizer(420))
                                .build()
                                .thenAccept(material -> {
                                    distanceCardViewRenderable = material;
                                    distanceCardViewRenderable.setShadowCaster(false);
                                    distanceCardViewRenderable.setShadowReceiver(false);
                                }).exceptionally(
                                throwable -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                    builder.setMessage(throwable.getMessage()).setTitle("Error");
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    return null;
                                });

                        float[] p1 = new float[]{object_coordinates.get(1).get(0), object_coordinates.get(1).get(1), object_coordinates.get(1).get(2)};
                        Pose pose1 = new Pose(p1, new float[]{0, 0, 0, 1});

                        Anchor anchor_1 = session.createAnchor(pose1);

                        AnchorNode anchorNode_1 = new AnchorNode(anchor_1);
                        anchorNode_1.setSmoothed(true);
                        anchorNode_1.setRenderable(placedAnchor);

                        float[] p2 = new float[]{object_coordinates.get(2).get(0), object_coordinates.get(2).get(1), object_coordinates.get(2).get(2)};
                        Pose pose2 = new Pose(p2, new float[]{0, 0, 0, 1});

                        Anchor anchor_2 = session.createAnchor(pose2);

                        AnchorNode anchorNode_2 = new AnchorNode(anchor_2);
                        anchorNode_2.setSmoothed(true);
                        anchorNode_2.setRenderable(placedAnchor);

                        AnchorNode distancenode = DrawViewRendererHorizontal(anchorNode_1, anchorNode_2, distanceCardViewRenderable, "cm");
                        DrawAnchorNode(distancenode);

                    } else {
                        check_complete_measuring_object = true;

                        List<Float> p = new ArrayList<>(object_coordinates.get(2));
                        point_to_render = convert_2D_to_3D_on_height_from_point(p);

                        float height_of_object = Math.abs(point_to_render[1] - height_of_floor);
                        for (int i = 0; i < 4; i++){
                            List<Float> p_temp = new ArrayList<>(object_coordinates.get(i));
                            object_coordinates.add(Arrays.asList(p_temp.get(0), p_temp.get(1) + height_of_object, p_temp.get(2)));
                        }

                        ObjectNodes.addAll(DrawRectangleBlank(object_coordinates.get(4), object_coordinates.get(5), object_coordinates.get(6), object_coordinates.get(7)));

                        ObjectNodes.add(LineBetweenTwoAnchorNodes(object_coordinates.get(0), object_coordinates.get(4)));
                        ObjectNodes.add(LineBetweenTwoAnchorNodes(object_coordinates.get(1), object_coordinates.get(5)));
                        ObjectNodes.add(LineBetweenTwoAnchorNodes(object_coordinates.get(2), object_coordinates.get(6)));
                        ObjectNodes.add(LineBetweenTwoAnchorNodes(object_coordinates.get(3), object_coordinates.get(7)));

                        ObjectNodes.add(DrawFillCube(Arrays.asList(object_coordinates.get(0), object_coordinates.get(1),
                                object_coordinates.get(2), object_coordinates.get(3),
                                object_coordinates.get(4), object_coordinates.get(5),
                                object_coordinates.get(6), object_coordinates.get(7))));

                        ViewRenderable
                                .builder()
                                .setView(this, R.layout.distance_table_room)
                                .setSizer(new DpToMetersViewSizer(420))
                                .build()
                                .thenAccept(material -> {
                                    distanceCardViewRenderable = material;
                                    distanceCardViewRenderable.setShadowCaster(false);
                                    distanceCardViewRenderable.setShadowReceiver(false);
                                }).exceptionally(
                                throwable -> {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                    builder.setMessage(throwable.getMessage()).setTitle("Error");
                                    AlertDialog dialog = builder.create();
                                    dialog.show();
                                    return null;
                                });


                        float[] p1 = new float[]{object_coordinates.get(1).get(0), object_coordinates.get(1).get(1), object_coordinates.get(1).get(2)};
                        Pose pose1 = new Pose(p1, new float[]{0, 0, 0, 1});

                        Anchor anchor_1 = session.createAnchor(pose1);

                        AnchorNode anchorNode_1 = new AnchorNode(anchor_1);
                        anchorNode_1.setSmoothed(true);
                        anchorNode_1.setRenderable(placedAnchor);

                        float[] p2 = new float[]{object_coordinates.get(5).get(0), object_coordinates.get(5).get(1), object_coordinates.get(5).get(2)};
                        Pose pose2 = new Pose(p2, new float[]{0, 0, 0, 1});

                        Anchor anchor_2 = session.createAnchor(pose2);

                        AnchorNode anchorNode_2 = new AnchorNode(anchor_2);
                        anchorNode_2.setSmoothed(true);
                        anchorNode_2.setRenderable(placedAnchor);

                        AnchorNode distancenode = DrawViewRendererVerticle(anchorNode_1, anchorNode_2, distanceCardViewRenderable, "cm");
                        DrawAnchorNode(distancenode);

                    }
                } else {
                    point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                    pose = new Pose(point_to_render, new float[]{0, 0, 0, 1});

                    List<Float> p = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);
                    room_corner_transfer_to_render.add(p);
                }
            }

            if (check_measuring_door_command) {
                if (check_complete_measuring_door) {
                    ViewRenderable
                            .builder()
                            .setView(this, R.layout.distance_table_room)
                            .setSizer(new DpToMetersViewSizer(420))
                            .build()
                            .thenAccept(material -> {
                                distanceCardViewRenderable = material;
                                distanceCardViewRenderable.setShadowCaster(false);
                                distanceCardViewRenderable.setShadowReceiver(false);
                            }).exceptionally(
                            throwable -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage(throwable.getMessage()).setTitle("Error");
                                AlertDialog dialog = builder.create();
                                dialog.show();
                                return null;
                            });

                    List<Float> position = Arrays.asList(0f, 0f, 0f);
                    position.set(0, (bottom_door_coordinate.get(0) + top_door_coordinate.get(0)) / 2);
                    position.set(1, (bottom_door_coordinate.get(1) + top_door_coordinate.get(1)) / 2);
                    position.set(2, (bottom_door_coordinate.get(2) + top_door_coordinate.get(2)) / 2);

                    List<List<Float>> vector = Arrays.asList(room_corner_transfer_to_render.get(list_coordinate_plane_for_door.get(0)),
                            room_corner_transfer_to_render.get(list_coordinate_plane_for_door.get(1)));

                    AnchorNode distancenode = DrawViewRendererForDoor(position, vector, distanceCardViewRenderable);
                    DrawAnchorNode(distancenode);

                    check_measuring_door_command = false;
                    check_complete_measuring_door = false;
                }

            } else if (check_measuring_object_command){

                if (check_complete_measuring_object){

                    List<Float> position = Arrays.asList(0f, 0f, 0f);
                    position.set(0, (object_coordinates.get(0).get(0) + object_coordinates.get(1).get(0) + object_coordinates.get(2).get(0) + object_coordinates.get(3).get(0)) / 4);
                    position.set(1, object_coordinates.get(4).get(1) + 0.2f);
                    if (position.get(1) > height_of_floor + 1.0f)
                        position.set(1, height_of_floor + 1.0f);

                    position.set(2, (object_coordinates.get(0).get(2) + object_coordinates.get(1).get(2) + object_coordinates.get(2).get(2) + object_coordinates.get(3).get(2)) / 4);

                    List<List<Float>> vector = Arrays.asList(object_coordinates.get(0), object_coordinates.get(1));

                    AnchorNode distancenode = DrawViewRendererForObject(position, vector, ObjectCardViewRenderable);
                    DrawAnchorNode(distancenode);

                    check_measuring_object_command = false;
                    check_complete_measuring_object = false;
                }
            } else {

                Anchor anchor = session.createAnchor(pose);

                AnchorNode anchorNode = new AnchorNode(anchor);

                for(int i = 0; i < AnchorNodes.size(); i++) {
                    if (checkDuplicatePoint(AnchorNodes.get(i), anchorNode)) {
                        if (i == 0) {
                            clickButton.setVisibility(View.GONE);
                            UndoButton.setVisibility(View.GONE);
                            Measuring_Layout.setTranslationZ(100);
                            room_corner_transfer_to_render.remove(room_corner_transfer_to_render.size() - 1);

                            List<Float> position = Arrays.asList(0f, 0f, 0f);
                            position.set(0, (room_corner_transfer_to_render.get(0).get(0) + room_corner_transfer_to_render.get(room_corner_transfer_to_render.size() - 1).get(0)) / 2);
                            position.set(1, room_corner_transfer_to_render.get(0).get(1) + 0.9f);
                            position.set(2, (room_corner_transfer_to_render.get(0).get(2) + room_corner_transfer_to_render.get(room_corner_transfer_to_render.size() - 1).get(2)) / 2);

                            List<List<Float>> vector = Arrays.asList(room_corner_transfer_to_render.get(0),
                                    room_corner_transfer_to_render.get(room_corner_transfer_to_render.size() - 1));

                            AnchorNode distancenode = DrawViewRendererForRoom(position, vector, RoomCardViewRenderable);
                            DrawAnchorNode(distancenode);
                        }
                        anchorNode = AnchorNodes.get(i);
                    }
                }

                anchorNode.setSmoothed(true);
                anchorNode.setRenderable(placedAnchor);

                AnchorNodes.add(anchorNode);

                ViewRenderable
                        .builder()
                        .setView(this, R.layout.distance_table_room)
                        .setSizer(new DpToMetersViewSizer(420))
                        .build()
                        .thenAccept(material -> {
                            distanceCardViewRenderable = material;
                            distanceCardViewRenderable.setShadowCaster(false);
                            distanceCardViewRenderable.setShadowReceiver(false);
                        }).exceptionally(
                        throwable -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(this);
                            builder.setMessage(throwable.getMessage()).setTitle("Error");
                            AlertDialog dialog = builder.create();
                            dialog.show();
                            return null;
                        });

                int n = AnchorNodes.size();
                if (n > 1) {
                    AnchorNode linenode = LineBetweenTwoAnchorNodes(AnchorNodes.get(n - 1), AnchorNodes.get(n - 2));
                    LineNodes.add(linenode);
                    AnchorNode distancenode = DrawViewRendererHorizontal(AnchorNodes.get(n - 1), AnchorNodes.get(n - 2), distanceCardViewRenderable, "m");
                    DistanceBetweenAnchorsCards.add(distancenode);
                }
            }

            thread_button = false;

        });

        UndoButton = findViewById(R.id.button_undo);
        UndoButton.setOnClickListener(v -> {

            thread_button = true;
            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            UndoButton.startAnimation(myAnim);

            int n = AnchorNodes.size();
            if (n <= 1) {
                DeleteAnchorNode(AnchorNodes.get(0));
                AnchorNodes.remove(0);
            } else {
                DeleteAnchorNode(AnchorNodes.get(n - 1));
                DeleteAnchorNode(AnchorNodes.get(n - 1));
                DeleteAnchorNode(LineNodes.get(n - 2));
                DeleteAnchorNode(LineNodes.get(n - 2));
                DeleteAnchorNode(DistanceBetweenAnchorsCards.get(n - 2));

                AnchorNodes.remove(n - 1);
                LineNodes.remove(n - 2);
                DistanceBetweenAnchorsCards.remove(n - 2);
            }
            thread_button = false;
        });

        Button RoomButton = findViewById(R.id.button_room);
        RoomButton.setOnClickListener(v -> {
            thread_button = true;

            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            RoomButton.startAnimation(myAnim);

            check_measuring_door_command = false;
            check_measuring_window_command = false;

            clickButton.setVisibility(View.VISIBLE);
            UndoButton.setVisibility(View.VISIBLE);
            Measuring_Layout.setTranslationZ(0);

            thread_button = false;
        });

        Button DoorButton = findViewById(R.id.button_door);
        DoorButton.setOnClickListener(v -> {
            thread_button = true;

            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            DoorButton.startAnimation(myAnim);

            check_measuring_door_command = true;
            check_measuring_door_at_bottom = true;
            check_measuring_window_command = false;

            clickButton.setVisibility(View.VISIBLE);
            UndoButton.setVisibility(View.VISIBLE);
            Measuring_Layout.setTranslationZ(0);

            thread_button = false;
        });


        Button WindowButton = findViewById(R.id.button_window);
        WindowButton.setOnClickListener(v -> {
            thread_button = true;

            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            WindowButton.startAnimation(myAnim);

            check_measuring_door_command = false;
            check_measuring_window_command = true;

            clickButton.setVisibility(View.VISIBLE);
            UndoButton.setVisibility(View.VISIBLE);
            Measuring_Layout.setTranslationZ(0);

            thread_button = false;
        });

        Button ObjectButton = findViewById(R.id.button_object);
        ObjectButton.setOnClickListener(v -> {
            thread_button = true;

            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            ObjectButton.startAnimation(myAnim);

            check_measuring_object_command = true;
            check_measuring_object_first = true;

            clickButton.setVisibility(View.VISIBLE);
            UndoButton.setVisibility(View.VISIBLE);
            Measuring_Layout.setTranslationZ(0);

            thread_button = false;
        });

        Button SettingButton = findViewById(R.id.button_setting);
        SettingButton.setOnClickListener(v -> {
            thread_button = true;

            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            SettingButton.startAnimation(myAnim);

            clickButton.setVisibility(View.GONE);
            UndoButton.setVisibility(View.GONE);
            Measuring_Layout.setTranslationZ(100);

            thread_button = false;
        });

        Button GenerateButton = findViewById(R.id.button_generate_plan);
        GenerateButton.setOnClickListener(v -> {
            thread_button = true;

            final Animation myAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.bounce_effect);
            GenerateButton.startAnimation(myAnim);


            Intent switchActivityRender = new Intent(this, Render_Sketch_Room.class);
            switchActivityRender.putExtra("height", height_of_room);
            switchActivityRender.putExtra("floor", room_corner_transfer_to_render);
            switchActivityRender.putExtra("door", doors_coordinate_transfer_to_render);
            switchActivityRender.putExtra("plane_for_door", plane_contains_doors);

            System.out.println("floor:");
            System.out.println(room_corner_transfer_to_render);
            System.out.println("door:");
            System.out.println(doors_coordinate_transfer_to_render);
            System.out.println("plane_for_door:");
            System.out.println(plane_contains_doors);

            startActivity(switchActivityRender);

            thread_button = false;
        });


        thread.start();


        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);

        if (arFragment != null) {
            arFragment.getTransformationSystem().setSelectionVisualizer(new CustomVisualizer());
        }

        assert arFragment != null;
        session = arFragment.getArSceneView().getSession();

        initRenderable();

        arFragment.getArSceneView().getScene().addOnUpdateListener(this);

        arFragment.setOnTapArPlaneListener((HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
            arFragment.getArSceneView().getArFrame();
            if (!check_choose_plane) {
                check_choose_plane = true;
                Anchor anchor = hitResult.createAnchor();
                AnchorNode anchorNode = new AnchorNode(anchor);

                height_of_floor = round(anchorNode.getWorldPosition().y, 4);

                arFragment.getArSceneView().getPlaneRenderer().getMaterial().thenAccept(
                        material -> material.setFloat3(
                                PlaneRenderer.MATERIAL_COLOR, new Color(0.0f, 0.0f, 1.0f, 1.0f))
                );

                Config config = Objects.requireNonNull(arFragment.getArSceneView().getSession()).getConfig();
                config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
                arFragment.getArSceneView().getSession().configure(config);
            }

        });
    }

    public float[] toAngles(float[] quat) {
        float[] angles = new float[3];

        float x = quat[0];
        float y = quat[1];
        float z = quat[2];
        float w = quat[3];

        float sqw = w * w;
        float sqx = x * x;
        float sqy = y * y;
        float sqz = z * z;
        float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        float test = quat[0] * quat[1] + quat[2] * quat[3];
        if (test > 0.499 * unit) { // singularity at north pole
            angles[1] = (float) (2 * Math.atan2(x, w));
            angles[2] = (float) (Math.PI / 2);
            angles[0] = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
            angles[1] = (float) (-2 * Math.atan2(x, w));
            angles[2] = (float) (-Math.PI / 2);
            angles[0] = 0;
        } else {
            angles[1] = (float) Math.atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw); // roll or heading
            angles[2] = (float) Math.asin(2 * test / unit); // pitch or attitude
            angles[0] = (float) Math.atan2(2 * x * w - 2 * y * z, -sqx + sqy - sqz + sqw); // yaw or bank
        }
        return angles;
    }

    public static float round(float value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.round(value * scale) / scale;
    }

    private String CheckCameraOrientationOnGround(@NotNull float[] cam_rotation_deg){
        float r0 = (float) (cam_rotation_deg[0] * 180 / Math.PI);
        float r2 = (float) (cam_rotation_deg[2] * 180 / Math.PI);

        if ((r0 > -90 && r0 < 0) && (r2 > -20 && r2 < 20)) return "portrait";

        if ((r0 > -180 && r0 < -90) && (r2 > -20 && r2 < 20)) return "reverse portrait";

        if (r2 > 20 && r2 < 90) return "landscape";

        if (r2 > -90 && r2 < -20) return "reverse landscape";

        return "invalid rotation";
    }

    private String CheckCameraOrientationOnHeight(@NotNull float[] cam_rotation_deg){
        float r0 = (float) (cam_rotation_deg[0] * 180 / Math.PI);
        float r2 = (float) (cam_rotation_deg[2] * 180 / Math.PI);

        if ((r0 > -80 && r0 < 80) && (r2 > -20 && r2 < 20)) return "portrait";

        if (((r0 > -180 && r0 < -100) || (r0 > 100 && r0 < 180)) && (r2 > -20 && r2 < 20)) return "reverse portrait";

        if (r2 > 20 && r2 < 80) return "landscape";

        if (r2 > -80 && r2 < -20) return "reverse landscape";

        return "invalid rotation";
    }


    private float[] convert_2D_to_3D_on_height_from_point(@NotNull List<Float> point_on_ground){
        float x = point_on_ground.get(0);
        float z = point_on_ground.get(2);

        int check_flipped_rotation = 1;
        float roll_rotation = 0;

        float[] cam_quat = Objects.requireNonNull(arFragment.getArSceneView().getArFrame()).getCamera().getDisplayOrientedPose().getRotationQuaternion();
        float[] cam_rotation_deg = toAngles(cam_quat);

        String phone_orientation_status = CheckCameraOrientationOnHeight(cam_rotation_deg);

        if (phone_orientation_status.equals("portrait") || phone_orientation_status.equals("reverse portrait")) {
            roll_rotation = (float) (Math.PI / 2 - cam_rotation_deg[0]);
            if (cam_rotation_deg[0] > 0) check_flipped_rotation = -1;

        } else if (phone_orientation_status.equals("landscape") || phone_orientation_status.equals("reverse landscape")){
            roll_rotation = - cam_rotation_deg[2];
            if (cam_rotation_deg[0] > 0) check_flipped_rotation = -1;
        }


        Vector3 camera_positon = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();

        float distance_at_y_axis_from_cam_to_node = (float) Math.sqrt(Math.pow(camera_positon.x - x, 2) + Math.pow(camera_positon.z - z, 2));
        float distance_height_from_camera_to_node = Math.abs((float) (distance_at_y_axis_from_cam_to_node / Math.tan(roll_rotation)));

        float distance_height_from_node_to_ground = Math.abs(camera_positon.y - height_of_floor) - check_flipped_rotation * distance_height_from_camera_to_node;

        return new float[]{round(x, 4), round(height_of_floor + distance_height_from_node_to_ground, 4), round(z, 4)};
    }

    private float[] convert_2D_to_3D_on_ground(float x, float y){
        float roll_rotation = 0;
        Ray ray = arFragment.getArSceneView().getScene().getCamera().screenPointToRay(x, y);

        float[] cam_quat = Objects.requireNonNull(arFragment.getArSceneView().getArFrame()).getCamera().getDisplayOrientedPose().getRotationQuaternion();
        Vector3 cam_pos  = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
        float[] cam_rotation_deg = toAngles(cam_quat);

        String phone_orientation_status = CheckCameraOrientationOnGround(cam_rotation_deg);

        if (phone_orientation_status.equals("portrait") || phone_orientation_status.equals("reverse portrait")) {
            roll_rotation = (float) (Math.PI / 2 + cam_rotation_deg[0]);
        } else if (phone_orientation_status.equals("landscape") || phone_orientation_status.equals("reverse landscape")){
            roll_rotation = cam_rotation_deg[2];
        }

        float height_from_cam_to_ground = Math.abs(cam_pos.y - height_of_floor);
        float distance_from_cam_to_node = (float) (height_from_cam_to_ground / Math.cos(roll_rotation));

        Vector3 anchor_position = ray.getPoint(distance_from_cam_to_node);

        return new float[]{round(anchor_position.x, 4), round(height_of_floor, 4), round(anchor_position.z, 4)};
    }

    private boolean checkPointIsNearTheSegmentedLine(@NotNull List<Float> point, @NotNull List<Integer> SegmentedLine){
        List<Float> p1 = room_corner_transfer_to_render.get(SegmentedLine.get(0));
        List<Float> p2 = room_corner_transfer_to_render.get(SegmentedLine.get(1));

        float upper_element = Math.abs((point.get(0) - p1.get(0)) * (p1.get(2) - p2.get(2)) - (point.get(2) - p1.get(2)) * (p1.get(0) - p2.get(0)));
        float bottom_element = (float) Math.sqrt(Math.pow(p1.get(0) - p2.get(0), 2) + Math.pow(p1.get(2) - p2.get(2), 2));

        return upper_element / bottom_element < epsilon;
    }

    private float[] GetPointRightAngledWithLine(@NotNull List<Float> point, @NotNull List<Integer> SegmentedLine){
        List<Float> p1 = room_corner_transfer_to_render.get(SegmentedLine.get(0));
        List<Float> p2 = room_corner_transfer_to_render.get(SegmentedLine.get(1));

        float slope = (p1.get(2) - p2.get(2)) / (p1.get(0) - p2.get(0));
        float m = -1 / slope;
        float x = (m * point.get(0) - point.get(2) - slope * p1.get(0) + p1.get(2)) / (m - slope);
        float z = slope * x - slope * p1.get(0) + p1.get(2);

        return new float[]{round(x, 4), height_of_floor, round(z, 4)};
    }

    private float[] GetPointIsParalelWithLineToFormRectangleAndRightAngledWithThatLine(List<Float> point){
        List<Float> p1 = object_coordinates.get(0);
        List<Float> p2 = object_coordinates.get(1);

        float m = (p1.get(2) - p2.get(2)) / (p1.get(0) - p2.get(0));

        float x = (m * point.get(0) - point.get(2) + p2.get(0) / m + p2.get(2)) / (m + 1/m);
        float z = -1/m * (x - p2.get(0)) + p2.get(2);

        return new float[]{round(x, 4), point.get(1), round(z, 4)};
    }

    private float[] GetPointTopOfTheDoor(@NotNull List<Integer> SegmentedLine, float center_x, float center_y){

        float roll_rotation = 0;

        List<Float> p1 = room_corner_transfer_to_render.get(SegmentedLine.get(0));
        List<Float> p2 = room_corner_transfer_to_render.get(SegmentedLine.get(1));

        float max_x = Math.max(p1.get(0), p2.get(0));
        float min_x = Math.min(p1.get(0), p2.get(0));

        Ray ray = arFragment.getArSceneView().getScene().getCamera().screenPointToRay(center_x, center_y);

        float[] cam_quat = Objects.requireNonNull(arFragment.getArSceneView().getArFrame()).getCamera().getDisplayOrientedPose().getRotationQuaternion();
        Vector3 cam_pos = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();

        float[] cam_rotation_deg = toAngles(cam_quat);
        float[] cam_point = new float[]{cam_pos.x, cam_pos.y, cam_pos.z};

        String phone_orientation_status = CheckCameraOrientationOnHeight(cam_rotation_deg);

        if (phone_orientation_status.equals("portrait") || phone_orientation_status.equals("reverse portrait")) {
            roll_rotation = (float) (Math.PI / 2 - cam_rotation_deg[1]);
        } else if (phone_orientation_status.equals("landscape") || phone_orientation_status.equals("reverse landscape")){
            roll_rotation = - cam_rotation_deg[1];
        }

        float angle_of_segmented_line_with_x_axis = (float) Math.atan2(p1.get(2) - p2.get(2), p1.get(0) - p2.get(0));

        float angle_of_phone_direction_with_segmented_line = (float) (Math.PI/2 + angle_of_segmented_line_with_x_axis - roll_rotation);

        float upper_element = Math.abs((cam_point[0] - p1.get(0)) * (p1.get(2) - p2.get(2)) - (cam_point[2] - p1.get(2)) * (p1.get(0) - p2.get(0)));
        float bottom_element = (float) Math.sqrt(Math.pow(p1.get(0) - p2.get(0), 2) + Math.pow(p1.get(2) - p2.get(2), 2));
        float distance_between_cam_pos_and_plane = upper_element / bottom_element;

        float distance_between_camera_to_center_point_of_cam_to_plane = (float) (distance_between_cam_pos_and_plane / Math.cos(angle_of_phone_direction_with_segmented_line));
        Vector3 anchor_position = ray.getPoint(distance_between_camera_to_center_point_of_cam_to_plane);

        float m = (p1.get(2) - p2.get(2))/(p1.get(0) - p2.get(0));
        float x = round(anchor_position.x, 4);
        if (x < min_x) x = min_x;
        if (x > max_x) x = max_x;

        float y = round(anchor_position.y, 4);
        float z = round(m * x - m * p1.get(0) + p1.get(2), 4);
        return new float[]{x, y, z};

    }

    private void DrawAnchorNode(@NotNull AnchorNode node){
        node.setEnabled(true);
        node.setParent(arFragment.getArSceneView().getScene());
        arFragment.getArSceneView().getScene().addChild(node);
    }

    @NotNull
    private List<AnchorNode> DrawRectangleBlank(@NotNull List<Float> p_bottom, @NotNull List<Float> p_top){
        List<Float> p1 = p_bottom;
        List<Float> p2 = Arrays.asList(p_bottom.get(0), p_top.get(1), p_bottom.get(2));
        List<Float> p3 = p_top;
        List<Float> p4 = Arrays.asList(p_top.get(0), p_bottom.get(1), p_top.get(2));

        AnchorNode linenode1 = LineBetweenTwoAnchorNodes(p1, p2);
        AnchorNode linenode2 = LineBetweenTwoAnchorNodes(p2, p3);
        AnchorNode linenode3 = LineBetweenTwoAnchorNodes(p3, p4);
        AnchorNode linenode4 = LineBetweenTwoAnchorNodes(p4, p1);

        return Arrays.asList(linenode1, linenode2, linenode3, linenode4);
    }

    @NotNull
    private List<AnchorNode> DrawRectangleBlank(List<Float> p1, List<Float> p2, List<Float> p3, List<Float> p4){

        AnchorNode linenode1 = LineBetweenTwoAnchorNodes(p1, p2);
        AnchorNode linenode2 = LineBetweenTwoAnchorNodes(p2, p3);
        AnchorNode linenode3 = LineBetweenTwoAnchorNodes(p3, p4);
        AnchorNode linenode4 = LineBetweenTwoAnchorNodes(p4, p1);
        return Arrays.asList(linenode1, linenode2, linenode3, linenode4);
    }

    @NotNull
    private AnchorNode LineBetweenTwoAnchorNodes(@NotNull AnchorNode node1, @NotNull AnchorNode node2){

        Vector3 point_1 = node1.getWorldPosition();
        Vector3 point_2 = node2.getWorldPosition();

        float line_size = distanceBetweenTwoPoints(point_1, point_2);
        float center_x = (point_1.x + point_2.x) / 2;
        float center_y = (point_1.y + point_2.y) / 2;
        float center_z = (point_1.z + point_2.z) / 2;

        Vector3 centerpoint = new Vector3(center_x, center_y, center_z);

        final Vector3 difference = Vector3.subtract(point_1, point_2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            LineBetweenAnchor = ShapeFactory.makeCube(
                                    new Vector3(0.005f, 0.005f, line_size),
                                    Vector3.zero(),
                                    material);
                            LineBetweenAnchor.setShadowCaster(false);
                            LineBetweenAnchor.setShadowReceiver(false);
                        });


        Pose pose = new Pose(new float[]{centerpoint.x, centerpoint.y, centerpoint.z},
                new float[]{rotationFromAToB.x, rotationFromAToB.y, rotationFromAToB.z, rotationFromAToB.w});

        Anchor anchor = session.createAnchor(pose);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setSmoothed(true);
        anchorNode.setRenderable(LineBetweenAnchor);

        DrawAnchorNode(anchorNode);

        return anchorNode;
    }

    @NotNull
    private AnchorNode LineBetweenTwoAnchorNodes(@NotNull List<Float> p1, @NotNull List<Float> p2){

        Vector3 point_1 = new Vector3(p1.get(0), p1.get(1), p1.get(2));
        Vector3 point_2 = new Vector3(p2.get(0), p2.get(1), p2.get(2));

        float line_size = distanceBetweenTwoPoints(point_1, point_2);
        float center_x = (point_1.x + point_2.x) / 2;
        float center_y = (point_1.y + point_2.y) / 2;
        float center_z = (point_1.z + point_2.z) / 2;

        Vector3 centerpoint = new Vector3(center_x, center_y, center_z);

        final Vector3 difference = Vector3.subtract(point_1, point_2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            LineBetweenAnchor = ShapeFactory.makeCube(
                                    new Vector3(0.005f, 0.005f, line_size),
                                    Vector3.zero(),
                                    material);
                            LineBetweenAnchor.setShadowCaster(false);
                            LineBetweenAnchor.setShadowReceiver(false);
                        });

        Pose pose = new Pose(new float[]{centerpoint.x, centerpoint.y, centerpoint.z},
                new float[]{rotationFromAToB.x, rotationFromAToB.y, rotationFromAToB.z, rotationFromAToB.w});

        Anchor anchor = session.createAnchor(pose);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setSmoothed(true);
        anchorNode.setRenderable(LineBetweenAnchor);

        DrawAnchorNode(anchorNode);

        return anchorNode;
    }


    @NotNull
    private AnchorNode DrawFillRectangle(@NotNull List<List<Float>> points){
        Vector3 p1 = new Vector3(points.get(0).get(0), points.get(0).get(1), points.get(0).get(2));
        Vector3 p2 = new Vector3(points.get(1).get(0), points.get(1).get(1), points.get(1).get(2));
        Vector3 p3 = new Vector3(points.get(2).get(0), points.get(2).get(1), points.get(2).get(2));
        Vector3 p4 = new Vector3(points.get(3).get(0), points.get(3).get(1), points.get(3).get(2));

        float width = distanceBetweenTwoPoints(p1, p2);
        float length = distanceBetweenTwoPoints(p1, p4);

        float center_x = (p1.x + p2.x + p3.x + p4.x) / 4;
        float center_y = (p1.y + p2.y + p3.y + p4.y) / 4;
        float center_z = (p1.z + p2.z + p3.z + p4.z) / 4;

        Vector3 centerpoint = new Vector3(center_x, center_y, center_z);

        final Vector3 difference = Vector3.subtract(p1, p2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

        if (Math.abs(p4.y - p1.y) < 0.001f){

            MaterialFactory.makeTransparentWithColor(this, new Color(1, 1, 1, 0))
                    .thenAccept(
                            material -> {
                                RectangleRender = ShapeFactory.makeCube(
                                        new Vector3(length, 0.005f, width),
                                        Vector3.zero(),
                                        material);
                                RectangleRender.setShadowCaster(false);
                                RectangleRender.setShadowReceiver(false);
                            });
        } else {

            MaterialFactory.makeTransparentWithColor(this, new Color(1, 1, 1, 0))
                    .thenAccept(
                            material -> {
                                RectangleRender = ShapeFactory.makeCube(
                                        new Vector3(0.005f, length, width),
                                        Vector3.zero(),
                                        material);
                                RectangleRender.setShadowCaster(false);
                                RectangleRender.setShadowReceiver(false);
                            });
        }
        Pose pose = new Pose(new float[]{centerpoint.x, centerpoint.y, centerpoint.z},
                new float[]{rotationFromAToB.x, rotationFromAToB.y, rotationFromAToB.z, rotationFromAToB.w});

        Anchor anchor = session.createAnchor(pose);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setSmoothed(true);
        anchorNode.setRenderable(RectangleRender);

        DrawAnchorNode(anchorNode);
        return anchorNode;
    }

    private AnchorNode DrawFillCube(List<List<Float>> points){
        Vector3 p1 = new Vector3(points.get(0).get(0), points.get(0).get(1), points.get(0).get(2));
        Vector3 p2 = new Vector3(points.get(1).get(0), points.get(1).get(1), points.get(1).get(2));
        Vector3 p3 = new Vector3(points.get(2).get(0), points.get(2).get(1), points.get(2).get(2));
        Vector3 p4 = new Vector3(points.get(4).get(0), points.get(4).get(1), points.get(4).get(2));

        float width = distanceBetweenTwoPoints(p1, p2);
        float length = distanceBetweenTwoPoints(p2, p3);
        float height = distanceBetweenTwoPoints(p1, p4);

        float center_x = (p1.x + p3.x) / 2;
        float center_y = (p1.y + p4.y) / 2;
        float center_z = (p1.z + p3.z) / 2;

        Vector3 centerpoint = new Vector3(center_x, center_y, center_z);

        final Vector3 difference = Vector3.subtract(p1, p2);
        final Vector3 directionFromTopToBottom = difference.normalized();
        final Quaternion rotationFromAToB =
                Quaternion.lookRotation(directionFromTopToBottom, Vector3.up());

        MaterialFactory.makeTransparentWithColor(this, new Color(1, 1, 1, 0))
                .thenAccept(
                        material -> {
                            CubeRender = ShapeFactory.makeCube(
                                    new Vector3(length, height, width),
                                    Vector3.zero(),
                                    material);
                            CubeRender.setShadowCaster(false);
                            CubeRender.setShadowReceiver(false);
                        });

        Pose pose = new Pose(new float[]{centerpoint.x, centerpoint.y, centerpoint.z},
                new float[]{rotationFromAToB.x, rotationFromAToB.y, rotationFromAToB.z, rotationFromAToB.w});

        Anchor anchor = session.createAnchor(pose);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setSmoothed(true);
        anchorNode.setRenderable(CubeRender);

        DrawAnchorNode(anchorNode);

        return anchorNode;
    }


    private float CalculateAreaOfPolygon(List<List<Float>> ListOf2DPoint){
        float initial = (float) 0.0;
        int n = ListOf2DPoint.size();
        for (int i = 0; i < n; i++){
            List<Float> p1 = ListOf2DPoint.get(i);
            List<Float> p2 = (i==n-1) ? ListOf2DPoint.get(0) : ListOf2DPoint.get(i + 1);

            initial += p1.get(0) * p2.get(1) - p1.get(1) * p2.get(0);
        }

        return Math.abs(initial) / 2;
    }

    private float CalculateAreaOfFloor(){
        List<List<Float>> ListOfPoint = new ArrayList<>();
        for (AnchorNode node:AnchorNodes){
            Vector3 pos = node.getWorldPosition();
            ListOfPoint.add(Arrays.asList(pos.x, pos.z));
        }

        return CalculateAreaOfPolygon(ListOfPoint);
    }

    private float CalculateVolumeOfRoom(){
        float area = CalculateAreaOfFloor();

        return area * height_of_room;
    }

    private void DeleteAnchorNode(AnchorNode node){
        arFragment.getArSceneView().getScene().removeChild(node);
        node.setEnabled(false);
        node.setParent(null);
    }

    private boolean checkDuplicatePoint(@NotNull AnchorNode node1, @NotNull AnchorNode node2){
        Vector3 pos1 = node1.getWorldPosition();
        Vector3 pos2 = node2.getWorldPosition();

        float distance = distanceBetweenTwoPoints(pos1, pos2);

        return distance < epsilon;
    }

    private float distanceBetweenTwoPoints(@NotNull Vector3 point1, @NotNull Vector3 point2){
        return (float) Math.sqrt(Math.pow(point1.x - point2.x, 2) + Math.pow(point1.y - point2.y, 2) + Math.pow(point1.z - point2.z, 2));
    }

    private void initRenderable() {
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(
                        material -> {
                            placedAnchor = ShapeFactory.makeSphere(
                                    0.015f,
                                    Vector3.zero(),
                                    material);
                            placedAnchor.setShadowCaster(false);
                            placedAnchor.setShadowReceiver(false);
                        });

        ViewRenderable
                .builder()
                .setView(this, R.layout.distance_table_room)
                .setSizer(new DpToMetersViewSizer(420))
                .build()
                .thenAccept(material -> {
                    RoomCardViewRenderable = material;
                    RoomCardViewRenderable.setShadowCaster(false);
                    RoomCardViewRenderable.setShadowReceiver(false);
                }).exceptionally(
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage()).setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                });

        ViewRenderable
                .builder()
                .setView(this, R.layout.distance_table_room)
                .setSizer(new DpToMetersViewSizer(420))
                .build()
                .thenAccept(material -> {
                    ObjectCardViewRenderable = material;
                    ObjectCardViewRenderable.setShadowCaster(false);
                    ObjectCardViewRenderable.setShadowReceiver(false);
                }).exceptionally(
                throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage()).setTitle("Error");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                });
    }

    private AnchorNode placeMidAnchor(Pose pose, Renderable viewdistance) {
        Anchor anchor = Objects.requireNonNull(arFragment.getArSceneView().getSession()).
                createAnchor(pose);

        AnchorNode anchorNode = new AnchorNode(anchor);
        anchorNode.setSmoothed(true);
        anchorNode.setRenderable(viewdistance);

        DrawAnchorNode(anchorNode);
        return anchorNode;
    }

    public AnchorNode DrawViewRendererHorizontal(AnchorNode node1, AnchorNode node2, ViewRenderable viewdistance, String params){

        Vector3 point1 = node1.getWorldPosition();
        Vector3 point2 = node2.getWorldPosition();

        float[] midPosition = {
                (point1.x + point2.x) / 2 + 0.02f,
                (point1.y + point2.y) / 2 + 0.02f,
                (point1.z + point2.z) / 2
        };

        Vector3 difference = Vector3.subtract(point1, point2);
        Vector3 directionFromTopToBottom = difference.normalized();
        Quaternion rotationFromAToB =
                Quaternion.lookRotation(Vector3.up(), directionFromTopToBottom);

        Quaternion finalrotation = Quaternion.multiply(rotationFromAToB, Quaternion.axisAngle(new Vector3(0,0,1), 90));

        float[] quaternion = {finalrotation.x, finalrotation.y, finalrotation.z, finalrotation.w};
        Pose pose = new Pose(midPosition, quaternion);

        if (params.equals("m"))
            measureDistanceOf2Points(point1, point2, viewdistance, "m");
        else if (params.equals("cm"))
            measureDistanceOf2Points(point1, point2, viewdistance, "cm");
        return placeMidAnchor(pose, viewdistance);
    }

    public AnchorNode DrawViewRendererVerticle(AnchorNode node1, AnchorNode node2, ViewRenderable viewdistance, String params){

        Vector3 point1 = node1.getWorldPosition();
        Vector3 point2 = node2.getWorldPosition();

        float[] midPosition = {
                (point1.x + point2.x) / 2,
                (point1.y + point2.y) / 2 + 0.01f,
                (point1.z + point2.z) / 2
        };

        Vector3 difference = Vector3.subtract(point1, point2);
        Vector3 directionFromTopToBottom = difference.normalized();
        Quaternion rotationFromAToB =
                Quaternion.lookRotation(Vector3.up(), directionFromTopToBottom);

        Quaternion finalrotation = Quaternion.multiply(rotationFromAToB, Quaternion.axisAngle(new Vector3(0,1,0), 90));
        finalrotation = Quaternion.multiply(finalrotation, Quaternion.axisAngle(new Vector3(0,0,1), -90));

        float[] quaternion = {finalrotation.x, finalrotation.y, finalrotation.z, finalrotation.w};
        Pose pose = new Pose(midPosition, quaternion);

        if (params.equals("m"))
            measureDistanceOf2Points(point1, point2, viewdistance, "m");
        else if (params.equals("cm"))
            measureDistanceOf2Points(point1, point2, viewdistance, "cm");
        return placeMidAnchor(pose, viewdistance);
    }

    public AnchorNode DrawViewRendererForDoor(List<Float> position, List<List<Float>> ListTwoPoint, ViewRenderable viewdistance){

        Vector3 point1 = new Vector3(ListTwoPoint.get(0).get(0), ListTwoPoint.get(0).get(1), ListTwoPoint.get(0).get(2));
        Vector3 point2 = new Vector3(ListTwoPoint.get(1).get(0), ListTwoPoint.get(1).get(1), ListTwoPoint.get(1).get(2));
        Vector3 difference = Vector3.subtract(point1, point2);
        Vector3 directionFromTopToBottom = difference.normalized();
        Quaternion rotationFromAToB =
                Quaternion.lookRotation(Vector3.up(), directionFromTopToBottom);

        Quaternion finalrotation = Quaternion.multiply(rotationFromAToB, Quaternion.axisAngle(new Vector3(0,1,0), 90));
        finalrotation = Quaternion.multiply(finalrotation, Quaternion.axisAngle(new Vector3(0,0,1), -90));

        float[] quaternion = {finalrotation.x, finalrotation.y, finalrotation.z, finalrotation.w};
        float[] midPosition = new float[]{position.get(0), position.get(1), position.get(2)};
        Pose pose = new Pose(midPosition, quaternion);

        MeasureDoor(viewdistance);
        return placeMidAnchor(pose, viewdistance);
    }

    public AnchorNode DrawViewRendererForRoom(List<Float> position, List<List<Float>> ListTwoPoint, ViewRenderable viewdistance){

        Vector3 point1 = new Vector3(ListTwoPoint.get(0).get(0), ListTwoPoint.get(0).get(1), ListTwoPoint.get(0).get(2));
        Vector3 point2 = new Vector3(ListTwoPoint.get(1).get(0), ListTwoPoint.get(1).get(1), ListTwoPoint.get(1).get(2));
        Vector3 difference = Vector3.subtract(point1, point2);
        Vector3 directionFromTopToBottom = difference.normalized();
        Quaternion rotationFromAToB =
                Quaternion.lookRotation(Vector3.up(), directionFromTopToBottom);

        Quaternion finalrotation = Quaternion.multiply(rotationFromAToB, Quaternion.axisAngle(new Vector3(0,1,0), 90));
        finalrotation = Quaternion.multiply(finalrotation, Quaternion.axisAngle(new Vector3(0,0,1), -90));

        float[] quaternion = {finalrotation.x, finalrotation.y, finalrotation.z, finalrotation.w};
        float[] midPosition = new float[]{position.get(0), position.get(1), position.get(2)};
        Pose pose = new Pose(midPosition, quaternion);

        MeasureRoom(viewdistance);
        return placeMidAnchor(pose, viewdistance);
    }

    public AnchorNode DrawViewRendererForObject(List<Float> position, List<List<Float>> ListTwoPoint, ViewRenderable viewdistance){

        Vector3 point1 = new Vector3(ListTwoPoint.get(0).get(0), ListTwoPoint.get(0).get(1), ListTwoPoint.get(0).get(2));
        Vector3 point2 = new Vector3(ListTwoPoint.get(1).get(0), ListTwoPoint.get(1).get(1), ListTwoPoint.get(1).get(2));
        Vector3 difference = Vector3.subtract(point1, point2);
        Vector3 directionFromTopToBottom = difference.normalized();
        Quaternion rotationFromAToB =
                Quaternion.lookRotation(Vector3.up(), directionFromTopToBottom);

        Quaternion finalrotation = Quaternion.multiply(rotationFromAToB, Quaternion.axisAngle(new Vector3(0,1,0), 90));
        finalrotation = Quaternion.multiply(finalrotation, Quaternion.axisAngle(new Vector3(0,0,1), -90));

        float[] quaternion = {finalrotation.x, finalrotation.y, finalrotation.z, finalrotation.w};
        float[] midPosition = new float[]{position.get(0), position.get(1), position.get(2)};
        Pose pose = new Pose(midPosition, quaternion);

        MeasureObject(viewdistance);
        return placeMidAnchor(pose, viewdistance);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void MeasureRoom(ViewRenderable viewdistance) {

        float area = round(CalculateAreaOfFloor(), 2);
        float volume = round(CalculateVolumeOfRoom(), 2);

        String height_string = String.format("Height: %.2f m", height_of_room);
        String volume_string = String.format("Volume: %.2f m³", volume);
        String area_string = String.format("Area: %.2f m²", area);

        TextView textView = viewdistance.getView().findViewById(R.id.distanceCardRoom);
        textView.setText("Room:\n" + height_string + "\n" + area_string + "\n" + volume_string);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void MeasureDoor(ViewRenderable viewdistance) {

        float height_of_door = round(Math.abs(top_door_coordinate.get(1) - bottom_door_coordinate.get(1)), 2);
        float width = round((float) Math.sqrt(Math.pow(top_door_coordinate.get(0) - bottom_door_coordinate.get(0), 2) +
                Math.pow(top_door_coordinate.get(2) - bottom_door_coordinate.get(2), 2)), 2);

        float area = round(width * height_of_door, 2);

        String height_string = String.format("Height: %.2fm", height_of_door);
        String width_string = String.format("Width: %.2fm", width);
        String area_string = String.format("Area: %.2fm²", area);

        TextView textView = viewdistance.getView().findViewById(R.id.distanceCardRoom);
        textView.setText("Door:\n" + height_string + "\n" + width_string + "\n" + area_string);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void MeasureObject(ViewRenderable viewdistance) {

        float height_of_object = round(Math.abs(object_coordinates.get(4).get(1) - height_of_floor) * 100, 2);
        float width = round((float) Math.sqrt(Math.pow(object_coordinates.get(0).get(0) - object_coordinates.get(1).get(0), 2) +
                Math.pow(object_coordinates.get(0).get(2) - object_coordinates.get(1).get(2), 2)) * 100, 2);
        float length = round((float) Math.sqrt(Math.pow(object_coordinates.get(1).get(0) - object_coordinates.get(2).get(0), 2) +
                Math.pow(object_coordinates.get(1).get(2) - object_coordinates.get(2).get(2), 2)) * 100, 2);


        String height_string = String.format("Height: %.2fcm", height_of_object);
        String width_string = String.format("Width: %.2fcm", width);
        String length_string = String.format("Length: %.2fcm", length);

        TextView textView = viewdistance.getView().findViewById(R.id.distanceCardRoom);
        textView.setText(height_string + "\n" + width_string +  "\n" + length_string);
    }

    @SuppressLint("DefaultLocale")
    private void measureDistanceOf2Points(Vector3 point1, Vector3 point2, ViewRenderable viewdistance, String params) {
        if (point1.length() == 0 || point2.length() == 0) return;

        float distanceMeter = distanceBetweenTwoPoints(point1, point2);

        String distanceTextWith = "";
        if (params.equals("cm")) {
            distanceMeter *= 100;
            distanceTextWith = String.format("%.2fcm", distanceMeter);
        } else if (params.equals("m")) {
            distanceTextWith = String.format("%.2fm", distanceMeter);
        }
        TextView textView = viewdistance.getView().findViewById(R.id.distanceCardRoom);
        textView.setText(distanceTextWith);
    }

    boolean checkIsSupportedDeviceOrFinish(Activity activity) {
        ActivityManager systemService =
                (ActivityManager) Objects.requireNonNull(activity
                        .getSystemService(Context.ACTIVITY_SERVICE));
        String openGLVersionString = systemService.getDeviceConfigurationInfo().getGlEsVersion();

        if (Double.parseDouble(openGLVersionString) < Constants.MIN_OPENGL_VERSION) {
            @SuppressLint("DefaultLocale") String msg = String.format("Sceneform requires OpenGL ES %f later", Constants.MIN_OPENGL_VERSION);
            Log.e(TAG, msg);
            Toast.makeText(activity, msg, Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        if (!check_choose_plane || thread_button) return;

        try {

            if (middleAnchorNode != null) {
                DeleteAnchorNode(middleAnchorNode);
                middleAnchorNode = null;
            }
            if (LineDragAnchorNode != null) {
                DeleteAnchorNode(LineDragAnchorNode);
                LineDragAnchorNode = null;
            }

            if (middleDoorAnchorNode != null){
                for (AnchorNode node:middleDoorAnchorNode) DeleteAnchorNode(node);
                middleDoorAnchorNode = null;
            }

            if (middleObjectAnchorNode != null){
                for (AnchorNode node:middleObjectAnchorNode) DeleteAnchorNode(node);
                middleObjectAnchorNode = null;
            }

            for(AnchorNode node:AnchorNodes) DeleteAnchorNode(node);

            Pose pose;
            float[] point_to_render;

            if (!check_measuring_height_of_room){
                if (!check_get_floor_point_to_measure_height){
                    point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                } else {
                    Vector3 temp = AnchorNodes.get(0).getWorldPosition();
                    List<Float> p = Arrays.asList(temp.x, temp.y, temp.z);
                    point_to_render = convert_2D_to_3D_on_height_from_point(p);
                }
            } else{
                if (check_measuring_door_command){
                    if (check_measuring_door_at_bottom) {
                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                        List<Float> point = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);

                        for (int i = 0; i < room_corner_transfer_to_render.size(); i++) {
                            int t2 = (i == room_corner_transfer_to_render.size() - 1) ? 0 : (i + 1);
                            if (checkPointIsNearTheSegmentedLine(point, Arrays.asList(i, t2)))
                                point_to_render = GetPointRightAngledWithLine(point, Arrays.asList(i, t2));
                        }
                    } else{
                        point_to_render = GetPointTopOfTheDoor(list_coordinate_plane_for_door, (float) (image_dimension[0] / 2), (float) image_dimension[1]);

                        middleDoorAnchorNode = new ArrayList<>();
                        middleDoorAnchorNode.addAll(DrawRectangleBlank(bottom_door_coordinate, Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2])));


                        List<Float> p1 = new ArrayList<>(bottom_door_coordinate);
                        List<Float> p2 = Arrays.asList(point_to_render[0], bottom_door_coordinate.get(1), point_to_render[2]);
                        List<Float> p3 = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);
                        List<Float> p4 = Arrays.asList(bottom_door_coordinate.get(0), point_to_render[1], bottom_door_coordinate.get(2));

                        middleDoorAnchorNode.add(DrawFillRectangle(Arrays.asList(p1, p2, p3, p4)));

                        for (AnchorNode node:AnchorNodes) DrawAnchorNode(node);
                        return;
                    }
                } else if (check_measuring_object_command){
                    if (check_measuring_object_first){
                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);

                    } else if (check_measuring_object_second){

                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                        List<Float> p = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);

                        middleObjectAnchorNode = new ArrayList<>();
                        middleObjectAnchorNode.add(LineBetweenTwoAnchorNodes(object_coordinates.get(0), p));

                    } else if (check_measuring_object_third){

                        point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);

                        List<Float> p = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);

                        point_to_render = GetPointIsParalelWithLineToFormRectangleAndRightAngledWithThatLine(p);

                        List<Float> p3 = Arrays.asList(point_to_render[0], point_to_render[1], point_to_render[2]);

                        float p4_x = object_coordinates.get(0).get(0) + p3.get(0) - object_coordinates.get(1).get(0);
                        float p4_y = object_coordinates.get(0).get(1) + p3.get(1) - object_coordinates.get(1).get(1);
                        float p4_z = object_coordinates.get(0).get(2) + p3.get(2) - object_coordinates.get(1).get(2);

                        List<Float> p4 = Arrays.asList(p4_x, p4_y, p4_z);

                        middleObjectAnchorNode = new ArrayList<>();
                        middleObjectAnchorNode.addAll(DrawRectangleBlank(object_coordinates.get(0), object_coordinates.get(1), p3, p4));

                        middleObjectAnchorNode.add(DrawFillRectangle(Arrays.asList(object_coordinates.get(0), object_coordinates.get(1), p3, p4)));

                        for (AnchorNode node:AnchorNodes) DrawAnchorNode(node);
                        return;
                    } else {
                        List<Float> p = new ArrayList<>(object_coordinates.get(2));
                        point_to_render = convert_2D_to_3D_on_height_from_point(p);

                        float height_of_object = Math.abs(point_to_render[1] - height_of_floor);

                        List<List<Float>> object_coordinates_temp = new ArrayList<>();
                        for (int i = 0; i < 4; i++){
                            List<Float> p_temp = new ArrayList<>(object_coordinates.get(i));
                            object_coordinates_temp.add(Arrays.asList(p_temp.get(0), p_temp.get(1) + height_of_object, p_temp.get(2)));
                        }

                        middleObjectAnchorNode = new ArrayList<>();

                        middleObjectAnchorNode.addAll(DrawRectangleBlank(object_coordinates.get(0), object_coordinates.get(1), object_coordinates.get(2), object_coordinates.get(3)));
                        middleObjectAnchorNode.addAll(DrawRectangleBlank(object_coordinates_temp.get(0), object_coordinates_temp.get(1), object_coordinates_temp.get(2), object_coordinates_temp.get(3)));

                        middleObjectAnchorNode.add(LineBetweenTwoAnchorNodes(object_coordinates.get(0), object_coordinates_temp.get(0)));
                        middleObjectAnchorNode.add(LineBetweenTwoAnchorNodes(object_coordinates.get(1), object_coordinates_temp.get(1)));
                        middleObjectAnchorNode.add(LineBetweenTwoAnchorNodes(object_coordinates.get(2), object_coordinates_temp.get(2)));
                        middleObjectAnchorNode.add(LineBetweenTwoAnchorNodes(object_coordinates.get(3), object_coordinates_temp.get(3)));


                        middleObjectAnchorNode.add(DrawFillCube(Arrays.asList(object_coordinates.get(0), object_coordinates.get(1),
                                object_coordinates.get(2), object_coordinates.get(3),
                                object_coordinates_temp.get(0), object_coordinates_temp.get(1),
                                object_coordinates_temp.get(2), object_coordinates_temp.get(3))));

                        for (AnchorNode node:AnchorNodes) DrawAnchorNode(node);
                        return;
                    }
                }

                else {
                    point_to_render = convert_2D_to_3D_on_ground((float) (image_dimension[0] / 2), (float) image_dimension[1]);
                }
            }

            pose = new Pose(point_to_render, new float[]{0, 0, 0, 1});

            session = arFragment.getArSceneView().getSession();
            assert session != null;
            Anchor midanchor = session.createAnchor(pose);

            middleAnchorNode = new AnchorNode(midanchor);

            for (AnchorNode node:AnchorNodes){
                if (checkDuplicatePoint(middleAnchorNode, node)){
                    middleAnchorNode = node;
                    continue;
                }
                DrawAnchorNode(node);
            }

            middleAnchorNode.setSmoothed(true);
            middleAnchorNode.setRenderable(placedAnchor);
            DrawAnchorNode(middleAnchorNode);

            int n = AnchorNodes.size();
            if (n > 0 && !check_measuring_door_command && !check_measuring_window_command && !check_measuring_object_command){
                if (!checkDuplicatePoint(middleAnchorNode, AnchorNodes.get(n - 1))) {
                    LineDragAnchorNode = LineBetweenTwoAnchorNodes(middleAnchorNode, AnchorNodes.get(n - 1));
                }
            }

        } catch (Exception ignored){}

    }
}
