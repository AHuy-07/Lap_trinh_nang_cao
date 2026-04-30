package common.models;

import java.util.HashMap;
import java.util.Map;

public class Seller extends User{
    Map<String, Integer> sellingItemList = new HashMap<>();
    int balance = 0;

    public Seller(String loginUsername, String password) {
        super(loginUsername, password);
    }

    public void addSellingItemList(String item) {
        sellingItemList.put(item, sellingItemList.getOrDefault(item, 0) + 1);
    }

    public void sellerDeposit(int money) {
        balance += money;
    }

    //Phuong thuc sendRequest: LAHUY code socket

}
