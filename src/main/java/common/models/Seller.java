package common.models;

import java.util.HashMap;
import java.util.Map;

public class Seller extends User{
    Map<String, Integer> sellingProductList = new HashMap<>();
    long balance = 0;

    public Seller(int id, String loginUsername, long balance) {
        super(id, loginUsername, "SELLER", balance);
    }

    public void addSellingProductList(String Product) {
        sellingProductList.put(Product, sellingProductList.getOrDefault(Product, 0) + 1);
    }

    public void sellerDeposit(int money) {
        balance += money;
    }
}
