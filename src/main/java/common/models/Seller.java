package common.models;

import java.util.ArrayList;
import java.util.List;

public class Seller extends User{
    private List<AuctionItem> sellingList = new ArrayList<>();
    private double balance = 0.0;

    public Seller(String loginUsername, String password) {
        super(loginUsername, password);
    }

    public void addSellingItemList(AuctionItem item) {
        sellingList.add(item);
    }

    public void sellerDeposit(double money) {
        if(money>0){
            balance += money;
        }else{throw new IllegalArgumentException("So tien phai lon hon 0");}
    }

    public List<AuctionItem> getSellingItems() {
        return new ArrayList<>(this.sellingList);
        //Dam bao an toan cho danh sach san pham dau gia
    }

    public double getBalance(){
        return balance;
    }
    //Phuong thuc sendRequest: LAHUY code socket
}
