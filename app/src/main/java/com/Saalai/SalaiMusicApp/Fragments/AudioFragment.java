package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.AudioAdapter;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AudioFragment extends Fragment {

    private RecyclerView recyclerView;
    private AudioAdapter audioAdapter;
    private ArrayList<AudioModel> audioList;
    private String artistName, artistImageUrl;

    private ImageView imgAlbumArt, btnFullPlayPause,backbtn;
    private TextView tvFullPlayerSongName, tvFullPlayerSongArtist;

    // Broadcast to update adapter
    private final BroadcastReceiver audioUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_AUDIO_ADAPTER".equals(intent.getAction()) && audioAdapter != null) {
                audioAdapter.notifyDataSetChanged();
            }
        }
    };

    // Broadcast to update mini/full player UI
    private final BroadcastReceiver miniPlayerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("UPDATE_MINI_PLAYER".equals(intent.getAction())) {
                AudioModel currentAudio = PlayerManager.getCurrentAudio();
                updateFullPlayerUI(currentAudio);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get data from arguments
        if (getArguments() != null) {
            artistName = getArguments().getString("artist_name");
            artistImageUrl = getArguments().getString("artist_image");
            ArrayList<AudioModel> receivedSongs = (ArrayList<AudioModel>) getArguments().getSerializable("songs_list");

            if (receivedSongs != null) {
                audioList = receivedSongs;
            } else {
                audioList = new ArrayList<>();
            }
        } else {
            audioList = new ArrayList<>();
        }
    }




    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio, container, false);

        PlayerManager.init(getContext());

        // RecyclerView setup
        recyclerView = view.findViewById(R.id.audioRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Full player views
        imgAlbumArt = view.findViewById(R.id.imgAlbumArt);
        tvFullPlayerSongName = view.findViewById(R.id.tvFullPlayerSongName);
        tvFullPlayerSongArtist = view.findViewById(R.id.tvFullPlayerSongArtist);
        btnFullPlayPause = view.findViewById(R.id.btnFullPlayPause);
        backbtn=view.findViewById(R.id.back);

        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
                // If no fragments in back stack, check if activity can handle back press
                else if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            }
        });




        // Update title if we have artist name
        if (artistName != null && getActivity() != null) {
            getActivity().setTitle(artistName + " - Songs");
        }

        // Adapter setup
        // In AudioFragment.java
        audioAdapter = new AudioAdapter(audioList, getContext(), (audioModel, progressBar) -> {
            // Always set online playlist when playing from AudioFragment
            PlayerManager.setAudioList(audioList, PlayerManager.PlaylistType.ONLINE);

            PlayerManager.playAudio(audioModel, () -> {
                progressBar.setVisibility(View.GONE);
                broadcastUpdate();
            });

            updateFullPlayerUI(audioModel);
            Toast.makeText(getContext(), "Playing: " + audioModel.getAudioName(), Toast.LENGTH_SHORT).show();
        });

        recyclerView.setAdapter(audioAdapter);

        // Full player play/pause click
        btnFullPlayPause.setOnClickListener(v -> {
            AudioModel currentAudio = PlayerManager.getCurrentAudio();
            if (currentAudio == null) return;

            if (PlayerManager.isPlaying()) {
                PlayerManager.pausePlayback();
            } else {
                PlayerManager.getPlayer().start();
            }

            btnFullPlayPause.setImageResource(PlayerManager.isPlaying() ? R.drawable.pause : R.drawable.play_player);
            broadcastUpdate();
        });

        // Check for currently playing audio and update UI
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null) {
            updateFullPlayerUI(currentAudio);
        }




        return view;
    }




    private void updateFullPlayerUI(AudioModel audio) {
        if (audio == null) return;

        tvFullPlayerSongName.setText(audio.getAudioName());
        tvFullPlayerSongArtist.setText(audio.getAudioArtist() != null ? audio.getAudioArtist() : "Unknown Artist");
        btnFullPlayPause.setImageResource(PlayerManager.isPlaying() ? R.drawable.pause : R.drawable.play_player);

        if (artistImageUrl != null && !artistImageUrl.isEmpty()) {
            Picasso.get()
                    .load(artistImageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .into(imgAlbumArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            // Extract colors using Palette
                            imgAlbumArt.setDrawingCacheEnabled(true);
                            imgAlbumArt.buildDrawingCache(true);
                            Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) imgAlbumArt.getDrawable()).getBitmap();
                            if (bitmap != null) {
                                Palette.from(bitmap).generate(palette -> {
                                    if (!isAdded() || getContext() == null) return;

                                    int defaultColor = getResources().getColor(R.color.bgblack);
                                    int darkColor = palette.getDarkVibrantColor(defaultColor);

                                    // Avoid pure black colors (skip them)
                                    if (darkColor == android.graphics.Color.BLACK) {
                                        darkColor = defaultColor;
                                    }

                                    android.graphics.drawable.GradientDrawable gradient =
                                            new android.graphics.drawable.GradientDrawable(
                                                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                                    new int[]{defaultColor, darkColor}
                                            );

                                    View rootView = getView();
                                    if (rootView != null) {
                                        rootView.setBackground(gradient);
                                    }
                                });
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            imgAlbumArt.setImageResource(R.drawable.video_placholder);
                            if (getView() != null)
                                getView().setBackgroundColor(getResources().getColor(R.color.bgblack));
                        }
                    });
        } else {
            imgAlbumArt.setImageResource(R.drawable.video_placholder);
            if (getView() != null)
                getView().setBackgroundColor(getResources().getColor(R.color.bgblack));
        }
    }

    private void broadcastUpdate() {
        if (getContext() != null) {
            Context context = getContext();

            // Create explicit intents with your app's package name
            Intent miniPlayerIntent = new Intent("UPDATE_MINI_PLAYER");
            Intent audioAdapterIntent = new Intent("UPDATE_AUDIO_ADAPTER");

            // Set the package to restrict broadcast to your app only
            miniPlayerIntent.setPackage(context.getPackageName());
            audioAdapterIntent.setPackage(context.getPackageName());

            // Send the broadcasts
            context.sendBroadcast(miniPlayerIntent);
            context.sendBroadcast(audioAdapterIntent);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio != null) updateFullPlayerUI(currentAudio);

        // Register receivers with proper flags for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(audioUpdateReceiver, new IntentFilter("UPDATE_AUDIO_ADAPTER"), Context.RECEIVER_NOT_EXPORTED);
            requireContext().registerReceiver(miniPlayerReceiver, new IntentFilter("UPDATE_MINI_PLAYER"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(audioUpdateReceiver, new IntentFilter("UPDATE_AUDIO_ADAPTER"));
            requireContext().registerReceiver(miniPlayerReceiver, new IntentFilter("UPDATE_MINI_PLAYER"));
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(audioUpdateReceiver);
        requireContext().unregisterReceiver(miniPlayerReceiver);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Picasso.get().cancelRequest(imgAlbumArt);
    }
}