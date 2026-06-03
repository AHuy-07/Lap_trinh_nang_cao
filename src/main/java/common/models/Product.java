package common.models;

public class Product extends Entity {
    private String detail;
    private int isSold = 0;
    private String sellerName;
    private Long winPrice;
    public Product(String id, String type, String name, String detail, String sellerName) {
        super(id, type, name);
        this.detail = detail;
        this.sellerName = sellerName;
        this.winPrice = 0L;
    }

    //Begin: Setter/getter
    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return this.detail;
    }

    public void setIsSold(int isSold) {
        this.isSold = isSold;
    }

    public int getIsSold() {
        return this.isSold;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setWinPrice(Long winPrice) {
        this.winPrice = winPrice;
    }

    public Long getWinPrice() {
        return winPrice;
    }

    //End: Getter/setter
}