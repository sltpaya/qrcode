package com.qr;

import android.graphics.Rect;
import android.os.Handler;

import com.qr.camera.CameraManager;

/**
 * Author: SLTPAYA
 * Date: 2017/11/4
 */
public interface ICameraP {

    Handler getHandler();

    Rect getCropRect();

    CameraManager getCameraManager();

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     *
     * @param rawResult The contents of the barcode.
     */
    void handleDecode(QResult rawResult);

    /**
     * The luminance value of the camera view
     * @param value int
     */
    void luminance(int value);

}
