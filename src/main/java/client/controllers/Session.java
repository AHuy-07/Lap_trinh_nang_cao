package client.controllers;

import client.controllers.sellerController.SellerDashboardController;
import common.Request;
import common.models.User;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.function.Consumer;

public class Session {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;
    private static volatile Session instance;

    private User currentUser;
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    // Biến để lưu callback của yêu cầu hiện tại (Ví dụ: Login, CreateRoom)
    private Consumer<Request> currentResponseCallback;

    private Consumer<Request> realtimeBidCallback;

    // Tham chiếu đến đối tượng để dùng In-app Notifi
    private SellerDashboardController sellerDashboardController;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            synchronized (Session.class) {
                if (instance == null) {
                    instance = new Session();
                }
            }
        }
        return instance;
    }

    public void setSellerDashboardController(SellerDashboardController controller) {
        this.sellerDashboardController = controller;
    }

    public void sendRequest(Request request, Consumer<Request> callback) {
        try {
            connectIfNeeded();
            this.currentResponseCallback = callback; // lưu lại để dùng khi có tin về
            oos.writeObject(request);
            oos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setRealtimeBidCallback(Consumer<Request> realtimeBidCallback) {
        this.realtimeBidCallback = realtimeBidCallback;
    }

    public void clearRealtimeBidCallback() {
        this.realtimeBidCallback = null;
    }

    private synchronized void connectIfNeeded() throws Exception {
        if (socket == null || socket.isClosed()) {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
            startListening();
        }

    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (socket != null && !socket.isClosed()) {
                    Object received = ois.readObject();
                    if (received instanceof Request) {
                        Request response = (Request) received;
                        handleResponse(response);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleResponse(Request response) {
        String action = response.getAction();

        if (action.equals("CREATE_ROOM_SUCCESS") || action.equals("CREATE_ROOM_REJECTED")) {
            Platform.runLater(() -> {
                if (sellerDashboardController != null) {
                    String msg = action.equals("CREATE_ROOM_SUCCESS") ? "Phòng của bạn đã được duyệt!" : "Phòng của bạn bị từ chối!";
                    String color = action.equals("CREATE_ROOM_SUCCESS") ? "#2ecc71" : "#e74c3c";

                    sellerDashboardController.showInAppNotification(msg, color);
                    sellerDashboardController.loadMyRooms();
                }
            });
        } else if (action.equals("NEW_BID") || action.equals("AUCTION_ENDED")) {
            Platform.runLater(() -> {
                if (realtimeBidCallback != null) {
                    realtimeBidCallback.accept(response);
                }
            });
        } else if (currentResponseCallback != null) {
            Platform.runLater(() -> {
                currentResponseCallback.accept(response);
            });
        }
    }

    public void setConnection(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        this.socket = socket;
        this.oos = oos;
        this.ois = ois;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() { return currentUser; }

    public String getCurrentUsername() {
        return currentUser.getUsername();
    }
}