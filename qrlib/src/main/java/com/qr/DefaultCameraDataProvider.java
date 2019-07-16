package com.qr;

import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;

public class DefaultCameraDataProvider implements ICameraDataProvider {

    protected Handler handler;
    protected ICameraP i;

    @Override
    public void init(Handler handler, ICameraP i) {
        this.handler = handler;
        this.i = i;
    }

    @Override
    public void decode(byte[] data, Camera.Size size, int width, int height, ICameraP i) {

    }

    @Override
    public boolean needAutoFocus() {
        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Point cameraResolution = i.getCameraManager().getCameraResolution();
        decode(data, camera.getParameters().getPreviewSize(), cameraResolution.x, cameraResolution.y, i);
    }

}
