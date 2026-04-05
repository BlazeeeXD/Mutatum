package com.blaze.camera;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blaze.camera.MainActivity;
import com.blaze.camera.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/*
 * PURPOSE:
 * - Lets user select a storage folder (SAF)
 * - Loads and displays images from that folder
 * - Navigates to camera + image detail views
 *
 * UI:
 * - recyclerView: grid display (images)
 * - emptyStateLayout: shown when no folder/images
 * - tvCurrentPath / tvStorageStatus: storage info
 * - btnSelectFolder: opens folder picker
 * - fabCamera: opens camera (requires mounted folder)
 *
 * DATA:
 * - currentFolderUri: active storage directory
 * - SharedPreferences: persists selected folder URI
 *
 * FLOW:
 * - onCreateView():
 *     init UI → setup RecyclerView → setup actions
 * - onResume():
 *     reload saved folder + refresh gallery
 *
 * STORAGE:
 * - folderPickerLauncher:
 *     user selects directory → persist URI permission → save + load
 * - loadImagesFromFolder():
 *     scan directory → filter images → update UI
 *
 * NAVIGATION:
 * - image click → open ImageDetailFragment (via bundle)
 * - camera FAB → open CameraFragment
 *
 * THREADING:
 * - ExecutorService: background file scan
 * - UI updates via main thread
 *
 */


public class GalleryFragment extends Fragment {

    private TextView tvCurrentPath, tvStorageStatus;
    private RecyclerView recyclerView;
    private View emptyStateLayout;
    private GalleryAdapter adapter;
    private Uri currentFolderUri;

    private final String PREFS_NAME = "StoragePrefs";
    private final String KEY_FOLDER_URI = "saved_working_dir";

    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        requireActivity().getContentResolver().takePersistableUriPermission(
                                treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );
                        saveFolderUri(treeUri);
                        loadImagesFromFolder(treeUri);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);

        tvCurrentPath = view.findViewById(R.id.tvCurrentPath);
        tvStorageStatus = view.findViewById(R.id.tvStorageStatus);
        recyclerView = view.findViewById(R.id.galleryRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        MaterialButton btnSelectFolder = view.findViewById(R.id.btnSelectFolder);
        FloatingActionButton fabCamera = view.findViewById(R.id.fabCamera);

        setupRecyclerView();

        btnSelectFolder.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folderPickerLauncher.launch(intent);
        });

        fabCamera.setOnClickListener(v -> {
            if (currentFolderUri == null) {
                Toast.makeText(getContext(), "Mount a directory first!", Toast.LENGTH_SHORT).show();
                return;
            }

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(new CameraFragment(), true);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkSavedFolder();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new GalleryAdapter(getContext(), new ArrayList<>(), file -> {

            Bundle bundle = new Bundle();
            bundle.putString("image_uri", file.getUri().toString());
            bundle.putString("image_name", file.getName());
            bundle.putLong("image_size", file.length());
            bundle.putLong("image_date", file.lastModified());

            ImageDetailFragment detailFragment = new ImageDetailFragment();
            detailFragment.setArguments(bundle);

            MainActivity mainActivity = (MainActivity) getActivity();
            if (mainActivity != null) {
                mainActivity.loadFragment(detailFragment, true);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void saveFolderUri(Uri uri) {
        currentFolderUri = uri;
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FOLDER_URI, uri.toString()).apply();
    }

    private void checkSavedFolder() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedUriStr = prefs.getString(KEY_FOLDER_URI, null);

        if (savedUriStr != null) {
            currentFolderUri = Uri.parse(savedUriStr);
            loadImagesFromFolder(currentFolderUri);
        } else {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void loadImagesFromFolder(Uri folderUri) {
        tvStorageStatus.setText("Scanning Storage...");
        tvStorageStatus.setTextColor(getResources().getColor(R.color.primary_accent, null));

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            DocumentFile dir = DocumentFile.fromTreeUri(requireContext(), folderUri);
            List<DocumentFile> imageFiles = new ArrayList<>();

            if (dir != null && dir.exists() && dir.isDirectory()) {
                for (DocumentFile file : dir.listFiles()) {
                    String mimeType = file.getType();
                    String name = file.getName();

                    boolean isImage = false;
                    if (mimeType != null && mimeType.startsWith("image/")) {
                        isImage = true;
                    } else if (name != null && (name.toLowerCase().endsWith(".jpg") ||
                            name.toLowerCase().endsWith(".jpeg") ||
                            name.toLowerCase().endsWith(".png"))) {
                        isImage = true;
                    }

                    if (isImage) {
                        imageFiles.add(file);
                    }
                }
            }

            requireActivity().runOnUiThread(() -> {
                if (dir != null) {
                    tvCurrentPath.setText("Mounted: /" + dir.getName());
                    tvStorageStatus.setText("Storage Active ●");
                    tvStorageStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                }

                if (imageFiles.isEmpty()) {
                    emptyStateLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyStateLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.updateData(imageFiles);
                }
            });
        });
    }
}