package ru.alexis.audioguide.fortest;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import java.io.IOException;

/**
 * Created by Jerry on 2/15/2018.
 */

public class AudioServiceBinder extends Binder {

    // Save local audio file uri ( local storage file. ).
    private Uri audioFileUri = null;

    // Save web audio file url.
    private String audioFileUrl = "";

    // Check if stream audio.
    private boolean streamAudio = false;

    // Media player that play audio.
    private MediaPlayer audioPlayer = null;

    // Caller activity context, used when play local audio file.
    private Context context = null;

    // This Handler object is a reference to the caller activity's Handler.
    // In the caller activity's handler, it will update the audio play progress.
    private Handler audioProgressUpdateHandler;

    // This is the message signal that inform audio progress updater to update audio progress.
     final int UPDATE_AUDIO_PROGRESS_BAR = 1;

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    private String getAudioFileUrl() {
        return audioFileUrl;
    }

     void setAudioFileUrl(String audioFileUrl) {
        this.audioFileUrl = audioFileUrl;
    }

    private boolean isStreamAudio() {
        return streamAudio;
    }

     void setStreamAudio(boolean streamAudio) {
        this.streamAudio = streamAudio;
    }

    private Uri getAudioFileUri() {
        return audioFileUri;
    }

    // Start play audio.
     void startAudio(){
        initAudioPlayer();
        if(audioPlayer!=null) {
            audioPlayer.start();
        }
    }

    // Pause playing audio.
     void pauseAudio(){
        if(audioPlayer!=null) {
            audioPlayer.pause();
        }
    }

    // Stop play audio.
     void stopAudio(){
        if(audioPlayer!=null) {
            audioPlayer.stop();
            destroyAudioPlayer();
        }
    }

     void setAudioProgressUpdateHandler(Handler audioProgressUpdateHandler) {
        this.audioProgressUpdateHandler = audioProgressUpdateHandler;
    }

    // Initialise audio player.
    private void initAudioPlayer(){
        try {
            if (audioPlayer == null) {
                audioPlayer = new MediaPlayer();

                if (!TextUtils.isEmpty(getAudioFileUrl())) {
                    if (isStreamAudio()) {
                        audioPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    }
                    audioPlayer.setDataSource(getAudioFileUrl());
                } else {
                    audioPlayer.setDataSource(getContext(), getAudioFileUri());
                }

                audioPlayer.prepare();

                // This thread object will send update audio progress message to caller activity every 1 second.
                Thread updateAudioProgressThread = new Thread()
                {
                    @Override
                    public void run() {
                        while(true){
                            // Create update audio progress message.
                            Message updateAudioProgressMsg = new Message();
                            updateAudioProgressMsg.what = UPDATE_AUDIO_PROGRESS_BAR;

                            // Send the message to caller activity's update audio prgressbar Handler object.
                            audioProgressUpdateHandler.sendMessage(updateAudioProgressMsg);

                            // Sleep one second.
                            try {
                                Thread.sleep(1000);
                            }catch(InterruptedException ex){
                                ex.printStackTrace();
                            }
                        }
                    }
                };
                // Run above thread object.
                updateAudioProgressThread.start();
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    // Destroy audio player.
    private void destroyAudioPlayer(){
        if(audioPlayer!=null)
        {
            if(audioPlayer.isPlaying())
            {
                audioPlayer.stop();
            }

            audioPlayer.release();

            audioPlayer = null;
        }
    }

    // Return current audio play position.
    private int getCurrentAudioPosition(){
        int ret = 0;
        if(audioPlayer != null)
        {
            ret = audioPlayer.getCurrentPosition();
        }
        return ret;
    }

    // Return total audio file duration.
    private int getTotalAudioDuration(){
        int ret = 0;
        if(audioPlayer != null)
        {
            ret = audioPlayer.getDuration();
        }
        return ret;
    }

    // Return current audio player progress value.
     int getAudioProgress(){
        int ret = 0;
        int currAudioPosition = getCurrentAudioPosition();
        int totalAudioDuration = getTotalAudioDuration();
        if(totalAudioDuration > 0) {
            ret = (currAudioPosition * 100) / totalAudioDuration;
        }
        return ret;
    }
}