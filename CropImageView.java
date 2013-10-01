import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

public class CropImageView extends ImageView {

    private static final String TAG = "CropImageView";

    Matrix matrix;

    float minScale = 1f;
    float maxScale = 5f;
    float[] m;

    float y1, y2;

    int viewWidth, viewHeight;

    float firstScale;
    float saveScale = 1f;
    protected float origWidth, origHeight;
    int oldMeasuredWidth, oldMeasuredHeight;

    Paint semitransparent;
    Paint transparent;

    ScaleGestureDetector mScaleDetector;
    GestureDetector mGestureDetector;

    boolean isRotated;

    Context context;

    public CropImageView(Context context) {
        super(context);
        setup(context);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    private void setup(Context context) {
        super.setClickable(true);
        this.context = context;

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

    private class GestureListener extends
            GestureDetector.SimpleOnGestureListener {
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

    private class ScaleListener extends
            ScaleGestureDetector.SimpleOnScaleGestureListener {
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
        if (drawable == null || drawable.getIntrinsicWidth() == 0
                || drawable.getIntrinsicHeight() == 0)
            return;
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
        y1 = (viewHeight - viewWidth) / 2;
        y2 = y1 + viewWidth;

        if (oldMeasuredHeight == viewWidth && oldMeasuredHeight == viewHeight
                || viewWidth == 0 || viewHeight == 0)
            return;
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
        RectF rTop = new RectF(0, 0, viewWidth, y1);
        RectF rMid = new RectF(0, y1, viewWidth, y2);
        RectF rBtm = new RectF(0, y2, viewWidth, viewHeight);
        canvas.drawRect(rTop, semitransparent);
        canvas.drawRect(rBtm, semitransparent);
        canvas.drawRect(rMid, transparent);
    }

    public Bitmap getCroppedBitmap(int width, int height) {
        Bitmap result = null;
        try {
            Bitmap bitmap = ((BitmapDrawable) getDrawable()).getBitmap();
            matrix.getValues(m);
            float pX = m[Matrix.MTRANS_X];
            float pY = m[Matrix.MTRANS_Y];

            int x = (int) ((0 - pX) / (saveScale * firstScale));
            int y = (int) ((y1 - pY) / (saveScale * firstScale));
            int s = (int) (viewWidth / (saveScale * firstScale));

            result = Bitmap.createBitmap(bitmap, x, y, s, s);
            if (width > 0 && height > 0)
                result = Bitmap.createScaledBitmap(result, width, height, true);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "fail to crop image", e);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "fail to crop image", e);
        }
        if (result != null)
            result.recycle();
        return null;
    }

    public Bitmap rotate(Bitmap b, int degrees) {
        if (degrees != 0 && b != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) b.getWidth() / 2,
                    (float) b.getHeight() / 2);
            try {
                Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(),
                        b.getHeight(), m, true);
                if (b != b2) {
                    b.recycle();
                    b = b2;
                }
            } catch (OutOfMemoryError e) {
                Log.e(TAG, "fail to rotate bitmap", e);
            }
        }
        isRotated = true;
        setImageBitmap(b);
        return b;
    }
}
