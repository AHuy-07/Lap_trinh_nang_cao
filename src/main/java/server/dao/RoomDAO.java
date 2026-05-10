package server.dao;

import common.models.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class RoomDAO {
    private static final Logger logger = LoggerFactory.getLogger(RoomDAO.class);

    private static final Connection connection = ConnectDatabase.getConnection();

    public static Room createRoom (Room room) {
        room.setBidStep(Room.calculateDefaultBidStep(room.getStartingPrice()));

        String queryFindRoomId = "SELECT 1 FROM Room WHERE roomId = ? LIMIT 1";
        String queryInsertValue = "INSERT INTO Room (roomId, roomName, status, productId, sellerName, startingPrice, beginTime) VALUES(?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryFindRoomId)) {
            preparedStatement.setString(1, room.getRoomId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return null;
                }
            }

            try (PreparedStatement insertInfo = connection.prepareStatement(queryInsertValue)) {
                insertInfo.setString(1, room.getRoomId());
                insertInfo.setString(2, room.getRoomName());
                insertInfo.setString(3, "PENDING");
                insertInfo.setString(4, room.getProductId());
                insertInfo.setString(5, room.getSellerName());
                insertInfo.setLong(6, room.getStartingPrice());
                insertInfo.setString(7, room.getBeginTime());

                int insertStatus = insertInfo.executeUpdate();

                if (insertStatus > 0) {
                    return room;
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi tạo phòng", e);
        }
        return null;
    }

    public static List<Room> getPendingRooms() {
        List<Room> list = new ArrayList<>();

        String query = "SELECT * FROM Room WHERE status = 'PENDING'";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Room room = new Room(
                        resultSet.getString("roomId"),
                        resultSet.getString("roomName"),
                        resultSet.getString("productId"),
                        resultSet.getString("sellerName"),
                        resultSet.getLong("startingPrice"),
                        resultSet.getString("beginTime")
                );
                room.setStatus(resultSet.getString("status"));
                list.add(room);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi tìm phòng", e);
        }
        return list;
    }

    public static boolean updateRoomStatus(String roomId, String newStatus) {
        String query = "UPDATE Room SET status = ? WHERE roomId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(2, roomId);
            preparedStatement.setString(1, newStatus);

            int result = preparedStatement.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi cập nhật trạng thái phòng {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    public static List<Room> getRoomsBySeller(String username) {
        List<Room> list = new ArrayList<>();

        String query = "SELECT * FROM Room where sellerName = ? " +
                "ORDER BY " +
                "CASE status " +
                "WHEN 'PENDING' THEN 1 " +
                "ELSE 2 " +
                "END ASC, " +
                "roomId DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Room room = new Room(
                        resultSet.getString("roomId"),
                        resultSet.getString("roomName"),
                        resultSet.getString("productId"),
                        username,
                        resultSet.getLong("startingPrice"),
                        resultSet.getString("beginTime")
                );
                room.setStatus(resultSet.getString("status"));
                list.add(room);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy phòng của {}: {}", username, e.getMessage());
        }
        return list;
    }

    public static List<Room> getActiveRooms() {
        List<Room> list = new ArrayList<>();

        String query = "SELECT * FROM Room WHERE status = 'ACTIVE' ORDER BY roomId DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Room room = mapResultSetToRoom(resultSet);
                list.add(room);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy danh sách phòng ACTIVE", e);
        }

        return list;
    }

    public static Room getRoomById(String roomId) {
        String query = "SELECT * FROM Room WHERE roomId = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, roomId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToRoom(resultSet);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy phòng {}", roomId, e);
        }

        return null;
    }

    private static Room mapResultSetToRoom(ResultSet resultSet) throws SQLException {
        Room room = new Room(
                resultSet.getString("roomId"),
                resultSet.getString("roomName"),
                resultSet.getString("productId"),
                resultSet.getString("sellerName"),
                resultSet.getLong("startingPrice"),
                resultSet.getString("beginTime")
        );

        room.setStatus(resultSet.getString("status"));
        room.setEndTime(resultSet.getString("endTime"));
        room.setWinPrice(resultSet.getLong("winPrice"));
        room.setWinnerUsername(resultSet.getString("winnerUsername"));

        try {
            room.setBidStep(resultSet.getLong("bidStep"));
        } catch (SQLException ignored) {
            room.setBidStep(10000);
        }

        return room;
    }
}
