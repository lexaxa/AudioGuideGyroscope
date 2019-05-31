package ru.alexis.audioguide.compass;


import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

import ru.alexis.audioguide.R;

public class GraphicActivity extends Activity {
    private final String TAG="MyLog";
    private TriangleView mView;
    public PointF A, B, P;
    public double distance, angleA, angleB, angleP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.graphic_activity);
        mView = new TriangleView(this);
        mView.tvACoordinate   = (TextView)findViewById(R.id.tvACoordinate);
        mView.tvBCoordinate   = (TextView)findViewById(R.id.tvBCoordinate);
        mView.tvGpsCoordinate = (TextView)findViewById(R.id.tvGpsCoordinate);
        mView.tvPCoordinate   = (TextView)findViewById(R.id.tvPCoordinate);
    }

    public void click(View v) {
        switch (v.getId()) {
            case R.id.tvTitle:
                Intent intent = new Intent(this, CompassActivity.class);
                intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                break;
            case R.id.btL:
                mView.setA();
                break;
            case R.id.btR:
                mView.setB();
                break;
        }
    }

    public void setA(float x, float y) {
        A.x = x;
        A.y = y;
    }
    public void setB(float x, float y) {
        B.x = x;
        B.y = y;
    }
    public void setP(float x, float y) {
        P.x = x;
        P.y = y;
    }
    public void showP() {
        mView.tvPCoordinate.setText(String.format(Locale.CHINESE, "E %.2f\nN %.2f",
                P.x, P.y));
    }
    @Override
    protected void onStart() {
        super.onStart();
        mView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mView.stop();
    }
}