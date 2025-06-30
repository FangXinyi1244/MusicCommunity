
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
    private DatabaseHelper dbHelper;

    public MusicDao(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * 插入或更新音乐信息
     * @param musicInfo 音乐信息对象
     * @return 音乐在数据库中的ID
     */
    public long insertOrUpdateMusicInfo(MusicInfo musicInfo) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long musicId = -1;

        // 检查音乐是否已存在
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_MUSIC_INFO,
                new String[]{DatabaseHelper.COLUMN_MUSIC_ID},
                DatabaseHelper.COLUMN_MUSIC_URL + " = ?",
                new String[]{musicInfo.getMusicUrl()},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            // 音乐已存在，获取其ID
            musicId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_ID));
            Log.d(TAG, "音乐已存在，ID: " + musicId + ", 更新: " + musicInfo.getMusicName());
            // 可以选择更新其他字段，这里简化为只返回ID
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_MUSIC_NAME, musicInfo.getMusicName());
            values.put(DatabaseHelper.COLUMN_AUTHOR, musicInfo.getAuthor());
            values.put(DatabaseHelper.COLUMN_COVER_URL, musicInfo.getCoverUrl());
            values.put(DatabaseHelper.COLUMN_LYRIC_URL, musicInfo.getLyricUrl());
            db.update(DatabaseHelper.TABLE_MUSIC_INFO, values, DatabaseHelper.COLUMN_MUSIC_ID + " = ?", new String[]{String.valueOf(musicId)});
        } else {
            // 音乐不存在，插入新音乐
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_MUSIC_NAME, musicInfo.getMusicName());
            values.put(DatabaseHelper.COLUMN_AUTHOR, musicInfo.getAuthor());
            values.put(DatabaseHelper.COLUMN_MUSIC_URL, musicInfo.getMusicUrl());
            values.put(DatabaseHelper.COLUMN_COVER_URL, musicInfo.getCoverUrl());
            values.put(DatabaseHelper.COLUMN_LYRIC_URL, musicInfo.getLyricUrl());
            musicId = db.insert(DatabaseHelper.TABLE_MUSIC_INFO, null, values);
            Log.d(TAG, "插入新音乐，ID: " + musicId + ", 名称: " + musicInfo.getMusicName());
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return musicId;
    }

    /**
     * 根据ID获取音乐信息
     * @param musicId 音乐ID
     * @return MusicInfo对象，如果不存在则返回null
     */
    public MusicInfo getMusicInfoById(long musicId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        MusicInfo musicInfo = null;

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_MUSIC_INFO,
                null, // 所有列
                DatabaseHelper.COLUMN_MUSIC_ID + " = ?",
                new String[]{String.valueOf(musicId)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            musicInfo = cursorToMusicInfo(cursor);
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return musicInfo;
    }

    /**
     * 收藏/取消收藏音乐
     * @param musicId 音乐ID
     * @param isLiked 是否收藏 (true: 收藏, false: 取消收藏)
     */
    public void setMusicLikedStatus(long musicId, boolean isLiked) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
        db.close();
    }

    /**
     * 获取音乐的收藏状态
     * @param musicId 音乐ID
     * @return true: 已收藏, false: 未收藏
     */
    public boolean getMusicLikedStatus(long musicId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        boolean isLiked = false;

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_LIKED_MUSIC,
                new String[]{DatabaseHelper.COLUMN_IS_LIKED},
                DatabaseHelper.COLUMN_LIKED_MUSIC_ID + " = ?",
                new String[]{String.valueOf(musicId)},
                null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            isLiked = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_IS_LIKED)) == 1;
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return isLiked;
    }

    /**
     * 保存播放列表
     * @param playlist 音乐信息列表
     */
    public void savePlaylist(List<MusicInfo> playlist) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 清空旧的播放列表
            db.delete(DatabaseHelper.TABLE_PLAYLIST, null, null);
            Log.d(TAG, "旧播放列表已清空");

            // 插入新的播放列表
            for (int i = 0; i < playlist.size(); i++) {
                MusicInfo musicInfo = playlist.get(i);
                long musicId = insertOrUpdateMusicInfo(musicInfo); // 确保音乐信息已存在并获取ID

                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID, musicId);
                values.put(DatabaseHelper.COLUMN_SEQUENCE_NUM, i);
                db.insert(DatabaseHelper.TABLE_PLAYLIST, null, values);
            }
            db.setTransactionSuccessful();
            Log.d(TAG, "播放列表保存成功，共 " + playlist.size() + " 首歌曲");
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * 加载播放列表
     * @return 音乐信息列表
     */
    public List<MusicInfo> loadPlaylist() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<MusicInfo> playlist = new ArrayList<>();

        String query = "SELECT T1.*, T2." + DatabaseHelper.COLUMN_IS_LIKED + " FROM " +
                DatabaseHelper.TABLE_MUSIC_INFO + " T1 INNER JOIN " +
                DatabaseHelper.TABLE_PLAYLIST + " T2 ON T1." + DatabaseHelper.COLUMN_MUSIC_ID + " = T2." + DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID +
                " ORDER BY T2." + DatabaseHelper.COLUMN_SEQUENCE_NUM + " ASC;";

        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                MusicInfo musicInfo = cursorToMusicInfo(cursor);
                // 从liked_music表中获取is_liked状态，如果存在的话
                int isLikedColumnIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_LIKED);
                if (isLikedColumnIndex != -1) {
                    musicInfo.setLiked(cursor.getInt(isLikedColumnIndex) == 1);
                }
                playlist.add(musicInfo);
            } while (cursor.moveToNext());
        }

        if (cursor != null) {
            cursor.close();
        }
        db.close();
        Log.d(TAG, "播放列表加载成功，共 " + playlist.size() + " 首歌曲");
        return playlist;
    }

    /**
     * 从Cursor中解析MusicInfo对象
     */
    private MusicInfo cursorToMusicInfo(Cursor cursor) {
        MusicInfo musicInfo = new MusicInfo();
        musicInfo.setId(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_ID)));
        musicInfo.setMusicName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_NAME)));
        musicInfo.setAuthor(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_AUTHOR)));
        musicInfo.setMusicUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MUSIC_URL)));
        musicInfo.setCoverUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COVER_URL)));
        musicInfo.setLyricUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LYRIC_URL)));
        return musicInfo;
    }

    /**
     * 从播放列表中删除指定音乐
     * @param musicId 音乐在music_info表中的ID
     */
    public void deleteMusicFromPlaylist(long musicId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            // 从playlist表中删除该音乐
            int rowsAffected = db.delete(
                    DatabaseHelper.TABLE_PLAYLIST,
                    DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID + " = ?",
                    new String[]{String.valueOf(musicId)}
            );
            if (rowsAffected > 0) {
                Log.d(TAG, "从播放列表删除音乐成功，musicId: " + musicId);
                // 重新排序sequence_num
                reorderPlaylistSequence(db);
            } else {
                Log.d(TAG, "播放列表中未找到音乐，musicId: " + musicId);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * 重新排序播放列表的sequence_num
     * 在删除歌曲后调用，确保sequence_num是连续的
     * @param db 数据库实例
     */
    private void reorderPlaylistSequence(SQLiteDatabase db) {
        // 获取当前播放列表中的所有music_id，按旧的sequence_num排序
        String query = "SELECT " + DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID + ", " + DatabaseHelper.COLUMN_SEQUENCE_NUM + " FROM " +
                DatabaseHelper.TABLE_PLAYLIST + " ORDER BY " + DatabaseHelper.COLUMN_SEQUENCE_NUM + " ASC;";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null && cursor.moveToFirst()) {
            int newSequence = 0;
            do {
                long musicId = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID));
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COLUMN_SEQUENCE_NUM, newSequence);
                db.update(
                        DatabaseHelper.TABLE_PLAYLIST,
                        values,
                        DatabaseHelper.COLUMN_PLAYLIST_MUSIC_ID + " = ?",
                        new String[]{String.valueOf(musicId)}
                );
                newSequence++;
            } while (cursor.moveToNext());
            Log.d(TAG, "播放列表sequence_num重新排序完成");
        }

        if (cursor != null) {
            cursor.close();
        }
    }
}


