/*
 * Copyright (C) 2014 Sergej Shafarenka, halfbit.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.Maxr1998.xposed.maxlock.lib;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;

import de.Maxr1998.xposed.maxlock.R;

@SuppressLint("NewApi")
public class FabView extends ImageView {

    private static final int TOP_LEFT = 1;
    private static final int TOP_RIGHT = 2;
    private static final int BOTTOM_LEFT = 3;
    private static final int BOTTOM_RIGHT = 4;

    private static final int NORMAL = 1;
    private static final int SMALL = 2;

    protected final ShapeDrawable mBackgroundDrawable;

    protected final int mFabAttachTo;
    protected final int mFabAttachAt;
    protected final int mFabSize;
    protected final int mFabAttachPadding;
    protected final int mFabRevealAfterMs;
    protected final int mShadowOffset;
    protected final int mFabRadius;
    private final FabRevealer mFabRevealer;
    private final TouchSpotAnimator mTouchSpotAnimator;
    protected int mBackgroundColor;
    protected int mBackgroundColorDarker;
    protected View mAttachedToView;

    //-- constructors
    private final OnGlobalLayoutListener mLayoutListener = new OnGlobalLayoutListener() {

        @Override
        public void onGlobalLayout() {

            if (mAttachedToView == null) {
                ViewGroup parent = (ViewGroup) getParent();
                mAttachedToView = parent.findViewById(mFabAttachTo);

                if (mAttachedToView == null) {
                    throw new IllegalArgumentException("cannot find view to attach");
                }
            }

            int transY, transX;

            switch (mFabAttachAt) {
                case TOP_LEFT: {
                    transY = mAttachedToView.getTop() - getHeight() / 2;
                    transX = mAttachedToView.getLeft() + mFabAttachPadding - mShadowOffset;
                    break;
                }

                case TOP_RIGHT: {
                    transY = mAttachedToView.getTop() - getHeight() / 2;
                    transX = mAttachedToView.getRight() - getWidth() - mFabAttachPadding + mShadowOffset;
                    break;
                }

                case BOTTOM_LEFT: {
                    transY = mAttachedToView.getBottom() - getHeight() / 2;
                    transX = mAttachedToView.getLeft() + mFabAttachPadding - mShadowOffset;
                    break;
                }

                case BOTTOM_RIGHT:
                default: {
                    //transY = mAttachedToView.getBottom() - getHeight() / 2;
                    transY = mAttachedToView.getBottom() - getHeight() - mFabAttachPadding + mShadowOffset;
                    transX = mAttachedToView.getRight() - getWidth() - mFabAttachPadding + mShadowOffset;
                    break;
                }
            }

            setY(transY);
            setX(transX);
        }

    };

    public FabView(Context context) {
        this(context, null, 0);
    }

    public FabView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    //-- overrides

    public FabView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setScaleType(ScaleType.CENTER_INSIDE);

        // http://www.google.com/design/spec/patterns/promoted-actions.html#promoted-actions-floating-action-button

        final float density = getResources().getDisplayMetrics().density;

        final TypedArray styles = context.obtainStyledAttributes(attrs, R.styleable.FabView, 0, 0);
        mFabAttachTo = styles.getResourceId(R.styleable.FabView_fab_attachTo, 0);
        mFabAttachAt = styles.getInt(R.styleable.FabView_fab_attachAt, TOP_RIGHT);
        mFabSize = styles.getInt(R.styleable.FabView_fab_size, NORMAL);
        mFabAttachPadding = (int) styles.getDimension(R.styleable.FabView_fab_padding, 16 * density);
        mFabRevealAfterMs = styles.getInteger(R.styleable.FabView_fab_revealAfterMs, -1);
        styles.recycle();

        switch (mFabSize) {
            case SMALL:
                mShadowOffset = (int) (2 * density);
                mFabRadius = (int) (20 * density);
                break;

            case NORMAL:
            default:
                mShadowOffset = (int) (3 * density);
                mFabRadius = (int) (28 * density);
                break;
        }

        mBackgroundDrawable = new ShapeDrawable(new OvalShape());

        final Paint paint = mBackgroundDrawable.getPaint();
        paint.setShadowLayer(mShadowOffset, 0f, 0f * density, 0x60000000);
        paint.setColor(mBackgroundColor);
        setLayerType(LAYER_TYPE_SOFTWARE, paint);

        mTouchSpotAnimator = new TouchSpotAnimator();
        mFabRevealer = new FabRevealer();
        if (mFabRevealAfterMs > -1) {
            setVisibility(GONE);
            getViewTreeObserver().addOnPreDrawListener(mFabRevealer);
        }

    }

    protected static int transformColor(int fromColor, int toColor, float factor) {
        final float unfactor = 1f - factor;
        final int alpha = (int) (unfactor * Color.alpha(fromColor) + factor * Color.alpha(toColor));
        final int red = (int) (unfactor * Color.red(fromColor) + factor * Color.red(toColor));
        final int green = (int) (unfactor * Color.green(fromColor) + factor * Color.green(toColor));
        final int blue = (int) (unfactor * Color.blue(fromColor) + factor * Color.blue(toColor));
        return Color.argb(alpha, red, green, blue);
    }

    protected static int transformAlpha(int color, @SuppressWarnings("SameParameterValue") int fromAlpha, @SuppressWarnings("SameParameterValue") int toAlpha, float factor) {
        final float unfactor = 1f - factor;
        final int alpha = (int) (factor * fromAlpha + unfactor * toAlpha);
        return setAlpha(color, alpha);
    }

    protected static int setAlpha(int color, int alpha) {
        return ((color & 0x00FFFFFF) | (alpha << 24));
    }

    protected static int getDarkerColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.85f;
        return Color.HSVToColor(hsv);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = 2 * mShadowOffset + 2 * mFabRadius;
        final int sizeSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(sizeSpec, sizeSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mBackgroundDrawable.setBounds(left + mShadowOffset, top + mShadowOffset, right - mShadowOffset, bottom - mShadowOffset);
        mTouchSpotAnimator.layout(left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setPivotX(w / 2);
        setPivotY(h / 2);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onDetachedFromWindow() {
        getViewTreeObserver().removeGlobalOnLayoutListener(mLayoutListener);
        super.onDetachedFromWindow();
    }

    //-- inner classes

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        mBackgroundDrawable.draw(canvas);
        mTouchSpotAnimator.draw(canvas);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        mTouchSpotAnimator.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    //-- utility methods

    @Override
    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundColorDarker = getDarkerColor(color);
        if (mBackgroundDrawable != null) {
            mBackgroundDrawable.getPaint().setColor(color);
        }
        invalidate();
    }

    @Override
    public void setBackground(Drawable background) {
        if (background instanceof ColorDrawable) {
            ColorDrawable cd = (ColorDrawable) background;
            setBackgroundColor(cd.getColor());
        } else {
            throw new UnsupportedOperationException("only color drawable are supported for now");
        }
    }

    protected class FabRevealer implements Runnable, OnPreDrawListener {

        public void show(@SuppressWarnings("SameParameterValue") boolean animate) {
            if (getVisibility() == VISIBLE) return;
            if (animate) {
                setScaleX(0);
                setScaleY(0);
                setVisibility(VISIBLE);
                animate()
                        .setInterpolator(new AccelerateInterpolator())
                        .setDuration(300)
                        .scaleX(1)
                        .scaleY(1);

            } else {
                setVisibility(VISIBLE);
            }
        }

        //-- utility methods

        @Override
        public void run() {
            show(true);
        }

        @Override
        public boolean onPreDraw() {
            getHandler().postDelayed(mFabRevealer, mFabRevealAfterMs);
            getViewTreeObserver().removeOnPreDrawListener(this);
            return true;
        }

    }

    protected class TouchSpotAnimator implements AnimatorUpdateListener, AnimatorListener {

        private static final int ANIM_DURATION = 300;

        private final PointF mTouchPoint = new PointF();
        private final Paint mTouchSpotPaint = new Paint();
        private final Path mClipPath = new Path();

        private final int mTargetRadius;
        private final int mTouchSpotColor;

        private int mTouchSpotRadius = 0;
        private ValueAnimator mTouchSpotAnimator;

        public TouchSpotAnimator() {
            mTouchSpotColor = 0x00000000;
            mTouchSpotPaint.setColor(mTouchSpotColor);
            mTouchSpotPaint.setStyle(Style.FILL);
            mTargetRadius = mFabRadius * 5 / 3;
        }

        public void layout(int left, int top, int right, int bottom) {
            mClipPath.reset();
            mClipPath.addCircle((right - left) / 2, (bottom - top) / 2, mFabRadius, Direction.CW);
        }

        public void onTouchEvent(MotionEvent event) {
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    mTouchPoint.x = event.getX();
                    mTouchPoint.y = event.getY();
                    animate();
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
        }

        public void draw(Canvas canvas) {
            if (mTouchSpotRadius > 0) {
                canvas.save();
                canvas.clipPath(mClipPath);
                canvas.drawCircle(mTouchPoint.x, mTouchPoint.y, mTouchSpotRadius, mTouchSpotPaint);
                canvas.restore();
            }
        }

        public void animate() {
            if (mTouchSpotAnimator == null) {
                mTouchSpotAnimator = new ValueAnimator();
                mTouchSpotAnimator.setInterpolator(new AccelerateInterpolator());
                mTouchSpotAnimator.addUpdateListener(this);
                mTouchSpotAnimator.addListener(this);
            } else {
                if (mTouchSpotAnimator.isRunning()) {
                    mTouchSpotAnimator.cancel();
                }
            }
            mTouchSpotAnimator.setFloatValues(0.2f, 1f);
            mTouchSpotAnimator.setDuration(ANIM_DURATION);
            mTouchSpotAnimator.start();
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            final float factor = animation.getAnimatedFraction();
            mTouchSpotRadius = (int) (mTargetRadius * factor);
            mTouchSpotPaint.setColor(transformAlpha(mTouchSpotColor, 0x00, 0x38, factor));
            mBackgroundDrawable.getPaint().setColor(transformColor(mBackgroundColorDarker, mBackgroundColor, factor));
            invalidate();
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            mTouchSpotRadius = 0;
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mTouchSpotRadius = 0;
        }

        @Override
        public void onAnimationStart(Animator animation) {
        }

        @Override
        public void onAnimationRepeat(Animator animation) {
        }

    }

}