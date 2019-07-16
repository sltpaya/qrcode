package com.qr;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import com.qr.camera.CameraManager;
import com.qr.camera.focus.QRFocusCallback;
import com.qr.decode.AllDecode;
import com.qr.decode.base.QDecode;
import com.qr.decode.base.QRHandler;
import java.io.IOException;

/**
 * Author: SLTPAYA
 * Date: 2017/11/4
 *
 * 相机控制器
 */
public class CameraController implements SurfaceHolder.Callback, ICameraP {

    private final String TAG = getClass().getSimpleName();

    private Context mContext;
    private CameraManager cameraManager;
    private QRHandler handler;

    private SurfaceView scanPreview;
    private Rect mCropRect = null;

    private boolean isHasSurface = false;
    private QRFocusCallback focusCallback;

    private QRCallback mQRCallback;

    private ICameraDataProvider dataProvider;

    private CameraController(SurfaceView surfaceView) {
        mContext = surfaceView.getContext();
        this.scanPreview = surfaceView;
    }

    /**
     * 由代理对象获取CameraController
     * @return Proxy
     */
    public static Proxy with(@NonNull SurfaceView surfaceView) {
        Proxy p = new Proxy();
        p.with(surfaceView);
        return p;
    }

    /**
     * 添加二维码处理逻辑回调
     * @param QRCallback QRCallback
     */
    void setQRCallback(QRCallback QRCallback) {
        mQRCallback = QRCallback;
        assert mQRCallback != null;
    }

    public void setDataProvider(ICameraDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public void handleDecode(QResult rawResult) {
        mQRCallback.handleDecode(rawResult);
    }

    @Override
    public void luminance(int value) {
        mQRCallback.luminance(value);
    }

    public void setAutoFocusCallback(QRFocusCallback focusCallback) {
        this.focusCallback = focusCallback;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    /**
     * 当camera没有初始化的时候对象为null
     * @return CameraManager
     */
    @Override
    public @Nullable
    CameraManager getCameraManager() {
        return cameraManager;
    }

    public void onPause() {
        if (handler != null) {
            handler.destroy();
            handler = null;
        }
        if (!isHasSurface) {
            scanPreview.getHolder().removeCallback(this);
        }
        QDecode.needDecode = false;
        cameraManager.stopPreview();
        cameraManager.closeDriver();
    }

    /**
     * SurfaceHolder已经创建了， 此时手动重启相机
     */
    public synchronized void onResumeCamera() {
        if (handler != null) {
            handler.destroy();
            handler = null;
        }
        initCamera(scanPreview.getHolder());
    }

    public void onResume() {
        cameraManager = new CameraManager(mContext);
        cameraManager.setAutoFocusCallback(focusCallback);

        if (handler != null) {
            handler.destroy();
            handler = null;
        }

        if (isHasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(scanPreview.getHolder());
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            scanPreview.getHolder().addCallback(this);
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface) {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public Rect getCropRect() {
        return mCropRect;
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new QRHandler(cameraManager, this, dataProvider);
            }
            Point resolution = cameraManager.getCameraResolution();
            mCropRect = mQRCallback.getRect(resolution.x, resolution.y);
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            mQRCallback.displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            mQRCallback.displayFrameworkBugMessageAndExit();
        }
    }

    /**
     * 内部代理类
     */
    public static class Proxy {

        private CameraController c;

        private Proxy() {}

        private void with(@NonNull SurfaceView surfaceView) {
            c = new CameraController(surfaceView);
        }

        public CameraController build(@NonNull QRCallback callback, ICameraDataProvider dataProvider) {
            c.setQRCallback(callback);
            c.setDataProvider(dataProvider);
            return c;
        }

        public CameraController build(@NonNull QRCallback callback) {
            c.setQRCallback(callback);
            c.setDataProvider(new AllDecode());
            return c;
        }

    }

}
