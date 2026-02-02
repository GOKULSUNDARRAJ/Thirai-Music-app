package com.Saalai.SalaiMusicApp.Fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.Saalai.SalaiMusicApp.Custom.WaveformView;
import com.Saalai.SalaiMusicApp.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CreatepodcastFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;

    // TAG for logging
    private static final String TAG = "CreatePodcastFragment";

    // Views
    private EditText editTextPodcastName;
    private CardView uploadContainer;
    private ImageView imageViewPreview;
    private ImageView imageViewUploadIcon;
    private ConstraintLayout imguploadlayout;
    private TextView textViewTimer;
    private ImageView buttonPlayPause,buttonDeleteImage;
    private ImageView buttonRecord1, buttonRecord;
    private LinearLayout recordlayout1, recordlayout2;
    private ImageView buttonStop;
    private TextView textViewRecordingStatus;

    // Recording variables
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private String recordedFilePath;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isTimerRunning = false;
    private CountDownTimer timer;
    private long recordingTime = 0;
    private Handler waveformHandler = new Handler();
    private Runnable waveformRunnable;

    private boolean isRecordingPaused = false;

    // Permissions
    private String[] permissions = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    private Uri selectedImageUri;
    private WaveformView waveformView;

    private List<String> recordedSegments = new ArrayList<>();
    private String currentSegmentPath = null;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Creating view for CreatePodcastFragment");

        try {
            View view = inflater.inflate(R.layout.fragment_createprodcast, container, false);
            Log.d(TAG, "onCreateView: Layout inflated successfully");

            initViews(view);
            setupListeners();
            setupBackButtonHandler(view);
            checkPermissions();

            return view;
        } catch (Exception e) {
            Log.e(TAG, "onCreateView: Error inflating layout", e);
            Toast.makeText(getContext(), "Error loading create podcast screen", Toast.LENGTH_SHORT).show();
            throw e;
        }
    }

    private void initViews(View view) {
        Log.d(TAG, "initViews: Initializing views");

        try {
            waveformView = view.findViewById(R.id.waveformView);
            Log.d(TAG, "initViews: WaveformView found: " + (waveformView != null));

            editTextPodcastName = view.findViewById(R.id.editTextPodcastName);
            Log.d(TAG, "initViews: EditText found: " + (editTextPodcastName != null));

            uploadContainer = view.findViewById(R.id.uploadContainer);
            Log.d(TAG, "initViews: Upload container found: " + (uploadContainer != null));

            imageViewPreview = view.findViewById(R.id.imageViewPreview);
            Log.d(TAG, "initViews: ImageViewPreview found: " + (imageViewPreview != null));

            imageViewUploadIcon = view.findViewById(R.id.imageViewUploadIcon);
            Log.d(TAG, "initViews: ImageViewUploadIcon found: " + (imageViewUploadIcon != null));

            imguploadlayout = view.findViewById(R.id.imguploadlayout);
            Log.d(TAG, "initViews: Image upload layout found: " + (imguploadlayout != null));

            // Add delete button initialization
            buttonDeleteImage = view.findViewById(R.id.buttonDeleteImage);
            Log.d(TAG, "initViews: Delete button found: " + (buttonDeleteImage != null));

            textViewTimer = view.findViewById(R.id.textViewTimer);
            Log.d(TAG, "initViews: TextViewTimer found: " + (textViewTimer != null));

            buttonPlayPause = view.findViewById(R.id.buttonPlayPause);
            Log.d(TAG, "initViews: PlayPause button found: " + (buttonPlayPause != null));

            buttonRecord = view.findViewById(R.id.buttonRecord);
            Log.d(TAG, "initViews: Record button found: " + (buttonRecord != null));

            buttonRecord1 = view.findViewById(R.id.buttonRecord1);
            Log.d(TAG, "initViews: Record1 button found: " + (buttonRecord1 != null));

            recordlayout1 = view.findViewById(R.id.recordlayout1);
            Log.d(TAG, "initViews: RecordLayout1 found: " + (recordlayout1 != null));

            recordlayout2 = view.findViewById(R.id.recordlayout2);
            Log.d(TAG, "initViews: RecordLayout2 found: " + (recordlayout2 != null));

            buttonStop = view.findViewById(R.id.buttonStop);
            Log.d(TAG, "initViews: Stop button found: " + (buttonStop != null));

            textViewRecordingStatus = view.findViewById(R.id.textViewRecordingStatus);
            Log.d(TAG, "initViews: Recording status text found: " + (textViewRecordingStatus != null));

            Log.d(TAG, "initViews: All views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "initViews: Error initializing views", e);
        }
    }

    private void setupListeners() {
        Log.d(TAG, "setupListeners: Setting up click listeners");

        // Upload picture
        uploadContainer.setOnClickListener(v -> {
            Log.d(TAG, "uploadContainer clicked");
            openImagePicker();
        });

        // Delete image button
        buttonDeleteImage.setOnClickListener(v -> {
            Log.d(TAG, "buttonDeleteImage clicked");
            deleteSelectedImage();
        });

        // Record button (first layout)
        // Record button (first layout)
        buttonRecord.setOnClickListener(v -> {
            Log.d(TAG, "buttonRecord clicked, isRecording: " + isRecording + ", isRecordingPaused: " + isRecordingPaused);

            // Log the state for debugging
            Log.d(TAG, "Current state - isRecording: " + isRecording +
                    ", isRecordingPaused: " + isRecordingPaused +
                    ", mediaRecorder: " + (mediaRecorder != null));

            if (isRecording) {
                // Currently recording, so pause it
                pauseRecording();
            } else if (isRecordingPaused) {
                // Recording is paused, so resume it
                resumeRecording();
            } else {
                // Not recording and not paused, so start new recording
                startRecording();
            }
        });

        // Record button (second layout)
        buttonRecord1.setOnClickListener(v -> {
            Log.d(TAG, "buttonRecord1 clicked, switching layouts");
            recordlayout2.setVisibility(View.VISIBLE);
            recordlayout1.setVisibility(View.GONE);
            Log.d(TAG, "Layout visibility - recordlayout1: " + recordlayout1.getVisibility() +
                    ", recordlayout2: " + recordlayout2.getVisibility());

            // If we already have a recording file, just show it
            if (recordedFilePath != null && new File(recordedFilePath).exists()) {
                updateUIAfterRecording();
            } else {
                // Otherwise start a new recording
                if (!isRecording && !isRecordingPaused) {
                    startRecording();
                }
            }
        });

        // Play/Pause button - ADD MORE LOGGING
        // Play/Pause button - ADD MORE LOGGING
        buttonPlayPause.setOnClickListener(v -> {
            Log.d(TAG, "buttonPlayPause clicked, isPlaying: " + isPlaying +
                    ", recordedFilePath: " + recordedFilePath +
                    ", isRecordingPaused: " + isRecordingPaused +
                    ", segments count: " + recordedSegments.size());

            // If recording is paused and we have segments, stop recording first to combine them
            if (isRecordingPaused && !recordedSegments.isEmpty()) {
                Log.d(TAG, "Recording is paused with segments. Stopping recording first...");
                stopRecording(); // This will combine segments and create final file
                updateUIAfterRecording();

                // Small delay to ensure file is created
                new Handler().postDelayed(() -> {
                    tryPlaying();
                }, 500);
                return;
            }

            // Check if we have a recording file
            if (recordedFilePath == null) {
                Log.w(TAG, "No recording file to play");
                Toast.makeText(requireContext(), "Please record audio first", Toast.LENGTH_SHORT).show();
                return;
            }

            File recordingFile = new File(recordedFilePath);
            if (!recordingFile.exists()) {
                Log.w(TAG, "Recording file doesn't exist: " + recordedFilePath);
                Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "File exists: " + recordingFile.exists() + ", size: " + recordingFile.length());

            if (isPlaying) {
                pausePlaying();
            } else {
                startPlaying();
            }
        });


        // Stop button
        buttonStop.setOnClickListener(v -> {
            Log.d(TAG, "buttonStop clicked");
            onStopButtonClicked();
        });
    }


    private void tryPlaying() {
        if (recordedFilePath == null) {
            Log.w(TAG, "tryPlaying: recordedFilePath is still null after stopping");
            Toast.makeText(requireContext(), "No recording available to play", Toast.LENGTH_SHORT).show();
            return;
        }

        File recordingFile = new File(recordedFilePath);
        if (!recordingFile.exists()) {
            Log.w(TAG, "tryPlaying: File still doesn't exist: " + recordedFilePath);
            Toast.makeText(requireContext(), "Recording file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "tryPlaying: File created successfully, size: " + recordingFile.length());

        if (isPlaying) {
            pausePlaying();
        } else {
            startPlaying();
        }
    }

    private void deleteSelectedImage() {
        Log.d(TAG, "deleteSelectedImage: Deleting selected image");

        if (selectedImageUri != null) {
            // Clear the selected image
            selectedImageUri = null;

            // Reset image preview
            imageViewPreview.setImageResource(android.R.color.transparent);
            imageViewPreview.setVisibility(View.GONE);

            // Show upload layout again
            imguploadlayout.setVisibility(View.VISIBLE);

            // Hide delete button
            buttonDeleteImage.setVisibility(View.GONE);

            Log.d(TAG, "deleteSelectedImage: Image deleted successfully");
            Toast.makeText(requireContext(), "Image removed", Toast.LENGTH_SHORT).show();
        } else {
            Log.w(TAG, "deleteSelectedImage: No image to delete");
            Toast.makeText(requireContext(), "No image to delete", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBackButtonHandler(View view) {
        Log.d(TAG, "setupBackButtonHandler: Setting up back button handler");

        // Set up back button press listener for the root view
        view.setFocusableInTouchMode(true);
        view.requestFocus();
        view.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                Log.d(TAG, "Back button pressed, recordlayout2 visible: " +
                        (recordlayout2 != null && recordlayout2.getVisibility() == View.VISIBLE));

                // Check if second layout is visible
                if (recordlayout2 != null && recordlayout2.getVisibility() == View.VISIBLE) {
                    Log.d(TAG, "Switching back to first layout from back button");
                    switchToFirstLayout();
                    return true; // Consume the back press
                }
            }
            return false;
        });
    }

    private void switchToFirstLayout() {
        Log.d(TAG, "switchToFirstLayout: Switching to first layout");

        // Stop recording if active
        if (isRecording) {
            stopRecording();
        }

        // Stop playback if active
        if (isPlaying) {
            stopPlaying();
        }

        // Switch layouts
        recordlayout2.setVisibility(View.GONE);
        recordlayout1.setVisibility(View.VISIBLE);

        // Don't reset recording time, keep it for display
        updateUIAfterRecording();

        Log.d(TAG, "Layout switched - recordlayout1: " + recordlayout1.getVisibility() +
                ", recordlayout2: " + recordlayout2.getVisibility());
    }



    private void checkPermissions() {
        Log.d(TAG, "checkPermissions: Checking permissions");

        // Always check audio permission
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Audio permission not granted, requesting...");
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            Log.d(TAG, "Audio permission already granted");
        }

        // For Android versions below 10, check storage permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Storage permission not granted, requesting...");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            } else {
                Log.d(TAG, "Storage permission already granted");
            }
        }
    }

    private boolean checkAllPermissions() {
        // Check audio permission
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "checkAllPermissions: Audio permission not granted");
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return false;
        }

        // For Android versions below 10, check storage permissions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "checkAllPermissions: Storage permission not granted");
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
                return false;
            }
        }

        Log.d(TAG, "checkAllPermissions: All permissions granted");
        return true;
    }

    private void startRecording() {
        Log.d(TAG, "startRecording: Attempting to start recording");


        if (!checkAllPermissions()) {
            Log.w(TAG, "startRecording: Permissions not granted");
            Toast.makeText(requireContext(), "Please grant all permissions to record", Toast.LENGTH_SHORT).show();
            return;
        }

        // If we're starting a fresh recording (not resuming), clear old segments
        if (!isRecordingPaused && recordedSegments.isEmpty()) {
            // Clear any old recorded file path if starting fresh
            recordedFilePath = null;
            Log.d(TAG, "startRecording: Starting fresh recording, cleared old file path");
        }

        try {
            // Create a new segment file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);

            if (storageDir == null) {
                storageDir = requireContext().getFilesDir();
                Log.d(TAG, "startRecording: Using internal storage as fallback");
            }

            if (storageDir == null) {
                Log.e(TAG, "startRecording: Storage directory is null");
                Toast.makeText(requireContext(), "Cannot access storage", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!storageDir.exists()) {
                boolean created = storageDir.mkdirs();
                Log.d(TAG, "startRecording: Directory created: " + created);
            }

            currentSegmentPath = new File(storageDir, "segment_" + timeStamp + ".mp3").getAbsolutePath();
            Log.d(TAG, "startRecording: New segment created: " + currentSegmentPath);

            // Setup MediaRecorder with compatible settings
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(96000);
            mediaRecorder.setAudioSamplingRate(44100);
            mediaRecorder.setOutputFile(currentSegmentPath);

            try {
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (Exception e) {
                Log.e(TAG, "startRecording: Error preparing MediaRecorder", e);
                Toast.makeText(requireContext(), "Recording setup failed", Toast.LENGTH_SHORT).show();
                cleanupAfterFailedRecording();
                return;
            }

            isRecording = true;
            isRecordingPaused = false; // Reset paused state when recording starts
            Log.d(TAG, "startRecording: Recording started successfully");
            updateUIForRecording();
            startTimer();
            startWaveformAnimation();



        } catch (Exception e) {
            Log.e(TAG, "startRecording: Unexpected error", e);
            Toast.makeText(requireContext(), "Recording failed to start", Toast.LENGTH_SHORT).show();
            cleanupAfterFailedRecording();
        }
    }

    private void pauseRecording() {
        Log.d(TAG, "pauseRecording: Stopping current segment");

        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                // Save the segment
                if (currentSegmentPath != null) {
                    recordedSegments.add(currentSegmentPath);
                    currentSegmentPath = null;
                    Log.d(TAG, "pauseRecording: Segment saved, total segments: " + recordedSegments.size());
                }

                // CORRECT THE STATE: Set isRecording to false and isRecordingPaused to true
                isRecording = false;
                isRecordingPaused = true; // THIS WAS MISSING!

                stopTimer();
                stopWaveformAnimation();

                updateUIForPausedRecording();
                Toast.makeText(requireContext(), "Recording paused", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "pauseRecording: Error stopping recording", e);
                Toast.makeText(requireContext(), "Error pausing recording", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Add this method to resume recording
    private void resumeRecording() {
        Log.d(TAG, "resumeRecording: Starting new segment");


        // Reset the paused state BEFORE starting new recording
        isRecordingPaused = false;
        startRecording(); // This will create a new segment


    }


    private void updateUIForPausedRecording() {
        Log.d(TAG, "updateUIForPausedRecording: Updating UI for paused recording");

        buttonRecord.setImageResource(R.drawable.micwhite);
        buttonRecord.setBackgroundResource(R.drawable.circle_record_button);
        textViewRecordingStatus.setText("Recording paused. Tap to resume.");
        textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.bgred));

        // Enable play button when recording is paused
        buttonPlayPause.setEnabled(true);

        // Update buttonRecord1 if it exists
        if (buttonRecord1 != null) {
            buttonRecord1.setImageResource(R.drawable.micwhite);
            buttonRecord1.setBackgroundResource(R.drawable.circle_record_button);
        }
    }



    private void stopRecording() {
        Log.d(TAG, "stopRecording: Stopping recording and combining segments");


        // Stop current recording if active
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                // Save the current segment
                if (currentSegmentPath != null) {
                    recordedSegments.add(currentSegmentPath);
                    currentSegmentPath = null;
                }

            } catch (Exception e) {
                Log.e(TAG, "stopRecording: Error stopping recording", e);
            }
        }

        // Combine all segments into one file
        combineRecordedSegments();

        // RESET ALL RECORDING STATES
        isRecording = false;
        isRecordingPaused = false; // Make sure this is reset
        stopTimer();
        stopWaveformAnimation();

        updateUIAfterRecording();

    }

    private void combineRecordedSegments() {
        if (recordedSegments.isEmpty()) {
            Log.w(TAG, "combineRecordedSegments: No segments to combine");
            recordedFilePath = null;
            return;
        }

        try {
            // Create final output file
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            File outputFile = new File(storageDir, "final_recording_" + timeStamp + ".mp3");
            recordedFilePath = outputFile.getAbsolutePath();

            Log.d(TAG, "combineRecordedSegments: Creating final file at: " + recordedFilePath);

            // If only one segment, just rename it
            if (recordedSegments.size() == 1) {
                File segmentFile = new File(recordedSegments.get(0));
                Log.d(TAG, "combineRecordedSegments: Single segment size: " + segmentFile.length());

                if (segmentFile.renameTo(outputFile)) {
                    Log.d(TAG, "combineRecordedSegments: Single segment renamed to " + recordedFilePath);
                } else {
                    // If rename fails, copy the file
                    Log.d(TAG, "combineRecordedSegments: Rename failed, copying file");
                    copyFile(segmentFile, outputFile);
                }
                recordedSegments.clear();

                // Verify file was created
                if (outputFile.exists()) {
                    Log.d(TAG, "combineRecordedSegments: Final file created successfully, size: " + outputFile.length());
                } else {
                    Log.e(TAG, "combineRecordedSegments: Final file not created!");
                }
                return;
            }

            // For multiple segments, we need to combine them
            Log.d(TAG, "combineRecordedSegments: " + recordedSegments.size() + " segments to combine");

            // Show segment info
            for (int i = 0; i < recordedSegments.size(); i++) {
                File segment = new File(recordedSegments.get(i));
                Log.d(TAG, "combineRecordedSegments: Segment " + i + ": " + segment.getAbsolutePath() +
                        ", exists: " + segment.exists() + ", size: " + segment.length());
            }

            // For now, just use the first segment (or implement proper merging)
            File firstSegment = new File(recordedSegments.get(0));
            if (firstSegment.exists()) {
                copyFile(firstSegment, outputFile);
                Log.d(TAG, "combineRecordedSegments: Copied first segment to final file");
            } else {
                Log.e(TAG, "combineRecordedSegments: First segment doesn't exist!");
                Toast.makeText(requireContext(), "Error: Recording segments corrupted", Toast.LENGTH_SHORT).show();
                return;
            }

            // Clean up segment files
            for (String segmentPath : recordedSegments) {
                File segmentFile = new File(segmentPath);
                if (segmentFile.exists() && !segmentFile.getAbsolutePath().equals(outputFile.getAbsolutePath())) {
                    boolean deleted = segmentFile.delete();
                    Log.d(TAG, "combineRecordedSegments: Deleted segment " + segmentPath + ": " + deleted);
                }
            }

            recordedSegments.clear();

            // Final verification
            if (outputFile.exists()) {
                Log.d(TAG, "combineRecordedSegments: Final file created successfully: " + recordedFilePath +
                        ", size: " + outputFile.length() + " bytes");
                Toast.makeText(requireContext(), "Recording saved: " + outputFile.length() + " bytes",
                        Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "combineRecordedSegments: ERROR - Final file not created!");
                Toast.makeText(requireContext(), "Error saving recording", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "combineRecordedSegments: Error", e);
            Toast.makeText(requireContext(), "Error combining recordings: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(File source, File dest) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }


    private void onStopButtonClicked() {
        Log.d(TAG, "onStopButtonClicked: Handling stop button click");

        // Stop all recording and playback
        stopRecording();
        stopPlaying();

        // Show file info
        if (recordedFilePath != null) {
            File recordingFile = new File(recordedFilePath);
            if (recordingFile.exists()) {
                textViewRecordingStatus.setText("Recording saved (" + recordingFile.length() + " bytes). Ready to play.");
            } else {
                textViewRecordingStatus.setText("Recording saved. Ready to play.");
            }
        } else {
            textViewRecordingStatus.setText("No recording available");
        }

        textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));

        // Switch back to first layout
        switchToFirstLayout();
    }



    private void cleanupAfterFailedRecording() {
        Log.d(TAG, "cleanupAfterFailedRecording: Cleaning up after failed recording");

        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "cleanupAfterFailedRecording: Error releasing mediaRecorder", e);
            }
            mediaRecorder = null;
        }
        isRecording = false;
        stopTimer();
        stopWaveformAnimation();
        updateUIAfterRecording();
    }

    private void startPlaying() {
        Log.d(TAG, "startPlaying: Attempting to play recording");

        if (recordedFilePath == null) {
            Log.w(TAG, "startPlaying: recordedFilePath is null");
            Toast.makeText(requireContext(), "No recording to play", Toast.LENGTH_SHORT).show();
            return;
        }

        File recordingFile = new File(recordedFilePath);
        Log.d(TAG, "startPlaying: Recording file exists: " + recordingFile.exists() +
                ", path: " + recordedFilePath + ", size: " + recordingFile.length());

        if (!recordingFile.exists()) {
            Toast.makeText(requireContext(), "No recording to play", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recordingFile.length() == 0) {
            Toast.makeText(requireContext(), "Recording file is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Stop any current playback
            if (mediaPlayer != null) {
                Log.d(TAG, "startPlaying: Releasing existing mediaPlayer");
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();

            // Set error listener first
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "startPlaying: MediaPlayer error - what: " + what + ", extra: " + extra);
                isPlaying = false;
                updateUIAfterPlaying();
                Toast.makeText(requireContext(), "Error playing audio (code: " + what + ")",
                        Toast.LENGTH_SHORT).show();

                // Try alternative playback method
                playWithAudioAttributes();
                return true; // Error handled
            });

            mediaPlayer.setDataSource(recordedFilePath);

            // Use prepareAsync instead of prepare for better error handling
            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "startPlaying: MediaPlayer prepared successfully");
                mp.start();
                isPlaying = true;
                updateUIForPlaying();
                Toast.makeText(requireContext(), "Playing recording...", Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "startPlaying: Playback completed");
                isPlaying = false;
                updateUIAfterPlaying();
                buttonPlayPause.setImageResource(R.drawable.playwhite);
                Toast.makeText(requireContext(), "Playback finished", Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.prepareAsync(); // Use async preparation

        } catch (IOException e) {
            Log.e(TAG, "startPlaying: IOException", e);
            Toast.makeText(requireContext(), "Failed to play recording: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            isPlaying = false;
            updateUIAfterPlaying();
        } catch (Exception e) {
            Log.e(TAG, "startPlaying: Unexpected error", e);
            Toast.makeText(requireContext(), "Failed to play recording", Toast.LENGTH_SHORT).show();
            isPlaying = false;
            updateUIAfterPlaying();
        }
    }

    // Alternative playback method with AudioAttributes
    private void playWithAudioAttributes() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();

            // Set audio attributes for better compatibility
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.media.AudioAttributes audioAttributes = new android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                mediaPlayer.setAudioAttributes(audioAttributes);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            }

            mediaPlayer.setDataSource(recordedFilePath);

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                updateUIForPlaying();
                Toast.makeText(requireContext(), "Playing with alternative method...",
                        Toast.LENGTH_SHORT).show();
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "playWithAudioAttributes: Still failing - what: " + what + ", extra: " + extra);
                Toast.makeText(requireContext(), "Cannot play this audio format",
                        Toast.LENGTH_SHORT).show();
                return true;
            });

            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "playWithAudioAttributes: Error", e);
            Toast.makeText(requireContext(), "Audio playback failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void pausePlaying() {
        Log.d(TAG, "pausePlaying: Attempting to pause playback, isPlaying: " + isPlaying +
                ", mediaPlayer: " + (mediaPlayer != null));

        if (mediaPlayer != null && isPlaying) {
            try {
                mediaPlayer.pause();
                isPlaying = false;
                buttonPlayPause.setImageResource(R.drawable.playwhite);
                Log.d(TAG, "pausePlaying: Playback paused");
                Toast.makeText(requireContext(), "Playback paused", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "pausePlaying: Error pausing playback", e);
                Toast.makeText(requireContext(), "Error pausing playback", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.w(TAG, "pausePlaying: No active playback to pause");
        }
    }

    private void stopPlaying() {
        Log.d(TAG, "stopPlaying: Attempting to stop playback, mediaPlayer: " + (mediaPlayer != null));

        if (mediaPlayer != null) {
            if (isPlaying) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            Log.d(TAG, "stopPlaying: Playback stopped");
            updateUIAfterPlaying();
        }
    }

    private void resetRecording() {
        Log.d(TAG, "resetRecording: Resetting recording state");
        recordingTime = 0;
        textViewTimer.setText("00:00:00");
        textViewRecordingStatus.setText("Ready to record");
        recordedFilePath = null;
        isRecordingPaused = false;
        currentSegmentPath = null;

        // Clean up segment files
        for (String segmentPath : recordedSegments) {
            File segmentFile = new File(segmentPath);
            if (segmentFile.exists()) {
                boolean deleted = segmentFile.delete();
                Log.d(TAG, "resetRecording: Deleted segment " + segmentPath + ": " + deleted);
            }
        }
        recordedSegments.clear();
    }


    // Update the startTimer method to accept a starting time
    private void startTimer(long startTime) {
        Log.d(TAG, "startTimer: Starting recording timer, isTimerRunning: " + isTimerRunning);

        // Stop any existing timer
        stopTimer();

        // Set the current recording time
        recordingTime = startTime;
        updateTimerText();

        timer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                recordingTime += 1000;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "Timer finished");
                isTimerRunning = false;
            }
        }.start();

        isTimerRunning = true;
    }

    // Overload for default start from 0
    private void startTimer() {
        startTimer(0);
    }

    private void stopTimer() {
        Log.d(TAG, "stopTimer: Stopping timer, timer exists: " + (timer != null) +
                ", isTimerRunning: " + isTimerRunning);

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        isTimerRunning = false;
    }

    private void updateTimerText() {
        long hours = TimeUnit.MILLISECONDS.toHours(recordingTime);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(recordingTime) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(recordingTime) % 60;

        String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        textViewTimer.setText(time);
        Log.v(TAG, "updateTimerText: Time updated to: " + time);
    }

    private void updateUIForRecording() {
        Log.d(TAG, "updateUIForRecording: Updating UI for recording state");

        buttonRecord.setImageResource(R.drawable.pause);
        buttonRecord.setBackgroundResource(R.drawable.circle_record_button);
        buttonRecord.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
        textViewRecordingStatus.setText("Recording...");
        textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));

        // DON'T disable play button during recording
        // buttonPlayPause.setEnabled(false);
        buttonPlayPause.setEnabled(true);
        buttonStop.setEnabled(true);

        // Update buttonRecord1 if it exists
        if (buttonRecord1 != null) {
            buttonRecord1.setImageResource(R.drawable.pause);
            buttonRecord1.setBackgroundResource(R.drawable.circle_record_button);
            buttonRecord1.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
        }
    }

    private void updateUIAfterRecording() {
        Log.d(TAG, "updateUIAfterRecording: Updating UI after recording");


        buttonRecord.setImageResource(R.drawable.micwhite);
        buttonRecord.setBackgroundResource(R.drawable.circle_record_button);

        if (recordedFilePath != null && new File(recordedFilePath).exists()) {
            File file = new File(recordedFilePath);
            textViewRecordingStatus.setText("Recording saved (" + file.length() + " bytes). Ready to play.");
        } else {
            textViewRecordingStatus.setText("Ready to record");
        }

        textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));

        // Enable play button only if we have a recording file
        if (recordedFilePath != null && new File(recordedFilePath).exists()) {
            buttonPlayPause.setEnabled(true);
            buttonPlayPause.setAlpha(1.0f);
        } else {
            buttonPlayPause.setEnabled(false);
            buttonPlayPause.setAlpha(0.5f);
        }

        // Update buttonRecord1 if it exists
        if (buttonRecord1 != null) {
            buttonRecord1.setImageResource(R.drawable.micwhite);
            buttonRecord1.setBackgroundResource(R.drawable.circle_record_button);
        }

        // Reset pause state - IMPORTANT!
        isRecordingPaused = false;

    }


    private void updateUIForPlaying() {
        Log.d(TAG, "updateUIForPlaying: Updating UI for playing state");

        buttonPlayPause.setImageResource(R.drawable.pause);
        textViewRecordingStatus.setText("Playing...");
        textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue));
        buttonRecord.setEnabled(false);

        if (buttonRecord1 != null) {
            buttonRecord1.setEnabled(false);
        }
    }

    private void updateUIAfterPlaying() {
        Log.d(TAG, "updateUIAfterPlaying: Updating UI after playing");

        buttonPlayPause.setImageResource(R.drawable.playwhite);
        textViewRecordingStatus.setText("Ready to record");
        textViewRecordingStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray));
        buttonRecord.setEnabled(true);

        if (buttonRecord1 != null) {
            buttonRecord1.setEnabled(true);
        }
    }

    private void openImagePicker() {
        Log.d(TAG, "openImagePicker: Opening image picker");

        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "openImagePicker: Error opening image picker", e);
            Toast.makeText(requireContext(), "Cannot open image picker", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK
                && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Log.d(TAG, "onActivityResult: Image selected: " + selectedImageUri);

            imageViewPreview.setImageURI(selectedImageUri);
            imageViewPreview.setVisibility(View.VISIBLE);
            imguploadlayout.setVisibility(View.GONE);

            // Show delete button
            buttonDeleteImage.setVisibility(View.VISIBLE);

        } else {
            Log.d(TAG, "onActivityResult: No image selected or cancelled");
        }
    }

    private void createPodcast() {
        Log.d(TAG, "createPodcast: Creating podcast");

        String podcastName = editTextPodcastName.getText().toString().trim();
        Log.d(TAG, "createPodcast: Podcast name: " + podcastName);

        if (podcastName.isEmpty()) {
            Log.w(TAG, "createPodcast: Podcast name is empty");
            editTextPodcastName.setError("Please enter podcast name");
            return;
        }

        if (selectedImageUri == null) {
            Log.w(TAG, "createPodcast: No image selected");
            Toast.makeText(requireContext(), "Please upload a cover image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (recordedFilePath == null || !new File(recordedFilePath).exists()) {
            Log.w(TAG, "createPodcast: No recording found");
            Toast.makeText(requireContext(), "Please record audio for your podcast", Toast.LENGTH_SHORT).show();
            return;
        }

        // Here implement your podcast creation logic
        Log.i(TAG, "createPodcast: Creating podcast - Name: " + podcastName +
                ", Image: " + selectedImageUri + ", Audio: " + recordedFilePath);

        Toast.makeText(requireContext(),
                "Creating podcast: " + podcastName, Toast.LENGTH_SHORT).show();

        // TODO: Upload image and audio to your server
        // TODO: Save podcast metadata to database
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: requestCode=" + requestCode +
                ", grantResults length=" + (grantResults != null ? grantResults.length : 0));

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Audio permission granted");
                Toast.makeText(requireContext(), "Audio permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "onRequestPermissionsResult: Audio permission denied");
                Toast.makeText(requireContext(), "Audio permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Storage permission granted");
                Toast.makeText(requireContext(), "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "onRequestPermissionsResult: Storage permission denied");
                Toast.makeText(requireContext(), "Storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startWaveformAnimation() {
        Log.d(TAG, "startWaveformAnimation: Starting waveform animation");

        // Clear any existing callbacks
        stopWaveformAnimation();

        waveformRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    animateWaveform();
                    waveformHandler.postDelayed(this, 100); // Update every 100ms
                } else {
                    Log.d(TAG, "Waveform animation stopped - recording ended");
                }
            }
        };

        waveformHandler.post(waveformRunnable);
    }

    private void stopWaveformAnimation() {
        Log.d(TAG, "stopWaveformAnimation: Stopping waveform animation");
        if (waveformHandler != null) {
            waveformHandler.removeCallbacksAndMessages(null);
        }
        if (waveformRunnable != null) {
            waveformHandler.removeCallbacks(waveformRunnable);
            waveformRunnable = null;
        }
    }

    private void animateWaveform() {
        // Create a new random waveform path
        if (waveformView != null) {
            float amplitude = (float) (Math.random() * 50); // Random amplitude
            waveformView.addAmplitude(amplitude);
            Log.v(TAG, "animateWaveform: Added amplitude: " + amplitude);
        } else {
            Log.w(TAG, "animateWaveform: waveformView is null");
        }
    }

    private void cleanupResources() {
        Log.d(TAG, "cleanupResources: Cleaning up all resources");

        // Stop and cancel timer
        stopTimer();

        // Stop recording
        stopRecording();

        // Stop playing
        stopPlaying();

        // Clear waveform handler
        stopWaveformAnimation();

        // Release media resources
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "cleanupResources: Error releasing mediaRecorder", e);
            }
            mediaRecorder = null;
        }

        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "cleanupResources: Error releasing mediaPlayer", e);
            }
            mediaPlayer = null;
        }

        // Reset pause state
        isRecordingPaused = false;
    }


    // Lifecycle methods for debugging
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Fragment created");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Fragment started");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Fragment paused");

        // Clean up resources when fragment is paused
        cleanupResources();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Fragment stopped");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Fragment destroying");

        cleanupResources();
    }
}