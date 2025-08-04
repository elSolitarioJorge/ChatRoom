package com.example.chatroomtest;

import android.graphics.Bitmap;

public class ChatMessage {
    private String sender;
    private String content;
    private boolean sent;
    private boolean system;
    private long timestamp;
    private String avatarPath;


    public ChatMessage(String sender, String content, boolean sent, long timestamp) {
        this.sender = sender;
        this.content = content;
        this.sent = sent;
        this.timestamp = timestamp;
        this.system = "系统".equals(sender);
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public boolean isSent() {
        return sent;
    }

    public boolean isSystem() {
        return system;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}
