package com.avos.minute;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.avos.minute.recorder.*;
import com.avos.minute.util.*;
import com.example.videokitsample.R;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.ProgressBar;
import android.widget.LinearLayout;

public class FullscreenShootActivity extends Activity {
    private static final String TAG = FullscreenShootActivity.class.getSimpleName();

    private static int LONG_PRESS_TIME = 200; // Time in miliseconds
    private long tsCanRecord = 6000;
    private static final String TASK_UPDATE_RECORD_PROGRESS = "record_progress";

    private FrameLayout cameraPreviewFrame;
    private LinearLayout portraitOverlay;
    private VideoView videoView;
    private FrameLayout videoPreviewOverlay;
    private Button nextButton;
    private ProgressBar progressBar;
    private int mOrientation = -1;
    private OrientationEventListener mOrientationEventListener;

    private RecordingManager mRecManager;
    private String videoFilepath = "";
    private String snapshotFilepath = "";

    private enum internalStatus {
        Recroding,
        Previewing,
    };
    private internalStatus status;
    private ProgressCounter progressCounter = null;
    
    private int fileSeq = -1;
    private String videoTempDir = null;
    final Handler _handler = new Handler();
    Runnable _longPressed = new Runnable() {
        public void run() {
            String videoFile = nextVideoFilepath();
            Log.d(TAG, "start recording (file=" + videoFile + ")...");
            mRecManager.startRecording(videoFile);
            progressCounter = new ProgressCounter(tsCanRecord, 60);
            progressCounter.start();
        }
    };

    private String nextVideoFilepath() {
        if (videoTempDir == null) {
            File mediaStorageDir = new File(Environment
                    .getExternalStorageDirectory().getPath(), "wanpai");
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            fileSeq = 1;
            videoTempDir = mediaStorageDir + File.separator + "TEMP_" + timeStamp;
            File dir = new File(videoTempDir);
            dir.mkdirs();
        }
        fileSeq++;
        return videoTempDir + File.separator + fileSeq + ".mp4";
    }

    private String getFinalVideoFilepath() {
        if (videoTempDir == null) {
            File mediaStorageDir = new File(Environment
                    .getExternalStorageDirectory().getPath(), "wanpai");
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());
            fileSeq = 1;
            videoTempDir = mediaStorageDir + File.separator + "TEMP_" + timeStamp;
            File dir = new File(videoTempDir);
            dir.mkdirs();
        }
        return videoTempDir + "-merged.mp4";        
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoot);
        this.cameraPreviewFrame = (FrameLayout) findViewById(R.id.camera_preview_frame);
        this.videoView = (VideoView) findViewById(R.id.video_preview_frame);
        this.videoPreviewOverlay = (FrameLayout) findViewById(R.id.video_preview_overlay);
        changePreviewOverlayHeight(this.videoPreviewOverlay);
        cameraPreviewFrame.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                case (MotionEvent.ACTION_DOWN):
                    Log.d(TAG, "Action was DOWN " + String.format("x:%g y:%g", event.getX(), event.getY()));
                    _handler.postDelayed(_longPressed, LONG_PRESS_TIME);
                    return true;
                case (MotionEvent.ACTION_MOVE):
                    return true;
                case (MotionEvent.ACTION_UP):
                case (MotionEvent.ACTION_CANCEL):
                    Log.d(TAG, "Action was UP or CANCEL");
                    _handler.removeCallbacks(_longPressed);
                    mRecManager.stopRecording();
                    if (null != progressCounter) {
                        progressCounter.cancel();
                    }
                    return true;
                case (MotionEvent.ACTION_OUTSIDE):
                    return true;
                default:
                    return false;
                }
            }
        });
        this.portraitOverlay = (LinearLayout) findViewById(R.id.camera_portrait_overlay);
        this.progressBar = (ProgressBar) findViewById(R.id.progressBar);

        mOrientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return;
                }
//                Log.d(TAG, "set Orientation: " + orientation);
//                mRecManager.setOrientation(orientation);
                mOrientation = orientation;
            }
        };

        status = internalStatus.Recroding;
        nextButton = (Button) findViewById(R.id.next_button);
        nextButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mOrientationEventListener.disable();
                if (internalStatus.Recroding == status) {
                    //videoView
                    new VideoMergeTask().execute((Void) null);
                } else if (internalStatus.Previewing == status) {
                    Log.d(TAG, "begin to crop video");
                    VideoEngine engine = new VideoEngine();
                    engine.crop(videoFilepath, Utils.getFinalVideoPath(), 480, 480);
                    Log.d(TAG, "end to crop video");
                    Toast.makeText(FullscreenShootActivity.this, "Video process successed.", Toast.LENGTH_LONG).show();
                }
            }
        });
        this.mRecManager = RecordingManager.getInstance(FullscreenShootActivity.this, cameraPreviewFrame);
        videoFilepath = Utils.getNextVideoPath();
        snapshotFilepath = Utils.getNextSnapshotPath();
        mRecManager.init(videoFilepath, snapshotFilepath);
        nextButton.setVisibility(View.GONE);
    }
    
    public int playbackVideo(String videoFilepath) {
        cameraPreviewFrame.removeAllViews();
        cameraPreviewFrame.setVisibility(View.GONE);
        status = internalStatus.Previewing;
        nextButton.setText("Crop");

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        android.widget.FrameLayout.LayoutParams params = (android.widget.FrameLayout.LayoutParams) videoView.getLayoutParams();
        params.width =  metrics.widthPixels;
        params.height = metrics.widthPixels * mRecManager.getVideoWidth() / mRecManager.getVideoHeight();
        Log.d(TAG, "layout width=" + params.width + ", height=" + params.height);
        videoView.setLayoutParams(params);
        videoView.setVisibility(View.VISIBLE);

        videoView.setVideoURI(Uri.parse(videoFilepath));
        videoView.start();
        videoView.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                Log.d(TAG, "ERROR HERE");
                videoView.stopPlayback();
                videoView.setVisibility(View.GONE);
                return true;
            }
        });
        videoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer arg0) {
                videoView.start();
                Log.d(TAG, "vide prepared here");
                videoView.setVisibility(View.VISIBLE);
                videoView.measure(0, 0);
                Log.d(TAG,
                        "video size" + videoView.getMeasuredHeight() + ":"
                                + videoView.getMeasuredWidth());

            }

        });
        videoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer arg0) {
                Log.d(TAG, "vide play again");
                videoView.start();
            }
        });
        return 0;
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
    private Handler progressHandler = new Handler() {
        public void handleMessage(Message msg) {
            String task = (String) msg.obj;
            if (TASK_UPDATE_RECORD_PROGRESS.equals(task)) {
                progressBar.setProgress(msg.what);
                if (msg.what > 30 && nextButton.getVisibility() != View.VISIBLE) {
                    nextButton.setVisibility(View.VISIBLE);
                    nextButton.setText("Preview");
                }
            }
        }
    };

    class ProgressCounter extends CountDownTimer {
        public ProgressCounter(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onFinish() {
            if (internalStatus.Recroding == status) {
                try {
                    mRecManager.stopRecording();
                    nextButton.setText("Preview");
                    nextButton.setVisibility(View.VISIBLE);
                    new VideoMergeTask().execute((Void) null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void onTick(long millisUntilFinished) {
            tsCanRecord = millisUntilFinished;
            Message msg = new Message();
            msg.obj = TASK_UPDATE_RECORD_PROGRESS;
            msg.what = 100 - (int) millisUntilFinished / 60;
            progressHandler.sendMessage(msg);
        }
    }
    
    public class VideoMergeTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
//            loading.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            String finalVideoFilepath = getFinalVideoFilepath();
            VideoUtils.MergeFiles(videoTempDir, finalVideoFilepath);
            return null;
        }

        @Override
        protected void onPostExecute(Void arg0) {
            String finalVideoFilepath = getFinalVideoFilepath();
            playbackVideo(finalVideoFilepath);
        }
    }

}
