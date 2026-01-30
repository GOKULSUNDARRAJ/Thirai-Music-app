package com.Saalai.SalaiMusicApp.Activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Saalai.SalaiMusicApp.Fragments.HomeFragment;
import com.Saalai.SalaiMusicApp.Fragments.ProfileFragment;
import com.bumptech.glide.Glide;
import com.Saalai.SalaiMusicApp.Adapters.BottomNavAdapter;
import com.Saalai.SalaiMusicApp.ApiService.ApiClient;
import com.Saalai.SalaiMusicApp.ApiService.ApiService;
import com.Saalai.SalaiMusicApp.Fragments.AudioDownloadFragment;
import com.Saalai.SalaiMusicApp.Fragments.CatchUpDetailFragment;
import com.Saalai.SalaiMusicApp.Fragments.MovieVideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.RadioFragment;
import com.Saalai.SalaiMusicApp.Fragments.RadioPlayerFragment;
import com.Saalai.SalaiMusicApp.Fragments.SaalaiFragment;
import com.Saalai.SalaiMusicApp.Fragments.TopMenuViewModel;
import com.Saalai.SalaiMusicApp.Fragments.TvShowEpisodeFragment;
import com.Saalai.SalaiMusicApp.Fragments.VideoPlayerFragment;
import com.Saalai.SalaiMusicApp.Models.AudioModel;
import com.Saalai.SalaiMusicApp.Models.BottomNavItem;
import com.Saalai.SalaiMusicApp.Models.NavigationDataManager;
import com.Saalai.SalaiMusicApp.Models.TopNavItem;
import com.Saalai.SalaiMusicApp.PlayerBottomSheetFragment;
import com.Saalai.SalaiMusicApp.PlayerManager;
import com.Saalai.SalaiMusicApp.R;
import com.Saalai.SalaiMusicApp.SharedPrefManager.SharedPrefManager;


import androidx.palette.graphics.Palette;

import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity implements
        VideoPlayerFragment.VideoPlayerListener,
        MovieVideoPlayerFragment.MoviePlayerListener,
        TvShowEpisodeFragment.TvShowPlayerListener,
        CatchUpDetailFragment.CatchUpPlayerListener,
        SaalaiFragment.DrawerToggleListener,
        RadioFragment.DrawerToggleListener,
        AudioDownloadFragment.DrawerToggleListener,
        RadioPlayerFragment.RadioPlayerListener {

    // Dynamic Bottom Navigation
    private RecyclerView bottomNavRecyclerView;
    private BottomNavAdapter bottomNavAdapter;
    private List<BottomNavItem> bottomNavItems = new ArrayList<>();

    private int selectedTab = 1;

    // Broadcast Receivers
    private BroadcastReceiver closeReceiver;
    private BroadcastReceiver miniPlayerReceiver;

    // Mini Player Views
    private LinearLayout miniAudioPlayer;
    private TextView tvMiniSongName, tvMiniSongArtist;
    private ImageView btnMiniPlayPause, imgMiniAlbumArt;
    private ProgressBar miniProgressBar;
    private final Handler seekBarHandler = new Handler();
    private Runnable updateSeekBarRunnable;
    private SharedPrefManager sharedPrefManager;

    DrawerLayout drawerLayout;

    private Bundle fragmentState = new Bundle();
    private String currentFragmentTag = "";
    private boolean isDataLoaded = false;
    private boolean shouldRestoreState = false;
    private static final String SCROLL_POSITION_KEY = "scroll_position";
    private static final String PREF_NAME = "all_fragment_state";
    private int scrollYPosition = 0;
    private Parcelable recyclerViewState;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.black));
        }

        View rootView = findViewById(R.id.root_view);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && Build.VERSION.SDK_INT <= 34) {
            // For Android 11 to 14, explicitly disable edge-to-edge
            getWindow().setDecorFitsSystemWindows(true);

            if (rootView != null) {
                rootView.setOnApplyWindowInsetsListener((v, insets) -> {
                    v.setPadding(0, 0, 0, 0);
                    return insets;
                });
            }
        } else {
            // Pre-Android 12
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            if (rootView != null) {
                rootView.setFitsSystemWindows(true); // critical for proper layout
            }
        }

        Log.d("MainActivity", "=== MAIN ACTIVITY STARTED ===");

        // Load navigation from SharedPreferences (NO API CALL)
        loadNavigationFromSharedPrefs();

        PlayerManager.init(this);
        initViews();
        initDynamicBottomNavigation();
        setupCloseReceiver();
        initMiniPlayer();
        setupMiniPlayerReceiver();

        // Mini player click opens bottom sheet
        if (miniAudioPlayer != null) {
            miniAudioPlayer.setOnClickListener(v -> {
                PlayerBottomSheetFragment bottomSheet = new PlayerBottomSheetFragment();
                bottomSheet.show(getSupportFragmentManager(), "FullPlayerSheet");
            });
        }

        sharedPrefManager = SharedPrefManager.getInstance(this);

        String userName = sharedPrefManager.getUserName();
        String userMobile = sharedPrefManager.getUserMobile();

        drawerLayout = findViewById(R.id.drawer_layout);
        LinearLayout drawerContainer = findViewById(R.id.custom_drawer_container);



        ImageView profileIcon = findViewById(R.id.iv_user_info);
        if (profileIcon != null) {
            profileIcon.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(Gravity.LEFT);
                }
            });
        }

        ConstraintLayout cardView = findViewById(R.id.cardView);
        LinearLayout btnHelp = findViewById(R.id.helpandsupportll);
        LinearLayout termsll = findViewById(R.id.termsll);
        ImageView closell = findViewById(R.id.close);
        ConstraintLayout lllogout = findViewById(R.id.lllogout);

        if (cardView != null) {
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Handle card view click
                }
            });
        }



        if (drawerLayout != null) {
            drawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));
        }

        TextView tvUserName = findViewById(R.id.username);
        TextView tvUserMobile = findViewById(R.id.usermobile);

        if (tvUserName != null && tvUserMobile != null) {
            tvUserName.setText(userName);
            tvUserMobile.setText(userMobile);
        }

        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, HelpandSupportActivity.class));
            });
        }

        if (termsll != null) {
            termsll.setOnClickListener(v -> {

                startActivity(new Intent(MainActivity.this, TermsActivity.class));
            });
        }

        if (closell != null) {
            closell.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.closeDrawers();
            });
        }

        if (lllogout != null) {
            lllogout.setOnClickListener(v -> {
                callLogoutApi();
            });
        }

        TextView tvVersionName = findViewById(R.id.versionname);

        try {
            // Get app version name from the package manager
            String versionName = getPackageManager()
                    .getPackageInfo(getPackageName(), 0)
                    .versionName;

            // Set text in TextView
            if (tvVersionName != null) {
                tvVersionName.setText("Version: " + versionName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (tvVersionName != null) {
                tvVersionName.setText("Version: N/A");
            }
        }

        if (drawerLayout != null) {
            // Disable swiping from edges
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            // Optional: Allow drawer to be opened programmatically but not via swipe
            // drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    // ===================== LOAD FROM SHARED PREFERENCES =====================
    private void loadNavigationFromSharedPrefs() {
        Log.d("MainActivity", "Loading navigation from SharedPreferences...");

        NavigationDataManager navManager = NavigationDataManager.getInstance(this);

        if (navManager.isNavigationLoaded()) {
            // Load bottom navigation
            List<BottomNavItem> bottomItems = navManager.getBottomNavigation();
            if (!bottomItems.isEmpty()) {
                Log.d("MainActivity", "Bottom nav found in SharedPreferences: " + bottomItems.size() + " items");
                updateBottomNavigation(bottomItems);
            } else {
                Log.d("MainActivity", "No bottom navigation in SharedPreferences");
                // Load default fragment
                loadFragment(new HomeFragment());
            }

            // Load top navigation
            List<TopNavItem> topItems = navManager.getTopNavigation();
            if (!topItems.isEmpty()) {
                Log.d("MainActivity", "Top nav found in SharedPreferences: " + topItems.size() + " items");
                updateTopMenuInFragment(topItems);
            } else {
                Log.d("MainActivity", "No top navigation in SharedPreferences");
            }
        } else {
            Log.d("MainActivity", "Navigation not loaded in SharedPreferences");
            // Show default fragment
            loadFragment(new HomeFragment());
        }
    }

    // ===================== INIT VIEWS =====================
    private void initViews() {
        imgMiniAlbumArt = findViewById(R.id.imgMiniAlbumArt);

        // Hide old static navigation
        LinearLayout oldBottomNav = findViewById(R.id.custom_bottom_navigation);
        if (oldBottomNav != null) {
            oldBottomNav.setVisibility(View.GONE);
        }

        // Initially load default fragment (will be replaced if we have data)
        if (bottomNavItems.isEmpty()) {
            loadFragment(new HomeFragment());
        }
    }

    // ===================== DYNAMIC BOTTOM NAVIGATION =====================
    private void initDynamicBottomNavigation() {
        bottomNavRecyclerView = findViewById(R.id.bottom_nav_recycler_view);

        if (bottomNavRecyclerView == null) {
            Log.e("MainActivity", "Bottom nav RecyclerView not found in layout!");
            return;
        }

        // Calculate column count based on bottom nav items size
        // You can adjust the logic based on your needs
        int columnCount = calculateColumnCount(bottomNavItems);

        GridLayoutManager layoutManager = new GridLayoutManager(this, columnCount, GridLayoutManager.VERTICAL, false);

        // Disable scrolling since all items should be visible
        bottomNavRecyclerView.setNestedScrollingEnabled(false);

        bottomNavRecyclerView.setLayoutManager(layoutManager);

        // Initialize adapter
        bottomNavAdapter = new BottomNavAdapter(bottomNavItems, new BottomNavAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (position >= 0 && position < bottomNavItems.size()) {
                    BottomNavItem selectedItem = bottomNavItems.get(position);
                    loadFragmentForBottomNav(selectedItem);
                }
            }
        });

        bottomNavRecyclerView.setAdapter(bottomNavAdapter);

        Log.d("MainActivity", "Bottom navigation initialized with " + columnCount + " columns");
    }

    // Method to calculate column count based on items
    private int calculateColumnCount(List<BottomNavItem> items) {
        if (items == null || items.isEmpty()) {
            return 3; // Default to 3 if no items
        }

        int itemCount = items.size();

        // Example logic:
        // - 1-2 items: 2 columns
        // - 3-4 items: 3 columns
        // - 5+ items: 4 columns (maximum for bottom nav)

        if (itemCount <= 2) {
            return 2;
        } else if (itemCount <= 4) {
            return 3;
        } else {
            return 4; // Maximum recommended for bottom navigation
        }
    }

    // ===================== UPDATE TOP MENU IN FRAGMENT =====================
    private void updateTopMenuInFragment(List<TopNavItem> topItems) {
        SaalaiFragment saalaiFragment = (SaalaiFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_container);

        if (saalaiFragment != null) {
            saalaiFragment.updateTopMenu(topItems);
            Log.d("MainActivity", "Top menu updated in existing SaalaiFragment");
        } else {
            // Store in TopMenuViewModel for when fragment is created
            TopMenuViewModel topMenuViewModel = new ViewModelProvider(MainActivity.this)
                    .get(TopMenuViewModel.class);
            topMenuViewModel.setTopMenu(topItems);
            Log.d("MainActivity", "Top menu saved to ViewModel: " + topItems.size() + " items");
        }
    }

    // ===================== UPDATE BOTTOM NAVIGATION =====================
    private void updateBottomNavigation(List<BottomNavItem> items) {
        // Update the adapter
        bottomNavItems.clear();
        bottomNavItems.addAll(items);

        if (bottomNavAdapter != null) {
            bottomNavAdapter.notifyDataSetChanged();
        }

        // Select first item by default and load its fragment
        if (!bottomNavItems.isEmpty()) {
            if (bottomNavAdapter != null) {
                bottomNavAdapter.setSelectedPosition(0);
            }
            loadFragmentForBottomNav(bottomNavItems.get(0));
            Log.d("MainActivity", "Loaded first fragment: " + bottomNavItems.get(0).getBottommenuName());
        }

        Log.d("MainActivity", "Bottom navigation updated with " + items.size() + " items");
    }

    // ===================== LOAD FRAGMENT FOR BOTTOM NAV =====================
    private void loadFragmentForBottomNav(BottomNavItem item) {
        Fragment fragment = null;
        String fragmentName = item.getBottommenuName().toLowerCase();

        // Map bottom menu items to fragments
        switch (fragmentName) {
            case "saalai":
                fragment = new HomeFragment();
                break;
            case "radio":
                fragment = new AudioDownloadFragment();
                break;
            case "audio":
                fragment = new ProfileFragment();
                break;
            default:
                // Try to match by ID if name doesn't match
                switch (item.getBottommenuId()) {
                    case 1: // Saalai
                        fragment = new HomeFragment();
                        break;
                    case 2: // Radio
                        fragment = new AudioDownloadFragment();
                        break;
                    case 3: // Audio
                        fragment = new ProfileFragment();
                        break;
                    default:
                        fragment = new HomeFragment();
                        break;
                }
                break;
        }

        if (fragment != null) {
            loadFragment(fragment);
        }
    }

    // ===================== FRAGMENT LOADING =====================
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // ===================== CLOSE RECEIVER =====================
    private void setupCloseReceiver() {
        closeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("CLOSE_MAIN_ACTIVITY".equals(intent.getAction())) {
                    finish();
                }
            }
        };

        IntentFilter filter = new IntentFilter("CLOSE_MAIN_ACTIVITY");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(closeReceiver, filter);
        }
    }

    // ===================== MINI PLAYER =====================
    private void initMiniPlayer() {
        miniAudioPlayer = findViewById(R.id.miniaudioplayer);
        tvMiniSongName = findViewById(R.id.tvMiniSongName);
        tvMiniSongArtist = findViewById(R.id.tvMiniSongArtist);
        btnMiniPlayPause = findViewById(R.id.btnMiniPlayPause);
        miniProgressBar = findViewById(R.id.miniProgressBar);

        if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.GONE);

        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> toggleMiniPlayer());
        }

        // SeekBar updater
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (PlayerManager.getPlayer() != null && PlayerManager.getCurrentAudio() != null) {
                    int duration = PlayerManager.getPlayer().getDuration();
                    int current = PlayerManager.getPlayer().getCurrentPosition();
                    if (duration > 0) {
                        int progress = (int) (((float) current / duration) * 100);
                        miniProgressBar.setProgress(progress);
                    }
                }
                seekBarHandler.postDelayed(this, 500);
            }
        };
        seekBarHandler.post(updateSeekBarRunnable);
    }

    private void toggleMiniPlayer() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.GONE);
            return;
        }

        if (PlayerManager.isPlaying()) {
            PlayerManager.pausePlayback();
            if (btnMiniPlayPause != null) btnMiniPlayPause.setImageResource(R.drawable.play_player);
        } else {
            if (PlayerManager.getPlayer() != null) {
                PlayerManager.getPlayer().start();
                if (btnMiniPlayPause != null) btnMiniPlayPause.setImageResource(R.drawable.pause);
            }
        }

        loadMiniPlayerAlbumArt(currentAudio);
        sendBroadcast(new Intent("UPDATE_MINI_PLAYER"));
        sendBroadcast(new Intent("UPDATE_AUDIO_ADAPTER"));
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void setupMiniPlayerReceiver() {
        miniPlayerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("UPDATE_MINI_PLAYER".equals(intent.getAction())) {
                    updateMiniPlayerUI();
                }
            }
        };

        IntentFilter filter = new IntentFilter("UPDATE_MINI_PLAYER");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(miniPlayerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(miniPlayerReceiver, filter);
        }
    }

    private void updateMiniPlayerUI() {
        AudioModel currentAudio = PlayerManager.getCurrentAudio();
        if (currentAudio == null) {
            if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.GONE);
            return;
        }

        if (tvMiniSongName != null) tvMiniSongName.setText(currentAudio.getAudioName());
        if (tvMiniSongArtist != null) {
            tvMiniSongArtist.setText(currentAudio.getAudioArtist() != null ?
                    currentAudio.getAudioArtist() : "Unknown Artist");
        }
        if (miniAudioPlayer != null) miniAudioPlayer.setVisibility(View.VISIBLE);
        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setImageResource(PlayerManager.isPlaying() ? R.drawable.pause : R.drawable.play_player);
        }
        loadMiniPlayerAlbumArt(currentAudio);
    }

    private void loadMiniPlayerAlbumArt(AudioModel audio) {
        if (audio.getImageUrl() != null && !audio.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .asBitmap()
                    .load(audio.getImageUrl())
                    .placeholder(R.drawable.video_placholder)
                    .into(new com.bumptech.glide.request.target.BitmapImageViewTarget(imgMiniAlbumArt) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            super.setResource(resource);
                            if (resource != null) {
                                Palette.from(resource).generate(palette -> {
                                    int defaultColor = getResources().getColor(R.color.gray);
                                    int lightColor = palette.getLightVibrantColor(defaultColor);
                                    if (miniAudioPlayer != null) {
                                        miniAudioPlayer.setBackgroundColor(lightColor);
                                    }
                                });
                            }
                        }
                    });
        } else {
            if (imgMiniAlbumArt != null) imgMiniAlbumArt.setImageResource(R.drawable.video_placholder);
            if (miniAudioPlayer != null) miniAudioPlayer.setBackgroundColor(getResources().getColor(R.color.gray));
        }
    }

    // ===================== LOGOUT API =====================
    private void callLogoutApi() {
        SharedPrefManager sp = SharedPrefManager.getInstance(MainActivity.this);
        String accessToken = sp.getAccessToken();

        LinearLayout btnLayout = findViewById(R.id.btnlayout);
        ProgressBar progressBar = findViewById(R.id.progressBar);

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e("Logout", "No token found, user already logged out");
            return;
        }

        // Show loading
        if (btnLayout != null) btnLayout.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ResponseBody> call = apiService.logout(accessToken);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // Hide loading
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnLayout != null) btnLayout.setVisibility(View.VISIBLE);

                if (response.isSuccessful()) {
                    Log.d("Logout", "User Logged Out successfully");

                    sp.clearAccessToken();
                    if (drawerLayout != null) drawerLayout.closeDrawers();

                    Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Log.e("Logout", "Failed to logout, server code: " + response.code());
                    Toast.makeText(MainActivity.this, "Logout failed. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Hide loading
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (btnLayout != null) btnLayout.setVisibility(View.VISIBLE);

                Log.e("Logout", "Logout API call failed", t);
                Toast.makeText(MainActivity.this, "Network error. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ===================== PLAYER LISTENER METHODS =====================
    @Override
    public void onVideoPlayerStarted() {
        hideNavigationBars();
    }

    @Override
    public void onVideoPlayerFinished() {
        showNavigationBars();
    }

    @Override
    public void onMoviePlayerStarted() {
        hideNavigationBars();
    }

    @Override
    public void onMoviePlayerFinished() {
        showNavigationBars();
    }

    @Override
    public void onTvShowPlayerStarted() {
        hideNavigationBars();
    }

    @Override
    public void onTvShowPlayerFinished() {
        showNavigationBars();
    }

    @Override
    public void onCatchUpPlayerStarted() {
        hideNavigationBars();
    }

    @Override
    public void onCatchUpPlayerFinished() {
        showNavigationBars();
    }

    @Override
    public void onRadioPlayerStarted() {
        hideNavigationBars();
    }

    @Override
    public void onRadioPlayerFinished() {
        showNavigationBars();
    }

    // ===================== BACK PRESS HANDLING =====================
    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (fragment instanceof VideoPlayerFragment) {
            VideoPlayerFragment videoFragment = (VideoPlayerFragment) fragment;
            if (videoFragment.handleBackPress()) {
                return;
            }
            showNavigationBars();
            getSupportFragmentManager().popBackStack();

        } else if (fragment instanceof MovieVideoPlayerFragment) {
            MovieVideoPlayerFragment movieFragment = (MovieVideoPlayerFragment) fragment;
            if (movieFragment.handleBackPress()) {
                return;
            }
            showNavigationBars();
            getSupportFragmentManager().popBackStack();

        } else if (fragment instanceof TvShowEpisodeFragment) {
            TvShowEpisodeFragment tvShowFragment = (TvShowEpisodeFragment) fragment;
            if (tvShowFragment.handleBackPress()) {
                return;
            }
            showNavigationBars();
            getSupportFragmentManager().popBackStack();

        } else if (fragment instanceof CatchUpDetailFragment) {
            CatchUpDetailFragment catchUpFragment = (CatchUpDetailFragment) fragment;
            if (catchUpFragment.handleBackPress()) {
                return;
            }
            showNavigationBars();
            getSupportFragmentManager().popBackStack();

        } else {
            if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            } else {
                super.onBackPressed();
            }
        }
    }

    // ===================== PiP HANDLING =====================
    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof VideoPlayerFragment) {
            ((VideoPlayerFragment) fragment).onUserLeaveHint();
        } else if (fragment instanceof MovieVideoPlayerFragment) {
            ((MovieVideoPlayerFragment) fragment).onUserLeaveHint();
        } else if (fragment instanceof TvShowEpisodeFragment) {
            ((TvShowEpisodeFragment) fragment).onUserLeaveHint();
        } else if (fragment instanceof CatchUpDetailFragment) {
            ((CatchUpDetailFragment) fragment).onUserLeaveHint();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof VideoPlayerFragment) {
            ((VideoPlayerFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        } else if (fragment instanceof MovieVideoPlayerFragment) {
            ((MovieVideoPlayerFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        } else if (fragment instanceof TvShowEpisodeFragment) {
            ((TvShowEpisodeFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        } else if (fragment instanceof CatchUpDetailFragment) {
            ((CatchUpDetailFragment) fragment).onPictureInPictureModeChanged(isInPictureInPictureMode);
        }
    }

    // ===================== NAVIGATION BAR CONTROL =====================
    public void hideNavigationBars() {
        runOnUiThread(() -> {
            // Set immersive mode for fullscreen
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );

            // Hide dynamic bottom navigation with slide-down animation
            if (bottomNavRecyclerView != null) {
                bottomNavRecyclerView.animate()
                        .translationY(bottomNavRecyclerView.getHeight())
                        .setDuration(300)
                        .setInterpolator(new AccelerateInterpolator())
                        .withEndAction(() -> bottomNavRecyclerView.setVisibility(View.GONE))
                        .start();
            }

            // Hide mini player with fade-out and slide-down animation
            androidx.cardview.widget.CardView cardMiniPlayer = findViewById(R.id.cardMiniPlayer);
            if (cardMiniPlayer != null) {
                cardMiniPlayer.animate()
                        .alpha(0f)
                        .translationY(cardMiniPlayer.getHeight())
                        .setDuration(250)
                        .setInterpolator(new AccelerateInterpolator())
                        .withEndAction(() -> cardMiniPlayer.setVisibility(View.GONE))
                        .start();
            }

            // Adjust fragment container to full screen
            FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                RelativeLayout.LayoutParams params =
                        (RelativeLayout.LayoutParams) fragmentContainer.getLayoutParams();
                params.removeRule(RelativeLayout.ABOVE);
                params.removeRule(RelativeLayout.BELOW);
                params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
                fragmentContainer.setLayoutParams(params);
            }
        });
    }

    public void showNavigationBars() {
        runOnUiThread(() -> {
            // Exit immersive mode
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

            // Show dynamic bottom navigation with slide-up animation
            if (bottomNavRecyclerView != null) {
                bottomNavRecyclerView.setVisibility(View.VISIBLE);
                bottomNavRecyclerView.setTranslationY(bottomNavRecyclerView.getHeight());
                bottomNavRecyclerView.animate()
                        .translationY(0)
                        .setDuration(400)
                        .setInterpolator(new OvershootInterpolator(0.8f))
                        .start();
            }

            // Show mini player with fade-in and slide-up animation (if audio is playing)
            androidx.cardview.widget.CardView cardMiniPlayer = findViewById(R.id.cardMiniPlayer);
            if (cardMiniPlayer != null) {
                if (PlayerManager.getCurrentAudio() != null) {
                    cardMiniPlayer.setVisibility(View.VISIBLE);
                    cardMiniPlayer.setAlpha(0f);
                    cardMiniPlayer.setTranslationY(cardMiniPlayer.getHeight());
                    cardMiniPlayer.animate()
                            .alpha(1f)
                            .translationY(0)
                            .setDuration(350)
                            .setInterpolator(new OvershootInterpolator(0.6f))
                            .setStartDelay(100)
                            .start();
                } else {
                    cardMiniPlayer.setVisibility(View.GONE);
                }
            }

            // Restore fragment container layout
            FrameLayout fragmentContainer = findViewById(R.id.fragment_container);
            if (fragmentContainer != null) {
                RelativeLayout.LayoutParams params =
                        (RelativeLayout.LayoutParams) fragmentContainer.getLayoutParams();
                params.removeRule(RelativeLayout.ALIGN_PARENT_TOP);
                params.removeRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

                // Position fragment container above the mini player
                if (cardMiniPlayer != null && cardMiniPlayer.getVisibility() == View.VISIBLE) {
                    params.addRule(RelativeLayout.ABOVE, R.id.cardMiniPlayer);
                } else {
                    params.addRule(RelativeLayout.ABOVE, R.id.bottom_nav_recycler_view);
                }

                params.height = RelativeLayout.LayoutParams.MATCH_PARENT;
                fragmentContainer.setLayoutParams(params);
            }
        });
    }

    // ===================== DRAWER TOGGLE =====================
    @Override
    public void onToggleDrawer() {
        if (drawerLayout != null) {
            if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                drawerLayout.closeDrawer(Gravity.LEFT);
            } else {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        }
    }

    // ===================== UTILITY METHODS =====================
    private int getBottomNavHeight() {
        int totalHeight = 0;

        if (bottomNavRecyclerView != null && bottomNavRecyclerView.getVisibility() == View.VISIBLE) {
            totalHeight += bottomNavRecyclerView.getHeight();
        }

        androidx.cardview.widget.CardView cardMiniPlayer = findViewById(R.id.cardMiniPlayer);
        if (cardMiniPlayer != null && cardMiniPlayer.getVisibility() == View.VISIBLE) {
            totalHeight += cardMiniPlayer.getHeight();
        }

        return totalHeight;
    }

    // ===================== LIFECYCLE METHODS =====================
    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if navigation data is available
        if (bottomNavItems.isEmpty()) {
            Log.d("MainActivity", "No navigation data, checking SharedPreferences again");
            loadNavigationFromSharedPrefs();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (closeReceiver != null) unregisterReceiver(closeReceiver);
        if (miniPlayerReceiver != null) unregisterReceiver(miniPlayerReceiver);
        seekBarHandler.removeCallbacks(updateSeekBarRunnable);
    }

    private int getTopLayoutHeight() {
        RelativeLayout topLayout = findViewById(R.id.top_layout);
        if (topLayout != null) {
            return topLayout.getHeight();
        }
        return 0;
    }

    // ===================== PUBLIC METHODS =====================
    public void selectTab(int position) {
        if (bottomNavAdapter != null && position >= 0 && position < bottomNavItems.size()) {
            bottomNavAdapter.setSelectedPosition(position);
            loadFragmentForBottomNav(bottomNavItems.get(position));
        }
    }

    public List<BottomNavItem> getBottomNavItems() {
        return bottomNavItems;
    }

}