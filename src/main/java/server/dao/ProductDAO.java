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
                /*
                - Số 0 là chưa được động vào
                - Số 1 là đã được kiểu lấy vào phòng. Trường hợp này sẽ không được lấy ra nữa
                - Số 2 là đã bán
                 */
                int isSold = resultSet.getInt("isSold");
                if (isSold == 0) return true;
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi tìm thông tin sản phẩm", e);
        }
        return false;
    }

    public static void updateProductStatus(String productId, int status) {
        String query = "UPDATE Product SET isSold = ? WHERE productId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(2, productId);
            preparedStatement.setInt(1, status);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi cập nhật trạng thái sản phẩm", e);
        }
    }

    public static void updateRoomId(String productId, String roomId) {
        String query = "UPDATE Product SET roomId = ? WHERE productId = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)){
            preparedStatement.setString(2, productId);
            preparedStatement.setString(1, roomId);

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi cập nhật trạng thái sản phẩm", e);
        }
    }
}
