package com.qzz.musiccommunity.instance;

import android.util.Log;

import com.qzz.musiccommunity.model.BannerItem;
import com.qzz.musiccommunity.model.HorizontalCardItem;
import com.qzz.musiccommunity.model.OneColumnItem;
import com.qzz.musiccommunity.model.TwoColumnItem;
import com.qzz.musiccommunity.model.iface.ListItem;
import com.qzz.musiccommunity.network.dto.MusicInfo;
import java.util.ArrayList;
import java.util.List;

public class MusicManager {
    private static final String TAG = "MusicManager";

    // 单例实例
    private static volatile MusicManager instance;

    // 音乐列表数据
    private List<MusicInfo> currentPlaylist = new ArrayList<>();
    private int currentPosition = 0;

    // 私有构造函数防止外部实例化
    private MusicManager() {}

    // 获取单例实例的方法
    public static MusicManager getInstance() {
        if (instance == null) {
            synchronized (MusicManager.class) {
                if (instance == null) {
                    instance = new MusicManager();
                    Log.d(TAG, "MusicManager 单例已创建");
                }
            }
        }
        return instance;
    }

    // 设置当前播放列表
    public void setPlaylist(List<MusicInfo> playlist) {
        if (playlist == null) {
            currentPlaylist = new ArrayList<>();
        } else {
            currentPlaylist = new ArrayList<>(playlist);
        }
        Log.d(TAG, "已更新播放列表，共 " + currentPlaylist.size() + " 首歌曲");
    }

    // 添加歌曲到播放列表
    public void addToPlaylist(MusicInfo musicInfo) {
        if (musicInfo != null) {
            currentPlaylist.add(musicInfo);
            Log.d(TAG, "已添加歌曲：" + musicInfo.getMusicName());
        }
    }

    // 添加多首歌曲到播放列表
    public void addAllToPlaylist(List<MusicInfo> musicList) {
        if (musicList != null && !musicList.isEmpty()) {
            currentPlaylist.addAll(musicList);
            Log.d(TAG, "已添加 " + musicList.size() + " 首歌曲到播放列表");
        }
    }

    // 获取当前播放列表
    public List<MusicInfo> getPlaylist() {
        return new ArrayList<>(currentPlaylist);
    }

    // 清空播放列表
    public void clearPlaylist() {
        currentPlaylist.clear();
        currentPosition = 0;
        Log.d(TAG, "播放列表已清空");
    }

    // 设置当前播放位置
    public void setCurrentPosition(int position) {
        if (position >= 0 && position < currentPlaylist.size()) {
            currentPosition = position;
            Log.d(TAG, "当前播放位置已设置为：" + position);
        } else {
            Log.e(TAG, "设置位置无效：" + position);
        }
    }

    // 获取当前播放位置
    public int getCurrentPosition() {
        return currentPosition;
    }

    // 获取当前播放的音乐
    public MusicInfo getCurrentMusic() {
        if (currentPlaylist.isEmpty() || currentPosition >= currentPlaylist.size()) {
            return null;
        }
        return currentPlaylist.get(currentPosition);
    }

    // 获取播放列表大小
    public int getPlaylistSize() {
        return currentPlaylist.size();
    }

    // 从主页的列表项收集所有音乐信息
    public void collectMusicFromListItems(List<ListItem> listItems) {
        if (listItems == null || listItems.isEmpty()) {
            Log.d(TAG, "没有可收集的音乐数据");
            return;
        }

        List<MusicInfo> allMusic = new ArrayList<>();

        for (ListItem item : listItems) {
            if (item instanceof BannerItem) {
                allMusic.addAll(((BannerItem) item).getMusicList());
            } else if (item instanceof HorizontalCardItem) {
                allMusic.addAll(((HorizontalCardItem) item).getMusicList());
            } else if (item instanceof OneColumnItem) {
                allMusic.addAll(((OneColumnItem) item).getMusicList());
            } else if (item instanceof TwoColumnItem) {
                allMusic.addAll(((TwoColumnItem) item).getMusicList());
            }
        }

        setPlaylist(allMusic);
        Log.d(TAG, "已从列表项收集 " + allMusic.size() + " 首歌曲");
    }
}
