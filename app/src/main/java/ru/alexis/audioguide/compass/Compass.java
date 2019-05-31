package ru.alexis.audioguide.compass;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//import com.imobile.libs.Proj;
//import com.imobile.libs.Tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

class Compass implements SensorEventListener, LocationListener {
    private static final String TAG = "MyLog";

    private CompassActivity mainActivity;
    private SensorManager mSensorManager;
    private Sensor gSensor;
    private Sensor mSensor;
    private Sensor oSensor;
    private Sensor rSensor;
    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] mOrientation = new float[3];
    private float tazimuth=0f;
    private float currectAzimuth = 0, fixAzimuth = 0;
    private int count = 0;
    private boolean pause;

    // compass arrow to rotate
    ImageView arrowView = null;
    ImageView compassView = null;
    TextView mTvCompass, mTvGps, mTvControllPoint, mTvPADS;

    // GPS
    private static final int SECONDS_TO_MILLISECONDS = 1000;
    private boolean mStarted;
    private boolean mFaceTrueNorth;
    private LocationManager mLocationManager;
    private LocationProvider mProvider;

    private GeomagneticField mGeomagneticField;
    private long minTime = SECONDS_TO_MILLISECONDS; // Min Time between location updates, in milliseconds
    private float minDistance = 0; // Min Distance between location updates, in meters

    private Proj mProj;
    private CPDB cpdb;

    private double lastX = 0, lastY = 0;
    private double O1X=0, O1Y=0, O1A=0, O2X=0, O2Y=0, O2A=0;
    private double[] currentLocation = new double[]{0,0};

    private void warn(String msg, int level) {
        if (level < 2) Log.d("MyLog", msg);
        if (level != 1) Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();
    }

    Compass(Context context) {
        mainActivity = (CompassActivity) context;
        mSensorManager = (SensorManager) mainActivity.getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            warn("請檢查本應用程式是否授權使用『位置』及『儲存』", 0);
            mainActivity.finish();
            return;
        }
        mProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (mProvider == null) {
            warn("未支援 GPS", 0);
            mainActivity.finish();
            return;
        }
        minTime = (long) (SECONDS_TO_MILLISECONDS);
        minDistance = 0;

        gSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        oSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        rSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mProj = new Proj();
        cpdb = new CPDB(context);
        restore();
    }

    void start() {
        mSensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);

        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            promptEnableGps();
        }
        mLocationManager.requestLocationUpdates(mProvider.getName(), minTime, minDistance, this);
        mFaceTrueNorth = true;
        gpsStart();
    }

    void stop() {
        if (gSensor != null) mSensorManager.unregisterListener(this, gSensor);
        if (mSensor != null) mSensorManager.unregisterListener(this, mSensor);
        if (oSensor != null) mSensorManager.unregisterListener(this, oSensor);
        if (rSensor != null) mSensorManager.unregisterListener(this, rSensor);
        gpsStop();
    }
    private void save() {
        File file = new File(Environment.getExternalStorageDirectory()+"/save-compass-1.txt");
        try {
            FileWriter f = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(f);
            bufferedWriter.write(
                    O1X+"\n"+
                            O1Y+"\n"+
                            O1A+"\n"+
                            O2X+"\n"+
                            O2Y+"\n"+
                            O2A+"\n"
            );
            bufferedWriter.flush();
            bufferedWriter.close();
            f.close();
        } catch (IOException e) {
            warn("無法儲存資料到 save-compass-1.txt", 0);
        }
    }
    private void restore() {
        File file = new File(Environment.getExternalStorageDirectory()+"/save-compass-1.txt");
        try {
            FileReader f = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(f);
            String line;
            if ((line = bufferedReader.readLine()) != null) {
                O1X = Float.parseFloat(line.trim());
                if ((line = bufferedReader.readLine()) != null) {
                    O1Y = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    O1A = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    O2X = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    O2Y = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    O2A = Float.parseFloat(line.trim());
                }
            }
            bufferedReader.close();
            f.close();
        } catch (IOException e) {
            warn("無法從 save-compass-1.txt 讀取資料", 0);
        }
    }

    void setPause(boolean p) { pause = p; count = 0; }
    private float O1Selected=0, O2Selected=0;
    void setPADS(int o1o2) {
        if (currentLocation == null || currentLocation[0] == 0 || currentLocation[1] == 0) return;
        if (o1o2 == 1) {
            if (O1Selected > 0) {
                O1Selected = 0;
                O1X = O1Y = 0;
            } else {
                O1Selected = 1;
                O1X = (float)currentLocation[0]; O1Y = (float)currentLocation[1];
                O1A = tazimuth;
            }
        } else if (o1o2 == 2) {
            if (O2Selected > 0) {
                O2Selected = 0;
                O2X = O2Y = 0;
            } else {
                O2Selected = 1;
                O2X = (float)currentLocation[0]; O2Y = (float)currentLocation[1];
                O2A = tazimuth;
            }
        } else return;
        O1X = (int)(O1X/10) * 10;
        O1Y = (int)(O1Y/10) * 10;
        O2X = (int)(O2X/10) * 10;
        O2Y = (int)(O2Y/10) * 10;
        String msg = "";
        if (O1Selected > 0 && O2Selected > 0 && (O1X != O2X && O1Y != O2Y)) {
            double[] res = Tools.Pads(O1X, O1Y, 0, 0, O1A, 0, O2X, O2Y, 0, 0, O2A, 0);
            res[0] = (int)(res[0]/10) * 10;
            res[1] = (int)(res[1]/10) * 10;
            msg += String.format(Locale.CHINESE, "O1--T:%d公尺\nO2--T:%d公尺\nO1--O2:%d公尺\nT(%d,%d)\n",
                    (int)Tools.len(res[0]-O1X, res[1]-O1Y),
                    (int)Tools.len(res[0]-O2X, res[1]-O2Y),
                    (int)Tools.len(O1X-O2X, O1Y-O2Y),
                    (int)res[0], (int)res[1]
            );
        }
        if (O1Selected > 0 && O1X > 0 && O1Y > 0) msg += String.format(Locale.CHINESE,
                "O1方位角=%s\n(%d,%d)\n", Tools.Deg2DmsStr(O1A), (int)O1X, (int)O1Y);
        if (O2Selected > 0 && O2X > 0 && O2Y > 0) msg += String.format(Locale.CHINESE,
                "O2方位角=%s\n(%d,%d)\n", Tools.Deg2DmsStr(O2A), (int)O2X, (int)O2Y);
        mTvPADS.setText(msg);
        save();
    }

    private void adjustArrow() {
        if (compassView == null) {
            return;
        }
        compassView.clearAnimation();
        float azimuth = (mOrientation[0] + 90 + 360) % 360;
        if (mFaceTrueNorth && mGeomagneticField != null) {
            tazimuth = (azimuth +mGeomagneticField.getDeclination()+360)%360;
        } else tazimuth = azimuth;

        Animation an = new RotateAnimation(-currectAzimuth, -tazimuth,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        an.setDuration(200);
        an.setRepeatCount(0);
        an.setFillAfter(true);
        compassView.startAnimation(an);

        currectAzimuth = tazimuth;

        if (!pause) {
            if (++count%100 == 1) {
                mTvCompass.setTextColor(Color.BLUE);
                showCompassText(currectAzimuth, false);
            }
            fixAzimuth = tazimuth;

            if (count < 0) {
                count = 0;
            }
        }
        else {
            mTvCompass.setTextColor(Color.RED);
            showCompassText(fixAzimuth, true);
        }
    }

    private void showCompassText(float ta, boolean pause) {
        if (mFaceTrueNorth && mGeomagneticField != null) {
            double a = (ta+mGeomagneticField.getDeclination()+360)%360;
            mTvCompass.setText(String.format(Locale.CHINESE, "正北方位角 %s\n  %.3f度(%s)\n  %.2f mil(密位)\n磁方位角\n  %.3f度(%s)\n  %.2f mil(密位)\n",
                    (pause?"<<< 暫停更新 >>>":""),
                    ta,
                    Tools.Deg2DmsStr(ta),
                    Tools.Deg2Mil(ta),
                    a,
                    Tools.Deg2DmsStr(a),
                    Tools.Deg2Mil(a)
            ));
        } else {
            mTvCompass.setText(String.format(Locale.CHINESE, "磁方位角 %s\n  %.3f度(%s)\n  %.2f mil(密位)\n",
                    (pause?"<<< 暫停更新 >>>":""),
                    ta,
                    Tools.Deg2DmsStr(ta),
                    Tools.Deg2Mil(ta)
            ));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                lowPass(event.values, mGravity);
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                lowPass(event.values, mGeomagnetic);
            }
            float R[] = new float[9];
            float I[] = new float[9];
            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                mOrientation[0] = (float)Math.toDegrees(orientation[0]);
                adjustArrow();
            }
        }
    }
    private float[] lowPass(float[] input, float[] output) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + 0.03f * (input[i] - output[i]);
        }
        return output;
    }
    // GPS

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    public void onStatusChanged(String provider, int status, Bundle extras) {}
    public void onProviderEnabled(String provider) {}
    public void onProviderDisabled(String provider) {}

    private void promptEnableGps() {
        new AlertDialog.Builder(mainActivity)
                .setMessage("啟動 GPS")
                .setPositiveButton("啟動 GPS",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                mainActivity.startActivity(intent);
                            }
                        }
                )
                .setNegativeButton("關閉",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                )
                .show();
    }
    private void removeStatusListener() {
    }

    public void onLocationChanged(Location location) {
        mGeomagneticField = new GeomagneticField((float) location.getLatitude(),
                (float) location.getLongitude(), (float) location.getAltitude(),
                location.getTime());
        currentLocation = mProj.LL2TM2(location.getLongitude(), location.getLatitude());
        mTvGps.setText(String.format(Locale.CHINESE, "== GPS資訊 ==\n經度=%s\n緯度=%s\n橢球高=%.2f公尺\n速度=%.2f公尺/秒\n航向:%s\nTM2 E %.2f公尺, N %.2f公尺",
                Tools.Deg2DmsStr2(location.getLongitude()),
                Tools.Deg2DmsStr2(location.getLatitude()),
                location.getAltitude(),
                location.getSpeed(),
                Tools.ang2Str(location.getBearing()),
                currentLocation[0], currentLocation[1]));

        double dx = currentLocation[0] - lastX;
        double dy = currentLocation[1]  - lastY;
        if (Math.sqrt(dx*dx + dy*dy) > 1) {
            lastX = currentLocation[0]/10; lastX *= 10;
            lastY = currentLocation[1]/10; lastY *= 10;
            List<CPDB.CP> cps = cpdb.getCp(lastX, lastY, 0);
            if (cps.size() > 0) {
                String cpMsg = "鄰近控制點:\n";
                int i=0;
                for (CPDB.CP cp : cps) {
                    double dx1 = cp.x - currentLocation[0], dy1 = cp.y-currentLocation[1];
                    double[] resCP = Tools.POLd(dy1, dx1);
                    cpMsg += String.format(Locale.CHINESE, "%s %d@%d%s\n距離=%.0f公尺\n方位=%s\n",
                            cp.number, (++i), cp.t, (cp.name.length()>0?"("+cp.name+")":""),
                            resCP[0], Tools.Deg2DmsStr2(resCP[1])
                    );
                }
                mTvControllPoint.setText(cpMsg);
            }
        }
    }

    private synchronized void gpsStart() {
        if (!mStarted) {
            if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            {
                return;
            }
            mLocationManager.requestLocationUpdates(mProvider.getName(), minTime, minDistance, this);
            mStarted = true;
        }
    }
    private synchronized void gpsStop() {
        if (mStarted) {
            warn("gpsStop()", 1);
            mLocationManager.removeUpdates(this);
            mStarted = false;
        }
        removeStatusListener();
    }
}