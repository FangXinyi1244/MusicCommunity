package com.qzz.musiccommunity.instance;

import android.content.Context;
import android.util.Log;

import com.qzz.musiccommunity.database.MusicDao;
import com.qzz.musiccommunity.database.dto.MusicInfo;
import com.qzz.musiccommunity.model.BannerItem;
import com.qzz.musiccommunity.model.HorizontalCardItem;
import com.qzz.musiccommunity.model.OneColumnItem;
import com.qzz.musiccommunity.model.TwoColumnItem;
import com.qzz.musiccommunity.model.iface.ListItem;
import java.util.ArrayList;
import java.util.List;

public class MusicManager {
    private static final String TAG = "MusicManager";

    // 单例实例
    private static volatile MusicManager instance;
    private MusicDao musicDao;

    // 音乐列表数据
    private List<MusicInfo> currentPlaylist = new ArrayList<>();
    private int currentPosition = 0;

    // 私有构造函数防止外部实例化
    private MusicManager(Context context) {
        musicDao =  MusicDao.getInstance(context.getApplicationContext());
        // 初始化时从本地缓存加载播放列表
        currentPlaylist = musicDao.loadPlaylist();
        Log.d(TAG, "MusicManager 单例已创建，并从本地加载 " + currentPlaylist.size() + " 首歌曲");
    }

    // 获取单例实例的方法
    public static MusicManager getInstance(Context context) {
        if (instance == null) {
            synchronized (MusicManager.class) {
                if (instance == null) {
                    instance = new MusicManager(context);
                }
            }
        }
        return instance;
    }

    // 无参数的getInstance方法（用于后续调用）
    public static MusicManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("MusicManager未初始化！请先调用getInstance(Context)");
        }
        return instance;
    }
    // 添加一个检查是否已初始化的方法
    public static boolean isInitialized() {
        return instance != null;
    }


    // 设置当前播放列表
    public void setPlaylist(List<MusicInfo> playlist) {
        if (playlist == null) {
            currentPlaylist = new ArrayList<>();
        } else {
            currentPlaylist = new ArrayList<>(playlist);
        }
        currentPosition = 0; // 重置播放位置
        musicDao.savePlaylist(currentPlaylist); // 更新本地缓存
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
        musicDao.savePlaylist(currentPlaylist); // 更新本地缓存
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
        musicDao.savePlaylist(currentPlaylist); // 更新本地缓存
        Log.d(TAG, "播放列表重新排列完成，当前播放: " + newMusic.getMusicName() + "，列表总数: " + currentPlaylist.size());
    }

    /**
     * 删除指定位置的歌曲
     * @param position 要删除的歌曲在播放列表中的位置
     * @return 是否删除成功
     */
    public boolean removeSong(int position) {
        if (position < 0 || position >= currentPlaylist.size()) {
            Log.e(TAG, "removeSong: 无效的位置 " + position);
            return false;
        }

        MusicInfo removedMusic = currentPlaylist.get(position);
        currentPlaylist.remove(position);

        // 调整当前播放位置
        if (currentPosition > position) {
            // 删除的歌曲在当前播放歌曲之前，当前位置需要前移
            currentPosition--;
        } else if (currentPosition == position) {
            // 删除的是当前播放歌曲
            if (currentPlaylist.isEmpty()) {
                // 如果播放列表为空，重置位置
                currentPosition = 0;
            } else if (currentPosition >= currentPlaylist.size()) {
                // 如果当前位置超出范围，调整到最后一首
                currentPosition = currentPlaylist.size() - 1;
            }
            // 如果当前位置仍在有效范围内，保持不变
        }
        musicDao.savePlaylist(currentPlaylist); // 更新本地缓存
        Log.d(TAG, "删除歌曲: " + removedMusic.getMusicName() +
                ", 新的播放位置: " + currentPosition +
                ", 剩余歌曲数: " + currentPlaylist.size());

        return true;
    }

    // 在MusicManager类中添加此方法
    public boolean hasPlaylist() {
        // 根据您的实现返回是否有播放列表
        return currentPlaylist != null && !currentPlaylist.isEmpty();
    }

    /**
     * 从播放列表中删除指定的歌曲
     * @param musicInfo 要删除的音乐信息
     * @return 是否删除成功
     */
    public boolean removeMusic(MusicInfo musicInfo) {
        if (musicInfo == null) {
            Log.e(TAG, "removeMusic: musicInfo为null");
            return false;
        }

        int existingIndex = findMusicIndex(musicInfo);
        if (existingIndex != -1) {
            return removeSong(existingIndex);
        } else {
            Log.e(TAG, "删除失败: 播放列表中未找到歌曲 " + musicInfo.getMusicName());
            return false;
        }
    }

    // 清空播放列表
    public void clearPlaylist() {
        currentPlaylist.clear();
        currentPosition = 0;
        musicDao.savePlaylist(currentPlaylist); // 更新本地缓存
        Log.d(TAG, "播放列表已清空");
    }

    // 获取当前播放列表（返回副本以防止外部修改）
    public List<MusicInfo> getPlaylist() {
        return new ArrayList<>(currentPlaylist);
    }

    // 设置当前播放位置
    public void setCurrentPosition(int position) {
        if (currentPlaylist.isEmpty()) {
            currentPosition = 0;
            Log.d(TAG, "播放列表为空，位置设置为0");
            return;
        }

        if (position >= 0 && position < currentPlaylist.size()) {
            currentPosition = position;
            Log.d(TAG, "当前播放位置已设置为：" + position);
        } else {
            Log.e(TAG, "设置位置无效：" + position + "，播放列表大小：" + currentPlaylist.size());
        }
    }

    // 获取当前播放位置
    public int getCurrentPosition() {
        return currentPosition;
    }

    // 获取当前播放的音乐
    public MusicInfo getCurrentMusic() {
        if (currentPlaylist.isEmpty() || currentPosition < 0 || currentPosition >= currentPlaylist.size()) {
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

    /**
     * 获取指定位置的音乐信息
     * @param position 位置索引
     * @return 音乐信息，如果位置无效则返回null
     */
    public MusicInfo getMusicAt(int position) {
        if (position >= 0 && position < currentPlaylist.size()) {
            return currentPlaylist.get(position);
        }
        return null;
    }

    /**
     * 检查位置是否有效
     * @param position 要检查的位置
     * @return 位置是否有效
     */
    public boolean isValidPosition(int position) {
        return position >= 0 && position < currentPlaylist.size();
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


