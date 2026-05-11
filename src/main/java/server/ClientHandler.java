package server;

import common.Request;
import common.models.BidTransaction;
import common.models.Product;
import common.models.Room;
import common.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.dao.BidDAO;
import server.dao.ProductDAO;
import server.dao.RoomDAO;
import server.dao.UserDAO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
            default:
                logger.warn("Hành động không xác định {}", action);
        }
    }

    private void handleGetMyProducts(Request req) {
        List<Product> list = ProductDAO.getProductsBySeller(this.username);
        sendResponse(new Request("GET_MY_PRODUCTS_SUCCESS", list));
    }

    private void handleGetActiveRooms() {
        List<Room> activeRooms = RoomDAO.getActiveRooms();
        sendResponse(new Request("GET_ACTIVE_ROOMS_SUCCESS", activeRooms));
    }

    private void handleJoinRoom(Request req) {
        if (this.username == null) {
            sendResponse(new Request("JOIN_ROOM_FAIL", "Bạn cần đăng nhập trước khi vào phòng"));
            return;
        }

        if (!"BIDDER".equalsIgnoreCase(this.userRole)) {
            sendResponse(new Request("JOIN_ROOM_FAIL", "Chỉ bidder mới được vào phòng đấu giá"));
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
        sendResponse(new Request("JOIN_ROOM_SUCCESS", room));
    }

    private void handlePlaceBid(Request req) {
        BidTransaction bidRequest = (BidTransaction) req.getData();
        String roomId = bidRequest.getRoomId();
        long bidAmount = bidRequest.getBidAmount();

        Room room = RoomDAO.getRoomById(roomId);

        if (room == null) {
            sendResponse(new Request("PLACE_BID_FAIL", "Phòng không tồn tại"));
            return;
        }

        if (!"ACTIVE".equals(room.getStatus())) {
            sendResponse(new Request("PLACE_BID_FAIL", "Phòng không còn hoạt động"));
            return;
        }

        if (this.username == null) {
            sendResponse(new Request("PLACE_BID_FAIL", "Bạn cần đăng nhập trước khi đấu giá"));
            return;
        }

        if (!"BIDDER".equalsIgnoreCase(this.userRole)) {
            sendResponse(new Request("PLACE_BID_FAIL", "Chỉ bidder mới được tham gia đấu giá"));
            return;
        }

        if (this.username.equals(room.getSellerName())) {
            sendResponse(new Request("PLACE_BID_FAIL", "Seller không được đấu giá phòng của chính mình"));
            return;
        }

        if (!isRoomInAuctionTime(room)) {
            sendResponse(new Request("PLACE_BID_FAIL", "Hiện không nằm trong thời gian đấu giá"));
            return;
        }

        Object roomLock = roomBidLocks.computeIfAbsent(roomId, key -> new Object());

        synchronized (roomLock) {
            Room latestRoom = RoomDAO.getRoomById(roomId);

            if (latestRoom == null) {
                sendResponse(new Request("PLACE_BID_FAIL", "Phòng không tồn tại"));
                return;
            }

            if (!"ACTIVE".equals(latestRoom.getStatus())) {
                sendResponse(new Request("PLACE_BID_FAIL", "Phòng không còn hoạt động"));
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

            boolean success = BidDAO.placeBid(latestRoom, this.username, bidAmount);

            if (!success) {
                sendResponse(new Request("PLACE_BID_FAIL", "Không thể đặt giá, vui lòng thử lại"));
                return;
            }

            BidTransaction latestBid = BidDAO.getLatestBid(roomId);

            sendResponse(new Request("PLACE_BID_SUCCESS", latestBid));
            AppServer.broadcastToRoom(roomId, new Request("NEW_BID", latestBid));
        }
    }

    private boolean isRoomInAuctionTime(Room room) {
        try {
            LocalDateTime now = LocalDateTime.now();

            if (room.getBeginTime() != null && !room.getBeginTime().isBlank()) {
                LocalDateTime beginTime = LocalDateTime.parse(room.getBeginTime());

                if (now.isBefore(beginTime)) {
                    return false;
                }
            }

            if (room.getEndTime() != null && !room.getEndTime().isBlank()) {
                LocalDateTime endTime = LocalDateTime.parse(room.getEndTime());

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

            AppServer.addOnlineUser(this.username, this);

            sendResponse(new Request("LOGIN_SUCCESS", user));
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
                int status = newStatus.equals("ACTIVE") ? 2 : 0;
                ProductDAO.updateProductStatus(productId, status);
                if (newStatus.equals("ACTIVE")) {
                    ProductDAO.updateRoomId(productId, roomId);
                }
                AppServer.pendingSellers.remove(roomId);
            }
        }

        sendResponse(new Request("SUCCESS", "Đã thực hiện quyết định " + newStatus));
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
}