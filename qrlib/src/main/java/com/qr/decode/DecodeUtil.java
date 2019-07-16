package com.qr.decode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.qr.QResult;
import com.qr.util.BitmapUtils;
import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import java.io.FileNotFoundException;

/**
 * Author：SLTPAYA
 * Date：2017/11/17 16:42
 */
public class DecodeUtil {

    private static ImageScanner mImageScanner;

    static {
        mImageScanner = new ImageScanner();
        mImageScanner.setConfig(0, Config.X_DENSITY, 3);
        mImageScanner.setConfig(0, Config.Y_DENSITY, 3);
    }

    /**
     * 从Uri中解析出B
     *
     * @param context Context
     * @param uri     Uri
     * @return Bitmap, may be null, if the uri error
     */
    public static Bitmap decodeUriAsBitmap(Context context,Uri uri) {
        Bitmap bitmap = null;
        try {
            bitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    public interface DecodeCallback {

        /**
         * URI解析Bitmap错误
         */
        void onUriFailed();

        /**
         * 扫描无结果
         */
        void onNonResult();

        /**
         * 扫描结果
         *
         * @param result 结果
         */
        void onResult(QResult result);

    }

    private static DecodeCallback mCallback;

    private static Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (mCallback != null) {
                switch (msg.what) {
                    case 0:
                        mCallback.onResult((QResult) msg.obj);
                        break;
                    case 1:
                        mCallback.onNonResult();
                        break;
                }
            }
        }
    };

    public static void decodeQRCode(Uri uri, Context context, final DecodeCallback callback) {
        mCallback = callback;
        final Bitmap bitmapChoose = DecodeUtil.decodeUriAsBitmap(context, uri);
        if (bitmapChoose != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final QResult result = DecodeUtil.decodeQRCode(bitmapChoose);
                    if (!result.isEmpty()) {
                        Message.obtain(handler, 0, result).sendToTarget();
                    } else {
                        Message.obtain(handler, 1).sendToTarget();
                    }
                }
            }).start();
        } else {
            callback.onUriFailed();
        }
    }


    /**
     * 由Bitmap解码二维码内容
     * 当识别结果为空的时候, 返回结果字符为null
     *
     * @param bitmap Bitmap
     * @return QResult
     */
    public static QResult decodeQRCode(Bitmap bitmap) {
        try {
            byte[] data = BitmapUtils.getYUVByBitmap(bitmap);

            Image barcode = new Image(bitmap.getWidth(), bitmap.getHeight(), "Y800");
            barcode.setData(data);

            int result = mImageScanner.scanImage(barcode);

            String rawResult = null;
            if (result != 0) {
                SymbolSet symSet = mImageScanner.getResults();
                for (Symbol sym : symSet)
                    rawResult = sym.getData();
            }
            return new QResult(rawResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new QResult(null);
    }



}
