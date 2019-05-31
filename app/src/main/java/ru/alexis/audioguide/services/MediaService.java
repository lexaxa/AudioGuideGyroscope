package ru.alexis.audioguide.services;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.alexis.audioguide.MainActivity;
import ru.alexis.audioguide.R;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * helper methods.
 */
public class MediaService extends IntentService implements MediaPlayer.OnPreparedListener{

    private static final String TAG = MediaService.class.getSimpleName();

    public static final String EXTRA_DESCRIPTION = "ru.alexis.audioguide.extra.DESCRIPTION";
    public static final String EXTRA_FILENAME = "ru.alexis.audioguide.extra.FILENAME";
    private static final int TIME_TO_SEEK = 5 * 1000; // 5 sec
    public static final int UPDATE_AUDIO_PROGRESS_BAR = 1;
    public static final int STOP_AUDIO_PROGRESS_BAR = 2;

    private final IBinder mediaBinder = new MediaBinder();
    private MediaPlayer mMediaPlayer = null;
    boolean isPlaying;
    AtomicBoolean isPlayingA = new AtomicBoolean(false);
    private String trackName;
    private Handler audioProgressUpdateHandler;
    private int progress;
    // Caller activity context, used when play local audio file.
    private Context context = null;
    private Thread updateAudioProgressThread;


    public MediaService() {
        super("MediaService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
//        initMusicPlayer();
    }

    @Override
    public void onDestroy() {
        Log.d("TAG", "onDestroy: ");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        super.onDestroy();
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public class MediaBinder extends Binder{
        public MediaService getService(){
            return MediaService.this;
        }
    }

    @Override
    public boolean bindService(Intent service, ServiceConnection conn, int flags) {
        Log.d(TAG, "bindService: ");
        return super.bindService(service, conn, flags);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: ");
        return mediaBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.d(TAG, "onHandleIntent: ");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind: ");
        if(mMediaPlayer != null) {
            if(mMediaPlayer.isPlaying()){
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
        }
        isPlaying = false;
        isPlayingA.set(false);
        return true;
    }

    public void setAudioProgressUpdateHandler(Handler audioProgressUpdateHandler) {
        this.audioProgressUpdateHandler = audioProgressUpdateHandler;
    }

    public void initMusicPlayer(){
        mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK );
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setLooping(false);
    }

    @Override
    public void onPrepared(MediaPlayer player){
        Log.d(TAG, "onPrepared: player");
        player.start();
        isPlaying = true;
        isPlayingA.set(true);
    }

    public void setTrack(String trackName) {
        this.trackName = trackName;
    }

    public void initPlayer(){
        Log.d(TAG, "initPlayer: " + trackName);

        if(mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Log.d(TAG, "onCompletion: ");
//                isPlaying = false;
                }
            });
        }
        if(trackName == null) return;

        try {
            AssetManager am = context.getAssets();
            AssetFileDescriptor afd = am.openFd(trackName);
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (IOException e) {
            Log.e(TAG, "initPlayer: error open file " + trackName + "\n" + e.getMessage());
        }
        mMediaPlayer.prepareAsync();

        // This thread object will send update audio progress message to caller activity every 1 second.
        // Create update audio progress message.
// Send the message to caller activity's update audio progressbar Handler object.
// Sleep one second.
        if (updateAudioProgressThread == null) {
            updateAudioProgressThread = new Thread() {
                @Override
                public void run() {
                    while (true) {
                        if (isPlayingA.get()) {
                            Log.d(TAG, "run: " + isPlaying + "=" + isPlayingA.get());
                            // Create update audio progress message.
                            Message updateAudioProgressMsg = new Message();
                            updateAudioProgressMsg.what = UPDATE_AUDIO_PROGRESS_BAR;

                            // Send the message to caller activity's update audio progressbar Handler object.
                            audioProgressUpdateHandler.sendMessage(updateAudioProgressMsg);
                        }

                        // Sleep one second.
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }

                    }
                }
            };
            // Run above thread object.
            updateAudioProgressThread.start();
        }
    }


    /**
     * Handle action Play in the provided background thread with the provided
     * parameters.
     */
    public void handleActionPlay(String fileName) {

        Log.d(TAG, "handleActionPlay: " + ((mMediaPlayer!=null)?mMediaPlayer.isPlaying():""));

        if(trackName == null || mMediaPlayer == null || !TextUtils.equals(trackName, fileName)){
            Log.d(TAG, "audioPlayer: stop play old file");
            handleActionStop();
            setTrack(fileName);
            initPlayer();
        }else{
            Log.d(TAG, "handleActionPlay: start");
            mMediaPlayer.start();
            isPlaying = true;
            isPlayingA.set(true);
        }
    }
    // Pause audio
    public void handleActionPause(){
        Log.d(TAG, "handleActionPause: ");
        if(mMediaPlayer != null){
            mMediaPlayer.pause();
        }
        isPlaying = false;
        isPlayingA.set(false);
    }
    // Stop play audio
    private void handleActionStop() {
        Log.d(TAG, "handleActionStop: ");
        if(mMediaPlayer != null) {
            mMediaPlayer.stop();
        }
        isPlaying = false;
    }

    // Shift to prev or next time
    public void handleActionSeek(int time) {
        if(mMediaPlayer.isPlaying()) {
            mMediaPlayer.seekTo(mMediaPlayer.getCurrentPosition() + time);
        }
    }

    // Return current audio player progress value.
    public int getAudioProgress(){
        if(mMediaPlayer == null || !isPlaying()) return 0;

        int progress = 0;
        int curPos = mMediaPlayer.getCurrentPosition();
        int duration = mMediaPlayer.getDuration();
        if(duration > 0) {
            progress = (curPos * 100) / duration;
        }
//        Log.d(TAG, "getAudioProgress: " + progress);
        return progress;
    }

    public boolean isPlaying() {
        return isPlaying;
    }
}
