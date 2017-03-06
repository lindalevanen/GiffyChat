package com.example.linda.giffychat.Entity;

/**
 * Created by Linda on 02/03/17.
 */

public class User {

    private String email, userName, uuid;

    public User(String email, String userName, String uuid) {
        this.email = email;
        this.userName = userName;
        this.uuid = uuid;
    }

    public User() {}

    public String getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
