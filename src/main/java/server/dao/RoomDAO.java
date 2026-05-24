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
        String newId = RoomDAO.generateNewId();
        room.setRoomId(newId);

        room.setBidStep(Room.calculateDefaultBidStep(room.getStartingPrice()));

        String queryFindRoomId = "SELECT 1 FROM Room WHERE roomId = ? LIMIT 1";
        String queryInsertValue = "INSERT INTO Room (roomId, roomName, status, productId, sellerName, startingPrice, beginTime, endTime, winPrice) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
                insertInfo.setString(8, room.getEndTime());
                insertInfo.setLong(9, room.getWinPrice());

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
                        resultSet.getString("beginTime"),
                        resultSet.getString("endTime")
                );
                room.setStatus(resultSet.getString("status"));
                list.add(room);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi tìm phòng", e);
        }
        return list;
    }
// them 1 phut
    public static boolean updateEndTime(String roomId, String newEndTime) {
        String query = "UPDATE Room SET endTime = ? WHERE roomId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, newEndTime);
            preparedStatement.setString(2, roomId);
            int result = preparedStatement.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi cập nhật endTime phòng {}: {}", roomId, e.getMessage());
            return false;
        }
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
                "WHEN 'ACTIVE' THEN 1 " +
                "WHEN 'PENDING' THEN 2 " +
                "ELSE 3 " +
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
                        resultSet.getString("beginTime"),
                        resultSet.getString("endTime")
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

    public static List<Room> getRoomsForBidder() {
        List<Room> list = new ArrayList<>();

        String query = "SELECT r.*, p.productName FROM Room r " +
                "LEFT JOIN Product p ON r.productId = p.productId " +
                "WHERE r.status IN ('ACTIVE', 'CLOSED') " +
                "ORDER BY CASE r.status WHEN 'ACTIVE' THEN 1 WHEN 'CLOSED' THEN 2 END, " +
                "r.roomId DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Room room = mapResultSetToRoom(resultSet);
                try {
                    room.setProductName(resultSet.getString("productName"));
                } catch (SQLException ignored) {}
                list.add(room);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy danh sách phòng cho bidder", e);
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
                resultSet.getString("beginTime"),
                resultSet.getString("endTime")
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

    //lấy id cuối cùng của 1 phòng
    public static String getLastRoomId(){
        String sql = "SELECT roomId FROM Room ORDER BY LENGTH(roomId) DESC, roomId DESC LIMIT 1";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getString("roomId");
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy ID cuối cùng", e);
        }
        return null;
    }
    // tự dộng thêm id vào cho phòng
    public static String generateNewId() {
        String lastId = RoomDAO.getLastRoomId();

        if (lastId == null || lastId.isEmpty()) {
            return "R_001";
        }

        try {
            String numberPart = lastId.substring(2);

            int number = Integer.parseInt(numberPart);
            number++;
            return String.format("R_%03d", number);

        } catch (NumberFormatException e) {

            System.err.println("Lỗi định dạng không hợp lệ: " + lastId);
            return "R_ERROR";
        }
    }

    //hàm đưa thông tin của phòng chiến thắng về server xử lý (23/05)
    public static List<Room> getWonRoomsByUsername(String username) {
        List<Room> wonRooms = new ArrayList<>();
        String sql = "SELECT r.*, p.productName \n" +
                "FROM Room r \n" +
                "LEFT JOIN Product p ON r.productId = p.productId \n" +
                "WHERE r.status = 'CLOSED' AND r.winnerUsername = ?";
        Connection conn = ConnectDatabase.getConnection();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Room room = new Room();
                    room.setRoomId(rs.getString("roomId"));
                    room.setRoomName(rs.getString("roomName"));
                    room.setStatus(rs.getString("status"));
                    room.setProductId(rs.getString("productId"));
                    room.setSellerName(rs.getString("sellerName"));
                    room.setStartingPrice(rs.getLong("startingPrice"));
                    room.setBeginTime(rs.getString("beginTime"));
                    room.setEndTime(rs.getString("endTime"));
                    room.setWinPrice(rs.getLong("winPrice"));
                    room.setWinnerUsername(rs.getString("winnerUsername"));
                    room.setProductName(rs.getString("productName"));

                    wonRooms.add(room);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi truy vấn danh sách phòng đã chiến thắng của: " + username);
            e.printStackTrace();
        }

        return wonRooms;
    }
}