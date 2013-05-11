package com.example.videokitsample;

import com.avos.minute.recorder.*;
import com.avos.minute.util.*;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static int LONG_PRESS_TIME = 200; // Time in miliseconds

    private LinearLayout mPortraitOverlay;
    private FrameLayout mPreviewFrame;
    private FrameLayout mVideoPlayFrame;

    private FrameLayout mVideoPreviewOverlay;
    private FrameLayout mPreviewOverlay;

    private int mOrientation = -1;
    private OrientationEventListener mOrientationEventListener;

    final Handler _handler = new Handler();
    Runnable _longPressed = new Runnable() {
        public void run() {
            Log.d(TAG, "start recording...");
            mRecManager.startRecording();
        }
    };

    private RecordingManager mRecManager;
    private String videoFilepath = "";
    private String snapshotFilepath = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPortraitOverlay = (LinearLayout) findViewById(R.id.camera_portrait_overlay);
        mVideoPlayFrame = (FrameLayout) findViewById(R.id.video_view);
        mVideoPreviewOverlay = (FrameLayout) findViewById(R.id.video_preview_overlay);
        changePreviewOverlayHeight(mVideoPreviewOverlay);
        mPreviewFrame = (FrameLayout) findViewById(R.id.camera_preview_frame);
        mPreviewFrame.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                case (MotionEvent.ACTION_DOWN):
                    Log.d(TAG, "Action was DOWN");
                    Log.d(TAG, String.format("x:%g y:%g", event.getX(), event.getY()));
                    //_handler.postDelayed(_longPressed, LONG_PRESS_TIME);
                    mRecManager.startRecording();
                    return true;
                case (MotionEvent.ACTION_MOVE):
                    return true;
                case (MotionEvent.ACTION_UP):
                case (MotionEvent.ACTION_CANCEL):
                    Log.d(TAG, "Action was UP or CANCEL");
                    //_handler.removeCallbacks(_longPressed);
                    mRecManager.stopRecording();
                    return true;
                case (MotionEvent.ACTION_OUTSIDE):
                    return true;
                default:
                    return false;
                }
            }
        });

        mPreviewOverlay = (FrameLayout) findViewById(R.id.camera_preview_overlay);
        changePreviewOverlayHeight(mPreviewOverlay);

        mOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }
                Log.d(TAG, "set Orientation: " + orientation);
                mRecManager.setOrientation(orientation);

                if (mOrientation == -1) {
                    mOrientation = orientation;
                }

                int prevOrientation = (mOrientation + 45) % 180 / 90 * 90;
                int curOrientation = (orientation + 45) % 180 / 90 * 90;

                mOrientation = orientation;
            }
        };

        Button playButton = (Button) findViewById(R.id.camera_play_button);
        playButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mOrientationEventListener.disable();
                mPreviewFrame.setVisibility(View.GONE);
                mPortraitOverlay.setVisibility(View.GONE);
                mVideoPlayFrame.setVisibility(View.VISIBLE);
                VideoView videoView = (VideoView) findViewById(R.id.video_play_view);
                videoView.setVideoURI(Uri.parse(videoFilepath));
                videoView.setVisibility(View.VISIBLE);
                videoView.start();
            }
        });
        Button shareButton = (Button) findViewById(R.id.video_share_button);
        shareButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "begin to crop video");
                VideoEngine engine = new VideoEngine();
                engine.crop(videoFilepath, Utils.getFinalVideoPath(), 480, 480);
                Log.d(TAG, "end to crop video");
                Toast.makeText(MainActivity.this, "Video process successed.", Toast.LENGTH_LONG).show();
            }
        });
//        mPlayImage = (ImageView) findViewById(R.id.camera_project_play);
//        mPlayImage.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Intent intent = new Intent(MainActivity.this, ClipPlayActivity.class);
////                startActivity(intent);
//            }
//        });
//
//        mCloseImage = (ImageView) findViewById(R.id.camera_close_img);
//        mCloseImage.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
////                Intent intent = new Intent(MainActivity.this, ProjectsActivity.class);
////                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
////                startActivity(intent);
//            }
//        });
        this.mRecManager = RecordingManager.getInstance(MainActivity.this, mPreviewFrame);
        videoFilepath = Utils.getNextVideoPath();
        snapshotFilepath = Utils.getNextSnapshotPath();
        mRecManager.init(videoFilepath, snapshotFilepath);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }
    
    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();

        // Initialize the default project before init the RecordingManager
        if (mRecManager != null) {
            mRecManager.onResume();
        }

        mRecManager.setOrientationEventListener(mOrientationEventListener);
        mOrientationEventListener.enable();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        if (mRecManager != null) {
            mRecManager.onPause();
        }
        super.onPause();
        mOrientationEventListener.disable();
    }

    @Override
    protected void onStart() {
        Log.v(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        super.onDestroy();
    }

    public int getOrientation() {
        return mOrientation;
    }
    public OrientationEventListener getOrientationEventListener() {
        return mOrientationEventListener;
    }

    /**
     * Adjust the overlay height depends on pixel density to show the camera
     * view in correct ratio.
     */
    private void changePreviewOverlayHeight(FrameLayout frameLayout) {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        LinearLayout.LayoutParams linearLayoutParams;
        linearLayoutParams = (LinearLayout.LayoutParams) frameLayout.getLayoutParams();
        linearLayoutParams.height = width;
        frameLayout.setLayoutParams(linearLayoutParams);
    }
}
