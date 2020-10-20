package com.nomihsa.facerecognition.mtcnn;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;

/**
 * 人脸对齐矫正
 */
public class Align {

    private static Matrix matrix;

    /**
     * Affine transformation
     * @param bitmap Original picture
     * @param landmarks landmarks
     * @return Transformed picture
     */
    public static Bitmap face_align(Bitmap bitmap, Point[] landmarks) {
        float diffEyeX = landmarks[1].x - landmarks[0].x;
        float diffEyeY = landmarks[1].y - landmarks[0].y;

        float fAngle;
        if (Math.abs(diffEyeY) < 1e-7) {
            fAngle = 0.f;
        } else {
            fAngle = (float) (Math.atan(diffEyeY / diffEyeX) * 180.0f / Math.PI);
        }
        matrix = new Matrix();
        matrix.setRotate(-fAngle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
