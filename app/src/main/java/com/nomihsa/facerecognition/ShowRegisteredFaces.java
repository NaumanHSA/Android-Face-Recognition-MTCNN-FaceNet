package com.nomihsa.facerecognition;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;


public class ShowRegisteredFaces extends AppCompatActivity {

    private static String TAG = "SHOW_REGISTERED_FACES";
    private RecyclerView recyclerView;
    ArrayList<Bitmap> registered_faces = new ArrayList<>();
    ArrayList<String> person_names = new ArrayList<>();
//    Bitmap[] registered_faces;
//    String[] person_names;
    File[] faces;
    RecyclerViewAdaptor recyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_registered_faces);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        show_registered_faces();
    }


    private void show_registered_faces() {

        File registered_faces_path = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        int faces_number;
        if (registered_faces_path.exists()) {
            faces = registered_faces_path.listFiles();
            faces_number = faces.length;

//            registered_faces = new Bitmap[faces_number];
//            person_names = new String[faces_number];

            for (int i = 0; i < faces.length; i++) {
                registered_faces.add(i, BitmapFactory.decodeFile(faces[i].getAbsolutePath()));
                person_names.add(i, faces[i].getName().split("\\.")[0]);
//                registered_faces[i] = BitmapFactory.decodeFile(faces[i].getAbsolutePath());
//                person_names[i] = faces[i].getName().split("\\.")[0];
            }

            RecyclerViewClickListener listener = new RecyclerViewClickListener() {
                @Override
                public void onClick(View view, int position) {
                    if (delete_item(position)) {
                        Toast.makeText(ShowRegisteredFaces.this, "Item deleted ", Toast.LENGTH_SHORT).show();
                        recyclerView.removeViewAt(position);
                        recyclerViewAdapter.update_data(position);
                    } else {
                        Toast.makeText(ShowRegisteredFaces.this, "Error with deleting item", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            recyclerViewAdapter = new RecyclerViewAdaptor(listener, registered_faces, person_names);
            recyclerView.setAdapter(recyclerViewAdapter);
        }
    }

    private boolean delete_item(int position) {
        File face = faces[position];
        try {
            if (face.delete()) {
                if (face.exists()) {
                    face.getCanonicalFile().delete();
                    if (face.exists()) {
                        getApplicationContext().deleteFile(face.getName());
                    }
                }
                return true;
            } else {
                Log.e("", "File not Deleted " + face.getPath());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }
}