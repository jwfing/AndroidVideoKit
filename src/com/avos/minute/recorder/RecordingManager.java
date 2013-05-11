package com.avos.minute.recorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;

public class RecordingManager implements Camera.ErrorCallback, MediaRecorder.OnErrorListener, MediaRecorder.OnInfoListener {

    private static final String TAG = RecordingManager.class.getSimpleName();
    private static final int FOCUS_AREA_RADIUS = 32;
    private static final int FOCUS_MAX_VALUE = 1000;
    private static final int FOCUS_MIN_VALUE = -1000;
    private static final long MINIMUM_RECORDING_TIME = 2000;
    private static final int MAXIMUM_RECORDING_TIME = 60 * 1000;
    private static final long CLIP_GRAPH_UPDATE_INTERVAL = 25;
    private static final long LOW_STORAGE_THRESHOLD = 5 * 1024 * 1024;
    private static final long RECORDING_FILE_LIMIT = 100 * 1024 * 1024;

    private boolean mPaused = true;

    private MediaRecorder mMediaRecorder = null;
    private boolean mRecording = false;

    private FrameLayout mPreviewFrame = null;

    private boolean mPreviewing = false;

    private TextureView mTextureView = null;
    private SurfaceTexture mSurfaceTexture = null;
    private boolean mSurfaceTextureReady = false;

    private SurfaceView mSurfaceView = null;
    private SurfaceHolder mHolder = null;
    private boolean mSurfaceViewReady = false;

    private Camera mCamera = null;
    private Camera.Parameters mParameters = null;
    private CamcorderProfile mCamcorderProfile = null;

    private int mOrientation = -1;
    private OrientationEventListener mOrientationEventListener = null;

    private long mStartRecordingTime;
    private int mVideoWidth;
    private int mVideoHeight;
    private long mStorageSpace;

    private Handler mHandler = new Handler();
    private Runnable mUpdateRecordingTimeTask = new Runnable() {
        @Override
        public void run() {
            long recordingTime = System.currentTimeMillis() - mStartRecordingTime;
            Log.d(TAG, String.format("Recording time:%d", recordingTime));
            mHandler.postDelayed(this, CLIP_GRAPH_UPDATE_INTERVAL);
        }
    };
    private Runnable mStopRecordingTask = new Runnable() {
        @Override
        public void run() {
            stopRecording();
        }
    };

    private static RecordingManager mInstance = null;
    private Activity currentActivity = null;
    private String destinationFilepath = "";
    private String snapshotFilepath = "";

    public static RecordingManager getInstance(Activity activity, FrameLayout previewFrame) {
        if (mInstance == null || mInstance.currentActivity != activity) {
            mInstance = new RecordingManager(activity, previewFrame);
        }
        return mInstance;
    }

    private RecordingManager(Activity activity, FrameLayout previewFrame) {
        currentActivity = activity;
        mPreviewFrame = previewFrame;
    }

    public void setDestinationFilepath(String filepath) {
        this.destinationFilepath = filepath;
    }
    public String getDestinationFilepath() {
        return this.destinationFilepath;
    }
    public void setSnapshotFilepath(String filepath) {
        this.snapshotFilepath = filepath;
    }
    public String getSnapshotFilepath() {
        return this.snapshotFilepath;
    }
    public void init(String videoPath, String snapshotPath) {
        Log.v(TAG, "init.");
        setDestinationFilepath(videoPath);
        setSnapshotFilepath(snapshotPath);
        if (!Utils.isExternalStorageAvailable()) {
            showStorageErrorAndFinish();
            return;
        }

        openCamera();
        if (mCamera == null) {
            showCameraErrorAndFinish();
            return;
        }

        if (useTexture()) {
            initTextureView();
        } else {
            initSurfaceView();
        }

        // start preview
        if (useTexture()) {
            mTextureView.setVisibility(View.VISIBLE);
        } else {
            mSurfaceView.setVisibility(View.VISIBLE);
        }
    }

    public void onResume() {
        Log.v(TAG, "onResume.");
        mPaused = false;

        // Open the camera
        if (mCamera == null) {
            openCamera();
            if (mCamera == null) {
                showCameraErrorAndFinish();
                return;
            }
        }

        // Initialize the surface texture or surface view
        if (useTexture() && mTextureView == null) {
            initTextureView();
            mTextureView.setVisibility(View.VISIBLE);
        } else if (!useTexture() && mSurfaceView == null) {
            initSurfaceView();
            mSurfaceView.setVisibility(View.VISIBLE);
        }

        // Start the preview
        if (!mPreviewing) {
            startPreview();
        }
    }

    private void openCamera() {
        Log.v(TAG, "openCamera");
        try {
            mCamera = Camera.open();
            mCamera.setErrorCallback(this);
            mCamera.setDisplayOrientation(90); // Since we only support portrait mode
            mParameters = mCamera.getParameters();
        } catch (RuntimeException e) {
            e.printStackTrace();
            mCamera = null;
        }
    }

    private void closeCamera() {
        Log.v(TAG, "closeCamera");
        if (mCamera == null) {
            Log.d(TAG, "Already stopped.");
            return;
        }

        mCamera.setErrorCallback(null);
        if (mPreviewing) {
            stopPreview();
        }
        mCamera.release();
        mCamera = null;
    }

    private void initTextureView() {
        mTextureView = new TextureView(currentActivity);
        mTextureView.setSurfaceTextureListener(new SurfaceTextureCallback());
        mTextureView.setVisibility(View.GONE);
        FrameLayout.LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        mTextureView.setLayoutParams(params);
        mPreviewFrame.addView(mTextureView);
    }

    private void releaseSurfaceTexture() {
        if (mSurfaceTexture != null) {
            mPreviewFrame.removeAllViews();
            mTextureView = null;
            mSurfaceTexture = null;
            mSurfaceTextureReady = false;
        }
    }

    private void initSurfaceView() {
        mSurfaceView = new SurfaceView(currentActivity);
        mSurfaceView.getHolder().addCallback(new SurfaceViewCallback());
        mSurfaceView.setVisibility(View.GONE);
        FrameLayout.LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        mSurfaceView.setLayoutParams(params);
        mPreviewFrame.addView(mSurfaceView);
    }

    private void releaseSurfaceView() {
        if (mSurfaceView != null) {
            mPreviewFrame.removeAllViews();
            mSurfaceView = null;
            mHolder = null;
            mSurfaceViewReady = false;
        }
    }

    private void startPreview() {
        if ((useTexture() && !mSurfaceTextureReady) || (!useTexture() && !mSurfaceViewReady)) {
            return;
        }

        Log.v(TAG, "startPreview.");
        if (mPreviewing) {
            stopPreview();
        }

        setCameraParameters();
        resizePreview();

        try {
            if (useTexture()) {
                mCamera.setPreviewTexture(mSurfaceTexture);
            } else {
                mCamera.setPreviewDisplay(mHolder);
            }
            mCamera.startPreview();
            mPreviewing = true;
        } catch (Exception e) {
            closeCamera();
            e.printStackTrace();
            Log.e(TAG, "startPreview failed.");
        }

    }

    private void stopPreview() {
        Log.v(TAG, "stopPreview");
        if (mCamera != null) {
            mCamera.stopPreview();
            mPreviewing = false;
        }
    }

    public void onPause() {
        mPaused = true;

        if (mRecording) {
            stopRecording();
        }
        closeCamera();

        if (useTexture()) {
            releaseSurfaceTexture();
        } else {
            releaseSurfaceView();
        }
    }

    private void setCameraParameters() {
        if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            mCamcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            mCamcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        } else {
            mCamcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        }
        mVideoWidth = mCamcorderProfile.videoFrameWidth;
        mVideoHeight = mCamcorderProfile.videoFrameHeight;
        mCamcorderProfile.fileFormat = MediaRecorder.OutputFormat.MPEG_4;
        mCamcorderProfile.videoFrameRate = 25;

        Log.v(TAG, "mVideoWidth=" + mVideoWidth + " mVideoHeight=" + mVideoHeight);
        mParameters.setPreviewSize(mVideoWidth, mVideoHeight);

        if (mParameters.getSupportedWhiteBalance().contains(Camera.Parameters.WHITE_BALANCE_AUTO)) {
            mParameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        }

        if (mParameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }

        mParameters.setRecordingHint(true);
        mParameters.set("cam_mode", 1);

        mCamera.setParameters(mParameters);
        mParameters = mCamera.getParameters();

        mCamera.setDisplayOrientation(90);
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Log.d(TAG, info.orientation + " degree");
    }

    private void resizePreview() {
        Log.d(TAG, String.format("Video size:%d|%d", mVideoWidth, mVideoHeight));
        Point optimizedSize = getOptimizedPreviewSize(mVideoWidth, mVideoHeight);
        Log.d(TAG, String.format("Optimized size:%d|%d", optimizedSize.x, optimizedSize.y));

        ViewGroup.LayoutParams params = (ViewGroup.LayoutParams) mPreviewFrame.getLayoutParams();
        params.width = optimizedSize.x;
        params.height = optimizedSize.y;
        mPreviewFrame.setLayoutParams(params);
    }

    public void setOrientation(int ori) {
        this.mOrientation = ori;
    }

    public void setOrientationEventListener(OrientationEventListener listener) {
        this.mOrientationEventListener = listener;
    }

    public Camera getCamera() {
        return mCamera;
    }

    @SuppressWarnings("serial")
    public void setFocusArea(float x, float y) {
        if (mCamera != null) {
            int viewWidth = mSurfaceView.getWidth();
            int viewHeight = mSurfaceView.getHeight();

            int focusCenterX = FOCUS_MAX_VALUE - (int) (x / viewWidth * (FOCUS_MAX_VALUE - FOCUS_MIN_VALUE));
            int focusCenterY = FOCUS_MIN_VALUE + (int) (y / viewHeight * (FOCUS_MAX_VALUE - FOCUS_MIN_VALUE));
            final int left = focusCenterY - FOCUS_AREA_RADIUS < FOCUS_MIN_VALUE ? FOCUS_MIN_VALUE : focusCenterY - FOCUS_AREA_RADIUS;
            final int top = focusCenterX - FOCUS_AREA_RADIUS < FOCUS_MIN_VALUE ? FOCUS_MIN_VALUE : focusCenterX - FOCUS_AREA_RADIUS;
            final int right = focusCenterY + FOCUS_AREA_RADIUS > FOCUS_MAX_VALUE ? FOCUS_MAX_VALUE : focusCenterY + FOCUS_AREA_RADIUS;
            final int bottom = focusCenterX + FOCUS_AREA_RADIUS > FOCUS_MAX_VALUE ? FOCUS_MAX_VALUE : focusCenterX + FOCUS_AREA_RADIUS;

            Camera.Parameters params = mCamera.getParameters();
            params.setFocusAreas(new ArrayList<Camera.Area>() {
                {
                    add(new Camera.Area(new Rect(left, top, right, bottom), 1000));
                }
            });
            mCamera.setParameters(params);
            mCamera.autoFocus(new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Log.d(TAG, "onAutoFocus");
                }
            });
        }
    }

    public void startRecording() {
        if (!mRecording) {
            updateStorageSpace();
            if (mStorageSpace <= LOW_STORAGE_THRESHOLD) {
                Log.v(TAG, "Storage issue, ignore the start request");
                Toast.makeText(currentActivity, "Storage issue, ignore the recording request", Toast.LENGTH_LONG).show();
                return;
            }

            if (!prepareMediaRecorder()) {
                Toast.makeText(currentActivity, "prepareMediaRecorder failed.", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d(TAG, "Successfully prepare media recorder.");
            try {
                mMediaRecorder.start();
            } catch (RuntimeException e) {
                Log.e(TAG, "MediaRecorder start failed.");
                releaseMediaRecorder();
                return;
            }

            mStartRecordingTime = System.currentTimeMillis();

            if (mOrientationEventListener != null) {
                mOrientationEventListener.disable();
            }

//            mClipGraph.setVisibility(View.VISIBLE);
//            mHandler.post(mUpdateRecordingTimeTask);
            mRecording = true;

        }
    }

    public void stopRecording() {
        if (mRecording) {
            if (!mPaused) {
                // Capture at least 1 second video
                long currentTime = System.currentTimeMillis();
                if (currentTime - mStartRecordingTime < MINIMUM_RECORDING_TIME) {
                    mHandler.postDelayed(mStopRecordingTask, MINIMUM_RECORDING_TIME - (currentTime - mStartRecordingTime));
                    return;
                }
            }

            if (mOrientationEventListener != null) {
                mOrientationEventListener.enable();
            }

//            mHandler.removeCallbacks(mUpdateRecordingTimeTask);

            try {
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.stop(); // stop the recording
                Toast.makeText(currentActivity, "Video file saved.", Toast.LENGTH_LONG).show();

                long stopRecordingTime = System.currentTimeMillis();
                Log.d(TAG, String.format("stopRecording. file:%s duration:%d", destinationFilepath, stopRecordingTime - mStartRecordingTime));

                // Calculate the duration of video
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this.destinationFilepath);
                String _length = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (_length != null) {
                    Log.d(TAG, String.format("clip duration:%d", Long.parseLong(_length)));
                }

                // Taking the snapshot of video
                Bitmap snapshot = ThumbnailUtils.createVideoThumbnail(this.destinationFilepath, Thumbnails.MICRO_KIND);
                try {
                    FileOutputStream out = new FileOutputStream(this.snapshotFilepath);
                    snapshot.compress(Bitmap.CompressFormat.JPEG, 70, out);
                    out.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

//                mActivity.showPlayButton();

            } catch (RuntimeException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
                // if no valid audio/video data has been received when stop() is
                // called
            } finally {
//                mClipGraph.setActiveRecordingTime(0);
//                mRecordingTimeTextView.setRecordingTime(0);
//                mActiveClip = null;

                releaseMediaRecorder(); // release the MediaRecorder object
                if (!mPaused) {
                    mParameters = mCamera.getParameters();
                }
                mRecording = false;
            }

        }
    }

    public void setRecorderOrientation(int orientation) {
        // For back camera only
        if (orientation != -1) {
            Log.d(TAG, "set orientationHint:" + (orientation + 135) % 360 / 90 * 90);
            mMediaRecorder.setOrientationHint((orientation + 135) % 360 / 90 * 90);
        }else {
            Log.d(TAG, "not set orientationHint to mediaRecorder");
        }
    }

    private boolean prepareMediaRecorder() {
        mMediaRecorder = new MediaRecorder();

        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mMediaRecorder.setProfile(mCamcorderProfile);

        mMediaRecorder.setMaxDuration(MAXIMUM_RECORDING_TIME);
        mMediaRecorder.setOutputFile(this.destinationFilepath);

        try {
            mMediaRecorder.setMaxFileSize(Math.min(RECORDING_FILE_LIMIT, mStorageSpace - LOW_STORAGE_THRESHOLD));
        } catch (RuntimeException exception) {
        }

        setRecorderOrientation(mOrientation);

        if (!useTexture()) {
            mMediaRecorder.setPreviewDisplay(mHolder.getSurface());
        }

        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }

        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setOnInfoListener(this);

        return true;

    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset(); // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock(); // lock camera for later use
        }
    }

    private Point getOptimizedPreviewSize(int videoWidth, int videoHeight) {
        Display display = currentActivity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        Point optimizedSize = new Point();
        optimizedSize.x = size.x;
        optimizedSize.y = (int) ((float) videoWidth / (float) videoHeight * size.x);

        return optimizedSize;
    }

    private void showCameraErrorAndFinish() {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentActivity.finish();
            }
        };
        new AlertDialog.Builder(currentActivity).setCancelable(false)
                .setTitle("Camera error")
                .setMessage("Cannot connect to the camera.")
                .setNeutralButton("OK", buttonListener)
                .show();
    }

    private void showStorageErrorAndFinish() {
        DialogInterface.OnClickListener buttonListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                currentActivity.finish();
            }
        };
        new AlertDialog.Builder(currentActivity).setCancelable(false)
                .setTitle("Storage error")
                .setMessage("Cannot read external storage.")
                .setNeutralButton("OK", buttonListener)
                .show();
    }

    private void updateStorageSpace() {
        mStorageSpace = getAvailableSpace();
        Log.v(TAG, "updateStorageSpace mStorageSpace=" + mStorageSpace);
    }

    private long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return -1;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return -1;
        }

        File directory = currentActivity.getExternalFilesDir("wanpai");
        directory.mkdirs();
        if (!directory.isDirectory() || !directory.canWrite()) {
            return -1;
        }

        try {
            StatFs stat = new StatFs(directory.getAbsolutePath());
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return -1;
    }

    private boolean useTexture() {
//        return false;
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1;
    }

    private class SurfaceViewCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.v(TAG, "surfaceChanged. width=" + width + ". height=" + height);
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.v(TAG, "surfaceCreated");
            mSurfaceViewReady = true;
            mHolder = holder;
            startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            mSurfaceViewReady = false;
        }

    }

    private class SurfaceTextureCallback implements TextureView.SurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            Log.v(TAG, "onSurfaceTextureAvailable");
            mSurfaceTextureReady = true;
            mSurfaceTexture = surface;
            startPreview();
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurfaceTextureReady = false;
            return false;
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }

    }

    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, "Camera onError. what=" + error + ".");
        if (error == Camera.CAMERA_ERROR_SERVER_DIED) {

        } else if (error == Camera.CAMERA_ERROR_UNKNOWN) {

        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
            stopRecording();
        } else if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            stopRecording();
            Toast.makeText(currentActivity, "Size limit reached", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e(TAG, "MediaRecorder onError. what=" + what + ". extra=" + extra);
        if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
            stopRecording();
        }
    }

}
