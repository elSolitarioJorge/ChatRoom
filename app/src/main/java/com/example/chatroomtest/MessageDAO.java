package com.example.chatroomtest;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private SQLiteDatabase db;
    private DBHelper dbHelper;

    public SQLiteDatabase getDb() {
        return db;
    }

    public MessageDAO(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void open() {
        db = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long saveMessage(ChatMessage message) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_SENDER, message.getSender());
        values.put(DBHelper.COLUMN_CONTENT, message.getContent());
        values.put(DBHelper.COLUMN_IS_SENT, message.isSent() ? 1 : 0);
        values.put(DBHelper.COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(DBHelper.COLUMN_AVATAR_PATH, message.getAvatarPath());
        values.put(DBHelper.COLUMN_IS_SYSTEM, message.isSystem() ? 1 : 0);

        return db.insert(DBHelper.TABLE_MESSAGES, null, values);
    }

    public List<ChatMessage> getAllMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        String orderBy = DBHelper.COLUMN_TIMESTAMP + " ASC";
        Cursor cursor = db.query(DBHelper.TABLE_MESSAGES, null, null, null, null, null, orderBy);

        if (cursor.moveToFirst()) {
            do {
                messages.add(cursorToMessage(cursor));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    public List<ChatMessage> getRecentMessages(int limit) {
        List<ChatMessage> messages = new ArrayList<>();
        String orderBy = DBHelper.COLUMN_TIMESTAMP + " DESC";
        String limitStr = String.valueOf(limit);
        Cursor cursor = db.query(DBHelper.TABLE_MESSAGES, null, null, null, null, null, orderBy, limitStr);

        if (cursor.moveToFirst()) {
            do {
                messages.add(0, cursorToMessage(cursor)); // 添加到开头，保持时间顺序
            } while (cursor.moveToNext());
        }
        cursor.close();
        return messages;
    }

    public void deleteAllMessages() {
        db.delete(DBHelper.TABLE_MESSAGES, null, null);
    }

    private ChatMessage cursorToMessage(Cursor cursor) {
        @SuppressLint("Range") String sender = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_SENDER));
        @SuppressLint("Range") String content = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_CONTENT));
        @SuppressLint("Range") boolean isSent = cursor.getInt(cursor.getColumnIndex(DBHelper.COLUMN_IS_SENT)) == 1;
        @SuppressLint("Range") long timestamp = cursor.getLong(cursor.getColumnIndex(DBHelper.COLUMN_TIMESTAMP));
        @SuppressLint("Range") String avatarPath = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_AVATAR_PATH));
        ChatMessage message = new ChatMessage(sender, content, isSent, timestamp);
        message.setAvatarPath(avatarPath);
        return message;
    }

    public void updateAvatarPath(String username, String avatarPath) {
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_AVATAR_PATH, avatarPath);

        String whereClause = DBHelper.COLUMN_SENDER + " = ?";
        String[] whereArgs = {username};

        db.update(DBHelper.TABLE_MESSAGES, values, whereClause, whereArgs);
    }
}
