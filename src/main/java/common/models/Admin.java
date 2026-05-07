package common.models;

public class Admin extends User{
    public Admin(int id, String loginUsername) {
        super(id, loginUsername, "ADMIN", 0.0);
    }
}
