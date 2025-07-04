package com.qzz.musiccommunity.Service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.instance.MusicManager;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.ui.views.MusicPlayer.MusicPlayerActivity;

import java.io.IOException;
import java.util.List;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicPlayerService";
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    // 播放模式枚举
    public enum PlayMode {
        SEQUENCE,    // 顺序播放
        RANDOM,      // 随机播放
        REPEAT_ONE   // 单曲循环
    }

    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;

    private MusicManager musicManager;
    private PlayMode currentPlayMode = PlayMode.SEQUENCE;

    private Handler progressHandler = new Handler();
    private Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
                int current = mediaPlayer.getCurrentPosition();
                int duration = mediaPlayer.getDuration();

                if (playbackStateChangeListener != null) {
                    playbackStateChangeListener.onProgressUpdate(current, duration);
                }
            }
            progressHandler.postDelayed(this, 1000); // 每秒更新
        }
    };

    private OnPlaybackStateChangeListener playbackStateChangeListener;

    public interface OnPlaybackStateChangeListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(int position);
        void onProgressUpdate(int currentPosition, int totalDuration);
    }

    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    private final IBinder binder = new MusicBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        createNotificationChannel();
        initMediaPlayer();

        // 如果已经初始化过，直接获取实例
        if (MusicManager.isInitialized()) {
            musicManager = MusicManager.getInstance();
        } else {
            // 如果未初始化，传入context进行初始化
            musicManager = MusicManager.getInstance(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 允许服务在后台运行
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Music playback controls");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    /**
     * 设置当前播放位置
     * @param position 要设置的播放位置
     */
    public void setCurrentPosition(int position) {
        List<MusicInfo> playlist = musicManager.getPlaylist();

        // 验证位置是否有效
        if (playlist == null || playlist.isEmpty() || position < 0 || position >= playlist.size()) {
            Log.e(TAG, "无法设置位置：无效的位置 " + position + " 或播放列表为空");
            return;
        }

        // 如果位置相同且已经在播放，不执行任何操作
        if (position == musicManager.getCurrentPosition() && isPrepared) {
            Log.d(TAG, "位置未变，保持当前播放状态");
            return;
        }

        Log.d(TAG, "设置当前播放位置为: " + position);

        // 更新MusicManager中的位置
        musicManager.setCurrentPosition(position);

        // 开始播放新位置的歌曲
        playAtPosition(position);
    }

    /**
     * 更新播放列表
     * @param newPlaylist 新的播放列表
     */
    public void updatePlaylist(List<MusicInfo> newPlaylist) {
        Log.d(TAG, "Service播放列表已更新，歌曲数量: " + (newPlaylist != null ? newPlaylist.size() : 0));

        // 如果播放列表为空，停止播放
        if (newPlaylist == null || newPlaylist.isEmpty()) {
            stop();
            return;
        }

        // 检查当前播放位置是否仍然有效
        int currentPosition = musicManager.getCurrentPosition();
        if (currentPosition >= newPlaylist.size()) {
            // 当前位置超出范围，调整到最后一首
            int newPosition = newPlaylist.size() - 1;
            musicManager.setCurrentPosition(newPosition);

            // 切换到新歌曲
            playAtPosition(newPosition);
        }
        // 如果当前位置仍有效，继续播放当前歌曲
    }

    /**
     * 设置播放模式
     * @param playMode 播放模式
     */
    public void setPlayMode(PlayMode playMode) {
        this.currentPlayMode = playMode;
        Log.d(TAG, "Service播放模式已更新: " + playMode);
    }

    /**
     * 获取当前播放模式
     * @return 当前播放模式
     */
    public PlayMode getPlayMode() {
        return currentPlayMode;
    }

    public void playAtPosition(int position) {
        List<MusicInfo> playlist = musicManager.getPlaylist();

        if (playlist == null || playlist.isEmpty() || position < 0 || position >= playlist.size()) {
            Log.e(TAG, "无法播放：无效的位置 " + position + " 或播放列表为空");
            return;
        }

        musicManager.setCurrentPosition(position);
        MusicInfo musicInfo = playlist.get(position);

        try {
            Log.d(TAG, "准备播放: " + musicInfo.getMusicName());
            mediaPlayer.reset();
            mediaPlayer.setDataSource(musicInfo.getMusicUrl());
            mediaPlayer.prepareAsync();
            isPrepared = false;

            updateNotification(musicInfo);

            if (playbackStateChangeListener != null) {
                playbackStateChangeListener.onSongChanged(position);
            }
        } catch (IOException e) {
            Log.e(TAG, "播放出错", e);
        }
    }

    public void play() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.start();
            startForeground(NOTIFICATION_ID, createNotification());

            if (playbackStateChangeListener != null) {
                playbackStateChangeListener.onPlaybackStateChanged(true);
            }
            Log.d(TAG, "开始播放");
        } else {
            Log.d(TAG, "无法播放：播放器未准备好");
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            stopForeground(false);
//            停止进度更新
            progressHandler.removeCallbacks(progressUpdateRunnable);

            if (playbackStateChangeListener != null) {
                playbackStateChangeListener.onPlaybackStateChanged(false);
            }
            Log.d(TAG, "暂停播放");
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            isPrepared = false;
            stopForeground(true);

            if (playbackStateChangeListener != null) {
                playbackStateChangeListener.onPlaybackStateChanged(false);
            }
            Log.d(TAG, "停止播放");
        }
    }

    /**
     * 强制重置播放器并切换到新歌曲
     * 此方法会中断所有当前播放和准备操作，确保立即切换
     * @param position 要播放的新位置
     */
    public void forcePlayAtPosition(int position) {
        Log.d(TAG, "强制切换到新位置: " + position);

        // 1. 停止当前所有播放和准备操作
        if (mediaPlayer != null) {
            // 取消所有可能的异步操作
            progressHandler.removeCallbacks(progressUpdateRunnable);

            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
            } catch (IllegalStateException e) {
                Log.e(TAG, "重置播放器时出错", e);
                // 如果当前播放器状态异常，则重新创建
                releaseMediaPlayer();
                initMediaPlayer();
            }
        }

        // 2. 更新播放位置
        List<MusicInfo> playlist = musicManager.getPlaylist();
        if (playlist == null || playlist.isEmpty() || position < 0 || position >= playlist.size()) {
            Log.e(TAG, "无法播放：无效的位置 " + position + " 或播放列表为空");
            return;
        }

        musicManager.setCurrentPosition(position);
        MusicInfo musicInfo = playlist.get(position);
        isPrepared = false;

        // 3. 开始新歌曲播放
        try {
            Log.d(TAG, "强制播放新歌曲: " + musicInfo.getMusicName());
            mediaPlayer.setDataSource(musicInfo.getMusicUrl());
            mediaPlayer.prepareAsync();

            // 4. 更新通知和界面
            updateNotification(musicInfo);
            if (playbackStateChangeListener != null) {
                playbackStateChangeListener.onSongChanged(position);
            }
        } catch (IOException e) {
            Log.e(TAG, "播放出错", e);
        }
    }

    /**
     * 完全释放MediaPlayer资源
     */
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.release();
            } catch (Exception e) {
                Log.e(TAG, "释放MediaPlayer时出错", e);
            }
            mediaPlayer = null;
        }
    }


    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaPlayer != null && isPrepared ? mediaPlayer.getDuration() : 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
            Log.d(TAG, "跳转到: " + position + "ms");
        }
    }

    public void setOnPlaybackStateChangeListener(OnPlaybackStateChangeListener listener) {
        this.playbackStateChangeListener = listener;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;
        Log.d(TAG, "媒体准备完成，开始播放");
        play();
        // 启动进度更新
        progressHandler.post(progressUpdateRunnable);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "当前歌曲播放完成");
        playNextBasedOnMode();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "播放错误: what=" + what + ", extra=" + extra);
        isPrepared = false;
        playNextBasedOnMode();
        return true;
    }

    /**
     * 根据播放模式播放下一首
     */
    private void playNextBasedOnMode() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        if (playlist == null || playlist.isEmpty()) {
            Log.d(TAG, "无法播放下一首：播放列表为空");
            return;
        }

        int currentPosition = musicManager.getCurrentPosition();
        int nextPosition = getNextPosition(currentPosition, playlist.size());

        Log.d(TAG, "根据播放模式(" + currentPlayMode + ")播放下一首: " + currentPosition + " -> " + nextPosition);
        playAtPosition(nextPosition);
    }

    /**
     * 播放下一首（手动切换）
     */
    public void playNext() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        if (playlist == null || playlist.isEmpty()) {
            Log.d(TAG, "无法播放下一首：播放列表为空");
            return;
        }

        int currentPosition = musicManager.getCurrentPosition();
        int nextPosition;

        // 手动切换时，即使是单曲循环模式也要切换到下一首
        if (currentPlayMode == PlayMode.RANDOM) {
            nextPosition = getRandomPosition(playlist.size());
        } else {
            nextPosition = (currentPosition + 1) % playlist.size();
        }

        Log.d(TAG, "手动播放下一首: " + currentPosition + " -> " + nextPosition);
        playAtPosition(nextPosition);
    }

    /**
     * 播放上一首
     */
    public void playPrevious() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        if (playlist == null || playlist.isEmpty()) {
            Log.d(TAG, "无法播放上一首：播放列表为空");
            return;
        }

        int currentPosition = musicManager.getCurrentPosition();
        int prevPosition;

        if (currentPlayMode == PlayMode.RANDOM) {
            prevPosition = getRandomPosition(playlist.size());
        } else {
            prevPosition = (currentPosition - 1 + playlist.size()) % playlist.size();
        }

        Log.d(TAG, "播放上一首: " + currentPosition + " -> " + prevPosition);
        playAtPosition(prevPosition);
    }

    /**
     * 根据当前播放模式获取下一首歌曲的位置
     */
    private int getNextPosition(int currentPosition, int playlistSize) {
        switch (currentPlayMode) {
            case SEQUENCE:
                return (currentPosition + 1) % playlistSize;
            case RANDOM:
                return getRandomPosition(playlistSize);
            case REPEAT_ONE:
                return currentPosition; // 单曲循环，继续播放当前歌曲
            default:
                return (currentPosition + 1) % playlistSize;
        }
    }

    /**
     * 获取随机位置
     */
    private int getRandomPosition(int playlistSize) {
        if (playlistSize <= 1) {
            return 0;
        }

        int currentPosition = musicManager.getCurrentPosition();
        int randomPosition;

        // 确保随机位置不是当前位置（避免连续播放同一首歌）
        do {
            randomPosition = (int) (Math.random() * playlistSize);
        } while (randomPosition == currentPosition && playlistSize > 1);

        return randomPosition;
    }

    private Notification createNotification() {
        MusicInfo currentMusic = musicManager.getCurrentMusic();
        if (currentMusic == null) {
            Log.e(TAG, "无法创建通知：当前没有音乐");
            return null;
        }

        Intent intent = new Intent(this, MusicPlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(currentMusic.getMusicName())
                .setContentText(currentMusic.getAuthor())
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(MusicInfo musicInfo) {
        Notification notification = createNotification();
        if (notification != null) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.notify(NOTIFICATION_ID, notification);
            Log.d(TAG, "通知已更新: " + musicInfo.getMusicName());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
    }
}
