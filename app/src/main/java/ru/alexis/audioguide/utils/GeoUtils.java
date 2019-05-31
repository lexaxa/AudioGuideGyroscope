package ru.alexis.audioguide.utils;

import android.opengl.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;
import android.view.Surface;

import java.util.ArrayList;
import java.util.Arrays;

import ru.alexis.audioguide.model.Place;

import static android.support.graphics.drawable.PathInterpolatorCompat.EPSILON;

public class GeoUtils {

    private static final String TAG = GeoUtils.class.getSimpleName();
    private static float EARTH_RADIUS = 6371000;
    private static float[] mAccelerometerReading = new float[3];
    private static float[] mMagnetometerReading = new float[3];
    private static float[] mAccelerometerReadingSrc = new float[3];
    private static float[] mMagnetometerReadingSrc = new float[3];
    private static float[] mGyroscopeReading = new float[3];
    private static float[] mRotationMatrix = new float[9];
    private static float[] mPrevRotationMatrix = new float[9];
    public static boolean isNeedRefresh = true;

    /** The lower this is, the greater the preference which is given to previous values. (slows change) */
    private static final float accelFilteringFactor = 0.1f;
    private static final float magFilteringFactor = 0.01f;

    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float[] deltaRotationVector = new float[4];
    private static float timestamp;
    private static float[] deltaRotationMatrix;
    private static float[] rotationCurrent;
    private static float[] mGyroscopeRotated;
    private static float[] angleChange = new float[3];

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static ArrayList<Place> points = new ArrayList<>();

    private GeoUtils() {
    }

    public static Location getFakeLocation(Location srcLocation, double azimuth) {

        int resDist = 1000;

        Location resLoc = new Location("fake location");

        // calc fake location from current location by azimuth to point through 10km
//        LAT1 = LAT + L * COS(AZIMUT * PI / 180) / (6371000 * PI / 180)
//        LON1 = LON + L * SIN(AZIMUT * PI / 180) / COS(LAT * PI / 180) / (6371000 * PI / 180)
        double resLatitude = srcLocation.getLatitude() + resDist * Math.cos(azimuth) / (6371000 * Math.PI / 180);
        double resLongitude = srcLocation.getLongitude() + resDist * Math.sin(azimuth) / Math.cos(srcLocation.getLatitude() * Math.PI / 180) / (6371000 * Math.PI / 180);

//        Log.d(TAG, "makeUseOfNewSensor: fake loc1: " + azimuth + "=" + resLatitude + ", " + resLongitude);

//        $newlat = sin($lat * PI / 180)*cos($dist * PI / 180)+cos($lat * PI / 180)*sin($dist * PI / 180)*cos($ang * PI / 180);
//        $newlat = asin($newlat);
//
//        $newlng = sin($dist * PI / 180)*sin($ang * PI / 180)/(cos($lat * PI / 180)*cos($dist * PI / 180)-sin($lat * PI / 180)*sin($dist * PI / 180)*cos($ang * PI / 180));
//        $newlng = atan($newlng);

//        https://www.programcreek.com/java-api-examples/?class=android.hardware.SensorManager&method=getOrientation
//        https://gis-lab.info/forum/viewtopic.php?t=9874

        resLoc.setLatitude(resLatitude);
        resLoc.setLongitude(resLongitude);

        return resLoc;
    }

    public static String getDirection(double azimuth) {
        /*
         * When facing north, this angle is 0, when facing south, this angle is &pi;.
         * Likewise, when facing east, this angle is &pi;/2, and
         * when facing west, this angle is -&pi;/2. The range of
         * values is -&pi; to &pi;.</li>
         */
        String direct;
        if (azimuth > -Math.PI / 6 && azimuth < Math.PI / 6) {
            direct = "North";
        } else if (azimuth > -Math.PI / 1.5 && azimuth < -Math.PI / 3) {
            direct = "West";
        } else if (azimuth < Math.PI / 1.5 && azimuth > Math.PI / 3) {
            direct = "East";
        } else if (azimuth < -Math.PI / 2 && azimuth > Math.PI / 2) {
            direct = "South";
        } else {
            direct = "N/A";
        }

        return direct;

    }
    // Action Per Rotation
    public static void getAPR(SensorEvent event, int rotation, float[] mOrientationAngles) {

        // @TODO Check sensor accuracy
//        if(event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
            mAccelerometerReading = exponentialSmoothing(event.values, mAccelerometerReading, 0.2f);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
            mMagnetometerReading = exponentialSmoothing(event.values, mMagnetometerReading, 0.5f);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            System.arraycopy(event.values, 0, mGyroscopeReading, 0, mGyroscopeReading.length);
            //Log.d(TAG, "getAPR: gyro " + mGyroscopeReading[0] + ":" + mGyroscopeReading[1] + ":" + mGyroscopeReading[2]);
            Log.d(TAG, "getAPR: gyro " +
                    String.format("%.2f", mGyroscopeReading[0]) + " : " +
                    String.format("%.2f", mGyroscopeReading[1]) + " : " +
                    String.format("%.2f", mGyroscopeReading[2])
            );

            // -> 0
            // |  1
            // <- 0
            if(Math.abs(mGyroscopeReading[1]) > 0.01){
                isNeedRefresh = true;
            }else{
                isNeedRefresh = false;
                //return;
            }

           // onGyroscopeChanged(event);

        } else {
            Log.d(TAG, "getAPR: sensor type " + event.sensor.getName());
            return;
        }
//        SensorManager.getAngleChange(angleChange, mRotationMatrix, mPrevRotationMatrix);
//        Log.d(TAG, "getAPR: " + Arrays.toString(angleChange));
        // Compute the three orientation angles based on the most recent readings from
        // the device's accelerometer and magnetometer.
        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(mRotationMatrix, null, mAccelerometerReading, mMagnetometerReading);

        float[] mRemappedR = changeRotation(rotation);
        // "mRotationMatrix" now has up-to-date information.
        SensorManager.getOrientation(mRemappedR, mOrientationAngles);
//        System.arraycopy(mRotationMatrix, 0, mPrevRotationMatrix, 0, mPrevRotationMatrix.length);
    }

    /** https://developer.android.com/guide/topics/location/strategies
     * Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * x = R cosθ sinφ
     * y = R sinθ sinφ
     * z = R cosθ
     * Где −180∘≤φ≤180∘ - это долгота, а −90∘≤θ≤90∘ - это широта, R - радиус земного шара.
     * @param location
     * @param decartCoords
     */
    public void getDecartCoords(Location location, double[] decartCoords){
        decartCoords[0] = EARTH_RADIUS * Math.cos(location.getLatitude()) * Math.sin(location.getLongitude());
        decartCoords[1] = EARTH_RADIUS * Math.sin(location.getLatitude()) * Math.sin(location.getLongitude());
        decartCoords[2] = EARTH_RADIUS * Math.cos(location.getLatitude());
    }

    private static void onGyroscopeChanged(SensorEvent event) {
        // This time step's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float) Math.sqrt(axisX*axisX + axisY*axisY + axisZ*axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude;
                axisY /= omegaMagnitude;
                axisZ /= omegaMagnitude;
            }

            // Integrate around this axis with the angular speed by the time step
            // in order to get a delta rotation from this sample over the time step
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;
            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;
        }
        timestamp = event.timestamp;
        deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
        // User code should concatenate the delta rotation we computed with the current
        // rotation in order to get the updated rotation.
        Matrix.multiplyMM(mGyroscopeRotated, 0, mGyroscopeReading , 0, deltaRotationMatrix, 0);
        Log.d(TAG, "onGyroscopeChanged: " + Arrays.toString(mGyroscopeRotated));
    }

    //https://www.youtube.com/watch?v=C7JQ7Rpwn2k
//    panX += angleChange[1]*panXSF;
//    panY += angleChange[0]*panYSF;
//    glTranslatef(panX, panY, 0);

    /*
    public static void filterKalman(){
        clear all;
        N=100  % number of samples
        a=0.1 % acceleration
        sigmaPsi=1
        sigmaEta=50;
        k=1:N
        x=k
        x(1)=0
        z(1)=x(1)+normrnd(0,sigmaEta);
        for t=1:(N-1)
          x(t+1)=x(t)+a*t+normrnd(0,sigmaPsi);
           z(t+1)=x(t+1)+normrnd(0,sigmaEta);
        end;
        %kalman filter
        xOpt(1)=z(1);
        eOpt(1)=sigmaEta; % eOpt(t) is a square root of the error dispersion (variance). It's not a random variable.
        for t=1:(N-1)
          eOpt(t+1)=sqrt((sigmaEta^2)*(eOpt(t)^2+sigmaPsi^2)/(sigmaEta^2+eOpt(t)^2+sigmaPsi^2))
          K(t+1)=(eOpt(t+1))^2/sigmaEta^2
         xOpt(t+1)=(xOpt(t)+a*t)*(1-K(t+1))+K(t+1)*z(t+1)
        end;
        plot(k,xOpt,k,z,k,x)
    }
     */

    private static float[] changeRotation(int rotation) {

        float[] mRemappedR = new float[9];
        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Z;
        //   0 - 0 Y-up
        // 270 - 1 Y-left
        // 180 - 2 Y-down
        //  90 - 3 Y-right
        switch (rotation) {
            case Surface.ROTATION_0:
                axisX = SensorManager.AXIS_X;
                axisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_270: // old 90
                axisX = SensorManager.AXIS_MINUS_Z;
                axisY = SensorManager.AXIS_X;
                break;
            case Surface.ROTATION_180:
                axisX = SensorManager.AXIS_MINUS_X;
                axisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_90: // old 270
                axisX = SensorManager.AXIS_Z;
                axisY = SensorManager.AXIS_MINUS_X;
                break;
            default:
                break;
        }
        SensorManager.remapCoordinateSystem(mRotationMatrix, axisX, axisY, mRemappedR);
        return mRemappedR;
    }

    /**
     * Translate angle from -360 to 360 at from -180 to 180
     * -350 to 10, 350 to -10
     * @param angle in degrees
     * @return angle in range -180 to 180
     */
    public static double getNormalAngle(double angle) {
        /*
        // wrong sign
        if (angle <= -180) {
            angle += (-2*(180 + angle));
        } else if(angle >= 180){
            angle += ( 2*(180 - angle));
        }*/
        //The longitude can be normalised to −180…+180 using (lon+540)%360-180
//            if(Math.abs(angle) > 180){
//                angle = (angle + 360) % 360;
//            }
//            if(Math.abs(angle2) > 180){
//                angle2= (angle2+ 360) % 360;
//            }
        return (angle + 540) % 360 - 180;
    }

    public static double getAngleBetween2Loc(Location curLoc, Location destLoc){
        return Math.toDegrees(
                Math.atan2(
                        Math.toRadians(destLoc.getLatitude() - curLoc.getLatitude()),
                        Math.toRadians(destLoc.getLongitude() - curLoc.getLongitude())
                )
        );
    }

    /**
     <pre>
     B
     /
     /
     /______C
     A
     </pre>
     */
    public static double getAngleBAC(Location a, Location b, Location c){

        float ab = a.distanceTo(b);
        float ac = a.distanceTo(c);
        float bc = b.distanceTo(c);
        return Math.toDegrees(Math.acos((ab*ab + ac*ac - bc*bc)/(2*ab*ac)));
    }

    public static float[] exponentialSmoothing(float[] input, float[] output, float alpha) {
        // x(k+1) = Kstab * z(k+1) + (1 - Kstab) * x(k)
        for (int i = 0; i < input.length; i++) {
//            output[i] = output[i] + alpha * (input[i] - output[i]);
            output[i] = alpha * input[i] + (1 - alpha) * output[i];
        }
        return output;
    }

    public static void setupPoints(){
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

    public static ArrayList<Place> getPoints() {
        return points;
    }

    private double getAzimuth(double y, double x) {

        /*
            аблица 1. Расчет азимута
            X        Y      Азимут
            0       <0      90
                    >0      270
            <0      любое   180-(arctg(Y/X) * 180/PI

            >0      <0
                    >0
*/
//        https://www.programcreek.com/java-api-examples/?code=karuiel/GPSTracker/GPSTracker-master/app/src/main/java/com/example/gpstracker/Route.java
        double az = 0;

        if (x == 0) {
            if (y < 0) {
                az = 90;
            } else {
                az = -90; //270;
            }
            return az;
        }
        if (x < 0) {
            az = 180 - Math.atan(y / x) * 180 / Math.PI;
//            if (y < 0) {
//                az = Math.atan(y/x)*180/Math.PI - 180;
//            } else {
//                az = Math.atan(y/x)*180/Math.PI + 180;
//            }
            return az;
        } else if (x > 0) {
            if (y < 0) {
                az = -Math.atan(y / x) * 180 / Math.PI;
            } else if (y > 0) {
                az = 360 - Math.atan(y / x) * 180 / Math.PI;
            }
            return az;
        }
        return 0;
    }
}
