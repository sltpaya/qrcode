package com.qr.decode;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.qr.ICameraP;
import com.qr.LuminanceLevel;
import com.qr.QResult;
import com.qr.decode.base.QDecode;
import com.qr.util.BitmapUtils;
import com.qr.util.ZXingUtils;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author：SLTPAYA
 * Date：2017/12/6 15:25
 */
@SuppressWarnings("SuspiciousNameCombination")
public class AllDecode extends QDecode {

    private Handler mHandler;
    private Camera.Size mSize;
    private ImageScanner mImageScanner;
    private MultiFormatReader multiFormatReader;

    private Rect mRect;
    private byte[] mData;
    private Image barcode;
    private boolean isInitData = false;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    public AllDecode(Handler handler, ICameraP i) {
        super(i);
        mHandler = handler;
        initComponents();
    }

    /**
     * 初始化组扫描组件
     */
    private void initComponents() {
        //zbar
        mImageScanner = new ImageScanner();
        mImageScanner.setConfig(Config.ENABLE, Config.X_DENSITY, 1);
        mImageScanner.setConfig(Config.ENABLE, Config.Y_DENSITY, 1);
        mImageScanner.enableCache(true);

        //zxing
        multiFormatReader = new MultiFormatReader();
        multiFormatReader.setHints(ZXingUtils.setMode(ZXingUtils.ALL_MODE));
    }

    @Override
    protected void decode(byte[] data, Camera.Size size, int width, int height) {
        QDecode.needDecode = false;
        if (!isInitData) {
            isInitData = true;
            Point cameraResolution = i.getCameraManager().getCameraResolution();
            Point screenResolution = i.getCameraManager().getScreenResolution();
            /*旋转取景框位置*/

            mRect = i.getCropRect();
            mRect.left   *= cameraResolution.y / screenResolution.x;
            mRect.right  *= cameraResolution.y / screenResolution.x;
            mRect.top    *= cameraResolution.x / screenResolution.y;
            mRect.bottom *= cameraResolution.x / screenResolution.y;
        }
        mSize = size;
        mData = data;

        /*构建解码图像*/
        barcode = new Image(mSize.width, mSize.height, Image.TYPE_Y800);
        barcode.setData(mData);
        //[注意： 摄像头取得是横屏数据， 取景框位置是相对于竖直屏幕位置而言， 所以把left-->top, top-->left， 此处取景框的宽高一致，无所谓]
        barcode.setCrop(mRect);

        executorService.execute(mAnalysisTask);
    }

    private boolean isLongTimeLose = false;
    private long lastZxingDecodeTime = 0;
    private static final int ZXING_OVER_TIME = 700;//解码耗时超过700ms, 视为cpu处理能力较弱
    private static final long ZXING_DECODE_INTER = 2000L;//zxing解码间隔, 在长CPU下

    private Runnable mAnalysisTask = new Runnable() {
        @Override
        public void run() {
            String resultStr;

            /*zbar*/
            resultStr = decodeWithZBar();

            /*判断是否需要使用zxing*/
            if (TextUtils.isEmpty(resultStr)) {
                long time = System.currentTimeMillis();

                if (isLongTimeLose) {//cpu能力过低, 开启{ZXING_DECODE_INTER}s间隔解码(zing)
                    if (time - lastZxingDecodeTime > ZXING_DECODE_INTER) {
                        //可以解码
                        lastZxingDecodeTime = time;
                        resultStr = decodeWithZxing();
                    }
                } else {//cpu能力足够的, 持续解码
                    resultStr = decodeWithZxing();
                }

                long lose = System.currentTimeMillis() - time;
                if (lose - ZXING_OVER_TIME > 0 && !isLongTimeLose) {
                    //解码时间太长
                    isLongTimeLose = true;
                }
            }

            /*处理亮度*/
            int bright = BitmapUtils.getLuminanceByThumbnail(mData, mRect, mSize.width);
            boolean handleLuminance = handleLuminance(bright);

            /*亮度消息处理*/
            if (handleLuminance) {
                mHandler.obtainMessage(1, bright).sendToTarget();
            }

            /*结果消息处理*/
            if (!TextUtils.isEmpty(resultStr)) {
                QResult qResult = new QResult(resultStr);
                Message message = mHandler.obtainMessage(0, qResult);
                message.sendToTarget();
            } else {
                QDecode.needDecode = true;
            }
        }
    };

    /**
     * 使用zbar解码
     *
     * @return result
     */
    private String decodeWithZBar() {
        String result = null;
        if (mImageScanner.scanImage(barcode) != 0) {
            SymbolSet symSet = mImageScanner.getResults();
            for (Symbol sym : symSet) {
                if (sym.getType() != Symbol.DATABAR) {
                    result = sym.getData();
                }
            }
        }
        return result;
    }

    /**
     * 使用zxing解码
     *
     * @return result
     */
    private String decodeWithZxing() {
        //zxing
        PlanarYUVLuminanceSource source = buildLuminanceSource(mData, mSize.width, mSize.height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                Result rawResult = multiFormatReader.decodeWithState(bitmap);
                return rawResult == null ? null : rawResult.getText();
            } catch (ReaderException re) {
                // continue
            } finally {
                multiFormatReader.reset();
            }
        }
        return null;
    }

    private LuminanceLevel lastLevel = null;

    /**
     * 处理亮度值是否改变
     * @param luminance int value
     * @return need send luminance message
     */
    private boolean handleLuminance(int luminance) {
        LuminanceLevel nowLevel;
        if (luminance >= 100) {
            nowLevel = LuminanceLevel.ACME;
        } else if (luminance > 60) {
            nowLevel = LuminanceLevel.HIGH;
        } else if (luminance > 30) {
            nowLevel = LuminanceLevel.MIDDLE;
        } else {
            nowLevel = LuminanceLevel.LOW;
        }

        if (lastLevel == null) {
            lastLevel = nowLevel;
            return false;
        }

        if (nowLevel != lastLevel) {
            //需要发送消息
            lastLevel = nowLevel;
            return true;
        }
        return false;
    }

    private PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = mRect;
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.top, rect.left, rect.width(), rect
                .height(), false);
    }

}
