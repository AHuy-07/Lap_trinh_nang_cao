package common.models;

import java.io.Serializable;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String roomId;
    private String bidderUsername;
    private long bidAmount;
    private String bidTime;

    public BidTransaction() {
    }

    public BidTransaction(String transactionId, String roomId, String bidderUsername, long bidAmount, String bidTime) {
        this.transactionId = transactionId;
        this.roomId = roomId;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public void setBidderUsername(String bidderUsername) {
        this.bidderUsername = bidderUsername;
    }

    public long getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(long bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getBidTime() {
        return bidTime;
    }

    public void setBidTime(String bidTime) {
        this.bidTime = bidTime;
    }
}