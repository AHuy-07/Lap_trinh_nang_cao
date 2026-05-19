package common.models;

import java.io.Serializable;

public class TransactionRecord implements Serializable {
    private String date;
    private String type;
    private String amount;
    private String status;

    public TransactionRecord(String date, String type, String amount, String status) {
        this.date = date;
        this.type = type;
        this.amount = amount;
        this.status = status;
    }

    public String getDate() { return date; }
    public String getType() { return type; }
    public String getAmount() { return amount; }
    public String getStatus() { return status; }
}