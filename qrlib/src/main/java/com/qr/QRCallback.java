package com.qr;

import android.graphics.Rect;

public interface QRCallback {

    void handleDecode(QResult rawResult);

    void luminance(int value);

    void displayFrameworkBugMessageAndExit();

    Rect getRect(int x, int y);

}
