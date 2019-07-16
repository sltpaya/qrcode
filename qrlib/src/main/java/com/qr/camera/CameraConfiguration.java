package com.qr.camera;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.qr.decode.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public final class CameraConfiguration {

    private static final String TAG = "CameraConfiguration";

    private static final int MIN_PREVIEW_PIXELS = 480 * 320;
    private static final double MAX_ASPECT_DISTORTION = 0.15;
    private final Context context;
    private Point screenResolution;
    private Point cameraResolution;

    /**
     * 设备方向
     */
    private int mOrientation;

    private Size mSurfaceSize;

    public CameraConfiguration(Context context) {
        this.context = context;
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            mOrientation = windowManager.getDefaultDisplay().getRotation();
        }
    }

    public Size getSurfaceSize() {
        return mSurfaceSize;
    }

    public int getOrientation() {
        return mOrientation;
    }

    @SuppressWarnings("SuspiciousNameCombination")
    public void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        assert manager != null;
        Display display = manager.getDefaultDisplay();

        parameters.setPreviewFpsRange(20, 40);
        parameters.setJpegQuality(100);
        parameters.setPictureFormat(ImageFormat.JPEG);

        screenResolution = getDisplaySize(display);

        Point screenResolutionForCamera = new Point();
        screenResolutionForCamera.x = screenResolution.x;
        screenResolutionForCamera.y = screenResolution.y;

        // Convert to vertical screen.
        if (screenResolution.x < screenResolution.y) {
            screenResolutionForCamera.x = screenResolution.y;
            screenResolutionForCamera.y = screenResolution.x;
        }

        cameraResolution = findBestPreviewSizeValue(parameters, screenResolutionForCamera);
    }

    public Size initSurfaceSize(List<Camera.Size> sizeList, int previewWidth, int previewHeight) {
        if (mOrientation == Surface.ROTATION_0 || mOrientation == Surface.ROTATION_180) {
            mSurfaceSize = getBigEnoughSize(sizeList, previewWidth, previewHeight);
        } else {
            mSurfaceSize = getBigEnoughSize(sizeList, previewHeight, previewWidth);
        }
        Log.d(TAG, getClass().getName() + ".initSurfaceSize() mSurfaceSize = " + mSurfaceSize.toString());
        return mSurfaceSize;
    }

    /**
     * 返回sizes中宽高大于最小宽高的最小尺寸
     */
    private Size getBigEnoughSize(List<Camera.Size> sizeList, int minWidth, int minHeight) {
        Camera.Size curSize = sizeList.get(0);
        boolean curBigEnough = curSize.width >= minWidth && curSize.height >= minHeight;
        for (int i = 1; i < sizeList.size(); i++) {
            Camera.Size nextSize = sizeList.get(i);
            boolean nextBigEnough = nextSize.width >= minWidth && nextSize.height >= minHeight;
            if (!curBigEnough && nextBigEnough) {//curSize尺寸不够，nextSize够
                curBigEnough = true;
                curSize = nextSize;
            } else if (curBigEnough ^ !nextBigEnough) {//curSize与nextSize尺寸同够或同不够
                long curPixels = (long) curSize.width * curSize.height;
                long nextPixels = (long) nextSize.width * nextSize.height;
                if (curBigEnough ^ (curPixels < nextPixels)) {//尺寸同够且curSize不小于nextSize 或 尺寸同不够且curSize小于nextSize
                    curSize = nextSize;
                }
            }
        }
        return new Size(curSize.width, curSize.height);
    }

    private Point getDisplaySize(final Display display) {
        final Point point = new Point();
        display.getSize(point);
        return point;
    }

    public void setDesiredCameraParameters(Camera camera, boolean safeMode) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.");
            return;
        }

        parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
        camera.setParameters(parameters);

        Camera.Parameters afterParameters = camera.getParameters();
        Camera.Size afterSize = afterParameters.getPreviewSize();
        if (afterSize != null && (cameraResolution.x != afterSize.width || cameraResolution.y != afterSize.height)) {
            cameraResolution.x = afterSize.width;
            cameraResolution.y = afterSize.height;
        }
        camera.setDisplayOrientation(90);
    }

    /**
     * Camera resolution.
     *
     * @return {@link Point}.
     */
    public Point getCameraResolution() {
        return cameraResolution;
    }

    /**
     * Screen resolution.
     *
     * @return {@link Point}.
     */
    public Point getScreenResolution() {
        return screenResolution;
    }

    /**
     * Calculate the preview interface size.
     *
     * @param parameters       camera params.
     * @param screenResolution screen resolution.
     * @return {@link Point}.
     */
    private Point findBestPreviewSizeValue(Camera.Parameters parameters, Point screenResolution) {
        List<Camera.Size> rawSupportedSizes = parameters.getSupportedPreviewSizes();
        if (rawSupportedSizes == null) {
            Log.w(TAG, "Device returned no supported preview sizes; using default");
            Camera.Size defaultSize = parameters.getPreviewSize();
            return new Point(defaultSize.width, defaultSize.height);
        }

        // Sort by size, descending
        List<Camera.Size> supportedPreviewSizes = new ArrayList<>(rawSupportedSizes);
        Collections.sort(supportedPreviewSizes, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size a, Camera.Size b) {
                int aPixels = a.height * a.width;
                int bPixels = b.height * b.width;
                if (bPixels < aPixels) {
                    return -1;
                }
                if (bPixels > aPixels) {
                    return 1;
                }
                return 0;
            }
        });

        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size supportedPreviewSize : supportedPreviewSizes) {
                previewSizesString.append(supportedPreviewSize.width)
                        .append('x')
                        .append(supportedPreviewSize.height)
                        .append(' ');
            }
            Log.i(TAG, "Supported preview sizes: " + previewSizesString);
        }

        double screenAspectRatio = (double) screenResolution.x / (double) screenResolution.y;

        // Remove sizes that are unsuitable
        Iterator<Camera.Size> it = supportedPreviewSizes.iterator();
        while (it.hasNext()) {
            Camera.Size supportedPreviewSize = it.next();
            int realWidth = supportedPreviewSize.width;
            int realHeight = supportedPreviewSize.height;
            if (realWidth * realHeight < MIN_PREVIEW_PIXELS) {
                it.remove();
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realHeight : realWidth;
            int maybeFlippedHeight = isCandidatePortrait ? realWidth : realHeight;

            double aspectRatio = (double) maybeFlippedWidth / (double) maybeFlippedHeight;
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            if (distortion > MAX_ASPECT_DISTORTION) {
                it.remove();
                continue;
            }

            if (maybeFlippedWidth == screenResolution.x && maybeFlippedHeight == screenResolution.y) {
                Point exactPoint = new Point(realWidth, realHeight);
                Log.d(TAG, "Found preview size exactly matching screen size: " + exactPoint);
                return exactPoint;
            }
        }

        // If no exact match, use largest preview size. This was not a great
        // idea on older devices because
        // of the additional computation needed. We're likely to get here on
        // newer Android 4+ devices, where
        // the CPU is much more powerful.
        if (!supportedPreviewSizes.isEmpty()) {
            Camera.Size largestPreview = supportedPreviewSizes.get(0);
            Point largestSize = new Point(largestPreview.width, largestPreview.height);
            Log.d(TAG, "Using largest suitable preview size: " + largestSize);
            return largestSize;
        }

        // If there is nothing at all suitable, return current preview size
        Camera.Size defaultPreview = parameters.getPreviewSize();
        Point defaultSize = new Point(defaultPreview.width, defaultPreview.height);
        Log.d(TAG, "No suitable preview sizes, using default: " + defaultSize);

        return defaultSize;
    }
}
