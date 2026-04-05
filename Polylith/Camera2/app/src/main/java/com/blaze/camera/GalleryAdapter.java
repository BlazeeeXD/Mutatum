package com.blaze.camera;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;

import com.blaze.camera.R;
import com.bumptech.glide.Glide;

import java.util.List;


/*
 * PURPOSE:
 * - Binds DocumentFile images to gallery grid UI
 * - Handles image loading and click events
 *
 * DATA:
 * - imageFiles: list of image files (DocumentFile)
 * - context: required for Glide + layout inflation
 * - listener: handles image click callbacks
 *
 * FLOW:
 * - onCreateViewHolder(): inflate gallery item layout
 * - onBindViewHolder():
 *     load image via Glide (URI)
 *     set filename text
 *     attach click → return selected file
 *
 * UPDATE:
 * - updateData():
 *     replaces dataset and refreshes grid
 *
 * INNER:
 * - ViewHolder:
 *     caches image + filename views
 *
 */



public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private List<DocumentFile> imageFiles;
    private Context context;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(DocumentFile file);
    }

    public GalleryAdapter(Context context, List<DocumentFile> imageFiles, OnImageClickListener listener) {
        this.context = context;
        this.imageFiles = imageFiles;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gallery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DocumentFile file = imageFiles.get(position);

        Glide.with(context)
                .load(file.getUri())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.imageView);

        holder.tvImageName.setText(file.getName());
        holder.itemView.setOnClickListener(v -> listener.onImageClick(file));
    }

    @Override
    public int getItemCount() {
        return imageFiles.size();
    }

    public void updateData(List<DocumentFile> newFiles) {
        this.imageFiles = newFiles;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvImageName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgGalleryItem);
            tvImageName = itemView.findViewById(R.id.tvImageName);
        }
    }
}
