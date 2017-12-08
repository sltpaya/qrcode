package com.qr.camera;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceHolder;
import com.qr.camera.focus.AutoFocusManager;
import com.qr.camera.focus.QRFocusCallback;
import com.qr.camera.open.OpenCameraInterface;
import java.io.IOException;

/**
 * Author：SLTPAYA
 * Date：2017/11/24 15:17
 */
public class CameraManager {

    private final String TAG = getClass().getSimpleName();
    private Camera camera;
    private int requestedCameraId = -1;
    private boolean initialized;
    private boolean previewing;
    private final CameraConfiguration configManager;
    private AutoFocusManager autoFocusManager;
    private QRFocusCallback focusCallback;
    private Context mContext;

    public Context getContext() {
        return mContext;
    }

    public CameraManager(Context context) {
        mContext = context;
        this.configManager = new CameraConfiguration(context);
    }

    /**
     * 开启相机
     */
    public synchronized void openDriver(SurfaceHolder holder) throws IOException {
        Camera theCamera = camera;
        if (theCamera == null) {

            if (requestedCameraId >= 0) {
                theCamera = OpenCameraInterface.open(requestedCameraId);
            } else {
                theCamera = OpenCameraInterface.open();
            }

            if (theCamera == null) {
                throw new IOException();
            }
            camera = theCamera;
        }

        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        theCamera.setPreviewDisplay(holder);

        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(theCamera);
        }

        Camera.Parameters parameters = theCamera.getParameters();
        String parametersFlattened = parameters == null ? null : parameters.flatten(); // Save
        try {
            configManager.setDesiredCameraParameters(theCamera, false);
        } catch (RuntimeException re) {
            // Driver failed
            Log.w(TAG, "Camera rejected parameters. Setting only minimal safe-mode parameters");
            if (parametersFlattened != null) {
                parameters = theCamera.getParameters();
                parameters.unflatten(parametersFlattened);
                try {
                    theCamera.setParameters(parameters);
                    configManager.setDesiredCameraParameters(theCamera, true);
                } catch (RuntimeException re2) {
                    // Well, darn. Give up
                    Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration");
                }
            }
        }
    }


    /**
     * 关闭相机
     */
    public synchronized void closeDriver() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            autoFocusManager.stop();
            camera.release();
            camera = null;
        }
    }


    /**
     * 相机是否开启
     * @return boolean
     */
    public synchronized boolean isOpen() {
        return camera != null;
    }

    /**
     * 开启预览
     */
    public synchronized void startPreview(Camera.PreviewCallback previewCallback) {
        Camera theCamera = camera;
        if (theCamera != null && !previewing) {
            camera.setPreviewCallback(previewCallback);
            theCamera.startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(camera, focusCallback);
        }
    }

    /**
     * 停止预览
     */
    public synchronized void stopPreview() {
        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }
        if (camera != null && previewing) {
            camera.stopPreview();
            try {
                camera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            previewing = false;
        }
    }

    public synchronized void onFocus() {
        if (autoFocusManager != null) {
            autoFocusManager.start();
        }
    }

    public void setAutoFocusCallback(@Nullable QRFocusCallback focusCallback) {
        this.focusCallback = focusCallback;
    }

    /**
     * Allows third party apps to specify the camera ID, rather than determine
     * it automatically based on available cameras and their orientation.
     *
     * @param cameraId camera ID of the camera to use. A negative value means
     *                 "no preference".
     */
    public synchronized void setManualCameraId(int cameraId) {
        requestedCameraId = cameraId;
    }

    /**
     * Update the status of the flash
     * if the flash status is turn on then turn off
     */
    public void updateFlash(IFlashStatusCallback callback) {
        boolean status;
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            String flashMode = parameters.getFlashMode();
            if (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                status = false;
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                status = true;
            }
            camera.setParameters(parameters);
            if (callback != null) {
                if (status) {
                    callback.on();
                    return;
                }
                callback.off();
            }
        }
    }

    public void updateFlash() {
        updateFlash(null);
    }

    /**
     * 获取相机分辨率
     * @return Point
     */
    public Point getCameraResolution() {
        return configManager.getCameraResolution();
    }

    public Point getScreenResolution() {
        return configManager.getScreenResolution();
    }

    /**
     * 获取相机预览尺寸大小
     */
    public Camera.Size getPreviewSize() {
        if (null != camera) {
            return camera.getParameters().getPreviewSize();
        }
        return null;
    }

    /**
     * 手电筒接口
     */
    public interface IFlashStatusCallback {
        void on();
        void off();
    }


}
