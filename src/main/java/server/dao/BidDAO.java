package server.dao;

import common.models.BidTransaction;
import common.models.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BidDAO {
    private static final Logger logger = LoggerFactory.getLogger(BidDAO.class);
    private static final Connection connection = ConnectDatabase.getConnection();

    public static boolean placeBid(Room room, String oldBidderUsername, String newBidderUsername, long bidAmount, long oldBidderBalance, long newBidderBalance) {
        synchronized (connection) {
            String transactionId = UUID.randomUUID().toString();
            String bidTime = java.time.LocalDateTime.now().toString();

            String insertBid = """
                INSERT INTO BidTransaction(transactionId, roomId, bidderUsername, bidAmount, bidTime)
                VALUES (?, ?, ?, ?, ?)
                """;

            String updateRoom = """
                UPDATE Room
                SET winPrice = ?, winnerUsername = ?
                WHERE roomId = ?
                """;

            String updateBalanceWinner = """
                UPDATE User
                SET balance = ?
                WHERE username = ?
                """;


            try {
                connection.setAutoCommit(false);

                try (PreparedStatement insertStatement = connection.prepareStatement(insertBid);
                     PreparedStatement updateStatement = connection.prepareStatement(updateRoom);
                     PreparedStatement updateBalanceOldWinner = connection.prepareStatement(updateBalanceWinner);
                     PreparedStatement updateBalanceNewWinner = connection.prepareStatement(updateBalanceWinner)) {

                    insertStatement.setString(1, transactionId);
                    insertStatement.setString(2, room.getRoomId());
                    insertStatement.setString(3, newBidderUsername);
                    insertStatement.setLong(4, bidAmount);
                    insertStatement.setString(5, bidTime);
                    insertStatement.executeUpdate();

                    updateStatement.setLong(1, bidAmount);
                    updateStatement.setString(2, newBidderUsername);
                    updateStatement.setString(3, room.getRoomId());
                    updateStatement.executeUpdate();

                    if (oldBidderUsername != null && !oldBidderUsername.isBlank()) {
                        updateBalanceOldWinner.setLong(1, oldBidderBalance);
                        updateBalanceOldWinner.setString(2, oldBidderUsername);
                        updateBalanceOldWinner.executeUpdate();
                    }

                    updateBalanceNewWinner.setLong(1, newBidderBalance);
                    updateBalanceNewWinner.setString(2, newBidderUsername);
                    updateBalanceNewWinner.executeUpdate();

                    connection.commit();
                    return true;
                }
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    logger.error("Lỗi rollback khi đặt giá", rollbackException);
                }

                logger.error("Lỗi SQL khi đặt giá", e);
                return false;
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Lỗi bật lại auto commit", e);
                }
            }
        }

    }

    public static long getCurrentPrice(String roomId) {
        String query = """
                SELECT startingPrice, winPrice
                FROM Room
                WHERE roomId = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, roomId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    long startingPrice = resultSet.getLong("startingPrice");
                    long winPrice = resultSet.getLong("winPrice");

                    return Math.max(startingPrice, winPrice);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy giá hiện tại của phòng {}", roomId, e);
        }

        return 0;
    }

    public static BidTransaction getLatestBid(String roomId) {
        String query = """
                SELECT *
                FROM BidTransaction
                WHERE roomId = ?
                ORDER BY bidAmount DESC, bidTime DESC
                LIMIT 1
                """;

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, roomId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new BidTransaction(
                            resultSet.getString("transactionId"),
                            resultSet.getString("roomId"),
                            resultSet.getString("bidderUsername"),
                            resultSet.getLong("bidAmount"),
                            resultSet.getString("bidTime")
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy bid mới nhất của phòng {}", roomId, e);
        }

        return null;
    }

    public static List<BidTransaction> getBidHistory(Room room) {
        List<BidTransaction> list = new ArrayList<>();
        String query = """
                SELECT bidderUsername, bidAmount, bidTime\s
                FROM BidTransaction\s
                WHERE roomId = ?\s
                ORDER BY bidTime DESC;
                """;
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, room.getRoomId());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    BidTransaction bid = new BidTransaction(
                            null,
                            null,
                            resultSet.getString("bidderUsername"),
                            resultSet.getLong("bidAmount"),
                            resultSet.getString("bidTime")
                    );
                    list.add(bid);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy lịch sử đấu giá của phòng {}", room.getRoomId(), e);
        }
        return list;
    }
}