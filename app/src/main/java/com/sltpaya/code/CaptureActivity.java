package com.sltpaya.code;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;
import com.muddzdev.pixelshot.PixelShot;
import com.qr.CameraController;
import com.qr.QRCallback;
import com.qr.QResult;
import com.qr.camera.CameraManager;
import com.qr.decode.DecodeUtil;
import com.qr.decode.SaveImg;
import com.sltpaya.code.view.FrameView;
import com.sltpaya.code.view.ProgressDialog;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Author：SLTPAYA
 * Date：2017/11/13 10:01
 * 扫码Activity
 */
public class CaptureActivity extends AppCompatActivity implements QRCallback {

    public static final String KEY_RESULT = "RESULT";
    private final static int S_GALLERY_CODE = 1000;
    private final String TAG = getClass().getSimpleName();
    public boolean mIsGalleryDecode = false;    //the decode bitmap is from gallery?
    private TextView mTag;
    private ViewGroup fl;
    private TextView mFlashTv;
    private FrameView frameView;
    private SurfaceView mSurfaceView;
    private BeepManager beepManager;
    private CameraController cameraController;
    private static final long scanAnimDuration = 4000;       //scan anim duration
    private Pattern urlPattern;
    private ProgressDialog mProgressDialog;

    {
        urlPattern = Pattern.compile("^((https?|ftp|file|HTTPS?)://)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        //Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        //Initialize the View
        initView();
        initListener();
        initCapture();
    }

    private void initView() {
        fl = findViewById(R.id.rl);
        mTag = findViewById(R.id.tag);
        frameView = findViewById(R.id.frame);
        mFlashTv = findViewById(R.id.flash);
        mSurfaceView = findViewById(R.id.surface);
    }

    private void initListener() {
        findViewById(R.id.back).setOnClickListener(v -> finish());
//        mFlashTv.setOnClickListener(v -> updateFlash());
        mFlashTv.setOnClickListener(v -> {
            Toast.makeText(this, "瓦城", Toast.LENGTH_SHORT).show();
            getSurfaceBitmap(mSurfaceView, new PixelCopyListener() {
                @Override
                public void onSurfaceBitmapReady(Bitmap surfaceBitmap) {
                    //Bitmap bm = Bitmap.createBitmap(surfaceBitmap, frameView.getLeft(), frameView.getTop(), frameView.getWidth(), frameView.getHeight());
                    SaveImg.saveImg(surfaceBitmap, System.currentTimeMillis()+".jpg", CaptureActivity.this);
                }

                @Override
                public void onSurfaceBitmapError() {
                    Log.d(TAG, "Couldn't create a bitmap of the SurfaceView");
                }
            });
        });
        findViewById(R.id.gallery).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_PICK);
            resetFlash();/*进入相册之前如果闪关灯是打开的,就关闭*/
            startActivityForResult(intent, S_GALLERY_CODE);
        });

        /*触摸之后手动对焦*/
        frameView.setOnClickListener(v -> {
            CameraManager manager = cameraController.getCameraManager();
            if (manager != null) {
                manager.onFocus();
            }
        });
    }

    /**
     * 初始化二维码识别组件
     */
    private void initCapture() {
        cameraController = CameraController.with(mSurfaceView).build(this);
        beepManager = new BeepManager(this);
    }

    /**
     * Update the flash status
     */
    private void updateFlash() {
        CameraManager manager = cameraController.getCameraManager();
        if (manager != null) {
            manager.updateFlash(new CameraManager.IFlashStatusCallback() {
                @Override
                public void on() {
                    mFlashTv.setSelected(true);
                    mFlashTv.setText("轻触照亮");
                }

                @Override
                public void off() {
                    mFlashTv.setSelected(false);
                    mFlashTv.setText("轻触照亮");
                }
            });
        }
    }

    public boolean isStartInFocusChanged = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!isStartInFocusChanged) {
            //开启动画
            isStartInFocusChanged = true;
            startScanAnim();
        }
    }

    private void startScanAnim() {
        frameView.setDuration(scanAnimDuration);
        frameView.startScan(Animation.INFINITE);
    }

    @Override
    protected void onResume() {
        cameraController.onResume();
        startScanAnim();
        if (mIsGalleryDecode) {
            //正在识别相册二维码, 此时不预览摄像头数据
            mSurfaceView.getHolder().removeCallback(cameraController);
            //不开启扫描动画
            frameView.stopScan();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        dismissProgress();
        beepManager.close();
        cameraController.onPause();
        frameView.stopScan();
        super.onPause();
    }

    /**
     * The flashing lights are on and off
     */
    private void resetFlash() {
        if (mFlashTv.isSelected()) {
            updateFlash();
        }
    }
@Nullable
    public static void getSurfaceBitmap(@NonNull SurfaceView surfaceView, @NonNull final PixelCopyListener listener) {
        final Bitmap bitmap = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
        final HandlerThread handlerThread = new HandlerThread(CaptureActivity.class.getSimpleName());
        handlerThread.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PixelCopy.request(surfaceView, bitmap, copyResult -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    listener.onSurfaceBitmapReady(bitmap);
                } else {
                    listener.onSurfaceBitmapError();
                }
                handlerThread.quitSafely();
            }, new Handler(handlerThread.getLooper()));
        } else {
            Log.d(PixelShot.class.getSimpleName(), "Saving an image of a SurfaceView is only supported from API 24");
        }
    }

    interface PixelCopyListener {
        void onSurfaceBitmapReady(Bitmap bitmap);
        void onSurfaceBitmapError();
    }

    @Override
    public void handleDecode(QResult rawResult) {
        resetFlash();/*The flashing lights are on and off*/
        beepManager.playBeepSoundAndVibrate();

        getSurfaceBitmap((SurfaceView) mSurfaceView, new PixelCopyListener() {
            @Override
            public void onSurfaceBitmapReady(Bitmap surfaceBitmap) {
                //Bitmap bm = Bitmap.createBitmap(surfaceBitmap, frameView.getLeft(), frameView.getTop(), frameView.getWidth(), frameView.getHeight());
                SaveImg.saveImg(surfaceBitmap, System.currentTimeMillis()+".jpg", CaptureActivity.this);
            }

            @Override
            public void onSurfaceBitmapError() {
                Log.d(TAG, "Couldn't create a bitmap of the SurfaceView");
            }
        });

        String text = rawResult.getText();
        /*跳转结果页*/
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(KEY_RESULT, text);
        startActivity(intent);
        finish();
    }

    @Override
    public void luminance(int value) {
        //如果手电筒已经亮了就不再受光度影响了
        if (mFlashTv.isSelected()) {
            return;
        }
        //如果手电筒已经显示了,就不再重复执行了
        int visibility = mFlashTv.getVisibility();

        if (value < 60) {
            //低亮度下, 没有显示就显示
            if (visibility != View.VISIBLE) {
                //mFlashTv.setVisibility(View.VISIBLE);
            }
        } else {
            //大亮度下没有隐藏就隐藏
            if (visibility != View.GONE) {
                //mFlashTv.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
        builder.setTitle("错误提示");
        builder.setMessage("开启相机失败, 请检查相机是否占用或有无权限");
        builder.setPositiveButton("好的", (dialog, which) -> finish());
        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination")
    public Rect getRect(int cameraHeight, int cameraWidth) {
        /*获取布局中扫描框的位置信息*/
//        int[] location = new int[2];
//        frameView.getLocationInWindow(location);
//
//        int cropLeft = location[0];
//        int cropTop = location[1];
//
//        /*获取扫描框父容器的宽高*/
//        int containerWidth = fl.getWidth();
//        int containerHeight = fl.getHeight();
//
//        int radioWidth = cameraWidth / containerWidth;
//        int radioHeight = cameraHeight / containerHeight;
//
//        /*计算最终截取的矩形的左上角顶点x坐标*/
//        int x = cropLeft * radioWidth;
//        /*计算最终截取的矩形的左上角顶点y坐标*/
//        int y = cropTop * radioHeight;
//
//        /*获取扫描框的宽高*/
//        int cropWidth = frameView.getWidth();
//        int cropHeight = frameView.getHeight();
//
//        /*计算最终截取的矩形的宽度 */
//        int width = cropWidth * radioWidth;
//        /*计算最终截取的矩形的高度 */
//        int height = cropHeight * radioHeight;

        /*生成最终的截取的矩形*/
        //return new Rect(x, y, width + x, height + y);
        return new Rect(frameView.getLeft(), frameView.getTop(), frameView.getRight(), frameView.getBottom());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;
        if (requestCode == S_GALLERY_CODE) {
            if (data == null || (uri = data.getData()) == null) {
                return;
            }
            handleGallery(uri);
        }
    }

    private void handleGallery(@NonNull Uri uri) {
        showProgress();
        mTag.setText("正在识别中...");
        mIsGalleryDecode = true;
        DecodeUtil.decodeQRCode(uri, CaptureActivity.this, new DecodeUtil.DecodeCallback() {
            @Override
            public void onUriFailed() {
                dismissProgress();
                mIsGalleryDecode = false;//相册图片错误
                Toast.makeText(CaptureActivity.this, "相册图片错误", Toast.LENGTH_SHORT).show();
                restartCamera();
            }

            @Override
            public void onNonResult() {
                dismissProgress();
                showNull();
                mIsGalleryDecode = false;
            }

            @Override
            public void onResult(final QResult result) {
                dismissProgress();
                handleDecode(result);
                mIsGalleryDecode = false;
            }
        });
    }

    /**
     * 显示处理进度条
     */
    private void showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog();
        }
        mProgressDialog.show(getFragmentManager(), TAG);
    }

    /**
     * 关闭处理进度条
     */
    private void dismissProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * 显示无二维码结果dialog
     */
    private void showNull() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CaptureActivity.this);
        builder.setTitle("结果");
        builder.setMessage("未识别到二维码");
        builder.setPositiveButton("好的", (dialog, which) -> restartCamera());
        builder.setOnCancelListener(dialog -> restartCamera());
        builder.show();
    }

    /**
     * 重新预览摄像头数据
     */
    private void restartCamera() {
        isStartInFocusChanged = false;
        cameraController.onResumeCamera();
        mTag.setText(null);
    }

}

