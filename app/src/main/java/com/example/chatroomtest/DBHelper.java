package com.example.chatroomtest;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "chat_history.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_MESSAGES = "messages";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_SENDER = "sender";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_IS_SENT = "is_sent";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_AVATAR_PATH = "avatar_path";
    public static final String COLUMN_IS_SYSTEM = "is_system";

    private static final String CREATE_TABLE_MESSAGES =
            "CREATE TABLE " + TABLE_MESSAGES + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_SENDER + " TEXT NOT NULL, "
                    + COLUMN_CONTENT + " TEXT NOT NULL, "
                    + COLUMN_IS_SENT + " INTEGER DEFAULT 0, "
                    + COLUMN_TIMESTAMP + " INTEGER NOT NULL, "
                    + COLUMN_AVATAR_PATH + " TEXT, "
                    + COLUMN_IS_SYSTEM + " INTEGER DEFAULT 0"
                    + ");";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MESSAGES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }
}
