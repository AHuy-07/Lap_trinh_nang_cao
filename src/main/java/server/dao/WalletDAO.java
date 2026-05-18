package server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import common.models.TransactionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletDAO {

    private static final Logger logger = LoggerFactory.getLogger(WalletDAO.class);

    // Tạo một ổ khóa để đảm bảo khi dùng chung 1 Connection, các luồng không bị race condition
    private static final Object DB_LOCK = new Object();

    public static long getBalance(String username) {
        String sql = "SELECT balance FROM User WHERE username = ?";

        try {
            Connection conn = ConnectDatabase.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong("balance");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy số dư cho user: {}", username, e);
        }
        return -1;
    }

    public static List<TransactionRecord> getHistory(String username) {
        List<TransactionRecord> list = new ArrayList<>();
        String sql = "SELECT amount, type, created_at FROM Money WHERE username = ? ORDER BY created_at DESC";
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

        try {
            Connection conn = ConnectDatabase.getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        long amt = rs.getLong("amount");
                        String rawType = rs.getString("type");
                        String time = rs.getString("created_at");

                        String displayType = rawType.equals("DEPOSIT") ? "Nạp tiền" : "Rút tiền";
                        String displayAmount = (rawType.equals("DEPOSIT") ? "+" : "-") + formatter.format(amt) + " VNĐ";

                        list.add(new TransactionRecord(time, displayType, displayAmount, "Thành công"));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lấy lịch sử giao dịch", e);
        }
        return list;
    }

    public static boolean processTransaction(String username, long amount, String type, String method) {
        String selectSql = "SELECT balance FROM User WHERE username = ? LIMIT 1";
        String updateSql = "UPDATE User SET balance = ? WHERE username = ?";
        String insertLogSql = "INSERT INTO Money (username, amount, type, method) VALUES (?, ?, ?, ?)";

        // Dùng synchronized để khóa Connection lại, người này giao dịch xong người kia mới được vào
        synchronized (ConnectDatabase.getConnection()) {
            Connection conn = null;
            try {
                conn = ConnectDatabase.getConnection();


                conn.setAutoCommit(false);

                long currentBalance = 0;

                // số dư hiện tại
                try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                    selectStmt.setString(1, username);
                    try (ResultSet rs = selectStmt.executeQuery()) {
                        if (rs.next()) {
                            currentBalance = rs.getLong("balance");
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // tính số dư
                long newBalance = currentBalance;
                if (type.equals("DEPOSIT")) {
                    newBalance = currentBalance + amount;
                } else if (type.equals("WITHDRAW")) {
                    if (currentBalance < amount) {
                        conn.rollback();
                        return false;
                    }
                    newBalance = currentBalance - amount;
                } else {
                    conn.rollback();
                    return false;
                }

                // cập nhật số dư
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, newBalance);
                    updateStmt.setString(2, username);
                    updateStmt.executeUpdate();
                }


                try (PreparedStatement insertStmt = conn.prepareStatement(insertLogSql)) {
                    insertStmt.setString(1, username);
                    insertStmt.setLong(2, amount);
                    insertStmt.setString(3, type);
                    insertStmt.setString(4, method);
                    insertStmt.executeUpdate();
                }

                // xác nhận giao dịch
                conn.commit();
                logger.info("Giao dịch thành công: User {} | Type: {} | Amount: {}", username, type, amount);
                return true;

            } catch (SQLException e) {
                logger.error("Lỗi SQL trong quá trình giao dịch.", e);
                try {
                    if (conn != null) conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Lỗi khi Rollback", ex);
                }
                return false;
            } finally {

                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    logger.error("Lỗi khi reset AutoCommit", e);
                }
            }
        }
    }
}