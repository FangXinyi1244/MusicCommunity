package com.qzz.musiccommunity.ui.views.MusicPlayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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
import com.qzz.musiccommunity.network.dto.MusicInfo;
import com.qzz.musiccommunity.ui.views.MusicPlayer.fragment.AlbumArtFragment;
import com.qzz.musiccommunity.ui.views.MusicPlayer.fragment.LyricFragment;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity implements MusicPlayerService.OnPlaybackStateChangeListener {

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
    private PlayMode currentPlayMode = PlayMode.SEQUENCE;
    private AlbumArtFragment albumArtFragment;
    private LyricFragment lyricFragment;
    public enum PlayMode {
        SEQUENCE, RANDOM, REPEAT_ONE
    }
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
        btnClose.setOnClickListener(v -> finish());
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
        loadAlbumArt(musicInfo.getCoverUrl());
        handler.post(() -> {
            if (albumArtFragment != null) {
                albumArtFragment.updateAlbumArt(musicInfo.getCoverUrl());
            }
            if (lyricFragment != null) {
                lyricFragment.updateLyric(musicInfo.getLyricUrl());
            }
        });
    }
    private void loadAlbumArt(String coverUrl) {
        Glide.with(this)
                .asBitmap()
                .load(coverUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Palette.from(resource).generate(palette -> {
                            if (palette != null) {
                                int dominantColor = palette.getDominantColor(0xFF424242);
                                rootLayout.setBackgroundColor(dominantColor);
                            }
                        });
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }
    private void playPrevious() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        if (playlist == null || playlist.isEmpty()) {
            Log.w(TAG, "playPrevious: 播放列表为空");
            return;
        }
        int currentPosition = musicManager.getCurrentPosition();
        int newPosition;
        switch (currentPlayMode) {
            case SEQUENCE:
            case REPEAT_ONE:
                newPosition = (currentPosition - 1 + playlist.size()) % playlist.size();
                break;
            case RANDOM:
                newPosition = (int) (Math.random() * playlist.size());
                break;
            default:
                newPosition = (currentPosition - 1 + playlist.size()) % playlist.size();
                break;
        }
        Log.d(TAG, "播放上一首: " + currentPosition + " -> " + newPosition);
        musicManager.setCurrentPosition(newPosition);
        updateSongInfo(playlist.get(newPosition));
        if (serviceBound) {
            musicService.playAtPosition(newPosition);
        }
    }
    private void playNext() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        if (playlist == null || playlist.isEmpty()) {
            Log.w(TAG, "playNext: 播放列表为空");
            return;
        }
        int currentPosition = musicManager.getCurrentPosition();
        int newPosition;
        switch (currentPlayMode) {
            case SEQUENCE:
                newPosition = (currentPosition + 1) % playlist.size();
                break;
            case REPEAT_ONE:
                newPosition = currentPosition;
                break;
            case RANDOM:
                newPosition = (int) (Math.random() * playlist.size());
                break;
            default:
                newPosition = (currentPosition + 1) % playlist.size();
                break;
        }
        Log.d(TAG, "播放下一首: " + currentPosition + " -> " + newPosition);
        musicManager.setCurrentPosition(newPosition);
        updateSongInfo(playlist.get(newPosition));
        if (serviceBound) {
            musicService.playAtPosition(newPosition);
        }
    }
    private void switchPlayMode() {
        switch (currentPlayMode) {
            case SEQUENCE:
                currentPlayMode = PlayMode.RANDOM;
                btnPlayMode.setImageResource(R.drawable.ic_shuffle);
                break;
            case RANDOM:
                currentPlayMode = PlayMode.REPEAT_ONE;
                btnPlayMode.setImageResource(R.drawable.ic_repeat_one);
                break;
            case REPEAT_ONE:
                currentPlayMode = PlayMode.SEQUENCE;
                btnPlayMode.setImageResource(R.drawable.ic_repeat);
                break;
        }
        Log.d(TAG, "切换播放模式: " + currentPlayMode);
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

