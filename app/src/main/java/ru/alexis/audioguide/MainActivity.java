package ru.alexis.audioguide;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

import ru.alexis.audioguide.ch4ardemo.AHActivity;
import ru.alexis.audioguide.fortest.CameraWithSurfaceActivity;
import ru.alexis.audioguide.fortest.SurfaceActivity;
import ru.alexis.audioguide.permission.CameraPermissionHelper;
import ru.alexis.audioguide.services.MediaService;
import ru.alexis.audioguide.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private MediaService mediaService;
    private MediaPlayer mMediaPlayerLocal;
    private Intent mediaIntent;
    private boolean isBound;
    boolean isPlaying;
    private MediaService.MediaBinder mediaBinder;
    private Handler audioProgressUpdateHandler;
    private NotificationCompat.Builder mBuilder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindAudioService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("SetTextI18n")
    public void onClick(View view) {
        Intent intent = null;

        switch (view.getId()){
            case R.id.button:
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }
                intent = new Intent(this, CameraWithPlacesActivity.class);
                break;
            case R.id.btn_show_gyroscope:
                intent = new Intent(this, GyroscopeActivity.class);
                break;
            case R.id.btn_show_camera_surface:
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }
                intent = new Intent(this, CameraWithSurfaceActivity.class);
                break;
            case R.id.btn_show_surface:
                intent = new Intent(this, SurfaceActivity.class);
                break;
            case R.id.btn_show_ah:
                intent = new Intent(this, AHActivity.class);
                break;
            case R.id.btn_play_audio:
                playAudio("О Петербурге.aac");
                break;
            case R.id.btn_stop_audio:
                stopAudio();
                break;
            case R.id.btn_play_service:
                playAudioService();
                break;
            case R.id.btn_stop_service:
                stopAudioService();
                break;
        }
        if(intent != null){
            startActivity(intent);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: ");
    }

    @SuppressLint("SetTextI18n")
    public void playAudio(String fileName){
        Log.d(TAG, "audioPlayer: " + ((mMediaPlayerLocal !=null)? mMediaPlayerLocal.isPlaying():""));
        if(mMediaPlayerLocal != null && isPlaying){
            mMediaPlayerLocal.pause();
            isPlaying = false;
            ((Button)findViewById(R.id.btn_play_audio)).setText("Play");
            return;

        }else if(mMediaPlayerLocal != null){
            mMediaPlayerLocal.start();
            isPlaying = true;
            ((Button)findViewById(R.id.btn_play_audio)).setText("Pause");
            return;
        }

        try {
            mMediaPlayerLocal = new MediaPlayer();
            AssetFileDescriptor afd = getAssets().openFd(fileName);
            mMediaPlayerLocal.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            Log.e(TAG, "audioPlayer: error open file " + fileName + "\n" + e.getMessage());
        }

        mMediaPlayerLocal.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        try {
            mMediaPlayerLocal.prepare(); // might take long! (for buffering, etc)
            mMediaPlayerLocal.start();
            isPlaying = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAudio() {
        ((Button)findViewById(R.id.btn_play_audio)).setText("Play - О Петербурге");
        if(mMediaPlayerLocal != null){
            mMediaPlayerLocal.release();
        }
        isPlaying = false;
        mMediaPlayerLocal = null;
    }

    private ServiceConnection mediaConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            mediaBinder = (MediaService.MediaBinder) iBinder;
            mediaService = mediaBinder.getService();
            Log.d(TAG, "onServiceConnected: mMediaBound");
            isBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected: ");
            isBound = false;
        }
    };

    // Create audio player progressbar updater.
    // This updater is used to update progressbar to reflect audio play process.
    @SuppressLint("HandlerLeak")
    private void createAudioProgressbarUpdater(){
        /* Initialize audio progress handler. */
        if(audioProgressUpdateHandler == null) {
            Log.d(TAG, "createAudioProgressbarUpdater: ");
            audioProgressUpdateHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    // The update process message is sent from AudioServiceBinder class's thread object.
                    if (msg.what == mediaBinder.getService().UPDATE_AUDIO_PROGRESS_BAR) {

                        if( mediaBinder != null) {
                            // Calculate the percentage.
                            int currProgress = mediaBinder.getService().getAudioProgress();

                            // Update progressbar. Make the value 10 times to show more clear UI change.
                            mediaBinder.getService().setProgress(currProgress);
                            //Log.d(TAG, "handleMessage: progress=" + currProgress);
                            if(mBuilder != null) {
                                Log.d(TAG, "handleMessage: mBuilder " + currProgress);
                                try {
                                    mBuilder.setProgress(100, currProgress, false);
                                    NotificationManagerCompat.from(getApplicationContext()).notify(1, mBuilder.build());
                                }catch (Exception e){
                                    Log.e(TAG, e.getMessage());
                                }
                            }
                        }
                    }else if(msg.what == mediaBinder.getService().STOP_AUDIO_PROGRESS_BAR){

                    }
                }
            };
        }
    }

    private void buildNotify(){
        Intent intentMain = new Intent(this , MainActivity.class);
        intentMain.setFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0 , intentMain, 0);
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        if(mBuilder == null) {
            mBuilder = new NotificationCompat.Builder(this, "123")
                    .setSmallIcon(R.drawable.ic_play_circle_filled)
                    .setContentTitle("Play audio")
                    .setContentText("О Петербурге...")
                    .setProgress(100, 0, false)
//                    .setStyle(new NotificationCompat.BigTextStyle()
//                            .bigText("Much longer text that cannot fit one line..."))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)

                    .setContentIntent(pendingIntent);
//        mBuilder.build();
            notificationManagerCompat.notify(1, mBuilder.build());
        }
    }

    public void playAudioService(){
        // Set application context.
        if(mBuilder == null){
            //buildNotify();
        }
        if(isPlaying) {
            ((Button) findViewById(R.id.btn_play_service)).setText("Play");
            mediaBinder.getService().handleActionPause();
        }else {
            buildNotify();
            mediaBinder.getService().setContext(getApplicationContext());
            createAudioProgressbarUpdater();
            mediaBinder.getService().setAudioProgressUpdateHandler(audioProgressUpdateHandler);
            mediaBinder.getService().setTrack("О Петербурге.aac");
            ((Button) findViewById(R.id.btn_play_service)).setText("Pause");
            mediaBinder.getService().handleActionPlay("О Петербурге.aac");
        }

        isPlaying = !isPlaying;
    }

    private void stopAudioService() {
        unBindAudioService();
    }

    public void bindAudioService(){
        if(mediaBinder == null) {
            Log.d(TAG, "bindAudioService: ");
            mediaIntent = new Intent(getApplicationContext(), MediaService.class);
            bindService(mediaIntent, mediaConnection, BIND_AUTO_CREATE);
        }
    }

    public void unBindAudioService(){
        isPlaying = false;
        if(mBuilder != null){
            Log.d(TAG, "unBindAudioService: cancel notify");
            NotificationManagerCompat.from(this).cancel(1);
        }
        try {
            if (isBound) {
                isBound = false;
                unbindService(mediaConnection);
            }
        }catch (Exception e){
            Log.e(TAG, "unBindAudioService: ", e.getCause());
        }
    }

    @Override
    protected void onDestroy() {
        unBindAudioService();
        super.onDestroy();
    }

}
