package com.example.chatroomtest;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.chatroomtest.databinding.FragmentChatBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatFragment extends Fragment {
    private FragmentChatBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList = new ArrayList<>();
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String ip;
    private final int serverPort = 8888;
    private ExecutorService executorService;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private Thread receiveThread;
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_STORAGE_PERMISSION = 1002;
    private String currentSelectedUser;
    private MessageDAO messageDAO;
    private static final String TAG = "ChatFragment";


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messageDAO = new MessageDAO(requireContext());
        messageDAO.open();
    }

    private void loadHistoryMessages() {
        List<ChatMessage> historyMessages = messageDAO.getRecentMessages(200);
        if (!historyMessages.isEmpty()) {
            messageList.clear();
            messageList.addAll(historyMessages);
            chatAdapter.notifyDataSetChanged();
            binding.recyclerView.scrollToPosition(messageList.size() - 1);
        }
    }

    private void saveMessageToDatabase(ChatMessage message) {
        String avatarPath = getAvatarPathForUser(message.getSender());
        message.setAvatarPath(avatarPath);
        messageDAO.saveMessage(message);
    }

    private String getAvatarPathForUser(String username) {
        File avatarFile = new File(requireContext().getFilesDir(), "avatar_" + username + ".png");
        return avatarFile.exists() ? avatarFile.getAbsolutePath() : null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        Bundle args = getArguments();
        if (args != null) {
            username = args.getString("username");
            ip = args.getString("ip");
        }
        chatAdapter = new ChatAdapter(requireContext(), messageList);
        chatAdapter.setOnAvatarClickListener(username -> {
            Log.d(TAG, "你的头像被点击了");
            currentSelectedUser = username;
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

                showPermissionExplanation();
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_STORAGE_PERMISSION);
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerView.setAdapter(chatAdapter);
        loadHistoryMessages();

        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newSingleThreadExecutor();
        }

        binding.btnSend.setOnClickListener(v -> sendMessage());

        connectToServer();

        return binding.getRoot();
    }

    private void showPermissionExplanation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("需要存储权限")
                .setMessage("需要存储权限来更改头像")
                .setPositiveButton("确定", (d, w) ->
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                REQUEST_STORAGE_PERMISSION))
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(getContext(), "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            requireContext().getContentResolver(), selectedImageUri);

                    String avatarPath = saveAvatar(currentSelectedUser, bitmap);

                    updateAvatarInMessages(currentSelectedUser, avatarPath);

                    chatAdapter.notifyDataSetChanged();

                    Toast.makeText(getContext(), "头像已更新", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), "加载图片失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String saveAvatar(String username, Bitmap bitmap) {
        try {
            File avatarFile = new File(requireContext().getFilesDir(), "avatar_" + username + ".png");
            FileOutputStream fos = new FileOutputStream(avatarFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            String avatarPath = avatarFile.getAbsolutePath();

            AvatarCache.getInstance().updateAvatarPath(username, avatarPath);

            updateAvatarPathInDatabase(username, avatarPath);
            updateAvatarInMessages(username, avatarPath);
            return avatarPath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 更新数据库中该用户的头像路径
    private void updateAvatarPathInDatabase(String username, String avatarPath) {
        // 更新所有该用户的消息记录
        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_AVATAR_PATH, avatarPath);

        String where = DBHelper.COLUMN_SENDER + " = ?";
        String[] whereArgs = {username};

        messageDAO.getDb().update(DBHelper.TABLE_MESSAGES, values, where, whereArgs);
    }

    // 更新当前消息列表中的头像路径
    private void updateAvatarInMessages(String username, String avatarPath) {
        updateAvatarPathInDatabase(username, avatarPath);

        for (int i = 0; i < messageList.size(); i++) {
            ChatMessage message = messageList.get(i);
            if (message.getSender().equals(username)) {
                message.setAvatarPath(avatarPath);
                chatAdapter.notifyItemChanged(i);
            }
        }
    }
    private void connectToServer() {
        executorService.execute(() -> {
            try {
                Log.d(TAG, "尝试连接服务器: " + ip + ":" + serverPort);
                socket = new Socket(ip, serverPort);
                Log.d(TAG, "连接服务器成功");
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                Log.d(TAG, "发送用户名: " + username);
                out.println(username);
                String loginResponse = in.readLine();
                Log.d(TAG, "登录响应: " + loginResponse);
                if (!"LOGIN_SUCCESS".equals(loginResponse)) {
                    throw new IOException("登录失败: " + loginResponse);
                }

                startReceivingMessages();

            } catch (IOException e) {
                mainHandler.post(() ->
                        Toast.makeText(getContext(), "连接服务器失败: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private void startReceivingMessages() {
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }
        receiveThread = new Thread(() -> {
            Log.d(TAG, "开始接收消息");
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    Log.d(TAG, "收到原始消息: " + message);

                    if (message.startsWith("ONLINE:")) {
                        final int onlineCount = Integer.parseInt(message.substring(7));
                        mainHandler.post(() -> {
                            binding.tvTitle.setText(getString(R.string.group_title, onlineCount));
                            Log.d(TAG, "在线人数更新: " + onlineCount);
                        });
                    } else {
                        final ChatMessage chatMessage = parseMessage(message);
                        mainHandler.post(() -> {
                            Log.d(TAG, "添加消息: " + chatMessage.getContent());
                            addMessage(chatMessage);
                        });
                    }
                }
            } catch (SocketException e) {
                Log.e(TAG, "Socket异常", e);
                mainHandler.post(() ->
                        Toast.makeText(getContext(), "连接断开", Toast.LENGTH_SHORT).show()
                );
            } catch (IOException e) {
                Log.e(TAG, "接收错误", e);
                mainHandler.post(() ->
                        Toast.makeText(getContext(), "接收错误: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            } finally {
                Log.d(TAG, "消息接收循环结束");
                closeConnection();
            }
        });

        receiveThread.start();
    }

    private void closeConnection() {
        try {
            if (out != null) {
                out.close();
                out = null;
            }
            if (in != null) {
                in.close();
                in = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "关闭连接错误", e);
        }
    }

    private ChatMessage parseMessage(String message) {
        String[] parts = message.split("§", 3);
        if (parts.length == 3) {
            return new ChatMessage(parts[1], parts[2], false, Long.parseLong(parts[0]));
        }
        return new ChatMessage("系统", "消息格式错误", false, System.currentTimeMillis());
    }

    private void sendMessage() {
        String content = binding.etMessage.getText().toString().trim();
        if (content.isEmpty()) {
            Log.d(TAG, "尝试发送空消息");
            return;
        }
        Log.d(TAG, "准备发送消息: " + content);

        ChatMessage message = new ChatMessage(username, content, true, System.currentTimeMillis());

        addMessage(message);

        if (executorService == null || executorService.isShutdown()) {
            Log.e(TAG, "线程池不可用");
            return;
        }
        Log.d(TAG, "提交发送任务");

        try {
            executorService.execute(() -> {
                Log.d(TAG, "开始执行发送任务");
                if (out != null) {
                    String msgToSend = message.getTimestamp() + "§" + username + "§" + content;
                    Log.d(TAG, "发送消息: " + msgToSend);
                    out.println(msgToSend);
                    out.flush(); // 确保消息立即发送
                } else {
                    Log.e(TAG, "输出流为null，无法发送消息");
                    mainHandler.post(() ->
                            Toast.makeText(getContext(), "连接已断开", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "任务被拒绝: " + e.getMessage());
        }

        binding.etMessage.setText("");
    }

    private void addMessage(ChatMessage message) {
        messageList.add(message);
        int position = messageList.size() - 1;
        chatAdapter.notifyItemInserted(position);
        binding.recyclerView.smoothScrollToPosition(position);

        saveMessageToDatabase(message);

        binding.recyclerView.post(() -> {
            View view = binding.recyclerView.getLayoutManager().findViewByPosition(position);
            if (view != null) {
                Animation animation;if (message.isSystem()) {
                    animation = AnimationUtils.loadAnimation(
                            getContext(), R.anim.fade_in
                    );
                } else {
                    animation = AnimationUtils.loadAnimation(
                            getContext(),
                            message.isSent() ? R.anim.slide_in_right : R.anim.slide_in_left
                    );
                }
                view.startAnimation(animation);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (messageDAO != null) {
            messageDAO.close();
        }
        closeConnection();
        if (receiveThread != null && receiveThread.isAlive()) {
            receiveThread.interrupt();
        }

        binding = null;
    }

    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }
}
