package com.Saalai.SalaiMusicApp;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Notification.NotificationHelper;

import java.io.IOException;
import java.util.List;

public class PlayerManager {

    private static final String TAG = "PlayerManager";
    private static PlayerManager instance;

    private MediaPlayer mediaPlayer;
    private AudioModel currentAudio;
    private List<AudioModel> audioList;
    private int currentIndex = 0;
    private Context context;

    // Notification helper
    private static NotificationHelper notificationHelper;

    // Store both playlists
    private List<AudioModel> onlinePlaylist;
    private List<AudioModel> offlinePlaylist;
    private List<AudioModel> currentPlaylist; // The currently active playlist

    // Listener for UI updates
    public enum PlaylistType {
        ONLINE, OFFLINE
    }

    private PlaylistType currentPlaylistType = PlaylistType.ONLINE;


    public interface OnAudioChangedListener {
        void onAudioChanged(AudioModel newAudio);
    }


    private OnAudioChangedListener listener;

    public void setOnAudioChangedListener(OnAudioChangedListener listener) {
        this.listener = listener;
    }

    private void notifyAudioChanged() {
        if (listener != null && currentAudio != null) {
            listener.onAudioChanged(currentAudio);
        }
    }

    // Private constructor
    private PlayerManager(Context ctx) {
        this.context = ctx.getApplicationContext();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> {
            Log.d(TAG, "Song finished, playing next...");
            playNext(null);
        });

        // Initialize notification helper
        notificationHelper = new NotificationHelper(context);
    }

    public static void init(Context ctx) {
        if (instance == null) {
            instance = new PlayerManager(ctx);
        }
    }

    public static PlayerManager getInstance() {
        if (instance == null) throw new IllegalStateException("PlayerManager not initialized.");
        return instance;
    }

    public static MediaPlayer getPlayer() {
        return getInstance().mediaPlayer;
    }

    public static AudioModel getCurrentAudio() {
        return getInstance().currentAudio;
    }

    public static boolean isPlaying() {
        MediaPlayer mp = getInstance().mediaPlayer;
        return mp != null && mp.isPlaying();
    }

    public static void setAudioList(List<AudioModel> list, PlaylistType type) {
        PlayerManager manager = getInstance();

        if (type == PlaylistType.ONLINE) {
            manager.onlinePlaylist = list;
        } else if (type == PlaylistType.OFFLINE) {
            manager.offlinePlaylist = list;
        }

        // Switch to this playlist
        manager.currentPlaylistType = type;
        manager.currentPlaylist = list;
        manager.currentIndex = 0; // Reset index when playlist changes

        Log.d(TAG, "Set " + type + " playlist with " + (list != null ? list.size() : 0) + " songs");
    }

    public static void setAudioList(List<AudioModel> list) {
        setAudioList(list, PlaylistType.ONLINE);
    }

    public static PlaylistType getCurrentPlaylistType() {
        return getInstance().currentPlaylistType;
    }


    public static void switchToPlaylist(PlaylistType type) {
        PlayerManager manager = getInstance();
        manager.currentPlaylistType = type;

        if (type == PlaylistType.ONLINE && manager.onlinePlaylist != null) {
            manager.currentPlaylist = manager.onlinePlaylist;
        } else if (type == PlaylistType.OFFLINE && manager.offlinePlaylist != null) {
            manager.currentPlaylist = manager.offlinePlaylist;
        }

        // Find current audio in new playlist
        if (manager.currentAudio != null && manager.currentPlaylist != null) {
            for (int i = 0; i < manager.currentPlaylist.size(); i++) {
                if (manager.currentPlaylist.get(i).getAudioUrl().equals(manager.currentAudio.getAudioUrl())) {
                    manager.currentIndex = i;
                    break;
                }
            }
        }
    }


    // Play audio with optional callback when prepared
    public static void playAudio(AudioModel audio, Runnable onPreparedCallback) {
        PlayerManager manager = getInstance();
        if (audio == null || audio.getAudioUrl() == null) return;

        // Find which playlist contains this song and set it as active
        if (manager.onlinePlaylist != null) {
            for (int i = 0; i < manager.onlinePlaylist.size(); i++) {
                if (manager.onlinePlaylist.get(i).getAudioUrl().equals(audio.getAudioUrl())) {
                    manager.currentPlaylist = manager.onlinePlaylist;
                    manager.currentPlaylistType = PlaylistType.ONLINE;
                    manager.currentIndex = i;
                    break;
                }
            }
        }

        // If not found in online, check offline
        if (manager.currentPlaylist == null && manager.offlinePlaylist != null) {
            for (int i = 0; i < manager.offlinePlaylist.size(); i++) {
                if (manager.offlinePlaylist.get(i).getAudioUrl().equals(audio.getAudioUrl())) {
                    manager.currentPlaylist = manager.offlinePlaylist;
                    manager.currentPlaylistType = PlaylistType.OFFLINE;
                    manager.currentIndex = i;
                    break;
                }
            }
        }

        manager.startAudio(audio, onPreparedCallback);
    }


    // In PlayerManager.java - modify the OnCompletionListener setup
    private void startAudio(AudioModel audio, Runnable onPreparedCallback) {
        try {
            Log.d(TAG, "startAudio: " + audio.getAudioName() +
                    ", URL: " + (audio.getAudioUrl() != null ? "valid" : "null"));

            if (mediaPlayer.isPlaying()) {
                Log.d(TAG, "Stopping current playback");
                mediaPlayer.stop();
            }

            mediaPlayer.reset();
            Log.d(TAG, "Setting data source: " + audio.getAudioUrl());
            mediaPlayer.setDataSource(audio.getAudioUrl());

            // Add error listener
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error - what: " + what + ", extra: " + extra);
                return true;
            });

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer prepared, starting playback");
                mp.start();
                currentAudio = audio;
                notifyAudioChanged();
                broadcastAudioChange();
                updateNotification(audio, true);

                mediaPlayer.setOnCompletionListener(m -> {
                    Log.d(TAG, "Song completed: " + audio.getAudioName());
                    playNext(null);
                });

                if (onPreparedCallback != null) onPreparedCallback.run();
            });

            Log.d(TAG, "Preparing async...");
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            Log.e(TAG, "IOException in startAudio: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException in startAudio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void pausePlayback() {
        PlayerManager manager = getInstance();
        MediaPlayer mp = manager.mediaPlayer;
        if (mp != null && mp.isPlaying()) {
            mp.pause();
            // Update notification to show play button
            manager.updateNotification(manager.currentAudio, false);
        }
        manager.broadcastAudioChange();
    }

    public static void resumePlayback() {
        PlayerManager manager = getInstance();
        MediaPlayer mp = manager.mediaPlayer;
        if (mp != null && !mp.isPlaying()) {
            mp.start();
            // Update notification to show pause button
            manager.updateNotification(manager.currentAudio, true);
        }
        manager.broadcastAudioChange();
    }

    public static void stopPlayback() {
        PlayerManager manager = getInstance();
        MediaPlayer mp = manager.mediaPlayer;
        if (mp != null) {
            if (mp.isPlaying()) mp.stop();
            mp.reset();
            manager.currentAudio = null;
            manager.broadcastAudioChange();
            manager.notifyAudioChanged();
            // Cancel notification when playback stops
            stopNotification();
        }
    }

    public static void releasePlayer() {
        PlayerManager manager = getInstance();
        MediaPlayer mp = manager.mediaPlayer;
        if (mp != null) {
            mp.release();
            manager.mediaPlayer = null;
            manager.currentAudio = null;
            // Cancel notification when player is released
            stopNotification();
            instance = null;
            Log.d(TAG, "Player released");
        }
    }

    // Add Runnable callback parameter
    // In PlayerManager.java - modify the playNext method:
    // In PlayerManager.java - modify playNext method:
    public static void playNext(Runnable onPreparedCallback) {
        PlayerManager manager = getInstance();

        Log.d(TAG, "playNext called - currentPlaylist: " +
                (manager.currentPlaylist != null ? manager.currentPlaylist.size() : "null") +
                " songs, currentIndex: " + manager.currentIndex);

        if (manager.currentPlaylist == null || manager.currentPlaylist.isEmpty()) {
            Log.e(TAG, "Cannot play next: playlist is null or empty");
            return;
        }

        // SPECIAL CASE: If playlist has only 1 song, don't auto-play next
        if (manager.currentPlaylist.size() == 1) {
            Log.d(TAG, "Single song playlist - stopping playback instead of looping");

            // Stop playback
            if (manager.mediaPlayer != null && manager.mediaPlayer.isPlaying()) {
                manager.mediaPlayer.stop();
            }

            // Clear current audio
            manager.currentAudio = null;
            manager.broadcastAudioChange();
            manager.notifyAudioChanged();

            // Update notification
            manager.updateNotification(null, false);

            return;
        }

        // Normal case for playlists with multiple songs
        manager.currentIndex++;
        if (manager.currentIndex >= manager.currentPlaylist.size()) {
            manager.currentIndex = 0;
            Log.d(TAG, "Looping back to start of playlist");
        }

        Log.d(TAG, "Next song index: " + manager.currentIndex +
                ", total songs: " + manager.currentPlaylist.size());

        AudioModel nextAudio = manager.currentPlaylist.get(manager.currentIndex);
        Log.d(TAG, "Attempting to play next: " + nextAudio.getAudioName());

        manager.startAudio(nextAudio, onPreparedCallback);
    }

    // Also add logging to startAudio method:



    public static void playPrevious(Runnable onPreparedCallback) {
        PlayerManager manager = getInstance();

        if (manager.currentPlaylist == null || manager.currentPlaylist.isEmpty()) return;

        manager.currentIndex--;
        if (manager.currentIndex < 0) {
            manager.currentIndex = manager.currentPlaylist.size() - 1;
        }

        AudioModel prevAudio = manager.currentPlaylist.get(manager.currentIndex);
        manager.startAudio(prevAudio, onPreparedCallback);
    }

    private void broadcastAudioChange() {
        if (context != null) {
            context.sendBroadcast(new Intent("UPDATE_MINI_PLAYER"));
            context.sendBroadcast(new Intent("UPDATE_AUDIO_ADAPTER"));
        }
    }

    // Notification methods
    private void updateNotification(AudioModel audio, boolean isPlaying) {
        if (notificationHelper != null && audio != null) {
            notificationHelper.showNotification(audio, isPlaying);
        }
    }

    public static void updateNotification() {
        PlayerManager manager = getInstance();
        if (manager.currentAudio != null) {
            manager.updateNotification(manager.currentAudio, isPlaying());
        }
    }

    public static void stopNotification() {
        if (notificationHelper != null) {
            notificationHelper.cancelNotification();
        }
    }

}