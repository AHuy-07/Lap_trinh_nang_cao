package server;

import common.Request;
import common.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.dao.UserDAO;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private String username;

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
            case "CHAT":
                // Logic gui tin nhan
                break;
            default:
                logger.warn("Hành động không xác định {}", action);
        }
    }

    private void handleLogin(Request req) {
        String[] info = (String[]) req.getData();
        User user = UserDAO.login(info[0], info[1]);

        try {
            if (user != null) {
                this.username = info[0];
                oos.writeObject(new Request("LOGIN_SUCCESS", user));
            } else {
                oos.writeObject(new Request("LOGIN_FAIL", "Sai tài khoản hoặc mật khẩu"));
            }
            oos.flush();
        }catch(IOException e) {
            logger.error("Lỗi khi xuất Object");
        }
    }

    private void handleSignUp(Request req) {
        String[] info = (String[]) req.getData();
        User user = UserDAO.signUp(info[0], info[1], info[2]);

        try {
            if (user != null) {
                oos.writeObject(new Request("SIGN_UP_SUCCESS", user));
            } else {
                oos.writeObject(new Request("SIGN_UP_FAIL", "Trùng tên đăng nhập"));
            }
            oos.flush();
        } catch(IOException e) {
            logger.error("Lỗi khi xuất Object");
        }
    }

    private void closeEverything() {
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            logger.error("lỗi không đóng được file {}", e.getMessage(), e);
        }
    }
}
