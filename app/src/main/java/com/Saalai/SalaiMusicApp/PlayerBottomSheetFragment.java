package com.Saalai.SalaiMusicApp;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.squareup.picasso.Picasso;

public class PlayerBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String TAG = "FullAudioPlayerBS";

    private TextView tvSongName, tvSongArtist, tvCurrentTime, tvDuration;
    private ImageView btnPlayPause, btnNext, btnPrev, imgAlbumArt, close,menu;
    private ProgressBar progressBar;
    private SeekBar seekBar;

    private Handler handler = new Handler();
    private Runnable updateSeekBarRunnable;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_audio_player, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvSongName = view.findViewById(R.id.tvFullPlayerSongName);
        tvSongArtist = view.findViewById(R.id.tvFullPlayerSongArtist);
        tvCurrentTime = view.findViewById(R.id.tvCurrentTime);
        tvDuration = view.findViewById(R.id.tvDuration);

        btnPlayPause = view.findViewById(R.id.btnFullPlayPause);
        btnNext = view.findViewById(R.id.btnFullNext);
        btnPrev = view.findViewById(R.id.btnFullPrev);
        imgAlbumArt = view.findViewById(R.id.imgAlbumArt);
        close = view.findViewById(R.id.close);
        menu = view.findViewById(R.id.menu);
        progressBar = view.findViewById(R.id.progressBarLoading);
        seekBar = view.findViewById(R.id.seekBar);

        close.setOnClickListener(v -> dismiss());

        setupUI();
    }

    private void setupUI() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) return;

        // Initial UI update
        safeUpdateUI();

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> {
            if (PlayerManager.isPlaying()) {
                PlayerManager.pausePlayback();
            } else {
                if (PlayerManager.getPlayer() != null) {
                    PlayerManager.getPlayer().start();
                    btnPlayPause.setImageResource(R.drawable.pause);
                }
            }
            btnPlayPause.setImageResource(PlayerManager.isPlaying() ? R.drawable.pause : R.drawable.play_player);
            broadcastUpdate();
        });

        // Next / Previous buttons
        btnNext.setOnClickListener(v -> PlayerManager.playNext(() -> {
            safeUpdateUI();
            restartSeekBarUpdater();
        }));

        btnPrev.setOnClickListener(v -> PlayerManager.playPrevious(() -> {
            safeUpdateUI();
            restartSeekBarUpdater();
        }));

        // Setup SeekBar
        setupSeekBar();

        // Load album art
        loadAlbumArtWithPalette(currentAudio);

        // Listen for audio changes
        PlayerManager.getInstance().setOnAudioChangedListener(newAudio -> safeUpdateUI());

        if (PlayerManager.getPlayer() != null) {
            // Buffering updates
            PlayerManager.getPlayer().setOnBufferingUpdateListener((mp, percent) -> {
                if (percent < 100) showLoading();
                else hideLoading();
            });

            // Playback errors
            PlayerManager.getPlayer().setOnErrorListener((mp, what, extra) -> {
                hideLoading();
                if (isAdded()) Toast.makeText(getContext(), "Playback error", Toast.LENGTH_SHORT).show();
                return true;
            });

            // Prepared listener
            PlayerManager.getPlayer().setOnPreparedListener(mp -> {
                hideLoading();
                seekBar.setMax(mp.getDuration());
                tvDuration.setText(formatTime(mp.getDuration()));
                if (!PlayerManager.isPlaying()) {
                    mp.start();
                    btnPlayPause.setImageResource(R.drawable.pause);
                }
                restartSeekBarUpdater(); // ✅ restart SeekBar for new track
            });

            // Completion listener to automatically play next
            PlayerManager.getPlayer().setOnCompletionListener(mp -> {
                PlayerManager.playNext(() -> {
                    safeUpdateUI();
                    restartSeekBarUpdater(); // ✅ restart SeekBar for next track
                });
            });
        }

        menu.setOnClickListener(v -> {
            // Use the activity's fragment manager instead of child fragment manager
            if (getContext() instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) getContext();
                MenuBottomSheetFragment bottomSheetFragment = new MenuBottomSheetFragment();

                // Pass data to the bottom sheet fragment if needed
                Bundle args = new Bundle();
                args.putString("audioName", PlayerManager.getCurrentAudio().getAudioName());
                args.putString("audioUrl", PlayerManager.getCurrentAudio().getAudioUrl());
                args.putString("artistName", PlayerManager.getCurrentAudio().getAudioArtist());
                args.putString("imageUrl", PlayerManager.getCurrentAudio().getImageUrl());
                bottomSheetFragment.setArguments(args);

                bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
            }
        });
    }


    private void restartSeekBarUpdater() {
        if (updateSeekBarRunnable != null) handler.removeCallbacks(updateSeekBarRunnable);
        setupSeekBar();
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnPlayPause.setVisibility(View.GONE);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
        btnPlayPause.setVisibility(View.VISIBLE);
    }

    private void safeUpdateUI() {
        if (!isAdded()) return;
        getActivity().runOnUiThread(this::updateUIForCurrentAudio);
    }

    private void updateUIForCurrentAudio() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) return;

        tvSongName.setText(currentAudio.getAudioName());
        tvSongArtist.setText(currentAudio.getAudioArtist() != null ? currentAudio.getAudioArtist() : "Unknown Artist");
        btnPlayPause.setImageResource(PlayerManager.isPlaying() ? R.drawable.pause : R.drawable.play_player);

        if (PlayerManager.getPlayer() != null && PlayerManager.getPlayer().isPlaying()) {
            seekBar.setMax(PlayerManager.getPlayer().getDuration());
            tvDuration.setText(formatTime(PlayerManager.getPlayer().getDuration()));
        }

        loadAlbumArt(currentAudio);
        loadAlbumArtWithPalette(currentAudio);
        broadcastUpdate();
    }

    private void loadAlbumArt(AudioModel audio) {
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(audio.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .into(imgAlbumArt);
        } else {
            imgAlbumArt.setImageResource(R.drawable.video_placholder);
        }
    }

    private void loadAlbumArtWithPalette(AudioModel audio) {
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            Picasso.get()
                    .load(audio.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .into(imgAlbumArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imgAlbumArt.getDrawable()).getBitmap();
                            if (bitmap != null) {
                                Palette.from(bitmap).generate(palette -> {
                                    int darkColor = palette.getDarkVibrantColor(0xFF000000);
                                    imgAlbumArt.setBackground(new android.graphics.drawable.GradientDrawable(
                                            android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                            new int[]{darkColor, 0x00000000}));
                                    View rootView = getView();
                                    if (rootView != null) {
                                        rootView.setBackground(new android.graphics.drawable.GradientDrawable(
                                                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                                new int[]{darkColor, 0xFF121212}));
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            imgAlbumArt.setImageResource(R.drawable.video_placholder);
                        }
                    });
        } else {
            imgAlbumArt.setImageResource(R.drawable.video_placholder);
        }
    }

    private void setupSeekBar() {
        if (PlayerManager.getPlayer() == null) return;

        int currentPosition = PlayerManager.getPlayer().getCurrentPosition();
        int duration = PlayerManager.getPlayer().getDuration();

        // Set SeekBar to current position immediately
        seekBar.setMax(duration);
        seekBar.setProgress(currentPosition);
        tvCurrentTime.setText(formatTime(currentPosition));
        tvDuration.setText(formatTime(duration));

        // Runnable to update SeekBar every second
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded()) return;
                if (PlayerManager.getPlayer() != null) {
                    int currentPos = PlayerManager.getPlayer().getCurrentPosition();
                    seekBar.setProgress(currentPos);
                    tvCurrentTime.setText(formatTime(currentPos));
                }
                handler.postDelayed(this, 500); // Update every 0.5 sec
            }
        };
        handler.post(updateSeekBarRunnable);



        handler.post(updateSeekBarRunnable);

        // SeekBar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && PlayerManager.getPlayer() != null) {
                    PlayerManager.getPlayer().seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }


    private void broadcastUpdate() {
        if (getActivity() != null) {
            String packageName = getActivity().getPackageName();

            android.content.Intent miniPlayerIntent = new android.content.Intent("UPDATE_MINI_PLAYER");
            android.content.Intent audioAdapterIntent = new android.content.Intent("UPDATE_AUDIO_ADAPTER");

            miniPlayerIntent.setPackage(packageName);
            audioAdapterIntent.setPackage(packageName);

            getActivity().sendBroadcast(miniPlayerIntent);
            getActivity().sendBroadcast(audioAdapterIntent);
        }
    }

    private String formatTime(int millis) {
        int minutes = (millis / 1000) / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(updateSeekBarRunnable);
        Log.d(TAG, "Bottom sheet destroyed, removed SeekBar handler callbacks");
    }

    @Override
    public void onStart() {
        super.onStart();
        final View view = getView();
        if (view != null) {
            view.post(() -> {
                View parent = (View) view.getParent();
                if (parent != null) {
                    BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(parent);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true);
                    behavior.setDraggable(true);
                    ViewGroup.LayoutParams params = parent.getLayoutParams();
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    parent.setLayoutParams(params);
                }
            });
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        dialog.setOnShowListener(dlg -> {
            BottomSheetDialog d = (BottomSheetDialog) dlg;
            FrameLayout bottomSheet = d.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<?> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
                behavior.setDraggable(true);
                bottomSheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        });
        return dialog;
    }

    @Override
    public int getTheme() {
        return R.style.FullExpandedBottomSheet;
    }
}
