package com.qr.decode;

import android.graphics.Point;

public class Size {

    public int width;
    public int height;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static Size toSize(Point point) {
        return new Size(point.x, point.y);
    }

}
