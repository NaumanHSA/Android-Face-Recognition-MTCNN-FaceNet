package com.nomihsa.facerecognition;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.ImageView;

import com.nomihsa.facerecognition.mobilefacenet.MobileFaceNet;
import com.nomihsa.facerecognition.mtcnn.Align;
import com.nomihsa.facerecognition.mtcnn.Box;
import com.nomihsa.facerecognition.mtcnn.MTCNN;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import static org.opencv.imgproc.Imgproc.getFontScaleFromHeight;


public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "CameraActivity";
    private MobileFaceNet mfn;
    private MTCNN mtcnn;
    private Mat mRGBA, mRGBAT;
    private Bitmap camera_frame_bitmap, registered_face_bitmap, new_face_bitmap, aligned_bitmap;
    private Matrix matrix;
    private File registered_faces_path;
    private float _score;
    private Vector<Box> boxes;
    private int RGBA_height;
    private int RGBA_width;
    private Dictionary<String, Box> dic_boxes;
    private Dictionary<String, Bitmap> dic_reg_faces;
    private List<String> registered_persons;
    private String name;
    private int counter;
    private Point top_left;
    private Point bottom_right;
    private Point text_point;
    private Box tempBox;
    private int cameraIndex;

    private JavaCameraView javaCameraView;
    private ImageView imageView;


    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(CameraActivity.this) {
        @Override
        public void onManagerConnected(int status) {

            if (status == BaseLoaderCallback.SUCCESS) {
                javaCameraView.enableView();
            } else
                super.onManagerConnected(status);
        }
    };

    private void init() {

        registered_faces_path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (registered_faces_path.exists()) {
            File[] faces = registered_faces_path.listFiles();
            for (File face_data : faces) {
                registered_face_bitmap = BitmapFactory.decodeFile(face_data.getAbsolutePath());
                name = face_data.getName().split("\\.")[0];
                dic_reg_faces.put(name, registered_face_bitmap);
                registered_persons.add(name);
            }
            Log.d(TAG, registered_persons.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imageView = (ImageView) findViewById(R.id.imageview_camera);
        javaCameraView = (JavaCameraView) findViewById(R.id.rec_camera_view);

        dic_boxes = new Hashtable<String, Box>();
        dic_reg_faces = new Hashtable<String, Bitmap>();
        registered_persons = new ArrayList<String>();

        javaCameraView.setCameraIndex(MainActivity.CAMERA_INDEX);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(CameraActivity.this);
        javaCameraView.setMaxFrameSize(640, 480);
        javaCameraView.setAlpha(0);
        try {
            mtcnn = new MTCNN(getAssets());
            mfn = new MobileFaceNet(getAssets());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        init();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        RGBA_height = height;
        RGBA_width = width;
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();

        MatToBitmap(mRGBA);
        recognize_face();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(camera_frame_bitmap);
            }
        });

//        if (dic_boxes != null) {
//            Enumeration<String> e = dic_boxes.keys();
//            String key;
//            while (e.hasMoreElements()) {
//                key = e.nextElement();
//                tempBox = dic_boxes.get(key);
//
//                top_left = transform_coordinates(tempBox.left(), tempBox.top());
//                bottom_right = transform_coordinates(tempBox.right(), tempBox.bottom());
//
//                Log.d(TAG, key);
//                if (registered_persons.contains(key)) {
//                    Imgproc.rectangle(mRGBA, top_left, bottom_right, new Scalar(0, 255, 0), 2);
//                    Imgproc.putText(mRGBA, key, bottom_right, 3, 1, new Scalar(0, 255, 0, 255), 1);
//                } else {
//                    Imgproc.rectangle(mRGBA, top_left, bottom_right, new Scalar(255, 0, 0), 2);
//                    Imgproc.putText(mRGBA, "unknown", top_left, 3, 1, new Scalar(255, 0, 0, 255), 1);
//                }
//            }
//        }
        return null;
    }

    private Point transform_coordinates_front(int x, int y) {
        return new Point(y, RGBA_height - x);
    }

    private Point transform_coordinates(int x, int y) {
        return new Point(y, RGBA_height - x);
    }

    public Uri getImageUri(Context context, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }


    private Rect rect;

    private void recognize_face() {
        try {
//            Bitmap new_image = Helper.handleSamplingAndRotationBitmap(CameraActivity.this, uri);
            boxes = Helper.detect_faces(CameraActivity.this, mtcnn, camera_frame_bitmap);

            for (Box box : boxes) {
                box = preprocess_box(box);
                rect = box.transform2Rect();
                // Crop face
                new_face_bitmap = MyUtil.crop(camera_frame_bitmap, rect);

                Enumeration<String> e = dic_reg_faces.keys();
                while (e.hasMoreElements()) {
                    name = e.nextElement();
                    registered_face_bitmap = dic_reg_faces.get(name);

                    // compare faces
                    _score = mfn.compare(registered_face_bitmap, new_face_bitmap);

                    if (_score > MobileFaceNet.THRESHOLD) {
                        com.nomihsa.facerecognition.mtcnn.Utils.drawRect(CameraActivity.this,
                                camera_frame_bitmap, rect, 3, true, name);
                    }
//                    else {
//                        com.nomihsa.facerecognition.mtcnn.Utils.drawRect(CameraActivity.this,
//                                camera_frame_bitmap, rect, 3, false, "unknown");
//                    }
                }
            }
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private Box preprocess_box(Box box) {
        aligned_bitmap = Align.face_align(camera_frame_bitmap, box.landmark);
//        Vector<Box> b = mtcnn.detectFaces(camera_frame_bitmap, camera_frame_bitmap.getWidth() / 5);
//        box = b.get(0);
        box.toSquareShape();
        box.limitSquare(aligned_bitmap.getWidth(), aligned_bitmap.getHeight());
        return box;
    }

    private void MatToBitmap(Mat mat) {
        try {
            camera_frame_bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat, camera_frame_bitmap);

            matrix = new Matrix();
            if (JavaCameraView.isFrontFacing)
                matrix.postRotate(270);
            else
                matrix.postRotate(90);
            camera_frame_bitmap = Bitmap.createBitmap(camera_frame_bitmap, 0, 0, camera_frame_bitmap.getWidth(), camera_frame_bitmap.getHeight(), matrix, true);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private Mat BitmapToMat(Bitmap bitmap) {

//        matrix = new Matrix();
//        matrix.postRotate(270);
//        bitmap = Bitmap.createBitmap(camera_frame, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCv", "Unable to load OpenCV");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        } else {
            Log.d("OpenCv", "OpenCV loaded");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        }
    }
}