package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class AppServer {
    private static final int PORT = 8080;
    private static final ConcurrentHashMap<String, Set<PrintWriter>> chatRooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat Server dang chay tren port " + PORT + "...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String currentRoom = null;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                username = in.readLine();
                System.out.println(username + " da ket noi.");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/create ")) {
                        handleCreateRoom(message);
                    } else if (message.startsWith("/join ")) {
                        handleJoinRoom(message);
                    } else if (message.equals("/leave")) {
                        handleLeaveRoom();
                    } else if (message.equals("/check")) {
                        handleCheckRoom();
                    } else {
                        broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Loi ket noi voi " + username);
            } finally {
                handleLeaveRoom();
                try { socket.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }

        private void handleCreateRoom(String message) {
            String roomName = message.substring(8).trim();
            if (!chatRooms.containsKey(roomName)) {
                chatRooms.put(roomName, Collections.synchronizedSet(new HashSet<>()));
                out.println("He thong: Tao nhom '" + roomName + "' thanh cong!");
            } else {
                out.println("He thong: Nhom '" + roomName + "' da ton tai.");
            }
        }

        private void handleJoinRoom(String message) {
            String roomName = message.substring(6).trim();
            if (chatRooms.containsKey(roomName)) {
                handleLeaveRoom(); // Thoat nhom hien tai truoc khi vao nhom moi
                currentRoom = roomName;
                chatRooms.get(currentRoom).add(out);
                out.println("He thong: Ban da tham gia nhom '" + roomName + "'.");
                broadcastMessage("He thong: " + username + " da tham gia nhom.");
            } else {
                out.println("He thong: Nhom '" + roomName + "' khong ton tai.");
            }
        }

        private void handleLeaveRoom() {
            if (currentRoom != null && chatRooms.containsKey(currentRoom)) {
                chatRooms.get(currentRoom).remove(out);
                broadcastMessage("He thong: " + username + " da roi khoi nhom.");
                out.println("He thong: Ban da roi nhom '" + currentRoom + "'.");
                currentRoom = null;
            }
        }

        private void handleCheckRoom() {
            if(chatRooms.isEmpty())
                out.println("Khong ton tai phong nao");
            else {
                out.println("Danh sach cac phong la: ");
                for(Map.Entry<String, Set<PrintWriter>> entry : chatRooms.entrySet()) {
                    out.println("Phong: " + entry.getKey() + ", So nguoi trong phong: " + entry.getValue().size());
                }
            }
        }

        private void broadcastMessage(String message) {
            if (currentRoom != null) {
                Set<PrintWriter> roomMembers = chatRooms.get(currentRoom);
                synchronized (roomMembers) {
                    for (PrintWriter writer : roomMembers) {
                        writer.println(message);
                    }
                }
            } else {
                out.println("He thong: Ban chua tham gia nhom nao. Hay dung lenh /join <ten_nhom>");
            }
        }
    }
}

// mvn exec:java -Dexec.mainClass="server.AppServer"
