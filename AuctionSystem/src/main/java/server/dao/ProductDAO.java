package server.dao;

import common.models.Product;
import common.models.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

    public static Product addProducts(String id, String name, String type, String detail, String sellerName){
        String findProducts = "SELECT * FROM Product WHERE productId = ? LIMIT 1";
        String insertValue = "INSERT INTO Product (productId, productName, type, details, sellerUsername) VALUES(?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(findProducts)){
            preparedStatement.setString(1, id);
            try(ResultSet resultSet = preparedStatement.executeQuery()){
                if(resultSet.next()){
                    return null;
                }
            }
            try(PreparedStatement insert = connection.prepareStatement(insertValue)){
                insert.setString(1, id);
                insert.setString(2, name);
                insert.setString(3, type);
                insert.setString(4, detail);
                insert.setString(5, sellerName);

                int insertStatus = insert.executeUpdate();
                if(insertStatus > 0){
                    return new Product(id, name, type, detail, sellerName);
                }
            }
        } catch(SQLException e){
            logger.error("Lỗi SQL thêm sản phẩm", e);
        } return null;
    }

    public static String getLastProductId() {
        String sql = "SELECT productId FROM Product ORDER BY LENGTH(productId) DESC, productId DESC LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            if (resultSet.next()) {
                return resultSet.getString("productId");
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy ID cuối cùng", e);
        }
        return null;
    }

    public static List<Product> getProductsBySeller(String username) {
        List<Product> list = new ArrayList<>();

        String query = "SELECT * FROM Product where sellerUsername = ? " +
                "ORDER BY " +
                "CASE isSold " +
                "WHEN 1 THEN 1 " +
                "WHEN 0 THEN 2 " +
                "ELSE 3 " +
                "END ASC, " +
                "productId DESC";

        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Product Product = new Product(
                        resultSet.getString("productId"),
                        resultSet.getString("type"),
                        resultSet.getString("productName"),
                        resultSet.getString("details"),
                        resultSet.getString("sellerUsername")
                );
                Product.setIsSold(resultSet.getInt("isSold"));
                list.add(Product);
            }
        } catch (SQLException e) {
            logger.error("Lỗi SQL khi lấy phòng của {}: {}", username, e.getMessage());
        }
        return list;
    }

    public static String generateNewId() {
        String lastId = ProductDAO.getLastProductId();

        if (lastId == null || lastId.isEmpty()) {
            return "P_001";
        }

        try {
            String numberPart = lastId.substring(2);

            int number = Integer.parseInt(numberPart);
            number++;
            return String.format("P_%03d", number);

        } catch (NumberFormatException e) {

            System.err.println("Lỗi định dạng không hợp lệ: " + lastId);
            return "P_ERROR";
        }
    }
}
