package server.dao;

import common.models.Bidder;
import common.models.Seller;
import common.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    // Hàm đăng nhập: Trả về đối tượng User (Bidder hoặc Seller)

    private static final Connection connection = ConnectDatabase.getConnection();

    public static User login(String username, String password) {

        String query = "SELECT * FROM User WHERE username = ? AND password = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                int id = resultSet.getInt("userId");
                String role = resultSet.getString("role");
                double balance = resultSet.getDouble("balance");

                logger.info("Người dùng {} xác thực thành công", username);

                if (role.equals("BIDDER")) {
                    return new Bidder(id, username, balance);
                } else if (role.equals("SELLER")) {
                    return new Seller(id, username, balance);
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi đăng nhập", e);
        }
        return null; // Nếu đăng nhập bị lỗi
    }

    public static User signUp(String username, String password, String role) {
        String queryFindUsername = "SELECT 1 FROM User WHERE username = ? LIMIT 1"; // Chỉ tìm 1 lần
        String queryInsertValue = "INSERT INTO User (username, password, role) VALUES(?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(queryFindUsername)) {
            preparedStatement.setString(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return null;
                }
            }
            try (PreparedStatement insertInfo = connection.prepareStatement(queryInsertValue)) {
                insertInfo.setString(1, username);
                insertInfo.setString(2, password);
                insertInfo.setString(3, role);

                int insertStatus = insertInfo.executeUpdate();
                if (insertStatus > 0) {
                    try (ResultSet resultSet = insertInfo.getGeneratedKeys()) {
                        if (resultSet.next()) {
                            int id = resultSet.getInt(1);
                            /*
                            - Phải để là số 1 để lấy cột 1, chứ không phải để là userId
                             */

                            if (role.equals("BIDDER")) {
                                return new Bidder(id, username, 0.0);
                            }else if (role.equals("SELLER")) {
                                return new Seller(id, username, 0.0);
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi đăng kí", e);
        }
        return null;
    }
}
