import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

public class CropImageView extends ImageView {

    Matrix matrix;

    float minScale = 1f;
    float maxScale = 5f;
    float[] m;

    float widthToHeightRatio = 1f;

    float y1, y2;

    int viewWidth, viewHeight;

    float firstScale;
    float saveScale = 1f;
    float origWidth, origHeight;
    int oldMeasuredWidth, oldMeasuredHeight;

    Paint semitransparent;
    Paint transparent;

    ScaleGestureDetector mScaleDetector;
    GestureDetector mGestureDetector;

    boolean isRotated;

    RectF rTop = new RectF();
    RectF rMid = new RectF();
    RectF rBtm = new RectF();

    public CropImageView(Context context) {
        this(context, null);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    private void setup(Context context) {
        super.setClickable(true);

        semitransparent = new Paint();
        semitransparent.setColor(Color.BLACK);
        semitransparent.setAlpha(200);
        transparent = new Paint();
        transparent.setColor(Color.WHITE);
        transparent.setAlpha(0);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        matrix = new Matrix();
        m = new float[9];
        setImageMatrix(matrix);
        setScaleType(ScaleType.MATRIX);

        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleDetector.onTouchEvent(event);
                mGestureDetector.onTouchEvent(event);
                setImageMatrix(matrix);
                return true;
            }
        });
    }

    public void setMaxZoom(float x) {
        maxScale = x;
    }

    public void setWidthToHeightRatio(float ratio) {
        if (widthToHeightRatio <= 0) {
            throw new IllegalStateException();
        }
        widthToHeightRatio = ratio;
    }

    private void translate(float dX, float dY) {
        matrix.getValues(m);
        float pX = m[Matrix.MTRANS_X];
        float pY = m[Matrix.MTRANS_Y];
        float fX = 0;
        float fY = 0;
        float rX = saveScale * origWidth + pX;
        float rY = saveScale * origHeight + pY;

        if (dX > 0) {
            if (pX <= 0)
                fX = dX + pX > 0 ? 0 - pX : dX;
            else
                fX = 0 - pX;
        } else if (dX < 0) {
            if (rX >= viewWidth)
                fX = dX + rX < viewWidth ? viewWidth - rX : dX;
            else
                fX = viewWidth - rX;
        }

        if (dY > 0 && pY <= y1) {
            if (pY <= y1)
                fY = dY + pY > y1 ? y1 - pY : dY;
            else
                fY = y1 - pY;
        } else if (dY < 0) {
            if (rY >= y2)
                fY = dY + rY < y2 ? y2 - rY : dY;
            else
                fY = y2 - rY;
        }

        if (fX != 0 || fY != 0)
            matrix.postTranslate(fX, fY);
    }

    private void fixScaling() {
        matrix.getValues(m);
        float pX = m[Matrix.MTRANS_X];
        float pY = m[Matrix.MTRANS_Y];
        float fX = 0;
        float fY = 0;
        float rX = saveScale * origWidth + pX;
        float rY = saveScale * origHeight + pY;

        if (pX > 0)
            fX = 0 - pX;
        else if (rX < viewWidth)
            fX = viewWidth - rX;

        if (pY > y1)
            fY = y1 - pY;
        else if (rY < y2)
            fY = y2 - rY;

        if (fX != 0 || fY != 0)
            matrix.postTranslate(fX, fY);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dX,
                float dY) {
            translate(-dX, -dY);
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float mScaleFactor = detector.getScaleFactor();
            float origScale = saveScale;
            saveScale *= mScaleFactor;
            if (saveScale > maxScale) {
                saveScale = maxScale;
                mScaleFactor = maxScale / origScale;
            } else if (saveScale < minScale) {
                saveScale = minScale;
                mScaleFactor = minScale / origScale;
            }

            if (origWidth * saveScale <= viewWidth
                    || origHeight * saveScale <= viewHeight)
                matrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2,
                        viewHeight / 2);
            else
                matrix.postScale(mScaleFactor, mScaleFactor,
                        detector.getFocusX(), detector.getFocusY());
            fixScaling();
            return true;
        }
    }

    private void initialize() {
        float scale;

        Drawable drawable = getDrawable();
        if (drawable == null || drawable.getIntrinsicWidth() == 0 || drawable.getIntrinsicHeight() == 0) {
            return;
        }

        int bmWidth = drawable.getIntrinsicWidth();
        int bmHeight = drawable.getIntrinsicHeight();

        float scaleX = (float) viewWidth / (float) bmWidth;
        float scaleY = (float) viewWidth / (float) bmHeight;
        scale = Math.max(scaleX, scaleY);
        matrix.setScale(scale, scale);
        firstScale = scale;

        float redundantYSpace = (float) viewHeight - (scale * (float) bmHeight);
        float redundantXSpace = (float) viewWidth - (scale * (float) bmWidth);
        redundantYSpace /= (float) 2;
        redundantXSpace /= (float) 2;

        matrix.postTranslate(redundantXSpace, redundantYSpace);

        origWidth = viewWidth - 2 * redundantXSpace;
        origHeight = viewHeight - 2 * redundantYSpace;

        fixScaling();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        int targetHeight = (int) (viewWidth / widthToHeightRatio);

        y1 = (viewHeight - targetHeight) / 2;
        y2 = y1 + targetHeight;

        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight || viewWidth == 0 || viewHeight == 0) {
            return;
        }

        oldMeasuredHeight = viewHeight;
        oldMeasuredWidth = viewWidth;

        if (saveScale == 1 || isRotated) {
            if (isRotated) {
                isRotated = false;
                saveScale = 1;
            }
            initialize();
            setImageMatrix(matrix);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        rTop.set(0, 0, viewWidth, y1);
        rMid.set(0, y1, viewWidth, y2);
        rBtm.set(0, y2, viewWidth, viewHeight);
        canvas.drawRect(rTop, semitransparent);
        canvas.drawRect(rBtm, semitransparent);
        canvas.drawRect(rMid, transparent);
    }

    public Bitmap getCroppedBitmap(int width, int height, Bitmap.Config config) throws OutOfMemoryError {
        Bitmap bitmap;
        if (BitmapDrawable.class.isInstance(getDrawable())) {
            bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        }
        if (bitmap == null) return null;

        Bitmap cropped = Bitmap.createBitmap(width, height, config);
        matrix.getValues(m);
        float pX = m[Matrix.MTRANS_X];
        float pY = m[Matrix.MTRANS_Y];
        int x = (int) ((0 - pX) / (saveScale * firstScale));
        int y = (int) ((y1 - pY) / (saveScale * firstScale));
        int w = (int) (viewWidth / (saveScale * firstScale));
        int h = (int) ((y2 - y1) / (saveScale * firstScale));

        Rect sr = new Rect(x, y, x + w, y + h);
        Canvas canvas = new Canvas(cropped);
        Rect dr = new Rect(0, 0, width, height);
        canvas.drawBitmap(bitmap, sr, dr, new Paint(Paint.FILTER_BITMAP_FLAG));
        return cropped;
    }

    public void rotate(int degrees) throws OutOfMemoryError {
        Bitmap b;
        if (BitmapDrawable.class.isInstance(getDrawable())) {
            b = ((BitmapDrawable) getDrawable()).getBitmap();
        }
        if (b == null) return;

        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);

            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (b != b2) {
                b.recycle();
                b = b2;
                isRotated = true;
                setImageBitmap(b);
            }
        }
    }

    public Rect getCroppedRect(int originalWidth, int originalHeight) {
        Bitmap bitmap;
        if (BitmapDrawable.class.isInstance(getDrawable())) {
            bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
        }
        if (bitmap == null) return new Rect();

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float wScale = (float) originalWidth / width;
        float hScale = (float) originalHeight / height;
        int scale = Math.round(Math.max(wScale, hScale));

        matrix.getValues(m);
        float pX = m[Matrix.MTRANS_X];
        float pY = m[Matrix.MTRANS_Y];
        int x = (int) ((0 - pX) / (saveScale * firstScale));
        int y = (int) ((y1 - pY) / (saveScale * firstScale));
        int w = (int) (viewWidth / (saveScale * firstScale));
        int h = (int) ((y2 - y1) / (saveScale * firstScale));

        return new Rect(x * scale, y * scale, (x + w) * scale, (y + h) * scale);
    }
}
