package server;

import common.Request;
import common.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.dao.ConnectDatabase;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AppServer {
    private static final int PORT = 8080;
    private static final ConcurrentHashMap<String, Set<PrintWriter>> chatRooms = new ConcurrentHashMap<>();
    public static final Logger logger = LoggerFactory.getLogger(AppServer.class);

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
}
