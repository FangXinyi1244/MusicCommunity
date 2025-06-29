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

    /**
     * 查找音乐在播放列表中的索引
     * 使用音乐URL作为唯一标识符进行比较
     */
    private int findMusicIndex(MusicInfo musicInfo) {
        if (musicInfo == null || musicInfo.getMusicUrl() == null) {
            return -1;
        }

        for (int i = 0; i < currentPlaylist.size(); i++) {
            MusicInfo music = currentPlaylist.get(i);
            if (music != null && music.getMusicUrl() != null &&
                    music.getMusicUrl().equals(musicInfo.getMusicUrl())) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 添加歌曲到播放列表（修正版本）
     * 如果歌曲已存在，则将其移动到列表末尾
     * 如果歌曲不存在，则直接添加到列表末尾
     */
    public void addToPlaylist(MusicInfo musicInfo) {
        if (musicInfo == null) {
            Log.e(TAG, "addToPlaylist: musicInfo为null");
            return;
        }

        // 检查歌曲是否已经在播放列表中
        int existingIndex = findMusicIndex(musicInfo);

        if (existingIndex == -1){
            // 如果歌曲不存在，直接添加到末尾
            currentPlaylist.add(musicInfo);
            Log.d(TAG, "添加新歌曲到末尾: " + musicInfo.getMusicName());
        }
    }

    /**
     * 添加新歌曲并重新排列播放列表
     * 将新歌曲放在列表开头，原有歌曲保持相对顺序放在后面
     */
    public void addAndReorderPlaylist(MusicInfo newMusic) {
        if (newMusic == null) {
            Log.e(TAG, "addAndReorderPlaylist: newMusic为null");
            return;
        }

        // 检查新歌曲是否已经在播放列表中
        int existingIndex = findMusicIndex(newMusic);

        if (existingIndex != -1) {
            // 如果歌曲已存在，将其移到开头
            currentPlaylist.remove(existingIndex);
            currentPlaylist.add(0, newMusic);
            Log.d(TAG, "歌曲已存在，移动到开头: " + newMusic.getMusicName());
        } else {
            // 如果歌曲不存在，添加到开头
            currentPlaylist.add(0, newMusic);
            Log.d(TAG, "添加新歌曲到开头: " + newMusic.getMusicName());
        }

        // 设置当前播放位置为0（新添加的歌曲）
        currentPosition = 0;

        Log.d(TAG, "播放列表重新排列完成，当前播放: " + newMusic.getMusicName() + "，列表总数: " + currentPlaylist.size());
    }



    // 清空播放列表
    public void clearPlaylist() {
        currentPlaylist.clear();
        currentPosition = 0;
        Log.d(TAG, "播放列表已清空");
    }

    // 获取当前播放列表
    public List<MusicInfo> getPlaylist() {
        return new ArrayList<>(currentPlaylist);
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

    // 检查播放列表是否为空
    public boolean isPlaylistEmpty() {
        return currentPlaylist.isEmpty();
    }

    // 从主页的列表项收集所有音乐信息（保留此方法用于批量操作）
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

