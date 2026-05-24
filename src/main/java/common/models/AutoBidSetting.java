package common.models;

import java.io.Serializable;

public class AutoBidSetting implements Serializable {
    private String roomId;
    private String username;
    private long maxPrice;

    public AutoBidSetting() {}

    public AutoBidSetting(String roomId, String username, long maxPrice) {
        this.roomId = roomId;
        this.username = username;
        this.maxPrice = maxPrice;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setMaxPrice(long maxPrice) {
        this.maxPrice = maxPrice;
    }

    public long getMaxPrice() {
        return maxPrice;
    }
}
