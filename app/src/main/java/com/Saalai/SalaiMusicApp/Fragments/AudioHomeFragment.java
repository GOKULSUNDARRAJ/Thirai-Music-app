package com.Saalai.SalaiMusicApp.Fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.Saalai.SalaiMusicApp.Adapters.ArtistAdapter;
import com.Saalai.SalaiMusicApp.Adapters.PlaylistSectionAdapter;

import com.Saalai.SalaiMusicApp.Models.ArtistCategory;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.PlaylistSection;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.ShimmerAdapter.ShimmerPlaylistSectionAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AudioHomeFragment extends Fragment implements ArtistAdapter.OnArtistClickListener {

    private RecyclerView sectionsRecyclerView;
    private PlaylistSectionAdapter sectionAdapter;
    private ShimmerPlaylistSectionAdapter shimmerAdapter;
    private LinearLayout emptyStateLayout;
    private Button btnRefresh;
    private ProgressBar progressBar;

    private boolean isLoading = false;
    private boolean isDataLoaded = false;

    private BroadcastReceiver songChangeReceiver;

    private static final int LOADING_DELAY = 1500; // 1.5 seconds

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_home, container, false);

        // Initialize views
        sectionsRecyclerView = view.findViewById(R.id.sectionsRecyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        btnRefresh = view.findViewById(R.id.btnRefresh);
        progressBar = view.findViewById(R.id.progressBar);

        // Setup recycler view
        setupRecyclerView();

        // Setup click listeners
        btnRefresh.setOnClickListener(v -> {
            retryLoading();
        });

        // Setup broadcast receiver
        setupBroadcastReceiver();

        // Start loading data
        showShimmerLoading();
        loadDataWithDelay();

        return view;
    }

    private void showShimmerLoading() {
        isLoading = true;
        isDataLoaded = false;

        // Show shimmer adapter
        if (getContext() != null) {
            shimmerAdapter = new ShimmerPlaylistSectionAdapter(getContext(), 6);
            sectionsRecyclerView.setAdapter(shimmerAdapter);
        }

        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        sectionsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void hideShimmerLoading() {
        isLoading = false;
    }

    private void showContent() {
        hideShimmerLoading();
        if (sectionAdapter != null) {
            sectionsRecyclerView.setAdapter(sectionAdapter);
        }
        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        sectionsRecyclerView.setVisibility(View.VISIBLE);
        isDataLoaded = true;
    }

    private void showEmptyState() {
        hideShimmerLoading();
        sectionsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        isDataLoaded = false;
    }

    private void showProgressBar() {
        sectionsRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void retryLoading() {
        showProgressBar();
        loadDataWithDelay();
    }

    private void loadDataWithDelay() {
        new Handler().postDelayed(() -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    loadData();
                });
            }
        }, LOADING_DELAY);
    }

    private void setupBroadcastReceiver() {
        songChangeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("UPDATE_AUDIO_ADAPTER".equals(action) ||
                        "UPDATE_MINI_PLAYER".equals(action)) {
                    refreshAllAdapters();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction("UPDATE_AUDIO_ADAPTER");
        filter.addAction("UPDATE_MINI_PLAYER");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(songChangeReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            requireActivity().registerReceiver(songChangeReceiver, filter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (songChangeReceiver != null) {
            try {
                requireActivity().unregisterReceiver(songChangeReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
    }

    private void refreshAllAdapters() {
        if (sectionAdapter != null && isDataLoaded) {
            sectionAdapter.refreshPlayingState();
        }
    }

    private void setupRecyclerView() {
        sectionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        sectionAdapter = new PlaylistSectionAdapter(new ArrayList<>(), this);
    }

    private void loadData() {
        // Simulate data loading
        // In real app, fetch from API or database
        boolean hasData = true; // Set to false to test empty state

        if (hasData) {
            List<PlaylistSection> sections = new ArrayList<>();

            // Latest Releases Section
            List<ArtistCategory> latestReleases = new ArrayList<>();

            // Tamil Romantic Songs category
            List<AudioModel> tamilRomanticSongs = Arrays.asList(
                    new AudioModel("Adada Mazhaida",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Adada-Mazhaida.mp3?alt=media&token=9f3cbc54-bff8-4762-8e52-67d45a7dedf0",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(2).jpeg?alt=media&token=1f8290d2-3c93-4000-8f41-52f2dadaffbb"),
                    new AudioModel("En Kadhal Solla",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/En-Kadhal-Solla.mp3?alt=media&token=a6242e73-e73d-43f4-bf62-d738715b0f5d",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(3).jpeg?alt=media&token=ed1386de-16e4-4cba-a4c0-fb3c9e245f0b"),
                    new AudioModel("Poongatre Poongatre",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Poongatre-Poongatre.mp3?alt=media&token=941661d7-f209-46c2-a2a4-bd8e10fff43d",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(4).jpeg?alt=media&token=6b712a64-65be-4eaf-9311-88d9048b5c1d"),
                    new AudioModel("Thuli Thuli Mazhaiyaai",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Thuli-Thuli-Mazhaiyaai.mp3?alt=media&token=0c82b85c-5af0-437f-9203-06fca39d5b14",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(6).jpeg?alt=media&token=630ee223-4385-4ac8-bf75-d7b81aae3e9e")
            );

            // Movie Hits category
            List<AudioModel> movieHits = Arrays.asList(
                    new AudioModel("Eppadio Mattikiten",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Eppadio%20Mattikiten.mp3?alt=media&token=36018104-bac6-452b-ae0b-623cf4a4eda8",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(7).jpeg?alt=media&token=aab77e56-702b-4dbf-b859-ad9d4fbb4218"),
                    new AudioModel("Suthuthe Suthuthe Bhoomi",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Suthuthe-Suthuthe-Bhoomi.mp3?alt=media&token=0ba440ea-6bfe-4192-b2c9-8e488ce6e50c",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(5).jpeg?alt=media&token=5c1e3d77-35de-4132-b3dd-42f57adc7e01"),
                    new AudioModel("Yedho Ondru Ennai",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Yedho-Ondru-Ennai.mp3?alt=media&token=2cedae42-0550-49cf-afd3-8dc7c7b4533b",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/maxresdefault.jpg?alt=media&token=d1eda643-b5f5-49eb-812a-6b13cd4121cc")
            );

            // Set artist for romantic songs
            for (AudioModel song : tamilRomanticSongs) {
                song.setAudioArtist("Various Artists");
            }

            // Set artist for movie hits
            for (AudioModel song : movieHits) {
                song.setAudioArtist("Movie Hits");
            }

            latestReleases.add(new ArtistCategory("Tamil Romantic", tamilRomanticSongs,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(2).jpeg?alt=media&token=1f8290d2-3c93-4000-8f41-52f2dadaffbb", 4));
            latestReleases.add(new ArtistCategory("Movie Hits", movieHits,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(5).jpeg?alt=media&token=5c1e3d77-35de-4132-b3dd-42f57adc7e01", 4));
            latestReleases.add(new ArtistCategory("Tamil Romantic", tamilRomanticSongs,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(2).jpeg?alt=media&token=1f8290d2-3c93-4000-8f41-52f2dadaffbb", 4));
            latestReleases.add(new ArtistCategory("Movie Hits", movieHits,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(3).jpeg?alt=media&token=ed1386de-16e4-4cba-a4c0-fb3c9e245f0b", 4));

            sections.add(new PlaylistSection("Latest Releases", latestReleases,3));

            // Popular Songs Section
            List<ArtistCategory> popularSongs = new ArrayList<>();

            // Emotional Songs category
            List<AudioModel> emotionalSongs = Arrays.asList(
                    new AudioModel("Suthuthe Suthuthe Bhoomi",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Suthuthe-Suthuthe-Bhoomi.mp3?alt=media&token=0ba440ea-6bfe-4192-b2c9-8e488ce6e50c",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(5).jpeg?alt=media&token=5c1e3d77-35de-4132-b3dd-42f57adc7e01"),
                    new AudioModel("Yedho Ondru Ennai",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Yedho-Ondru-Ennai.mp3?alt=media&token=2cedae42-0550-49cf-afd3-8dc7c7b4533b",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/maxresdefault.jpg?alt=media&token=d1eda643-b5f5-49eb-812a-6b13cd4121cc"),
                    new AudioModel("Eppadio Mattikiten",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Eppadio%20Mattikiten.mp3?alt=media&token=36018104-bac6-452b-ae0b-623cf4a4eda8",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(7).jpeg?alt=media&token=aab77e56-702b-4dbf-b859-ad9d4fbb4218")
            );

            // Classic Hits category
            List<AudioModel> classicHits = Arrays.asList(
                    new AudioModel("En Kadhal Solla",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/En-Kadhal-Solla.mp3?alt=media&token=a6242e73-e73d-43f4-bf62-d738715b0f5d",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(3).jpeg?alt=media&token=ed1386de-16e4-4cba-a4c0-fb3c9e245f0b"),
                    new AudioModel("Thuli Thuli Mazhaiyaai",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Thuli-Thuli-Mazhaiyaai.mp3?alt=media&token=0c82b85c-5af0-437f-9203-06fca39d5b14",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(6).jpeg?alt=media&token=630ee223-4385-4ac8-bf75-d7b81aae3e9e")
            );

            // Set artists
            for (AudioModel song : emotionalSongs) {
                song.setAudioArtist("Emotional Hits");
            }
            for (AudioModel song : classicHits) {
                song.setAudioArtist("Classic Favorites");
            }

            popularSongs.add(new ArtistCategory("Emotional Songs", emotionalSongs,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(5).jpeg?alt=media&token=5c1e3d77-35de-4132-b3dd-42f57adc7e01", 1));
            popularSongs.add(new ArtistCategory("Classic Hits", classicHits,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(3).jpeg?alt=media&token=ed1386de-16e4-4cba-a4c0-fb3c9e245f0b", 1));

            sections.add(new PlaylistSection("Popular Songs", popularSongs,1));

            // Your Favorites Section
            List<ArtistCategory> yourFavorites = new ArrayList<>();

            // Create more categories for the grid layout
            List<AudioModel> trendingNow = Arrays.asList(
                    new AudioModel("Adada Mazhaida",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Adada-Mazhaida.mp3?alt=media&token=9f3cbc54-bff8-4762-8e52-67d45a7dedf0",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(2).jpeg?alt=media&token=1f8290d2-3c93-4000-8f41-52f2dadaffbb"),
                    new AudioModel("En Kadhal Solla",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/En-Kadhal-Solla.mp3?alt=media&token=a6242e73-e73d-43f4-bf62-d738715b0f5d",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(3).jpeg?alt=media&token=ed1386de-16e4-4cba-a4c0-fb3c9e245f0b"),
                    new AudioModel("Poongatre Poongatre",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Poongatre-Poongatre.mp3?alt=media&token=941661d7-f209-46c2-a2a4-bd8e10fff43d",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(4).jpeg?alt=media&token=6b712a64-65be-4eaf-9311-88d9048b5c1d")
            );

            List<AudioModel> workoutMix = Arrays.asList(
                    new AudioModel("Suthuthe Suthuthe Bhoomi",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Suthuthe-Suthuthe-Bhoomi.mp3?alt=media&token=0ba440ea-6bfe-4192-b2c9-8e488ce6e50c",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(5).jpeg?alt=media&token=5c1e3d77-35de-4132-b3dd-42f57adc7e01"),
                    new AudioModel("Yedho Ondru Ennai",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Yedho-Ondru-Ennai.mp3?alt=media&token=2cedae42-0550-49cf-afd3-8dc7c7b4533b",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/maxresdefault.jpg?alt=media&token=d1eda643-b5f5-49eb-812a-6b13cd4121cc")
            );

            List<AudioModel> chillVibes = Arrays.asList(
                    new AudioModel("Thuli Thuli Mazhaiyaai",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Thuli-Thuli-Mazhaiyaai.mp3?alt=media&token=0c82b85c-5af0-437f-9203-06fca39d5b14",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(6).jpeg?alt=media&token=630ee223-4385-4ac8-bf75-d7b81aae3e9e"),
                    new AudioModel("Eppadio Mattikiten",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Eppadio%20Mattikiten.mp3?alt=media&token=36018104-bac6-452b-ae0b-623cf4a4eda8",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(7).jpeg?alt=media&token=aab77e56-702b-4dbf-b859-ad9d4fbb4218")
            );

            List<AudioModel> partyAnthems = Arrays.asList(
                    new AudioModel("Adada Mazhaida",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Adada-Mazhaida.mp3?alt=media&token=9f3cbc54-bff8-4762-8e52-67d45a7dedf0",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(2).jpeg?alt=media&token=1f8290d2-3c93-4000-8f41-52f2dadaffbb"),
                    new AudioModel("En Kadhal Solla",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/En-Kadhal-Solla.mp3?alt=media&token=a6242e73-e73d-43f4-bf62-d738715b0f5d",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(3).jpeg?alt=media&token=ed1386de-16e4-4cba-a4c0-fb3c9e245f0b"),
                    new AudioModel("Suthuthe Suthuthe Bhoomi",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/Suthuthe-Suthuthe-Bhoomi.mp3?alt=media&token=0ba440ea-6bfe-4192-b2c9-8e488ce6e50c",
                            "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(5).jpeg?alt=media&token=5c1e3d77-35de-4132-b3dd-42f57adc7e01")
            );

            // Set artists for new categories
            for (AudioModel song : trendingNow) {
                song.setAudioArtist("Trending Now");
            }
            for (AudioModel song : workoutMix) {
                song.setAudioArtist("Workout Mix");
            }
            for (AudioModel song : chillVibes) {
                song.setAudioArtist("Chill Vibes");
            }
            for (AudioModel song : partyAnthems) {
                song.setAudioArtist("Party Anthems");
            }

            // Add categories using Type 4 layout
            yourFavorites.add(new ArtistCategory("Trending Now", trendingNow,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(2).jpeg?alt=media&token=1f8290d2-3c93-4000-8f41-52f2dadaffbb", 2));
            yourFavorites.add(new ArtistCategory("Workout Mix", workoutMix,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(5).jpeg?alt=media&token=5c1e3d77-35de-4132-b3dd-42f57adc7e01", 2));
            yourFavorites.add(new ArtistCategory("Chill Vibes", chillVibes,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(6).jpeg?alt=media&token=630ee223-4385-4ac8-bf75-d7b81aae3e9e", 2));
            yourFavorites.add(new ArtistCategory("Party Anthems", partyAnthems,
                    "https://firebasestorage.googleapis.com/v0/b/course-c3c18.appspot.com/o/download%20(3).jpeg?alt=media&token=ed1386de-16e4-4cba-a4c0-fb3c9e245f0b", 2));

            sections.add(new PlaylistSection("Your Favorites", yourFavorites));

            // Update the adapter
            sectionAdapter.updateData(sections);
            showContent();
        } else {
            showEmptyState();
        }
    }

    @Override
    public void onArtistClick(String artistName, List<AudioModel> songs, String artistImageUrl) {
        // Navigate to AudioFragment with the selected artist's songs
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putString("artist_name", artistName);
        args.putString("artist_image", artistImageUrl);
        args.putSerializable("songs_list", new ArrayList<>(songs));
        fragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        // Broadcast to update mini player immediately
        broadcastMiniPlayerUpdate();
    }

    // Add this method to force update mini player
    private void broadcastMiniPlayerUpdate() {
        if (getContext() != null) {
            Intent intent = new Intent("UPDATE_MINI_PLAYER");
            intent.setPackage(getContext().getPackageName());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().sendBroadcast(intent, null); // No permission required for app-scoped broadcasts
            } else {
                getContext().sendBroadcast(intent);
            }
            Log.d("AudioHomeFragment", "Broadcasting mini player update");
        }
    }

    // Add this to check and play saved audio when fragment becomes visible
    @Override
    public void onResume() {
        super.onResume();
        refreshAllAdapters();
        // Broadcast to update mini player
        broadcastMiniPlayerUpdate();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Clean up shimmer animation
        if (shimmerAdapter != null && isLoading) {
            // Shimmer animation will be automatically stopped when adapter is changed
        }
    }
}