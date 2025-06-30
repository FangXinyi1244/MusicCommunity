package com.qzz.musiccommunity.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "music_player.db";
    private static final int DATABASE_VERSION = 1;

    // music_info 表
    public static final String TABLE_MUSIC_INFO = "music_info";
    public static final String COLUMN_MUSIC_ID = "id";
    public static final String COLUMN_MUSIC_NAME = "music_name";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_MUSIC_URL = "music_url";
    public static final String COLUMN_COVER_URL = "cover_url";
    public static final String COLUMN_LYRIC_URL = "lyric_url";

    // liked_music 表
    public static final String TABLE_LIKED_MUSIC = "liked_music";
    public static final String COLUMN_LIKED_MUSIC_ID = "music_id";
    public static final String COLUMN_IS_LIKED = "is_liked";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // playlist 表
    public static final String TABLE_PLAYLIST = "playlist";
    public static final String COLUMN_PLAYLIST_ID = "id";
    public static final String COLUMN_PLAYLIST_MUSIC_ID = "music_id";
    public static final String COLUMN_SEQUENCE_NUM = "sequence_num";

    // 创建 music_info 表的 SQL 语句
    private static final String CREATE_TABLE_MUSIC_INFO = "CREATE TABLE " +
            TABLE_MUSIC_INFO + " (" +
            COLUMN_MUSIC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_MUSIC_NAME + " TEXT NOT NULL, " +
            COLUMN_AUTHOR + " TEXT NOT NULL, " +
            COLUMN_MUSIC_URL + " TEXT NOT NULL UNIQUE, " +
            COLUMN_COVER_URL + " TEXT, " +
            COLUMN_LYRIC_URL + " TEXT);";

    // 创建 liked_music 表的 SQL 语句
    private static final String CREATE_TABLE_LIKED_MUSIC = "CREATE TABLE " +
            TABLE_LIKED_MUSIC + " (" +
            COLUMN_LIKED_MUSIC_ID + " INTEGER PRIMARY KEY, " +
            COLUMN_IS_LIKED + " INTEGER NOT NULL DEFAULT 0, " +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL);";

    // 创建 playlist 表的 SQL 语句
    private static final String CREATE_TABLE_PLAYLIST = "CREATE TABLE " +
            TABLE_PLAYLIST + " (" +
            COLUMN_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PLAYLIST_MUSIC_ID + " INTEGER NOT NULL, " +
            COLUMN_SEQUENCE_NUM + " INTEGER NOT NULL UNIQUE);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MUSIC_INFO);
        db.execSQL(CREATE_TABLE_LIKED_MUSIC);
        db.execSQL(CREATE_TABLE_PLAYLIST);
        Log.d("DatabaseHelper", "数据库表创建成功");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w("DatabaseHelper", "Upgrading database from version " + oldVersion + " to " +
                newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIKED_MUSIC);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MUSIC_INFO);
        onCreate(db);
    }
}


