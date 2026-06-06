package server;

import common.Request;
import common.models.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.dao.ConnectDatabase;
import server.dao.ProductDAO;
import server.dao.RoomDAO;
import server.dao.WalletDAO;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AppServer {
    private static final int PORT = 8080;
    public static final Logger logger = LoggerFactory.getLogger(AppServer.class);

    // Quản lí người dùng đã login (String: lưu Username, ClientHandler: lưu kết nối)
    public static final Map<String, ClientHandler> onlineUsers = new ConcurrentHashMap<>();

    // Quản lí các roomId đang PENDING. String: RoomId. ClientHandler: như trên
    public static final Map<String, ClientHandler> pendingSellers = new ConcurrentHashMap<>();

    public static final Map<String, Set<ClientHandler>> roomSubscribers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        if (ConnectDatabase.getConnection() != null) {
            logger.info("[SERVER] Kết nối database thành công");
        } else {
            logger.error("[SERVER] Không thể kết nối database. Dừng chương trình!");
            return;
        }

        manageActiveRoom();

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

    public static void manageActiveRoom() {
        Timer timer = new Timer(true);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    LocalDateTime now = LocalDateTime.now();

                    List<Room> activeRooms = RoomDAO.getActiveRooms();
                    for (Room room : activeRooms) {
                        LocalDateTime endTime = LocalDateTime.parse(room.getEndTime(), formatter);

                        if (now.isAfter(endTime)) {
                            handleEndRoom(room);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Lỗi ở manageActiveRoom", e);
                }
            }
        }, 0, 1000);
    }

    public static void handleEndRoom(Room room) {
        String roomId = room.getRoomId();

        boolean success = RoomDAO.updateRoomStatus(roomId, "CLOSED");

        if (success) {
            String winner = room.getWinnerUsername();
            long winPrice = room.getWinPrice();

            if (winner != null) {
                String seller = room.getSellerName();
                long sellerBalance = WalletDAO.getBalance(seller);
                WalletDAO.updateBalance(seller, sellerBalance + winPrice);
                String[] winData = new String[]{winner, Long.toString(winPrice), room.getRoomName(), room.getRoomId()};
                Request req = new Request("AUCTION_ENDED_WITH_WINNER", winData);
                ProductDAO.updateProductStatus(room.getProductId(), 2);
                broadcastToRoom(roomId, req);
            } else { // Nếu không có ai bid
                String[] data = new String[]{room.getRoomName(), room.getRoomId()};
                Request req = new Request("AUCTION_ENDED_WITH_NO_BID", data);
                ProductDAO.updateProductStatus(room.getProductId(), 0); // Cập nhật trạng thái sản phẩm về sẵn sàng
                broadcastToRoom(roomId, req);
            }
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

    public static void subscribeRoom(String roomId, ClientHandler handler) {
        roomSubscribers
                .computeIfAbsent(roomId, key -> ConcurrentHashMap.newKeySet())
                .add(handler);
    }

    public static void unsubscribeFromAllRooms(ClientHandler handler) {
        roomSubscribers.values().forEach(handlers -> handlers.remove(handler));
    }

    public static void broadcastToRoom(String roomId, Request request) {
        Set<ClientHandler> handlers = roomSubscribers.get(roomId);

        if (handlers == null || handlers.isEmpty()) {
            return;
        }

        for (ClientHandler handler : handlers) {
            handler.sendResponse(request);
        }
    }
}