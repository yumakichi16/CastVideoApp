package com.yumakichidrive.castvideoapp;

import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.googlecode.javacv.FFmpegFrameRecorder;
import com.googlecode.javacv.FrameRecorder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class CastvideoActivity extends ActionBarActivity {

    Camera mCamera;
    SurfaceView mCameraSurfaceView;
    int mPreviewWidth;
    int mPreviewHeight;
    private SurfaceHolder.Callback mSurfaceViewListener = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            mCamera = Camera.open();
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            mCamera.stopPreview();

            Camera.Parameters params = mCamera.getParameters();
            // size
            List<Camera.Size> sizes = params.getSupportedPreviewSizes();
            Camera.Size size = sizes.get(0);
            params.setPreviewSize(size.width, size.height);

            // orientation
            ViewGroup.LayoutParams layoutParams = mCameraSurfaceView.getLayoutParams();
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                // 縦
                mCamera.setDisplayOrientation(90);
                layoutParams.width = size.height;
                layoutParams.height = size.width;
                mPreviewWidth = mCamera.getParameters().getPreviewSize().height;
                mPreviewHeight = mCamera.getParameters().getPreviewSize().width;
            } else {
                mCamera.setDisplayOrientation(0);
                layoutParams.width = size.width;
                layoutParams.height = size.height;
                mPreviewWidth = mCamera.getParameters().getPreviewSize().width;
                mPreviewHeight = mCamera.getParameters().getPreviewSize().height;
            }
            mCameraSurfaceView.setLayoutParams(layoutParams);

            mCamera.setParameters(params);


            mCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        }
    };

    final int RECORDE_FLAME_CNT = 50;
    final int RECORDE_FLAME_RATE = 5;
    final String RECORDE_TMP_DIRNAME = "castvideo";
    int mPrenban = 0;
    int mSrenban = 1;
    FFmpegFrameRecorder mRecorder = null;
    private Camera.PreviewCallback mPreviewListener = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (data != null) {
                mCamera.setPreviewCallback(null);

                int pWidth = mCamera.getParameters().getPreviewSize().width;
                int pHeight = mCamera.getParameters().getPreviewSize().height;

                try {
                    IplImage iplimage = IplImage.create(pWidth, pHeight, IPL_DEPTH_8U, 2);
                    iplimage.getByteBuffer().put(data);
                    mRecorder.record(iplimage);
                    iplimage.release();
                    mSrenban++;
                } catch (FrameRecorder.Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    Timer mTimer;
    Handler mHandler = new Handler();
    class RecordeTimer extends TimerTask {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    recordePreview();
                }
            });
        }
    }
    public void recordePreview() {
        if (mSrenban > RECORDE_FLAME_CNT) {
            try {
                mRecorder.stop();
                // mp4 deploy
                // TODO..
                mSrenban = 1;
                mPrenban++;

                // RECORDER ReStart..
                mRecorder = new FFmpegFrameRecorder(getExternalFilesDir(RECORDE_TMP_DIRNAME).toString()
                        + "/movie" + String.valueOf(mPrenban) + ".mp4", mPreviewWidth, mPreviewHeight, 2);
                mRecorder.setFormat("mp4");
                mRecorder.setFrameRate(RECORDE_FLAME_RATE);
                mRecorder.start();
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
        mCamera.setPreviewCallback(mPreviewListener);
    }

    boolean mRecFlg = false;
    public void startRecorde() {
        if (!mRecFlg) {
            mRecFlg = true;

            // recorde tmp clear
            clearTmpDir();
            mPrenban = 0;
            mSrenban = 1;

            // make tmp dir
            File tmpDir = new File(getExternalFilesDir(RECORDE_TMP_DIRNAME).toString());
            tmpDir.mkdir();

            try {
                // get videoID
                // TODO..

                // start recorde
                mRecorder = new FFmpegFrameRecorder(getExternalFilesDir(RECORDE_TMP_DIRNAME).toString()
                        + "/movie" + String.valueOf(mPrenban) + ".mp4", mPreviewWidth, mPreviewHeight, 2);
                mRecorder.setFormat("mp4");
                mRecorder.setFrameRate(RECORDE_FLAME_RATE);
                mRecorder.start();
                mTimer = new Timer();
                mTimer.schedule(new RecordeTimer(), 1000/RECORDE_FLAME_RATE, 1000/RECORDE_FLAME_RATE);
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
//            Toast.makeText(CastvideoActivity.this, mCameraSurfaceView.getWidth() + "," + mCameraSurfaceView.getHeight(), Toast.LENGTH_SHORT).show();

        }
    }
    public void stopRecorde() {
        if (mRecFlg) {
            mRecFlg = false;
            mCamera.setPreviewCallback(null);
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            try {
                mRecorder.stop();
                mRecorder = null;
            } catch (FrameRecorder.Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void clearTmpDir() {
        File tmpDir = new File(getExternalFilesDir(RECORDE_TMP_DIRNAME).toString());
        File[] files = tmpDir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                files[i].delete();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_castvideo);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraSurfaceView = (SurfaceView)findViewById(R.id.surfaceView_camera);
        SurfaceHolder holder = mCameraSurfaceView.getHolder();
        holder.addCallback(mSurfaceViewListener);
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_castvideo, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_castvideo) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            if (!mRecFlg) {
                dialog.setMessage("ライブ動画配信を開始します。よろしいですか？");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startRecorde();
                    }
                });
            } else {
                dialog.setMessage("配信を終了します。よろしいですか？");
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopRecorde();
                    }
                });
            }
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            dialog.setCancelable(false);
            AlertDialog alertdialog = dialog.create();
            alertdialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void YUV_NV21_TO_BGR(int[] bgr, byte[] yuv, int width,
                                int height) {
        final int frameSize = width * height;

        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;

        int a = 0;
        for (int i = 0, ci = ii; i < height; ++i, ci += di) {
            for (int j = 0, cj = ij; j < width; ++j, cj += dj) {
                int y = (0xff & ((int) yuv[ci * width + cj]));
                int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width
                        + (cj & ~1) + 0]));
                int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width
                        + (cj & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int a0 = 1192 * (y - 16);
                int a1 = 1634 * (v - 128);
                int a2 = 832 * (v - 128);
                int a3 = 400 * (u - 128);
                int a4 = 2066 * (u - 128);

                int r = (a0 + a1) >> 10;
                int g = (a0 - a2 - a3) >> 10;
                int b = (a0 + a4) >> 10;

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                bgr[a++] = 0xff000000 | (b << 16) | (g << 8) | r;
            }
        }
    }
}
