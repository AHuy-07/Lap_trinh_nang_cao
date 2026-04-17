package common.models;

abstract class Item extends Entity {
    private String detail;
    private double startPrice;
    private boolean isSold = false;
    public Item(String id, String name, String detail, double startPrice) {
        super(id, name);
        this.detail = detail;
        this.startPrice = startPrice;
    }

    //Begin: Setter/getter
    public void setDetail(String detail) {
        this.detail = detail;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public String getDetail() {
        return this.detail;
    }

    public double getStartPrice() {
        return this.startPrice;
    }

    public void setIsSold(boolean isSold) {
        this.isSold = isSold;
    }

    public boolean getIsSold() {
        return this.isSold;
    }
    //End: Getter/setter
}
