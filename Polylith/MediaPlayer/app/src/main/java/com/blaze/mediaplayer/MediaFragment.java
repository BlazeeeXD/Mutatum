package com.blaze.mediaplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.ImageButton;
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

import com.blaze.mediaplayer.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.concurrent.TimeUnit;


/*
 * PURPOSE:
 * - Plays local audio files and streamed video URLs
 * - Provides unified controls (play/pause, seek, progress)
 *
 * MODES:
 * - Audio Mode: file picker → MediaPlayer → album art + metadata
 * - Video Mode: URL input → VideoView → stream playback
 *
 * UI:
 * - btnOpenFile / btnOpenUrl: load media
 * - btnPlayPause / btnRewind / btnForward: transport controls
 * - mediaSeekBar + tvDuration: progress tracking
 * - audioPlaceholder / videoView: media display
 * - statusText: current state feedback
 *
 * STATE:
 * - isVideoMode: toggles audio vs video
 * - isUserSeeking: prevents UI update conflicts
 * - currentAudioUri: selected audio source
 *
 * FLOW:
 * - setupToggleGroup(): switches mode + resets state
 * - setupButtons(): handles file picker, URL input, controls
 * - prepareAudioPlayer(): initializes MediaPlayer
 * - extractAlbumArt(): loads embedded artwork + metadata
 *
 * PLAYBACK:
 * - togglePlayPause(): unified control for audio/video
 * - seekMedia(): +/-10s navigation
 * - stopAllMedia(): resets active playback
 *
 * PROGRESS:
 * - Handler (updateProgress): updates seekbar + time every 500ms
 *
 * LIFECYCLE:
 * - onDestroy(): releases MediaPlayer + stops handler loop
 *
 */


public class MediaFragment extends Fragment {

    private boolean isVideoMode = false;
    private boolean isUserSeeking = false;

    private MaterialButton btnOpenFile, btnOpenUrl;
    private TextView statusText, tvDuration;
    private ImageView audioPlaceholder;
    private VideoView videoView;
    private SeekBar mediaSeekBar;

    // New Transport Control Buttons
    private ImageButton btnPlayPause, btnRewind, btnForward;

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

        // Bind the new buttons
        btnPlayPause = view.findViewById(R.id.btnPlayPause);
        btnRewind = view.findViewById(R.id.btnRewind);
        btnForward = view.findViewById(R.id.btnForward);
    }

    private void setupToggleGroup(View view) {
        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.mediaToggleGroup);
        MaterialButton btnModeAudio = view.findViewById(R.id.btnModeAudio);
        MaterialButton btnModeVideo = view.findViewById(R.id.btnModeVideo);

        // Grab your custom colors
        int activeBg = getContext().getColor(R.color.secondary_accent);
        int inactiveBg = Color.parseColor("#000000"); // Dark gray background
        int activeText = getContext().getColor(R.color.primary_bg_dark);
        int inactiveText = Color.parseColor("#F4E4C9");

        // Set Initial Default State (Audio Active)
        btnModeAudio.setBackgroundTintList(ColorStateList.valueOf(activeBg));
        btnModeAudio.setTextColor(activeText);
        btnModeVideo.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
        btnModeVideo.setTextColor(inactiveText);

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                stopAllMedia();
                resetProgressUI();

                if (checkedId == R.id.btnModeVideo) {
                    isVideoMode = true;

                    // SWAP COLORS TO VIDEO
                    btnModeVideo.setBackgroundTintList(ColorStateList.valueOf(activeBg));
                    btnModeVideo.setTextColor(activeText);
                    btnModeAudio.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
                    btnModeAudio.setTextColor(inactiveText);

                    btnOpenFile.setVisibility(View.GONE);
                    btnOpenUrl.setVisibility(View.VISIBLE);
                    audioPlaceholder.setVisibility(View.GONE);
                    videoView.setVisibility(View.VISIBLE);
                    statusText.setText("Awaiting Video URL");
                } else {
                    isVideoMode = false;

                    // SWAP COLORS TO AUDIO
                    btnModeAudio.setBackgroundTintList(ColorStateList.valueOf(activeBg));
                    btnModeAudio.setTextColor(activeText);
                    btnModeVideo.setBackgroundTintList(ColorStateList.valueOf(inactiveBg));
                    btnModeVideo.setTextColor(inactiveText);

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

        // Modern 3-button logic
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnRewind.setOnClickListener(v -> seekMedia(-10000));
        btnForward.setOnClickListener(v -> seekMedia(10000));
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
                btnPlayPause.setImageResource(R.drawable.ic_pause);
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
        audioPlaceholder.setImageResource(R.drawable.img_media);
    }

    private void prepareAudioPlayer() {
        if (audioPlayer != null) audioPlayer.release();
        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setDataSource(requireContext(), currentAudioUri);
            audioPlayer.prepare();
            mediaSeekBar.setMax(audioPlayer.getDuration());
            btnPlayPause.setImageResource(R.drawable.ic_play);
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

    private void togglePlayPause() {
        if (isVideoMode) {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPlayPause.setImageResource(R.drawable.ic_play);
                statusText.setText("Stream Paused");
            } else {
                videoView.start();
                btnPlayPause.setImageResource(R.drawable.ic_pause);
                statusText.setText("Playing Stream");
            }
        } else {
            if (audioPlayer != null) {
                if (audioPlayer.isPlaying()) {
                    audioPlayer.pause();
                    btnPlayPause.setImageResource(R.drawable.ic_play);
                    statusText.setText("Audio Paused");
                } else {
                    audioPlayer.start();
                    btnPlayPause.setImageResource(R.drawable.ic_pause);
                    statusText.setText("Playing Audio");
                }
            } else {
                Toast.makeText(getContext(), "Load media first", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void seekMedia(int offsetMillis) {
        if (isVideoMode && (videoView.isPlaying() || videoView.getDuration() > 0)) {
            int currentPos = videoView.getCurrentPosition();
            int duration = videoView.getDuration();
            int newPos = Math.max(0, Math.min(currentPos + offsetMillis, duration));
            videoView.seekTo(newPos);
        } else if (!isVideoMode && audioPlayer != null) {
            int currentPos = audioPlayer.getCurrentPosition();
            int duration = audioPlayer.getDuration();
            int newPos = Math.max(0, Math.min(currentPos + offsetMillis, duration));
            audioPlayer.seekTo(newPos);
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
        btnPlayPause.setImageResource(R.drawable.ic_play);
        statusText.setText("System idle");
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