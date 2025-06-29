package com.qzz.musiccommunity.network.dto;

import android.os.Parcel;
import android.os.Parcelable;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class MusicInfo implements Parcelable {
    private static final String TAG = "MusicInfo";

    private int id;
    private String musicName;
    private String author;
    private String coverUrl;
    private String musicUrl;
    private String lyricUrl;

    // 默认构造函数
    public MusicInfo() {
        Log.d(TAG, "MusicInfo created with default constructor");
    }

    // 从Parcel创建对象的构造函数
    protected MusicInfo(Parcel in) {
        id = in.readInt();
        musicName = in.readString();
        author = in.readString();
        coverUrl = in.readString();
        musicUrl = in.readString();
        lyricUrl = in.readString();

        Log.d(TAG, "MusicInfo created from Parcel: " + toString());
    }

    // 创建CREATOR静态字段
    public static final Creator<MusicInfo> CREATOR = new Creator<MusicInfo>() {
        @Override
        public MusicInfo createFromParcel(Parcel in) {
            Log.d(TAG, "Creating MusicInfo from Parcel");
            return new MusicInfo(in);
        }

        @Override
        public MusicInfo[] newArray(int size) {
            Log.d(TAG, "Creating MusicInfo array with size: " + size);
            return new MusicInfo[size];
        }
    };

    // 实现Parcelable接口的方法
    @Override
    public int describeContents() {
        return 0; // 通常返回0，除非包含文件描述符
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Log.d(TAG, "Writing MusicInfo to Parcel: " + toString());
        dest.writeInt(id);
        dest.writeString(musicName);
        dest.writeString(author);
        dest.writeString(coverUrl);
        dest.writeString(musicUrl);
        dest.writeString(lyricUrl);
    }

    // 现有的getters和setters（增加日志）
    public int getId() {
        return id;
    }

    public void setId(int id) {
        Log.d(TAG, "Setting id: " + this.id + " -> " + id);
        this.id = id;
    }

    public String getMusicName() {
        return musicName;
    }

    public void setMusicName(String musicName) {
        Log.d(TAG, "Setting musicName: " + this.musicName + " -> " + musicName);
        this.musicName = musicName;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        Log.d(TAG, "Setting author: " + this.author + " -> " + author);
        this.author = author;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        Log.d(TAG, "Setting coverUrl: " + this.coverUrl + " -> " + coverUrl);
        this.coverUrl = coverUrl;
    }

    public String getMusicUrl() {
        return musicUrl;
    }

    public void setMusicUrl(String musicUrl) {
        Log.d(TAG, "Setting musicUrl: " + this.musicUrl + " -> " + musicUrl);
        this.musicUrl = musicUrl;
    }

    public String getLyricUrl() {
        return lyricUrl;
    }

    public void setLyricUrl(String lyricUrl) {
        Log.d(TAG, "Setting lyricUrl: " + this.lyricUrl + " -> " + lyricUrl);
        this.lyricUrl = lyricUrl;
    }

    // 添加toString方法用于日志输出
    @Override
    public String toString() {
        return "MusicInfo{" +
                "id=" + id +
                ", musicName='" + musicName + '\'' +
                ", author='" + author + '\'' +
                ", coverUrl='" + coverUrl + '\'' +
                ", musicUrl='" + musicUrl + '\'' +
                ", lyricUrl='" + lyricUrl + '\'' +
                '}';
    }

    // 添加便捷的日志打印方法
    public void printInfo() {
        Log.i(TAG, "=== MusicInfo Details ===");
        Log.i(TAG, "ID: " + id);
        Log.i(TAG, "Music Name: " + musicName);
        Log.i(TAG, "Author: " + author);
        Log.i(TAG, "Cover URL: " + coverUrl);
        Log.i(TAG, "Music URL: " + musicUrl);
        Log.i(TAG, "Lyric URL: " + lyricUrl);
        Log.i(TAG, "========================");
    }

    // 添加简化的日志打印方法
    public void printBasicInfo() {
        Log.i(TAG, String.format("🎵 [%d] %s - %s", id, musicName, author));
    }

    // 添加equals和hashCode方法（便于调试和比较）
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MusicInfo musicInfo = (MusicInfo) o;
        boolean isEqual = id == musicInfo.id;

        Log.d(TAG, "Comparing MusicInfo objects - Equal: " + isEqual +
                " (this.id=" + this.id + ", other.id=" + musicInfo.id + ")");

        return isEqual;
    }

    @Override
    public int hashCode() {
        int hash = Integer.hashCode(id);
        Log.v(TAG, "Generated hashCode: " + hash + " for MusicInfo id: " + id);
        return hash;
    }

    // 添加数据验证方法
    public boolean isValid() {
        boolean valid = id > 0 &&
                musicName != null && !musicName.trim().isEmpty() &&
                author != null && !author.trim().isEmpty();

        Log.d(TAG, "Validation result: " + valid + " for " + toString());

        if (!valid) {
            Log.w(TAG, "Invalid MusicInfo detected:");
            if (id <= 0) Log.w(TAG, "  - Invalid id: " + id);
            if (musicName == null || musicName.trim().isEmpty())
                Log.w(TAG, "  - Invalid musicName: " + musicName);
            if (author == null || author.trim().isEmpty())
                Log.w(TAG, "  - Invalid author: " + author);
        }

        return valid;
    }

    // 添加URL验证方法
    public boolean hasValidUrls() {
        boolean hasCover = coverUrl != null && !coverUrl.trim().isEmpty();
        boolean hasMusic = musicUrl != null && !musicUrl.trim().isEmpty();
        boolean hasLyric = lyricUrl != null && !lyricUrl.trim().isEmpty();

        Log.d(TAG, String.format("URL validation - Cover: %b, Music: %b, Lyric: %b",
                hasCover, hasMusic, hasLyric));

        return hasMusic; // 至少需要有音乐URL
    }
}

