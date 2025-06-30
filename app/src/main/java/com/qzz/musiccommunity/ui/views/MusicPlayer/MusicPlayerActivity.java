package com.qzz.musiccommunity.ui.views.MusicPlayer;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.palette.graphics.Palette;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.Service.MusicPlayerService;
import com.qzz.musiccommunity.instance.MusicManager;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.ui.common.musicList.MusicPlaylistDialog;
import com.qzz.musiccommunity.ui.views.MusicPlayer.iface.ColorAwareComponent;
import com.qzz.musiccommunity.ui.views.MusicPlayer.fragment.AlbumArtFragment;
import com.qzz.musiccommunity.ui.views.MusicPlayer.fragment.LyricFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicPlayerActivity extends AppCompatActivity
        implements MusicPlayerService.OnPlaybackStateChangeListener,
        MusicPlaylistDialog.OnPlaylistActionListener {

    private static final String TAG = "MusicPlayerActivity";
    private static final int RECONNECT_DELAY_MS = 200;
    private static final int UI_REFRESH_DELAY_MS = 100;

    // UI组件
    private ViewPager2 viewPager;
    private TextView tvSongName, tvArtistName, tvCurrentTime, tvTotalTime;
    private ImageView btnClose, btnPlayPause, btnPrevious, btnNext, btnPlayMode, btnPlaylist, btnLike;
    private SeekBar seekBar;
    private ConstraintLayout rootLayout;

    // Service相关
    private MusicPlayerService musicService;
    private volatile boolean serviceBound = false;
    private volatile boolean isReconnecting = false;

    // 线程和Handler
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Runnable updateProgressRunnable;

    // 其他组件
    private MusicManager musicManager;
    private MusicPlayerService.PlayMode currentPlayMode = MusicPlayerService.PlayMode.SEQUENCE;
    private AlbumArtFragment albumArtFragment;
    private LyricFragment lyricFragment;
    private MusicPlaylistDialog currentPlaylistDialog;

    // 状态变量
    private boolean isLiked = false;
    private ValueAnimator likeAnimator;
    private int currentDominantColor = 0xFF424242;
    private boolean isActivityVisible = false;
    private boolean needToRefreshOnResume = false;
    private boolean isRotationPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        musicManager = MusicManager.getInstance(this);
        initViews();
        setupViewPager();
        setupListeners();

        MusicInfo currentMusic = musicManager.getCurrentMusic();
        if (currentMusic != null) {
            updateSongInfo(currentMusic);
            bindMusicService();
            Log.d(TAG, "从MusicManager恢复播放现有音乐: " + currentMusic.getMusicName());
        } else {
            Log.e(TAG, "MusicManager中没有当前播放音乐信息，无法初始化播放器");
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: 活动恢复到前台");
        isActivityVisible = true;

        // 使用延迟执行确保UI完全准备好
        mainHandler.postDelayed(this::refreshActivityState, UI_REFRESH_DELAY_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: 活动进入后台");
        isActivityVisible = false;

        if (albumArtFragment != null) {
            isRotationPaused = !albumArtFragment.isRotating();
        }

        stopUpdatingProgress();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: 活动不可见");
        needToRefreshOnResume = true;
    }

    /**
     * 刷新Activity状态 - 核心优化方法
     */
    private void refreshActivityState() {
        if (isServiceConnectionValid()) {
            Log.d(TAG, "服务连接有效，刷新状态");
            refreshUIState();
        } else {
            Log.d(TAG, "服务连接无效，重新建立连接");
            reconnectService();
        }
    }

    /**
     * 验证服务连接是否真正有效
     * 不仅检查引用，还要验证IPC连接状态
     */
    private boolean isServiceConnectionValid() {
        if (!serviceBound || musicService == null) {
            return false;
        }

        try {
            // 通过调用一个轻量级方法来验证IPC连接是否活跃
            // 如果服务进程已死或连接断开，这里会抛出RemoteException
            boolean isPlaying = musicService.isPlaying(); // 用现有的方法验证连接
            return true;
        } catch (RuntimeException e) {
            Log.w(TAG, "服务连接已断开: " + e.getMessage());
            // 清理无效状态
            serviceBound = false;
            musicService = null;
            return false;
        }
    }

    /**
     * 重新连接服务并刷新状态
     */
    private void reconnectService() {
        if (isReconnecting) {
            Log.d(TAG, "正在重连中，跳过此次重连请求");
            return;
        }

        isReconnecting = true;

        // 先清理旧连接
        if (serviceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                Log.w(TAG, "解绑服务时出错: " + e.getMessage());
            }
            serviceBound = false;
            musicService = null;
        }

        // 延迟重新绑定服务，避免频繁重连
        mainHandler.postDelayed(() -> {
            Log.d(TAG, "开始重新绑定服务");
            bindMusicService();
            needToRefreshOnResume = true;
            isReconnecting = false;
        }, RECONNECT_DELAY_MS);
    }

    /**
     * 刷新UI状态 - 添加异常处理和异步执行
     */
    private void refreshUIState() {
        try {
            // 异步获取当前状态，避免阻塞主线程
            executorService.execute(() -> {
                try {
                    if (!serviceBound || musicService == null) {
                        mainHandler.post(this::handleServiceError);
                        return;
                    }

                    // 获取服务状态
                    boolean isPlaying = musicService.isPlaying();
                    int playMode = musicService.getPlayMode().ordinal();
                    MusicInfo currentMusic = musicManager.getCurrentMusic();

                    // 回到主线程更新UI
                    mainHandler.post(() -> updateUIOnMainThread(isPlaying, playMode, currentMusic));

                } catch (RuntimeException e) {
                    Log.e(TAG, "获取服务状态失败: " + e.getMessage());
                    mainHandler.post(this::handleServiceError);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "刷新UI状态失败: " + e.getMessage());
            handleServiceError();
        }
    }

    /**
     * 在主线程更新UI
     */
    private void updateUIOnMainThread(boolean isPlaying, int playMode, MusicInfo currentMusic) {
        try {
            // 1. 刷新播放状态
            updatePlaybackState(isPlaying);

            // 2. 刷新当前歌曲信息
            if (currentMusic != null) {
                updateSongInfo(currentMusic);
            }

            // 3. 恢复唱片旋转状态
            if (albumArtFragment != null) {
                albumArtFragment.setRotationAnimation(isPlaying);
            }

            // 4. 同步播放模式
            setPlayMode(MusicPlayerService.PlayMode.values()[playMode]);

            // 5. 刷新进度条
            updateProgress();

            // 6. 启动进度更新
            if (isPlaying) {
                startUpdatingProgress();
            }

            Log.d(TAG, "UI状态刷新完成");

        } catch (Exception e) {
            Log.e(TAG, "更新UI时出错: " + e.getMessage());
        }
    }

    /**
     * 处理服务错误
     */
    private void handleServiceError() {
        Log.e(TAG, "服务连接出现错误，尝试重新连接");
        serviceBound = false;
        musicService = null;


        // 延迟重连
        mainHandler.postDelayed(this::reconnectService, RECONNECT_DELAY_MS);
    }

    /**
     * 安全的Service方法调用包装器
     */
    private boolean safeServiceCall(ServiceOperation operation) {
        if (!serviceBound || musicService == null) {
            handleServiceError();
            return false;
        }

        try {
            operation.execute(musicService);
            return true;
        } catch (RemoteException | RuntimeException e) {
            Log.e(TAG, "Service调用失败: " + e.getMessage());
            handleServiceError();
            return false;
        }
    }

    // 定义Service操作接口
    private interface ServiceOperation {
        void execute(MusicPlayerService service) throws RemoteException;
    }

    // 改进的ServiceConnection，增强错误处理
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
                musicService = binder.getService();
                serviceBound = true;

                // 设置回调监听器
                musicService.setOnPlaybackStateChangeListener(MusicPlayerActivity.this);
                Log.d(TAG, "服务已绑定");

                // 确保服务中的播放列表与MusicManager同步
                musicService.updatePlaylist(musicManager.getPlaylist());
                musicService.setCurrentPosition(musicManager.getCurrentPosition());

                // 同步播放模式
                currentPlayMode = musicService.getPlayMode();
                updatePlayModeButton();

                // 如果服务没有在播放，则开始播放当前音乐
                if (!musicService.isPlaying() && musicManager.getCurrentMusic() != null) {
                    musicService.play();
                }

                // 更新UI状态
                updatePlaybackState(musicService.isPlaying());
                updateProgress();

                // 重置重连标志
                isReconnecting = false;

            } catch (Exception e) {
                Log.e(TAG, "Service连接回调处理失败: " + e.getMessage());
                handleServiceError();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "服务连接意外断开");
            serviceBound = false;
            musicService = null;
            needToRefreshOnResume = true;

            // 如果Activity可见，尝试重连
            if (isActivityVisible) {
                mainHandler.postDelayed(() -> reconnectService(), RECONNECT_DELAY_MS);
            }
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Log.e(TAG, "服务绑定死亡");
            serviceBound = false;
            musicService = null;
            if (isActivityVisible) {
                mainHandler.postDelayed(() -> reconnectService(), RECONNECT_DELAY_MS);
            }
        }
    };

    // 改进后的播放控制方法
    private void playPrevious() {
        safeServiceCall(service -> service.playPrevious());
    }

    private void playNext() {
        safeServiceCall(service -> service.playNext());
    }

    private void updateProgress() {
        safeServiceCall(service -> {
            int current = service.getCurrentPosition();
            int duration = service.getDuration();
            onProgressChanged(current, duration);
        });
    }

    // 改进的进度更新机制
    private void startUpdatingProgress() {
        if (updateProgressRunnable == null) {
            updateProgressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isActivityVisible && serviceBound && musicService != null) {
                        safeServiceCall(service -> {
                            if (service.isPlaying()) {
                                int current = service.getCurrentPosition();
                                int duration = service.getDuration();
                                onProgressChanged(current, duration);
                            }
                        });
                    }
                    mainHandler.postDelayed(this, 1000);
                }
            };
        }

        mainHandler.removeCallbacks(updateProgressRunnable);
        mainHandler.post(updateProgressRunnable);
    }

    private void stopUpdatingProgress() {
        if (updateProgressRunnable != null) {
            mainHandler.removeCallbacks(updateProgressRunnable);
        }
    }

    // 其余方法保持不变...
    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tvSongName = findViewById(R.id.tvSongName);
        tvArtistName = findViewById(R.id.tvArtistName);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        btnClose = findViewById(R.id.btnClose);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnPlayMode = findViewById(R.id.btnPlayMode);
        btnPlaylist = findViewById(R.id.btnPlaylist);
        btnLike = findViewById(R.id.btnLike);
        seekBar = findViewById(R.id.seekBar);
        rootLayout = findViewById(R.id.rootLayout);
    }

    private void setupViewPager() {
        albumArtFragment = new AlbumArtFragment();
        lyricFragment = new LyricFragment();
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 1 && lyricFragment != null) {
                    MusicInfo currentMusic = musicManager.getCurrentMusic();
                    if (currentMusic != null) {
                        lyricFragment.updateLyric(currentMusic.getLyricUrl());
                    }
                }
            }
        });
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finishWithAnimation());

        btnPlayPause.setOnClickListener(v -> {
            safeServiceCall(service -> {
                if (service.isPlaying()) {
                    service.pause();
                } else {
                    service.play();
                }
            });
        });

        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnPlayMode.setOnClickListener(v -> switchPlayMode());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    safeServiceCall(service -> {
                        int duration = service.getDuration();
                        int newPosition = (duration * progress) / 100;
                        service.seekTo(newPosition);
                    });
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                safeServiceCall(service -> {
                    int duration = service.getDuration();
                    int newPosition = (duration * seekBar.getProgress()) / 100;
                    service.seekTo(newPosition);
                    Log.d(TAG, "拖拽进度条完成，跳转到: " + newPosition + "ms");
                });
            }
        });

        btnLike.setOnClickListener(v -> toggleLike());
        btnPlaylist.setOnClickListener(v -> showPlaylistDialog());
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "尝试绑定音乐服务");
    }

    private void unbindMusicService() {
        if (serviceBound) {
            try {
                unbindService(serviceConnection);
            } catch (Exception e) {
                Log.w(TAG, "解绑服务时出错: " + e.getMessage());
            }
            serviceBound = false;
            Log.d(TAG, "服务已解绑");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 停止所有异步操作
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // 清理Handler回调
        mainHandler.removeCallbacks(updateProgressRunnable);

        // 解绑服务
        unbindMusicService();

        Log.d(TAG, "Activity销毁");
    }

    // 保持其他现有方法不变，只更新涉及Service调用的部分...
    // [其余方法如updateSongInfo, extractColorFromBitmap等保持原样]

    // 示例：切换播放模式的安全调用
    private void switchPlayMode() {
        MusicPlayerService.PlayMode[] modes = MusicPlayerService.PlayMode.values();
        int currentIndex = -1;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == currentPlayMode) {
                currentIndex = i;
                break;
            }
        }
        int nextIndex = (currentIndex + 1) % modes.length;
        setPlayMode(modes[nextIndex]);
    }

    public void setPlayMode(MusicPlayerService.PlayMode playMode) {
        if (playMode != currentPlayMode) {
            currentPlayMode = playMode;
            updatePlayModeButton();

            safeServiceCall(service -> service.setPlayMode(playMode));

            if (currentPlaylistDialog != null) {
                currentPlaylistDialog.updatePlayMode(playMode);
            }

            Log.d(TAG, "播放模式已设置为: " + currentPlayMode);
        }
    }

    private void updatePlayModeButton() {
        switch (currentPlayMode) {
            case SEQUENCE:
                btnPlayMode.setImageResource(R.drawable.ic_repeat);
                break;
            case RANDOM:
                btnPlayMode.setImageResource(R.drawable.ic_shuffle);
                break;
            case REPEAT_ONE:
                btnPlayMode.setImageResource(R.drawable.ic_repeat_one);
                break;
        }
    }


    private void updateSongInfo(MusicInfo musicInfo) {
        if (musicInfo == null) {
            Log.e(TAG, "updateSongInfo: musicInfo is null");
            return;
        }

        try {
            tvSongName.setText(musicInfo.getMusicName());
            tvArtistName.setText(musicInfo.getAuthor());

            // 从重试次数0开始加载图片
            loadCoverImage(musicInfo.getCoverUrl(), 0);

            if (lyricFragment != null) {
                lyricFragment.updateLyric(musicInfo.getLyricUrl());
            }

            isLiked = musicInfo.isLiked();
            updateLikeButton();

        } catch (Exception e) {
            Log.e(TAG, "更新歌曲信息时出错: " + e.getMessage());
        }
    }

    private void loadCoverImage(String coverUrl, int retryCount) {
        Glide.with(this)
                .asBitmap()
                .load(coverUrl)
                .error(R.drawable.default_album_art)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (albumArtFragment != null) {
                            albumArtFragment.setAlbumArt(resource);

                            safeServiceCall(service -> {
                                albumArtFragment.setRotationAnimation(service.isPlaying());
                            });
                        }
                        extractColorFromBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        if (albumArtFragment != null) {
                            albumArtFragment.setAlbumArt(null);
                        }
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        if (retryCount < 3) {
                            // 重试，延迟1秒后重新加载
                            Log.w(TAG, "封面加载失败，正在重试: " + (retryCount + 1) + "/3");
                            new Handler().postDelayed(() -> {
                                loadCoverImage(coverUrl, retryCount + 1);
                            }, 1000);
                        } else {
                            Log.e(TAG, "封面加载失败，已重试3次");
                            // 可选：设置默认图片
                            if (albumArtFragment != null) {
                                albumArtFragment.setAlbumArt(null);
                            }
                        }
                    }
                });
    }


    private void extractColorFromBitmap(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int defaultColor = Color.parseColor("#424242");
                currentDominantColor = palette.getDominantColor(defaultColor);
                updateUIColors(currentDominantColor);
            }
        });
    }

    private void updateUIColors(int color) {
        rootLayout.setBackgroundColor(color);
        int textColor = getContrastColor(color);

        tvSongName.setTextColor(textColor);
        tvArtistName.setTextColor(textColor);
        tvCurrentTime.setTextColor(textColor);
        tvTotalTime.setTextColor(textColor);

        btnClose.setColorFilter(textColor);
        btnPrevious.setColorFilter(textColor);
        btnNext.setColorFilter(textColor);
        btnPlayMode.setColorFilter(textColor);
        btnPlaylist.setColorFilter(textColor);

        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(textColor));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(textColor));

        if (albumArtFragment instanceof ColorAwareComponent) {
            ((ColorAwareComponent) albumArtFragment).updateColors(color, textColor);
        }
        if (lyricFragment instanceof ColorAwareComponent) {
            ((ColorAwareComponent) lyricFragment).updateColors(color, textColor);
        }
    }

    private int getContrastColor(int color) {
        return ColorUtils.calculateLuminance(color) > 0.5 ? Color.BLACK : Color.WHITE;
    }

    private void updatePlaybackState(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            startUpdatingProgress();
            if (albumArtFragment != null) {
                if (isActivityVisible) {
                    albumArtFragment.setRotationAnimation(true);
                } else if (!isRotationPaused) {
                    isRotationPaused = false;
                }
            }
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
            stopUpdatingProgress();
            if (albumArtFragment != null) {
                albumArtFragment.setRotationAnimation(false);
                isRotationPaused = true;
            }
        }
    }

    public void onProgressChanged(int currentPosition, int totalDuration) {
        tvCurrentTime.setText(formatTime(currentPosition));
        tvTotalTime.setText(formatTime(totalDuration));
        if (totalDuration > 0) {
            seekBar.setProgress((int) (((float) currentPosition / totalDuration) * 100));
        }

        if (lyricFragment != null) {
            lyricFragment.updateProgress(currentPosition);
        }
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void finishWithAnimation() {
        if (isFinishing()) {
            return;
        }

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        AnimatorSet exitAnimatorSet = new AnimatorSet();

        ObjectAnimator slideDown = ObjectAnimator.ofFloat(rootLayout, "translationY", 0f, screenHeight);
        slideDown.setDuration(400);
        slideDown.setInterpolator(new AccelerateInterpolator(1.5f));

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(rootLayout, "alpha", 1.0f, 0.0f);
        fadeOut.setDuration(300);
        fadeOut.setStartDelay(100);
        fadeOut.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(rootLayout, "scaleX", 1.0f, 0.9f);
        scaleX.setDuration(400);
        scaleX.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(rootLayout, "scaleY", 1.0f, 0.9f);
        scaleY.setDuration(400);
        scaleY.setInterpolator(new AccelerateInterpolator());

        exitAnimatorSet.playTogether(slideDown, fadeOut, scaleX, scaleY);

        exitAnimatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                finish();
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                finish();
                overridePendingTransition(0, 0);
            }
        });

        exitAnimatorSet.start();
        Log.d(TAG, "开始执行退出动画");
    }

    private void toggleLike() {
        isLiked = !isLiked;
        musicManager.setMusicLikedStatus(isLiked);
        updateLikeButton();
        playLikeAnimation();
        Log.d(TAG, "点赞状态: " + (isLiked ? "已收藏" : "已取消收藏"));
    }

    private void updateLikeButton() {
        if (isLiked) {
            btnLike.setImageResource(R.drawable.ic_favorite);
            btnLike.setColorFilter(getResources().getColor(R.color.like_color_active, null));
        } else {
            btnLike.setImageResource(R.drawable.ic_favorite_border);
            btnLike.setColorFilter(getCurrentThemeColor());
        }
    }

    private void playLikeAnimation() {
        if (likeAnimator != null && likeAnimator.isRunning()) {
            likeAnimator.cancel();
        }

        if (isLiked) {
            playLikeScaleAnimation();
        } else {
            playUnlikeAnimation();
        }
    }

    private void playLikeScaleAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(btnLike, "scaleX", 1.0f, 1.3f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(btnLike, "scaleY", 1.0f, 1.3f);
        scaleUpX.setDuration(150);
        scaleUpY.setDuration(150);
        scaleUpX.setInterpolator(new AccelerateInterpolator());
        scaleUpY.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btnLike, "scaleX", 1.3f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btnLike, "scaleY", 1.3f, 1.0f);
        scaleDownX.setDuration(150);
        scaleDownY.setDuration(150);
        scaleDownX.setInterpolator(new OvershootInterpolator());
        scaleDownY.setInterpolator(new OvershootInterpolator());

        ObjectAnimator rotation = ObjectAnimator.ofFloat(btnLike, "rotation", 0f, 360f);
        rotation.setDuration(300);
        rotation.setInterpolator(new DecelerateInterpolator());

        animatorSet.playSequentially(scaleUpX, scaleDownX);
        animatorSet.play(scaleUpY).with(scaleUpX);
        animatorSet.play(scaleDownY).with(scaleDownX);
        animatorSet.play(rotation).after(scaleUpX);

        animatorSet.start();
    }

    private void playUnlikeAnimation() {
        likeAnimator = ValueAnimator.ofFloat(1f, 0.8f, 1f);
        likeAnimator.setDuration(300);
        likeAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            btnLike.setScaleX(scale);
            btnLike.setScaleY(scale);
        });
        likeAnimator.start();
    }

    private int getCurrentThemeColor() {
        return currentDominantColor;
    }

    // 音乐列表弹窗
    public void showPlaylistDialog() {
        currentPlaylistDialog = MusicPlaylistDialog.newInstance();
        currentPlaylistDialog.show(getSupportFragmentManager(), "MusicPlaylistDialog");
    }

    // 实现接口方法
    @Override
    public void onPlayMusicFromPlaylist(int position) {
        playMusicFromPlaylist(position);
    }

    @Override
    public void onPlaylistChanged() {
        handlePlaylistChanged();
    }

    @Override
    public void onPlayModeChanged(MusicPlayerService.PlayMode playMode) {
        setPlayMode(playMode);
    }

    @Override
    public void onSongChanged(int position) {
        MusicInfo musicInfo = musicManager.getMusicAt(position);
        if (musicInfo != null) {
            updateSongInfo(musicInfo);
            musicManager.setCurrentPosition(position);
        } else {
            Log.e(TAG, "onSongChanged: 位置 " + position + " 的音乐信息为null");
        }
    }

    @Override
    public MusicPlayerService.PlayMode getCurrentPlayMode() {
        return currentPlayMode;
    }

    public void playMusicFromPlaylist(int position) {
        if (!musicManager.isValidPosition(position)) {
            Log.e(TAG, "playMusicFromPlaylist: 无效位置 " + position);
            return;
        }

        MusicInfo musicInfo = musicManager.getMusicAt(position);
        if (musicInfo == null) {
            Log.e(TAG, "playMusicFromPlaylist: 位置 " + position + " 的音乐信息为null");
            return;
        }

        Log.d(TAG, "从播放列表播放歌曲: " + musicInfo.getMusicName() + ", 位置: " + position);

        musicManager.setCurrentPosition(position);
        updateSongInfo(musicInfo);

        safeServiceCall(service -> service.playAtPosition(position));
    }

    public void handlePlaylistChanged() {
        Log.d(TAG, "处理播放列表变化");

        if (musicManager.isPlaylistEmpty()) {
            Log.d(TAG, "播放列表为空，关闭Activity");
            finish();
            return;
        }

        safeServiceCall(service -> service.updatePlaylist(musicManager.getPlaylist()));

        MusicInfo currentMusic = musicManager.getCurrentMusic();
        if (currentMusic != null) {
            updateSongInfo(currentMusic);
        }

        if (currentPlaylistDialog != null) {
            currentPlaylistDialog.notifyPlaylistChanged();
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updatePlaybackState(isPlaying);
    }

    @Override
    public void onProgressUpdate(int currentPosition, int totalDuration) {
        onProgressChanged(currentPosition, totalDuration);
    }

    private class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return albumArtFragment;
            } else {
                return lyricFragment;
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
