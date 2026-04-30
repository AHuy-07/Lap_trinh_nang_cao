package common.models;

import java.io.Serializable;

// Lớp dùng để tạo đối tượng chứa dữ liệu được trả về từ database

public class User implements Serializable {
    private int id;
    private String username;
    private String role;
    private double balance;

    public User() {};
    public User(int id, String username, String role, double balance) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {return role;}
    public void setRole(String role) {this.role = role;}

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
