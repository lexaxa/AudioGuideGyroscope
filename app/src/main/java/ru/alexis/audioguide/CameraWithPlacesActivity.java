package ru.alexis.audioguide;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
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
import android.os.Bundle;
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

import ru.alexis.audioguide.model.Place;
import ru.alexis.audioguide.utils.AudioPlayer;
import ru.alexis.audioguide.utils.GeoUtils;

public class CameraWithPlacesActivity extends AppCompatActivity implements SensorEventListener {

    public static final String TAG = CameraWithPlacesActivity.class.getSimpleName();
    public static final int CAMERA_ANGLE = 40;

    SurfaceView surfaceView;
    CoordinateSurface surfaceViewCoords;
    RelativeLayout descView;
    SurfaceHolder mHolder;
    Camera camera;

    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = true;

    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
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
    private ArrayList<Place> points = new ArrayList<>();
    private Point screenSize;
    private float preAzimuth;
    private Display mDisplay;
    private static final double RAD_DEG = 180.0 / Math.PI;
    private static final double DEG_RAD = Math.PI / 180;
    private WindowManager windowManager;
    private LocationManager locationManager;
    Button btnPlay;
    Button btnStop;

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private static final float VALUE_DRIFT = 0.05f;
    private long preTime;
    private long prevTime;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);
        surfaceView = findViewById(R.id.surfaceView);
//        surfaceViewCoords = new CoordinateSurface(this);
        surfaceViewCoords = findViewById(R.id.surfaceViewCoordinate);
        descView = findViewById(R.id.rl_dynamic);
        AudioPlayer.getInstance().setContext(this);
        /*surfaceViewCoords.setOnTouchListener(new SurfaceView.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.performClick();
                float x = event.getX();
                float y = event.getY();
                int offsetX = getResources().getDrawable(R.mipmap.ic_place).getMinimumWidth();
                int offsetY = getResources().getDrawable(R.mipmap.ic_place).getMinimumWidth();
                Log.d(TAG, "onTouch: " + x + ", " + y + " : " + offsetX + ", " + offsetY);
                for (CoordinateSurface.DrawingThread.DrawingItem item : surfaceViewCoords.getDrawingThread().getLocations()) {
                    if(x >= item.x - offsetX/2 && x <= item.x + offsetX/2 &&
                            y >= item.y  && y <= item.y + offsetY){
                        Log.d(TAG, "onTouch: Clicked " + item.desc);
                        AudioPlayer.audioPlayer(item.desc, item.desc);
                    }
                }
                return true;
            }

        });*/
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

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        screenSize = new Point();
        mDisplay = windowManager.getDefaultDisplay();
        mDisplay.getSize(screenSize);
        setupPoints();
        setUpLocation();
        // force setup default location
        if(currentLocation == null) {
            currentLocation = new Location("LaVue location");
            currentLocation.setLongitude(30.341660);
            currentLocation.setLatitude(59.956995);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
//        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 1000000);
//        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 1000000);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_UI);
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
        double alphaSimple;
        double alphaRes;
        double alphaPlace;

        Location resLoc = GeoUtils.getFakeLocation(currentLocation, angle);
//        alphaRes = Math.toDegrees(Math.atan2(resLoc.getLatitude() - currentLocation.getLatitude(), resLoc.getLongitude() - currentLocation.getLongitude()));
        alphaRes = Math.toDegrees(Math.atan2(Math.toRadians(resLoc.getLatitude() - currentLocation.getLatitude()), Math.toRadians(resLoc.getLongitude() - currentLocation.getLongitude())));

        try {
            // TODO Change clearItems on modify attrs exist items
            surfaceViewCoords.getDrawingThread().clearItems();
        }catch(NullPointerException npe){
            Log.d(TAG, "makeUseOfNewSensor: NPE " + npe.getMessage());
        }
        String np = "";
        boolean isnpe = false;
        for (Place p :points) {

            float ab = currentLocation.distanceTo(p.getLocation());
            float ac = currentLocation.distanceTo(resLoc);
            float bc = p.getLocation().distanceTo(resLoc);
            alphaSimple = Math.toDegrees(Math.acos((ab*ab + ac*ac - bc*bc)/(2*ab*ac)));

//            alphaPlace = Math.toDegrees(Math.atan2(p.getLatitude() - currentLocation.getLatitude(), p.getLongitude() - currentLocation.getLongitude()));
            alphaPlace = Math.toDegrees(Math.atan2(Math.toRadians(p.getLatitude() - currentLocation.getLatitude()), Math.toRadians(p.getLongitude() - currentLocation.getLongitude())));

            double alpha = GeoUtils.getNormalAngle(alphaPlace - alphaRes);

            if(i++<4) {
                sb1.
                        append(String.format("%-16.15s", p.getStreet())).append("  ").
                        append(String.format("%.2f", alphaSimple)).append("°  ").
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

        GeoUtils.getAPR(event, mDisplay.getRotation(), mOrientationAngles);
        /*
        // @TODO Check sensor accuracy
        // if(event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) return;
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
        }

        // Compute the three orientation angles based on the most recent readings from
        // the device's accelerometer and magnetometer.
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);

        float[] mRemappedR = changeRotation1();
        // "mRotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(mRemappedR, mOrientationAngles);
*/
        double easing = 0.0005;
//        azimuth += easing * (mOrientationAngles[0] - azimuth);
//        pitch += easing * (mOrientationAngles[1] - pitch);
//        roll += easing * (mOrientationAngles[2] - roll);

        // Pitch and roll values that are close to but not 0 cause the
        // animation to flash a lot. Adjust pitch and roll to 0 for very
        // small values (as defined by VALUE_DRIFT).
        if (Math.abs(pitch) < VALUE_DRIFT) {
            pitch = 0;
        }
        if (Math.abs(roll) < VALUE_DRIFT) {
            roll = 0;
        }

        azimuth = (mOrientationAngles[0]);
        pitch = (mOrientationAngles[1]);
        roll = (mOrientationAngles[2]);

//        https://android-developers.googleblog.com/2010/09/one-screen-turn-deserves-another.html
//        https://stackoverflow.com/questions/4819626/android-phone-orientation-overview-including-compass?rq=1
        //will normalize azimuth and
        angle = azimuth;
        // Only notify other receivers if there is a change in orientation greater than 2.0 degrees
        if(Math.abs(Math.toDegrees(angle - preAngle)) >= 0.05) {
//        if(Math.abs(Math.toDegrees(angle - preAngle)) <= 2 && Math.abs(Math.toDegrees(angle - preAngle)) >= 0.05) {
//            Log.d(TAG, "onSensorChanged: rotation " + mDisplay.getRotation());
//             "mOrientationAngles" now has up-to-date information.
            tv_azimuth.setText( String.format("%.2f", Math.toDegrees(azimuth)));
            tv_pitch.setText( String.format("%.2f", Math.toDegrees(pitch)));
            tv_roll.setText( String.format("%.2f", Math.toDegrees(roll)));
            preAngle = angle;
            Log.d(TAG, "onSensorChanged: angle update to " + Math.toDegrees(angle));

            if(!GeoUtils.isNeedRefresh){
//                return;
            }
            makeUseOfNewSensor();
        }else{
//            preAngle = angle;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    public void setupPoints(){
        //59.956995, 30.341660 La Vue
        points.add(new Place(	59.956540, 30.321217	,"	Test				","	left	"	, "Исаакий.aac"));
        points.add(new Place(	59.945605, 30.342417	,"	Test				","	down	"	, "Здание ФСБ.aac"));
        points.add(new Place(	59.956497, 30.364089	,"	Test				","	right	"	, "Мечеть.aac"));
        points.add(new Place(	59.966525, 30.342503	,"	Test				","	top		"	, "Летний сад.aac"));
        points.add(new Place(	59.944167, 30.306803	,"	Центральный район	","	Биржа ростральные колонны	", "Биржа ростральные колонны.aac"	));
        points.add(new Place(	59.945824, 30.334997	,"	Центральный район	","	Летний сад					", "Летний сад.aac"	));
        points.add(new Place(	59.947342, 30.336155	,"	Центральный район	","	Летний дворец Петра Первого	", "Дворец Петра Первого.aac"	));
        points.add(new Place(	59.942310, 30.316080	,"	Центральный район	","	Большой эрмитаж				", "О Петербурге.aac"	));
        points.add(new Place(	59.942639, 30.277369	,"	Центральный район	","	Васильевский остров			", "Васильевский остров.aac"	));
        points.add(new Place(	59.948652, 30.349252	,"	Центральный район	","	Здание ФСБ					", "Здание ФСБ.aac"	));
        points.add(new Place(	59.940105, 30.314669	,"	Центральный район	","	Зимний дворец				", "Зимний дворец.aac"	));
        points.add(new Place(	59.934198, 30.305786	,"	Центральный район	","	Исаакиевский собор			", "Исаакий.aac"	));
        points.add(new Place(	59.955437, 30.337891	,"	Центральный район	","	Аврора						", "Крейсер Аврора.aac"	));
        points.add(new Place(	59.941577, 30.304666	,"	Центральный район	","	Кунсткамера					", "Кунсткамера.aac"	));
        points.add(new Place(	59.945825, 30.335071	,"	Центральный район	","	Летний сад					", "Летний сад.aac"	));
        points.add(new Place(	59.951829, 30.349310	,"	Центральный район	","	Литейный мост				", "Литейный мост.aac"	));
        points.add(new Place(	59.941315, 30.315638	,"	Центральный район	","	Малый Эрмитаж				", "Малый Эрмитаж.aac"	));
        points.add(new Place(	59.955098, 30.323902	,"	Центральный район	","	Мечеть						", "Мечеть.aac"	));
        points.add(new Place(	59.948779, 30.340559	,"	Центральный район	","	Набережная Кутузова			", "Набережная Кутузова.aac"	));
        points.add(new Place(	59.955521, 30.336317	,"	Центральный район	","	Нахимовское училище			", "Нахимовское училище.aac"	));
        points.add(new Place(	59.951448, 30.315941	,"	Центральный район	","	Петропавловская крепость	", "Петропавловская крепость.aac"	));
        points.add(new Place(	59.952536, 30.341417	,"	Центральный район	","	Река Нева					", "Река Нева.aac"	));
        points.add(new Place(	59.940250, 30.328807	,"	Центральный район	","	Спас на Крови				", "Спас на Крови.aac"	));
        points.add(new Place(	59.948950, 30.327464	,"	Центральный район	","	Троицкий мост				", "Троицкий мост.aac"	));
        points.add(new Place(	59.942686, 30.317877	,"	Центральный район	","	Эрмитажный театр			", "Эрмитажный театр.aac"  ));
/*        points.add(new Place(	59.9032574,30.2767394	,"		","Аврора	"	));
        points.add(new Place(	59.9032574,30.2767394	,"	Адмиралтейский район	","	Старо-Петергофский пр.,д.37	"	));
        points.add(new Place(	59.941736, 30.279738	,"	Василеостровский район	","	7-я линия В.О., д.40	"	    ));
        points.add(new Place(	59.9488286,30.2329235	,"	Василеостровский район	","	ул. Одоевского, д. 33,ТЦ «ДалПортСити», 3 этаж	"	));
        points.add(new Place(	59.984182, 30.344974	,"	Выборгский район    	","	Лесной пр., д.61,к.3	"	    ));
        points.add(new Place(	60.0514711,30.3353396	,"	Выборгский район	    ","	Энгельса пр., д.143, к.1	"	));
        points.add(new Place(	59.999467, 30.359877	,"	Выборгский район    	","	2-ой Муринский пр., д.38, ТЦ \"Клондайк\", 1 этаж"	));
        points.add(new Place(	60.035841, 30.415743 	,"	Калининский район	    ","	пр.Просвещения, д.87, к.1, ТЦ \"Северо-Муринский универмаг\", 2 этаж	"	));
        points.add(new Place(	59.8764876,30.2591313	,"	Кировский район     	","	Стачек пр., д.74/1	"	        ));
        points.add(new Place(	59.8612631,30.2488154	,"	Кировский район     	","	ул. Маршала Казакова, д.1, Универсам \"Пловдив\""	));
        points.add(new Place(	59.841211, 30.239511	,"	Кировский район         ","	пр. Ветеранов, д.51	"	        ));
        points.add(new Place(	59.933812, 30.433745 	,"	Красногвардейский район	","	пр. Уткин, д.13, к.15, ТК \"Польза\", 2 этаж	"	));
        points.add(new Place(	59.7291381,30.0835371	,"	Красное село            ","	пр. Ленина д.85, магазин \"Пятерочка\", 1 этаж	"	));
        points.add(new Place(	59.831987, 30.169263	,"	Красносельский район	","	пр.Ветеранов, д.140. Универсам \"Пловдив\"	"	    ));
        points.add(new Place(	59.997649, 29.765713	,"	Кронштадт	        ","	пр. Ленина 13 \"А\", \"Дом бытовых услуг\", 4 этаж	"	));
        points.add(new Place(	59.853970, 30.357280	,"	Московский район	","	ул. Типанова, д.29	"	            ));
        points.add(new Place(	59.896097, 30.421582	,"	Невский район	    ","	пр. Елизарова, д.15	"	            ));
        points.add(new Place(	59.905175, 30.48295   ,"	Невский район   	","	Большевиков пр., д.21	"	        ));
        points.add(new Place(	59.831571, 30.501811	,"	Невский район	    ","	ул. Тепловозная,  д.31, ТЦ«Порт Находка», 3 этаж	"	));
        points.add(new Place(	59.957735, 30.300956	,"	Петроградский район	","	ул. Большая Пушкарская, д.20	"	));
        points.add(new Place(	60.0017122,30.2988653	,"	Приморский район	","	Коломяжский пр., д.15/2, ТЦ \"Купеческий двор\", 3 этаж"));
        points.add(new Place(	59.986475, 30.297641	,"	Приморский район	","	ул. Савушкина, д.7, ТЦ \"Черная речка\", 1 этаж	"	    ));
        points.add(new Place(	59.990167, 30.257296	,"	Приморский район	","	Торфяная дор., д.7, ТРК \"Гулливер\", Центр Бытовых Услуг, 1 этаж	"	));
        points.add(new Place(	60.006582, 30.257776	,"	Приморский район	","	ул. Гаккелевская, д.34,ТЦ «Лидер», 2 этаж	"       	));
        points.add(new Place(	59.721493, 30.415303	,"	Пушкин          	","	ул. Конюшенная, д.38/37 	"	    ));
        points.add(new Place(	59.8290383,30.3801848	,"	Фрунзенский район	","	ул.Я.Гашека, д.5, ТЦ \"Балатон\", цокольный этаж	"	));
        points.add(new Place(	59.8686261,30.3699191	,"	Фрунзенский район	","	ул. Будапештская, д. 11, ТЦ \"Галерея\", 2 этаж	"	    ));
        points.add(new Place(	59.937001, 30.372742	,"	Центральный район	","	ул. 8-я Советская, д. 15/24	"	    ));
        points.add(new Place(	59.928809, 30.347128	,"	Центральный район	","	Владимирский пр., д.17	"	        ));
        points.add(new Place(	59.930871, 30.360146	,"	Центральный район	","	Лиговский пр., д.41/83	"	        ));
        points.add(new Place(	59.944576, 30.358772	,"	Центральный район	","	пр. Чернышевского, д.19, лит.А	"	));
        points.add(new Place(	54.215262, 37.611096	,"		                ","	ул. Максима Горького, д.27	"	    ));
        points.add(new Place(	58.5400634,31.2627005	,"                       ","	ул. Б.Санкт-Петербургская, 32	"	));
        points.add(new Place(	60.0210936,30.6443304 ,"	Всеволожск  	    ","	ул. Заводская, д. 8, Торговый комплекс \"Гриф\", секция № 13	"	));
        points.add(new Place(	60.050251, 30.445199  	,"	Всеволожский район ЛО","	п. Мурино, Привокзальная пл., д.1А, ТЦ \"Мечта\"	"	));
        points.add(new Place(	59.5635625,30.0921234	,"	Гатчина 	        ","	ул.Соборная, д.10А, 2 этаж	"	    ));
        points.add(new Place(	59.3765359,28.6042716	,"	Кингисепп	        ","	ул. Октябрьская, д.9, ТК \"НОРД\""  ));
        points.add(new Place(	59.4500996,32.0208764	,"	Кириши  	        ","	ул. Советская, дом 10А, ТЦ \"Каприз\", 2 этаж	"	    ));
        points.add(new Place(	58.733311, 29.844692	,"	Луга	            ","	ул. Урицкого, д.77, ТРК \"Айсберг\""));
        points.add(new Place(	59.6432904,33.5297004	,"	Тихвин  	        ","	6-ой микрорайон, дом 17	"	        ));
        points.add(new Place(	61.787008, 34.352008	,"	          	        ","	пр.Ленина, д.30 (цоколь)	"	    ));
        points.add(new Place(	57.802469, 28.357424	,"	    	            ","	ул. Вокзальная, д.26	"   	    ));
*/

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
}

