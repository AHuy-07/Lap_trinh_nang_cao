package common.models;

import java.util.ArrayList;

public class Bidder extends User {
    private int balance = 0;
    private ArrayList<String> bidHistory = new ArrayList<>();

    public Bidder(String loginUsername, String password) {
        super(loginUsername, password);
    }

    //Begin: Nap/rut tien
    public void userDeposit(int money) {
        System.out.println("Nap tien thanh cong");
        balance += money;
    }

    public void userWithdraw(double money) {
        if (money > balance) {
            System.out.println("Tai khoan ban khong du tien");
        }else {
            System.out.println("Giao dich thanh cong");
            balance -= money;
        }
    }
    //End: Nap/rut tien

    //Them san pham vao bidHistory
    public void addBidHistory(String item) {
        bidHistory.add(item);
    }

    //Xem san pham trong bidHistory
    public void viewBidHistory() {
        System.out.println("Lich su mua hang:");
        for (String item : bidHistory) {
            System.out.println(item);
        }
    }
}
