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

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity
        implements MusicPlayerService.OnPlaybackStateChangeListener,
        MusicPlaylistDialog.OnPlaylistActionListener {

    private static final String TAG = "MusicPlayerActivity";

    private ViewPager2 viewPager;
    private TextView tvSongName, tvArtistName, tvCurrentTime, tvTotalTime;
    private ImageView btnClose, btnPlayPause, btnPrevious, btnNext, btnPlayMode, btnPlaylist, btnLike;
    private SeekBar seekBar;
    private ConstraintLayout rootLayout;
    private MusicPlayerService musicService;
    private boolean serviceBound = false;
    private Handler handler = new Handler();
    private Runnable updateProgressRunnable;
    private MusicManager musicManager;
    private MusicPlayerService.PlayMode currentPlayMode = MusicPlayerService.PlayMode.SEQUENCE;
    private AlbumArtFragment albumArtFragment;
    private LyricFragment lyricFragment;
    private MusicPlaylistDialog currentPlaylistDialog;

    // 添加点赞状态变量
    private boolean isLiked = false;
    private ValueAnimator likeAnimator;
    private int currentDominantColor = 0xFF424242; // 默认色

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_player);

        // 初始化MusicManager
        musicManager = MusicManager.getInstance();

        initViews();
        setupViewPager();
        setupListeners();

        // 从Intent中获取传递的数据
        Bundle extras = getIntent().getExtras();
        MusicInfo musicInfo = null;

        if (extras != null) {
            // 获取当前要播放的音乐信息
            musicInfo = extras.getParcelable("MUSIC_INFO");

            // 可选：获取完整音乐列表
            ArrayList<MusicInfo> allMusicList = extras.getParcelableArrayList("ALL_MUSIC_LIST");
            if (allMusicList != null && !allMusicList.isEmpty()) {
                Log.d(TAG, "接收到完整音乐列表，包含 " + allMusicList.size() + " 首歌曲");
                // 可以选择将完整列表添加到MusicManager中作为候选歌曲
                // musicManager.addCandidateSongs(allMusicList);
            }

            // 可选：获取其他元数据
            String sourceActivity = extras.getString("SOURCE_ACTIVITY");
            long clickTime = extras.getLong("CLICK_TIME");
            Log.d(TAG, "音乐来源: " + sourceActivity + ", 点击时间: " + clickTime);
        }

        if (musicInfo != null) {
            // 将当前歌曲添加到播放列表开头，并重新排列
            musicManager.addAndReorderPlaylist(musicInfo);

            // 更新界面显示
            updateSongInfo(musicInfo);

            // 绑定并启动服务
            bindMusicService();

            Log.d(TAG, "成功接收并添加音乐: " + musicInfo.getMusicName());
        } else {
            // 如果没有传入新的音乐信息，检查是否有现有的播放列表
            if (!musicManager.isPlaylistEmpty()) {
                MusicInfo currentMusic = musicManager.getCurrentMusic();
                if (currentMusic != null) {
                    updateSongInfo(currentMusic);
                    bindMusicService();
                    Log.d(TAG, "恢复播放现有音乐: " + currentMusic.getMusicName());
                } else {
                    Log.e(TAG, "播放列表不为空但获取当前音乐失败");
                    finish();
                }
            } else {
                Log.e(TAG, "未传入音乐信息且播放列表为空，无法初始化播放器");
                finish();
            }
        }
    }

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
            if (serviceBound) {
                if (musicService.isPlaying()) {
                    musicService.pause();
                } else {
                    musicService.play();
                }
            }
        });
        btnPrevious.setOnClickListener(v -> playPrevious());
        btnNext.setOnClickListener(v -> playNext());
        btnPlayMode.setOnClickListener(v -> switchPlayMode());
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound) {
                    int duration = musicService.getDuration();
                    int newPosition = (duration * progress) / 100;
                    musicService.seekTo(newPosition);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 释放滑动后，确保播放内容已更新
                if (serviceBound) {
                    int duration = musicService.getDuration();
                    int newPosition = (duration * seekBar.getProgress()) / 100;
                    musicService.seekTo(newPosition);
                    Log.d(TAG, "拖拽进度条完成，跳转到: " + newPosition + "ms");
                }
            }
        });

        // 点赞按钮事件
        btnLike.setOnClickListener(v -> toggleLike());

        // 音乐列表按钮事件
        btnPlaylist.setOnClickListener(v -> showPlaylistDialog());
    }

    // 音乐列表弹窗
    public void showPlaylistDialog() {
        currentPlaylistDialog = MusicPlaylistDialog.newInstance();
        currentPlaylistDialog.show(getSupportFragmentManager(), "MusicPlaylistDialog");
    }

    /**
     * 实现OnPlaylistActionListener接口 - 从播放列表播放指定歌曲
     */
    @Override
    public void onPlayMusicFromPlaylist(int position) {
        playMusicFromPlaylist(position);
    }

    /**
     * 实现OnPlaylistActionListener接口 - 播放列表变化处理
     */
    @Override
    public void onPlaylistChanged() {
        handlePlaylistChanged();
    }

    /**
     * 实现OnPlaylistActionListener接口 - 播放模式变化处理
     */
    @Override
    public void onPlayModeChanged(MusicPlayerService.PlayMode playMode) {
        setPlayMode(playMode);
    }

    /**
     * 实现OnPlaylistActionListener接口 - 获取当前播放模式
     */
    @Override
    public MusicPlayerService.PlayMode getCurrentPlayMode() {
        return currentPlayMode;
    }

    /**
     * 从播放列表播放指定位置的歌曲
     */
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

        // 更新MusicManager的当前位置
        musicManager.setCurrentPosition(position);

        // 更新界面
        updateSongInfo(musicInfo);

        // 通过Service播放
        if (serviceBound) {
            musicService.playAtPosition(position);
        }
    }

    /**
     * 处理播放列表变化
     */
    public void handlePlaylistChanged() {
        Log.d(TAG, "处理播放列表变化");

        // 如果播放列表为空，关闭Activity
        if (musicManager.isPlaylistEmpty()) {
            Log.d(TAG, "播放列表为空，关闭Activity");
            finish();
            return;
        }

        // 通知Service播放列表已更新
        if (serviceBound) {
            musicService.updatePlaylist(musicManager.getPlaylist());
        }

        // 获取当前音乐并更新界面
        MusicInfo currentMusic = musicManager.getCurrentMusic();
        if (currentMusic != null) {
            updateSongInfo(currentMusic);
        }

        // 如果播放列表对话框还在显示，通知其更新
        if (currentPlaylistDialog != null) {
            currentPlaylistDialog.notifyPlaylistChanged();
        }
    }

    /**
     * 设置播放模式
     */
    public void setPlayMode(MusicPlayerService.PlayMode playMode) {
        if (playMode != currentPlayMode) {
            currentPlayMode = playMode;
            updatePlayModeButton();

            // 同步到Service
            if (serviceBound) {
                musicService.setPlayMode(playMode);
            }

            // 同步到对话框
            if (currentPlaylistDialog != null) {
                currentPlaylistDialog.updatePlayMode(playMode);
            }

            Log.d(TAG, "播放模式已设置为: " + currentPlayMode);
        }
    }

    /**
     * 更新播放模式按钮显示
     */
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

    /**
     * 执行退出动画并关闭Activity
     */
    private void finishWithAnimation() {
        // 防止重复触发
        if (isFinishing()) {
            return;
        }

        // 获取屏幕高度
        int screenHeight = getResources().getDisplayMetrics().heightPixels;

        // 创建动画集合
        AnimatorSet exitAnimatorSet = new AnimatorSet();

        // 1. 向下滑动动画
        ObjectAnimator slideDown = ObjectAnimator.ofFloat(
                rootLayout,
                "translationY",
                0f,
                screenHeight
        );
        slideDown.setDuration(400); // 400ms动画时长
        slideDown.setInterpolator(new AccelerateInterpolator(1.5f)); // 加速效果

        // 2. 渐隐动画
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(
                rootLayout,
                "alpha",
                1.0f,
                0.0f
        );
        fadeOut.setDuration(300); // 300ms渐隐
        fadeOut.setStartDelay(100); // 延迟100ms开始，让滑动先进行
        fadeOut.setInterpolator(new AccelerateInterpolator());

        // 3. 可选：添加缩放效果让动画更生动
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(
                rootLayout,
                "scaleX",
                1.0f,
                0.9f
        );
        scaleX.setDuration(400);
        scaleX.setInterpolator(new AccelerateInterpolator());

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(
                rootLayout,
                "scaleY",
                1.0f,
                0.9f
        );
        scaleY.setDuration(400);
        scaleY.setInterpolator(new AccelerateInterpolator());

        // 组合所有动画
        exitAnimatorSet.playTogether(slideDown, fadeOut, scaleX, scaleY);

        // 动画结束后关闭Activity
        exitAnimatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                // 确保在动画结束后调用finish()
                finish();
                // 禁用系统默认的Activity切换动画，因为我们已经有自定义动画
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                // 如果动画被取消，也要确保Activity能正常关闭
                finish();
                overridePendingTransition(0, 0);
            }
        });

        // 开始动画
        exitAnimatorSet.start();

        Log.d(TAG, "开始执行退出动画");
    }

    /**
     * 切换点赞状态
     */
    private void toggleLike() {
        isLiked = !isLiked;
        updateLikeButton();
        playLikeAnimation();

        Log.d(TAG, "点赞状态: " + (isLiked ? "已收藏" : "已取消收藏"));
    }

    /**
     * 更新点赞按钮的图标和颜色
     */
    private void updateLikeButton() {
        if (isLiked) {
            btnLike.setImageResource(R.drawable.ic_favorite);
            // 已点赞使用红色
            btnLike.setColorFilter(getResources().getColor(R.color.like_color_active, null));
        } else {
            btnLike.setImageResource(R.drawable.ic_favorite_border);
            // 未点赞使用当前主题色
            btnLike.setColorFilter(getCurrentThemeColor());
        }
    }

    /**
     * 播放点赞动画
     */
    private void playLikeAnimation() {
        // 取消之前的动画
        if (likeAnimator != null && likeAnimator.isRunning()) {
            likeAnimator.cancel();
        }

        if (isLiked) {
            // 点赞动画：缩放 + 旋转
            playLikeScaleAnimation();
        } else {
            // 取消点赞动画：简单缩放
            playUnlikeAnimation();
        }
    }

    /**
     * 点赞缩放动画
     */
    private void playLikeScaleAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();

        // 第一阶段：快速放大
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(btnLike, "scaleX", 1.0f, 1.3f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(btnLike, "scaleY", 1.0f, 1.3f);
        scaleUpX.setDuration(150);
        scaleUpY.setDuration(150);
        scaleUpX.setInterpolator(new AccelerateInterpolator());
        scaleUpY.setInterpolator(new AccelerateInterpolator());

        // 第二阶段：回弹缩小
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btnLike, "scaleX", 1.3f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btnLike, "scaleY", 1.3f, 1.0f);
        scaleDownX.setDuration(200);
        scaleDownY.setDuration(200);
        scaleDownX.setInterpolator(new OvershootInterpolator());
        scaleDownY.setInterpolator(new OvershootInterpolator());

        // 旋转动画
        ObjectAnimator rotation = ObjectAnimator.ofFloat(btnLike, "rotation", 0f, 360f);
        rotation.setDuration(350);
        rotation.setInterpolator(new DecelerateInterpolator());

        // 组合动画
        animatorSet.play(scaleUpX).with(scaleUpY);
        animatorSet.play(scaleDownX).with(scaleDownY).after(scaleUpX);
        animatorSet.play(rotation).with(scaleUpX);

        animatorSet.start();
    }

    /**
     * 取消点赞动画
     */
    private void playUnlikeAnimation() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btnLike, "scaleX", 1.0f, 0.8f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btnLike, "scaleY", 1.0f, 0.8f);
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(btnLike, "scaleX", 0.8f, 1.0f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(btnLike, "scaleY", 0.8f, 1.0f);

        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        scaleUpX.setDuration(150);
        scaleUpY.setDuration(150);

        animatorSet.play(scaleDownX).with(scaleDownY);
        animatorSet.play(scaleUpX).with(scaleUpY).after(scaleDownX);
        animatorSet.start();
    }

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        startService(intent);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;
            musicService.setOnPlaybackStateChangeListener(MusicPlayerActivity.this);

            // 同步播放模式到Service
            musicService.setPlayMode(currentPlayMode);

            Log.d(TAG, "Service已连接，当前位置: " + musicManager.getCurrentPosition());
            // 立即播放当前位置的音乐
            musicService.playAtPosition(musicManager.getCurrentPosition());
            startProgressUpdate();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.d(TAG, "Service连接断开");
        }
    };

    private void updateSongInfo(MusicInfo musicInfo) {
        if (musicInfo == null) {
            Log.e(TAG, "updateSongInfo: musicInfo为null");
            return;
        }
        Log.d(TAG, "更新歌曲信息: " + musicInfo.getMusicName());
        tvSongName.setText(musicInfo.getMusicName());
        tvArtistName.setText(musicInfo.getAuthor());

        // 先加载歌词，后加载专辑封面
        // 这样可以确保歌词内容已经准备好，再进行颜色调整
        handler.post(() -> {
            if (lyricFragment != null) {
                lyricFragment.updateLyric(musicInfo.getLyricUrl());
            }
            if (albumArtFragment != null) {
                albumArtFragment.updateAlbumArt(musicInfo.getCoverUrl());
            }
        });

        // 加载专辑封面并提取颜色
        loadAlbumArt(musicInfo.getCoverUrl());
    }

    private void loadAlbumArt(String coverUrl) {
        Glide.with(this)
                .asBitmap()
                .load(coverUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // 使用Palette提取颜色并应用到UI
                        extractColorsAndApply(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    /**
     * 从专辑封面提取颜色并应用到UI各组件
     */
    private void extractColorsAndApply(Bitmap albumArt) {
        Palette.from(albumArt).generate(palette -> {
            if (palette == null) {
                Log.w(TAG, "无法从专辑封面提取颜色");
                return;
            }

            // 提取主色调
            int dominantColor = palette.getDominantColor(0xFF424242);
            currentDominantColor = dominantColor;

            // 获取暗色调和亮色调
            Palette.Swatch darkVibrantSwatch = palette.getDarkVibrantSwatch();
            Palette.Swatch lightVibrantSwatch = palette.getLightVibrantSwatch();

            // 分析颜色的亮度
            float[] hsl = new float[3];
            ColorUtils.colorToHSL(dominantColor, hsl);
            boolean isDarkBackground = hsl[2] < 0.5f; // 亮度小于0.5认为是暗色

            // 设置背景色
            rootLayout.setBackgroundColor(dominantColor);

            // 设置专辑名和艺术家名称的颜色 - 使用高对比度
            int titleTextColor = getContrastColor(dominantColor, 1.0f);
            tvSongName.setTextColor(titleTextColor);
            tvArtistName.setTextColor(titleTextColor);

            // 设置时间显示TextView的颜色 - 使用与歌词相同的半透明对比色
            int timeTextColor = getContrastColor(dominantColor, 0.8f);
            tvCurrentTime.setTextColor(timeTextColor);
            tvTotalTime.setTextColor(timeTextColor);

            // 通知歌词组件更新颜色
            handler.post(() -> {
                if (lyricFragment != null) {
                    lyricFragment.setBackgroundColor(dominantColor);

                    // 可选：记录当前背景是深色还是浅色，以便歌词组件进一步优化
                    if (lyricFragment instanceof ColorAwareComponent) {
                        ((ColorAwareComponent) lyricFragment).setIsDarkBackground(isDarkBackground);
                    }
                }
            });

            Log.d(TAG, "颜色提取完成 - 背景色: " + Integer.toHexString(dominantColor) +
                    ", 是否深色背景: " + isDarkBackground);
        });
    }

    /**
     * 获取当前主题颜色（与歌词颜色保持一致）
     */
    private int getCurrentThemeColor() {
        // 如果有当前提取的主题色，使用对比色
        // 这里可以复用之前 extractColorsAndApply 中的逻辑
        return getContrastColor(getCurrentDominantColor(), 0.8f);
    }

    private int getCurrentDominantColor() {
        return currentDominantColor;
    }

    /**
     * 计算与背景色对比的文字颜色，与歌词Fragment使用相同的算法
     * @param backgroundColor 背景颜色
     * @param alpha 透明度 (0.0-1.0)
     * @return 计算后的文字颜色
     */
    private int getContrastColor(int backgroundColor, float alpha) {
        // 分析背景色亮度
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(backgroundColor, hsl);
        boolean isDarkBg = hsl[2] < 0.5f;

        // 基于背景色决定文字颜色
        int textColor;

        if (isDarkBg) {
            // 深色背景使用高亮度文字
            textColor = Color.rgb(
                    Math.min(255, 255 - Color.red(backgroundColor) + 40),
                    Math.min(255, 255 - Color.green(backgroundColor) + 40),
                    Math.min(255, 255 - Color.blue(backgroundColor) + 40)
            );
        } else {
            // 浅色背景使用低亮度文字
            textColor = Color.rgb(
                    Math.max(0, 255 - Color.red(backgroundColor) - 40),
                    Math.max(0, 255 - Color.green(backgroundColor) - 40),
                    Math.max(0, 255 - Color.blue(backgroundColor) - 40)
            );
        }

        // 确保WCAG AA级别对比度 (4.5:1)
        double contrast = ColorUtils.calculateContrast(textColor, backgroundColor);
        if (contrast < 4.5) {
            if (isDarkBg) {
                // 深色背景，提高文字亮度
                textColor = Color.rgb(
                        Math.min(255, Color.red(textColor) + 30),
                        Math.min(255, Color.green(textColor) + 30),
                        Math.min(255, Color.blue(textColor) + 30)
                );
            } else {
                // 浅色背景，降低文字亮度
                textColor = Color.rgb(
                        Math.max(0, Color.red(textColor) - 30),
                        Math.max(0, Color.green(textColor) - 30),
                        Math.max(0, Color.blue(textColor) - 30)
                );
            }
        }

        // 应用透明度
        return ColorUtils.setAlphaComponent(textColor, (int)(alpha * 255));
    }

    private void playPrevious() {
        if (serviceBound) {
            musicService.playPrevious();
        }
    }

    private void playNext() {
        if (serviceBound) {
            musicService.playNext();
        }
    }

    private void switchPlayMode() {
        // 循环切换播放模式
        MusicPlayerService.PlayMode[] modes = MusicPlayerService.PlayMode.values();
        int currentIndex = -1;

        // 找到当前模式的索引
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == currentPlayMode) {
                currentIndex = i;
                break;
            }
        }

        // 切换到下一个模式
        int nextIndex = (currentIndex + 1) % modes.length;
        setPlayMode(modes[nextIndex]);
    }

    private void startProgressUpdate() {
        if (updateProgressRunnable != null) {
            handler.removeCallbacks(updateProgressRunnable);
        }
        updateProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (serviceBound && musicService.isPlaying()) {
                    int currentPosition = musicService.getCurrentPosition();
                    int duration = musicService.getDuration();
                    if (duration > 0) {
                        int progress = (currentPosition * 100) / duration;
                        seekBar.setProgress(progress);
                        tvCurrentTime.setText(formatTime(currentPosition));
                        tvTotalTime.setText(formatTime(duration));
                        if (lyricFragment != null) {
                            lyricFragment.updateProgress(currentPosition);
                        }
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateProgressRunnable);
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        Log.d(TAG, "播放状态变化: " + isPlaying);
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        if (albumArtFragment != null) {
            albumArtFragment.setRotationAnimation(isPlaying);
        }
    }

    @Override
    public void onSongChanged(int position) {
        Log.d(TAG, "歌曲变化回调: " + position);
        musicManager.setCurrentPosition(position);
        List<MusicInfo> playlist = musicManager.getPlaylist();
        if (playlist != null && position < playlist.size()) {
            updateSongInfo(playlist.get(position));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Activity销毁");
        if (serviceBound) {
            unbindService(serviceConnection);
        }
        if (updateProgressRunnable != null) {
            handler.removeCallbacks(updateProgressRunnable);
        }
    }

    private class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return position == 0 ? albumArtFragment : lyricFragment;
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
