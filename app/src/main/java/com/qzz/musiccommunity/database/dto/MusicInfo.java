package com.qzz.musiccommunity.database.dto;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

/**
 * 音乐信息实体类 - 统一版本
 * 支持数据库存储、网络传输和Activity间传递
 */
public class MusicInfo implements Parcelable {
    private static final String TAG = "MusicInfo";

    // 是否开启调试日志（生产环境应设为false）
    private static final boolean DEBUG = false;

    // 基础信息
    private long id;
    private String musicName;
    private String author;
    private String album;           // 新增：专辑名称
    private long duration;          // 新增：音乐时长（毫秒）

    // URL信息
    private String musicUrl;
    private String coverUrl;
    private String lyricUrl;

    // 用户相关
    private boolean isLiked;        // 是否收藏
    private long playCount;         // 新增：播放次数
    private long addTime;           // 新增：添加时间戳

    // 音质信息（可选）
    private String quality;         // 音质标识：如 "128k", "320k", "lossless"
    private long fileSize;          // 文件大小（字节）

    /**
     * 默认构造函数
     */
    public MusicInfo() {
        this.addTime = System.currentTimeMillis();
        debugLog("MusicInfo created with default constructor");
    }

    /**
     * 便捷构造函数 - 基础信息
     */
    public MusicInfo(long id, String musicName, String author, String musicUrl) {
        this();
        this.id = id;
        this.musicName = musicName;
        this.author = author;
        this.musicUrl = musicUrl;
        debugLog("MusicInfo created with basic info: " + musicName + " - " + author);
    }

    /**
     * 从Parcel重建对象
     */
    protected MusicInfo(Parcel in) {
        id = in.readLong();
        musicName = in.readString();
        author = in.readString();
        album = in.readString();
        duration = in.readLong();
        musicUrl = in.readString();
        coverUrl = in.readString();
        lyricUrl = in.readString();
        isLiked = in.readByte() != 0;
        playCount = in.readLong();
        addTime = in.readLong();
        quality = in.readString();
        fileSize = in.readLong();

        debugLog("MusicInfo restored from Parcel: " + musicName);
    }

    public static final Creator<MusicInfo> CREATOR = new Creator<MusicInfo>() {
        @Override
        public MusicInfo createFromParcel(Parcel in) {
            return new MusicInfo(in);
        }

        @Override
        public MusicInfo[] newArray(int size) {
            return new MusicInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(musicName);
        dest.writeString(author);
        dest.writeString(album);
        dest.writeLong(duration);
        dest.writeString(musicUrl);
        dest.writeString(coverUrl);
        dest.writeString(lyricUrl);
        dest.writeByte((byte) (isLiked ? 1 : 0));
        dest.writeLong(playCount);
        dest.writeLong(addTime);
        dest.writeString(quality);
        dest.writeLong(fileSize);

        debugLog("MusicInfo written to Parcel: " + musicName);
    }

    // ==================== Getters and Setters ====================

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getMusicName() { return musicName; }
    public void setMusicName(String musicName) { this.musicName = musicName; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public String getMusicUrl() { return musicUrl; }
    public void setMusicUrl(String musicUrl) { this.musicUrl = musicUrl; }

    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

    public String getLyricUrl() { return lyricUrl; }
    public void setLyricUrl(String lyricUrl) { this.lyricUrl = lyricUrl; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { this.isLiked = liked; }

    public long getPlayCount() { return playCount; }
    public void setPlayCount(long playCount) { this.playCount = playCount; }

    public long getAddTime() { return addTime; }
    public void setAddTime(long addTime) { this.addTime = addTime; }

    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    // ==================== 实用方法 ====================

    /**
     * 增加播放次数
     */
    public void incrementPlayCount() {
        this.playCount++;
        debugLog("Play count incremented to: " + playCount + " for: " + musicName);
    }

    /**
     * 切换收藏状态
     * @return 返回新的收藏状态
     */
    public boolean toggleLike() {
        this.isLiked = !this.isLiked;
        debugLog("Like status toggled to: " + isLiked + " for: " + musicName);
        return this.isLiked;
    }

    /**
     * 获取格式化的时长字符串
     * @return 如："03:45"
     */
    public String getFormattedDuration() {
        if (duration <= 0) return "--:--";

        long totalSeconds = duration / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 获取显示名称（歌曲名 - 艺术家）
     */
    public String getDisplayName() {
        if (TextUtils.isEmpty(musicName)) return "未知歌曲";
        if (TextUtils.isEmpty(author)) return musicName;
        return musicName + " - " + author;
    }

    /**
     * 获取格式化的文件大小
     */
    public String getFormattedFileSize() {
        if (fileSize <= 0) return "未知大小";

        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    // ==================== 验证方法 ====================

    /**
     * 验证基础信息是否完整
     */
    public boolean isValid() {
        boolean valid = id > 0 &&
                !TextUtils.isEmpty(musicName) &&
                !TextUtils.isEmpty(author) &&
                !TextUtils.isEmpty(musicUrl);

        if (DEBUG && !valid) {
            Log.w(TAG, "Invalid MusicInfo: " + getValidationErrors());
        }

        return valid;
    }

    /**
     * 检查是否有音乐URL
     */
    public boolean hasMusicUrl() {
        return !TextUtils.isEmpty(musicUrl);
    }

    /**
     * 检查是否有封面URL
     */
    public boolean hasCoverUrl() {
        return !TextUtils.isEmpty(coverUrl);
    }

    /**
     * 检查是否有歌词URL
     */
    public boolean hasLyricsUrl() {
        return !TextUtils.isEmpty(lyricUrl);
    }

    /**
     * 获取验证错误信息
     */
    public String getValidationErrors() {
        StringBuilder errors = new StringBuilder();

        if (id <= 0) errors.append("Invalid ID; ");
        if (TextUtils.isEmpty(musicName)) errors.append("Missing music name; ");
        if (TextUtils.isEmpty(author)) errors.append("Missing author; ");
        if (TextUtils.isEmpty(musicUrl)) errors.append("Missing music URL; ");

        return errors.length() > 0 ? errors.toString() : "No errors";
    }

    // ==================== Object 重写方法 ====================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MusicInfo that = (MusicInfo) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "MusicInfo{" +
                "id=" + id +
                ", musicName='" + musicName + '\'' +
                ", author='" + author + '\'' +
                ", album='" + album + '\'' +
                ", duration=" + duration +
                ", isLiked=" + isLiked +
                ", playCount=" + playCount +
                ", quality='" + quality + '\'' +
                '}';
    }

    /**
     * 获取简化的描述信息
     */
    public String toSimpleString() {
        return String.format("🎵 [%d] %s", id, getDisplayName());
    }

    // ==================== 调试工具方法 ====================

    /**
     * 打印详细信息（仅在DEBUG模式下）
     */
    public void printDetailedInfo() {
        if (!DEBUG) return;

        Log.i(TAG, "=== MusicInfo Details ===");
        Log.i(TAG, "ID: " + id);
        Log.i(TAG, "Name: " + musicName);
        Log.i(TAG, "Author: " + author);
        Log.i(TAG, "Album: " + album);
        Log.i(TAG, "Duration: " + getFormattedDuration());
        Log.i(TAG, "Liked: " + isLiked);
        Log.i(TAG, "Play Count: " + playCount);
        Log.i(TAG, "Quality: " + quality);
        Log.i(TAG, "File Size: " + getFormattedFileSize());
        Log.i(TAG, "URLs - Music: " + hasMusicUrl() + ", Cover: " + hasCoverUrl() + ", Lyrics: " + hasLyricsUrl());
        Log.i(TAG, "========================");
    }

    /**
     * 条件日志输出
     */
    private void debugLog(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }

    // ==================== 静态工具方法 ====================

    /**
     * 创建一个测试用的MusicInfo对象
     */
    public static MusicInfo createTestMusic(long id, String name, String author) {
        MusicInfo music = new MusicInfo(id, name, author, "test_url");
        music.setDuration(180000); // 3分钟
        music.setQuality("320k");
        return music;
    }

    /**
     * 从另一个MusicInfo复制基础信息
     */
    public static MusicInfo copyFrom(MusicInfo source) {
        if (source == null) return null;

        MusicInfo copy = new MusicInfo();
        copy.id = source.id;
        copy.musicName = source.musicName;
        copy.author = source.author;
        copy.album = source.album;
        copy.duration = source.duration;
        copy.musicUrl = source.musicUrl;
        copy.coverUrl = source.coverUrl;
        copy.lyricUrl = source.lyricUrl;
        copy.isLiked = source.isLiked;
        copy.playCount = source.playCount;
        copy.quality = source.quality;
        copy.fileSize = source.fileSize;
        copy.addTime = System.currentTimeMillis(); // 使用当前时间作为复制时间

        return copy;
    }
}
