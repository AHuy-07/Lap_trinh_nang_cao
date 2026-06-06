package server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectDatabase {
    private static final Logger logger = LoggerFactory.getLogger(ConnectDatabase.class);
    private static volatile Connection connection = null;
    private static volatile boolean isTestMode = false;
    private static final String PROD_DB = "myDatabase.db";
    private static final String TEST_DB = "database_test.db";

    private ConnectDatabase() {}

    public static void setTestMode(boolean testMode) {
        isTestMode = testMode;
        // Close existing connection to force reconnection with new database
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                logger.info("Đã đóng kết nối database cũ");
            } catch (SQLException e) {
                logger.error("Lỗi khi đóng kết nối database: {}", e.getMessage(), e);
            }
        }
    }

    public static boolean isTestMode() {
        return isTestMode;
    }

    public static Connection getConnection() {
        if (connection == null) {
            synchronized (ConnectDatabase.class) {
                if (connection == null) {
                    try {
                        String dbFile = isTestMode ? TEST_DB : PROD_DB;
                        String url = "jdbc:sqlite:" + dbFile;
                        connection = DriverManager.getConnection(url);
                        String mode = isTestMode ? "TEST" : "PRODUCTION";
                        logger.info("Kết nối {} database thành công ({})", mode, dbFile);
                    } catch (SQLException e) {
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
