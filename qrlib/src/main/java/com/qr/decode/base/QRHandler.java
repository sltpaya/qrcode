package com.qr.decode.base;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.qr.ICameraP;
import com.qr.QResult;
import com.qr.camera.CameraManager;
import com.qr.decode.AllDecode;

/**
 * Author：SLTPAYA
 * Date：2017/11/24 15:37
 */
public class QRHandler extends Handler {

    private final ICameraP i;

    public QRHandler(CameraManager manager, ICameraP i) {
        super(Looper.getMainLooper());
        this.i = i;
        QDecode.reset();
        //构建解码器并且开启扫描
        QDecode decode = new AllDecode(this, i);
        manager.startPreview(decode);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                i.handleDecode((QResult) msg.obj);
                break;
            case 1:
                i.luminance((Integer) msg.obj);
                break;
        }
    }
}
