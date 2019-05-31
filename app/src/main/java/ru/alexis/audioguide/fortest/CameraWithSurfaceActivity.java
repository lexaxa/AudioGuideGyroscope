package ru.alexis.audioguide.fortest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

import ru.alexis.audioguide.CoordinateSurface;
import ru.alexis.audioguide.model.Cube;
import ru.alexis.audioguide.model.Place;
import ru.alexis.audioguide.R;
import ru.alexis.audioguide.utils.AudioPlayer;
import ru.alexis.audioguide.utils.GeoUtils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLSurfaceView;

public class CameraWithSurfaceActivity extends AppCompatActivity implements SensorEventListener {

    public static final String TAG = CameraWithSurfaceActivity.class.getSimpleName();
    public static final int CAMERA_ANGLE = 40;

    SurfaceView surfaceView;
    CoordinateSurface surfaceViewCoords;
    RelativeLayout descView;
    SurfaceHolder mHolder;
    Camera camera;

    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = true;

    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientationAngles = new float[3];
    private float angle;
    private float preAngle;

    private float azimuth;
    private float pitch;
    private float roll;
    private TextView tv_azimuth;
    private TextView tv_pitch;
    private TextView tv_roll;
    private TextView tv_distances2;
    private TextView tv_direction;
    private TextView tv_fakeLon;
    private TextView tv_fakeLat;
    private TextView tv_findLon;
    private TextView tv_findLat;
    private Location currentLocation;
    private SensorManager mSensorManager;

    private Point screenSize;
    private static final double RAD_DEG = 180.0 / Math.PI;
    private static final double DEG_RAD = Math.PI / 180;
    private LocationManager locationManager;
    Button btnPlay;
    Button btnStop;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;
    private long prevTime;

    private GLSurfaceView mGLSurfaceView;
    private MyRenderer mRenderer;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera_surface);
        surfaceView = findViewById(R.id.surfaceView);
        surfaceViewCoords = findViewById(R.id.surfaceViewCoordinate);
        descView = findViewById(R.id.rl_dynamic);
        AudioPlayer.getInstance().setContext(this);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = surfaceView.getHolder();
        mHolder.addCallback(new HolderCallback());
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        tv_azimuth = findViewById(R.id.tv_azimuth);
        tv_pitch = findViewById(R.id.tv_pitch);
        tv_roll = findViewById(R.id.tv_roll);
        tv_direction = findViewById(R.id.tv_direction);
        tv_distances2 = findViewById(R.id.tv_distances2);
        tv_fakeLon = findViewById(R.id.tv_fake_longitude);
        tv_fakeLat = findViewById(R.id.tv_fake_latitude);
        tv_findLon = findViewById(R.id.tv_find_longitude);
        tv_findLat = findViewById(R.id.tv_find_latitude);

        btnPlay = findViewById(R.id.btn_play_audio);
        btnStop = findViewById(R.id.btn_stop_audio);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        screenSize = new Point();
        Display mDisplay = windowManager.getDefaultDisplay();
        mDisplay.getSize(screenSize);
        GeoUtils.setupPoints();
        setUpLocation();
        // force setup default location
        if(currentLocation == null) {
            currentLocation = new Location("LaVue location");
            currentLocation.setLongitude(30.341660);
            currentLocation.setLatitude(59.956995);
        }

        // Get an instance of the SensorManager
        // Create our Preview view and set it as the content of our
        // Activity
        mRenderer = new MyRenderer();
        Log.d(TAG, "onCreate: mRotationVector");
        mGLSurfaceView = findViewById(R.id.gl_surfaceView); //new GLSurfaceView(this);
        mGLSurfaceView.setEGLConfigChooser(8,8,8,8,16,0);
        mGLSurfaceView.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mGLSurfaceView.setRenderer(mRenderer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus

        // @TODO Check camera permission
        camera = Camera.open(CAMERA_ID);
        setPreviewSize(FULL_SCREEN);

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        Log.d(TAG, "onResume: register sensors");
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI,SensorManager.SENSOR_DELAY_UI);

        mRenderer.start();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null)
            camera.release();
        camera = null;
        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);
        AudioPlayer.getInstance().stopPlayer();

        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        mRenderer.stop();
        mGLSurfaceView.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AudioPlayer.getInstance().stopPlayer();
    }

    public void setUpLocation() {

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            @SuppressLint("DefaultLocale")
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                currentLocation = location;
                Log.d(TAG, "onLocationChanged: " + location.getLongitude() +", " + location.getLatitude());
                tv_findLon.setText(String.format("%.6f", currentLocation.getLongitude()));
                tv_findLat.setText(String.format("%.6f", currentLocation.getLatitude()));
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }
            public void onProviderEnabled(String provider) {
            }
            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "setUpLocation: access denied");
            return;
        }else{
            Log.d(TAG, "setUpLocation: access granted");
        }
        // Register the listener with the Location Manager to receive location updates
        if(locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, locationListener);
        }
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void makeUseOfNewSensor() {

        if(currentLocation == null) {
            return;
        }

        StringBuilder sb1 = new StringBuilder();
        int i = 0;

        double alphaRes;
        double alphaPlace;

        Location resLoc = GeoUtils.getFakeLocation(currentLocation, angle);
        alphaRes = Math.toDegrees(Math.atan2(Math.toRadians(resLoc.getLatitude() - currentLocation.getLatitude()), Math.toRadians(resLoc.getLongitude() - currentLocation.getLongitude())));

        try {
            // TODO Change clearItems on modify attrs exist items
            surfaceViewCoords.getDrawingThread().clearItems();
        }catch(NullPointerException npe){
            Log.d(TAG, "makeUseOfNewSensor: NPE " + npe.getMessage());
        }
        String np = "";
        boolean isnpe = false;
        for (Place p :GeoUtils.getPoints()) {

            alphaPlace = Math.toDegrees(Math.atan2(Math.toRadians(p.getLatitude() - currentLocation.getLatitude()), Math.toRadians(p.getLongitude() - currentLocation.getLongitude())));

            double alpha = GeoUtils.getNormalAngle(alphaPlace - alphaRes);

            if(i++<4) {
                sb1.
                        append(String.format("%-16.15s", p.getStreet())).append("  ").
                        append(String.format("%.2f", alpha)).append("°  ").
                        append("\n");
            }
            double posx = screenSize.x /2  - (alpha) * screenSize.x / CAMERA_ANGLE;
            if(Math.abs(posx) < screenSize.x){
                try {
                    surfaceViewCoords.getDrawingThread().addItem((int) posx, 150, p);
                }catch(NullPointerException npe){
                    isnpe = true;
                    np = npe.getMessage();
                }
            }
        }
        if(!isnpe){
            try {
                surfaceViewCoords.getDrawingThread().moveItems();
            }catch(NullPointerException npe){
                Log.d(TAG, "makeUseOfNewSensor: NPE2 " + npe.getMessage());
            }
        }else{
            Log.d(TAG, "makeUseOfNewSensor: NPE " + np);
        }
        tv_fakeLat.setText(String.format("%.6f", resLoc.getLatitude()));
        tv_fakeLon.setText(String.format("%.6f", resLoc.getLongitude()));
        tv_direction.setText( GeoUtils.getDirection(angle) + " " + Math.round(Math.toDegrees(angle)));
        tv_distances2.setText(sb1.toString());
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {


        long now = System.currentTimeMillis();
        long elapsedTime = now - prevTime;
        if (elapsedTime < 250){
            // если прошло меньше 200 миллисекунд - сохраним текущее время
            return;
        }
        prevTime = now;
//        Log.d(TAG, "onSensorChanged: " + event.sensor.getType());
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);

            System.arraycopy(mRotationMatrix,0, mRenderer.mRotationMatrix, 0, mRenderer.mRotationMatrix.length);
            SensorManager.getOrientation( mRotationMatrix, mOrientationAngles );
//            int azimuthTo = (int) ( Math.toDegrees( mOrientationAngles[0] ) + 360 ) % 360;
//            Log.d(TAG, "onSensorChanged: "+azimuthTo);

        }

        azimuth = (mOrientationAngles[0]);
        pitch = (mOrientationAngles[1]);
        roll = (mOrientationAngles[2]);

        angle = azimuth;
        // Only notify other receivers if there is a change in orientation greater than 2.0 degrees
        if(Math.abs(Math.toDegrees(angle - preAngle)) >= 0.05) {
            tv_azimuth.setText( String.format("%.2f", Math.toDegrees(azimuth)));
            tv_pitch.setText( String.format("%.2f", Math.toDegrees(pitch)));
            tv_roll.setText( String.format("%.2f", Math.toDegrees(roll)));
            preAngle = angle;
            //makeUseOfNewSensor();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    public void setUserLocation(View view) {

        currentLocation = new Location("User location");
        //59.956995, 30.341660 La Vue

        EditText userLat = findViewById(R.id.et_user_latitude);
        EditText userLon = findViewById(R.id.et_user_longitude);
        if(userLat.getText().toString().isEmpty() || userLon.getText().toString().isEmpty()) {
            currentLocation.setLatitude(59.956995);
            currentLocation.setLongitude(30.341660);
        }else{
            currentLocation.setLatitude(Double.valueOf(userLat.getText().toString()));
            currentLocation.setLongitude(Double.valueOf(userLon.getText().toString()));
        }
    }
    public void pauseAudio(View v){ AudioPlayer.getInstance().playAudio();}
    public void stopAudio(View v){ AudioPlayer.getInstance().stopPlayer();}
    /* ========================================================== */

    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                camera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            setCameraDisplayOrientation(CAMERA_ID);

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                camera.setPreviewDisplay(mHolder);
                camera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
            if(camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        }
    }

    void setPreviewSize(boolean fullScreen) {

        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Camera.Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // преобразование
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        surfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        surfaceView.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);

        // задняя камера
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        } else
            // передняя камера
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = ((360 - degrees) - info.orientation);
                result += 360;
            }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }


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

    class MyRenderer implements GLSurfaceView.Renderer{
        private Cube mCube;
        private ru.alexis.audioguide.model.Point mPoint;
        private  float[] mRotationMatrix = new float[16];
        private Sensor mRotationVectorSensor;
        // Position the eye behind the origin.
        float eyeX = 0.0f;
        float eyeY = 0.0f;
        float eyeZ = 1.5f;

        // We are looking toward the distance
        float lookX = 0.0f;
        float lookY = 0.0f;
        float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        float upX = 1.0f;
        float upY = 1.0f;
        float upZ = 0.0f;

        public MyRenderer() {
            // find the rotation-vector sensor
            mRotationVectorSensor = mSensorManager.getDefaultSensor(
                    Sensor.TYPE_ROTATION_VECTOR);

            mCube = new Cube();
            mPoint = new ru.alexis.audioguide.model.Point();
            // initialize the rotation matrix to identity
            mRotationMatrix[ 0] = 1;
            mRotationMatrix[ 4] = 1;
            mRotationMatrix[ 8] = 1;
            mRotationMatrix[12] = 1;
        }
        public void start() {
            // enable our sensor when the activity is resumed, ask for
            // 10 ms updates.
//            mSensorManager2.registerListener(this, mRotationVectorSensor, 10000);
        }
        public void stop() {
            // make sure to turn our sensor off when the activity is paused
//            mSensorManager2.unregisterListener(this);
        }
        public void onSensorChanged(SensorEvent event) {
//             we received a sensor event. it is a good practice to check
//             that we received the proper event
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Log.d(TAG, "MyRenderer: " + mRotationVectorSensor.isWakeUpSensor());
            }
            Log.d(TAG, "onSensorChanged: " + event.sensor.getType());
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // convert the rotation-vector to a 4x4 matrix. the matrix
                // is interpreted by Open GL as the inverse of the
                // rotation-vector, which is what we want.
                SensorManager.getRotationMatrixFromVector(
                        mRotationMatrix , event.values);
            }
        }
        public void onDrawFrame(GL10 gl) {
            // clear screen
            Log.d(TAG, "onDrawFrame: " + Arrays.toString(mRotationMatrix));

            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
            // set-up modelview matrix
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0, 0, -3.0f);
            gl.glMultMatrixf(mRotationMatrix, 0);

            // draw our object
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
            mCube.draw(gl);

            gl.glLoadIdentity();
            gl.glTranslatef(-0.5f, 0.5f, -3.0f);
            gl.glMultMatrixf(mRotationMatrix, 0);
            mPoint.draw(gl);

            GLU.gluLookAt(gl,
                    0,
                    0,
                    0,
                    mRotationMatrix[4],
                    mRotationMatrix[5],
                    mRotationMatrix[6],
                    0,
                    0,
                    1
            );
        }
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d(TAG, "onSurfaceChanged: ");

            // set view-port
            gl.glViewport(0, 0, width, height);
            // set projection matrix
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);

        }
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.d(TAG, "onSurfaceCreated: ");
            // dither is enabled by default, we don't need it
            gl.glDisable(GL10.GL_DITHER);
            // clear screen in white
            gl.glClearColor(0,0f,0,0f);
        }
        public int loadShader(int type, String shaderCode){

            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            int shader = GLES20.glCreateShader(type);

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);

            return shader;
        }

        /*class Triangle{


            private final int mProgram;

            Triangle() {


                int vertexShader = mRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                        vertexShaderCode);
                int fragmentShader = mRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                        fragmentShaderCode);

                // create empty OpenGL ES Program
                mProgram = GLES20.glCreateProgram();

                // add the vertex shader to program
                GLES20.glAttachShader(mProgram, vertexShader);

                // add the fragment shader to program
                GLES20.glAttachShader(mProgram, fragmentShader);

                // creates OpenGL ES program executables
                GLES20.glLinkProgram(mProgram);
            }
        }*/

    }

}
