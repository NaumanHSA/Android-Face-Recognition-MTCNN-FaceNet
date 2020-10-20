package com.nomihsa.facerecognition;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


public class RowViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    public TextView textView;
    public ImageView imageView;
    private RecyclerViewClickListener mListener;

    public RowViewHolder(View view, RecyclerViewClickListener listener) {
        super(view);
        mListener = listener;
        view.setOnClickListener(this);
        textView =  view.findViewById(R.id.recyclerview__textview);
        imageView = view.findViewById(R.id.recyclerview_imageView);
    }

    @Override
    public void onClick(View view) {
        mListener.onClick(view, getAdapterPosition());
    }
}
