package com.qr;

import android.hardware.Camera;
import android.os.Handler;

public interface ICameraDataProvider extends Camera.PreviewCallback {

    void init(Handler handler, ICameraP i);

    void decode(byte[] data, Camera.Size size, int width, int height, ICameraP i);

    boolean needAutoFocus();

}
