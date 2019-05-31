package ru.alexis.audioguide.compass;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
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
import java.util.Locale;

/**
 * View that shows touch events and their history. This view demonstrates the
 * use of {@link #onTouchEvent(android.view.MotionEvent)} and {@link android.view.MotionEvent}s to keep
 * track of touch pointers across events.
 */
public class TriangleView extends View implements LocationListener {
    private final String TAG = "MyLog";
    // Is there an active touch?
    private boolean mHasTouch = false;

    GraphicActivity mainActivity;

    private boolean mStarted;
    private LocationManager mLocationManager;
    private LocationProvider mProvider;
    private Proj mProj;

    private Paint mPaint = new Paint();
    private Paint mTextPaint = new Paint();

    private static final int BACKGROUND_ACTIVE = Color.WHITE;

    // inactive border
    private static final float INACTIVE_BORDER_DP = 3f;
    private static final int INACTIVE_BORDER_COLOR = 0xFFffd060;
    private Paint mBorderPaint = new Paint();
    private float mBorderWidth;

    /// 處理座標問題, 座標的原點在左下角 (Ox, Oy) 處
    /// 在對應座標到 View 時， X = x + Ox; Y = (getHeight() - Oy) - y;
    private float Ox = 60, Oy = 60; // 視座標原點在 View 的 (60,60) 處
    private Point cA, cB, cP;
    private Path mTrianglePath;
    private double W, w, H, h, Ratio = 1;

    // 處理移動問題
    private int pressLine = 0; // 0 none, 1 left, 2 right

    public TextView tvACoordinate, tvBCoordinate, tvGpsCoordinate, tvPCoordinate;

    public TriangleView(Context context) {
        this(context, null);
    }

    public TriangleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TriangleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mainActivity = (GraphicActivity) context;

        mainActivity.A = new PointF(0, 0);
        mainActivity.B = new PointF(100, 0);
        mainActivity.distance = Tools.len(mainActivity.A.x - mainActivity.B.x, mainActivity.A.y - mainActivity.B.y);
        double r = mainActivity.distance * Math.sin(mainActivity.angleB) / (Math.sin(mainActivity.angleA - mainActivity.angleB));
        mainActivity.P = new PointF((float) (-r * Math.cos(mainActivity.angleA) + mainActivity.A.x),
                (float) (r * Math.sin(mainActivity.angleA) + mainActivity.A.y));
        mainActivity.angleA = 120 * Math.PI / 180;
        mainActivity.angleB = 60 * Math.PI / 180;

        initialisePaint();
        mLocationManager = (LocationManager) mainActivity.getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (mProvider == null) {
            warn("未支援 GPS", 0);
            mainActivity.finish();
            return;
        }

        mProj = new Proj();
        restore();
    }

    private void calcPath() {
        /// 有幾件事要做,  目前只知道 angleA, angleB 改變
        calcP();

        /// 1. 先依 D/cA/cB 計算座標, cA,cB 點永遠在 cA(0,0), cB(D,0)

        double D = calcD(), L = calcL(), R = calcR();
        double L2 = L * L;
        double R2 = R * R;
        double D2 = D * D;
        double fPy2 = mainActivity.P.y * mainActivity.P.y;

        /// 2. 縮放到 TriangleView 座標系統
        ///    在對應座標到 View 時， X = x + Ox; Y = (getHeight() - Oy) - y;
        ///   1) 先求比例值
        W = getWidth() - 20;
        w = W - Ox;
        H = getHeight() - 20;
        h = H - Oy;
        double bx = mainActivity.distance;
        double px = L * Math.cos(Math.PI - mainActivity.angleA);
        double py = L * Math.sin(Math.PI - mainActivity.angleA);
        if (pressLine != 2) { // 當壓在右邊上的時候，不要做縮放
            double r1 = py / h;
            double r2 = D / w;
            double r3 = (D - px) / w; // cP 在原點左邊為負值
            double r4 = px / w;
            Ratio = r1;
            if (r2 > Ratio) Ratio = r2;
            if (r3 > Ratio) Ratio = r3;
            if (r4 > Ratio) Ratio = r4;
        }

        ///   2) 再將 A, B, P 映射到畫布座標
        cA = new Point(mapX(0), mapY(0));
        cB = new Point(mapX(bx), mapY(0));
        cP = new Point(mapX(px), mapY(py));

        mTrianglePath = new Path();
        mTrianglePath.moveTo(cA.x, cA.y);
        mTrianglePath.lineTo(cB.x, cB.y);
        mTrianglePath.lineTo(cP.x, cP.y);
        mTrianglePath.lineTo(cA.x, cA.y);
    }

    private int mapX(double x) {
        return (int) ((double) ((x) / Ratio + Ox));
    }

    private int mapY(double y) {
        return (int) ((double) (h + 20 - (y) / Ratio));
    }

    private void initialisePaint() {
        // Calculate radiuses in px from dp based on screen density
        float density = getResources().getDisplayMetrics().density;

        // 一般點、線的畫筆
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(3);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);

        // Setup text paint for circle label
        mTextPaint.setTextSize(20f);
        mTextPaint.setColor(Color.YELLOW);

        // Setup paint for inactive border
        mBorderWidth = INACTIVE_BORDER_DP * density;
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setColor(INACTIVE_BORDER_COLOR);
        mBorderPaint.setStyle(Paint.Style.STROKE);
    }

    // BEGIN_INCLUDE(onTouchEvent)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        /*
         * Switch on the action. The action is extracted from the event by
         * applying the MotionEvent.ACTION_MASK. Alternatively a call to
         * event.getActionMasked() would yield in the action as well.
         */
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN: { // 類似上面，但是這邊還不是最後一點
                pressLine = 0;
                break;
            }
            case MotionEvent.ACTION_UP: { // 手指離開，視為最後一點離開
                mHasTouch = false;
                pressLine = 0;
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: { // 類似上面，但是這邊還不是最後一點
                pressLine = 0;
                break;
            }

            case MotionEvent.ACTION_DOWN: { // 手指按下去, 這個事件只有單點，視為第1點
                mHasTouch = true;

                double x, y, dl, dr, abs;
                double a, b, c;

                x = event.getX();
                y = event.getY();
                a = (cP.y - cA.y);
                b = -(cP.x - cA.x);
                c = -(a * cA.x + b * cA.y);
                abs = Math.sqrt(a * a + b * b);
                dl = Math.abs(a * x + b * y + c) / abs;

                a = (cP.y - cB.y);
                b = (cB.x - cP.x);
                c = -(a * cB.x + b * cB.y); // (cB.y * (cP.x - cB.x) + cB.x * (cB.y - cP.y));
                abs = Math.sqrt(a * a + b * b);
                dr = Math.abs(a * x + b * y + c) / abs;

                if (dl < 20 && 20 < dr) pressLine = 1;
                else if (dl > 20 && 20 > dr) pressLine = 2;
                else pressLine = 0;
                break;
            }

            case MotionEvent.ACTION_MOVE: { // 用來實現放大縮小，放遠放近, 在按第二點以上時，通常會產生一堆 ACTION_MOVE
                if (pressLine > 0) {
                    double x, y, Vx, Vy, len, cos, acos;

                    x = event.getX();
                    y = event.getY();
                    if (pressLine == 1) {
                        Vx = x - cA.x;
                        Vy = y - cA.y;
                    } else {
                        Vx = x - cB.x;
                        Vy = y - cB.y;
                    }
                    len = Math.sqrt(Vx * Vx + Vy * Vy);
                    cos = -Vx / len;
                    if (cos < -1 && cos > -2) cos = -1;
                    else if (cos > 1 && cos < 2) cos = 1;
                    acos = Math.acos(cos);
                    if (pressLine == 1) mainActivity.angleA = acos;
                    else mainActivity.angleB = acos;
                    calcP();
                    save();
                }
                break;
            }
        }

        // trigger redraw on UI thread
        this.postInvalidate();
        return true;
    }

    private void save() {
        File file = new File(Environment.getExternalStorageDirectory() + "/save-compass-2.txt");
        try {
            FileWriter f = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(f);
            bufferedWriter.write(
                    mainActivity.A.x + "\n" +
                            mainActivity.A.y + "\n" +
                            mainActivity.angleA + "\n" +
                            mainActivity.B.x + "\n" +
                            mainActivity.B.y + "\n" +
                            mainActivity.angleB + "\n"
            );
            bufferedWriter.flush();
            bufferedWriter.close();
            f.close();
        } catch (IOException e) {
            warn("無法儲存資料到 save-compass-2.txt", 0);
        }
    }

    private void restore() {
        File file = new File(Environment.getExternalStorageDirectory() + "/save-compass-2.txt");
        try {
            FileReader f = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(f);
            String line;
            if ((line = bufferedReader.readLine()) != null) {
                mainActivity.A.x = Float.parseFloat(line.trim());
                if ((line = bufferedReader.readLine()) != null) {
                    mainActivity.A.y = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    mainActivity.angleA = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    mainActivity.B.x = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    mainActivity.B.y = Float.parseFloat(line.trim());
                }
                if ((line = bufferedReader.readLine()) != null) {
                    mainActivity.angleB = Float.parseFloat(line.trim());
                }
            }
            bufferedReader.close();
            f.close();
        } catch (IOException e) {
            warn("無法從 save-compass-2.txt 讀取資料", 0);
        }
    }

    public void setA() {
        float ox = mainActivity.A.x, oy = mainActivity.A.y;
        mainActivity.A.x = (float) currentLocation[0];
        mainActivity.A.y = (float) currentLocation[1];
        mainActivity.B.x = (float) (mainActivity.B.x + (mainActivity.A.x - ox));
        mainActivity.B.y = (float) (mainActivity.B.y + (mainActivity.A.y - oy));
        tvACoordinate.setText(String.format("E %.2f\nN %.2f", mainActivity.A.x, mainActivity.A.y));
        save();
        this.invalidate();
    }

    public void setB() {
        mainActivity.B.x = (float) currentLocation[0];
        mainActivity.B.y = (float) currentLocation[1];
        tvBCoordinate.setText(String.format("E %.2f\nN %.2f", mainActivity.B.x, mainActivity.B.y));
        save();
        this.invalidate();
    }

    private double calcD() {
        return Tools.len(mainActivity.A.x - mainActivity.B.x, mainActivity.A.y - mainActivity.B.y);
    }

    private double calcL() {
        return Tools.len(mainActivity.A.x - mainActivity.P.x, mainActivity.A.y - mainActivity.P.y);
    }

    private double calcR() {
        return Tools.len(mainActivity.P.x - mainActivity.B.x, mainActivity.P.y - mainActivity.B.y);
    }

    private void drawL(Canvas canvas) {
        canvas.drawText("L", (int) ((cA.x + cP.x) / 2 - 25), (int) ((cA.y + cP.y) / 2), mTextPaint);
        canvas.drawText(String.format("L=%.2f 米", calcL()), Ox, 50, mTextPaint);
    }

    private void drawR(Canvas canvas) {
        canvas.drawText("R", (int) ((cB.x + cP.x) / 2 + 10), (int) ((cB.y + cP.y) / 2), mTextPaint);
        canvas.drawText(String.format("R=%.2f 米", calcR()), getWidth() - 200, 50, mTextPaint);
    }

    private void drawD(Canvas canvas) {
        canvas.drawText("D", (cA.x + cB.x) / 2 - 30, cA.y - 15, mTextPaint);
        canvas.drawText(String.format("%.2f 米", calcD()), (int) ((cA.x + cB.x) / 2 - 85), cA.y + 30, mTextPaint);
    }

    private void calcP() {
        mainActivity.angleP = mainActivity.angleA - mainActivity.angleB;
        mainActivity.distance = Tools.len(mainActivity.A.x - mainActivity.B.x, mainActivity.A.y - mainActivity.B.y);

        double r = mainActivity.distance * Math.sin(mainActivity.angleB) / (Math.sin(mainActivity.angleA - mainActivity.angleB));
        mainActivity.P.x = (float) (-r * Math.cos(mainActivity.angleA) + mainActivity.A.x);
        mainActivity.P.y = (float) (r * Math.sin(mainActivity.angleA) + mainActivity.A.y);
        mainActivity.showP();
    }

    // 底下相當於重新繪製 TriangleView
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        /// 有幾件事要做
        /// 1. 就是畫框框來讓使用者明確知道有沒有碰到螢幕
        if (mHasTouch) {
            if (pressLine == 0) canvas.drawColor(BACKGROUND_ACTIVE);
            else canvas.drawColor(Color.LTGRAY);
        } else {
            canvas.drawColor(Color.BLUE);
            canvas.drawRect(mBorderWidth, mBorderWidth, getWidth() - mBorderWidth, getHeight()
                    - mBorderWidth, mBorderPaint);
        }

        /// 2. 畫地線
        mPaint.setColor(Color.BLACK);
        canvas.drawLine(mBorderWidth, getHeight() - 60, getWidth() - mBorderWidth, getHeight() - 60, mPaint);
        mPaint.setColor(Color.RED);

        /// 3. 畫三角形
        calcPath();
        if (mTrianglePath != null) {
            canvas.drawPath(mTrianglePath, mPaint);
        }
        /// 4. 標註 D, L, R 字樣
        drawD(canvas);
        drawL(canvas);
        drawR(canvas);

        /// 5. 標註計算結果及角度值
        ///   1) 角 cA
        canvas.drawText("A", cA.x - 25, cA.y - 15, mTextPaint);
        canvas.drawText(Tools.Deg2DmsStr(Tools.Rad2Deg(mainActivity.angleA)), cA.x - 25, cA.y - 50, mTextPaint);

        ///   3) 角 cB
        canvas.drawText("B", cB.x - 35, cB.y - 15, mTextPaint);
        canvas.drawText(Tools.Deg2DmsStr(Tools.Rad2Deg(mainActivity.angleB)), cB.x - 130, cB.y - 45, mTextPaint);

        /// 5) 角 cP
        canvas.drawText("P", cP.x - 10, cP.y + 30, mTextPaint);
    }

    private void warn(String msg, int level) {
        if (level < 2) Log.d("MyLog", msg);
        if (level != 1) Toast.makeText(mainActivity, msg, Toast.LENGTH_LONG).show();
    }

    public void start() {
        if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.requestLocationUpdates(mProvider.getName(), 1000, 0, this);
    }

    void stop() {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    private void removeStatusListener() {
    }

    private double[] currentLocation = new double[]{0, 0};

    private synchronized void gpsStart() {
        if (!mStarted) {
            if (ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.requestLocationUpdates(mProvider.getName(), 1000, 0, this);
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

    public void onLocationChanged(Location location) {
        currentLocation = mProj.LL2TM2(location.getLongitude(), location.getLatitude());
        if (tvGpsCoordinate != null)
            tvGpsCoordinate.setText(String.format(Locale.CHINESE, "E %s\nN %s\nE %.2f米\nN %.2f米\n橢球高=%.2f米",
                    Tools.Deg2DmsStr2(location.getLongitude()), Tools.Deg2DmsStr2(location.getLatitude()),
                    currentLocation[0], currentLocation[1],
                    location.getAltitude()
            ));
    }
}