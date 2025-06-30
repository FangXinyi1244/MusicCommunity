package com.qzz.musiccommunity.ui.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.Service.MusicPlayerService;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.instance.MusicManager;
import com.qzz.musiccommunity.ui.views.MusicPlayer.MusicPlayerActivity;
import com.qzz.musiccommunity.ui.views.home.HomeActivity;

public class BottomMusicPlayerView extends FrameLayout {
    private static final String TAG = "BottomMusicPlayerView";

    private ImageView ivAlbumArt;
    private TextView tvSongName;
    private TextView tvArtistName;
    private ImageButton btnPlayPause;
    private ImageButton btnPlaylist;

    private MusicManager musicManager;
    private MusicPlayerService musicService;
    private boolean isServiceBound = false;

    private OnBottomPlayerActionListener actionListener;
    private boolean needUpdatePlaybackState = true;

    public interface OnBottomPlayerActionListener {
        void onBottomPlayerClick();
        void onPlaylistButtonClick();
    }

    // 创建服务连接
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;

            // 设置播放状态监听器
            musicService.setOnPlaybackStateChangeListener(new MusicPlayerService.OnPlaybackStateChangeListener() {
                @Override
                public void onPlaybackStateChanged(boolean isPlaying) {
                    updatePlayPauseButton(isPlaying);
                }

                @Override
                public void onSongChanged(int position) {
                    updatePlayerView();
                }

                @Override
                public void onProgressUpdate(int currentPosition, int totalDuration) {
                    // 底部播放器不需要进度更新
                }
            });

            // 服务连接后更新UI
            updatePlayerView();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isServiceBound = false;
        }
    };

    public BottomMusicPlayerView(Context context) {
        super(context);
        init(context);
    }

    public BottomMusicPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BottomMusicPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.bottom_player_layout, this, true);

        // 查找视图组件
        ivAlbumArt = findViewById(R.id.iv_album_art);
        tvSongName = findViewById(R.id.tv_song_name);
        tvArtistName = findViewById(R.id.tv_artist_name);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnPlaylist = findViewById(R.id.btn_playlist);

        musicManager = MusicManager.getInstance(context);
        bindMusicService();

        // 设置整个播放器区域的点击事件
        setOnClickListener(v -> {
            if (actionListener != null) {
                actionListener.onBottomPlayerClick();
            } else {
                Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
                getContext().startActivity(intent);
            }
        });

        // 正确设置播放/暂停按钮点击事件
        btnPlayPause.setOnClickListener(v -> {
            // 添加判断，避免重复点击
            if (v.isEnabled()) {
                v.setEnabled(false);
                if (isServiceBound && musicService != null) {
                    if (musicService.isPlaying()) {
                        musicService.pause();
                    } else {
                        musicService.play();
                    }
                }
                // 延迟恢复按钮可点击状态
                v.postDelayed(() -> v.setEnabled(true), 300);
            }
        });

        // 正确设置播放列表按钮点击事件
        btnPlaylist.setOnClickListener(v -> {
            // 添加判断，避免重复点击
            if (v.isEnabled()) {
                v.setEnabled(false);
                if (actionListener != null) {
                    actionListener.onPlaylistButtonClick();
                } else {
                    Intent intent = new Intent(getContext(), MusicPlayerActivity.class);
                    intent.putExtra("SHOW_PLAYLIST", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    getContext().startActivity(intent);
                }
                // 延迟恢复按钮可点击状态
                v.postDelayed(() -> v.setEnabled(true), 300);
            }
        });

        // 初始状态为隐藏
        setVisibility(View.GONE);
    }


    /**
     * 设置底部播放器操作监听器
     * @param listener 监听器对象
     */
    public void setOnBottomPlayerActionListener(OnBottomPlayerActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * 绑定音乐播放服务
     */
    private void bindMusicService() {
        if (!isServiceBound) {
            Intent intent = new Intent(getContext(), MusicPlayerService.class);
            // 先启动服务，确保服务实例存在
            getContext().startService(intent);
            getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * 解绑音乐播放服务
     */
    public void unbindMusicService() {
        if (isServiceBound) {
            getContext().unbindService(serviceConnection);
            isServiceBound = false;
        }
    }

    // 注册和解注册播放状态变化监听器
    public void registerPlaybackStateChangeListener() {
        needUpdatePlaybackState = true;
        if (isServiceBound && musicService != null) {
            musicService.setOnPlaybackStateChangeListener(new MusicPlayerService.OnPlaybackStateChangeListener() {
                @Override
                public void onPlaybackStateChanged(boolean isPlaying) {
                    if (needUpdatePlaybackState) {
                        updatePlayPauseButton(isPlaying);
                    }
                }
                @Override
                public void onSongChanged(int position) {
                    if (needUpdatePlaybackState) {
                        updatePlayerView();
                    }
                }
                @Override
                public void onProgressUpdate(int currentPosition, int totalDuration) {
                    // 底部播放器不需要进度更新
                }
            });
        }
    }

    public void unregisterPlaybackStateChangeListener() {
        needUpdatePlaybackState = false;
    }

    // 暂停更新任务，用于在切换Activity时减少资源消耗
    public void pauseUpdateTasks() {
        needUpdatePlaybackState = false;
    }

    /**
     * 更新播放/暂停按钮状态
     */
    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            btnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    /**
     * 更新播放器视图
     */
    public void updatePlayerView() {
        MusicInfo currentMusic = musicManager.getCurrentMusic();
        if (currentMusic != null) {
            tvSongName.setText(currentMusic.getMusicName());
            tvArtistName.setText(currentMusic.getAuthor());
            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.default_album_art)
                    .error(R.drawable.default_album_art);
            Glide.with(getContext())
                    .load(currentMusic.getCoverUrl())
                    .apply(options)
                    .into(ivAlbumArt);
            if (isServiceBound && musicService != null) {
                updatePlayPauseButton(musicService.isPlaying());
            }
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
            }
        } else {
            // 没有当前音乐时隐藏
            setVisibility(View.GONE);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        // 只有当父类没有处理事件时，我们才手动处理
        if (event.getAction() == MotionEvent.ACTION_UP && !result) {
            return performClick();
        }
        return result;
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }

}
