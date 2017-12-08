package com.qr.util;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Author：SLTPAYA
 * Date：2017/11/21 10:26
 */
public class BitmapUtils {

    private final static int THUMBNAIL_SCALE_FACTOR = 2;

    /**
     * 计算光源亮度值
     *
     * @param colors colors
     * @return value
     */
    private static int computeLuminance(int[] colors) {
        int r, g, b;
        float bright = 0;
        for (int color : colors) {
            r = (color | 0xff00ffff) >> 16 & 0x00ff;
            g = (color | 0xffff00ff) >> 8  & 0x0000ff;
            b = (color | 0xffffff00)       & 0x0000ff;
            bright += 0.1f * (3 * (r + 2 * g) + b);
        }
        return (int) (bright / colors.length);
    }

    /**
     * 计算取景框亮度值
     *
     * @param yuv       yuv
     * @param rect      取景框Rect[注意： 摄像头取得是横屏数据， 取景框位置是相对于竖直屏幕位置而言， 所以把left-->top, top-->left， 此处取景框的宽高一致，无所谓]
     * @param dataWidth Camera.Size 的宽度
     * @return value
     */
    public static int getLuminanceByThumbnail(byte[] yuv, Rect rect, int dataWidth) {
        return getLuminanceByThumbnail(yuv, rect.width(), rect.height(), rect.left, rect.top, dataWidth);
    }

    /**
     * 计算取景框亮度值
     *
     * @param yuv       yuv
     * @param width     取景框宽
     * @param height    取景框高
     * @param top       取景框Rect的top
     * @param left      取景框Rect的left
     * @param dataWidth Camera.Size 的宽度
     * @return value
     */
    public static int getLuminanceByThumbnail(byte[] yuv, int width, int height, int top, int left, int dataWidth) {
        return computeLuminance(render(yuv, width, height, top, left, dataWidth));
    }

    /**
     * 将yuv源渲染成colors， bitmap像素数组
     *
     * @param yuv       yuv
     * @param width     取景框宽
     * @param height    取景框高
     * @param top       取景框Rect的top
     * @param left      取景框Rect的left
     * @param dataWidth Camera.Size 的宽度
     * @return colors
     */
    private static int[] render(byte[] yuv, int width, int height, int top, int left, int dataWidth) {
        width = width / THUMBNAIL_SCALE_FACTOR;
        height = height / THUMBNAIL_SCALE_FACTOR;
        int[] pixels = new int[width * height];
        int inputOffset = top * dataWidth + left;

        for (int y = 0; y < height; y++) {
            int outputOffset = y * width;
            for (int x = 0; x < width; x++) {
                int grey = yuv[inputOffset + x * THUMBNAIL_SCALE_FACTOR] & 0xff;
                pixels[outputOffset + x] = 0xFF000000 | (grey * 0x00010101);
            }
            inputOffset += dataWidth * THUMBNAIL_SCALE_FACTOR;
        }
        return pixels;
    }

    /**
     * 获取位图的YUV数据
     *
     * @param bitmap Bitmap
     * @return yuv bytes
     */
    public static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int size = width * height;

        int pixels[] = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        return rgb2YCbCr420(pixels, width, height);
    }

    private static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // 屏蔽ARGB的透明度值
                int rgb = pixels[i * width + j] & 0x00FFFFFF;
                // 像素的颜色顺序为bgr，移位运算。
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // 套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                // 调整
                y = y < 16 ? 16 : (y > 255 ? 255 : y);
                u = u < 0 ? 0 : (u > 255 ? 255 : u);
                v = v < 0 ? 0 : (v > 255 ? 255 : v);
                // 赋值
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1)] = (byte) u;
                yuv[len + (i >> 1) * width + (j & ~1)] = (byte) v;
            }
        }
        return yuv;
    }

}
