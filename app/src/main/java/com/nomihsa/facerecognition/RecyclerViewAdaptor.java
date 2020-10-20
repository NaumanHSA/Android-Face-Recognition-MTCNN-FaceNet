package com.nomihsa.facerecognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class RecyclerViewAdaptor extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private RecyclerViewClickListener mListener;

    ArrayList<String> names;
    ArrayList<Bitmap> faces_bitmaps;
//    String[] names;
//    Bitmap[] faces_bitmaps;

    public RecyclerViewAdaptor(RecyclerViewClickListener listener,  ArrayList<Bitmap> b, ArrayList<String> n) {
        mListener = listener;
        faces_bitmaps = b;
        names = n;

    }

    public void update_data(int position){
        faces_bitmaps.remove(position);
        names.remove(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.recyclerview_row, parent, false);
        return new RowViewHolder(view, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof RowViewHolder){
            RowViewHolder rowholder = (RowViewHolder) holder;
            rowholder.imageView.setImageBitmap(faces_bitmaps.get(position));
            rowholder.textView.setText(names.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return faces_bitmaps.size();
    }
}
