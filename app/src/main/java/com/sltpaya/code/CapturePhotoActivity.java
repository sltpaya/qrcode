package com.sltpaya.code;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.qr.CameraController;
import com.qr.DefaultCameraDataProvider;
import com.qr.QRCallback;
import com.qr.QResult;
import com.qr.camera.CameraManager;
import com.qr.decode.DecodeUtil;
import com.qr.decode.Size;
import com.sltpaya.code.view.FrameView;
import com.sltpaya.code.view.ProgressDialog;

import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;

/**
 * Author：SLTPAYA
 * Date：2017/11/13 10:01
 * 扫码Activity
 */
public class CapturePhotoActivity extends AppCompatActivity implements QRCallback {

    public static final String KEY_RESULT = "RESULT";
    private final static int S_GALLERY_CODE = 1000;
    private final String TAG = getClass().getSimpleName();
    public boolean mIsGalleryDecode = false;    //the decode bitmap is from gallery?
    private TextView mTag;
    private ViewGroup fl;
    private TextView mTakeTv;
    private FrameView frameView;
    private SurfaceView mSurfaceView;
    private CameraController cameraController;
    private Pattern urlPattern;
    private ProgressDialog mProgressDialog;
    private ImageView previewIv;

    {
        urlPattern = Pattern.compile("^((https?|ftp|file|HTTPS?)://)[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]");
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        //Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_photo);
        //Initialize the View
        initView();
        initListener();
        initCapture();
    }

    private void initView() {
        fl = findViewById(R.id.rl);
        mTag = findViewById(R.id.tag);
        frameView = findViewById(R.id.frame);
        mTakeTv = findViewById(R.id.flash);
        mSurfaceView = findViewById(R.id.surface);
        previewIv = findViewById(R.id.previewIv);
    }

    private void initListener() {
        findViewById(R.id.back).setOnClickListener(v -> finish());
        findViewById(R.id.gallery).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_PICK);
            resetFlash();/*进入相册之前如果闪关灯是打开的,就关闭*/
            startActivityForResult(intent, S_GALLERY_CODE);
        });

        /*触摸之后手动对焦*/
        frameView.setOnClickListener(v -> {
            CameraManager manager = cameraController.getCameraManager();
            if (manager != null) {
                manager.onFocus();
            }
        });
        mTakeTv.setOnClickListener(v -> {
            CameraManager manager = cameraController.getCameraManager();
            if (manager != null) {
                manager.getCamera().takePicture(
                        null, null, new TakePictureCallback()
                );
            }
        });
    }

    private final class TakePictureCallback implements Camera.PictureCallback {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            //将图片保存至相册
//            if (y > 0) {
                System.out.println("竖屏  竖屏  竖屏  竖屏  竖屏  竖屏  ");
                //ContentResolver resolver = getContentResolver();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                Matrix matrix = new Matrix();
//                matrix.setRotate(90);
//                Bitmap bitmap2 = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);


            CameraManager cameraManager = cameraController.getCameraManager();
            Point screenResolution = cameraManager.getScreenResolution();//1080 1808

            Point cameraResolution = cameraManager.getCameraResolution();//1920 1080
            int width = cameraResolution.x;
            int height = cameraResolution.y;

            RectF processRect = processRect(Size.toSize(screenResolution), Size.toSize(cameraResolution), cameraManager.getConfigManager().getOrientation());

            Rect mRect = new Rect();

            if (!processRect.isEmpty()) {
                int frameLeft =   (int) (processRect.left     * width);
                int frameTop =    (int) (processRect.top      * height);
                int frameWidth =  (int) (processRect.width()  * width);
                int frameHeight = (int) (processRect.height() * height);
                mRect = new Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight);
            }

            mRect.set((int)(mRect.left*0.16F), (int)(mRect.top*0.16F), (int)(mRect.right*0.16F), (int)(mRect.bottom*0.16F));

            Bitmap bitmap1 = Bitmap.createBitmap(bitmap, mRect.left, mRect.top, mRect.width(), mRect.height());

            SaveImg.saveImg(bitmap1, System.currentTimeMillis() + ".jpg", CapturePhotoActivity.this);
            //MediaStore.Images.Media.insertImage(resolver, bitmap, "xxoo", "des");
//            } else {
//                System.out.println("横屏 横屏 横屏 横屏 横屏 横屏 ");
//                ContentResolver resolver = getContentResolver();
//                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                MediaStore.Images.Media.insertImage(resolver, bitmap, "xxoo", "des");
//            }

//            Toast.makeText(CapturePhotoActivity.this, "拍照成功", Toast.LENGTH_SHORT).show();
//            if (data != null && data.length > 0) {
////                BitmapFactory.Options options = new BitmapFactory.Options();
////
////                options.inJustDecodeBounds = true;
//
//                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//
//
//                SaveImg.saveImg(bitmap, System.currentTimeMillis() + ".jpg", CapturePhotoActivity.this);
                //bitmap = rotateBimap(CapturePhotoActivity.this, 90, bitmap);

//                bitmap = zoomImg(bitmap, 0, 0);
                previewIv.setImageBitmap(bitmap);
//            }
        }
    }

    public Rect getCropRect() {
        return new Rect(frameView.getLeft(), frameView.getTop(), frameView.getRight(), frameView.getBottom());
    }

    //CameraManager cameraManager = cameraController.getCameraManager();
    //Point screenResolution = cameraManager.getScreenResolution();//1080 1808

    //Point cameraResolution = cameraManager.getCameraResolution();//1920 1080

    private void process(Camera camera, int orientation, Point cameraResolution, Point screenResolution) {
        RectF processRect = processRect(Size.toSize(screenResolution),
                Size.toSize(cameraResolution), orientation);

        Rect mRect = new Rect();

        if (!processRect.isEmpty()) {
            int width = cameraResolution.x;
            int height = cameraResolution.y;
            int frameLeft =   (int) (processRect.left     * width);
            int frameTop =    (int) (processRect.top      * height);
            int frameWidth =  (int) (processRect.width()  * width);
            int frameHeight = (int) (processRect.height() * height);
            mRect = new Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight);
        }
    }

    private RectF processRect(Size previewSize, Size surfaceSize, int orientation) {
        int previewWidth = previewSize.width;
        int previewHeight = previewSize.height;

        int surfaceWidth = surfaceSize.width;
        int surfaceHeight = surfaceSize.height;

        Rect rect = getCropRect();
        RectF rectF = new RectF(rect.left, rect.top, rect.right, rect.bottom);
        int frameLeft = rect.left;
        int frameTop = rect.top;
        int frameRight = rect.right;
        int frameBottom = rect.bottom;
        if (frameLeft >= frameRight || frameTop >= frameBottom) {
            rectF.setEmpty();
            return rectF;
        }
        //交换宽高
        if (orientation % 2 == 0) {
            int temp = surfaceWidth;
            surfaceWidth = surfaceHeight;
            surfaceHeight = temp;
        }
        float ratio;//图像帧的缩放比，比如1000*2000像素的图像显示在100*200的View上，缩放比就是10
        if (previewWidth * surfaceHeight < surfaceWidth * previewHeight) {//图像帧的宽超出了View的左边，以高计算缩放比例
            ratio = 1F * surfaceHeight / previewHeight;
        } else {//图像帧的高超出了View的底边，以宽计算缩放比例
            ratio = 1F * surfaceWidth / previewWidth;
        }
        float leftRatio = Math.max(0, Math.min(1, ratio * frameLeft / surfaceWidth));//计算扫描框的左边在图像帧中所处的位置
        float rightRatio = Math.max(0, Math.min(1, ratio * frameRight / surfaceWidth));//计算扫描框的右边在图像帧中所处的位置
        float topRatio = Math.max(0, Math.min(1, ratio * frameTop / surfaceHeight));//计算扫描框的顶边在图像帧中所处的位置
        float bottomRatio = Math.max(0, Math.min(1, ratio * frameBottom / surfaceHeight));//计算扫描框的底边在图像帧中所处的位置
        switch (orientation) {//根据旋转角度对位置进行校正
            case Surface.ROTATION_0: {
                rectF.set(topRatio, 1 - rightRatio, bottomRatio, 1 - leftRatio);
                break;
            }
            case Surface.ROTATION_90: {
                rectF.set(leftRatio, topRatio, rightRatio, bottomRatio);
                break;
            }
            case Surface.ROTATION_180: {
                rectF.set(1 - bottomRatio, leftRatio, 1 - topRatio, rightRatio);
                break;
            }
            case Surface.ROTATION_270: {
                rectF.set(1 - rightRatio, 1 - bottomRatio, 1 - leftRatio, 1 - topRatio);
                break;
            }
        }
        return rectF;
    }

    public Bitmap rotateBimap(Context context, float degree, Bitmap srcBitmap) {
        Matrix matrix = new Matrix();
        matrix.reset();
        matrix.setRotate(degree);
        Bitmap bitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight()
                , matrix, true);
        return bitmap;
    }

    public static Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
//        float scaleWidth = ((float) newWidth) / width;
        float scaleWidth = 0.5f;
//        float scaleHeight = ((float) newHeight) / height;
        float scaleHeight = 0.5f;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 初始化二维码识别组件
     */
    private void initCapture() {
        cameraController = CameraController.with(mSurfaceView).build(this, new DefaultCameraDataProvider());
    }

    @Override
    protected void onResume() {
        cameraController.onResume();
        if (mIsGalleryDecode) {
            //正在识别相册二维码, 此时不预览摄像头数据
            mSurfaceView.getHolder().removeCallback(cameraController);
            //不开启扫描动画
            frameView.stopScan();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        dismissProgress();
        cameraController.onPause();
        frameView.stopScan();
        super.onPause();
    }

    /**
     * The flashing lights are on and off
     */
    private void resetFlash() {

    }

    @Override
    public void handleDecode(QResult rawResult) {
        resetFlash();/*The flashing lights are on and off*/

        String text = rawResult.getText();
        /*跳转结果页*/
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra(KEY_RESULT, text);
        startActivity(intent);
        finish();
    }

    @Override
    public void luminance(int value) {

    }

    @Override
    public void displayFrameworkBugMessageAndExit() {
        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(CapturePhotoActivity.this);
        builder.setTitle("错误提示");
        builder.setMessage("开启相机失败, 请检查相机是否占用或有无权限");
        builder.setPositiveButton("好的", (dialog, which) -> finish());
        builder.setOnCancelListener(dialog -> finish());
        builder.show();
    }

    @Override
    @SuppressWarnings("SuspiciousNameCombination")
    public Rect getRect(int cameraHeight, int cameraWidth) {
        return new Rect(frameView.getLeft(), frameView.getTop(), frameView.getRight(), frameView.getBottom());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;
        if (requestCode == S_GALLERY_CODE) {
            if (data == null || (uri = data.getData()) == null) {
                return;
            }
            handleGallery(uri);
        }
    }

    private void handleGallery(@NonNull Uri uri) {
        showProgress();
        mTag.setText("正在识别中...");
        mIsGalleryDecode = true;
        DecodeUtil.decodeQRCode(uri, CapturePhotoActivity.this, new DecodeUtil.DecodeCallback() {
            @Override
            public void onUriFailed() {
                dismissProgress();
                mIsGalleryDecode = false;//相册图片错误
                Toast.makeText(CapturePhotoActivity.this, "相册图片错误", Toast.LENGTH_SHORT).show();
                restartCamera();
            }

            @Override
            public void onNonResult() {
                dismissProgress();
                showNull();
                mIsGalleryDecode = false;
            }

            @Override
            public void onResult(final QResult result) {
                dismissProgress();
                handleDecode(result);
                mIsGalleryDecode = false;
            }
        });
    }

    /**
     * 显示处理进度条
     */
    private void showProgress() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog();
        }
        mProgressDialog.show(getFragmentManager(), TAG);
    }

    /**
     * 关闭处理进度条
     */
    private void dismissProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * 显示无二维码结果dialog
     */
    private void showNull() {
        AlertDialog.Builder builder = new AlertDialog.Builder(CapturePhotoActivity.this);
        builder.setTitle("结果");
        builder.setMessage("未识别到二维码");
        builder.setPositiveButton("好的", (dialog, which) -> restartCamera());
        builder.setOnCancelListener(dialog -> restartCamera());
        builder.show();
    }

    /**
     * 重新预览摄像头数据
     */
    private void restartCamera() {
        cameraController.onResumeCamera();
        mTag.setText(null);
    }

}

