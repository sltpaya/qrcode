package com.qr;

import android.text.TextUtils;

/**
 * Author：SLTPAYA
 * Date：2017/11/17 11:23
 */
public final class QResult {

    private final String text;

    private int bright = -1;

    public boolean handler = false;

    public QResult(String text) {
        this.text = text;
    }

    public int getBright() {
        return bright;
    }

    public void setBright(int bright) {
        this.bright = bright;
    }

    public String getText() {
        return text;
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(text);
    }

    public boolean hasBright() {
        return handler && bright != -1;
    }

}
