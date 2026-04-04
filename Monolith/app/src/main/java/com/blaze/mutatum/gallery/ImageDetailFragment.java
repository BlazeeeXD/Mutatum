package com.blaze.mutatum.gallery;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import com.blaze.mutatum.R;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/*
 * PURPOSE:
 * - Shows selected image in full view
 * - Displays file details (name, path, date, size)
 * - Allows deletion via SAF (DocumentFile)
 *
 * UI:
 * - imgPreview: high-res image (Glide)
 * - tvName / tvDate / tvSize / tvPath: metadata display
 * - btnDelete: triggers delete confirmation
 * - btnBack: navigates back to gallery
 *
 * DATA:
 * - imageUri: target image (passed via arguments)
 *
 * FLOW:
 * - onCreateView():
 *     extract arguments → load image → format + display metadata
 *
 * ACTIONS:
 * - showDeleteDialog():
 *     confirms deletion
 * - executeDelete():
 *     deletes file via DocumentFile → success → return to gallery
 *
 * UTIL:
 * - formatFileSize():
 *     converts bytes → readable format (KB/MB/GB)
 *
 */

public class ImageDetailFragment extends Fragment {

    private Uri imageUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_detail, container, false);

        ImageView imgPreview = view.findViewById(R.id.imgPreview);
        TextView tvName = view.findViewById(R.id.tvImgName);
        TextView tvDate = view.findViewById(R.id.tvImgDate);
        TextView tvSize = view.findViewById(R.id.tvImgSize);
        TextView tvPath = view.findViewById(R.id.tvImgPath);
        MaterialButton btnDelete = view.findViewById(R.id.btnDelete);
        ImageButton btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        if (getArguments() != null) {
            String uriString = getArguments().getString("image_uri");
            if (uriString != null) {
                imageUri = Uri.parse(uriString);

                Glide.with(this).load(imageUri).into(imgPreview);

                tvName.setText("Name: " + getArguments().getString("image_name"));
                tvPath.setText("Path: " + imageUri.getPath());

                long dateMs = getArguments().getLong("image_date");
                String dateString = new SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault()).format(new Date(dateMs));
                tvDate.setText("Date: " + dateString);

                long sizeBytes = getArguments().getLong("image_size");
                String sizeString = formatFileSize(sizeBytes);
                tvSize.setText("Size: " + sizeString);
            }
        }

        btnDelete.setOnClickListener(v -> showDeleteDialog());

        return view;
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format(Locale.getDefault(), "%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void showDeleteDialog() {
        new AlertDialog.Builder(requireContext(), android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Delete Image")
                .setMessage("Are you sure you want to permanently delete this image?")
                .setPositiveButton("DELETE", (dialog, which) -> executeDelete())
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void executeDelete() {
        if (imageUri != null) {
            try {
                DocumentFile file = DocumentFile.fromSingleUri(requireContext(), imageUri);
                if (file != null && file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        Toast.makeText(getContext(), "Image deleted", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack(); // Go back to Gallery
                    } else {
                        Toast.makeText(getContext(), "Failed to delete image", Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}