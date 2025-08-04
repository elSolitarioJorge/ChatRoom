package com.example.chatroomtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.preference.PreferenceManager;
import android.util.LruCache;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AvatarCache {
    private static AvatarCache instance;
    private final LruCache<String, Bitmap> memoryCache;

    private final Map<String, String> pathMap = new ConcurrentHashMap<>();

    private AvatarCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static synchronized AvatarCache getInstance() {
        if (instance == null) {
            instance = new AvatarCache();
        }
        return instance;
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null && bitmap != null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }

    public void removeFromCache(String username) {
        memoryCache.remove(username);
        pathMap.remove(username);
    }

    public void updateAvatarPath(String username, String newPath) {
        pathMap.put(username, newPath);
        memoryCache.remove(username);
    }

    public Bitmap loadAvatar(Context context, String username, String avatarPath) {

        Bitmap cachedBitmap = getBitmapFromMemCache(username);
        if (cachedBitmap != null) {
            return cachedBitmap;
        }

        String effectivePath = avatarPath;

        if (effectivePath == null || effectivePath.isEmpty()) {
            effectivePath = pathMap.get(username);
        }

        if (effectivePath != null && !effectivePath.isEmpty()) {
            Bitmap bitmap = BitmapFactory.decodeFile(effectivePath);
            if (bitmap != null) {
                addBitmapToMemoryCache(username, bitmap);
                return bitmap;
            }
        }

        File avatarFile = new File(context.getFilesDir(), "avatar_" + username + ".png");
        if (avatarFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(avatarFile.getAbsolutePath());
            if (bitmap != null) {
                pathMap.put(username, avatarFile.getAbsolutePath());
                addBitmapToMemoryCache(username, bitmap);
                return bitmap;
            }
        }

        int defaultResId = username.equals(getCurrentUser(context)) ?
                R.drawable.p2 : R.drawable.p1;
        Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), defaultResId);
        addBitmapToMemoryCache(username, defaultBitmap);
        return defaultBitmap;
    }

    private String getCurrentUser(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("current_user", "");
    }
}