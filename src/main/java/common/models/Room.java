package common.models;

import java.io.Serializable;

public class Room implements Serializable {
    private static final long serialVersionUID = 1L;

    private String roomId;
    private String roomName;
    private String status; // PENDING, ACTIVE, CLOSED
    private String productId;
    private long startingPrice;
    private String beginTime;
    private String endTime;
    private long winPrice;
    private String winnerUsername;

    public Room() {}

    public Room(String roomId, String roomName, String productId, long startingPrice) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.productId = productId;
        this.startingPrice = startingPrice;
        this.status = "PENDING"; // Mặc định khi mới tạo
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public long getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(long startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public long getWinPrice() {
        return winPrice;
    }

    public void setWinPrice(long winPrice) {
        this.winPrice = winPrice;
    }

    public String getWinnerUsername() {
        return winnerUsername;
    }

    public void setWinnerUsername(String winnerUsername) {
        this.winnerUsername = winnerUsername;
    }
}
