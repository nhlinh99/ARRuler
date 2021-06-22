package com.arruler.ViewRender3D;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.arruler.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Render_Sketch_Room extends Activity {

    private float mPreviousX = 0.0f;
    private float mPreviousY = 0.0f;
    private float mCubeRotationX = 0.0f;
    private float mCubeRotationY = 0.0f;
    private final float TOUCH_SCALE_FACTOR = 1.0f;

    private final List<List<List<Float>>> vertices = new ArrayList<>();
    private final List<Boolean> check_door_each_vertices = new ArrayList<>();
    private float height_of_room = 0f;
    private float SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ = 10f;
    private float SCALE_TRANSFER_REAL_WORLD_TO_RENDER_Y = 7f;

    private final List<List<Float>> all_vertices = new ArrayList<>();

    private float[] vertices_render = null;
    private byte[] indices_render = null;
    private float[] colors = null;

    public List<Float> MoveVertices(List<Float> moving_direction, List<Float> point){
        return Arrays.asList(point.get(0) + moving_direction.get(0), point.get(1) + moving_direction.get(1), point.get(2) + moving_direction.get(2));
    }

    public static float round(float value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (float) Math.round(value * scale) / scale;
    }

    public List<List<Float>> GenerateVertices(float height, List<List<Float>> vertices_list, boolean check_door){

        //front
        List<Float> wall_bottom_left = vertices_list.get(0);
        List<Float> wall_bottom_right = vertices_list.get(1);
        List<Float> wall_top_left = Arrays.asList(wall_bottom_left.get(0), wall_bottom_left.get(1) + height, wall_bottom_left.get(2));
        List<Float> wall_top_right = Arrays.asList(wall_bottom_right.get(0), wall_bottom_right.get(1) + height, wall_bottom_right.get(2));

        float x_middle = (wall_bottom_left.get(0) + wall_bottom_right.get(0)) / 2;
        float z_middle = (wall_bottom_left.get(2) + wall_bottom_right.get(2)) / 2;

        float x_moving_vertice_back = (float) (Math.abs(wall_bottom_left.get(2) - wall_bottom_right.get(2))
                / Math.sqrt(Math.pow(wall_bottom_left.get(2) - wall_bottom_right.get(2), 2) + Math.pow(wall_bottom_left.get(0) - wall_bottom_right.get(0), 2)));
        x_moving_vertice_back = round(x_moving_vertice_back, 3);

        float z_moving_vertice_back = (float) (Math.abs(wall_bottom_left.get(0) - wall_bottom_right.get(0))
                / Math.sqrt(Math.pow(wall_bottom_left.get(2) - wall_bottom_right.get(2), 2) + Math.pow(wall_bottom_left.get(0) - wall_bottom_right.get(0), 2)));
        z_moving_vertice_back = round(z_moving_vertice_back, 3);

        List<Float> moving_vertice_back;
        if (x_middle < 0){
            if (z_middle < 0){
                moving_vertice_back = Arrays.asList(- x_moving_vertice_back, 0f, - z_moving_vertice_back);
            } else{
                moving_vertice_back = Arrays.asList(- x_moving_vertice_back, 0f, z_moving_vertice_back);
            }
        } else {
            if (z_middle < 0){
                moving_vertice_back = Arrays.asList(x_moving_vertice_back, 0f, - z_moving_vertice_back);
            } else{
                moving_vertice_back = Arrays.asList(x_moving_vertice_back, 0f, z_moving_vertice_back);
            }
        }

        //back
        List<Float> wall_bottom_left_b = MoveVertices(moving_vertice_back, wall_bottom_left);
        List<Float> wall_bottom_right_b = MoveVertices(moving_vertice_back, wall_bottom_right);
        List<Float> wall_top_left_b = MoveVertices(moving_vertice_back, wall_top_left);
        List<Float> wall_top_right_b = MoveVertices(moving_vertice_back, wall_top_right);

        if (check_door){
            //front
            List<Float> door_bottom_left = vertices_list.get(2);
            List<Float> door_top_right = vertices_list.get(3);
            List<Float> door_top_left = Arrays.asList(door_bottom_left.get(0), door_top_right.get(1), door_bottom_left.get(2));
            List<Float> door_bottom_right = Arrays.asList(door_top_right.get(0), door_bottom_left.get(1), door_top_right.get(2));

            //back
            List<Float> door_bottom_left_b = MoveVertices(moving_vertice_back, door_bottom_left);
            List<Float> door_bottom_right_b = MoveVertices(moving_vertice_back, door_bottom_right);
            List<Float> door_top_left_b = MoveVertices(moving_vertice_back, door_top_left);
            List<Float> door_top_right_b = MoveVertices(moving_vertice_back, door_top_right);

            return Arrays.asList(wall_bottom_left, wall_top_left, door_bottom_left, door_top_left,
                    door_bottom_right, door_top_right, wall_bottom_right, wall_top_right,
                    wall_bottom_left_b, wall_top_left_b, door_bottom_left_b, door_top_left_b,
                    door_bottom_right_b, door_top_right_b, wall_bottom_right_b, wall_top_right_b);
        }

        return Arrays.asList(wall_bottom_left, wall_top_left, wall_bottom_right, wall_top_right,
                wall_bottom_left_b, wall_top_left_b, wall_bottom_right_b, wall_top_right_b);
    }

    public List<Integer> GenerateIndicesForRender(List<Integer> indices, boolean check_door){
        List<Integer> indices_render;

        if (!check_door){
            indices_render = Arrays.asList(
                    indices.get(0), indices.get(1), indices.get(2), indices.get(1), indices.get(2), indices.get(3), //front
                    indices.get(0), indices.get(1), indices.get(4), indices.get(1), indices.get(4), indices.get(5), //left
                    indices.get(2), indices.get(3), indices.get(6), indices.get(3), indices.get(6), indices.get(7), //right
                    indices.get(4), indices.get(5), indices.get(6), indices.get(5), indices.get(6), indices.get(7), //back
                    indices.get(1), indices.get(3), indices.get(5), indices.get(3), indices.get(5), indices.get(7), //top
                    indices.get(0), indices.get(2), indices.get(4), indices.get(2), indices.get(4), indices.get(6));//bottom
        } else{
            indices_render = Arrays.asList(
                    indices.get(0), indices.get(1), indices.get(2), indices.get(1), indices.get(2), indices.get(3), //front
                    indices.get(1), indices.get(3), indices.get(5), indices.get(1), indices.get(5), indices.get(7),
                    indices.get(4), indices.get(5), indices.get(6), indices.get(5), indices.get(6), indices.get(7),
                    indices.get(0), indices.get(1), indices.get(8), indices.get(1), indices.get(8), indices.get(9), //left
                    indices.get(6), indices.get(7), indices.get(14), indices.get(7), indices.get(14), indices.get(15), //right
                    indices.get(1), indices.get(7), indices.get(9), indices.get(7), indices.get(9), indices.get(15), //top
                    indices.get(8), indices.get(9), indices.get(10), indices.get(9), indices.get(10), indices.get(11), //back
                    indices.get(9), indices.get(11), indices.get(13), indices.get(11), indices.get(13), indices.get(15),
                    indices.get(12), indices.get(13), indices.get(14), indices.get(13), indices.get(14), indices.get(15),
                    indices.get(2), indices.get(3), indices.get(10), indices.get(3), indices.get(10), indices.get(11),//door
                    indices.get(3), indices.get(5), indices.get(11), indices.get(5), indices.get(11), indices.get(13),
                    indices.get(4), indices.get(5), indices.get(12), indices.get(5), indices.get(12), indices.get(13),
                    indices.get(0), indices.get(2), indices.get(8), indices.get(2), indices.get(8), indices.get(10),//bottom
                    indices.get(4), indices.get(6), indices.get(12), indices.get(6), indices.get(12), indices.get(14));
        }

        return indices_render;
    }

    public List<Integer> getIndices(List<List<Float>> AllVertices, List<List<Float>> Generated_Vertices){
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < Generated_Vertices.size(); i++){
            int indice = AllVertices.indexOf(Generated_Vertices.get(i));
            indices.add(indice);
        }

        return indices;
    }

    public void AddVertices(List<List<Float>> vertices_add){
        for (List<Float> vertice:vertices_add){
            if (!all_vertices.contains(vertice)){
                all_vertices.add(vertice);
            }
        }
    }

    public void TestVertices(){

        List<Integer> generated_indices = new ArrayList<>();

        for (int i = 0; i < vertices.size(); i++){
            List<List<Float>> generated_vertices = GenerateVertices(height_of_room, vertices.get(i), check_door_each_vertices.get(i));
            AddVertices(generated_vertices);
            List<Integer> indices = getIndices(all_vertices, generated_vertices);
            List<Integer> generated_indices_temp = GenerateIndicesForRender(indices, check_door_each_vertices.get(i));
            generated_indices.addAll(generated_indices_temp);
        }


        vertices_render = new float[all_vertices.size() * 3 + 1];
        colors = new float[all_vertices.size() * 4 + 1];
        indices_render = new byte[generated_indices.size() + 1];

        for (int i = 0; i < all_vertices.size(); i++){
            for (int k = 0; k < 3; k++){
                vertices_render[i * 3 + k] = all_vertices.get(i).get(k);
            }
        }

        for (int i = 0; i < generated_indices.size(); i++){
            indices_render[i] = generated_indices.get(i).byteValue();
        }

        for (int i = 0; i < all_vertices.size(); i++){
            colors[i * 4] = 1.0f;
            colors[i * 4 + 1] = 1.0f;
            colors[i * 4 + 2] = 1.0f;
            colors[i * 4 + 3] = 0.6f + new Random().nextFloat() * 0.4f;
        }

    }

    float distance(List<Float> a, List<Float> b) {
        return (float) Math.sqrt(Math.pow(a.get(0) - b.get(0), 2) + Math.pow(a.get(2) - b.get(2), 2));
    }
    boolean is_between(List<List<Float>> line, List<Float> c){
        return Math.abs(distance(line.get(0),c) + distance(c,line.get(1)) - distance(line.get(0),line.get(1))) < 0.05f;
    }

    void setParameters(float height, ArrayList<List<Float>> room_corner, ArrayList<List<List<Float>>> door_coordinate, ArrayList<List<Integer>> Plane_for_door){
        height_of_room = round(height * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_Y, 3);
        float[] middle_point = new float[]{0, 0, 0};
        for (List<Float> point: room_corner){
            middle_point[0] += point.get(0) / room_corner.size();
            middle_point[1] += point.get(1) / room_corner.size();
            middle_point[2] += point.get(2) / room_corner.size();
        }
        System.out.println("Middle Point:");
        System.out.println(Arrays.toString(middle_point));

        for (int i = 0; i < door_coordinate.size(); i++){
            List<Float> bottom_pos = door_coordinate.get(i).get(0);
            List<Float> top_pos = door_coordinate.get(i).get(1);

            bottom_pos.set(0, round((bottom_pos.get(0) - middle_point[0]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));
            bottom_pos.set(1, 0f);
            bottom_pos.set(2, round((bottom_pos.get(2) - middle_point[2]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));
            top_pos.set(0, round((top_pos.get(0) - middle_point[0]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));
            top_pos.set(1, round((top_pos.get(1) - middle_point[1]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_Y, 3));
            top_pos.set(2, round((top_pos.get(2) - middle_point[2]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));

            door_coordinate.set(i, Arrays.asList(bottom_pos, top_pos));
        }

        System.out.println("Door coordinate:");
        System.out.println(door_coordinate);

        for (int i = 0; i < room_corner.size(); i++){
            boolean have_door = false;

            List<Float> corner_1 = new ArrayList<>(room_corner.get(i));
            List<Float> corner_2 = (i == room_corner.size() - 1) ? new ArrayList<>(room_corner.get(0)) : new ArrayList<>(room_corner.get(i + 1));

            corner_1.set(0, round((corner_1.get(0) - middle_point[0]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));
            corner_1.set(1, 0f);
            corner_1.set(2, round((corner_1.get(2) - middle_point[2]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));
            corner_2.set(0, round((corner_2.get(0) - middle_point[0]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));
            corner_2.set(1, 0f);
            corner_2.set(2, round((corner_2.get(2) - middle_point[2]) * SCALE_TRANSFER_REAL_WORLD_TO_RENDER_XZ, 3));

            for (int j = 0; j < Plane_for_door.size(); j++){
                if (i == Plane_for_door.get(j).get(0)){
                    List<Float> bottom_left_door;
                    List<Float> top_right_door;
                    List<List<Float>> door_pos = door_coordinate.get(j);
                    if (is_between(Arrays.asList(corner_1, door_pos.get(0)), door_pos.get(1))){
                        bottom_left_door = Arrays.asList(door_pos.get(1).get(0), door_pos.get(0).get(1), door_pos.get(1).get(2));
                        top_right_door   = Arrays.asList(door_pos.get(0).get(0), door_pos.get(1).get(1), door_pos.get(0).get(2));
                    } else{
                        bottom_left_door = door_pos.get(0);
                        top_right_door   = door_pos.get(1);
                    }

                    vertices.add(Arrays.asList(corner_1, corner_2, bottom_left_door, top_right_door));
                    check_door_each_vertices.add(true);
                    have_door = true;
                    break;
                }
            }

            if (!have_door) {
                vertices.add(Arrays.asList(corner_1, corner_2));
                check_door_each_vertices.add(false);
            }
        }

        System.out.println(check_door_each_vertices);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_render_sketch_room);


        float height = this.getIntent().getFloatExtra("height", 0);
        ArrayList<List<Float>> Room_corner = (ArrayList<List<Float>>) this.getIntent().getSerializableExtra("floor");
        ArrayList<List<List<Float>>> Door_coordinate = (ArrayList<List<List<Float>>>) this.getIntent().getSerializableExtra("door");
        ArrayList<List<Integer>> Plane_for_door = (ArrayList<List<Integer>>) this.getIntent().getSerializableExtra("plane_for_door");


        System.out.println("Transfer data:");
        System.out.println(height);
        System.out.println(Room_corner);
        System.out.println(Door_coordinate);

        setParameters(height, Room_corner, Door_coordinate, Plane_for_door);

        System.out.println("Input data:");
        System.out.println(height_of_room);
        System.out.println(vertices);

        TestVertices();

        LinearLayout v = findViewById(R.id.linearlayout);
        GLSurfaceView view = new GLSurfaceView(this);

        MyGLRenderer MyRender = new MyGLRenderer();
        MyRender.AddHouseSketch(vertices_render, colors, indices_render);

        view.setRenderer(MyRender);

        view.setOnTouchListener((v1, e) -> {
            float x = e.getX();
            float y = e.getY();

            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                mCubeRotationX += dx * TOUCH_SCALE_FACTOR / 1000;
                mCubeRotationY += dy * TOUCH_SCALE_FACTOR / 2000;

                if (mCubeRotationX > 1.0f) mCubeRotationX = -1.0f;
                if (mCubeRotationY > 1.0f) mCubeRotationY = -1.0f;
                if (mCubeRotationX < -1.0f) mCubeRotationX = 1.0f;
                if (mCubeRotationY < -1.0f) mCubeRotationY = 1.0f;

                MyRender.setRotation(mCubeRotationX * 360, mCubeRotationY * 360);
            }
            mPreviousX = x;
            mPreviousY = y;
            return true;
        });

        v.addView(view);
    }
}