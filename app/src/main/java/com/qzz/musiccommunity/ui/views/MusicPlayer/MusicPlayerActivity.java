package com.qzz.musiccommunity.ui.views.MusicPlayer;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import java.util.List;

public class MusicPlayerActivity extends AppCompatActivity implements MusicPlayerService.OnPlaybackStateChangeListener {

    private ViewPager2 viewPager;
    private TextView tvSongName, tvArtistName, tvCurrentTime, tvTotalTime;
    private ImageView btnClose, btnPlayPause, btnPrevious, btnNext, btnPlayMode, btnPlaylist, btnLike;
    private SeekBar seekBar;
    private ConstraintLayout rootLayout;

    private MusicPlayerService musicService;
    private boolean serviceBound = false;
    private Handler handler = new Handler();
    private Runnable updateProgressRunnable;

    private List<MusicInfo> playlist;
    private int currentPosition = 0;
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

        initViews();
        setupViewPager();
        setupListeners();

        // 使用MusicManager获取播放列表和当前位置
        MusicManager musicManager = MusicManager.getInstance();
        playlist = musicManager.getPlaylist();
        currentPosition = musicManager.getCurrentPosition();

        if (playlist != null && !playlist.isEmpty()) {
            updateSongInfo(playlist.get(currentPosition));
            bindMusicService();
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
            public void onStopTrackingTouch(SeekBar seekBar) {}
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
            
            // 设置播放列表并开始播放
            musicService.setPlaylist(playlist);
            musicService.playAtPosition(currentPosition);
            
            startProgressUpdate();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void updateSongInfo(MusicInfo musicInfo) {
        tvSongName.setText(musicInfo.getMusicName());
        tvArtistName.setText(musicInfo.getAuthor());
        
        // 更新专辑图片和背景色
        loadAlbumArt(musicInfo.getCoverUrl());
        
        // 更新Fragment中的数据
        if (albumArtFragment != null) {
            albumArtFragment.updateAlbumArt(musicInfo.getCoverUrl());
        }
        if (lyricFragment != null) {
            lyricFragment.updateLyric(musicInfo.getLyricUrl());
        }
    }

    private void loadAlbumArt(String coverUrl) {
        Glide.with(this)
                .asBitmap()
                .load(coverUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // 提取主题色并设置背景
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
        if (playlist == null || playlist.isEmpty()) return;
        
        switch (currentPlayMode) {
            case SEQUENCE:
            case REPEAT_ONE:
                currentPosition = (currentPosition - 1 + playlist.size()) % playlist.size();
                break;
            case RANDOM:
                currentPosition = (int) (Math.random() * playlist.size());
                break;
        }
        
        updateSongInfo(playlist.get(currentPosition));
        if (serviceBound) {
            musicService.playAtPosition(currentPosition);
        }
    }

    private void playNext() {
        if (playlist == null || playlist.isEmpty()) return;
        
        switch (currentPlayMode) {
            case SEQUENCE:
                currentPosition = (currentPosition + 1) % playlist.size();
                break;
            case REPEAT_ONE:
                // 单曲循环不改变位置
                break;
            case RANDOM:
                currentPosition = (int) (Math.random() * playlist.size());
                break;
        }
        
        updateSongInfo(playlist.get(currentPosition));
        if (serviceBound) {
            musicService.playAtPosition(currentPosition);
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
    }

    private void startProgressUpdate() {
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
                        
                        // 通知歌词Fragment更新进度
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
        btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        
        // 控制专辑图片旋转动画
        if (albumArtFragment != null) {
            albumArtFragment.setRotationAnimation(isPlaying);
        }
    }

    @Override
    public void onSongChanged(int position) {
        currentPosition = position;
        if (playlist != null && position < playlist.size()) {
            updateSongInfo(playlist.get(position));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

