package com.Saalai.SalaiMusicApp.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ArtistCategory> artistList;
    private OnArtistClickListener onArtistClickListener;
    private Context context;

    public interface OnArtistClickListener {
        void onArtistClick(String artistName, List<AudioModel> songs, String artistImageUrl);
    }

    public ArtistAdapter(List<ArtistCategory> artistList, OnArtistClickListener listener) {
        this.artistList = artistList;
        this.onArtistClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return artistList.get(position).getAdapterType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == 1) {
            View type1View = inflater.inflate(R.layout.item_artist_type1, parent, false);
            return new Type1ViewHolder(type1View);
        } else if (viewType == 2) {
            View type2View = inflater.inflate(R.layout.item_artist_type2, parent, false);
            return new Type2ViewHolder(type2View);
        } else if (viewType == 3) {
            View type3View = inflater.inflate(R.layout.item_artist_type3, parent, false);
            return new Type3ViewHolder(type3View);
        } else {
            // Type 4
            View type4View = inflater.inflate(R.layout.item_artist_type4, parent, false);
            return new Type4ViewHolder(type4View);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ArtistCategory artist = artistList.get(position);

        // Check if this playlist contains the currently playing song
        boolean containsCurrentSong = containsCurrentPlayingSong(artist);

        int viewType = holder.getItemViewType();
        if (viewType == 1) {
            ((Type1ViewHolder) holder).bind(artist, containsCurrentSong);
        } else if (viewType == 2) {
            ((Type2ViewHolder) holder).bind(artist, containsCurrentSong);
        } else if (viewType == 3) {
            ((Type3ViewHolder) holder).bind(artist, containsCurrentSong);
        } else {
            ((Type4ViewHolder) holder).bind(artist, containsCurrentSong);
        }
    }

    // Method to check if this playlist contains the currently playing song
    private boolean containsCurrentPlayingSong(ArtistCategory artist) {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) return false;

        for (AudioModel song : artist.getSongs()) {
            // Compare by both name and path for accuracy
            if (song.getAudioName().equals(currentAudio.getAudioName()) &&
                    song.getAudioUrl().equals(currentAudio.getAudioUrl())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    public void updateData(List<ArtistCategory> newArtistList) {
        this.artistList = newArtistList;
        notifyDataSetChanged();
    }

    // Method to refresh highlighting
    public void refreshPlayingState() {
        notifyDataSetChanged();
    }

    // Type 1: Square with radius
    class Type1ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;

        public Type1ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentlyPlaying) {
            artistName.setText(artist.getArtistName());
            songCount.setText(artist.getSongs().size() + " songs");

            // Load image
            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(32)))
                    .into(artistImage);

            // Highlight if this playlist contains the currently playing song
            if (isCurrentlyPlaying) {
                // Set green border or background
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.bgred));
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
                songCount.setTextColor(context.getResources().getColor(R.color.bgred));
            } else {
                // Reset to normal colors
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.card_background));
                artistName.setTextColor(context.getResources().getColor(R.color.white));
                songCount.setTextColor(context.getResources().getColor(R.color.light_gray));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getArtistName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl()
                    );
                }
            });
        }
    }

    // Type 2: Round
    class Type2ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;

        public Type2ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentlyPlaying) {
            artistName.setText(artist.getArtistName());
            songCount.setText(artist.getSongs().size() + " songs");

            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .into(artistImage);

            // Highlight if this playlist contains the currently playing song
            if (isCurrentlyPlaying) {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.bgred));
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
                songCount.setTextColor(context.getResources().getColor(R.color.bgred));
            } else {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.card_background));
                artistName.setTextColor(context.getResources().getColor(R.color.white));
                songCount.setTextColor(context.getResources().getColor(R.color.light_gray));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getArtistName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl()
                    );
                }
            });
        }
    }

    // Type 3: Different layout
    class Type3ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;

        public Type3ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentlyPlaying) {
            artistName.setText(artist.getArtistName());
            songCount.setText(artist.getSongs().size() + " songs");

            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                    .into(artistImage);

            // Highlight if this playlist contains the currently playing song
            if (isCurrentlyPlaying) {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.bgred));
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
                songCount.setTextColor(context.getResources().getColor(R.color.bgred));
            } else {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.card_background));
                artistName.setTextColor(context.getResources().getColor(R.color.white));
                songCount.setTextColor(context.getResources().getColor(R.color.light_gray));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getArtistName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl()
                    );
                }
            });
        }
    }

    // Type 4: New layout
    class Type4ViewHolder extends RecyclerView.ViewHolder {
        private TextView artistName;
        private TextView songCount;
        private CardView cardView;
        private ImageView artistImage;


        public Type4ViewHolder(@NonNull View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            songCount = itemView.findViewById(R.id.songCount);
            cardView = itemView.findViewById(R.id.artistCard);
            artistImage = itemView.findViewById(R.id.artistImage);
        }

        public void bind(ArtistCategory artist, boolean isCurrentlyPlaying) {
            artistName.setText(artist.getArtistName());
            songCount.setText(artist.getSongs().size() + " songs");

            // Set artist initial (first character of artist name)
            if (artist.getArtistName() != null && !artist.getArtistName().isEmpty()) {

            }

            // Load image with different transformation for type 4
            Glide.with(itemView.getContext())
                    .load(artist.getArtistImageUrl())
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(24)))
                    .into(artistImage);

            // Highlight if this playlist contains the currently playing song
            if (isCurrentlyPlaying) {
                artistName.setTextColor(context.getResources().getColor(R.color.bgred));
                songCount.setTextColor(context.getResources().getColor(R.color.bgred));

            } else {
                cardView.setCardBackgroundColor(context.getResources().getColor(R.color.card_background));
                artistName.setTextColor(context.getResources().getColor(R.color.white));
                songCount.setTextColor(context.getResources().getColor(R.color.light_gray));
            }

            cardView.setOnClickListener(v -> {
                if (onArtistClickListener != null) {
                    onArtistClickListener.onArtistClick(
                            artist.getArtistName(),
                            artist.getSongs(),
                            artist.getArtistImageUrl()
                    );
                }
            });
        }
    }
}