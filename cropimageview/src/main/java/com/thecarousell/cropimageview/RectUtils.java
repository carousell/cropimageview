package com.thecarousell.cropimageview;

import android.graphics.Rect;
import android.graphics.RectF;

public class RectUtils {

    static final Rect EMPTY_RECT = new Rect();

    static final RectF EMPTY_RECT_F = new RectF();

    /**
     * Reusable rectangle for general internal usage
     */
    static final RectF RECT = new RectF();

    /**
     * Reusable point for general internal usage
     */
    static final float[] POINTS = new float[6];

    /**
     * Reusable point for general internal usage
     */
    static final float[] POINTS2 = new float[6];

    /**
     * Get left value of the bounding rectangle of the given points.
     */
    static float getRectLeft(float[] points) {
        return Math.min(Math.min(Math.min(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * Get top value of the bounding rectangle of the given points.
     */
    static float getRectTop(float[] points) {
        return Math.min(Math.min(Math.min(points[1], points[3]), points[5]), points[7]);
    }

    /**
     * Get right value of the bounding rectangle of the given points.
     */
    static float getRectRight(float[] points) {
        return Math.max(Math.max(Math.max(points[0], points[2]), points[4]), points[6]);
    }

    /**
     * Get bottom value of the bounding rectangle of the given points.
     */
    static float getRectBottom(float[] points) {
        return Math.max(Math.max(Math.max(points[1], points[3]), points[5]), points[7]);
    }

    /**
     * Get a rectangle for the given 4 points (x0,y0,x1,y1,x2,y2,x3,y3) by finding the min/max 2
     * points that
     * contains the given 4 points and is a straight rectangle.
     */
    static Rect getRectFromPoints(float[] points, int imageWidth, int imageHeight,
            boolean fixAspectRatio, int aspectRatioX, int aspectRatioY) {
        int left = Math.round(Math.max(0, getRectLeft(points)));
        int top = Math.round(Math.max(0, getRectTop(points)));
        int right = Math.round(Math.min(imageWidth, getRectRight(points)));
        int bottom = Math.round(Math.min(imageHeight, getRectBottom(points)));

        Rect rect = new Rect(left, top, right, bottom);
        if (fixAspectRatio) {
            fixRectForAspectRatio(rect, aspectRatioX, aspectRatioY);
        }

        return rect;
    }

    /**
     * Fix the given rectangle if it doesn't confirm to aspect ration rule.<br>
     * Make sure that width and height are equal if 1:1 fixed aspect ratio is requested.
     */
    private static void fixRectForAspectRatio(Rect rect, int aspectRatioX, int aspectRatioY) {
        if (aspectRatioX == aspectRatioY && rect.width() != rect.height()) {
            if (rect.height() > rect.width()) {
                rect.bottom -= rect.height() - rect.width();
            } else {
                rect.right -= rect.width() - rect.height();
            }
        }
    }

    /**
     * Get width of the bounding rectangle of the given points.
     */
    static float getRectWidth(float[] points) {
        return getRectRight(points) - getRectLeft(points);
    }

    /**
     * Get heightof the bounding rectangle of the given points.
     */
    static float getRectHeight(float[] points) {
        return getRectBottom(points) - getRectTop(points);
    }

    /**
     * Get horizontal center value of the bounding rectangle of the given points.
     */
    static float getRectCenterX(float[] points) {
        return (getRectRight(points) + getRectLeft(points)) / 2f;
    }

    /**
     * Get verical center value of the bounding rectangle of the given points.
     */
    static float getRectCenterY(float[] points) {
        return (getRectBottom(points) + getRectTop(points)) / 2f;
    }
}
