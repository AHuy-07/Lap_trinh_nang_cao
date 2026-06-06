package server;

import common.Request;
import common.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.dao.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private static final Map<String, Object> roomBidLocks = new ConcurrentHashMap<>();

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String username;
    private String userRole;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Khởi tạo Stream: out trước, in sau để tránh deadlock
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());

            Object received;

            while((received = ois.readObject()) != null) {
                if (received instanceof Request) {
                    Request req = (Request) received;
                    handleRequest(req);
                }
            }
        } catch(Exception e) {
            logger.error("Lỗi kết nối với Client {}: {}", username, e.getMessage());
        } finally {
            closeEverything();
        }
    }

    private void handleRequest(Request req)  {
        String action = req.getAction();

        switch (action) {
            case "LOGIN":
                handleLogin(req);
                break;
            case "SIGN_UP":
                handleSignUp(req);
                break;
            case "LOG_OUT":
                handleLogOut(req);
                break;
            case "CREATE_ROOM":
                handleCreateRoom(req);
                break;
            case "GET_PENDING_ROOMS":
                handleGetPendingRooms(req);
                break;
            case "APPROVE_ROOM":
                handleAdminDecision(req, "ACTIVE");
                break;
            case "REJECTED_ROOM":
                handleAdminDecision(req, "CLOSED");
                break;
            case "GET_MY_ROOMS":
                handleGetMyRooms();
                break;
            case "GET_ACTIVE_ROOMS":
                handleGetActiveRooms();
                break;
            case "JOIN_ROOM":
                handleJoinRoom(req);
                break;
            case "PLACE_BID":
                handlePlaceBid(req);
                break;
            case "LEAVE_ROOM":
                handleLeaveRoom(req);
                break;
            case "ADD_PRODUCTS":
                handleAddProducts(req);
                break;
            case "GET_MY_PRODUCTS":
                handleGetMyProducts(req);
                break;
            case "GET_BID_HISTORY":
                handleGetBidHistory(req);
                break;
            // xử lý ví tiền
            case "GET_WALLET_INFO":
                handleGetWalletInfo(req);
                break;
            case "DEPOSIT":
                handleDepositWallet(req);
                break;
            case "WITHDRAW":
                handleWithdrawWallet(req);
                break;
            case "GET_HISTORY":
                handleGetWalletHistory();
                break;
            case "FORCE_END_ROOM":
                handleForceEndRoom(req);
                break;
            case "TOGGLE_AUTO_BID":
                handleToggleAutoBid(req);
                break;
            case "CHECK_AUTO_BID_STATUS":
                handleCheckAutoBidStatus(req);
                break;
            // xử lý sản phẩm đấu giá thành công (23/05)
            case "GET_MY_WON_PRODUCTS":
                handleGetMyWonProducts(req);
                break;
            // xóa sản phẩm đã thêm vào trong seller
            case "DELETE_PRODUCT":
                handleDeleteProduct(req);
                break;
            default:
                logger.warn("Hành động không xác định {}", action);
        }
    }

    private void handleCheckAutoBidStatus(Request req) {
        String[] data = (String[])req.getData();
        String roomId = data[0];
        String username = data[1];
        long maxPrice = AutoBidDAO.checkAutoBidStatus(roomId, username);

        if (maxPrice == -1) {
            sendResponse(new Request("CHECK_AUTO_BID_FALSE", -1));
        } else {
            sendResponse(new Request("CHECK_AUTO_BID_TRUE", maxPrice));
        }
    }

    private void handleToggleAutoBid(Request req) {
        String[] data = (String[])req.getData();
        String roomId = data[0];
        String status = data[2];

        // 31/05

        if ("ON".equals(status)) {
            long maxPrice = Long.parseLong(data[1]);
            long createAt = Long.parseLong(data[3]);
            AutoBidDAO.saveAutoBid(roomId, this.username, maxPrice, createAt);
            sendResponse(new Request("TOGGLE_AUTO_BID_SUCCESS", "Đã bật đấu giá tự động"));
        } else {
            AutoBidDAO.removeAutoBid(roomId, this.username);
            sendResponse(new Request("TOGGLE_AUTO_BID_SUCCESS", "Đã tắt đấu giá tự động"));
        }
    }


    private void handleForceEndRoom(Request req) {
        Room room = (Room) req.getData();
        Room target = RoomDAO.getRoomById(room.getRoomId());

        if (target != null && target.getStatus().equals("ACTIVE")) {
            AppServer.handleEndRoom(target);
            sendResponse(new Request("FORCE_END_ROOM_SUCCESS", "Đã kết thúc phiên đấu giá chủ động!"));
        } else {
            sendResponse(new Request("FORCE_END_ROOM_FAIL", "Phòng không tồn tại hoặc đã đóng trước đó."));
        }
    }

    private void handleGetBidHistory(Request req) {
        Room room = (Room) req.getData();
        List<BidTransaction> list = BidDAO.getBidHistory(room);
        sendResponse(new Request("GET_BID_HISTORY_SUCCESS", list));
    }

    private void handleGetMyProducts(Request req) {
        List<Product> list = ProductDAO.getProductsBySeller(this.username);
        sendResponse(new Request("GET_MY_PRODUCTS_SUCCESS", list));
    }

    private void handleGetActiveRooms() {
        List<Room> rooms = RoomDAO.getRoomsForBidder();
        sendResponse(new Request("GET_ACTIVE_ROOMS_SUCCESS", rooms));
    }

    private void handleJoinRoom(Request req) {
        if (this.username == null) {
            sendResponse(new Request("JOIN_ROOM_FAIL", "Bạn cần đăng nhập trước khi vào phòng"));
            return;
        }

        String roomId = (String) req.getData();
        Room room = RoomDAO.getRoomById(roomId);

        if (room == null) {
            sendResponse(new Request("JOIN_ROOM_FAIL", "Phòng không tồn tại"));
            return;
        }

        if (!"ACTIVE".equals(room.getStatus())) {
            sendResponse(new Request("JOIN_ROOM_FAIL", "Phòng chưa được mở đấu giá"));
            return;
        }

        AppServer.subscribeRoom(roomId, this);
        int participantCount = BidDAO.getParticipantCount(roomId);
        Object[] data = new Object[]{room, participantCount};
        sendResponse(new Request("JOIN_ROOM_SUCCESS", data));
    }

    private void handlePlaceBid(Request req) {
        BidTransaction bidRequest = (BidTransaction) req.getData();
        String roomId = bidRequest.getRoomId();
        long bidAmount = bidRequest.getBidAmount();

        Room room = RoomDAO.getRoomById(roomId);

        if (!checkBidCondition(room)) {
            return;
        }

        Object roomLock = roomBidLocks.computeIfAbsent(roomId, key -> new Object());

        synchronized (roomLock) {
            Room latestRoom = RoomDAO.getRoomById(roomId);

            if (!checkBidCondition(latestRoom)) {
                return;
            }

            long bidStep = Room.calculateDefaultBidStep(latestRoom.getStartingPrice());
            long currentPrice = BidDAO.getCurrentPrice(roomId);
            long minimumNextPrice = currentPrice + bidStep;

            if (bidAmount < minimumNextPrice) {
                sendResponse(new Request(
                        "PLACE_BID_FAIL",
                        "Giá phải tối thiểu là " + minimumNextPrice
                ));
                return;
            }

            long currentBalance = WalletDAO.getBalance(this.username);
            if (currentBalance < bidAmount) {
                sendResponse(new Request(
                        "PLACE_BID_FAIL",
                        "Số dư ví không đủ!"
                ));
                return;
            }

            // Lấy thông tin của người thắng cũ ra
            String oldWinner = latestRoom.getWinnerUsername();
            long oldWinPrice = 0L;
            long oldBidderBalance = 0L;

            if (oldWinner != null) {
                oldWinPrice = latestRoom.getWinPrice();
                oldBidderBalance = WalletDAO.getBalance(oldWinner);
                if (oldWinner.equals(this.username)) {
                    sendResponse(new Request(
                            "PLACE_BID_FAIL",
                            "Bạn phải chờ người khác đấu giá đã!"
                    ));
                    return;
                }
            }



            boolean success = BidDAO.placeBid(latestRoom, oldWinner, this.username, bidAmount, oldBidderBalance + oldWinPrice, currentBalance - bidAmount);

            if (!success) {
                sendResponse(new Request("PLACE_BID_FAIL", "Không thể đặt giá, vui lòng thử lại"));
                return;
            }

            BidTransaction latestBid = BidDAO.getLatestBid(roomId);
            int participantCount = BidDAO.getParticipantCount(roomId);
            Object[] data = new Object[]{latestBid, participantCount};
            sendResponse(new Request("PLACE_BID_SUCCESS", data));
            AppServer.broadcastToRoom(roomId, new Request("NEW_BID", data));
            //Them 1 phut
            extendEndTimeIfNeeded(roomId, latestRoom);
            triggerAutoBid(roomId);
        }
    }

    // Hàm bot chạy auto đấu giá
    private void triggerAutoBid(String roomId) {
        /*

         */

        new Thread(() -> {
            while (true) {
                Room room = RoomDAO.getRoomById(roomId);
                if (room == null || !room.getStatus().equals("ACTIVE")) {
                    break;
                }

                long currentPrice = BidDAO.getCurrentPrice(roomId);
                long bidStep = Room.calculateDefaultBidStep(room.getStartingPrice());
                long nextPrice = currentPrice + bidStep;

                List<AutoBidSetting> bidders = AutoBidDAO.getAutoBidders(roomId);
                if (bidders == null || bidders.isEmpty()) {
                    break;
                }

                PriorityQueue<AutoBidSetting> priorityQueue = new PriorityQueue<>(Comparator.comparingLong(AutoBidSetting::getCreateAt).thenComparingLong(AutoBidSetting::getMaxPrice));
                priorityQueue.addAll(bidders);

                boolean actionTake = false;


                Set<AutoBidSetting> autobidNeedToDelete = new HashSet<>();

                while (!priorityQueue.isEmpty()) {
                    AutoBidSetting bot = priorityQueue.poll();

                    if (bot.getUsername().equals(room.getWinnerUsername())) {
                        continue;
                    }

                    if (nextPrice > bot.getMaxPrice()) {
                        autobidNeedToDelete.add(bot);
                        continue;
                    }

                    long botBalance = WalletDAO.getBalance(bot.getUsername());
                    if (botBalance < nextPrice) {
                        autobidNeedToDelete.add(bot);
                        continue;
                    }

                    String oldWinner = room.getWinnerUsername();
                    long oldWinPrice = 0L;
                    long oldBidderBalance = 0L;

                    if (oldWinner != null) {
                        oldWinPrice = room.getWinPrice();
                        oldBidderBalance = WalletDAO.getBalance(oldWinner);
                    }

                    // Xu li phan Xung dot
                    AutoBidSetting earlierConflictBot = null;
                    for (AutoBidSetting otherbot : bidders) {
                        if (autobidNeedToDelete.contains(otherbot)) {
                            continue;
                        }
                        if (!otherbot.getUsername().equals(bot.getUsername())) {
                            Long otherBotBalance = WalletDAO.getBalance(otherbot.getUsername());
                            if (otherBotBalance < nextPrice) {
                                autobidNeedToDelete.add(otherbot);
                                continue;
                            }
                            if (otherbot.getMaxPrice() < nextPrice) {
                                autobidNeedToDelete.add(otherbot);
                                continue;
                            }
                            if (earlierConflictBot != null) {
                                if (otherbot.getCreateAt() < earlierConflictBot.getCreateAt()) {
                                    earlierConflictBot = otherbot;
                                }
                            } else {
                                if (otherbot.getCreateAt() < bot.getCreateAt()) {
                                    earlierConflictBot = otherbot;
                                }
                            }
                        }
                    }


                    boolean success;

                    if (earlierConflictBot != null) {
                        long earlierConflictBotBalance = WalletDAO.getBalance(earlierConflictBot.getUsername());
                        success = BidDAO.placeBid(room, oldWinner, earlierConflictBot.getUsername(), nextPrice, oldBidderBalance + oldWinPrice, earlierConflictBotBalance - nextPrice);
                    } else {
                        success = BidDAO.placeBid(room, oldWinner, bot.getUsername(), nextPrice, oldBidderBalance + oldWinPrice, botBalance - nextPrice);
                    }

                    if (success) {
                        int participantCount = BidDAO.getParticipantCount(roomId);
                        BidTransaction latestBid = BidDAO.getLatestBid(roomId);
                        Object[] data = new Object[]{latestBid, participantCount};
                        AppServer.broadcastToRoom(roomId, new Request("NEW_BID", data));
                        extendEndTimeIfNeeded(roomId, room);
                        actionTake = true;
                        break;
                    }

                }

                for (AutoBidSetting bot : autobidNeedToDelete) {
                    AutoBidDAO.removeAutoBid(roomId, bot.getUsername());
                    AppServer.sendToSpecificUser(bot.getUsername(), new Request("AUTO_BID_DISABLED_NO_MONEY", room));
                }

                if (!actionTake) {
                    break;
                }

                try {
                    // Cho bot nghỉ 500 ms
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.error("Lỗi khi cho bot nghỉ: {}", e.getMessage(), e);
                    Thread.currentThread().interrupt(); // Đảm bảo trạng thái ngắt của luồng được giữ nguyên
                    break;
                }
            }
        }).start();
    }

    //Them 1 phut
    private void extendEndTimeIfNeeded(String roomId, Room room) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            if (room.getEndTime() == null || room.getEndTime().isBlank()) return;
            LocalDateTime endTime = LocalDateTime.parse(room.getEndTime(), formatter);
            LocalDateTime now = LocalDateTime.now();
            long secondsLeft = java.time.Duration.between(now, endTime).toSeconds();
            if (secondsLeft > 0 && secondsLeft <= 60) {
                LocalDateTime newEndTime = endTime.plusMinutes(1);
                String newEndTimeStr = newEndTime.format(formatter);
                boolean updated = RoomDAO.updateEndTime(roomId, newEndTimeStr);
                if (updated) {
                    AppServer.broadcastToRoom(roomId, new Request("END_TIME_EXTENDED", newEndTimeStr));
                }
            }
        } catch (Exception e) {
            logger.warn("Không thể kiểm tra / gia hạn thời gian phòng {}: {}", roomId, e.getMessage());
        }
    }

    private boolean checkBidCondition(Room room) {
        if (room == null) {
            sendResponse(new Request("PLACE_BID_FAIL", "Phòng không tồn tại"));
            return false;
        }

        if (!"ACTIVE".equals(room.getStatus())) {
            sendResponse(new Request("PLACE_BID_FAIL", "Phòng không còn hoạt động"));
            return false;
        }


        if (!isRoomInAuctionTime(room)) {
            sendResponse(new Request("PLACE_BID_FAIL", "Hiện không nằm trong thời gian đấu giá"));
            return false;
        }
        return true;
    }

    private boolean isRoomInAuctionTime(Room room) {
        /*
        Formatter giúp định nghĩa beginTime sang đúng chuẩn ISO của localdateTime
         */
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        try {
            LocalDateTime now = LocalDateTime.now();

//            System.out.println(">>> [DEBUG TIME] Now: " + now
//                    + " | Room Begin: " + room.getBeginTime()
//                    + " | Room End: " + room.getEndTime());

            if (room.getBeginTime() != null && !room.getBeginTime().isBlank()) {
                LocalDateTime beginTime = LocalDateTime.parse(room.getBeginTime(), formatter);

                if (now.isBefore(beginTime)) {
                    return false;
                }
            }

            if (room.getEndTime() != null && !room.getEndTime().isBlank()) {
                LocalDateTime endTime = LocalDateTime.parse(room.getEndTime(), formatter);

                if (now.isAfter(endTime)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.warn("Không parse được thời gian phòng {}: {}", room.getRoomId(), e.getMessage());
            return true;
        }
    }

    private void handleLeaveRoom(Request req) {
        AppServer.unsubscribeFromAllRooms(this);
        sendResponse(new Request("LEAVE_ROOM_SUCCESS", null));
    }

    private void handleGetMyRooms() {
        List<Room> list = RoomDAO.getRoomsBySeller(this.username);
        sendResponse(new Request("GET_MY_ROOMS_SUCCESS", list));
    }

    private void handleGetPendingRooms(Request req) {
        try {
            List<Room> pendingList = RoomDAO.getPendingRooms();
            if (pendingList.size() == 0) {
                oos.writeObject(new Request("NOT_EXIST_PENDING_ROOM", null));
            }else {
                oos.writeObject(new Request("GET_PENDING_ROOMS_SUCCESS", pendingList));
            }
        } catch (IOException e) {
            logger.error("Lỗi khi gửi danh sách phòng cho Admin: {}", e.getMessage());
        }
    }

    private void handleLogin(Request req) {
        String[] info = (String[]) req.getData();
        User user = UserDAO.login(info[0], info[1]);

        if (user != null) {
            this.username = user.getUsername();
            this.userRole = user.getRole();


            ClientHandler handler = AppServer.onlineUsers.get(this.username);
            if (handler != null) {
                sendResponse(new Request("LOGIN_FAIL", "Tài khoản đã được đăng nhập ở nơi khác"));
            }else {
                AppServer.addOnlineUser(this.username, this);
                sendResponse(new Request("LOGIN_SUCCESS", user));
            }
        } else {
            sendResponse(new Request("LOGIN_FAIL", "Sai tài khoản hoặc mật khẩu"));
        }
    }

    private void handleSignUp(Request req) {
        String[] info = (String[]) req.getData();
        User user = UserDAO.signUp(info[0], info[1], info[2]);

        if (user != null) {
            sendResponse(new Request("SIGN_UP_SUCCESS", user));
        } else {
            sendResponse(new Request("SIGN_UP_FAIL", "Trùng tên đăng nhập"));
        }
    }

    private void handleLogOut(Request req) {
        sendResponse(new Request("LOG_OUT_SUCCESS", null));
        AppServer.removeOnlineUser(this.username);
        AppServer.pendingSellers.values().removeIf(handler -> handler.equals(this));
        AppServer.unsubscribeFromAllRooms(this);

        logger.info("[SERVER] Người dùng {} đã được xóa khỏi danh sách online ngầm.", this.username);
    }

    private void handleCreateRoom(Request req) {
        Room roomRequest = (Room) req.getData();
        boolean checkProduct = ProductDAO.isProductValid(roomRequest.getProductId());

        if (!checkProduct) {
            sendResponse(new Request("CREATE_ROOM_FAIL", "Sản phẩm không khả dụng"));
        } else {
            Room roomResponse = RoomDAO.createRoom(roomRequest);

            if (roomResponse != null) {
                sendResponse(new Request("SEND_CREATE_ROOM_SUCCESS", null));
                AppServer.pendingSellers.put(roomResponse.getRoomId(), this);
                ProductDAO.updateProductStatus(roomRequest.getProductId(), 1);
                AppServer.sendToSpecificUser("admin", new Request("NEW_PENDING_ROOM", roomResponse));
            } else {
                sendResponse(new Request("CREATE_ROOM_FAIL", "Trùng mã phòng"));
            }
        }
    }

    private void handleAdminDecision(Request req, String newStatus) {
        Room room = (Room) req.getData();
        String roomId = room.getRoomId();
        String productId = room.getProductId();

        boolean success = RoomDAO.updateRoomStatus(roomId, newStatus);

        if (success) {
            ClientHandler handler = AppServer.pendingSellers.get(roomId);
            if (handler != null) {
                String responseAction = newStatus.equals("ACTIVE") ? "CREATE_ROOM_SUCCESS" : "CREATE_ROOM_REJECTED";
                String responseData = "Phòng" + roomId + " đã được " + (newStatus.equals("ACTIVE") ? "Duyệt" : "Từ chối");
                handler.sendResponse(new Request(responseAction, responseData));
                int status = newStatus.equals("ACTIVE") ? 1 : 0;
                ProductDAO.updateProductStatus(productId, status);
                if (newStatus.equals("ACTIVE")) {
                    ProductDAO.updateRoomId(productId, roomId);
                }
                AppServer.pendingSellers.remove(roomId);
            }
        }

        sendResponse(new Request("SUCCESS", "Đã thực hiện quyết định " + newStatus));
    }

    //hàm xử lý tiền
    private void handleGetWalletInfo(Request req) {
        String reqUsername = (String) req.getData(); // Client gửi lên username

        // Bảo mật: Kiểm tra xem client có đang request đúng tài khoản của mình không
        if (this.username == null || !this.username.equals(reqUsername)) {
            sendResponse(new Request("GET_WALLET_FAIL", "Không có quyền truy cập!"));
            return;
        }

        long balance = server.dao.WalletDAO.getBalance(this.username);

        if (balance != -1) {
            sendResponse(new Request("GET_WALLET_SUCCESS", String.valueOf(balance)));
        } else {
            sendResponse(new Request("GET_WALLET_FAIL", "Lỗi khi tải số dư từ CSDL!"));
        }
    }

    private void handleDepositWallet(Request req) {
        String[] data = (String[]) req.getData();
        String reqUsername = data[0];
        long amount = Long.parseLong(data[1]);
        String method = data[2];

        if (this.username == null || !this.username.equals(reqUsername)) {
            sendResponse(new Request("DEPOSIT_FAIL", "Yêu cầu không hợp lệ!"));
            return;
        }

        boolean success = server.dao.WalletDAO.processTransaction(this.username, amount, "DEPOSIT", method);

        if (success) {

            sendResponse(new Request("DEPOSIT_SUCCESS", ""));
        } else {
            sendResponse(new Request("DEPOSIT_FAIL", "Lỗi ghi nhận giao dịch vào CSDL!"));
        }
    }

    private void handleWithdrawWallet(Request req) {
        String[] data = (String[]) req.getData();
        String reqUsername = data[0];
        long amount = Long.parseLong(data[1]);
        String method = data[2];

        if (this.username == null || !this.username.equals(reqUsername)) {
            sendResponse(new Request("WITHDRAW_FAIL", "Yêu cầu không hợp lệ!"));
            return;
        }

        boolean success = server.dao.WalletDAO.processTransaction(this.username, amount, "WITHDRAW", method);

        if (success) {
            sendResponse(new Request("WITHDRAW_SUCCESS", ""));
        } else {
            sendResponse(new Request("WITHDRAW_FAIL", "Số dư không đủ hoặc lỗi hệ thống!"));
        }
    }

    private void handleGetWalletHistory() {
        if (this.username == null) return;
        List<TransactionRecord> history = server.dao.WalletDAO.getHistory(this.username);
        sendResponse(new Request("GET_HISTORY_SUCCESS", history));
    }

    public String getUserRole() {
        return userRole;
    }

    private void closeEverything() {
        // Xóa username khỏi danh sách khi offline
        AppServer.removeOnlineUser(this.username);

        AppServer.pendingSellers.values().removeIf(handler -> handler.equals(this));
        AppServer.unsubscribeFromAllRooms(this);

        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.error("lỗi không đóng được file {}", e.getMessage(), e);
        }
    }

    public synchronized void sendResponse(Request response) {
        try {
            oos.writeObject(response);
            oos.flush();
        } catch (IOException e) {
            logger.error("Lỗi gửi dữ liệu cho {}: {}", username, e.getMessage());
        }
    }

    private void handleAddProducts(Request req){
        String[] data = (String[]) req.getData();
        String name = data[0];
        String type = data[1];
        String details = data[2];

        String id = ProductDAO.generateNewId();

        try {
            Product newProduct = ProductDAO.addProducts(id, name, type, details, username);

            if (newProduct != null) {
                oos.writeObject(new Request("ADD_SUCCESS", newProduct));
            } else {
                oos.writeObject(new Request("ADD_FAIL", "Lỗi ghi vào Database!"));
            }
            oos.flush();

        } catch (IOException e) {
            logger.error("Lỗi khi tải dữ liệu", e);
        }
    }

    //23/05
    private void handleGetMyWonProducts(Request req) {
        String username = (String) req.getData();

        try {
            // Gọi DAO để lấy danh sách từ Database
            List<Room> wonRooms = RoomDAO.getWonRoomsByUsername(username);

            if (wonRooms != null) {
                oos.writeObject(new Request("GET_MY_WON_PRODUCTS_SUCCESS", wonRooms));
            } else {
                oos.writeObject(new Request("GET_MY_WON_PRODUCTS_FAIL", "Không có dữ liệu!"));
            }
            oos.flush();

        } catch (IOException e) {
            logger.error("Lỗi khi tải danh sách phòng chiến thắng", e);
        }
    }

    private void handleDeleteProduct(Request req){
        String productId = (String) req.getData();

        // Gọi hàm DAO ở bước 1 để xóa
        boolean isDeleted = ProductDAO.deleteProduct(productId);
        int isSold = ProductDAO.getIsSoldStatus(productId);

        if (isSold == 0) {
            if (isDeleted) {
                sendResponse(new Request("DELETE_PRODUCT_SUCCESS", productId));
            } else {
                sendResponse(new Request("DELETE_PRODUCT_FAIL", "Không thể xóa hoặc sản phẩm không tồn tại!"));
            }
        } else {
            sendResponse(new Request("DELETE_PRODUCT_FAIL", "Sản phẩm đang được đấu giá hoặc đã bán!"));
        }


    }
}