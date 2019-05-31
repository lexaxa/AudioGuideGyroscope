package ru.alexis.audioguide.fortest;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.media.MediaPlayer;
import android.support.annotation.Nullable;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
@Deprecated
public class MediaService extends IntentService implements MediaPlayer.OnPreparedListener{
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_PLAY = "ru.alexis.audioguide.action.PLAY";
    private static final String ACTION_PAUSE = "ru.alexis.audioguide.action.PAUSE";
    private static final String ACTION_STOP = "ru.alexis.audioguide.action.STOP";

    // TODO: Rename parameters
    private static final String EXTRA_DESCRIPTION = "ru.alexis.audioguide.extra.DESCRIPTION";
    private static final String EXTRA_FILENAME = "ru.alexis.audioguide.extra.FILENAME";

    MediaPlayer mMediaPlayer = null;

    public MediaService() {
        super("MediaService");
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if(intent.getAction().equals(ACTION_PLAY)){
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnPreparedListener(this);
//            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.prepareAsync();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void onPrepared(MediaPlayer player){
        player.start();
    }
    /**
     * Starts this service to perform action Play with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionPlay(Context context, String param1, String param2) {
        Intent intent = new Intent(context, MediaService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_DESCRIPTION, param1);
        intent.putExtra(EXTRA_FILENAME, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Pause with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionPause(Context context, String param1, String param2) {
        Intent intent = new Intent(context, MediaService.class);
        intent.setAction(ACTION_PAUSE);
        intent.putExtra(EXTRA_DESCRIPTION, param1);
        intent.putExtra(EXTRA_FILENAME, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Pause with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionStop(Context context, String param1, String param2) {
        Intent intent = new Intent(context, MediaService.class);
        intent.setAction(ACTION_STOP);
        intent.putExtra(EXTRA_DESCRIPTION, param1);
        intent.putExtra(EXTRA_FILENAME, param2);
        context.startService(intent);
    }

//    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // ... react appropriately ...
        // The MediaPlayer has moved to the Error state, must be reset!
        return false;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_PLAY.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_DESCRIPTION);
                final String param2 = intent.getStringExtra(EXTRA_FILENAME);
                handleActionPlay(param1, param2);
            } else if (ACTION_PAUSE.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_DESCRIPTION);
                final String param2 = intent.getStringExtra(EXTRA_FILENAME);
                handleActionPause(param1, param2);
            } else if (ACTION_STOP.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_DESCRIPTION);
                final String param2 = intent.getStringExtra(EXTRA_FILENAME);
                handleActionStop(param1, param2);
            }
        }
    }

    /**
     * Handle action Play in the provided background thread with the provided
     * parameters.
     */
    private void handleActionPlay(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Pause in the provided background thread with the provided
     * parameters.
     */
    private void handleActionPause(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Stop in the provided background thread with the provided
     * parameters.
     */
    private void handleActionStop(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
