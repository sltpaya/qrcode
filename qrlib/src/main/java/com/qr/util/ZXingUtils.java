package com.qr.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.qr.decode.DecodeFormatManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Author：SLTPAYA
 * Date：2017/12/6 14:03
 */
public class ZXingUtils {

    public static final int ALL_MODE = 0X300;
    public static final int QRCODE_MODE = 0X200;
    public static final int BARCODE_MODE = 0X100;

    public static Map<DecodeHintType, Object> setMode(int decodeMode) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

        Collection<BarcodeFormat> decodeFormats = new ArrayList<>();
        decodeFormats.addAll(EnumSet.of(BarcodeFormat.AZTEC));
        decodeFormats.addAll(EnumSet.of(BarcodeFormat.PDF_417));

        switch (decodeMode) {
            case BARCODE_MODE:
                decodeFormats.addAll(DecodeFormatManager.getBarCodeFormats());
                break;

            case QRCODE_MODE:
                decodeFormats.addAll(DecodeFormatManager.getQrCodeFormats());
                break;

            case ALL_MODE:
                decodeFormats.addAll(DecodeFormatManager.getBarCodeFormats());
                decodeFormats.addAll(DecodeFormatManager.getQrCodeFormats());
                break;

            default:
                break;
        }

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        return hints;
    }
}
