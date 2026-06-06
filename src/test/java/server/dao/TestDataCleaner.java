package server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestDataCleaner {
    private static final Logger logger = LoggerFactory.getLogger(TestDataCleaner.class);

    // List of tables in order of dependency (foreign keys must be deleted first)
    private static final String[] TABLES = {
            "BidTransaction",
            "AutoBid",
            "Room",
            "Product",
            "User"
    };

    /**
     * Clears all test data from the database by truncating tables
     * Tables are truncated in reverse order of dependencies to avoid foreign key constraints
     */
    public static void clearTestData() {
        Connection connection = ConnectDatabase.getConnection();
        if (connection == null) {
            logger.error("Không thể lấy kết nối database để xóa dữ liệu test");
            return;
        }

        try (Statement statement = connection.createStatement()) {
            // Disable foreign key constraints during truncation
            statement.execute("PRAGMA foreign_keys = OFF");
            logger.debug("Đã tắt ràng buộc foreign key");

            // Delete from each table
            for (String table : TABLES) {
                try {
                    String deleteSQL = "DELETE FROM " + table;
                    int rows = statement.executeUpdate(deleteSQL);
                    logger.debug("Đã xóa {} dòng từ bảng: {}", rows, table);
                } catch (SQLException e) {
                    logger.warn("Không thể xóa dữ liệu từ bảng {}: {}", table, e.getMessage());
                }
            }

            // Re-enable foreign key constraints
            statement.execute("PRAGMA foreign_keys = ON");
            logger.debug("Đã bật lại ràng buộc foreign key");
            logger.info("Đã xóa tất cả dữ liệu test");
        } catch (SQLException e) {
            logger.error("Lỗi khi xóa dữ liệu test: {}", e.getMessage(), e);
        }
    }

    /**
     * Initializes the test database by copying schema from production database
     * This should be called once before running tests.
     */
    public static void initializeTestDatabase() {
        Connection testConnection = ConnectDatabase.getConnection();
        if (testConnection == null) {
            logger.error("Không thể lấy kết nối database test để khởi tạo");
            return;
        }

        try {
            // Check if tables already exist
            DatabaseMetaData dbMeta = testConnection.getMetaData();
            ResultSet tables = dbMeta.getTables(null, null, "%", new String[]{"TABLE"});
            
            int tableCount = 0;
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                if (!tableName.startsWith("sqlite_")) {
                    tableCount++;
                }
            }
            
            if (tableCount > 0) {
                logger.info("Test database đã có {} bảng", tableCount);
                return;
            }
            
            // If no tables exist, copy schema from production database
            logger.info("Test database trống, sẽ copy schema từ production database");
            copySchemaFromProduction(testConnection);
            
        } catch (SQLException e) {
            logger.warn("Lỗi khi kiểm tra schema test database: {}", e.getMessage());
        }
    }

    /**
     * Copy schema from production database to test database
     */
    private static void copySchemaFromProduction(Connection testConnection) {
        Connection prodConnection = null;
        try {
            // Disable foreign keys in test connection
            try (Statement testStmt = testConnection.createStatement()) {
                testStmt.execute("PRAGMA foreign_keys = OFF");
            }
            
            // Connect to production database to get schema
            prodConnection = DriverManager.getConnection("jdbc:sqlite:myDatabase.db");
            
            try (Statement prodStatement = prodConnection.createStatement()) {
                // Get all table creation statements from production database
                ResultSet rs = prodStatement.executeQuery(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
                );
                
                try (Statement testStatement = testConnection.createStatement()) {
                    while (rs.next()) {
                        String createSQL = rs.getString("sql");
                        if (createSQL != null && !createSQL.trim().isEmpty()) {
                            try {
                                // Drop table if it exists first
                                String tableName = extractTableName(createSQL);
                                if (tableName != null) {
                                    testStatement.execute("DROP TABLE IF EXISTS " + tableName);
                                }
                                
                                testStatement.execute(createSQL);
                                logger.debug("Đã tạo bảng: {}", tableName);
                            } catch (SQLException e) {
                                logger.debug("Lỗi khi tạo bảng: {}", e.getMessage());
                            }
                        }
                    }
                    
                    // Re-enable foreign key constraints
                    testStatement.execute("PRAGMA foreign_keys = ON");
                    logger.info("Đã copy schema từ production database thành công");
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi khi copy schema từ production database: {}", e.getMessage(), e);
        } finally {
            if (prodConnection != null) {
                try {
                    prodConnection.close();
                } catch (SQLException e) {
                    logger.error("Lỗi khi đóng production database connection: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * Extract table name from CREATE TABLE statement
     */
    private static String extractTableName(String createSQL) {
        try {
            String[] parts = createSQL.split("\\s+");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("TABLE".equalsIgnoreCase(parts[i])) {
                    // Next part is table name (might have backticks or quotes)
                    String name = parts[i + 1].replaceAll("[`\"]", "");
                    return name;
                }
            }
        } catch (Exception e) {
            logger.warn("Không thể trích xuất tên bảng: {}", e.getMessage());
        }
        return null;
    }
}




