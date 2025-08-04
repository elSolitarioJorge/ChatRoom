package com.yaoheng.chatroom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {
    private static final int PORT = 8888;
    private static final int BROADCAST_INTERVAL = 240;
    private static final Map<String, PrintWriter> userWriters = new ConcurrentHashMap<>();
    private static final AtomicInteger onlineCount = new AtomicInteger(0);
    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("聊天服务器已启动，监听端口: " + PORT);

            scheduler.scheduleAtFixedRate(
                    () -> broadcastSystemEvent(),
                    BROADCAST_INTERVAL,
                    BROADCAST_INTERVAL,
                    TimeUnit.SECONDS
            );

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("新的客户端连接: " + clientSocket.getRemoteSocketAddress());
                executor.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            executor.shutdown();
            scheduler.shutdown();
        }
    }

    private static void broadcastSystemEvent() {
        if (onlineCount.get() > 0) {
            long currentTimeMillis = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            String formattedTime = sdf.format(new Date(currentTimeMillis));
            String eventMsg = System.currentTimeMillis() + "§系统§" + formattedTime;
            broadcast(eventMsg, "");
            System.out.println("已发送系统定时广播");
        }
    }

    // 广播消息给所有客户端
    public static void broadcast(String message, String username) {
        PrintWriter currentWriter = userWriters.get(username);
        for (PrintWriter writer : userWriters.values()) {
            if (!writer.equals(currentWriter)) {
                try {
                    writer.println(message);
                    writer.flush(); // 确保消息立即发送
                    System.out.println("广播消息: " + message);
                } catch (Exception e) {
                    System.err.println("广播消息失败: " + e.getMessage());
                }
            }
        }
    }

    // 更新在线人数
    public static void updateOnlineCount() {
        String countMsg = "ONLINE:" + onlineCount;
        broadcast(countMsg, "");
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 获取用户名
                username = in.readLine();
                if (username == null) {
                    System.out.println("客户端未发送用户名");
                    return;
                }
                username = username.trim();
                System.out.println("客户端登录尝试: " + username);
                // 检查用户名是否重复
                if (userWriters.containsKey(username)) {
                    out.println("ERROR:用户名已存在");
                    System.out.println("用户名已存在: " + username);
                    socket.close();
                    return;
                }

                out.println("LOGIN_SUCCESS");
                System.out.println("登录成功: " + username);

                userWriters.put(username, out);
                onlineCount.incrementAndGet();
                updateOnlineCount();

                String joinMsg = System.currentTimeMillis() + "§系统§" + "\"" + username + "\"" + " 加入了群聊";
                broadcast(joinMsg, username);

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("收到来自 " + username + " 的消息: " + message);
                    if ("exit".equalsIgnoreCase(message)) {
                        break;
                    }
                    broadcast(message, username);
                }
            } catch (SocketException e) {
                System.out.println(username + " 的连接异常断开: " + e.getMessage());
            } catch (IOException e) {
                System.out.println(username + " 的IO错误: " + e.getMessage());
            } finally {
                disconnectClient();
            }
        }

        private void disconnectClient() {
            System.out.println("清理资源: " + username);

            if (username != null) {
                // 从在线列表中移除
                if (userWriters.remove(username) != null) {
                    onlineCount.decrementAndGet();
                    updateOnlineCount();
                    String leaveMsg = System.currentTimeMillis() + "§系统§" + "\"" + username + "\"" + " 离开了聊天室";
                    broadcast(leaveMsg, username);
                }
            }

            // 关闭资源
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("关闭资源时出错: " + e.getMessage());
            }

            System.out.println("资源清理完成");
        }
    }
}
