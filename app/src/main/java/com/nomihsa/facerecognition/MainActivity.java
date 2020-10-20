package com.nomihsa.facerecognition;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.nomihsa.facerecognition.mobilefacenet.MobileFaceNet;
import com.nomihsa.facerecognition.mtcnn.MTCNN;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private MTCNN mtcnn;
    private MobileFaceNet mfn;
    private ImageView imageView;
    private TextView textView;

    private Uri currentphotoUri;
    private File currentphotoFile;
    private String timeStamp;

    private final int READ_STORAGE_PERMISSION_CODE = 1;
    private final int WRITE_STORAGE_PERMISSION_CODE = 2;
    private final int CAMERA_PERMISSION_CODE = 3;
    private final String APP_LOCATION = "faceRecognition";
    private final String FILE_PROVIDER_PATH = "com.nomihsa.facerecognition.fileprovider";
    private final int REGISTER_CAMEREA_REQUEST_CODE = 100;
    private final int REGISTER_GALLERY_REQUEST_CODE = 101;
    private final int RECOGNIZE_CAMEREA_REQUEST_CODE = 102;
    private final int RECOGNIZE_GALLERY_REQUEST_CODE = 103;
    public static int CAMERA_INDEX = 98;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button btn_register = findViewById(R.id.btn_register);
        Button btn_recognize = findViewById(R.id.btn_recognize);
        Button btn_show_faces = findViewById(R.id.btn_show_faces);
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.textView);


        if (!checkPermissions()) {
            requestPermissions();
        }
        start();
        try {
            mtcnn = new MTCNN(getAssets());
            mfn = new MobileFaceNet(getAssets());
        } catch (IOException e) {
            e.printStackTrace();
        }
        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    take_photo(MainActivity.this, 0, "register");
                } else {
                    requestPermissions();
                }

            }
        });
        btn_recognize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    take_photo(MainActivity.this, 2, "recognize");
                } else {
                    requestPermissions();
                }
            }
        });
        btn_show_faces.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ShowRegisteredFaces.class);
                startActivity(intent);
            }
        });
    }

    private boolean checkPermissions() {
        return ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        PermissionListener permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(MainActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
                start();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        };
        TedPermission.with(MainActivity.this)
                .setPermissionListener(permissionListener)
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }

    private void start() {
        try {
            File folder = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/" + APP_LOCATION + "/faces");
            if (!folder.exists()) {
                if (folder.mkdir()) {
                    Toast.makeText(this, "App folder created...", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception ex) {
            Log.e("Folder", "Error with creating Folder");
        }
    }

    private void take_photo(Context context, final int reference, String module) {
        final CharSequence[] options_reg = {"Take Photo", "Choose from Gallery", "Cancel"};
        final CharSequence[] options_rec = {"Run on Live Camera", "Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose a photo");

        if (module.equals("register")) {
            builder.setItems(options_reg, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (options_reg[item].equals("Take Photo")) {
                        takeCameraPhoto(reference, "register");
                    } else if (options_reg[item].equals("Choose from Gallery")) {
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhoto, reference + 1);
                    } else if (options_reg[item].equals("Cancel")) {
                        dialog.dismiss();
                    }
                }
            });
        }
        if (module.equals("recognize")) {
            builder.setItems(options_rec, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    if (options_rec[item].equals("Run on Live Camera")) {
                        run_live_camera();
                    } else if (options_rec[item].equals("Take Photo")) {
                        takeCameraPhoto(reference, "recognize");
                    } else if (options_rec[item].equals("Choose from Gallery")) {
                        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        startActivityForResult(pickPhoto, reference + 1);
                    } else if (options_rec[item].equals("Cancel")) {
                        dialog.dismiss();
                    }
                }
            });
        }
        builder.show();
    }

    private void run_live_camera() {

        final CharSequence[] options = {"Front Facing Camera", "Back Camera"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Camera View");
        builder.setCancelable(false);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("TFront Facing Camera")) {
                    CAMERA_INDEX = CameraBridgeViewBase.CAMERA_ID_FRONT;
                } else {
                    CAMERA_INDEX = CameraBridgeViewBase.CAMERA_ID_BACK;
                }
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });
        builder.show();
    }

    private void takeCameraPhoto(final int _request_code, String module) {

//        String storagePath = module.equals("register") ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_PODCASTS;
        String storagePath = Environment.DIRECTORY_PODCASTS;
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                timeStamp = DateFormat.getDateTimeInstance().format(new Date());
                File storageDir = getExternalFilesDir(storagePath);
                File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
                currentphotoUri = FileProvider.getUriForFile(this, FILE_PROVIDER_PATH, image);
                currentphotoFile = image;

                // Continue only if the File was successfully created
                if (image != null) {
                    Uri photoURI = FileProvider.getUriForFile(this, FILE_PROVIDER_PATH, image);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, _request_code);
                }
            } catch (IOException ex) {
                Log.e("IOException", "File not read");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0: {
                    request_name(currentphotoUri, "camera");
                }
                break;
                case 1: {
                    Uri selectedImage = data.getData();
                    request_name(selectedImage, "gallery");
                }
                break;
                case 2: {
                    recognize_face(currentphotoUri, "camera");
                }
                break;
                case 3: {
                    Uri selectedImage = data.getData();
                    recognize_face(selectedImage, "gallery");
                }
                break;
            }
        }
    }

    private void register_face(Uri uri, String module, String person_name) {
        try {
            Bitmap bitmap = Helper.handleSamplingAndRotationBitmap(MainActivity.this, uri);
            imageView.setImageBitmap(bitmap);
            bitmap = Helper.detect_and_crop_face(MainActivity.this, mtcnn, bitmap);

            if (bitmap != null) {
                File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                File cropped = new File(storageDir, person_name + ".jpg");
//                File cropped = File.createTempFile(person_name, ".jpg", storageDir);

                FileOutputStream out = new FileOutputStream(cropped);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.flush();
                out.close();
                Toast.makeText(this, "Face registered successfully", Toast.LENGTH_SHORT).show();

                if (module.equals("camera")) {
                    delete_temp();
                }
            }
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private void delete_temp(){
        try {
            if (currentphotoFile.delete()) {
                if (currentphotoFile.exists()) {
                    currentphotoFile.getCanonicalFile().delete();
                    if (currentphotoFile.exists()) {
                        getApplicationContext().deleteFile(currentphotoFile.getName());
                    }
                }
            } else {
                Log.e("", "File not Deleted " + currentphotoUri.getPath());
            }
        } catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    private void request_name(final Uri uri, final String module) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Person Name");
        builder.setMessage("Enter the person's name you want to register. (else face will not be registered)");

        LinearLayout layout = new LinearLayout(MainActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText input = new EditText(MainActivity.this);
        layout.addView(input);
        builder.setView(layout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();
                if (!text.equals("")) {
                    register_face(uri, module, text);
                } else {
                    delete_temp();
                    dialog.cancel();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void recognize_face(Uri uri, String module) {
        try {
            Bitmap new_image = Helper.handleSamplingAndRotationBitmap(MainActivity.this, uri);
            imageView.setImageBitmap(new_image);
            new_image = Helper.detect_and_crop_face(MainActivity.this, mtcnn, new_image);

            File registered_faces_path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            Bitmap registered_face;
            boolean isSame;

            if (new_image != null) {
                if (registered_faces_path.exists()) {
                    File[] faces = registered_faces_path.listFiles();

                    for (File face_data : faces) {
                        registered_face = BitmapFactory.decodeFile(face_data.getAbsolutePath());
                        float _score = mfn.compare(registered_face, new_image);
                        isSame = _score > MobileFaceNet.THRESHOLD;
                        if (isSame) {
                            String name = face_data.getName().split("\\.")[0];
                            textView.setTextColor(Color.GREEN);
                            textView.setText(name);
                        } else {
                            textView.setTextColor(Color.RED);
                            textView.setText("Unknown Person");
                        }
                    }
                }

                if (module.equals("camera")) {
                    if (currentphotoFile.delete()) {
                        if (currentphotoFile.exists()) {
                            currentphotoFile.getCanonicalFile().delete();
                            if (currentphotoFile.exists()) {
                                getApplicationContext().deleteFile(currentphotoFile.getName());
                            }
                        }
                    } else {
                        Log.e("", "File not Deleted " + currentphotoUri.getPath());
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }
}
