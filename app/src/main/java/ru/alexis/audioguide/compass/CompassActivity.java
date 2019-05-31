package ru.alexis.audioguide.compass;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import ru.alexis.audioguide.R;

public class CompassActivity extends Activity {
    private final String TAG="MyLog";
    private Compass compass;
    private boolean pause=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        compass = new Compass(this);
        if (compass == null) {
            return;
        }
        compass.arrowView = (ImageView) findViewById(R.id.main_image_hands);
        compass.arrowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickOnCP(view);
            }
        });
        compass.compassView = (ImageView) findViewById(R.id.main_image_compass);
        compass.compassView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickOnCP(view);
            }
        });

        compass.mTvCompass = (TextView)findViewById(R.id.tvCompass);
        compass.mTvCompass.setText(" ");
        compass.mTvCompass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause = !pause;
                compass.setPause(pause);
            }
        });

        compass.mTvControllPoint = (TextView)findViewById(R.id.tvControlPoint);
        compass.mTvControllPoint.setText(
                "                                 \n"+
                        "                                 \n"+
                        "                                 \n"+
                        "                                 \n"+
                        "                                 \n");

        compass.mTvGps = (TextView)findViewById(R.id.tvGPS);
        compass.mTvGps.setText(" ");
        compass.mTvGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compass.setPADS(1);
            }
        });

        compass.mTvPADS = (TextView)findViewById(R.id.tvPADS);
        compass.mTvPADS.setText(" ");
        compass.mTvPADS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compass.setPADS(2);
            }
        });
    }

    public void clickOnCP(View v) {
        Intent intent = new Intent(this, GraphicActivity.class);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }
    @Override
    protected void onStart() {
        super.onStart();
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        compass.stop();
        super.onDestroy();
    }
}