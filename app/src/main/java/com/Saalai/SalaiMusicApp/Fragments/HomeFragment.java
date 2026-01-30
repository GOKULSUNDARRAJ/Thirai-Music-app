package com.Saalai.SalaiMusicApp.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; // Import LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.Saalai.SalaiMusicApp.Adapters.TopTabAdapter;
import com.Saalai.SalaiMusicApp.Adapters.TopTabAdapterMusic;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.NavigationDataManager;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.Fragments.TopMenuViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    // Views
    private FrameLayout fragmentContainer;
    private RecyclerView topTabsRecycler;

    // Data
    private List<TopNavItem> topNavItems = new ArrayList<>();
    private int selectedTab = 0;

    // Adapters
    private TopTabAdapterMusic topTabAdapter;

    // ViewModel
    private TopMenuViewModel viewModel;

    // Current fragment
    private Fragment currentFragment;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(TopMenuViewModel.class);

        initializeViews(view);
        setupTopTabsRecycler();

        // Observe ViewModel for top menu changes
        observeTopMenuViewModel();

        // Load from SharedPreferences as fallback
        loadTopMenuFromSharedPrefs();

        return view;
    }

    private void initializeViews(View view) {
        topTabsRecycler = view.findViewById(R.id.top_tabs_recycler);
        fragmentContainer = view.findViewById(R.id.fragment_container);
    }

    private void setupTopTabsRecycler() {
        // Use LinearLayoutManager instead of GridLayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );

        topTabsRecycler.setLayoutManager(layoutManager);
        topTabsRecycler.setHasFixedSize(false);

        // Create adapter with click listener - using TopTabAdapterMusic
        topTabAdapter = new TopTabAdapterMusic(
                requireContext(),
                topNavItems,
                new TopTabAdapterMusic.OnTabClickListener() {  // Use TopTabAdapterMusic.OnTabClickListener
                    @Override
                    public void onTabClick(int position) {
                        Log.d("HomeFragment", "Top tab clicked at: " + position);
                        selectTab(position);
                    }
                }
        );

        topTabsRecycler.setAdapter(topTabAdapter);
    }

    private void observeTopMenuViewModel() {
        viewModel.getTopMenuLiveData().observe(getViewLifecycleOwner(), new Observer<List<TopNavItem>>() {
            @Override
            public void onChanged(List<TopNavItem> items) {
                if (items != null && !items.isEmpty()) {
                    Log.d("HomeFragment", "Received top menu from ViewModel: " + items.size() + " items");
                    updateTopMenu(items);
                } else {
                    Log.d("HomeFragment", "No top menu data in ViewModel");
                }
            }
        });
    }

    private void loadTopMenuFromSharedPrefs() {
        Log.d("HomeFragment", "Loading top menu from SharedPreferences...");

        NavigationDataManager navManager = NavigationDataManager.getInstance(requireContext());

        if (navManager.isNavigationLoaded()) {
            List<TopNavItem> topItems = navManager.getTopNavigation();
            if (!topItems.isEmpty()) {
                Log.d("HomeFragment", "Found top nav in SharedPreferences: " + topItems.size() + " items");
                updateTopMenu(topItems);
            } else {
                Log.d("HomeFragment", "No top navigation in SharedPreferences");

            }
        } else {
            Log.d("HomeFragment", "Navigation not loaded in SharedPreferences");

        }
    }

    private void updateTopMenu(List<TopNavItem> items) {
        this.topNavItems.clear();
        this.topNavItems.addAll(items);

        if (topTabAdapter != null) {
            topTabAdapter.notifyDataSetChanged();

            // Since we're using LinearLayoutManager, we don't need to set span count
            // LinearLayoutManager doesn't have setSpanCount() method

            // Select first tab by default
            if (!items.isEmpty()) {
                selectTab(0);
            }
        }
    }

    private void selectTab(int position) {
        if (position < 0 || position >= topNavItems.size()) {
            Log.e("HomeFragment", "Invalid tab position: " + position);
            return;
        }

        Log.d("HomeFragment", "selectTab: " + position);
        selectedTab = position;

        if (topTabAdapter != null) {
            topTabAdapter.setSelectedPosition(position);
        }

        // Load corresponding fragment
        loadFragmentForTab(position);

        // Scroll to position
        scrollToPosition(position);
    }

    private void loadFragmentForTab(int position) {
        if (position >= 0 && position < topNavItems.size()) {
            TopNavItem item = topNavItems.get(position);
            String tabName = item.getTopmenuName().toLowerCase();

            Fragment fragment = null;

            switch (tabName) {
                case "all":
                    fragment = new AudioHomeFragment();
                    break;
                case "music":
                    fragment = new MusicFragment();
                    break;
                case "live tv":
                case "live":
                    fragment = new LiveTvFragment();
                    break;
                case "movies":
                    fragment = new MoviesFragment();
                    break;
                case "tv shows":
                case "tvshows":
                    fragment = new TvShowsFragment();
                    break;
                case "catch up":
                case "catchup":
                    fragment = new CatchUpFragment();
                    break;
                case "devotion":
                    // Create or use your DevotionFragment
                    // fragment = new DevotionFragment();
                    fragment = new AudioHomeFragment(); // Default for now
                    break;
                default:
                    fragment = new AudioHomeFragment();
                    break;
            }

            if (fragment != null && fragmentContainer != null) {
                currentFragment = fragment;
                FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                transaction.replace(R.id.fragment_container, fragment);
                transaction.commit();
            }
        }
    }

    private void scrollToPosition(int position) {
        if (topTabsRecycler != null && topTabAdapter != null) {
            topTabsRecycler.smoothScrollToPosition(position);
        }
    }



    // ===================== EXISTING AUDIO METHODS =====================

    private void broadcastMiniPlayerUpdate() {
        if (getContext() != null) {
            Intent intent = new Intent("UPDATE_MINI_PLAYER");
            intent.setPackage(getContext().getPackageName());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().sendBroadcast(intent, null);
            } else {
                getContext().sendBroadcast(intent);
            }
            Log.d("HomeFragment", "Broadcasting mini player update");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check if current song is null and play saved audio
        checkAndPlaySavedAudio();

        // Broadcast to update mini player
        broadcastMiniPlayerUpdate();
    }

    private void checkAndPlaySavedAudio() {
        if (PlayerManager.getCurrentAudio() == null) {
            AudioModel savedAudio = loadSavedAudio();
            ArrayList<AudioModel> savedPlaylist = loadFullPlaylist();

            if (savedAudio != null && !savedPlaylist.isEmpty()) {
                PlayerManager.setAudioList(savedPlaylist);
                AudioModel audioToPlay = findAudioInPlaylist(savedAudio, savedPlaylist);

                if (audioToPlay != null) {
                    PlayerManager.playAudio(audioToPlay, () -> {
                        Log.d("HomeFragment", "Saved audio started playing: " + audioToPlay.getAudioName());

                        if (PlayerManager.isPlaying()){
                            PlayerManager.pausePlayback();
                        }
                        broadcastMiniPlayerUpdate();
                    });
                    Log.d("HomeFragment", "Playing saved audio from playlist: " + audioToPlay.getAudioName());
                }
            }
        } else {
            Log.d("HomeFragment", "Audio is already playing: " + PlayerManager.getCurrentAudio().getAudioName());
        }
    }

    private AudioModel loadSavedAudio() {
        SharedPreferences prefs = requireContext().getSharedPreferences("SavedAudio", Context.MODE_PRIVATE);
        String name = prefs.getString("audioName", null);
        String url = prefs.getString("audioUrl", null);
        String image = prefs.getString("imageUrl", null);
        String artist = prefs.getString("audioArtist", null);

        Log.d("SavedAudio", "Loading saved audio - Name: " + name + ", URL: " + url + ", Artist: " + artist);

        if (name != null && url != null) {
            AudioModel savedAudio = new AudioModel(name, url, image);
            savedAudio.setAudioArtist(artist != null ? artist : "Unknown Artist");
            Log.d("SavedAudio", "Successfully loaded: " + savedAudio.getAudioName());
            return savedAudio;
        } else {
            Log.d("SavedAudio", "No saved audio found or incomplete data");
            return null;
        }
    }

    private ArrayList<AudioModel> loadFullPlaylist() {
        ArrayList<AudioModel> playlist = new ArrayList<>();
        SharedPreferences prefs = requireContext().getSharedPreferences("SavedPlaylist", Context.MODE_PRIVATE);

        int playlistSize = prefs.getInt("playlistSize", 0);

        if (playlistSize > 0) {
            for (int i = 0; i < playlistSize; i++) {
                String name = prefs.getString("songName_" + i, null);
                String url = prefs.getString("songUrl_" + i, null);
                String image = prefs.getString("songImage_" + i, null);
                String artist = prefs.getString("songArtist_" + i, null);

                if (name != null && url != null) {
                    AudioModel song = new AudioModel(name, url, image);
                    song.setAudioArtist(artist != null ? artist : "Unknown Artist");
                    playlist.add(song);
                }
            }
            Log.d("HomeFragment", "Loaded full playlist with " + playlist.size() + " songs");
        } else {
            Log.d("HomeFragment", "No saved playlist found");
        }

        return playlist;
    }

    private AudioModel findAudioInPlaylist(AudioModel savedAudio, ArrayList<AudioModel> playlist) {
        for (AudioModel song : playlist) {
            if (song.getAudioUrl().equals(savedAudio.getAudioUrl())) {
                return song;
            }
        }
        return savedAudio;
    }
}