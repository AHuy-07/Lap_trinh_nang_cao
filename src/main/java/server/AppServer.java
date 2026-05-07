package server;

import common.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.dao.ConnectDatabase;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AppServer {
    private static final int PORT = 8080;
    public static final Logger logger = LoggerFactory.getLogger(AppServer.class);

    // Quản lí người dùng đã login (String: lưu Username, ClientHandler: lưu kết nối)
    public static final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    // Quản lí các roomId đang PENDING. String: RoomId. ClientHandler: như trên
    public static final Map<String, ClientHandler> pendingSellers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        if (ConnectDatabase.getConnection() != null) {
            logger.info("[SERVER] Kết nối database thành công");
        } else {
            logger.error("[SERVER] Không thể kết nối database. Dừng chương trình!");
            return;
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("[SERVER] Server đấu giá đang chạy trên port {}...", PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            logger.error("[SERVER] Lỗi ServerSocket: ", e);
        } finally {
            ConnectDatabase.closeConnection();
        }
    }

    public static void addOnlineUser(String username, ClientHandler handler) {
        onlineUsers.put(username, handler);
    }

    public static void removeOnlineUser(String username) {
        onlineUsers.remove(username);
    }

    public static void sendToSpecificUser(String targetUsername, Request request) {
        ClientHandler handler = onlineUsers.get(targetUsername);
        if (handler != null) {
            handler.sendResponse(request);
        } else {
            logger.warn("[SERVER] Không tìm thấy {} để gửi thông báo", targetUsername);
        }
    }
}
