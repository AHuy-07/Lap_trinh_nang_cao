package client.controllers;

import common.Request;
import common.models.User;
import javafx.concurrent.Task;

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

    public void sendRequest(Request request, Consumer<Request> onSuccess, Consumer<Throwable> onFailed) {
        Task<Request> task = new Task<Request>() {
            @Override
            protected Request call() throws Exception {
                // 1. Mở kết nối nếu đây là lần đầu
                if (socket == null || socket.isClosed()) {
                    socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                    oos = new ObjectOutputStream(socket.getOutputStream());
                    ois = new ObjectInputStream(socket.getInputStream());
                }

                // 2. Gửi Request đi
                oos.writeObject(request);
                oos.flush();

                // 3. Đợi và trả về
                return (Request) ois.readObject();
            }
        };

        task.setOnSucceeded(e -> {
            Request response = task.getValue();
            onSuccess.accept(response);
        });

        task.setOnFailed(e -> {
            Throwable error = task.getException();
            onFailed.accept(error);
        });

        new Thread(task).start();
    }

    public void setConnection(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
        this.socket = socket;
        this.oos = oos;
        this.ois = ois;
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
}
