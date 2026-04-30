package client;

/*
* Tác dụng của class này:
- Khi chạy trực tiếp một class kế thừa Application, máy ảo Java(JVM) sẽ ưu tiên tìm Javafx ở
  tầng hệ thống trước khi nhìn vào câú hình Maven. Không thấy là nó báo lỗi luôn
- Nếu ta tạo thêm 1 class phụ, JVM sẽ vào Pom để kiểm tra luôn
 */

public class Main {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
