package com.qr.decode.base;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;

import com.qr.ICameraDataProvider;
import com.qr.ICameraP;

/**
 * Author：SLTPAYA
 * Date：2017/11/24 15:30
 */
public abstract class QDecode implements ICameraDataProvider {

    public static volatile boolean needDecode = true;
    protected ICameraP i;

    @Override
    public void init(Handler handler, ICameraP i) {
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
            decode(data, camera.getParameters().getPreviewSize(), cameraResolution.x, cameraResolution.y, i);
        }
    }

    @Override
    public boolean needAutoFocus() {
        return true;
    }
}
