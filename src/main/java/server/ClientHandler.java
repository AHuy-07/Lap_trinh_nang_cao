package server;

import common.Request;
import common.models.Room;
import common.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.dao.ProductDAO;
import server.dao.RoomDAO;
import server.dao.UserDAO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
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
            default:
                logger.warn("Hành động không xác định {}", action);
        }
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
                AppServer.pendingSellers.put(roomResponse.getRoomId(), this);

                AppServer.sendToSpecificUser("admin", new Request("NEW_PENDING_ROOM", roomResponse));
            } else {
                sendResponse(new Request("CREATE_ROOM_FAIL", "Trùng mã phòng"));
            }
        }
    }

    private void handleAdminDecision(Request req, String newStatus) {
        String roomId = (String) req.getData();

        boolean success = RoomDAO.updateRoomStatus(roomId, newStatus);

        if (success) {
            ClientHandler handler = AppServer.pendingSellers.get(roomId);
            if (handler != null) {
                String responseAction = newStatus.equals("ACTIVE") ? "CREATE_ROOM_SUCCESS" : "CREATE_ROOM_REJECTED";
                String responseData = "Phòng" + roomId + " đã được " + (newStatus.equals("ACTIVE") ? "Duyệt" : "Từ chối");
                handler.sendResponse(new Request(responseAction, responseData));

                AppServer.pendingSellers.remove(roomId);
            }
        }

        sendResponse(new Request("SUCCESS", "Đã thực hiện quyết định " + newStatus));
    }

    private void handleGetPendingRooms() {
        List<Room> pendingList = RoomDAO.getPendingRooms();
        if (pendingList.isEmpty()) {
            sendResponse(new Request("NOT_EXIST_PENDING_ROOM", null));
        } else {
            sendResponse(new Request("GET_PENDING_ROOMS_SUCCESS", pendingList));
        }
    }

    public String getUserRole() {
        return userRole;
    }

    private void closeEverything() {
        // Xóa username khỏi danh sách khi offline
        AppServer.removeOnlineUser(this.username);

        AppServer.pendingSellers.values().removeIf(handler -> handler.equals(this));

        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.error("lỗi không đóng được file {}", e.getMessage(), e);
        }
    }

    public void sendResponse(Request response) {
        try {
            oos.writeObject(response);
            oos.flush();
        } catch (IOException e) {
            logger.error("Lỗi gửi dữ liệu cho {}: {}", username, e.getMessage());
        }
    }
}
