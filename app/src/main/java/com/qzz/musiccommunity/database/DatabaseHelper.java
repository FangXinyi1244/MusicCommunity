package com.qzz.musiccommunity.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";

    private static final String DATABASE_NAME = "music_player.db";
    private static final int DATABASE_VERSION = 2; // 增加版本号以支持升级

    // music_info 表
    public static final String TABLE_MUSIC_INFO = "music_info";
    public static final String COLUMN_MUSIC_ID = "id";
    public static final String COLUMN_MUSIC_NAME = "music_name";
    public static final String COLUMN_AUTHOR = "author";
    public static final String COLUMN_MUSIC_URL = "music_url";
    public static final String COLUMN_COVER_URL = "cover_url";
    public static final String COLUMN_LYRIC_URL = "lyric_url";
    public static final String COLUMN_DURATION = "duration"; // 新增：歌曲时长
    public static final String COLUMN_FILE_SIZE = "file_size"; // 新增：文件大小
    public static final String COLUMN_CREATED_TIME = "created_time"; // 新增：创建时间

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
    public static final String COLUMN_ADDED_TIME = "added_time"; // 新增：添加时间

    // 创建 music_info 表的 SQL 语句 - 增强版
    private static final String CREATE_TABLE_MUSIC_INFO = "CREATE TABLE IF NOT EXISTS " +
            TABLE_MUSIC_INFO + " (" +
            COLUMN_MUSIC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_MUSIC_NAME + " TEXT NOT NULL, " +
            COLUMN_AUTHOR + " TEXT NOT NULL, " +
            COLUMN_MUSIC_URL + " TEXT NOT NULL UNIQUE, " +
            COLUMN_COVER_URL + " TEXT, " +
            COLUMN_LYRIC_URL + " TEXT, " +
            COLUMN_DURATION + " INTEGER DEFAULT 0, " +
            COLUMN_FILE_SIZE + " INTEGER DEFAULT 0, " +
            COLUMN_CREATED_TIME + " INTEGER DEFAULT (strftime('%s','now')));";

    // 创建 liked_music 表的 SQL 语句 - 增强版
    private static final String CREATE_TABLE_LIKED_MUSIC = "CREATE TABLE IF NOT EXISTS " +
            TABLE_LIKED_MUSIC + " (" +
            COLUMN_LIKED_MUSIC_ID + " INTEGER PRIMARY KEY, " +
            COLUMN_IS_LIKED + " INTEGER NOT NULL DEFAULT 0 CHECK (" + COLUMN_IS_LIKED + " IN (0, 1)), " +
            COLUMN_TIMESTAMP + " INTEGER NOT NULL DEFAULT (strftime('%s','now')), " +
            "FOREIGN KEY (" + COLUMN_LIKED_MUSIC_ID + ") REFERENCES " +
            TABLE_MUSIC_INFO + " (" + COLUMN_MUSIC_ID + ") ON DELETE CASCADE);";

    // 创建 playlist 表的 SQL 语句 - 增强版
    private static final String CREATE_TABLE_PLAYLIST = "CREATE TABLE IF NOT EXISTS " +
            TABLE_PLAYLIST + " (" +
            COLUMN_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PLAYLIST_MUSIC_ID + " INTEGER NOT NULL, " +
            COLUMN_SEQUENCE_NUM + " INTEGER NOT NULL, " +
            COLUMN_ADDED_TIME + " INTEGER DEFAULT (strftime('%s','now')), " +
            "UNIQUE (" + COLUMN_PLAYLIST_MUSIC_ID + ", " + COLUMN_SEQUENCE_NUM + "), " +
            "FOREIGN KEY (" + COLUMN_PLAYLIST_MUSIC_ID + ") REFERENCES " +
            TABLE_MUSIC_INFO + " (" + COLUMN_MUSIC_ID + ") ON DELETE CASCADE);";

    // 索引创建语句
    private static final String[] CREATE_INDEXES = {
            "CREATE INDEX IF NOT EXISTS idx_music_name ON " + TABLE_MUSIC_INFO + " (" + COLUMN_MUSIC_NAME + ");",
            "CREATE INDEX IF NOT EXISTS idx_author ON " + TABLE_MUSIC_INFO + " (" + COLUMN_AUTHOR + ");",
            "CREATE INDEX IF NOT EXISTS idx_music_url ON " + TABLE_MUSIC_INFO + " (" + COLUMN_MUSIC_URL + ");",
            "CREATE INDEX IF NOT EXISTS idx_liked_timestamp ON " + TABLE_LIKED_MUSIC + " (" + COLUMN_TIMESTAMP + ");",
            "CREATE INDEX IF NOT EXISTS idx_playlist_sequence ON " + TABLE_PLAYLIST + " (" + COLUMN_SEQUENCE_NUM + ");"
    };

    // 所有表名列表
    private static final List<String> ALL_TABLES = Arrays.asList(
            TABLE_MUSIC_INFO,
            TABLE_LIKED_MUSIC,
            TABLE_PLAYLIST
    );

    private static DatabaseHelper instance;
    private final Context context;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context.getApplicationContext();
    }

    /**
     * 单例模式获取实例
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "开始创建数据库，版本: " + DATABASE_VERSION);

        try {
            // 开启事务
            db.beginTransaction();

            // 启用外键约束
            db.execSQL("PRAGMA foreign_keys=ON;");

            // 创建表
            createTables(db);

            // 创建索引
            createIndexes(db);

            // 验证表结构
            validateTables(db);

            // 提交事务
            db.setTransactionSuccessful();
            Log.i(TAG, "数据库创建成功");

        } catch (Exception e) {
            Log.e(TAG, "数据库创建失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        } finally {
            try {
                db.endTransaction();
            } catch (Exception e) {
                Log.e(TAG, "结束事务时出错", e);
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "升级数据库从版本 " + oldVersion + " 到 " + newVersion);

        try {
            db.beginTransaction();

            if (oldVersion < 2) {
                // 从版本1升级到版本2
                upgradeToVersion2(db);
            }

            // 验证升级后的表结构
            validateTables(db);
            createIndexes(db);

            db.setTransactionSuccessful();
            Log.i(TAG, "数据库升级成功");

        } catch (Exception e) {
            Log.e(TAG, "数据库升级失败，进行重建", e);
            // 升级失败，删除所有表重新创建
            recreateDatabase(db);
        } finally {
            try {
                db.endTransaction();
            } catch (Exception e) {
                Log.e(TAG, "结束升级事务时出错", e);
            }
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // 每次打开数据库时启用外键约束
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }

        // 检查表的完整性
        checkAndRepairTables(db);
    }

    /**
     * 创建所有表
     */
    private void createTables(SQLiteDatabase db) {
        Log.d(TAG, "开始创建表");
        db.execSQL(CREATE_TABLE_MUSIC_INFO);
        Log.d(TAG, "创建表: " + TABLE_MUSIC_INFO);

        db.execSQL(CREATE_TABLE_LIKED_MUSIC);
        Log.d(TAG, "创建表: " + TABLE_LIKED_MUSIC);

        db.execSQL(CREATE_TABLE_PLAYLIST);
        Log.d(TAG, "创建表: " + TABLE_PLAYLIST);
    }

    /**
     * 创建索引
     */
    private void createIndexes(SQLiteDatabase db) {
        Log.d(TAG, "开始创建索引");
        for (String indexSql : CREATE_INDEXES) {
            try {
                db.execSQL(indexSql);
            } catch (Exception e) {
                Log.w(TAG, "创建索引失败: " + indexSql, e);
            }
        }
    }

    /**
     * 验证表结构
     */
    private void validateTables(SQLiteDatabase db) {
        for (String tableName : ALL_TABLES) {
            if (!isTableExists(db, tableName)) {
                throw new RuntimeException("表 " + tableName + " 不存在");
            }
        }
        Log.d(TAG, "所有表验证通过");
    }

    /**
     * 检查表是否存在
     */
    private boolean isTableExists(SQLiteDatabase db, String tableName) {
        try (Cursor cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{tableName})) {
            return cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "检查表存在性时出错: " + tableName, e);
            return false;
        }
    }

    /**
     * 检查并修复表
     */
    private void checkAndRepairTables(SQLiteDatabase db) {
        try {
            List<String> missingTables = new ArrayList<>();

            for (String tableName : ALL_TABLES) {
                if (!isTableExists(db, tableName)) {
                    missingTables.add(tableName);
                    Log.w(TAG, "发现缺失的表: " + tableName);
                }
            }

            if (!missingTables.isEmpty()) {
                Log.i(TAG, "开始修复缺失的表");
                db.beginTransaction();
                try {
                    // 重新创建缺失的表
                    for (String tableName : missingTables) {
                        recreateTable(db, tableName);
                    }
                    createIndexes(db);
                    db.setTransactionSuccessful();
                    Log.i(TAG, "表修复完成");
                } finally {
                    db.endTransaction();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "表修复过程中出错", e);
        }
    }

    /**
     * 重新创建指定表
     */
    private void recreateTable(SQLiteDatabase db, String tableName) {
        switch (tableName) {
            case TABLE_MUSIC_INFO:
                db.execSQL(CREATE_TABLE_MUSIC_INFO);
                break;
            case TABLE_LIKED_MUSIC:
                db.execSQL(CREATE_TABLE_LIKED_MUSIC);
                break;
            case TABLE_PLAYLIST:
                db.execSQL(CREATE_TABLE_PLAYLIST);
                break;
        }
        Log.d(TAG, "重新创建表: " + tableName);
    }

    /**
     * 升级到版本2
     */
    private void upgradeToVersion2(SQLiteDatabase db) {
        Log.d(TAG, "执行版本2升级");

        // 为music_info表添加新列
        try {
            db.execSQL("ALTER TABLE " + TABLE_MUSIC_INFO +
                    " ADD COLUMN " + COLUMN_DURATION + " INTEGER DEFAULT 0;");
        } catch (SQLiteException e) {
            Log.w(TAG, "添加duration列失败，可能已存在", e);
        }

        try {
            db.execSQL("ALTER TABLE " + TABLE_MUSIC_INFO +
                    " ADD COLUMN " + COLUMN_FILE_SIZE + " INTEGER DEFAULT 0;");
        } catch (SQLiteException e) {
            Log.w(TAG, "添加file_size列失败，可能已存在", e);
        }

        try {
            db.execSQL("ALTER TABLE " + TABLE_MUSIC_INFO +
                    " ADD COLUMN " + COLUMN_CREATED_TIME + " INTEGER DEFAULT (strftime('%s','now'));");
        } catch (SQLiteException e) {
            Log.w(TAG, "添加created_time列失败，可能已存在", e);
        }

        // 为playlist表添加新列
        try {
            db.execSQL("ALTER TABLE " + TABLE_PLAYLIST +
                    " ADD COLUMN " + COLUMN_ADDED_TIME + " INTEGER DEFAULT (strftime('%s','now'));");
        } catch (SQLiteException e) {
            Log.w(TAG, "添加added_time列失败，可能已存在", e);
        }
    }

    /**
     * 重建整个数据库
     */
    private void recreateDatabase(SQLiteDatabase db) {
        Log.w(TAG, "重建数据库");
        try {
            db.beginTransaction();

            // 删除所有表
            for (String tableName : ALL_TABLES) {
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
            }

            // 重新创建
            createTables(db);
            createIndexes(db);

            db.setTransactionSuccessful();
            Log.i(TAG, "数据库重建完成");
        } finally {
            db.endTransaction();
        }
    }

    /**
     * 获取数据库实例并确保表存在
     */
    public SQLiteDatabase getWritableDatabaseSafe() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            // 确保表存在
            checkAndRepairTables(db);
            return db;
        } catch (Exception e) {
            Log.e(TAG, "获取可写数据库时出错", e);
            throw new RuntimeException("数据库访问失败", e);
        }
    }

    /**
     * 获取只读数据库实例
     */
    public SQLiteDatabase getReadableDatabaseSafe() {
        try {
            return this.getReadableDatabase();
        } catch (Exception e) {
            Log.e(TAG, "获取只读数据库时出错", e);
            throw new RuntimeException("数据库访问失败", e);
        }
    }

    /**
     * 数据库完整性检查
     */
    public boolean checkDatabaseIntegrity() {
        try (SQLiteDatabase db = getReadableDatabaseSafe()) {
            try (Cursor cursor = db.rawQuery("PRAGMA integrity_check", null)) {
                if (cursor.moveToFirst()) {
                    String result = cursor.getString(0);
                    boolean isOk = "ok".equals(result);
                    Log.d(TAG, "数据库完整性检查结果: " + result);
                    return isOk;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "数据库完整性检查失败", e);
        }
        return false;
    }

    /**
     * 获取数据库信息
     */
    public String getDatabaseInfo() {
        StringBuilder info = new StringBuilder();
        try (SQLiteDatabase db = getReadableDatabaseSafe()) {
            info.append("数据库名称: ").append(DATABASE_NAME).append("\n");
            info.append("数据库版本: ").append(db.getVersion()).append("\n");
            info.append("数据库路径: ").append(db.getPath()).append("\n");

            for (String tableName : ALL_TABLES) {
                try (Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null)) {
                    if (cursor.moveToFirst()) {
                        info.append("表 ").append(tableName).append(" 记录数: ").append(cursor.getInt(0)).append("\n");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取数据库信息失败", e);
            info.append("获取信息失败: ").append(e.getMessage());
        }
        return info.toString();
    }
}
