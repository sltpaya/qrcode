package com.qr.camera.focus;

import android.os.AsyncTask;

/**
 * Author：SLTPAYA
 * Date：2017/11/24 17:09
 */
public class AutoFocusTask extends AsyncTask<Object, Object, Object> {

    private final AutoFocusManager mAutoFocusManager;

    public AutoFocusTask(AutoFocusManager autoFocusManager) {
        mAutoFocusManager = autoFocusManager;
    }

    @Override
    protected Object doInBackground(Object... objects) {
        try {
            Thread.sleep(AutoFocusManager.AUTO_FOCUS_INTERVAL_MS);
        } catch (InterruptedException e) {
            // continue
        }
        mAutoFocusManager.start();
        return null;
    }
}
