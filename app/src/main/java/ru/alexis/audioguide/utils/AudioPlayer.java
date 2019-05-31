package ru.alexis.audioguide.utils;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;

import ru.alexis.audioguide.R;

public class AudioPlayer {

    private static volatile AudioPlayer instance;
    private static MediaPlayer mp;
    private static boolean isPlaying;
    private WeakReference<Context> context = null;
    private static final String TAG = AudioPlayer.class.getSimpleName();
    private String curPlayingFileName;

    private AudioPlayer(){}

    public static AudioPlayer getInstance() {
        AudioPlayer linst = instance;
        if(linst == null){
            synchronized (AudioPlayer.class) {
                linst = instance;
                if (linst == null) {
                    instance = linst = new AudioPlayer();
                }
            }
        }
        return linst;
    }
    public boolean getIsPlaying(){
        return isPlaying;
    }
    public Context getContext(){
        return context.get();
    }
    public void setContext(Context cntx){
        context = new WeakReference<>(cntx);
    }

    public static void audioPlayer(String fileName){
//        @TODO Move MediaPlayer to MediaBrowserServiceCompat
        //set up MediaPlayer
        Log.d(TAG, "audioPlayer: " + ((mp!=null)?mp.isPlaying():"init") + ", " + fileName);
        if(!fileName.equalsIgnoreCase(getInstance().curPlayingFileName)){
            Log.d(TAG, "audioPlayer: stop play old file " + getInstance().curPlayingFileName);
            getInstance().stopPlayer();
        }else if(mp != null && isPlaying){
            mp.pause();
            isPlaying = false;
            return;
        }else if(mp != null){
            mp.start();
            isPlaying = true;
            getInstance().curPlayingFileName = fileName;
            return;
        }
        if(fileName.equalsIgnoreCase("")){
            return;
        }
        try {
            mp = new MediaPlayer();
            AssetFileDescriptor afd = getInstance().getContext().getAssets().openFd(fileName);
            mp.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } catch (Exception e) {
            Log.e(TAG, "audioPlayer: error open file " + fileName + "\n" + e.getMessage());
        }

//        mp = MediaPlayer.create(getInstance().context.get(), R.raw.big_ermitage);
        Log.d(TAG, "audioPlayer: 3");
        mp.setWakeMode(getInstance().context.get(), PowerManager.PARTIAL_WAKE_LOCK);
//        WifiLock wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE))
//                .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");
//        wifiLock.acquire();
//        // onPause or onStop media
//        wifiLock.release()
//        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//            @Override
//            public void onPrepared(MediaPlayer player) {
//                Log.d(TAG, "onPrepared: player");
//                mp.start();
//
//                isPlaying = true;
////                btnPlay.setText("Pause");
//            }
//        });
        try {
            String url = "http://privatsklad.ru/audioguide.nsf/";
//            mp.setDataSource(path + File.separator + fileName);
//            mp.setDataSource(url + fileName);
            //mp.setDataSource(this, R.raw.big_ermitage); //Uri.parse("android.resource://ru.alexis.audioguide/res/raw/big_ermitage"));
//            mp.prepareAsync();
            mp.prepare(); // might take long! (for buffering, etc)
            mp.start();
            isPlaying = true;
            getInstance().curPlayingFileName = fileName;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playAudio(){
        if(mp != null && isPlaying){
            mp.pause();
            isPlaying = false;
        }else if(mp != null){
            mp.start();
            isPlaying = true;
        }
    }

    public void stopPlayer() {
        isPlaying = false;
        if(mp != null){
            mp.release();
        }
        mp = null;
    }

}
