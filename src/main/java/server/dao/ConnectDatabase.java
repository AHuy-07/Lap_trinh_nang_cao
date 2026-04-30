package server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectDatabase {
    private static final Logger logger = LoggerFactory.getLogger(ConnectDatabase.class);
    private static volatile Connection connection = null;

    private ConnectDatabase() {}

    public static Connection getConnection() {
        if (connection == null) {
            synchronized (ConnectDatabase.class) {
                if (connection == null) {
                    try {
                        Class.forName("org.sqlite.JDBC");

                        String url = "jdbc:sqlite:myDatabase.db";
                        connection = DriverManager.getConnection(url);
                        logger.info("Kết nối database thành công");
                    } catch (ClassNotFoundException | SQLException e) {
                        logger.error("Lỗi khi khởi tạo kết nối database: {}", e.getMessage(), e);
                    }
                }
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                logger.info("Đã đóng kết nối database");
            } catch (SQLException e) {
                logger.error("Lỗi khi đóng kết nối database: {}", e.getMessage(), e);
            }
        }
    }
}
