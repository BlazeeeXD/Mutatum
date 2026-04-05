package com.blaze.camera;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;

import com.blaze.camera.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
 * PURPOSE:
 * - Displays live camera preview
 * - Captures photos and saves to user-selected directory
 *
 * UI:
 * - viewFinder: live camera preview
 * - btnCapture: triggers photo capture
 * - btnBack: returns to gallery
 *
 * DATA:
 * - targetFolderUri: storage location (loaded from SharedPreferences)
 * - imageCapture: CameraX capture pipeline
 *
 * FLOW:
 * - onCreateView():
 *     init views → load storage URI → setup buttons → handle permission
 *
 * CAMERA:
 * - startCamera():
 *     initializes CameraX → binds preview + capture to lifecycle
 *
 * CAPTURE:
 * - takePhoto():
 *     create file → open output stream → capture image → save
 *     success → toast + navigate back
 *
 * PERMISSIONS:
 * - CAMERA runtime permission required
 * - denied → exit fragment
 *
 * THREADING:
 * - cameraExecutor: background camera operations
 *
 */



public class CameraFragment extends Fragment {

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private Uri targetFolderUri;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission required.", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        viewFinder = view.findViewById(R.id.viewFinder);
        FloatingActionButton btnCapture = view.findViewById(R.id.btnCapture);
        ImageButton btnBack = view.findViewById(R.id.btnBackToGallery);

        cameraExecutor = Executors.newSingleThreadExecutor();

        SharedPreferences prefs = requireActivity().getSharedPreferences("StoragePrefs", Context.MODE_PRIVATE);
        String savedUriStr = prefs.getString("saved_working_dir", null);

        if (savedUriStr != null) {
            targetFolderUri = Uri.parse(savedUriStr);
        } else {
            Toast.makeText(getContext(), "Error: Target folder lost.", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        }

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        btnCapture.setOnClickListener(v -> takePhoto());

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        return view;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(getContext(), "Camera initialization failed.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        if (imageCapture == null || targetFolderUri == null) return;

        try {
            DocumentFile dir = DocumentFile.fromTreeUri(requireContext(), targetFolderUri);
            if (dir == null || !dir.exists()) throw new Exception("Directory not found");

            String fileName = "BLAZE_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg";
            DocumentFile imageFile = dir.createFile("image/jpeg", fileName);
            if (imageFile == null) throw new Exception("Could not create file");

            OutputStream outputStream = requireActivity().getContentResolver().openOutputStream(imageFile.getUri());
            ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(outputStream).build();

            imageCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(requireContext()),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Toast.makeText(getContext(), "Photo Captured: " + fileName, Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Toast.makeText(getContext(), "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        } catch (Exception e) {
            Toast.makeText(getContext(), "File System Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}