package com.arruler.ViewRender3D;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class MyGLRenderer implements GLSurfaceView.Renderer {

    private RoomRender mHouse;
    private float mCubeRotationX = 1.0f;
    private float mCubeRotationY = 1.0f;

    public void AddHouseSketch(float[] vertices, float[] colors, byte[] indices){
        mHouse = new RoomRender(vertices, colors, indices);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);

        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        gl.glTranslatef(0.0f, -8.0f, -50.0f);
        gl.glRotatef(mCubeRotationX, 0.0f,1.0f,0.0f );
        gl.glRotatef(mCubeRotationY, 1.0f, 0.0f,0.0f );

        mHouse.draw(gl);

        gl.glLoadIdentity();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void setRotation(float mX, float mY){
        mCubeRotationX = mX;
        mCubeRotationY = mY;
    }
}