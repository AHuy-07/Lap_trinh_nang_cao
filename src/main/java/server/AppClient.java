package server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class AppClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Nhap ten hien thi cua ban: ");
        String username = scanner.nextLine();

        System.out.print("Chon vai tro (1: Admin, 2: User): ");
        String roleChoice = scanner.nextLine();
        boolean isAdmin = roleChoice.equals("1");

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println(username + (isAdmin ? " (Admin)" : ""));

            System.out.println("==================================================");
            System.out.println("Da ket noi den Server!");
            if (isAdmin) {
                System.out.println("Lenh Admin: /create <ten_nhom> de tao nhom moi.");
            }
            System.out.println("Lenh chung: /join <ten_nhom> (vao nhom), /leave (roi nhom), /check (kiem tra phong)");
            System.out.println("==================================================");

            Thread readThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Da ngat ket noi voi may chu.");
                }
            });
            readThread.start();

            while (true) {
                String input = scanner.nextLine();

                if (input.startsWith("/create ") && !isAdmin) {
                    System.out.println("Loi: Chi Admin moi co quyen tao nhom chat.");
                    continue;
                }

                if (input.equalsIgnoreCase("exit")) {
                    break;
                }
                out.println(input);
            }

        } catch (IOException e) {
            System.out.println("Khong the ket noi den may chu: " + e.getMessage());
        }
    }
}

// mvn exec:java -Dexec.mainClass="server.ChatClient"