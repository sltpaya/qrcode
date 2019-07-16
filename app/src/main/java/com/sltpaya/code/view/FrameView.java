package com.sltpaya.code.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import com.sltpaya.code.R;


/**
 * Author：SLTPAYA
 * Date：2017/11/3 14:15
 */
public class FrameView extends View {

    private int mCenterY;
    private int mCenterX;
    private int mWidth;
    private int mHeight;

    private Path mPath;
    private Paint mScanPaint;
    private Paint mStrokePaint;
    private Canvas mCanvas;

    private int mLeftColor = 0xe6f76a2b;
    private int mRightColor = 0xe6f76a2b;
    private int mMiddleColor = 0x00f76a2b;
    private int segmentColor = 0xfff76a2b;

    private static final int maxWidth = 200;//最大200dp

    private float segmentLength = 6;    //6dp   边框线长度
    private float segmentWidth = 1;     //1dp 边框线高度
    private float scanWidth = 1;       //1dp 扫描线的高度
    private float mScanY;
    private ValueAnimator mScanAnimator;
    private LinearGradient mGradientLeft;
    private LinearGradient mGradientRight;
    private int mScanLineLeftX;
    private int mScanLineRightX;
    private float mPercent = 0.6f;

    public interface AnimListener {
        void onStart();

        void onFinished();

        void onRepeatEnd();
    }

    private AnimListener animListener;

    public void setAnimListener(AnimListener animListener) {
        this.animListener = animListener;
    }


    public FrameView(Context context) {
        this(context, null);
    }

    public FrameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        segmentWidth = dpConvertPx((int) segmentWidth);
        segmentLength = dpConvertPx((int) segmentLength);
        scanWidth = dpConvertPx((int) scanWidth);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.FrameView, defStyle, 0);

        segmentWidth = array.getDimension(R.styleable.FrameView_segment_width, segmentWidth);
        segmentLength = array.getDimension(R.styleable.FrameView_segment_length, segmentLength);

        segmentColor = array.getColor(R.styleable.FrameView_segment_color, segmentColor);

        mLeftColor = array.getColor(R.styleable.FrameView_left_color, mLeftColor);
        mMiddleColor = array.getColor(R.styleable.FrameView_middle_color, mMiddleColor);
        mRightColor = array.getColor(R.styleable.FrameView_right_color, mRightColor);

        scanWidth = array.getDimension(R.styleable.FrameView_scan_width, scanWidth);
        mPercent = array.getFloat(R.styleable.FrameView_percent, mPercent);
        array.recycle();

        initTools();

    }


    private void initTools() {
        mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setColor(segmentColor);
        mStrokePaint.setStrokeWidth(segmentWidth);

        mScanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mScanPaint.setStrokeWidth(scanWidth);
        mScanPaint.setStyle(Paint.Style.FILL);

        mPath = new Path();
    }

    /**
     * 将dp转换为px
     *
     * @param dp dp值
     * @return 相应的像素
     * @see TypedValue
     */
    private int dpConvertPx(int dp) {
        DisplayMetrics m = getResources().getDisplayMetrics();
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, m) + 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = (int) (dm.widthPixels * mPercent);
        int height = (int) (dm.widthPixels * mPercent);
        setMeasuredDimension(width, height);
    }

    PointF lt;
    PointF lb;
    PointF rt;
    PointF rb;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = (int) (getMeasuredWidth() - getPaddingLeft() - getPaddingRight() - segmentWidth / 2);
        mHeight = (int) (getMeasuredHeight() - getPaddingTop() - getPaddingBottom() - segmentWidth / 2);
        mCenterX = getMeasuredWidth() / 2;
        mCenterY = getMeasuredHeight() / 2;

        //边框
        lt = new PointF(getPaddingLeft() + segmentWidth / 2, segmentWidth / 2 + getPaddingTop());

        lb = new PointF(getPaddingLeft() + segmentWidth / 2, getMeasuredHeight() - getPaddingBottom() - segmentWidth / 2);

        rt = new PointF(getMeasuredWidth() - getPaddingRight() - segmentWidth / 2, segmentWidth / 2 + getPaddingTop());
        rb = new PointF(getMeasuredWidth() - getPaddingRight() - segmentWidth / 2, getMeasuredHeight() - getPaddingBottom() - segmentWidth / 2);

        //扫描线
        mScanLineLeftX = (int) (getPaddingLeft() - segmentWidth / 2);
        mScanLineRightX = (int) (getMeasuredWidth() - getPaddingRight() - segmentWidth / 2);

        mGradientLeft = new LinearGradient(mScanLineLeftX, mScanY, mCenterX, mScanY, mMiddleColor, mLeftColor, Shader.TileMode.CLAMP);
        mGradientRight = new LinearGradient(mCenterX, mScanY, mScanLineRightX, mScanY, mRightColor, mMiddleColor, Shader.TileMode.CLAMP);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mCanvas = canvas;
        drawSegment();
        drawScanLine();
    }

    private long duration = 2 * 1000;

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    /**
     * 开始扫描动画一次
     */
    public void startScan() {
        startScan(0);
    }

    public void startScan(int repeatCount) {
        stopScan();
        mScanAnimator = ValueAnimator.ofFloat(getPaddingTop() + segmentWidth, getMeasuredHeight() - getPaddingBottom() - segmentWidth);
        mScanAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mScanY = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        mScanAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isScan = false;
                if (animListener != null) {
                    animListener.onFinished();
                }
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                if (animListener != null) {
                    animListener.onRepeatEnd();
                }
            }

            @Override
            public void onAnimationStart(Animator animation) {
                isScan = true;
                if (animListener != null) {
                    animListener.onStart();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isScan = false;
                if (animListener != null) {
                    animListener.onFinished();
                }
            }
        });
//        mScanAnimator.setInterpolator(new BounceInterpolator());
        mScanAnimator.setDuration(duration);
        mScanAnimator.setRepeatMode(ValueAnimator.RESTART);
        mScanAnimator.setRepeatCount(repeatCount);
        mScanAnimator.start();
    }

    public void stopScan() {
        if (mScanAnimator != null) {
            mScanAnimator.cancel();
            mScanAnimator = null;
        }
    }

    private boolean isScan = false;

    /**
     * 绘制扫描线
     */
    private void drawScanLine() {
        if (isScan) {
            //左边渐变到中央
            mScanPaint.setShader(mGradientLeft);
            mCanvas.drawLine(mScanLineLeftX, mScanY, mCenterX, mScanY, mScanPaint);
            //右边渐变到中央
            mScanPaint.setShader(mGradientRight);
            mCanvas.drawLine(mCenterX, mScanY, mScanLineRightX, mScanY, mScanPaint);
        }
    }

    /**
     * 绘制边框
     */
    private void drawSegment() {
        mPath.reset();

        //左上角
        mPath.moveTo(lt.x + segmentLength, lt.y);
        mPath.lineTo(lt.x, lt.y);
        mPath.lineTo(lt.x, lt.y + segmentLength);
        //右上角
        mPath.moveTo(rt.x - segmentLength, rt.y);
        mPath.lineTo(rt.x, rt.y);
        mPath.lineTo(rt.x, rt.y + segmentLength);
        //左下角
        mPath.moveTo(lb.x, lb.y - segmentLength);
        mPath.lineTo(lb.x, lb.y);
        mPath.lineTo(lb.x + segmentLength, lb.y);

        //右下角
        mPath.moveTo(rb.x - segmentLength, rb.y);
        mPath.lineTo(rb.x, rb.y);
        mPath.lineTo(rb.x, rb.y - segmentLength);

        mCanvas.drawPath(mPath, mStrokePaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopScan();
    }

}
