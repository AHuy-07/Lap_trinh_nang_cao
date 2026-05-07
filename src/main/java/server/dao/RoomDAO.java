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
        String queryFindRoomId = "SELECT 1 FROM Room WHERE roomId = ? LIMIT 1";
        String queryInsertValue = "INSERT INTO Room (roomId, roomName, status, productId, startingPrice, beginTime) VALUES(?, ?, ?, ?, ?, ?)";

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
                insertInfo.setLong(5, room.getStartingPrice());
                insertInfo.setString(6, room.getBeginTime());

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
                        resultSet.getLong("startingPrice")
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
        String query = "UPDATE Room WHERE roomId = ? SET status = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, roomId);
            preparedStatement.setString(2, newStatus);

            int result = preparedStatement.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi cập nhật trạng thái phòng {}: {}", roomId, e.getMessage());
            return false;
        }
    }
}
