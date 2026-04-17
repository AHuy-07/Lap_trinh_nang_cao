package common.models;

abstract class User {
    private String loginUsername;
    private String password;

    public User(String loginUsername, String password) {
        this.loginUsername = loginUsername;
        this.password = password;
    }
}
