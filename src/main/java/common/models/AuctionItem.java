package common.models;

public class AuctionItem {
    //Lớp này sẽ là wrapper cho các sản phẩm đưa vào đấu giá

    private String auctionId;
    private Entity product; 
    private String sellerName;
    private double startingPrice;
    private boolean isSold;

    public AuctionItem(String auctionId, Entity product, String sellerName, double startingPrice) {
        this.auctionId = auctionId;
        this.product = product;
        this.sellerName = sellerName;
        this.startingPrice = startingPrice;
        this.isSold = false;
    }

    public void displayAuctionInfo() {
        System.out.println("=== AUCTION: " + auctionId + " ===");
        System.out.println("Seller: " + sellerName + " | Start Price: " + startingPrice);
        
        if (product instanceof DisplayDetails) {
            ((DisplayDetails) product).displayProductin4();
        }
    }

    //Begin: Setter/getter
    public String getAuctionId() {
        return auctionId;
    }

    public Entity getProduct() {
        return product;
    }

    public String getSellerName() {
        return sellerName;
    }
    public void setStartingPrice(double startPrice) {
        this.startingPrice = startPrice;
    }

    public double getStartingPrice() {
        return this.startingPrice;
    }

    public void setIsSold(boolean isSold) {
        this.isSold = isSold;
    }

    public boolean getIsSold() {
        return this.isSold;
    }
    //End: Getter/setter
}
