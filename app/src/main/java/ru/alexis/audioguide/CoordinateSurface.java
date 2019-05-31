package ru.alexis.audioguide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;

import ru.alexis.audioguide.model.Place;
import ru.alexis.audioguide.utils.AudioPlayer;

public class CoordinateSurface extends SurfaceView implements SurfaceHolder.Callback, SurfaceView.OnTouchListener{

    private static final String TAG = CoordinateSurface.class.getSimpleName();
    public static final int SPACE_BETWEEN_ICON = 10;
    public static final int SIZE_SMALL_ICON = 50;
    public static final int SCALE_SMALL_ICON = 2;

    private DrawingThread drawingThread;
    SurfaceHolder surfaceHolder;

    float scale;
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
//    private boolean isShowDesc;
    private final Canvas descCanvas = new Canvas();

    public CoordinateSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "CoordinateSurface: context attrs");
        init();
    }

    public CoordinateSurface(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        Log.d(TAG, "CoordinateSurface: context attrs style");
        init();
    }

    public CoordinateSurface(Context context) {
        super(context);
        Log.d(TAG, "CoordinateSurface: context");
        init();
    }

    public DrawingThread getDrawingThread() {
        return drawingThread;
    }

    private void init() {
        surfaceHolder = getHolder();
        surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
        setOnTouchListener(this);
        surfaceHolder.addCallback(this);
        mTextPaint.setTextSize((int)(10 * scale));
        //mTextPaint.setARGB(120,200,200,100);
        mTextPaint.setColor(0xffffffff);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawingThread = new DrawingThread(getHolder(), BitmapFactory.decodeResource(
                getResources(), R.mipmap.ic_place));
        drawingThread.start();
        Log.d(TAG, "surfaceCreated: ");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int frmt, int w, int h) {
        scale = getContext().getResources().getDisplayMetrics().density;
        Log.d(TAG, "surfaceChanged: scale is " + scale);
        drawingThread.updateSize(w, h);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawingThread.quit();
        drawingThread = null;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int offsetX = drawingThread.mIcon.getWidth();
        int offsetY = drawingThread.mIcon.getHeight();
        Log.d(TAG, "onTouch: " + x + ", " + y + " : " + offsetX + ", " + offsetY);
        for (DrawingThread.DrawingItem item : drawingThread.getLocations()) {
            // click on place item
            if(x >= item.x - offsetX/2 && x <= item.x + offsetX/2 &&
               y >= item.y  && y <= item.y + offsetY){
                Log.d(TAG, "onTouch: Clicked " + item.place.getStreet());
//                item.isShowDesc = !item.isShowDesc;
                if(drawingThread.curShowItem == item) {
//                    AudioPlayer.getInstance().stopPlayer();
                    drawingThread.curShowItem = null;
                }else{
                    drawingThread.curShowItem = item;
                }
                drawingThread.mReceiver.sendEmptyMessage(DrawingThread.MSG_MOVE);
                break;
            }else if(x >= drawingThread.descRect.left + 100 + SPACE_BETWEEN_ICON * (0) && x <= drawingThread.descRect.left + 200 + SPACE_BETWEEN_ICON * (0) &&
                    y >= drawingThread.descRect.top - 100 && y <= drawingThread.descRect.top ){
//            }else if(x >= item.x - SIZE_SMALL_ICON && x <= item.x && y >= item.y + 100 + SPACE_BETWEEN_ICON * (0) && y <= item.y + 100 + SPACE_BETWEEN_ICON *(0) + SIZE_SMALL_ICON * 1 ){
                // click on left small place icon
                Log.d(TAG, "onTouch: Clicked small place icon " + item.place.getStreet());
                break;
            }else if(x >= drawingThread.descRect.left + 100*2 + SPACE_BETWEEN_ICON * (1) && x <= drawingThread.descRect.left + 300 + SPACE_BETWEEN_ICON * (1) &&
                    y >= drawingThread.descRect.top - 100 && y <= drawingThread.descRect.top ){
//            }else if(x >= item.x - SIZE_SMALL_ICON && x <= item.x && y >= item.y + 100 + SPACE_BETWEEN_ICON * (1) && y <= item.y + 100 + SPACE_BETWEEN_ICON *(1) + SIZE_SMALL_ICON * 2 ){
                // click on play icon
                Log.d(TAG, "onTouch: Clicked play icon " + item.place.getStreet());

                AudioPlayer.audioPlayer(drawingThread.curShowItem.place.getAudioFileName());
//                ((Button) getRootView().findViewById(R.id.btn_play_audio)).setText("||");
                break;
            }else if(false){
                // stub to handle next icons
                break;
            }
        }
        return false;
    }

    public class DrawingThread extends HandlerThread implements Handler.Callback {

        private SurfaceHolder drawingHolder;
        private Paint paint;
        private Bitmap mIcon;
        private ArrayList<DrawingItem> mLocations;

        private static final int MSG_ADD = 100;
        private static final int MSG_MOVE = 101;
        private static final int MSG_CLEAR = 102;
        private static final int MSG_CLEAR_ITEMS = 103;
        private Handler mReceiver;
        private final Paint mPaint = new Paint();
        private final Rect playrect = new Rect();
        private final Rect playrect2 = new Rect();
        private final Bitmap bmicplay = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_place);
        private Drawable icplay;
        private Drawable icpause;
        private DrawingItem curShowItem;
        private Rect descRect = new Rect();

        DrawingThread(SurfaceHolder holder, Bitmap icon) {
            super("Drawing thread");
            this.drawingHolder = holder;
            mLocations = new ArrayList<>();
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setFakeBoldText(false);
            paint.setStrokeWidth(2);
            paint.setColor(Color.WHITE);

            mPaint.setARGB(125,0,0,0);

            mIcon = icon;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                icplay = getResources().getDrawable(R.drawable.ic_play_circle_filled, null);
                icpause = getResources().getDrawable(R.drawable.ic_pause_circle_filled, null);
            }else{
                icplay = getResources().getDrawable(R.drawable.ic_pause_circle_filled);
                icpause = getResources().getDrawable(R.drawable.ic_play_circle_filled);
            }
        }

        @Override
        protected void onLooperPrepared() {
            mReceiver = new Handler(getLooper(), this);
            // Start the rendering
            mReceiver.sendEmptyMessage(MSG_MOVE);
        }

        @Override
        public boolean quit() {
            // Clear all messages before dying
            mReceiver.removeCallbacksAndMessages(null);
            return super.quit();
        }

        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ADD:
                    // Create a new item at the touch location,
                    // with a randomized start direction

                    DrawingItem newItem = new DrawingItem(msg.arg1, msg.arg2,(Place)msg.obj);
                    if(mLocations.size() < 20) {
                        mLocations.add(newItem);
                    }else{
                        Log.d(TAG, "handleMessage: MSG_ADD too may elements");
                    }
                    break;
                case MSG_CLEAR:
                    // Remove all messages
                    mLocations.clear();
                    break;
                case MSG_CLEAR_ITEMS:
                    // Remove all objects
                    mLocations.clear();
                    break;

                case MSG_MOVE:
                    // Render a frame
                    Canvas c = drawingHolder.lockCanvas();
                    if (c == null) {
                        break;
                    }
                    // Clear Canvas first
                    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    // TODO Check collision two near items and transform to one icon
                    // Draw each item
                    for (DrawingItem item : mLocations) {
                        // Update location
                        // Draw to the Canvas
                        c.drawBitmap(mIcon, item.x - mIcon.getWidth()/2, item.y, paint);
                        paint.setTextSize((int)(10 * scale));

                        c.save();
                        c.rotate(-30, item.x, item.y);
                        c.drawText(item.place.getStreet(), item.x, item.y, paint);
                        c.restore();
                    }
                    if(curShowItem != null){
                        drawDesc2(c, curShowItem);
                    }
                    // Release to be rendered to the screen
                    drawingHolder.unlockCanvasAndPost(c);
                    break;
            }
            // Post the next frame
//            mReceiver.sendEmptyMessage(MSG_MOVE);
            return true;
        }

        private void drawDesc(Canvas c, DrawingItem item) {

            String mText =
                    "Наименование: " + item.place.getStreet() + "\n" +
                    "Расстояние: " + "N/A" + "\n" +
                    "Дата постройки: " + "N/A" + "\n" +
                    "Координаты: " + item.place.getLatitude() + ", " + item.place.getLongitude() + "\n" +
                    "Описание: " + "N/A" + "\n";
            StaticLayout mTextLayout = new StaticLayout(mText, mTextPaint, 300, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
//                        int x = 100, y = 100;
//                        for (String line: mText.split("\n")) {
//                            canvas.drawText(line, x, y, mTextPaint);
//                            y += mTextPaint.descent() - mTextPaint.ascent();
//                        }

            c.save();
// calculate x and y position where your text will be placed

            c.translate(item.x, item.y + 100);
            c.drawRect(new Rect(0, 0, item.x + mTextLayout.getWidth(), item.y + mTextLayout.getHeight()), mPaint);

            playrect.set( - 50, 0,  0, 50);
            playrect2.set( - 50, 60,  0, 110);

            c.drawRect(playrect, mPaint);
            c.drawRect(playrect2, mPaint);
            c.drawBitmap(bmicplay, null, playrect, mPaint);

            if(AudioPlayer.getInstance().getIsPlaying()){
                c.drawBitmap(drawableToBitmap(icpause), null, playrect2, mPaint);
            }else {
                c.drawBitmap(drawableToBitmap(icplay), null, playrect2, mPaint);
            }

            mTextLayout.draw(c);
            c.restore();
        }

        private void drawDesc2(Canvas c, DrawingItem item) {

            descCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            String mText =
                    "Наименование: " + item.place.getStreet() + "\n" +
                    "Расстояние: " + "N/A" + "\n" +
                    "Дата постройки: " + "N/A" + "\n" +
                    "Координаты: " + item.place.getLatitude() + ", " + item.place.getLongitude() + "\n" +
                    "Описание: " + "N/A" + "\n";
//            StaticLayout mTextLayout = new StaticLayout(mText, mTextPaint, 300, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
//                        int x = 100, y = 100;
//                        for (String line: mText.split("\n")) {
//                            canvas.drawText(line, x, y, mTextPaint);
//                            y += mTextPaint.descent() - mTextPaint.ascent();
//                        }
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            // text color - #3D3D3D
            paint.setColor(Color.WHITE);
//            paint.setARGB(125,0,0,0);
            // text size in pixels
            paint.setTextSize((int) (15 * scale));
            // text shadow
            paint.setShadowLayer(1f, 0f, 1f, Color.RED);

            // draw text to the Canvas center
            Rect bounds = new Rect();

            int noOfLines = 0;
            for (String line: mText.split("\n")) {
                noOfLines++;
            }

            paint.getTextBounds(mText, 0, mText.length(), bounds);
            int x = 20;
            int y = (c.getHeight() - bounds.height()*noOfLines);

            Paint mPaint = new Paint();
            mPaint.setColor(getResources().getColor(R.color.transparentBlack));
            int left = 0;
            int top = (c.getHeight() - bounds.height()*(noOfLines+1));
            int right = c.getWidth();
            int bottom = c.getHeight();
            descRect = new Rect(left, top, right, bottom);
            c.drawRect(descRect, mPaint);
//            Bitmap descBitmap = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.ARGB_8888);

            for (String line: mText.split("\n")) {
                c.drawText(line, x, y, paint);
                y += paint.descent() - paint.ascent();
            }

            c.save();
// calculate x and y position where your text will be placed

            c.translate(left + 100, top - 100);
//            c.drawRect(new Rect(0, 0, item.x + mTextLayout.getWidth(), item.y + mTextLayout.getHeight()), paint);

            playrect.set( 0, 0,  100, 100);
            playrect2.set( 110, 0,  210, 100);

            c.drawRect(playrect, paint);
            c.drawRect(playrect2, paint);
            c.drawBitmap(bmicplay, null, playrect, paint);

            if(AudioPlayer.getInstance().getIsPlaying()){
                c.drawBitmap(drawableToBitmap(icpause), null, playrect2, mPaint);
            }else {
                c.drawBitmap(drawableToBitmap(icplay), null, playrect2, mPaint);
            }

//            mTextLayout.draw(c);
            c.restore();
//            isShowDesc = true;
        }

        Bitmap drawableToBitmap (Drawable drawable) {

            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable)drawable).getBitmap();
            }

            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }
        void updateSize(int width, int height) {
        }

        void drawMultiLineText(String str, float x, float y, Paint paint, Canvas canvas) {
            String[] lines = str.split("\n");
            float txtSize = -paint.ascent() + paint.descent();

            if (paint.getStyle() == Paint.Style.FILL_AND_STROKE || paint.getStyle() == Paint.Style.STROKE){
                txtSize += paint.getStrokeWidth(); //add stroke width to the text size
            }
            float lineSpace = txtSize * 0.2f;  //default line spacing

            for (int i = 0; i < lines.length; ++i) {
                canvas.drawText(lines[i], x, y + (txtSize + lineSpace) * i, paint);
            }
        }

        public void addItem(int x, int y, Place place) {
            // Pass the location into the Handler using Message arguments
            Message msg = Message.obtain(mReceiver, MSG_ADD, x, y, place);
            mReceiver.sendMessage(msg);
        }
        public void moveItems(){
            mReceiver.sendEmptyMessage(MSG_MOVE);
        }
        public void clearMessages(){
            mReceiver.sendEmptyMessage(MSG_CLEAR);
        }
        public void clearItems() {
            mReceiver.sendEmptyMessage(MSG_CLEAR_ITEMS);
        }

        ArrayList<DrawingItem> getLocations() {
            return mLocations;
        }

        class DrawingItem {
            // Current location marker
            // TODO Add list with left icons and its orders
            int x, y;
            int distance;
            Place place;
//            private boolean isShowDesc;

            DrawingItem(int x, int y, Place place) {
                this.x = x;
                this.y = y;
                this.place = place;
            }
        }
    }
}
