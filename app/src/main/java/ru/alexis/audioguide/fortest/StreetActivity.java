package ru.alexis.audioguide.fortest;

import android.hardware.SensorListener;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import javax.microedition.khronos.opengles.GL10;

public class StreetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
/*
    private final SensorListener mListener = new SensorListener() {
        public void onSensorChanged(int sensor, float[] values) {
//            if (!okToAct()) {
//                return;
//            }
            float yaw;
            float pitch;

            yaw = values[0];
            pitch = values[1];

            // Street.log("sensorChanged (portrait: "
            // + " yaw " + yaw + ", pitch " + pitch
            // + ", " + values[0] + " " + values[1] + " "+ values[2] + ")");

            // Yaw is 0 to 360 degrees. The value of 360 needs to be mapped to 0
            yaw = yaw < 360.0f ? yaw : yaw - 360.0f;

            float newYaw = yaw; //mYawFilter.filter(yaw);

            // Street.log(String.format("yaw: %g -> %g", yaw, newYaw));

            pitch = mPitchFilter.filter(pitch);

            // The filter may have normalized pitch into 0..360. Return
            // it to -180..180)
            if (pitch > 180.0f) {
                pitch = pitch - 360.0f;
            }

            // Pitch coord system conversion
            // 0 == device flat on back == looking down at ground == 0.0
            // -90 == horizontal right side up == 0.5
            // -180 == upside down, looking at sky = 1.0
            // 90 == horizontal, inverted device.
            // It would be fun to handle the upside down view correctly, but
            // for now don't handle it, because it requires street view to
            // render upside down.
            //
            // In terms of what's comfortable, the natural pitch for holding
            // the phone in the hand at eye level is around -80 degrees.
            // Holding above eye level is comfortable up to about -100 degrees.
            // holding in lap is about -43 degrees.

            final float bottomPitch = -50.0f;
            final float bottomTilt = 0.2f;
            final float topPitch = -100.0f;
            final float topTilt = 0.95f;
            final float scalePitchToTilt = (topTilt - bottomTilt)
                    / (topPitch - bottomPitch);

            float newTilt = StreetMath.clamp(
                    bottomTilt + scalePitchToTilt * (pitch - bottomPitch),
                    0.0f, 1.0f);

            // Street.log(String.format("pitch: %g -> %g", pitch, newTilt));

            if ((newYaw != mUserOrientation.yaw)
                    || (newTilt != mUserOrientation.tilt)) {
                reportUserActivity();
                mUserOrientation.yaw = newYaw;
                mUserOrientation.tilt = newTilt;

                // Adjust each parameter to make sure we're pointing in a
                // reasonable direction. This clamps tilt on cylindrical
                // panoramas.
                doYaw(0.0f);
                doTilt(0.0f);
                doZoom(0.0f);

                updateRendererUserOrientation();
            }
        }

        @Override
        public void onAccuracyChanged(int sensor, int accuracy) {

        }

    };
//        Second, here's the code that takes the pan/tilt angles and sets
//        the OpenGL camera:

        protected void draw(GL10 gl, UserOrientation userOrientation) {
            // Adjust direction for the panorama yaw.
            // (We add 180.0f to convert from pointing at the edge of the
            // image to pointing at the center of the image.)
            float imageYawDeg =
                    StreetMath.wrap(userOrientation.yaw - mConfig.mPanoYawDeg
                            + 180.0f, StreetMath.D360);

            float pan = StreetMath.degreesToUnit(imageYawDeg);
            float tilt = userOrientation.tilt * 0.5f;
            float scale = StreetMath.exp2(-userOrientation.zoom);
            float cameraAngleY = scale * 0.125f; // Default 45 degrees
            float cameraAngleX = cameraAngleY * mAspectRatio;

            // To draw a tiled image map, we first point the camera
            // at the portion of the map we want to view, then we draw the
            // map. The map knows to read the camera, extract the frustum,
            // and clip itself against the frustum.

            // Implement zooming by adjusting the field-of-view.

            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();

            float fovYDeg = GLSurfaceView.Renderer.getUnzoomedVerticalFov(mAspectRatio) * scale;
            GLU.gluPerspective(gl, fovYDeg, mAspectRatio, 0.1f, 4.0f);

            float[] pt = new float[3];
            computePoint(pan, tilt, pt, 0);

            float[] up = new float[3];
            computePoint(pan, tilt + 0.25f, up, 0);

            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            GLU.gluLookAt(gl, 0.0f, 0.0f, 0.0f, pt[0], pt[1], pt[2],
                    up[0], up[1], up[2]);

            // A camera angle of 0.5 would see the whole image.
            float viewScale = cameraAngleY * 2.0f;

            drawTiles(gl, cameraAngleX, cameraAngleY, mViewHeight, viewScale,
                    ZOOM_LEVEL_BIAS, userOrientation.zoom);
        }

        public static void computePoint(float unitAngleX, float unitAngleY,
                                        float[] result, int resultOffset) {
            float sinX = StreetMath.sinUnit(unitAngleX);
            float cosX = StreetMath.cosUnit(unitAngleX);
            float sinY = StreetMath.sinUnit(unitAngleY);
            float cosY = StreetMath.cosUnit(unitAngleY);

            float x = -sinY * sinX;
            float z = sinY * cosX;
            float y = -cosY;

            result[resultOffset] = x;
            result[resultOffset + 1] = y;
            result[resultOffset + 2] = z;
        }
    */
}
