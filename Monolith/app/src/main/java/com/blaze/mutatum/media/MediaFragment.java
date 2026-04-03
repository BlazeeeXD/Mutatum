package com.blaze.mutatum.media;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blaze.mutatum.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.concurrent.TimeUnit;

public class MediaFragment extends Fragment {

    private boolean isVideoMode = false;
    private boolean isUserSeeking = false;

    private MaterialButton btnOpenFile, btnOpenUrl;
    private TextView statusText, tvDuration;
    private ImageView audioPlaceholder;
    private VideoView videoView;
    private SeekBar mediaSeekBar;

    private MediaPlayer audioPlayer;
    private Uri currentAudioUri;
    private Handler progressHandler = new Handler(Looper.getMainLooper());

    private final ActivityResultLauncher<Intent> audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    currentAudioUri = result.getData().getData();
                    statusText.setText("Loaded: " + currentAudioUri.getLastPathSegment());
                    extractAlbumArt(currentAudioUri);
                    prepareAudioPlayer();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media, container, false);

        initViews(view);
        setupToggleGroup(view);
        setupButtons(view);
        setupSeekBar();

        progressHandler.post(updateProgress);

        return view;
    }

    private void initViews(View view) {
        btnOpenFile = view.findViewById(R.id.btnOpenFile);
        btnOpenUrl = view.findViewById(R.id.btnOpenUrl);
        statusText = view.findViewById(R.id.statusText);
        tvDuration = view.findViewById(R.id.tvDuration);
        audioPlaceholder = view.findViewById(R.id.audioPlaceholder);
        videoView = view.findViewById(R.id.videoView);
        mediaSeekBar = view.findViewById(R.id.audioSeekBar);
    }

    private void setupToggleGroup(View view) {
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.mediaToggleGroup);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                stopAllMedia();
                resetProgressUI();
                if (checkedId == R.id.btnModeVideo) {
                    isVideoMode = true;
                    btnOpenFile.setVisibility(View.GONE);
                    btnOpenUrl.setVisibility(View.VISIBLE);
                    audioPlaceholder.setVisibility(View.GONE);
                    videoView.setVisibility(View.VISIBLE);
                    statusText.setText("Awaiting Video URL");
                } else {
                    isVideoMode = false;
                    btnOpenFile.setVisibility(View.VISIBLE);
                    btnOpenUrl.setVisibility(View.GONE);
                    audioPlaceholder.setVisibility(View.VISIBLE);
                    videoView.setVisibility(View.GONE);
                    statusText.setText(currentAudioUri != null ? "Audio ready" : "System idle");
                }
            }
        });
    }

    private void setupButtons(View view) {
        btnOpenFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            audioPickerLauncher.launch(intent);
        });

        btnOpenUrl.setOnClickListener(v -> showUrlDialog());

        view.findViewById(R.id.btnPlay).setOnClickListener(v -> playMedia());
        view.findViewById(R.id.btnPause).setOnClickListener(v -> pauseMedia());
        view.findViewById(R.id.btnStop).setOnClickListener(v -> stopAllMedia());
        view.findViewById(R.id.btnRestart).setOnClickListener(v -> restartMedia());
    }

    private void showUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Video Stream URL");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText("https://media.w3.org/2010/05/sintel/trailer.mp4");
        builder.setView(input);

        builder.setPositiveButton("Load", (dialog, which) -> {
            String url = input.getText().toString();
            videoView.setVideoURI(Uri.parse(url));
            statusText.setText("Buffering Stream...");

            videoView.setOnPreparedListener(mp -> {
                statusText.setText("Stream Ready");
                mediaSeekBar.setMax(videoView.getDuration());
                videoView.start();
            });
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void extractAlbumArt(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(requireContext(), uri);
            byte[] art = retriever.getEmbeddedPicture();
            if (art != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                audioPlaceholder.setImageBitmap(bitmap);
                audioPlaceholder.setImageTintList(null);
            } else {
                resetAudioPlaceholder();
            }

            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null) statusText.setText(title);

            retriever.release();
        } catch (Exception e) {
            resetAudioPlaceholder();
        }
    }

    private void resetAudioPlaceholder() {
        audioPlaceholder.setImageResource(android.R.drawable.ic_menu_gallery);
        audioPlaceholder.setColorFilter(android.graphics.Color.parseColor("#444444"));
    }

    private void prepareAudioPlayer() {
        if (audioPlayer != null) audioPlayer.release();
        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setDataSource(requireContext(), currentAudioUri);
            audioPlayer.prepare();
            mediaSeekBar.setMax(audioPlayer.getDuration());
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error loading audio engine", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupSeekBar() {
        mediaSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isVideoMode && videoView.isPlaying()) {
                        videoView.seekTo(progress);
                    } else if (!isVideoMode && audioPlayer != null) {
                        audioPlayer.seekTo(progress);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isUserSeeking = false;
            }
        });
    }

    private final Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (!isUserSeeking) {
                int currentPos = 0;
                int totalDuration = 0;

                if (isVideoMode && videoView.isPlaying()) {
                    currentPos = videoView.getCurrentPosition();
                    totalDuration = videoView.getDuration();
                } else if (!isVideoMode && audioPlayer != null && audioPlayer.isPlaying()) {
                    currentPos = audioPlayer.getCurrentPosition();
                    totalDuration = audioPlayer.getDuration();
                }

                if (totalDuration > 0) {
                    mediaSeekBar.setProgress(currentPos);
                    tvDuration.setText(formatTime(currentPos) + " / " + formatTime(totalDuration));
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    private String formatTime(int millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }

    private void resetProgressUI() {
        mediaSeekBar.setProgress(0);
        tvDuration.setText("00:00 / 00:00");
    }

    private void playMedia() {
        if (isVideoMode) {
            videoView.start();
            statusText.setText("Playing Stream");
        } else if (audioPlayer != null) {
            audioPlayer.start();
            statusText.setText("Playing Audio");
        } else {
            Toast.makeText(getContext(), "Load media first", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseMedia() {
        if (isVideoMode && videoView.isPlaying()) {
            videoView.pause();
            statusText.setText("Stream Paused");
        } else if (!isVideoMode && audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();
            statusText.setText("Audio Paused");
        }
    }

    private void stopAllMedia() {
        if (isVideoMode) {
            videoView.stopPlayback();
            videoView.resume();
        } else if (audioPlayer != null) {
            audioPlayer.stop();
            prepareAudioPlayer();
        }
        resetProgressUI();
        statusText.setText("System idle");
    }

    private void restartMedia() {
        if (isVideoMode) {
            videoView.seekTo(0);
            videoView.start();
        } else if (audioPlayer != null) {
            audioPlayer.seekTo(0);
            audioPlayer.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioPlayer != null) {
            audioPlayer.release();
            audioPlayer = null;
        }
        progressHandler.removeCallbacks(updateProgress);
    }
}