package ru.alexis.audioguide.fortest;

import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Arrays;

import ru.alexis.audioguide.model.Cube;
import ru.alexis.audioguide.model.CubeCutted;
import ru.alexis.audioguide.model.Point;

/**
     * Wrapper activity demonstrating the use of the new
     * {@link SensorEvent#values rotation vector sensor}
     * ({@link Sensor#TYPE_ROTATION_VECTOR TYPE_ROTATION_VECTOR}).
     *
     * @see Sensor
     * @see SensorEvent
     * @see SensorManager
     *
     */
public class SurfaceActivity extends AppCompatActivity {

    public static final String TAG = SurfaceActivity.class.getSimpleName();

        private GLSurfaceView mGLSurfaceView;
        private SensorManager mSensorManager;
        private MyRenderer mRenderer;
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Get an instance of the SensorManager
            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            // Create our Preview view and set it as the content of our
            // Activity
            mRenderer = new MyRenderer();
            mGLSurfaceView = new GLSurfaceView(this);
            mGLSurfaceView.setRenderer(mRenderer);
            setContentView(mGLSurfaceView);
        }
        @Override
        protected void onResume() {
            // Ideally a game should implement onResume() and onPause()
            // to take appropriate action when the activity looses focus
            super.onResume();
            mRenderer.start();
            mGLSurfaceView.onResume();
        }
        @Override
        protected void onPause() {
            // Ideally a game should implement onResume() and onPause()
            // to take appropriate action when the activity looses focus
            super.onPause();
            mRenderer.stop();
            mGLSurfaceView.onPause();
        }
        class MyRenderer implements GLSurfaceView.Renderer, SensorEventListener {
            private Point mPoint;
            private Cube mCube;
            private ArrayList<Point> points;
            private CubeCutted mCubeCutted;
            private Sensor mRotationVectorSensor;
            private final float[] mRotationMatrix = new float[16];
            private float[] mPrevRotationMatrix = new float[16];
            private float[] angleChange = new float[3];
            float prevAngle;
            private long prevTime;

            final float cubePositions[][] = {
                    {0.0f, 0.0f, 0.0f},
                    {2.0f, 5.0f, -15.0f},
                    {-1.5f, -2.2f, -2.5f},
                    {-3.8f, -2.0f, -12.3f},
                    {2.4f, -0.4f, -3.5f},
                    {-1.7f, 3.0f, -7.5f},
                    {1.3f, -2.0f, -2.5f},
                    {1.5f, 2.0f, -2.5f},
                    {1.5f, 0.2f, -1.5f},
                    {-1.3f, 1.0f, -1.5f}
            };
            private float[] mViewMatrix = new float[16];
            private final float[] mMVPMatrix = new float[16];
            private final float[] mProjectionMatrix = new float[16];

            // Position the eye behind the origin.
            float eyeX = 0.0f;
            float eyeY = 0.0f;
            float eyeZ = 1.5f;

            // We are looking toward the distance
            float lookX = 0.0f;
            float lookY = 0.0f;
            float lookZ = -5.0f;

            // Set our up vector. This is where our head would be pointing were we holding the camera.
            float upX = 0.0f;
            float upY = 1.0f;
            float upZ = 0.0f;
            private final float[] mOrientation = new float[3];

            MyRenderer() {
                // find the rotation-vector sensor
                mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                mCube = new Cube();
                mCubeCutted = new CubeCutted();
                mPoint = new Point();
                points = new ArrayList();
                points.add(new Point());
                points.add(new Point());
                points.add(new Point());
                // initialize the rotation matrix to identity
                mRotationMatrix[0] = 1;
                mRotationMatrix[4] = 1;
                mRotationMatrix[8] = 1;
                mRotationMatrix[12] = 1;
                // Render the view only when there is a change in the drawing data.
                // To allow the triangle to rotate automatically, this line is commented out:
                //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            }

            public void start() {
                // enable our sensor when the activity is resumed, ask for
                // 10 ms updates.
                mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
            }

            public void stop() {
                // make sure to turn our sensor off when the activity is paused
                mSensorManager.unregisterListener(this);
            }

            public void onSensorChanged(SensorEvent event) {
                // we received a sensor event. it is a good practice to check
                // that we received the proper event
                if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    // convert the rotation-vector to a 4x4 matrix. the matrix
                    // is interpreted by Open GL as the inverse of the
                    // rotation-vector, which is what we want.
                    SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

                    // create a camera matrix (YXZ order is pretty standard)
                    // you may want to negate some of these constant 1s to match expectations.
//                    Matrix.setRotateM(mViewMatrix, 0, event.values[1], 0, 1, 0);
//                    Matrix.rotateM(mViewMatrix, 0, event.values[0], 1, 0, 0);
//                    Matrix.rotateM(mViewMatrix, 0, event.values[2], 0, 0, 1);
//                    Matrix.translateM(mViewMatrix, 0, -eyeX, -eyeY, -eyeZ);
                }
            }
            public void addModel(Point point) {
                if(!points.contains(point)) {
                    points.add(point);
                }
            }
            public void onDrawFrame(GL10 gl) {
                // clear screen
                SensorManager.getAngleChange(angleChange, mRotationMatrix, mPrevRotationMatrix);
//                Log.d("TAG", "onDrawFrame: " + Arrays.toString(mRotationMatrix));

//                GLU.gluLookAt(gl,
//                        eyeX, eyeY, eyeZ,
//                        lookX, lookY, lookZ,
//                        upX, upY, upZ);
//                // * Create a rotation transformation for the triangle
                long time = SystemClock.uptimeMillis() % 5000L;
                float angleRot = 0.0090f * ((int) time);
//                Log.d("TAG", "onDrawFrame: " + angleRot);
                gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
                int azimuthTo = (int) ( Math.toDegrees( SensorManager.getOrientation( mRotationMatrix, mOrientation )[0] ) + 360 ) % 360;
                Log.d(TAG, "onDrawFrame: " + Arrays.toString(mOrientation));
                lookX = mRotationMatrix[1];
                lookY = mRotationMatrix[2];
                lookZ = -5.0f;

//                upX = mRotationMatrix[1];
//                upY = mRotationMatrix[2];
//                upZ = mRotationMatrix[0];



                GLU.gluLookAt(gl, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);


//                for (Iterator<Model3D> iterator = models.iterator(); iterator.hasNext();)
//                {
//                    Model3D model = iterator.next();
//                    model.draw(gl);
//                }


                Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, mOrientation[1], mOrientation[2], mOrientation[0], upX, upY, upZ);
                //setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);


                // set-up modelView matrix
                gl.glMatrixMode(GL10.GL_MODELVIEW);
                gl.glLoadIdentity();
                gl.glTranslatef(0, 0, -3.0f);
                gl.glMultMatrixf(mRotationMatrix, 0);
                // draw our object
                gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
                gl.glEnableClientState(GL10.GL_COLOR_ARRAY);


//                // * Create a rotation transformation for the triangle
//                long time = SystemClock.uptimeMillis() % 4000L;
//                float angleRot = 0.090f * ((int) time);
//                // * Create a rotation transformation for the triangle
//                Matrix.setRotateM(mRotationMatrix, 0, angleRot, angleRot, 0, -1.0f);
//                // * Combine the rotation matrix with the projection and camera view
//                // * Note that the mMVPMatrix factor *must be first* in order
//                // * for the matrix multiplication product to be correct.
//                float[] scratch = new float[16];
//                float[] mMVPMatrix = new float[16];
//                Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);

                mCube.draw(gl);

                gl.glLoadIdentity();
                gl.glTranslatef(-0.5f, -0.5f, -3.0f);
//                gl.glMultMatrixf(mRotationMatrix, 0);
//                GLU.gluLookAt(gl,0,0,0,0,0,0,0,0,0);


                mPoint.draw(gl);

                gl.glLoadIdentity();
                gl.glTranslatef(0.5f, 0.5f, -3.0f);
                // Set the camera position (View matrix)
                //Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
//
//                // Calculate the projection and view transformation
                Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

                //gl.glMultMatrixf(mRotationMatrix, 0);
                mCubeCutted.draw(gl);

                GLU.gluPerspective(gl,45,1,1,50);

                mPrevRotationMatrix = mRotationMatrix;
            }

            public void onSurfaceChanged(GL10 gl, int width, int height) {
                Log.d("TAG", "onSurfaceChanged: ");
                float ratio = (float) width / height;
                // Set the OpenGL viewport to the same size as the surface.
                gl.glViewport(0, 0, width, height);
                // set projection matrix
                gl.glMatrixMode(GL10.GL_PROJECTION);
                gl.glLoadIdentity();
                gl.glFrustumf(-ratio, ratio, -1, 1, 0.5f, 30);

                gl.glMatrixMode(GL10.GL_MODELVIEW);
                gl.glLoadIdentity();

            }

            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                Log.d("TAG", "onSurfaceCreated: ");
                // clear screen in white
//                gl.glClearColor(1, 1, 1, 1);


                // Set the background clear color to gray.
                gl.glClearColor(0.5f, 0.5f, 0.7f, 0.5f);

                // dither is enabled by default, we don't need it
                gl.glDisable(GL10.GL_DITHER);

                // Set the view matrix. This matrix can be said to represent the camera position.
                // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
                // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
//                Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }

        }
}
