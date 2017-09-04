package com.thecarousell.cropimageview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Custom view that provides cropping capabilities to an image.
 */
public class CropImageView extends FrameLayout {

    /**
     * Image view widget used to show the image for cropping.
     */
    private final ImageView mImageView;

    /**
     * Overlay over the image view to show cropping UI.
     */
    private final CropOverlayView mCropOverlayView;

    /**
     * The matrix used to transform the cropping image in the image view
     */
    private final Matrix mImageMatrix = new Matrix();

    /**
     * Reusing matrix instance for reverse matrix calculations.
     */
    private final Matrix mImageInverseMatrix = new Matrix();

    /**
     * Rectengale used in image matrix transformation calculation (reusing rect instance)
     */
    private final float[] mImagePoints = new float[8];

    /**
     * Animation class to smooth animate zoom-in/out
     */
    private CropImageAnimation mAnimation;

    private Bitmap mBitmap;

    /**
     * How much the image is rotated from original clockwise
     */
    private int mDegreesRotated;

    /**
     * if the image flipped horizontally
     */
    private boolean mFlipHorizontally;

    /**
     * if the image flipped vertically
     */
    private boolean mFlipVertically;

    private int mLayoutWidth;

    private int mLayoutHeight;

    /**
     * The initial scale type of the image in the crop image view
     */
    private CropImage.ScaleType mScaleType;

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping
     * image.<br>
     * default: true, may disable for animation or frame transition.
     */
    private boolean mShowCropOverlay = true;

    /**
     * if auto-zoom functionality is enabled.<br>
     * default: true.
     */
    private boolean mAutoZoomEnabled = true;

    /**
     * The max zoom allowed during cropping
     */
    private int mMaxZoom;

    /**
     * callback to be invoked when crop overlay is released.
     */
    private OnSetCropOverlayReleasedListener mOnCropOverlayReleasedListener;

    /**
     * The sample size the image was loaded by if was loaded by URI
     */
    private int mLoadedSampleSize = 1;

    /**
     * The current zoom level to to scale the cropping image
     */
    private float mZoom = 1;

    /**
     * The X offset that the cropping image was translated after zooming
     */
    private float mZoomOffsetX;

    /**
     * The Y offset that the cropping image was translated after zooming
     */
    private float mZoomOffsetY;

    /**
     * Used to detect size change to handle auto-zoom using {@link #handleCropWindowChanged(boolean,
     * boolean)} in
     * {@link #layout(int, int, int, int)}.
     */
    private boolean mSizeChanged;

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        CropImageOptions options = new CropImageOptions();

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CropImageView, 0,
                    0);
            try {
                options.fixAspectRatio = ta.getBoolean(
                        R.styleable.CropImageView_cropFixAspectRatio, options.fixAspectRatio);
                options.aspectRatioX = ta.getInteger(R.styleable.CropImageView_cropAspectRatioX,
                        options.aspectRatioX);
                options.aspectRatioY = ta.getInteger(R.styleable.CropImageView_cropAspectRatioY,
                        options.aspectRatioY);
                options.scaleType = CropImage.ScaleType.values()[ta.getInt(
                        R.styleable.CropImageView_cropScaleType, options.scaleType.ordinal())];
                options.autoZoomEnabled = ta.getBoolean(
                        R.styleable.CropImageView_cropAutoZoomEnabled, options.autoZoomEnabled);
                options.multiTouchEnabled = ta.getBoolean(
                        R.styleable.CropImageView_cropMultiTouchEnabled,
                        options.multiTouchEnabled);
                options.maxZoom = ta.getInteger(R.styleable.CropImageView_cropMaxZoom,
                        options.maxZoom);
                options.cropShape = CropImage.CropShape.values()[ta.getInt(
                        R.styleable.CropImageView_cropShape, options.cropShape.ordinal())];
                options.guidelines = CropImage.Guidelines.values()[ta.getInt(
                        R.styleable.CropImageView_cropGuidelines,
                        options.guidelines.ordinal())];
                options.snapRadius = ta.getDimension(R.styleable.CropImageView_cropSnapRadius,
                        options.snapRadius);
                options.touchRadius = ta.getDimension(R.styleable.CropImageView_cropTouchRadius,
                        options.touchRadius);
                options.initialCropWindowPaddingRatio = ta.getFloat(
                        R.styleable.CropImageView_cropInitialCropWindowPaddingRatio,
                        options.initialCropWindowPaddingRatio);
                options.borderLineThickness = ta.getDimension(
                        R.styleable.CropImageView_cropBorderLineThickness,
                        options.borderLineThickness);
                options.borderLineColor = ta.getInteger(
                        R.styleable.CropImageView_cropBorderLineColor, options.borderLineColor);
                options.borderCornerThickness = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerThickness,
                        options.borderCornerThickness);
                options.borderCornerOffset = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerOffset,
                        options.borderCornerOffset);
                options.borderCornerLength = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerLength,
                        options.borderCornerLength);
                options.borderCornerColor = ta.getInteger(
                        R.styleable.CropImageView_cropBorderCornerColor,
                        options.borderCornerColor);
                options.guidelinesThickness = ta.getDimension(
                        R.styleable.CropImageView_cropGuidelinesThickness,
                        options.guidelinesThickness);
                options.guidelinesColor = ta.getInteger(
                        R.styleable.CropImageView_cropGuidelinesColor, options.guidelinesColor);
                options.backgroundColor = ta.getInteger(
                        R.styleable.CropImageView_cropBackgroundColor, options.backgroundColor);
                options.showCropOverlay = ta.getBoolean(
                        R.styleable.CropImageView_cropShowCropOverlay, mShowCropOverlay);
                options.borderCornerThickness = ta.getDimension(
                        R.styleable.CropImageView_cropBorderCornerThickness,
                        options.borderCornerThickness);
                options.minCropWindowWidth = (int) ta.getDimension(
                        R.styleable.CropImageView_cropMinCropWindowWidth,
                        options.minCropWindowWidth);
                options.minCropWindowHeight = (int) ta.getDimension(
                        R.styleable.CropImageView_cropMinCropWindowHeight,
                        options.minCropWindowHeight);
                options.minCropResultWidth = (int) ta.getFloat(
                        R.styleable.CropImageView_cropMinCropResultWidthPX,
                        options.minCropResultWidth);
                options.minCropResultHeight = (int) ta.getFloat(
                        R.styleable.CropImageView_cropMinCropResultHeightPX,
                        options.minCropResultHeight);
                options.maxCropResultWidth = (int) ta.getFloat(
                        R.styleable.CropImageView_cropMaxCropResultWidthPX,
                        options.maxCropResultWidth);
                options.maxCropResultHeight = (int) ta.getFloat(
                        R.styleable.CropImageView_cropMaxCropResultHeightPX,
                        options.maxCropResultHeight);
                options.flipHorizontally = ta.getBoolean(
                        R.styleable.CropImageView_cropFlipHorizontally,
                        options.flipHorizontally);
                options.flipVertically = ta.getBoolean(
                        R.styleable.CropImageView_cropFlipHorizontally, options.flipVertically);

                // if aspect ratio is set then set fixed to true
                if (ta.hasValue(R.styleable.CropImageView_cropAspectRatioX) &&
                        ta.hasValue(R.styleable.CropImageView_cropAspectRatioX) &&
                        !ta.hasValue(R.styleable.CropImageView_cropFixAspectRatio)) {
                    options.fixAspectRatio = true;
                }
            } finally {
                ta.recycle();
            }
        }

        options.validate();

        mScaleType = options.scaleType;
        mAutoZoomEnabled = options.autoZoomEnabled;
        mMaxZoom = options.maxZoom;
        mShowCropOverlay = options.showCropOverlay;
        mFlipHorizontally = options.flipHorizontally;
        mFlipVertically = options.flipVertically;

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.crop_image_view, this, true);

        mImageView = v.findViewById(R.id.ImageView_image);
        mImageView.setScaleType(ImageView.ScaleType.MATRIX);

        mCropOverlayView = v.findViewById(R.id.CropOverlayView);
        mCropOverlayView.setCropWindowChangeListener(
                new CropOverlayView.CropWindowChangeListener() {
                    @Override
                    public void onCropWindowChanged(boolean inProgress) {
                        handleCropWindowChanged(inProgress, true);
                        OnSetCropOverlayReleasedListener listener = mOnCropOverlayReleasedListener;
                        if (listener != null && !inProgress) {
                            listener.onCropOverlayReleased(getCropRect());
                        }
                    }
                });
        mCropOverlayView.setInitialAttributeValues(options);
    }

    /**
     * Get the scale type of the image in the crop view.
     */
    public CropImage.ScaleType getScaleType() {
        return mScaleType;
    }

    /**
     * Set the scale type of the image in the crop view
     */
    public void setScaleType(CropImage.ScaleType scaleType) {
        if (scaleType != mScaleType) {
            mScaleType = scaleType;
            mZoom = 1;
            mZoomOffsetX = mZoomOffsetY = 0;
            mCropOverlayView.resetCropOverlayView();
            requestLayout();
        }
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    public CropImage.CropShape getCropShape() {
        return mCropOverlayView.getCropShape();
    }

    /**
     * The shape of the cropping area - rectangle/circular.<br>
     * To set square/circle crop shape set aspect ratio to 1:1.
     */
    public void setCropShape(CropImage.CropShape cropShape) {
        mCropOverlayView.setCropShape(cropShape);
    }

    /**
     * if auto-zoom functionality is enabled. default: true.
     */
    public boolean isAutoZoomEnabled() {
        return mAutoZoomEnabled;
    }

    /**
     * Set auto-zoom functionality to enabled/disabled.
     */
    public void setAutoZoomEnabled(boolean autoZoomEnabled) {
        if (mAutoZoomEnabled != autoZoomEnabled) {
            mAutoZoomEnabled = autoZoomEnabled;
            handleCropWindowChanged(false, false);
            mCropOverlayView.invalidate();
        }
    }

    /**
     * Set multi touch functionality to enabled/disabled.
     */
    public void setMultiTouchEnabled(boolean multiTouchEnabled) {
        if (mCropOverlayView.setMultiTouchEnabled(multiTouchEnabled)) {
            handleCropWindowChanged(false, false);
            mCropOverlayView.invalidate();
        }
    }

    /**
     * The max zoom allowed during cropping.
     */
    public int getMaxZoom() {
        return mMaxZoom;
    }

    /**
     * The max zoom allowed during cropping.
     */
    public void setMaxZoom(int maxZoom) {
        if (mMaxZoom != maxZoom && maxZoom > 0) {
            mMaxZoom = maxZoom;
            handleCropWindowChanged(false, false);
            mCropOverlayView.invalidate();
        }
    }

    /**
     * the min size the resulting cropping image is allowed to be, affects the cropping window
     * limits
     * (in pixels).<br>
     */
    public void setMinCropResultSize(int minCropResultWidth, int minCropResultHeight) {
        mCropOverlayView.setMinCropResultSize(minCropResultWidth, minCropResultHeight);

    }

    /**
     * the max size the resulting cropping image is allowed to be, affects the cropping window
     * limits
     * (in pixels).<br>
     */
    public void setMaxCropResultSize(int maxCropResultWidth, int maxCropResultHeight) {
        mCropOverlayView.setMaxCropResultSize(maxCropResultWidth, maxCropResultHeight);
    }

    /**
     * Get the amount of degrees the cropping image is rotated cloackwise.<br>
     *
     * @return 0-360
     */
    public int getRotatedDegrees() {
        return mDegreesRotated;
    }

    /**
     * Set the amount of degrees the cropping image is rotated cloackwise.<br>
     *
     * @param degrees 0-360
     */
    public void setRotatedDegrees(int degrees) {
        if (mDegreesRotated != degrees) {
            rotateImage(degrees - mDegreesRotated);
        }
    }

    /**
     * whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false allows it
     * to be changed.
     */
    public boolean isFixAspectRatio() {
        return mCropOverlayView.isFixAspectRatio();
    }

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect ratio, while false
     * allows it to be changed.
     */
    public void setFixedAspectRatio(boolean fixAspectRatio) {
        mCropOverlayView.setFixedAspectRatio(fixAspectRatio);
    }

    /**
     * whether the image should be flipped horizontally
     */
    public boolean isFlippedHorizontally() {
        return mFlipHorizontally;
    }

    /**
     * Sets whether the image should be flipped horizontally
     */
    public void setFlippedHorizontally(boolean flipHorizontally) {
        if (mFlipHorizontally != flipHorizontally) {
            mFlipHorizontally = flipHorizontally;
            applyImageMatrix(getWidth(), getHeight(), true, false);
        }
    }

    /**
     * whether the image should be flipped vertically
     */
    public boolean isFlippedVertically() {
        return mFlipVertically;
    }

    /**
     * Sets whether the image should be flipped vertically
     */
    public void setFlippedVertically(boolean flipVertically) {
        if (mFlipVertically != flipVertically) {
            mFlipVertically = flipVertically;
            applyImageMatrix(getWidth(), getHeight(), true, false);
        }
    }

    /**
     * Get the current guidelines option set.
     */
    public CropImage.Guidelines getGuidelines() {
        return mCropOverlayView.getGuidelines();
    }

    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to show when resizing
     * the application.
     */
    public void setGuidelines(CropImage.Guidelines guidelines) {
        mCropOverlayView.setGuidelines(guidelines);
    }

    /**
     * both the X and Y values of the aspectRatio.
     */
    public Pair<Integer, Integer> getAspectRatio() {
        return new Pair<>(mCropOverlayView.getAspectRatioX(), mCropOverlayView.getAspectRatioY());
    }

    /**
     * Sets the both the X and Y values of the aspectRatio.<br>
     * Sets fixed aspect ratio to TRUE.
     *
     * @param aspectRatioX int that specifies the new X value of the aspect ratio
     * @param aspectRatioY int that specifies the new Y value of the aspect ratio
     */
    public void setAspectRatio(int aspectRatioX, int aspectRatioY) {
        mCropOverlayView.setAspectRatioX(aspectRatioX);
        mCropOverlayView.setAspectRatioY(aspectRatioY);
        setFixedAspectRatio(true);
    }

    /**
     * Clears set aspect ratio values and sets fixed aspect ratio to FALSE.
     */
    public void clearAspectRatio() {
        mCropOverlayView.setAspectRatioX(1);
        mCropOverlayView.setAspectRatioY(1);
        setFixedAspectRatio(false);
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a
     * specified bounding box when the crop window edge is less than or equal to
     * this distance (in pixels) away from the bounding box edge. (default: 3dp)
     */
    public void setSnapRadius(float snapRadius) {
        if (snapRadius >= 0) {
            mCropOverlayView.setSnapRadius(snapRadius);
        }
    }

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping
     * image.<br>
     * default: true, may disable for animation or frame transition.
     */
    public boolean isShowCropOverlay() {
        return mShowCropOverlay;
    }

    /**
     * if to show crop overlay UI what contains the crop window UI surrounded by background over the
     * cropping
     * image.<br>
     * default: true, may disable for animation or frame transition.
     */
    public void setShowCropOverlay(boolean showCropOverlay) {
        if (mShowCropOverlay != showCropOverlay) {
            mShowCropOverlay = showCropOverlay;
            setCropOverlayVisibility();
        }
    }

    /**
     * Gets the source Bitmap's dimensions. This represents the largest possible crop rectangle.
     *
     * @return a Rect instance dimensions of the source Bitmap
     */
    public Rect getWholeImageRect() {
        if (mBitmap == null) {
            return null;
        }

        int orgWidth = mBitmap.getWidth() * mLoadedSampleSize;
        int orgHeight = mBitmap.getHeight() * mLoadedSampleSize;
        return new Rect(0, 0, orgWidth, orgHeight);
    }

    /**
     * Gets the crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.
     *
     * @return a Rect instance containing cropped area boundaries of the source Bitmap
     */
    public Rect getCropRect() {
        if (mBitmap != null) {

            // get the points of the crop rectangle adjusted to source bitmap
            float[] points = getCropPoints();

            int orgWidth = mBitmap.getWidth() * mLoadedSampleSize;
            int orgHeight = mBitmap.getHeight() * mLoadedSampleSize;

            // get the rectangle for the points (it may be larger than original if rotation is
            // not stright)
            return RectUtils.getRectFromPoints(points, orgWidth, orgHeight,
                    mCropOverlayView.isFixAspectRatio(), mCropOverlayView.getAspectRatioX(),
                    mCropOverlayView.getAspectRatioY());
        } else {
            return null;
        }
    }

    /**
     * Gets the 4 points of crop window's position relative to the source Bitmap (not the image
     * displayed in the CropImageView) using the original image rotation.<br>
     * Note: the 4 points may not be a rectangle if the image was rotates to NOT stright angle (!=
     * 90/180/270).
     *
     * @return 4 points (x0,y0,x1,y1,x2,y2,x3,y3) of cropped area boundaries
     */
    public float[] getCropPoints() {

        // Get crop window position relative to the displayed image.
        RectF cropWindowRect = mCropOverlayView.getCropWindowRect();

        float[] points = new float[]{
                cropWindowRect.left,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.top,
                cropWindowRect.right,
                cropWindowRect.bottom,
                cropWindowRect.left,
                cropWindowRect.bottom
        };

        mImageMatrix.invert(mImageInverseMatrix);
        mImageInverseMatrix.mapPoints(points);

        for (int i = 0; i < points.length; i++) {
            points[i] *= mLoadedSampleSize;
        }

        return points;
    }

    /**
     * Set the crop window position and size to the given rectangle.<br>
     * Image to crop must be first set before invoking this, for async - after complete callback.
     *
     * @param rect window rectangle (position and size) relative to source bitmap
     */
    public void setCropRect(Rect rect) {
        mCropOverlayView.setInitialCropWindowRect(rect);
    }

    /**
     * Reset crop window to initial rectangle.
     */
    public void resetCropRect() {
        mZoom = 1;
        mZoomOffsetX = 0;
        mZoomOffsetY = 0;
        mDegreesRotated = 0;
        mFlipHorizontally = false;
        mFlipVertically = false;
        applyImageMatrix(getWidth(), getHeight(), false, false);
        mCropOverlayView.resetCropWindowRect();
    }

    /**
     * Set the callback to be invoked when crop overlay is released
     */
    public void setOnSetCropOverlayReleasedListener(OnSetCropOverlayReleasedListener listener) {
        mOnCropOverlayReleasedListener = listener;
    }

    /**
     * Sets a Bitmap as the content of the CropImageView.
     *
     * @param bitmap the Bitmap to set
     */
    public void setImageBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap, 1, 0);
    }

    /**
     * Sets a Bitmap as the content of the CropImageView.
     *
     * @param bitmap         the Bitmap to set
     * @param loadSampleSize the sample size of bitmap
     * @param degreesRotated the degrees bitmap should be rotated
     */
    public void setImageBitmap(Bitmap bitmap, int loadSampleSize, int degreesRotated) {
        mCropOverlayView.setInitialCropWindowRect(null);
        setBitmap(bitmap, loadSampleSize, degreesRotated);
    }

    /**
     * Clear the current image set for cropping.
     */
    public void clearImage() {
        clearImageInt();
        mCropOverlayView.setInitialCropWindowRect(null);
    }

    /**
     * Rotates image by the specified number of degrees clockwise.<br>
     * Negative values represent counter-clockwise rotations.
     *
     * @param degrees Integer specifying the number of degrees to rotate.
     */
    public void rotateImage(int degrees) {
        if (mBitmap != null) {
            // Force degrees to be a non-zero value between 0 and 360 (inclusive)
            if (degrees < 0) {
                degrees = (degrees % 360) + 360;
            } else {
                degrees = degrees % 360;
            }

            boolean flipAxes =
                    !mCropOverlayView.isFixAspectRatio() && ((degrees > 45 && degrees < 135) || (
                            degrees > 215 && degrees < 305));
            RectUtils.RECT.set(mCropOverlayView.getCropWindowRect());
            float halfWidth =
                    (flipAxes ? RectUtils.RECT.height() : RectUtils.RECT.width()) / 2f;
            float halfHeight =
                    (flipAxes ? RectUtils.RECT.width() : RectUtils.RECT.height()) / 2f;
            if (flipAxes) {
                boolean isFlippedHorizontally = mFlipHorizontally;
                mFlipHorizontally = mFlipVertically;
                mFlipVertically = isFlippedHorizontally;
            }

            mImageMatrix.invert(mImageInverseMatrix);

            RectUtils.POINTS[0] = RectUtils.RECT.centerX();
            RectUtils.POINTS[1] = RectUtils.RECT.centerY();
            RectUtils.POINTS[2] = 0;
            RectUtils.POINTS[3] = 0;
            RectUtils.POINTS[4] = 1;
            RectUtils.POINTS[5] = 0;
            mImageInverseMatrix.mapPoints(RectUtils.POINTS);

            // This is valid because degrees is not negative.
            mDegreesRotated = (mDegreesRotated + degrees) % 360;

            applyImageMatrix(getWidth(), getHeight(), true, false);

            // adjust the zoom so the crop window size remains the same even after image scale
            // change
            mImageMatrix.mapPoints(RectUtils.POINTS2, RectUtils.POINTS);
            mZoom /= Math.sqrt(
                    Math.pow(RectUtils.POINTS2[4] - RectUtils.POINTS2[2], 2) + Math.pow(
                            RectUtils.POINTS2[5] - RectUtils.POINTS2[3], 2));
            mZoom = Math.max(mZoom, 1);

            applyImageMatrix(getWidth(), getHeight(), true, false);

            mImageMatrix.mapPoints(RectUtils.POINTS2, RectUtils.POINTS);

            // adjust the width/height by the changes in scaling to the image
            double change = Math.sqrt(
                    Math.pow(RectUtils.POINTS2[4] - RectUtils.POINTS2[2], 2) + Math.pow(
                            RectUtils.POINTS2[5] - RectUtils.POINTS2[3], 2));
            halfWidth *= change;
            halfHeight *= change;

            // calculate the new crop window rectangle to center in the same location and have
            // proper width/height
            RectUtils.RECT.set(RectUtils.POINTS2[0] - halfWidth,
                    RectUtils.POINTS2[1] - halfHeight,
                    RectUtils.POINTS2[0] + halfWidth, RectUtils.POINTS2[1] + halfHeight);

            mCropOverlayView.resetCropOverlayView();
            mCropOverlayView.setCropWindowRect(RectUtils.RECT);
            applyImageMatrix(getWidth(), getHeight(), true, false);
            handleCropWindowChanged(false, false);

            // make sure the crop window rectangle is within the cropping image bounds after all
            // the changes
            mCropOverlayView.fixCurrentCropWindowRect();
        }
    }

    /**
     * Flips the image horizontally.
     */
    public void flipImageHorizontally() {
        mFlipHorizontally = !mFlipHorizontally;
        applyImageMatrix(getWidth(), getHeight(), true, false);
    }

    /**
     * Flips the image vertically.
     */
    public void flipImageVertically() {
        mFlipVertically = !mFlipVertically;
        applyImageMatrix(getWidth(), getHeight(), true, false);
    }

    /**
     * Set the given bitmap to be used in for cropping<br>
     * Optionally clear full if the bitmap is new, or partial clear if the bitmap has been
     * manipulated.
     */
    private void setBitmap(Bitmap bitmap, int loadSampleSize, int degreesRotated) {
        if (mBitmap == null || !mBitmap.equals(bitmap)) {

            mImageView.clearAnimation();

            clearImageInt();

            mBitmap = bitmap;
            mImageView.setImageBitmap(mBitmap);

            mLoadedSampleSize = loadSampleSize;
            mDegreesRotated = degreesRotated;

            applyImageMatrix(getWidth(), getHeight(), true, false);

            if (mCropOverlayView != null) {
                mCropOverlayView.resetCropOverlayView();
                setCropOverlayVisibility();
            }
        }
    }

    /**
     * Clear the current image set for cropping.<br>
     * Full clear will also clear the data of the set image like Uri or Resource id while partial
     * clear
     * will only clear the bitmap.
     */
    private void clearImageInt() {
        mBitmap = null;

        // clean the loaded image flags for new image
        mLoadedSampleSize = 1;
        mDegreesRotated = 0;
        mZoom = 1;
        mZoomOffsetX = 0;
        mZoomOffsetY = 0;
        mImageMatrix.reset();

        mImageView.setImageBitmap(null);

        setCropOverlayVisibility();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mBitmap != null) {

            // Bypasses a baffling bug when used within a ScrollView, where heightSize is set to 0.
            if (heightSize == 0) {
                heightSize = mBitmap.getHeight();
            }

            int desiredWidth;
            int desiredHeight;

            double viewToBitmapWidthRatio = Double.POSITIVE_INFINITY;
            double viewToBitmapHeightRatio = Double.POSITIVE_INFINITY;

            // Checks if either width or height needs to be fixed
            if (widthSize < mBitmap.getWidth()) {
                viewToBitmapWidthRatio = (double) widthSize / (double) mBitmap.getWidth();
            }
            if (heightSize < mBitmap.getHeight()) {
                viewToBitmapHeightRatio = (double) heightSize / (double) mBitmap.getHeight();
            }

            // If either needs to be fixed, choose smallest ratio and calculate from there
            if (viewToBitmapWidthRatio != Double.POSITIVE_INFINITY
                    || viewToBitmapHeightRatio != Double.POSITIVE_INFINITY) {
                if (viewToBitmapWidthRatio <= viewToBitmapHeightRatio) {
                    desiredWidth = widthSize;
                    desiredHeight = (int) (mBitmap.getHeight() * viewToBitmapWidthRatio);
                } else {
                    desiredHeight = heightSize;
                    desiredWidth = (int) (mBitmap.getWidth() * viewToBitmapHeightRatio);
                }
            } else {
                // Otherwise, the picture is within frame layout bounds. Desired width is simply
                // picture size
                desiredWidth = mBitmap.getWidth();
                desiredHeight = mBitmap.getHeight();
            }

            int width = getOnMeasureSpec(widthMode, widthSize, desiredWidth);
            int height = getOnMeasureSpec(heightMode, heightSize, desiredHeight);

            mLayoutWidth = width;
            mLayoutHeight = height;

            setMeasuredDimension(mLayoutWidth, mLayoutHeight);

        } else {
            setMeasuredDimension(widthSize, heightSize);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        super.onLayout(changed, l, t, r, b);

        if (mLayoutWidth > 0 && mLayoutHeight > 0) {
            // Gets original parameters, and creates the new parameters
            ViewGroup.LayoutParams origParams = this.getLayoutParams();
            origParams.width = mLayoutWidth;
            origParams.height = mLayoutHeight;
            setLayoutParams(origParams);

            if (mBitmap != null) {
                applyImageMatrix(r - l, b - t, true, false);

                if (mSizeChanged) {
                    mSizeChanged = false;
                    handleCropWindowChanged(false, false);
                }
            } else {
                updateImageBounds(true);
            }
        } else {
            updateImageBounds(true);
        }
    }

    /**
     * Detect size change to handle auto-zoom using {@link #handleCropWindowChanged(boolean,
     * boolean)} in
     * {@link #layout(int, int, int, int)}.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mSizeChanged = oldw > 0 && oldh > 0;
    }

    /**
     * Handle crop window change to:<br>
     * 1. Execute auto-zoom-in/out depending on the area covered of cropping window relative to the
     * available view area.<br>
     * 2. Slide the zoomed sub-area if the cropping window is outside of the visible view
     * sub-area.<br>
     *
     * @param inProgress is the crop window change is still in progress by the user
     * @param animate    if to animate the change to the image matrix, or set it directly
     */
    private void handleCropWindowChanged(boolean inProgress, boolean animate) {
        int width = getWidth();
        int height = getHeight();
        if (mBitmap != null && width > 0 && height > 0) {

            RectF cropRect = mCropOverlayView.getCropWindowRect();
            if (inProgress) {
                if (cropRect.left < 0 || cropRect.top < 0 || cropRect.right > width
                        || cropRect.bottom > height) {
                    applyImageMatrix(width, height, false, false);
                }
            } else if (mAutoZoomEnabled || mZoom > 1) {
                float newZoom = 0;
                // keep the cropping window covered area to 50%-65% of zoomed sub-area
                if (mZoom < mMaxZoom && cropRect.width() < width * 0.5f
                        && cropRect.height() < height * 0.5f) {
                    newZoom = Math.min(mMaxZoom,
                            Math.min(width / (cropRect.width() / mZoom / 0.64f),
                                    height / (cropRect.height() / mZoom / 0.64f)));
                }
                if (mZoom > 1 && (cropRect.width() > width * 0.65f
                        || cropRect.height() > height * 0.65f)) {
                    newZoom = Math.max(1, Math.min(width / (cropRect.width() / mZoom / 0.51f),
                            height / (cropRect.height() / mZoom / 0.51f)));
                }
                if (!mAutoZoomEnabled) {
                    newZoom = 1;
                }

                if (newZoom > 0 && newZoom != mZoom) {
                    if (animate) {
                        if (mAnimation == null) {
                            // lazy create animation single instance
                            mAnimation = new CropImageAnimation(mImageView, mCropOverlayView);
                        }
                        // set the state for animation to start from
                        mAnimation.setStartState(mImagePoints, mImageMatrix);
                    }

                    mZoom = newZoom;

                    applyImageMatrix(width, height, true, animate);
                }
            }
        }
    }

    /**
     * Apply matrix to handle the image inside the image view.
     *
     * @param width  the width of the image view
     * @param height the height of the image view
     */
    private void applyImageMatrix(float width, float height, boolean center, boolean animate) {
        if (mBitmap != null && width > 0 && height > 0) {

            mImageMatrix.invert(mImageInverseMatrix);
            RectF cropRect = mCropOverlayView.getCropWindowRect();
            mImageInverseMatrix.mapRect(cropRect);

            mImageMatrix.reset();

            // move the image to the center of the image view first so we can manipulate it from
            // there
            mImageMatrix.postTranslate((width - mBitmap.getWidth()) / 2,
                    (height - mBitmap.getHeight()) / 2);
            mapImagePointsByImageMatrix();

            // rotate the image the required degrees from center of image
            if (mDegreesRotated > 0) {
                mImageMatrix.postRotate(mDegreesRotated, RectUtils.getRectCenterX(mImagePoints),
                        RectUtils.getRectCenterY(mImagePoints));
                mapImagePointsByImageMatrix();
            }

            // scale the image to the image view, image rect transformed to know new width/height
            float scale = Math.min(width / RectUtils.getRectWidth(mImagePoints),
                    height / RectUtils.getRectHeight(mImagePoints));
            if (mScaleType == CropImage.ScaleType.FIT_CENTER || (
                    mScaleType == CropImage.ScaleType.CENTER_INSIDE
                            && scale < 1) || (scale > 1 && mAutoZoomEnabled)) {
                mImageMatrix.postScale(scale, scale, RectUtils.getRectCenterX(mImagePoints),
                        RectUtils.getRectCenterY(mImagePoints));
                mapImagePointsByImageMatrix();
            }

            // scale by the current zoom level
            float scaleX = mFlipHorizontally ? -mZoom : mZoom;
            float scaleY = mFlipVertically ? -mZoom : mZoom;
            mImageMatrix.postScale(scaleX, scaleY, RectUtils.getRectCenterX(mImagePoints),
                    RectUtils.getRectCenterY(mImagePoints));
            mapImagePointsByImageMatrix();

            mImageMatrix.mapRect(cropRect);

            if (center) {
                // set the zoomed area to be as to the center of cropping window as possible
                mZoomOffsetX = width > RectUtils.getRectWidth(mImagePoints) ? 0
                        : Math.max(Math.min(width / 2 - cropRect.centerX(),
                                -RectUtils.getRectLeft(mImagePoints)),
                                getWidth() - RectUtils.getRectRight(mImagePoints)) / scaleX;
                mZoomOffsetY = height > RectUtils.getRectHeight(mImagePoints) ? 0
                        : Math.max(Math.min(height / 2 - cropRect.centerY(),
                                -RectUtils.getRectTop(mImagePoints)),
                                getHeight() - RectUtils.getRectBottom(mImagePoints)) / scaleY;
            } else {
                // adjust the zoomed area so the crop window rectangle will be inside the area in
                // case it was moved outside
                mZoomOffsetX = Math.min(Math.max(mZoomOffsetX * scaleX, -cropRect.left),
                        -cropRect.right + width) / scaleX;
                mZoomOffsetY = Math.min(Math.max(mZoomOffsetY * scaleY, -cropRect.top),
                        -cropRect.bottom + height) / scaleY;
            }

            // apply to zoom offset translate and update the crop rectangle to offset correctly
            mImageMatrix.postTranslate(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY);
            cropRect.offset(mZoomOffsetX * scaleX, mZoomOffsetY * scaleY);
            mCropOverlayView.setCropWindowRect(cropRect);
            mapImagePointsByImageMatrix();
            mCropOverlayView.invalidate();

            // set matrix to apply
            if (animate) {
                // set the state for animation to end in, start animation now
                mAnimation.setEndState(mImagePoints, mImageMatrix);
                mImageView.startAnimation(mAnimation);
            } else {
                mImageView.setImageMatrix(mImageMatrix);
            }

            // update the image rectangle in the crop overlay
            updateImageBounds(false);
        }
    }

    /**
     * Adjust the given image rectangle by image transformation matrix to know the final rectangle
     * of the image.<br>
     * To get the proper rectangle it must be first reset to orginal image rectangle.
     */
    private void mapImagePointsByImageMatrix() {
        mImagePoints[0] = 0;
        mImagePoints[1] = 0;
        mImagePoints[2] = mBitmap.getWidth();
        mImagePoints[3] = 0;
        mImagePoints[4] = mBitmap.getWidth();
        mImagePoints[5] = mBitmap.getHeight();
        mImagePoints[6] = 0;
        mImagePoints[7] = mBitmap.getHeight();
        mImageMatrix.mapPoints(mImagePoints);
    }

    /**
     * Determines the specs for the onMeasure function. Calculates the width or height
     * depending on the mode.
     *
     * @param measureSpecMode The mode of the measured width or height.
     * @param measureSpecSize The size of the measured width or height.
     * @param desiredSize     The desired size of the measured width or height.
     * @return The final size of the width or height.
     */
    private static int getOnMeasureSpec(int measureSpecMode, int measureSpecSize, int desiredSize) {

        // Measure Width
        int spec;
        if (measureSpecMode == MeasureSpec.EXACTLY) {
            // Must be this size
            spec = measureSpecSize;
        } else if (measureSpecMode == MeasureSpec.AT_MOST) {
            // Can't be bigger than...; match_parent value
            spec = Math.min(desiredSize, measureSpecSize);
        } else {
            // Be whatever you want; wrap_content
            spec = desiredSize;
        }

        return spec;
    }

    /**
     * Set visibility of crop overlay to hide it when there is no image or specificly set by client.
     */
    private void setCropOverlayVisibility() {
        if (mCropOverlayView != null) {
            mCropOverlayView.setVisibility(
                    mShowCropOverlay && mBitmap != null ? VISIBLE : INVISIBLE);
        }
    }

    /**
     * Update the scale factor between the actual image bitmap and the shown image.<br>
     */
    private void updateImageBounds(boolean clear) {
        if (mBitmap != null && !clear) {

            // Get the scale factor between the actual Bitmap dimensions and the displayed
            // dimensions for width/height.
            float scaleFactorWidth =
                    mBitmap.getWidth() * mLoadedSampleSize / RectUtils.getRectWidth(mImagePoints);
            float scaleFactorHeight =
                    mBitmap.getHeight() * mLoadedSampleSize / RectUtils.getRectHeight(
                            mImagePoints);
            mCropOverlayView.setCropWindowLimits(getWidth(), getHeight(), scaleFactorWidth,
                    scaleFactorHeight);
        }

        // set the bitmap rectangle and update the crop window after scale factor is set
        mCropOverlayView.setBounds(clear ? null : mImagePoints, getWidth(), getHeight());
    }

    /**
     * Interface definition for a callback to be invoked when the crop overlay is released.
     */
    public interface OnSetCropOverlayReleasedListener {

        /**
         * Called when the crop overlay changed listener is called and inProgress is false.
         *
         * @param rect The rect coordinates of the cropped overlay
         */
        void onCropOverlayReleased(Rect rect);
    }
}
