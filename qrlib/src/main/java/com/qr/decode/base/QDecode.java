package com.qr.decode.base;

import android.graphics.Point;
import android.hardware.Camera;
import com.qr.ICameraP;

/**
 * Author：SLTPAYA
 * Date：2017/11/24 15:30
 */
public abstract class QDecode implements Camera.PreviewCallback {

    public static boolean needDecode = true;
    protected final ICameraP i;

    public QDecode(ICameraP i) {
        this.i = i;
    }

    static void reset() {
        needDecode = true;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (QDecode.needDecode) {
            QDecode.needDecode = false;
            Point cameraResolution = i.getCameraManager().getCameraResolution();
            decode(data, camera.getParameters().getPreviewSize(), cameraResolution.x, cameraResolution.y);
        }
    }
    protected abstract void decode(byte[] data, Camera.Size size, int width, int height);

}
