package com.qzz.musiccommunity.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.qzz.musiccommunity.database.dto.MusicInfo;

import java.util.ArrayList;
import java.util.List;

public class MusicDao {

    private static final String TAG = "MusicDao";
    private static MusicDao instance;
    private DatabaseHelper dbHelper;

    // 私有构造函数，使用单例模式
    private MusicDao(Context context) {
        // 使用 DatabaseHelper 的单例实例
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * 获取 MusicDao 的单例实例
     */
    public static synchronized MusicDao getInstance(Context context) {
        if (instance == null) {
            instance = new MusicDao(context);
        }
        return instance;
    }

    /**
     * 插入或更新音乐信息
     * @param musicInfo 音乐信息对象
     * @return 音乐在数据库中的ID
     */
    public long insertOrUpdateMusicInfo(MusicInfo musicInfo) {
        if (musicInfo == null || musicInfo.getMusicUrl() == null) {
            Log.e(TAG, "音乐信息不能为空");
            return -1;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        long musicId = -1;

        try {
            // 使用安全获取可写数据库的方法
            db = dbHelper.getWritableDatabaseSafe();
            db.beginTransaction();

            // 检查音乐是否已存在
            cursor = db.query(
                    DatabaseHelper.TABLE_MUSIC_INFO,
                    new String[]{DatabaseHelper.COLUMN_MUSIC_ID},
                    DatabaseHelper.COLUMN_MUSIC_URL + " = ?",
                    new String[]{musicInfo.getMusicUrl()},
                    null, null, null
            );

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_MUSIC_NAME, musicInfo.getMusicName());
            values.put(DatabaseHelper.COLUMN_AUTHOR, musicInfo.getAuthor());
            values.put(DatabaseHelper.COLUMN_MUSIC_URL, musicInfo.getMusicUrl());
            values.put(DatabaseHelper.COLUMN_COVER_URL, musicInfo.getCoverUrl());
            values.put(DatabaseHelper.COLUMN_LYRIC_URL, musicInfo.getLyricUrl());

            if (cursor != null && cursor.moveToFirst()) {
                // 音乐已存在，获取其ID并更新
                musicId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_ID));
                db.update(
                        DatabaseHelper.TABLE_MUSIC_INFO,
                        values,
                        DatabaseHelper.COLUMN_MUSIC_ID + " = ?",
                        new String[]{String.valueOf(musicId)}
                );
                Log.d(TAG, "音乐已存在，ID: " + musicId + ", 更新: " + musicInfo.getMusicName());
            } else {
                // 音乐不存在，插入新音乐
                musicId = db.insert(DatabaseHelper.TABLE_MUSIC_INFO, null, values);
                Log.d(TAG, "插入新音乐，ID: " + musicId + ", 名称: " + musicInfo.getMusicName());
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "插入或更新音乐信息时出错", e);
            musicId = -1;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                try {
                    db.endTransaction();
                } catch (Exception e) {
                    Log.e(TAG, "结束事务时出错", e);
                }
            }
        }
        return musicId;
    }

    /**
     * 根据ID获取音乐信息
     * @param musicId 音乐ID
     * @return MusicInfo对象，如果不存在则返回null
     */
    public MusicInfo getMusicInfoById(long musicId) {
        if (musicId <= 0) {
            Log.w(TAG, "无效的音乐ID: " + musicId);
            return null;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        MusicInfo musicInfo = null;

        try {
            db = dbHelper.getReadableDatabaseSafe();

            // 联合查询，同时获取收藏状态
            String query = "SELECT m.*, COALESCE(l." + DatabaseHelper.COLUMN_IS_LIKED + ", 0) as is_liked" +
                    " FROM " + DatabaseHelper.TABLE_MUSIC_INFO + " m" +
                    " LEFT JOIN " + DatabaseHelper.TABLE_LIKED_MUSIC + " l" +
                    " ON m." + DatabaseHelper.COLUMN_MUSIC_ID + " = l." + DatabaseHelper.COLUMN_LIKED_MUSIC_ID +
                    " WHERE m." + DatabaseHelper.COLUMN_MUSIC_ID + " = ?";

            cursor = db.rawQuery(query, new String[]{String.valueOf(musicId)});

            if (cursor != null && cursor.moveToFirst()) {
                musicInfo = cursorToMusicInfo(cursor);
            }

        } catch (Exception e) {
            Log.e(TAG, "根据ID获取音乐信息时出错", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return musicInfo;
    }

    /**
     * 收藏/取消收藏音乐
     * @param musicId 音乐ID
     * @param isLiked 是否收藏 (true: 收藏, false: 取消收藏)
     */
    public void setMusicLikedStatus(long musicId, boolean isLiked) {
        if (musicId <= 0) {
            Log.w(TAG, "无效的音乐ID: " + musicId);
            return;
        }

        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabaseSafe();
            db.beginTransaction();

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_IS_LIKED, isLiked ? 1 : 0);
            values.put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());

            int rowsAffected = db.update(
                    DatabaseHelper.TABLE_LIKED_MUSIC,
                    values,
                    DatabaseHelper.COLUMN_LIKED_MUSIC_ID + " = ?",
                    new String[]{String.valueOf(musicId)}
            );

            if (rowsAffected == 0) {
                // 如果没有更新，说明是第一次设置，则插入
                values.put(DatabaseHelper.COLUMN_LIKED_MUSIC_ID, musicId);
                db.insert(DatabaseHelper.TABLE_LIKED_MUSIC, null, values);
                Log.d(TAG, "插入收藏状态: musicId=" + musicId + ", isLiked=" + isLiked);
            } else {
                Log.d(TAG, "更新收藏状态: musicId=" + musicId + ", isLiked=" + isLiked);
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "设置收藏状态时出错", e);
        } finally {
            if (db != null) {
                try {
                    db.endTransaction();
                } catch (Exception e) {
                    Log.e(TAG, "结束事务时出错", e);
                }
            }
        }
    }

    /**
     * 获取音乐的收藏状态
     * @param musicId 音乐ID
     * @return true: 已收藏, false: 未收藏
     */
    public boolean getMusicLikedStatus(long musicId) {
        if (musicId <= 0) {
            return false;
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        boolean isLiked = false;

        try {
            db = dbHelper.getReadableDatabaseSafe();
            cursor = db.query(
                    DatabaseHelper.TABLE_LIKED_MUSIC,
                    new String[]{DatabaseHelper.COLUMN_IS_LIKED},
                    DatabaseHelper.COLUMN_LIKED_MUSIC_ID + " = ?",
                    new String[]{String.valueOf(musicId)},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                isLiked = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_LIKED)) == 1;
            }

        } catch (Exception e) {
            Log.e(TAG, "获取收藏状态时出错", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return isLiked;
    }

    /**
     * 保存播放列表
     * @param playlist 音乐信息列表
     */
    public void savePlaylist(List<MusicInfo> playlist) {
        if (playlist == null) {
            Log.w(TAG, "播放列表不能为空");
            return;
        }

        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabaseSafe();
            db.beginTransaction();

            // 清空旧的播放列表
            db.delete(DatabaseHelper.TABLE_PLAYLIST, null, null);
            Log.d(TAG, "旧播放列表已清空");

            // 插入新的播放列表
            for (int i = 0; i < playlist.size(); i++) {
                MusicInfo musicInfo = playlist.get(i);
                if (musicInfo == null) continue;

                long musicId = insertOrUpdateMusicInfo(musicInfo); // 确保音乐信息已存在并获取ID
                if (musicId == -1) continue;

                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID, musicId);
                values.put(DatabaseHelper.COLUMN_SEQUENCE_NUM, i);
                db.insert(DatabaseHelper.TABLE_PLAYLIST, null, values);
            }

            db.setTransactionSuccessful();
            Log.d(TAG, "播放列表保存成功，共 " + playlist.size() + " 首歌曲");

        } catch (Exception e) {
            Log.e(TAG, "保存播放列表时出错", e);
        } finally {
            if (db != null) {
                try {
                    db.endTransaction();
                } catch (Exception e) {
                    Log.e(TAG, "结束事务时出错", e);
                }
            }
        }
    }

    /**
     * 加载播放列表
     * @return 音乐信息列表
     */
    public List<MusicInfo> loadPlaylist() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<MusicInfo> playlist = new ArrayList<>();

        try {
            db = dbHelper.getReadableDatabaseSafe();

            // 优化的查询，联合三个表获取完整信息
            String query = "SELECT m.*, COALESCE(l." + DatabaseHelper.COLUMN_IS_LIKED + ", 0) as is_liked" +
                    " FROM " + DatabaseHelper.TABLE_MUSIC_INFO + " m" +
                    " INNER JOIN " + DatabaseHelper.TABLE_PLAYLIST + " p" +
                    " ON m." + DatabaseHelper.COLUMN_MUSIC_ID + " = p." + DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID +
                    " LEFT JOIN " + DatabaseHelper.TABLE_LIKED_MUSIC + " l" +
                    " ON m." + DatabaseHelper.COLUMN_MUSIC_ID + " = l." + DatabaseHelper.COLUMN_LIKED_MUSIC_ID +
                    " ORDER BY p." + DatabaseHelper.COLUMN_SEQUENCE_NUM + " ASC";

            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        MusicInfo musicInfo = cursorToMusicInfo(cursor);
                        playlist.add(musicInfo);
                    } catch (Exception e) {
                        Log.w(TAG, "解析播放列表项时出错，跳过", e);
                    }
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "播放列表加载成功，共 " + playlist.size() + " 首歌曲");

        } catch (Exception e) {
            Log.e(TAG, "加载播放列表时出错", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return playlist;
    }

    /**
     * 获取收藏的音乐列表
     * @return 收藏的音乐列表
     */
    public List<MusicInfo> getLikedMusicList() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<MusicInfo> likedMusicList = new ArrayList<>();

        try {
            db = dbHelper.getReadableDatabaseSafe();

            // 查询收藏的音乐，按时间倒序排列
            String query = "SELECT m.*, l." + DatabaseHelper.COLUMN_IS_LIKED + " as is_liked" +
                    " FROM " + DatabaseHelper.TABLE_MUSIC_INFO + " m" +
                    " INNER JOIN " + DatabaseHelper.TABLE_LIKED_MUSIC + " l" +
                    " ON m." + DatabaseHelper.COLUMN_MUSIC_ID + " = l." + DatabaseHelper.COLUMN_LIKED_MUSIC_ID +
                    " WHERE l." + DatabaseHelper.COLUMN_IS_LIKED + " = 1" +
                    " ORDER BY l." + DatabaseHelper.COLUMN_TIMESTAMP + " DESC";

            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        MusicInfo musicInfo = cursorToMusicInfo(cursor);
                        likedMusicList.add(musicInfo);
                    } catch (Exception e) {
                        Log.w(TAG, "解析收藏音乐项时出错，跳过", e);
                    }
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "收藏音乐列表加载成功，共 " + likedMusicList.size() + " 首歌曲");

        } catch (Exception e) {
            Log.e(TAG, "加载收藏音乐列表时出错", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return likedMusicList;
    }

    /**
     * 根据关键词搜索音乐
     * @param keyword 搜索关键词
     * @return 搜索结果列表
     */
    public List<MusicInfo> searchMusic(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<MusicInfo> searchResults = new ArrayList<>();
        String searchKeyword = "%" + keyword.trim() + "%";

        try {
            db = dbHelper.getReadableDatabaseSafe();

            // 搜索音乐名称或作者包含关键词的音乐
            String query = "SELECT m.*, COALESCE(l." + DatabaseHelper.COLUMN_IS_LIKED + ", 0) as is_liked" +
                    " FROM " + DatabaseHelper.TABLE_MUSIC_INFO + " m" +
                    " LEFT JOIN " + DatabaseHelper.TABLE_LIKED_MUSIC + " l" +
                    " ON m." + DatabaseHelper.COLUMN_MUSIC_ID + " = l." + DatabaseHelper.COLUMN_LIKED_MUSIC_ID +
                    " WHERE m." + DatabaseHelper.COLUMN_MUSIC_NAME + " LIKE ? OR m." + DatabaseHelper.COLUMN_AUTHOR + " LIKE ?" +
                    " ORDER BY m." + DatabaseHelper.COLUMN_MUSIC_NAME + " ASC";

            cursor = db.rawQuery(query, new String[]{searchKeyword, searchKeyword});

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        MusicInfo musicInfo = cursorToMusicInfo(cursor);
                        searchResults.add(musicInfo);
                    } catch (Exception e) {
                        Log.w(TAG, "解析搜索结果项时出错，跳过", e);
                    }
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "搜索完成，关键词: " + keyword + ", 结果数: " + searchResults.size());

        } catch (Exception e) {
            Log.e(TAG, "搜索音乐时出错", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return searchResults;
    }

    /**
     * 删除音乐信息（同时删除相关的收藏和播放列表记录）
     * @param musicId 音乐ID
     * @return 是否删除成功
     */
    public boolean deleteMusicInfo(long musicId) {
        if (musicId <= 0) {
            Log.w(TAG, "无效的音乐ID: " + musicId);
            return false;
        }

        SQLiteDatabase db = null;
        boolean success = false;

        try {
            db = dbHelper.getWritableDatabaseSafe();
            db.beginTransaction();

            // 删除收藏记录
            db.delete(DatabaseHelper.TABLE_LIKED_MUSIC,
                    DatabaseHelper.COLUMN_LIKED_MUSIC_ID + " = ?",
                    new String[]{String.valueOf(musicId)});

            // 删除播放列表记录
            db.delete(DatabaseHelper.TABLE_PLAYLIST,
                    DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID + " = ?",
                    new String[]{String.valueOf(musicId)});

            // 删除音乐信息
            int deletedRows = db.delete(DatabaseHelper.TABLE_MUSIC_INFO,
                    DatabaseHelper.COLUMN_MUSIC_ID + " = ?",
                    new String[]{String.valueOf(musicId)});

            success = deletedRows > 0;
            db.setTransactionSuccessful();

            Log.d(TAG, "删除音乐信息，ID: " + musicId + ", 成功: " + success);

        } catch (Exception e) {
            Log.e(TAG, "删除音乐信息时出错", e);
            success = false;
        } finally {
            if (db != null) {
                try {
                    db.endTransaction();
                } catch (Exception e) {
                    Log.e(TAG, "结束事务时出错", e);
                }
            }
        }
        return success;
    }

    /**
     * 获取所有音乐信息
     * @return 所有音乐信息列表
     */
    public List<MusicInfo> getAllMusicInfo() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        List<MusicInfo> allMusicList = new ArrayList<>();

        try {
            db = dbHelper.getReadableDatabaseSafe();

            // 查询所有音乐，同时获取收藏状态
            String query = "SELECT m.*, COALESCE(l." + DatabaseHelper.COLUMN_IS_LIKED + ", 0) as is_liked" +
                    " FROM " + DatabaseHelper.TABLE_MUSIC_INFO + " m" +
                    " LEFT JOIN " + DatabaseHelper.TABLE_LIKED_MUSIC + " l" +
                    " ON m." + DatabaseHelper.COLUMN_MUSIC_ID + " = l." + DatabaseHelper.COLUMN_LIKED_MUSIC_ID +
                    " ORDER BY m." + DatabaseHelper.COLUMN_MUSIC_ID + " DESC";

            cursor = db.rawQuery(query, null);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    try {
                        MusicInfo musicInfo = cursorToMusicInfo(cursor);
                        allMusicList.add(musicInfo);
                    } catch (Exception e) {
                        Log.w(TAG, "解析音乐信息项时出错，跳过", e);
                    }
                } while (cursor.moveToNext());
            }

            Log.d(TAG, "获取所有音乐信息成功，共 " + allMusicList.size() + " 首歌曲");

        } catch (Exception e) {
            Log.e(TAG, "获取所有音乐信息时出错", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return allMusicList;
    }

    /**
     * 从Cursor中解析MusicInfo对象
     */
    private MusicInfo cursorToMusicInfo(Cursor cursor) {
        MusicInfo musicInfo = new MusicInfo();

        try {
            musicInfo.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_ID)));
            musicInfo.setMusicName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_NAME)));
            musicInfo.setAuthor(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AUTHOR)));
            musicInfo.setMusicUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_URL)));
            musicInfo.setCoverUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COVER_URL)));
            musicInfo.setLyricUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LYRIC_URL)));

            // 尝试获取收藏状态
            int isLikedIndex = cursor.getColumnIndex("is_liked");
            if (isLikedIndex != -1) {
                boolean isLiked = cursor.getInt(isLikedIndex) == 1;
                musicInfo.setLiked(isLiked);
            }
        } catch (Exception e) {
            Log.e(TAG, "解析音乐信息时出错", e);
        }

        return musicInfo;
    }

    /**
     * 清理无效数据（清理孤立的收藏和播放列表记录）
     */
    public void cleanUpInvalidData() {
        SQLiteDatabase db = null;

        try {
            db = dbHelper.getWritableDatabaseSafe();
            db.beginTransaction();

            // 清理孤立的收藏记录
            String cleanLikedSql = "DELETE FROM " + DatabaseHelper.TABLE_LIKED_MUSIC +
                    " WHERE " + DatabaseHelper.COLUMN_LIKED_MUSIC_ID +
                    " NOT IN (SELECT " + DatabaseHelper.COLUMN_MUSIC_ID +
                    " FROM " + DatabaseHelper.TABLE_MUSIC_INFO + ")";
            db.execSQL(cleanLikedSql);

            // 清理孤立的播放列表记录
            String cleanPlaylistSql = "DELETE FROM " + DatabaseHelper.TABLE_PLAYLIST +
                    " WHERE " + DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID +
                    " NOT IN (SELECT " + DatabaseHelper.COLUMN_MUSIC_ID +
                    " FROM " + DatabaseHelper.TABLE_MUSIC_INFO + ")";
            db.execSQL(cleanPlaylistSql);

            db.setTransactionSuccessful();
            Log.d(TAG, "清理无效数据完成");

        } catch (Exception e) {
            Log.e(TAG, "清理无效数据时出错", e);
        } finally {
            if (db != null) {
                try {
                    db.endTransaction();
                } catch (Exception e) {
                    Log.e(TAG, "结束事务时出错", e);
                }
            }
        }
    }
}
