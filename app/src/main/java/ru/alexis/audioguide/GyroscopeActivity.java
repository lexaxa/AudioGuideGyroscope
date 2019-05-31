package ru.alexis.audioguide;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

import ru.alexis.audioguide.model.Place;
import ru.alexis.audioguide.utils.GeoUtils;

public class GyroscopeActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = GyroscopeActivity.class.getSimpleName();
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
    private TextView tv_fakeLon;
    private TextView tv_fakeLat;
    private TextView tv_findLon;
    private TextView tv_findLat;
    private Location currentLocation;
    private SensorManager mSensorManager;
    private ArrayList<Place> points = new ArrayList<>();
    private WindowManager windowManager;
    private Display mDisplay;
    private static final double RAD_DEG = 180.0 / Math.PI;
    private static final double DEG_RAD = Math.PI / 180;
    private LocationManager locationManager;
    private static final float VALUE_DRIFT = 0.05f;
    private long prevTime;
//    final Stack<Runnable> mStack = new Stack<Runnable>();
    public static final int CAMERA_ANGLE = 40;
    private Point screenSize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gyroskop);

        tv_azimuth = findViewById(R.id.tv_azimuth);
        tv_pitch = findViewById(R.id.tv_pitch);
        tv_roll = findViewById(R.id.tv_roll);
        tv_distances2 = findViewById(R.id.tv_distances2);
        tv_fakeLon = findViewById(R.id.tv_fake_longitude);
        tv_fakeLat = findViewById(R.id.tv_fake_latitude);
        tv_findLon = findViewById(R.id.tv_find_longitude);
        tv_findLat = findViewById(R.id.tv_find_latitude);

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mDisplay = windowManager.getDefaultDisplay();
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        setupPoints();
        setUpLocation();
        // force setup default location
        if(currentLocation == null) {
            currentLocation = new Location("LaVue location");
            currentLocation.setLongitude(30.341660);
            currentLocation.setLatitude(59.956995);
        }

        PackageManager manager = getPackageManager();
        boolean hasCompass = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        if (!hasCompass)
        {
            Toast.makeText(this, "WARNING: DEVICE HAS NO COMPASS", Toast.LENGTH_LONG).show();
            Log.d(TAG, "onCreate: WARNING: DEVICE HAS NO COMPASS");
        }

        screenSize = new Point();
        mDisplay = windowManager.getDefaultDisplay();
        mDisplay.getSize(screenSize);

        Log.d(TAG, "onCreate: screenSize " + screenSize.x + ":" + screenSize.y);

    }

    public void setUpLocation() {

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if(currentLocation != null &&
                        location.getLatitude() != currentLocation.getLatitude() &&
                        location.getLongitude() != currentLocation.getLongitude()){
                    Log.d(TAG, "onLocationChanged: " + location.getLongitude() + ", " + location.getLatitude());
                }
                currentLocation = location;
                tv_findLon.setText(String.format(Locale.ROOT, "%.6f", currentLocation.getLatitude()));
                tv_findLat.setText(String.format(Locale.ROOT, "%.6f", currentLocation.getLongitude()));
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
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.d(TAG, "setUpLocation: access denied");
            return;
        }else{
            Log.d(TAG, "setUpLocation: access granted");
        }
        // Register the listener with the Location Manager to receive location updates
        if(locationManager != null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }

    private void makeUseOfNewSensor() {

        if(currentLocation == null) {
            return;
        }
        StringBuilder sb1 = new StringBuilder();
        int i = 0;
        double alphaPlace;
        Location fakeLocation = GeoUtils.getFakeLocation(currentLocation, angle);
        double alphaFakeLoc = GeoUtils.getAngleBetween2Loc(currentLocation, fakeLocation);

        for (Place p :points) {
            alphaPlace = GeoUtils.getAngleBetween2Loc(currentLocation, p.getLocation());

            double angleToCenter = GeoUtils.getNormalAngle(alphaPlace - alphaFakeLoc);
//            double posX = screenSize.x /2  - (angleToCenter) * screenSize.x / CAMERA_ANGLE;
            sb1.
                    append(String.format(Locale.ROOT,"%-16.15s", p.getStreet())).append(" ").
                    append(String.format(Locale.ROOT,"%.2f", angleToCenter)).append("° ").
                    append("\n");

            if(i++>10) break;
        }
        sb1.
                append("place          ").
                append("p-res ").
                append("\n");
        tv_distances2.setText(sb1.toString());
        tv_fakeLat.setText(String.format(Locale.ROOT, "%.6f", fakeLocation.getLatitude()));
        tv_fakeLon.setText(String.format(Locale.ROOT, "%.6f", fakeLocation.getLongitude()));
    }

    // Get readings from accelerometer and magnetometer. To simplify calculations,
    // consider storing these readings as unit vectors.
    @Override
    public void onSensorChanged(SensorEvent event) {

        long now = System.currentTimeMillis();
        long elapsedTime = now - prevTime;
        if (elapsedTime < 250){
            // если прошло меньше ___ миллисекунд - ждем
            return;
        }
        prevTime = now;

        GeoUtils.getAPR(event, mDisplay.getRotation(), mOrientationAngles);

        float easing = 0.05f;
//        xx += easing * (mOrientationAngles[0] - xx);
//        azimuth = easing * mOrientationAngles[0] + (1 - easing) * azimuth;
//        pitch =  easing * mOrientationAngles[1] + (1 - easing) * pitch;
//        roll =  easing * mOrientationAngles[2] + (1 - easing) * roll;
        azimuth = (mOrientationAngles[0]);
        pitch = (mOrientationAngles[1]);
        roll = (mOrientationAngles[2]);

        // Pitch and roll values that are close to but not 0 cause the
        // animation to flash a lot. Adjust pitch and roll to 0 for very
        // small values (as defined by VALUE_DRIFT).
            if (Math.abs(pitch) < VALUE_DRIFT) {
                pitch = 0;
            }
            if (Math.abs(roll) < VALUE_DRIFT) {
                roll = 0;
            }

//        https://android-developers.googleblog.com/2010/09/one-screen-turn-deserves-another.html
//        https://stackoverflow.com/questions/4819626/android-phone-orientation-overview-including-compass?rq=1
        //will normalize azimuth and
        //mOrientationAngles[0] = mOrientationAngles[0] >= 0 ? mOrientationAngles[0]: mOrientationAngles[0] + 360;
        angle = azimuth;
        // Only notify other receivers if there is a change in orientation greater than 2.0 degrees
        if(Math.abs(Math.toDegrees(angle - preAngle)) >= 0.05) {
//            pitch += easing * (mOrientationAngles[1] - pitch);
//            roll += easing * (mOrientationAngles[2] - roll);
//        if(Math.abs(Math.toDegrees(angle - preAngle)) <= 2 && Math.abs(Math.toDegrees(angle - preAngle)) >= 0.05) {
//            Log.d(TAG, "onSensorChanged: rotation " + mDisplay.getRotation());
//             "mOrientationAngles" now has up-to-date information.
            tv_azimuth.setText( String.format(Locale.ROOT,"%.2f", Math.toDegrees(azimuth)));
            tv_pitch.setText( String.format(Locale.ROOT,"%.2f", Math.toDegrees(pitch)));
            tv_roll.setText( String.format(Locale.ROOT,"%.2f", Math.toDegrees(roll)));
            Log.d(TAG, "onSensorChanged: angle update to " + Math.toDegrees(angle));
            preAngle = angle;
//            if(angle != preAngle) {
//            }
            if(!GeoUtils.isNeedRefresh){
               // return;
            }

            makeUseOfNewSensor();
        }else{
//            if((int)(angle*180/Math.PI) != (int)(preAngle*180/Math.PI)) {
//                Log.d(TAG, "onSensorChanged: angle wait is ------ " + (int) (angle * 180 / Math.PI) + " - " + (int) (preAngle * 180 / Math.PI));
//                preAngle = angle;
//            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
        // You must implement this callback in your code.
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Get updates from the accelerometer and magnetometer at a constant rate.
        // To make batch operations more efficient and reduce power consumption,
        // provide support for delaying updates to the application.
        //
        // In this example, the sensor reporting delay is small enough such that
        // the application receives an update before the system checks the sensor
        // readings again.
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_UI, SensorManager.SENSOR_DELAY_UI);
//        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_NORMAL);
//        startSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't receive any more updates from either sensor.
        mSensorManager.unregisterListener(this);
    }

    public void setupPoints(){
        //59.956995, 30.341660 La Vue
        points.add(new Place(	59.956540, 30.321217	,"	Test				","	left_____________________	"	));
        points.add(new Place(	59.945605, 30.342417	,"	Test				","	down_____________________	"	));
        points.add(new Place(	59.956497, 30.364089	,"	Test				","	right____________________	"	));
        points.add(new Place(	59.966525, 30.342503	,"	Test				","	top______________________	"	));
        points.add(new Place(	59.955437, 30.337891	,"	Центральный район	","	Avrora___________________   "	));
        points.add(new Place(	59.944167, 30.306803	,"	Центральный район	","	Birzha Rostralnie kolonni	"	));
        points.add(new Place(	59.945824, 30.334997	,"	Центральный район	","	Letniy sad_______________	"	));
        points.add(new Place(	59.947342, 30.336155	,"	Центральный район	","	Letniy dvorez Petra Pervogo	"	));
        points.add(new Place(	59.942310, 30.316080	,"	Центральный район	","	Bolshoy Ermitage_________	"	));
        points.add(new Place(	59.942639, 30.277369	,"	Центральный район	","	Vasilevskiy ostrov			"	));
        points.add(new Place(	59.948652, 30.349252	,"	Центральный район	","	Zdanie FSB______			"	));
        points.add(new Place(	59.940105, 30.314669	,"	Центральный район	","	Zimniy Dvorez___			"	));
        points.add(new Place(	59.934198, 30.305786	,"	Центральный район	","	Исаакиевский собор			"	));
        points.add(new Place(	59.941577, 30.304666	,"	Центральный район	","	Кунсткамера					"	));
        points.add(new Place(	59.945825, 30.335071	,"	Центральный район	","	Летний сад					"	));
        points.add(new Place(	59.951829, 30.349310	,"	Центральный район	","	Литейный мост				"	));
        points.add(new Place(	59.941315, 30.315638	,"	Центральный район	","	Малый Эрмитаж				"	));
        points.add(new Place(	59.955098, 30.323902	,"	Центральный район	","	Мечеть						"	));
        points.add(new Place(	59.948779, 30.340559	,"	Центральный район	","	Набережная Кутузова			"	));
        points.add(new Place(	59.955521, 30.336317	,"	Центральный район	","	Нахимовское училище			"	));
        points.add(new Place(	59.951448, 30.315941	,"	Центральный район	","	Петропавловская крепость	"	));
        points.add(new Place(	59.952536, 30.341417	,"	Центральный район	","	Река Нева					"	));
        points.add(new Place(	59.940250, 30.328807	,"	Центральный район	","	Спас на Крови				"	));
        points.add(new Place(	59.948950, 30.327464	,"	Центральный район	","	Троицкий мост				"	));
        points.add(new Place(	59.942686, 30.317877	,"	Центральный район	","	Эрмитажный театр			"	));
//        points.add(new Place(	59.9032574,30.2767394	,"	                    	"," Аврора	"	));
//        points.add(new Place(	59.9032574,30.2767394	,"	Адмиралтейский район	","	Старо-Петергофский пр.,д.37	"	));
//        points.add(new Place(	59.941736, 30.279738	,"	Василеостровский район	","	7-я линия В.О., д.40	"	    ));
//        points.add(new Place(	59.9488286,30.2329235	,"	Василеостровский район	","	ул. Одоевского, д. 33,ТЦ «ДалПортСити», 3 этаж	"	));
//        points.add(new Place(	59.984182, 30.344974	,"	Выборгский район    	","	Лесной пр., д.61,к.3	"	    ));
//        points.add(new Place(	60.0514711,30.3353396	,"	Выборгский район	    ","	Энгельса пр., д.143, к.1	"	));
//        points.add(new Place(	59.999467, 30.359877	,"	Выборгский район    	","	2-ой Муринский пр., д.38, ТЦ \"Клондайк\", 1 этаж"	));
//        points.add(new Place(	60.035841, 30.415743 	,"	Калининский район	    ","	пр.Просвещения, д.87, к.1, ТЦ \"Северо-Муринский универмаг\", 2 этаж	"	));
//        points.add(new Place(	59.8764876,30.2591313	,"	Кировский район     	","	Стачек пр., д.74/1	"	        ));
//        points.add(new Place(	59.8612631,30.2488154	,"	Кировский район     	","	ул. Маршала Казакова, д.1, Универсам \"Пловдив\""	));
//        points.add(new Place(	59.841211, 30.239511	,"	Кировский район         ","	пр. Ветеранов, д.51	"	        ));
//        points.add(new Place(	59.933812, 30.433745 	,"	Красногвардейский район	","	пр. Уткин, д.13, к.15, ТК \"Польза\", 2 этаж	"	));
//        points.add(new Place(	59.7291381,30.0835371	,"	Красное село            ","	пр. Ленина д.85, магазин \"Пятерочка\", 1 этаж	"	));
//        points.add(new Place(	59.831987, 30.169263	,"	Красносельский район	","	пр.Ветеранов, д.140. Универсам \"Пловдив\"	"	    ));
//        points.add(new Place(	59.997649, 29.765713	,"	Кронштадт	            ","	пр. Ленина 13 \"А\", \"Дом бытовых услуг\", 4 этаж	"	));
//        points.add(new Place(	59.853970, 30.357280	,"	Московский район    	","	ул. Типанова, д.29	"	            ));
//        points.add(new Place(	59.896097, 30.421582	,"	Невский район	        ","	пр. Елизарова, д.15	"	            ));
//        points.add(new Place(	59.905175, 30.48295     ,"	Невский район       	","	Большевиков пр., д.21	"	        ));
//        points.add(new Place(	59.831571, 30.501811	,"	Невский район	        ","	ул. Тепловозная,  д.31, ТЦ«Порт Находка», 3 этаж	"	));
//        points.add(new Place(	59.957735, 30.300956	,"	Петроградский район 	","	ул. Большая Пушкарская, д.20	"	));
//        points.add(new Place(	60.0017122,30.2988653	,"	Приморский район    	","	Коломяжский пр., д.15/2, ТЦ \"Купеческий двор\", 3 этаж"));
//        points.add(new Place(	59.986475, 30.297641	,"	Приморский район	    ","	ул. Савушкина, д.7, ТЦ \"Черная речка\", 1 этаж	"	    ));
//        points.add(new Place(	59.990167, 30.257296	,"	Приморский район    	","	Торфяная дор., д.7, ТРК \"Гулливер\", Центр Бытовых Услуг, 1 этаж	"	));
//        points.add(new Place(	60.006582, 30.257776	,"	Приморский район    	","	ул. Гаккелевская, д.34,ТЦ «Лидер», 2 этаж	"       	));
//        points.add(new Place(	59.721493, 30.415303	,"	Пушкин              	","	ул. Конюшенная, д.38/37 	"	    ));
//        points.add(new Place(	59.8290383,30.3801848	,"	Фрунзенский район   	","	ул.Я.Гашека, д.5, ТЦ \"Балатон\", цокольный этаж	"	));
//        points.add(new Place(	59.8686261,30.3699191	,"	Фрунзенский район   	","	ул. Будапештская, д. 11, ТЦ \"Галерея\", 2 этаж	"	    ));
//        points.add(new Place(	59.937001, 30.372742	,"	Центральный район   	","	ул. 8-я Советская, д. 15/24	"	    ));
//        points.add(new Place(	59.928809, 30.347128	,"	Центральный район   	","	Владимирский пр., д.17	"	        ));
//        points.add(new Place(	59.930871, 30.360146	,"	Центральный район   	","	Лиговский пр., д.41/83	"	        ));
//        points.add(new Place(	59.944576, 30.358772	,"	Центральный район     	","	пр. Чернышевского, д.19, лит.А	"	));
//        points.add(new Place(	54.215262, 37.611096	,"		                    ","	ул. Максима Горького, д.27	"	    ));
//        points.add(new Place(	58.5400634,31.2627005	,"                          ","	ул. Б.Санкт-Петербургская, 32	"	));
//        points.add(new Place(	60.0210936,30.6443304   ,"	Всеволожск  	        ","	ул. Заводская, д. 8, Торговый комплекс \"Гриф\", секция № 13	"	));
//        points.add(new Place(	60.050251, 30.445199  	,"	Всеволожский район ЛО   ","	п. Мурино, Привокзальная пл., д.1А, ТЦ \"Мечта\"	"	));
//        points.add(new Place(	59.5635625,30.0921234	,"	Гатчина 	            ","	ул.Соборная, д.10А, 2 этаж	"	    ));
//        points.add(new Place(	59.3765359,28.6042716	,"	Кингисепп	            ","	ул. Октябрьская, д.9, ТК \"НОРД\""  ));
//        points.add(new Place(	59.4500996,32.0208764	,"	Кириши  	            ","	ул. Советская, дом 10А, ТЦ \"Каприз\", 2 этаж	"	    ));
//        points.add(new Place(	58.733311, 29.844692	,"	Луга	                ","	ул. Урицкого, д.77, ТРК \"Айсберг\""));
//        points.add(new Place(	59.6432904,33.5297004	,"	Тихвин  	            ","	6-ой микрорайон, дом 17	"	        ));
//        points.add(new Place(	61.787008, 34.352008	,"	          	            ","	пр.Ленина, д.30 (цоколь)	"	    ));
//        points.add(new Place(	57.802469, 28.357424	,"	    	                ","	ул. Вокзальная, д.26	"   	    ));
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

//
//    private void startSensors1() {

    //        if(mAccelerometerReading != null && mMagnetometerReading != null){
//            Log.d(TAG, "onSensorChanged: add to stack");
//            mStack.push(new Calculater(new float[][] { mAccelerometerReading, mMagnetometerReading }));
//        }


//        final Stack<Runnable> mStack = new Stack<Runnable>();
//
//        new Thread() {
//            public void run() {
//                while(true)
//                {
//                    try {
//                        if(mStack.size()!=0) {
//                            Runnable r = mStack.pop();
//                            r.run();
//                        }
//                    } catch(Exception ex){}
//                }
//            }
//        }.start();
//    }
//    private class Calculater implements Runnable {
//        float[][] theValues;
//        public Calculater(float[][] values) {
//            theValues = values;
//        }
//        public void run() {
////            int[] degrees = SensorManager.getOrientation(theValues[0], theValues[1]);
////            Log.e("",String.valueOf(degrees[0]));
//            SensorManager.getRotationMatrix(mRotationMatrix, null, theValues[0], theValues[1]);
//
//            float[] mRemappedR = new float[9];
//            mRemappedR = mRotationMatrix.clone();
////            mRemappedR = changeRotation1();
////        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRemappedR);
//            // "mRotationMatrix" now has up-to-date information.
//            SensorManager.getOrientation(mRemappedR, mOrientationAngles);
//
//            Log.d(TAG, "run: " + mOrientationAngles[0] + "="+ mOrientationAngles[1] + "+" + mOrientationAngles[2]);
//
//        }
//    }
//    private float[] rotationVectorAction(float[] values) {
//        float[] result = new float[3];
//        float vec[] = values;
//        float quat[] = new float[4];
//        float[] orientation = new float[3];
//        SensorManager.getQuaternionFromVector(quat, vec);
//        float[] rotMat = new float[9];
//        SensorManager.getRotationMatrixFromVector(rotMat, quat);
//        SensorManager.getOrientation(rotMat, orientation);
//        result[0] = (float) orientation[0];
//        result[1] = (float) orientation[1];
//        result[2] = (float) orientation[2];
//        return result;
//    }
//    private void main (float values[]) {
//        double max = Math.PI / 2 - 0.01;
//        double min = -max;
//
//        float[] result = rotationVectorAction(values);
//        float azimuthm = result[1];
//        float pitchm = result[2];
//        pitchm = (float) Math.max(min, pitchm);
//        pitchm = (float) Math.min(max, pitchm);
//        float dx = (float) (Math.sin(azimuthm) * (-Math.cos(pitchm)));
//        float dy = (float) Math.sin(pitchm);
//        float dz = (float) (Math.cos(azimuthm) * Math.cos(pitchm));
//        Log.d(TAG, "main: " + azimuthm + " : " + pitchm + " : ");
//    }
//    public float onOrientationChanged(float azimuth, float pitch, float roll){
//        float angle;
//        azimuth = (float) Math.toDegrees(azimuth);
//        pitch = (float) Math.toDegrees(pitch);
//        roll = (float) Math.toDegrees(roll);
//        // fix pitch and roll
//        if (Math.abs(pitch) + Math.abs(roll) != 0){
//            float x = 90 / (Math.abs(pitch) + Math.abs(roll));
//            pitch *= x;
//            roll *= x;
//        }
//
//        if (pitch < 0){
//            angle = roll;
//        }else{
//            if (roll < 0) angle = -90 - (roll + 90);
//            else angle = 90 + (90 - roll);
//        }
//
//        angle = angle - 90;
//        if (angle < -180) angle = 360 + angle;
//        return angle;
//    }
//    public float[] changeRotation1() {
//
//        float[] mRemappedR = new float[9];
//        int axisX = SensorManager.AXIS_X;
//        int axisY = SensorManager.AXIS_Z;
////   0 - 0 Y-up
//// 270 - 1 Y-left
//// 180 - 2 Y-down
////  90 - 3 Y-right
//        switch (mDisplay.getRotation()) {
//            case Surface.ROTATION_0:
//                axisX = SensorManager.AXIS_X;
//                axisY = SensorManager.AXIS_Z;
//                break;
//            case Surface.ROTATION_270: // old 90
//                axisX = SensorManager.AXIS_MINUS_Z;
//                axisY = SensorManager.AXIS_X;
//                break;
//            case Surface.ROTATION_180:
//                axisX = SensorManager.AXIS_MINUS_X;
//                axisY = SensorManager.AXIS_MINUS_Z;
//                break;
//            case Surface.ROTATION_90: // old 270
//                axisX = SensorManager.AXIS_Z;
//                axisY = SensorManager.AXIS_MINUS_X;
//                break;
//            default:
//                break;
//        }
//        SensorManager.remapCoordinateSystem(mRotationMatrix, axisX, axisY, mRemappedR);
//        return mRemappedR;
//    }
//    public float[] changeRotation2(){
//        float[] mRemappedR = new float[9];
//        int axisX = SensorManager.AXIS_X;
//        int axisY = SensorManager.AXIS_Y;
//
//        switch (mDisplay.getRotation()) {
//            case Surface.ROTATION_0:
//                axisX = SensorManager.AXIS_X;
//                axisY = SensorManager.AXIS_Y;
//                break;
//            case Surface.ROTATION_90:
//                axisX = SensorManager.AXIS_Y;
//                axisY = SensorManager.AXIS_MINUS_X;
//                break;
//            case Surface.ROTATION_180:
//                axisX = SensorManager.AXIS_MINUS_X;
//                axisY = SensorManager.AXIS_MINUS_Y;
//                break;
//            case Surface.ROTATION_270:
//                axisX = SensorManager.AXIS_MINUS_Y;
//                axisY = SensorManager.AXIS_X;
//                break;
//            default:
//                break;
//        }
//        SensorManager.remapCoordinateSystem(mRotationMatrix, axisX, axisY, mRemappedR);
//        return mRemappedR;
//    }
//    public void changeRotation3(){
//
////        mRemappedR = mRotationMatrix.clone();
////        mRemappedR = changeRotation2();
////        SensorManager.getOrientation(mRemappedR, mOrientationAngles);
//
////        switch(mDisplay.getRotation()) {
////            case Surface.ROTATION_0:  // normal rotation
////                break;
////            case Surface.ROTATION_90:  // phone is turned 90 degrees counter-clockwise
//////                mOrientationAngles[2] += 90 * Math.PI / 180;
////                break;
////            case Surface.ROTATION_180: // phone is rotated 180 degrees
//////                mOrientationAngles[2] += 180 * Math.PI / 180;
////                break;
////            case Surface.ROTATION_270:  // phone is turned 90 degrees clockwise
//////                mOrientationAngles[2] += 270 * Math.PI / 180;
////                break;
////        }
//
//        switch(mDisplay.getRotation()) {
//            case Surface.ROTATION_0:  // normal rotation
//                break;
//            case Surface.ROTATION_90:  // phone is turned 90 degrees counter-clockwise
//                float temp = -pitch;
//                pitch = roll;
//                roll = temp;
//                break;
//            case Surface.ROTATION_180: // phone is rotated 180 degrees
//                pitch = -pitch;
//                roll = -roll;
//                break;
//            case Surface.ROTATION_270:  // phone is turned 90 degrees clockwise
//                temp = pitch;
//                pitch = -roll;
//                roll = temp;
//                break;
//        }
//    }
//    private void oldUseSensor(SensorEvent event){
//        //GeoUtils.getAPR(event, mDisplay.getRotation(), azimutharr, pitcharr, rollarr);
//
//        // @TODO Check sensor accuracy
//        // if(event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) return;
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
//        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
//        }
//        // Compute the three orientation angles based on the most recent readings from
//        // the device's accelerometer and magnetometer.
//        // Update rotation matrix, which is needed to update orientation angles.
//        SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);
//
//        float[] mRemappedR = new float[9];
//        mRemappedR = mRotationMatrix;
//        // @TODO Check this section
//        switch (mDisplay.getRotation()) {
//            case Surface.ROTATION_0:
//                //                    mSensorX = event.values[0];
//                //                    mSensorY = event.values[1];
//                //                SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Y, mRemappedR);
//                mRemappedR = mRotationMatrix.clone();
//                //                    rotationMatrixAdjusted = rotationMatrix.clone();
//                break;
//            case Surface.ROTATION_90:
//                //                    mSensorX = -event.values[1];
//                //                    mSensorY = event.values[0];
//                //                    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, mRemappedR);
//                SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mRemappedR);
//                break;
//            case Surface.ROTATION_180:
//                //                    mSensorX = -event.values[0];
//                //                    mSensorY = -event.values[1];
//                //                    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, mRemappedR);
//                SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, mRemappedR);
//                break;
//            case Surface.ROTATION_270:
//                //                    mSensorX = event.values[1];
//                //                    mSensorY = -event.values[0];
//                //                    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mRemappedR);
//                SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, mRemappedR);
//                break;
//        }
//
//        // "mRotationMatrix" now has up-to-date information.
//        SensorManager.getOrientation(mRemappedR, mOrientationAngles);
//
//        double easing = 0.05;
//        azimuth += easing * (mOrientationAngles[0] - azimuth);
//        pitch += easing * (mOrientationAngles[1] - pitch);
//        roll += easing * (mOrientationAngles[2] - roll);
//
//
//        // Pitch and roll values that are close to but not 0 cause the
//        // animation to flash a lot. Adjust pitch and roll to 0 for very
//        // small values (as defined by VALUE_DRIFT).
//        if (Math.abs(pitch) < VALUE_DRIFT) {
//            pitch = 0;
//        }
//        if (Math.abs(roll) < VALUE_DRIFT) {
//            roll = 0;
//        }
//
//        long now = System.currentTimeMillis();
//        long elapsedTime = now - prevTime;
//        if (elapsedTime > 250){
//            // если прошло больше 250 миллисекунд - сохраним текущее время
//            // и повернем картинку на 2 градуса.
//            // точка вращения - центр картинки
//            prevTime = now;
//            //            Log.d(TAG, "onSensorChanged: time " + elapsedTime);
//        }
//
//        // Only notify other receivers if there is a change in orientation greater than 2.0 degrees
//        if(Math.abs(Math.toDegrees(azimuth - preAngle)) >= 2.0) {
//            Log.d(TAG, "onSensorChanged: rotation " + mDisplay.getRotation());
//            //             "mOrientationAngles" now has up-to-date information.
//            tv_azimuth.setText( String.format( "%.2f", Math.toDegrees(azimuth)) );
//            tv_pitch.setText( String.format("%.2f", Math.toDegrees(pitch)) );
//            tv_roll.setText( String.format("%.2f", Math.toDegrees(roll)) );
//            preAngle = azimuth;
//
//            makeUseOfNewSensor();
//        }
//    }

}
