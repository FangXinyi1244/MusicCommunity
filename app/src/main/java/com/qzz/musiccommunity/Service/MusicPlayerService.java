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
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.qzz.musiccommunity.R;
import com.qzz.musiccommunity.instance.MusicManager;
import com.qzz.musiccommunity.network.dto.MusicInfo;
import com.qzz.musiccommunity.ui.views.MusicPlayer.MusicPlayerActivity;

import java.io.IOException;
import java.util.List;

public class MusicPlayerService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = "MusicPlayerService";
    private static final String CHANNEL_ID = "MusicPlayerChannel";
    private static final int NOTIFICATION_ID = 1;

    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;

    private MusicManager musicManager;

    private OnPlaybackStateChangeListener playbackStateChangeListener;

    public interface OnPlaybackStateChangeListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onSongChanged(int position);
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

        musicManager = MusicManager.getInstance();
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

            if (playbackStateChangeListener != null) {
                playbackStateChangeListener.onPlaybackStateChanged(false);
            }
            Log.d(TAG, "暂停播放");
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            stopForeground(true);

            if (playbackStateChangeListener != null) {
                playbackStateChangeListener.onPlaybackStateChanged(false);
            }
            Log.d(TAG, "停止播放");
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
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "当前歌曲播放完成");
        playNext();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "播放错误: what=" + what + ", extra=" + extra);
        isPrepared = false;
        playNext();
        return true;
    }

    public void playNext() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        int currentPosition = musicManager.getCurrentPosition();

        if (playlist != null && !playlist.isEmpty()) {
            int nextPosition = (currentPosition + 1) % playlist.size();
            Log.d(TAG, "播放下一首，当前位置: " + currentPosition + " -> 下一位置: " + nextPosition);
            playAtPosition(nextPosition);
        } else {
            Log.d(TAG, "无法播放下一首：播放列表为空");
        }
    }

    public void playPrevious() {
        List<MusicInfo> playlist = musicManager.getPlaylist();
        int currentPosition = musicManager.getCurrentPosition();

        if (playlist != null && !playlist.isEmpty()) {
            int prevPosition = (currentPosition - 1 + playlist.size()) % playlist.size();
            Log.d(TAG, "播放上一首，当前位置: " + currentPosition + " -> 上一位置: " + prevPosition);
            playAtPosition(prevPosition);
        } else {
            Log.d(TAG, "无法播放上一首：播放列表为空");
        }
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

