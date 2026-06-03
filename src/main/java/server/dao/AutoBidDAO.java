package server.dao;
import common.models.AutoBidSetting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class AutoBidDAO {
    private static final Logger logger = LoggerFactory.getLogger(AutoBidDAO.class);
    private static final Connection connection = ConnectDatabase.getConnection();

    public static boolean saveAutoBid(String roomId, String username, long maxPrice, long createAt) {
        synchronized (connection) {
            String sql = """
                    INSERT INTO AutoBid(roomId, username, maxPrice, createAt) VALUES(?, ?, ?, ?) 
                    ON CONFLICT(roomId, username) DO UPDATE SET maxPrice = ?
                    """;
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, roomId);
                preparedStatement.setString(2, username);
                preparedStatement.setLong(3, maxPrice);
                preparedStatement.setLong(4, createAt);
                preparedStatement.setLong(5, maxPrice);
                return preparedStatement.executeUpdate() > 0;
            } catch (SQLException e) {
                logger.error("Lỗi khi lưu thông tin autoBid của người dùng {}, ở phòng {}", username, roomId, e);
                return false;
            }
        }
    }

    public static boolean removeAutoBid(String roomId, String username) {
        synchronized (connection) {
            String sql = "DELETE FROM AutoBid WHERE roomId = ? AND username = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, roomId);
                preparedStatement.setString(2, username);
                return preparedStatement.executeUpdate() > 0;
            } catch (SQLException e) {
                logger.error("Lỗi khi xóa thông tin autoBid của người dùng {}, ở phòng {}", username, roomId, e);
                return false;
            }
        }
    }

    public static List<AutoBidSetting> getAutoBidders(String roomId) {
        List<AutoBidSetting> list = new ArrayList<>();
        String sql = "SELECT username, maxPrice, createAt FROM AutoBid WHERE roomId = ?";
        synchronized (connection) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, roomId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        list.add(new AutoBidSetting(roomId, resultSet.getString("username"), resultSet.getLong("maxPrice"), resultSet.getLong("createAt")));
                    }
                }
            } catch (SQLException e) {
                logger.error("Lỗi khi lấy danh sách autoBids của phòng {}", roomId);
            }
        }

        return list;
    }

    public static long checkAutoBidStatus(String roomId, String username) {
        String sql = "SELECT maxPrice FROM AutoBid WHERE roomId = ? AND username = ? LIMIT 1";
        synchronized (connection) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                preparedStatement.setString(1, roomId);
                preparedStatement.setString(2, username);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        logger.info("Lấy autobid thanhf công");
                        return resultSet.getLong("maxPrice");
                    }
                }
            } catch (SQLException e) {
                logger.error("Lỗi khi lấy danh sách autoBids của phòng {}", roomId);
            }
        }
        return -1;
    }

    // 31/5, thêm phần kiểm tra xem giá đặt cao nhất trong phòng, để tránh trùng autobid
//    public static Set<Long> getMaxAutoBidPriceInRoom(String roomId) {
//        String sql = "SELECT DISTINCT maxPrice FROM AutoBid WHERE roomId = ?";
//        Set<Long> maxPriceSet = new HashSet<>();
//        synchronized (connection) {
//            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
//                preparedStatement.setString(1, roomId);
//                try (ResultSet resultSet = preparedStatement.executeQuery()) {
//                    while (resultSet.next()) {
//                        long maxPrice = resultSet.getLong("maxPrice");
//                        maxPriceSet.add(maxPrice);
//                    }
//                }
//            } catch (SQLException e) {
//                logger.error("Lỗi khi lấy max price của AutoBid trong phòng {}", roomId, e.getMessage());
//            }
//        }
//        return maxPriceSet;
//    }
}
