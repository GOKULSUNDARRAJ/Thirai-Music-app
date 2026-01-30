package com.Saalai.SalaiMusicApp.Adapters;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.MenuBottomSheetFragment;
import com.Saalai.SalaiMusicApp.MenuBottomSheetFragmentDownloads;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class AudioAdapterfordownload extends RecyclerView.Adapter<AudioAdapterfordownload.AudioViewHolder> {

    private ArrayList<AudioModel> audioList;
    private Context context;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(AudioModel audioModel, ProgressBar progressBar);
    }

    public AudioAdapterfordownload(ArrayList<AudioModel> audioList, Context context, OnItemClickListener listener) {
        this.audioList = audioList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio_for_download, parent, false);
        return new AudioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        AudioModel audio = audioList.get(position);
        holder.tvAudioName.setText(audio.getAudioName());

        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        boolean isCurrent = currentAudio != null && currentAudio.getAudioUrl().equals(audio.getAudioUrl());

        // Set default
        holder.bufferProgress.setVisibility(View.GONE);
        stopWaveAnimation(holder);

        if (isCurrent && PlayerManager.isPlaying()) {
            holder.playButton.setImageResource(R.drawable.pause);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
            startWaveAnimation(holder);
        } else {
            holder.playButton.setImageResource(R.drawable.play_player);
            holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.white));
        }

        // Load image
        // Load image
        String imageUrl = audio.getImageUrl();

        Log.d("AudioAdapter", "Image URL: " + imageUrl);

        if (imageUrl != null && !imageUrl.isEmpty()) {

            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.video_placholder)
                    .error(R.drawable.video_placholder)
                    .into(holder.imgAudioArt, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("AudioAdapter", "Image loaded successfully: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("AudioAdapter", "Image loading failed: " + imageUrl, e);
                        }
                    });

        } else {
            Log.w("AudioAdapter", "Image URL is empty. Loading placeholder.");
            holder.imgAudioArt.setImageResource(R.drawable.video_placholder);
        }


        holder.itemView.setOnClickListener(v -> {
            if (isCurrent) {
                if (PlayerManager.isPlaying()) {
                    PlayerManager.pausePlayback();
                    stopWaveAnimation(holder);
                    holder.playButton.setImageResource(R.drawable.play_player);
                    holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.white));
                } else {
                    PlayerManager.resumePlayback();
                    startWaveAnimation(holder);
                    holder.playButton.setImageResource(R.drawable.pause);
                    holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
                }
            } else {
                holder.bufferProgress.setVisibility(View.VISIBLE);
                saveAudioLocally(audio);
                saveFullPlaylist(); // Save the full playlist
                PlayerManager.setAudioList(audioList); // Set the full playlist
                PlayerManager.playAudio(audio, () -> {
                    holder.bufferProgress.setVisibility(View.GONE);
                    startWaveAnimation(holder);
                    holder.playButton.setImageResource(R.drawable.pause);
                    holder.tvAudioName.setTextColor(context.getResources().getColor(R.color.green));
                });

                if (listener != null) {
                    listener.onItemClick(audio, holder.bufferProgress);
                }
            }
        });

        holder.menu.setOnClickListener(v -> {
            // Use the activity's fragment manager instead of child fragment manager
            if (context instanceof AppCompatActivity) {
                AppCompatActivity activity = (AppCompatActivity) context;
                MenuBottomSheetFragmentDownloads bottomSheetFragment = new MenuBottomSheetFragmentDownloads();

                // Pass data to the bottom sheet fragment if needed
                Bundle args = new Bundle();
                args.putString("audioName", audio.getAudioName());
                args.putString("audioUrl", audio.getAudioUrl());
                args.putString("artistName", audio.getAudioArtist());
                args.putString("imageUrl", audio.getImageUrl());
                bottomSheetFragment.setArguments(args);

                bottomSheetFragment.show(activity.getSupportFragmentManager(), "MenuBottomSheet");
            }
        });
    }

    @Override
    public int getItemCount() {
        return audioList.size();
    }

    static class AudioViewHolder extends RecyclerView.ViewHolder {
        TextView tvAudioName;
        ImageView playButton, imgAudioArt,menu;
        ProgressBar bufferProgress;
        View bar1, bar2, bar3;
        View waveContainer;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAudioName = itemView.findViewById(R.id.audioTitle);
            playButton = itemView.findViewById(R.id.playButton);
            imgAudioArt = itemView.findViewById(R.id.imgAudioArt);
            menu = itemView.findViewById(R.id.menu);
            bufferProgress = itemView.findViewById(R.id.imageProgress);
            waveContainer = itemView.findViewById(R.id.waveContainer);
            bar1 = itemView.findViewById(R.id.bar1);
            bar2 = itemView.findViewById(R.id.bar2);
            bar3 = itemView.findViewById(R.id.bar3);
        }
    }

    private void startWaveAnimation(AudioViewHolder holder) {
        holder.waveContainer.setVisibility(View.VISIBLE);

        // 🔹 First bar - small wave
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(holder.bar1, "scaleY", 0.7f, 1.4f);
        anim1.setDuration(300);
        anim1.setRepeatCount(ValueAnimator.INFINITE);
        anim1.setRepeatMode(ValueAnimator.REVERSE);
        anim1.setInterpolator(new LinearInterpolator());
        anim1.start();

        // 🔹 Second bar - medium wave
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(holder.bar2, "scaleY", 0.8f, 1.8f);
        anim2.setDuration(400);
        anim2.setRepeatCount(ValueAnimator.INFINITE);
        anim2.setRepeatMode(ValueAnimator.REVERSE);
        anim2.setInterpolator(new LinearInterpolator());
        anim2.start();

        // 🔹 Third bar - big wave
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(holder.bar3, "scaleY", 0.9f, 2.3f);
        anim3.setDuration(350);
        anim3.setRepeatCount(ValueAnimator.INFINITE);
        anim3.setRepeatMode(ValueAnimator.REVERSE);
        anim3.setInterpolator(new LinearInterpolator());
        anim3.start();

        // Save animators as tags for later stop
        holder.bar1.setTag(anim1);
        holder.bar2.setTag(anim2);
        holder.bar3.setTag(anim3);
    }

    private void stopWaveAnimation(AudioViewHolder holder) {
        holder.waveContainer.setVisibility(View.GONE);

        if (holder.bar1.getTag() instanceof ObjectAnimator) ((ObjectAnimator) holder.bar1.getTag()).cancel();
        if (holder.bar2.getTag() instanceof ObjectAnimator) ((ObjectAnimator) holder.bar2.getTag()).cancel();
        if (holder.bar3.getTag() instanceof ObjectAnimator) ((ObjectAnimator) holder.bar3.getTag()).cancel();

        holder.bar1.setScaleY(1f);
        holder.bar2.setScaleY(1f);
        holder.bar3.setScaleY(1f);
    }

    private void saveAudioLocally(AudioModel audio) {
        if (context != null) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putString("audioName", audio.getAudioName());
            editor.putString("audioUrl", audio.getAudioUrl());
            editor.putString("imageUrl", audio.getImageUrl());
            editor.putString("audioArtist", audio.getAudioArtist());
            editor.apply();

            Log.d("AudioAdapter", "Saved current audio: " + audio.getAudioName() + ", Artist: " + audio.getAudioArtist());
        }
    }

    private void saveFullPlaylist() {
        if (context != null && audioList != null && !audioList.isEmpty()) {
            android.content.SharedPreferences prefs = context.getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);
            android.content.SharedPreferences.Editor editor = prefs.edit();

            // Save playlist size
            editor.putInt("playlistSize", audioList.size());

            // Save each song in the playlist
            for (int i = 0; i < audioList.size(); i++) {
                AudioModel song = audioList.get(i);
                editor.putString("songName_" + i, song.getAudioName());
                editor.putString("songUrl_" + i, song.getAudioUrl());
                editor.putString("songImage_" + i, song.getImageUrl());
                editor.putString("songArtist_" + i, song.getAudioArtist());
            }

            editor.apply();
            Log.d("AudioAdapter", "Saved full playlist with " + audioList.size() + " songs");
        }
    }



}