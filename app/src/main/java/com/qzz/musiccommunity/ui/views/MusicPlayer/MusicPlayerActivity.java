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
        musicManager = MusicManager.getInstance(this);

        initViews();
        setupViewPager();
        setupListeners();

        // 从MusicManager获取当前播放状态和信息
        MusicInfo currentMusic = musicManager.getCurrentMusic();
        if (currentMusic != null) {
            updateSongInfo(currentMusic);
            bindMusicService();
            Log.d(TAG, "从MusicManager恢复播放现有音乐: " + currentMusic.getMusicName());
        } else {
            Log.e(TAG, "MusicManager中没有当前播放音乐信息，无法初始化播放器");
            finish(); // 如果没有音乐信息，则关闭Activity
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
     * 实现OnPlaybackStateChangeListener接口 - 歌曲改变
     * 注意：这个方法接收位置参数，而不是MusicInfo对象
     */
    @Override
    public void onSongChanged(int position) {
        // 从播放列表获取相应位置的音乐信息
        MusicInfo musicInfo = musicManager.getMusicAt(position);
        if (musicInfo != null) {
            // 更新当前歌曲UI
            updateSongInfo(musicInfo);
            // 同步MusicManager的当前位置
            musicManager.setCurrentPosition(position);
        } else {
            Log.e(TAG, "onSongChanged: 位置 " + position + " 的音乐信息为null");
        }
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
     * 更新进度
     */
    private void updateProgress() {
        if (serviceBound && musicService != null) {
            int current = musicService.getCurrentPosition();
            int duration = musicService.getDuration();
            onProgressChanged(current, duration);
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

        // 第二阶段：回弹
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(btnLike, "scaleX", 1.3f, 1.0f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(btnLike, "scaleY", 1.3f, 1.0f);
        scaleDownX.setDuration(150);
        scaleDownY.setDuration(150);
        scaleDownX.setInterpolator(new OvershootInterpolator());
        scaleDownY.setInterpolator(new OvershootInterpolator());

        // 旋转动画
        ObjectAnimator rotation = ObjectAnimator.ofFloat(btnLike, "rotation", 0f, 360f);
        rotation.setDuration(300);
        rotation.setInterpolator(new DecelerateInterpolator());

        // 组合动画
        animatorSet.playSequentially(scaleUpX, scaleDownX);
        animatorSet.play(scaleUpY).with(scaleUpX);
        animatorSet.play(scaleDownY).with(scaleDownX);
        animatorSet.play(rotation).after(scaleUpX); // 旋转在放大后开始

        animatorSet.start();
    }

    /**
     * 取消点赞动画
     */
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

    /**
     * ServiceConnection，正确设置监听器
     */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
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
            musicService.setPlayMode(currentPlayMode);
            // 如果服务没有在播放，则开始播放当前音乐
            if (!musicService.isPlaying() && musicManager.getCurrentMusic() != null) {
                musicService.play();
            }

            // 更新UI状态
            updatePlaybackState(musicService.isPlaying());
            updateProgress();
            updatePlayModeButton();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.d(TAG, "服务已断开");
        }
    };

    private void bindMusicService() {
        Intent intent = new Intent(this, MusicPlayerService.class);
        // 启动服务，确保服务在后台运行
        startService(intent);
        // 绑定服务，以便Activity可以与服务通信
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        Log.d(TAG, "尝试绑定音乐服务");
    }

    private void unbindMusicService() {
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
            Log.d(TAG, "服务已解绑");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindMusicService();
        handler.removeCallbacks(updateProgressRunnable);
        Log.d(TAG, "Activity销毁");
    }

    private void updateSongInfo(MusicInfo musicInfo) {
        if (musicInfo == null) {
            Log.e(TAG, "updateSongInfo: musicInfo is null");
            return;
        }
        tvSongName.setText(musicInfo.getMusicName());
        tvArtistName.setText(musicInfo.getAuthor());

        // 加载封面并提取颜色
        Glide.with(this)
                .asBitmap()
                .load(musicInfo.getCoverUrl())
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (albumArtFragment != null) {
                            albumArtFragment.setAlbumArt(resource);
                        }
                        extractColorFromBitmap(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        if (albumArtFragment != null) {
                            albumArtFragment.setAlbumArt(null);
                        }
                    }
                });

        // 更新歌词Fragment
        if (lyricFragment != null) {
            lyricFragment.updateLyric(musicInfo.getLyricUrl());
        }

        // 更新点赞状态
        // isLiked = musicManager.getMusicDao().getMusicLikedStatus(musicInfo.getId()); // 需要MusicDao实例
        // updateLikeButton();
    }

    private void extractColorFromBitmap(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette != null) {
                int defaultColor = Color.parseColor("#424242"); // 深灰色
                currentDominantColor = palette.getDominantColor(defaultColor);
                int mutedColor = palette.getMutedColor(defaultColor);
                int vibrantColor = palette.getVibrantColor(defaultColor);

                // 使用提取的颜色更新UI
                updateUIColors(currentDominantColor);
            }
        });
    }

    private void updateUIColors(int color) {
        // 设置背景颜色，可以根据需要调整透明度或饱和度
        rootLayout.setBackgroundColor(color);

        // 调整文本颜色以确保可读性
        int textColor = getContrastColor(color);
        tvSongName.setTextColor(textColor);
        tvArtistName.setTextColor(textColor);
        tvCurrentTime.setTextColor(textColor);
        tvTotalTime.setTextColor(textColor);

        // 更新按钮颜色
        btnClose.setColorFilter(textColor);
        btnPrevious.setColorFilter(textColor);
        btnNext.setColorFilter(textColor);
        btnPlayMode.setColorFilter(textColor);
        btnPlaylist.setColorFilter(textColor);
        // btnLike.setColorFilter(textColor); // 点赞按钮颜色单独处理

        // 更新SeekBar颜色
        seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(textColor));
        seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(textColor));

        // 通知Fragment更新颜色
        if (albumArtFragment instanceof ColorAwareComponent) {
            ((ColorAwareComponent) albumArtFragment).updateColors(color, textColor);
        }
        if (lyricFragment instanceof ColorAwareComponent) {
            ((ColorAwareComponent) lyricFragment).updateColors(color, textColor);
        }
    }

    private int getContrastColor(int color) {
        // 根据背景色亮度选择黑色或白色作为对比色
        return ColorUtils.calculateLuminance(color) > 0.5 ? Color.BLACK : Color.WHITE;
    }

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        updatePlaybackState(isPlaying);
    }


    @Override
    public void onProgressUpdate(int currentPosition, int totalDuration) {
        onProgressChanged(currentPosition, totalDuration);
    }
//    @Override
//    public void onMusicChanged(MusicInfo musicInfo) {
//        updateSongInfo(musicInfo);
//        // 更新MusicManager的当前播放位置
//        musicManager.setCurrentPosition(musicManager.getPlaylist().indexOf(musicInfo));
//    }


//    进度条控制
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

    // 添加播放状态变化时的专辑封面动画控制
    private void updatePlaybackState(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            startUpdatingProgress();

            // 启动专辑旋转动画
            if (albumArtFragment != null) {
                albumArtFragment.setRotationAnimation(true);
            }
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
            stopUpdatingProgress();

            // 暂停专辑旋转动画
            if (albumArtFragment != null) {
                albumArtFragment.setRotationAnimation(false);
            }
        }
    }



    private void startUpdatingProgress() {
        if (updateProgressRunnable == null) {
            updateProgressRunnable = new Runnable() {
                @Override
                public void run() {
                    if (serviceBound && musicService.isPlaying()) {
                        int current = musicService.getCurrentPosition();
                        int duration = musicService.getDuration();
                        onProgressChanged(current, duration);
                    }
                    handler.postDelayed(this, 1000); // 每秒更新一次
                }
            };
        }
        handler.post(updateProgressRunnable);
    }

    private void stopUpdatingProgress() {
        if (updateProgressRunnable != null) {
            handler.removeCallbacks(updateProgressRunnable);
        }
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
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

    /**
     * 切换播放模式
     */
    private void switchPlayMode() {
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

    private int getCurrentThemeColor() {
        // 返回当前背景的主色调，用于点赞按钮未点赞时的颜色
        return currentDominantColor;
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


