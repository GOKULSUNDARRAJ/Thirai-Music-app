package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Adapters.AudioAdapter;
import com.Saalai.SalaiMusicApp.Adapters.AudioAdapterfordownload;
import com.Saalai.SalaiMusicApp.AudioDownloadManager;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class AudioDownloadFragment extends Fragment {

    private SaalaiFragment.DrawerToggleListener drawerToggleListener;
    private CircleImageView profileIcon;
    private RecyclerView recyclerViewDownloads;
    private TextView noDownloadsText;
    private ArrayList<AudioModel> offlineSongsList;
    private AudioAdapterfordownload audioAdapter;
    private AudioDownloadManager downloadManager;

    // Broadcast Receiver for song changes
    private BroadcastReceiver songChangeReceiver;
    private boolean isReceiverRegistered = false;

    public interface DrawerToggleListener {
        void onToggleDrawer();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            drawerToggleListener = (SaalaiFragment.DrawerToggleListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DrawerToggleListener");
        }
    }

    // In AudioDownloadFragment.java - modify onCreateView:

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_audio_download, container, false);

        profileIcon = view.findViewById(R.id.iv_user_info);
        recyclerViewDownloads = view.findViewById(R.id.recyclerViewDownloads);
        noDownloadsText = view.findViewById(R.id.noDownloadsText);

        // Initialize download manager
        downloadManager = new AudioDownloadManager(requireContext());

        // Initialize the offline songs list
        offlineSongsList = new ArrayList<>();

        // DON'T set empty playlist here!
        // PlayerManager.setAudioList(offlineSongsList); // REMOVE THIS LINE

        // Setup RecyclerView FIRST (without setting playlist)
        setupRecyclerView();

        // Setup swipe to delete
        setupSwipeToDelete();

        // Load downloaded songs
        loadDownloadedSongs();

        // Set click listener for profile icon
        profileIcon.setOnClickListener(v -> handleProfileClick());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload downloaded songs when fragment resumes
        loadDownloadedSongs();

        // Register broadcast receiver for song changes
        registerSongChangeReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister broadcast receiver
        unregisterSongChangeReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Ensure receiver is unregistered
        unregisterSongChangeReceiver();
    }

    private void registerSongChangeReceiver() {
        if (!isReceiverRegistered && getContext() != null) {
            songChangeReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    Log.d("AudioDownloadFragment", "Broadcast received: " + action);

                    if (action != null) {
                        switch (action) {
                            case "UPDATE_AUDIO_ADAPTER":
                                updateAdapterPlayingState();
                                break;
                            case "DOWNLOAD_UPDATE":
                                // When download is updated (added or deleted), reload the songs
                                Log.d("AudioDownloadFragment", "Reloading downloaded songs after DOWNLOAD_UPDATE");
                                loadDownloadedSongs();
                                break;
                            case "SONG_CHANGED":
                                handleSongChanged();
                                break;
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
            filter.addAction("UPDATE_AUDIO_ADAPTER");
            filter.addAction("DOWNLOAD_UPDATE");
            filter.addAction("SONG_CHANGED");

            // Use ContextCompat for backward compatibility
            androidx.core.content.ContextCompat.registerReceiver(
                    getContext(),
                    songChangeReceiver,
                    filter,
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            );

            isReceiverRegistered = true;
            Log.d("AudioDownloadFragment", "Broadcast receiver registered");
        }
    }

    private void unregisterSongChangeReceiver() {
        if (isReceiverRegistered && songChangeReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(songChangeReceiver);
                isReceiverRegistered = false;
                Log.d("AudioDownloadFragment", "Broadcast receiver unregistered");
            } catch (Exception e) {
                Log.e("AudioDownloadFragment", "Error unregistering receiver: " + e.getMessage());
            }
        }
    }

    private void updateAdapterPlayingState() {
        if (audioAdapter != null) {
            getActivity().runOnUiThread(() -> {
                audioAdapter.notifyDataSetChanged();
                Log.d("AudioDownloadFragment", "Adapter updated for playing state change");
            });
        }
    }

    private void handleSongChanged() {
        // Refresh the list to ensure current playing song is highlighted
        if (audioAdapter != null) {
            getActivity().runOnUiThread(() -> {
                audioAdapter.notifyDataSetChanged();
                Log.d("AudioDownloadFragment", "Adapter updated for song change");
            });
        }
    }

    private void setupRecyclerView() {
        audioAdapter = new AudioAdapterfordownload(offlineSongsList, getContext(), new AudioAdapterfordownload.OnItemClickListener() {
            @Override
            public void onItemClick(AudioModel audioModel, android.widget.ProgressBar progressBar) {
                // Play song from local file path
                playOfflineSong(audioModel, progressBar);
            }
        });

        // DON'T set empty playlist here
        // PlayerManager.setAudioList(offlineSongsList); // REMOVE THIS LINE

        recyclerViewDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDownloads.setAdapter(audioAdapter);
    }

    private void setupSwipeToDelete() {
        ItemTouchHelper.SimpleCallback swipeToDeleteCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                AudioModel deletedSong = offlineSongsList.get(position);

                // Delete the song
                deleteDownloadedSong(deletedSong, position);
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas canvas, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY,
                                    int actionState, boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;

                // Draw red background when swiping
                if (dX < 0) {
                    android.graphics.drawable.Drawable deleteIcon = getResources().getDrawable(R.drawable.baseline_auto_delete_24);
                    int deleteIconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int deleteIconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                    int deleteIconBottom = deleteIconTop + deleteIcon.getIntrinsicHeight();
                    int deleteIconLeft = itemView.getRight() - deleteIconMargin - deleteIcon.getIntrinsicWidth();
                    int deleteIconRight = itemView.getRight() - deleteIconMargin;

                    deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);

                    // Draw red background
                    android.graphics.drawable.Drawable background = new android.graphics.drawable.ColorDrawable(getResources().getColor(R.color.red));
                    background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                    background.draw(canvas);

                    // Draw delete icon
                    deleteIcon.draw(canvas);
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchHelper.attachToRecyclerView(recyclerViewDownloads);
    }

    private void loadDownloadedSongs() {
        // Check if fragment is attached to activity
        if (getActivity() == null || !isAdded()) {
            Log.e("AudioDownloadFragment", "Fragment not attached to activity, skipping loadDownloadedSongs");
            return;
        }

        getActivity().runOnUiThread(() -> {
            // Get downloaded songs from AudioDownloadManager
            List<AudioModel> downloadedSongs = downloadManager.getDownloadedSongs();

            offlineSongsList.clear();

            if (downloadedSongs != null && !downloadedSongs.isEmpty()) {
                offlineSongsList.addAll(downloadedSongs);
                showDownloadsList();
                Log.d("AudioDownloadFragment", "Loaded " + downloadedSongs.size() + " downloaded songs");

                // ONLY set playlist if we have songs AND we're currently playing
                // Let's not automatically set it here
                // PlayerManager.setAudioList(offlineSongsList); // REMOVE THIS LINE
            } else {
                showNoDownloads();
                Log.d("AudioDownloadFragment", "No downloaded songs found");
            }

            // Notify adapter
            if (audioAdapter != null) {
                audioAdapter.notifyDataSetChanged();
                Log.d("AudioDownloadFragment", "Adapter notified of data change");
            } else {
                Log.w("AudioDownloadFragment", "Adapter is null, cannot notify");
            }
        });
    }

    private void showDownloadsList() {
        recyclerViewDownloads.setVisibility(View.VISIBLE);
        noDownloadsText.setVisibility(View.GONE);
    }

    private void showNoDownloads() {
        recyclerViewDownloads.setVisibility(View.GONE);
        noDownloadsText.setVisibility(View.VISIBLE);
        noDownloadsText.setText("No downloaded songs\n\nDownload songs to listen offline");
    }

    // In AudioDownloadFragment.java
    private void playOfflineSong(AudioModel audioModel, android.widget.ProgressBar progressBar) {
        progressBar.setVisibility(View.VISIBLE);

        // Create a new list with JUST this song for offline playback
        // This prevents mixing with the online playlist
        ArrayList<AudioModel> singleSongPlaylist = new ArrayList<>();
        singleSongPlaylist.add(audioModel);

        // Set a special playlist for single offline songs
        PlayerManager.setAudioList(singleSongPlaylist);

        // Play the selected song
        PlayerManager.playAudio(audioModel, () -> {
            progressBar.setVisibility(View.GONE);
            if (audioAdapter != null) {
                audioAdapter.notifyDataSetChanged();
            }
            broadcastSongChanged();
        });

        Log.d("OfflinePlayer", "Playing offline song: " + audioModel.getAudioName());
    }

    private void handleProfileClick() {
        if (drawerToggleListener != null) {
            drawerToggleListener.onToggleDrawer();
        }
    }

    // Method to delete a downloaded song
    private void deleteDownloadedSong(AudioModel audioModel, int position) {
        boolean isDeleted = downloadManager.deleteDownloadedSong(audioModel);

        if (isDeleted) {
            // Remove from list
            offlineSongsList.remove(position);
            audioAdapter.notifyItemRemoved(position);

            // Show snackbar with undo option
            showUndoSnackbar(audioModel, position);

            // Update UI if no downloads left
            if (offlineSongsList.isEmpty()) {
                showNoDownloads();
            }

            Toast.makeText(getContext(), "Song deleted", Toast.LENGTH_SHORT).show();

            // Broadcast download update to refresh other parts of the app
            broadcastDownloadUpdate();

            // Also broadcast song change in case the deleted song was playing
            broadcastSongChanged();
        } else {
            Toast.makeText(getContext(), "Failed to delete song", Toast.LENGTH_SHORT).show();
            // Refresh the list if deletion failed
            audioAdapter.notifyItemChanged(position);
        }
    }

    private void broadcastDownloadUpdate() {
        if (getContext() != null) {
            Intent downloadUpdateIntent = new Intent("DOWNLOAD_UPDATE");
            downloadUpdateIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(downloadUpdateIntent);
            Log.d("AudioDownloadFragment", "DOWNLOAD_UPDATE broadcast sent");
        }
    }

    private void broadcastSongChanged() {
        if (getContext() != null) {
            Intent songChangedIntent = new Intent("SONG_CHANGED");
            songChangedIntent.setPackage(getContext().getPackageName());
            getContext().sendBroadcast(songChangedIntent);
            Log.d("AudioDownloadFragment", "SONG_CHANGED broadcast sent");
        }
    }

    private void showUndoSnackbar(AudioModel deletedSong, int position) {
        Snackbar snackbar = Snackbar.make(recyclerViewDownloads, "Song deleted", Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", v -> {
            // Restore the song
            boolean isRestored = downloadManager.restoreDownloadedSong(deletedSong);
            if (isRestored) {
                offlineSongsList.add(position, deletedSong);
                audioAdapter.notifyItemInserted(position);
                if (offlineSongsList.size() == 1) {
                    showDownloadsList();
                }

                // Update PlayerManager with restored list
                PlayerManager.setAudioList(offlineSongsList);

                // Broadcast updates
                broadcastDownloadUpdate();
                broadcastSongChanged();
            } else {
                Toast.makeText(getContext(), "Failed to restore song", Toast.LENGTH_SHORT).show();
            }
        });
        snackbar.show();
    }
}