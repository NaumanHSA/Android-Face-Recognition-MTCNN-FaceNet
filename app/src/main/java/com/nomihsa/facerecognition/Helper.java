package com.nomihsa.facerecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import com.nomihsa.facerecognition.mtcnn.Align;
import com.nomihsa.facerecognition.mtcnn.Box;
import com.nomihsa.facerecognition.mtcnn.MTCNN;

import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

public class Helper {

    public static Bitmap detect_and_crop_face(Context context, MTCNN mtcnn, Bitmap image) {

        Vector<Box> boxes = mtcnn.detectFaces(image, image.getWidth() / 5);
        if (boxes.size() == 0) {
            Toast.makeText(context, "No face detected", Toast.LENGTH_LONG).show();
            return null;

        } else if (boxes.size() > 1) {
            Toast.makeText(context, "There were more than one face detected. Make sure there's a single face to register",
                    Toast.LENGTH_LONG).show();
            return null;
        } else {
            // Because there is only one face in each photo used here, the first value is used to crop the face
            Box box = boxes.get(0);

            // Face correction
            image = Align.face_align(image, box.landmark);
            boxes = mtcnn.detectFaces(image, image.getWidth() / 5);
            box = boxes.get(0);

            box.toSquareShape();
            box.limitSquare(image.getWidth(), image.getHeight());
            Rect rect = box.transform2Rect();

            // Crop face
            Bitmap bitmap = MyUtil.crop(image, rect);

            return bitmap;
        }
    }

    public static Vector<Box> detect_faces(Context context, MTCNN mtcnn, Bitmap image) {

        Vector<Box> boxes = mtcnn.detectFaces(image, image.getWidth() / 5);
        return boxes;

//        if (boxes.size() > 0) {
//            Toast.makeText(context, "No face detected", Toast.LENGTH_LONG).show();
//            return null;
//
//        } else {
//            // Because there is only one face in each photo used here, the first value is used to crop the face
//            Box box = boxes.get(0);
//
//            // Face correction
//            image = Align.face_align(image, box.landmark);
//            boxes = mtcnn.detectFaces(image, image.getWidth() / 5);
//            box = boxes.get(0);
//
//            box.toSquareShape();
//            box.limitSquare(image.getWidth(), image.getHeight());
//            Rect rect = box.transform2Rect();
//
//            // Crop face
//            Bitmap bitmap = MyUtil.crop(image, rect);
//
//            return bitmap;

    }



    public static Bitmap handleSamplingAndRotationBitmap(Context context, Uri selectedImage)
            throws IOException {
        int MAX_HEIGHT = 1024;
        int MAX_WIDTH = 1024;

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream imageStream = context.getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, MAX_WIDTH, MAX_HEIGHT);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        imageStream = context.getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);

        img = rotateImageIfRequired(context, img, selectedImage);
        return img;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = Math.min(heightRatio, widthRatio);

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

    /**
     * Rotate an image if required.
     *
     * @param img           The image bitmap
     * @param selectedImage Image URI
     * @return The resulted Bitmap after manipulation
     */
    private static Bitmap rotateImageIfRequired(Context context, Bitmap img, Uri selectedImage) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private static Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }

}
