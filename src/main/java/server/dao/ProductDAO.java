package server.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductDAO {
    private static final Logger logger = LoggerFactory.getLogger(ProductDAO.class);

    private static final Connection connection = ConnectDatabase.getConnection();

    public static boolean isProductValid(String productId) {
        String sql = "SELECT isSold FROM Product WHERE productId = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, productId);

            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int isSold = resultSet.getInt("isSold");
                if (isSold == 0) return true;
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi tìm thông tin sản phẩm", e);
        }
        return false;
    }
}
