package com.example.linda.giffychat;

import com.example.linda.giffychat.Entity.User;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Contains some global constants. This might not be the best practice in Android but gonna
 * figure out something better later.
 */

public class Constants {

    public static final String favoritePrefsName = "favoritePrefs";
    public static final String messagePrefsName = "messagePrefs";

    public static HashMap<String, String> userColors;
    public static ArrayList<User> currentUsers;
    public static String ownPlayerID;
    public static String partnerPlayerID;

    public static void initUserColors() {
        userColors = new HashMap<>();
    }

    public static void addUserColor(String uuid, String color) {
        userColors.put(uuid, color);
    }

    public static String getUserColor(String uuid) {
        if(userColors.get(uuid) != null) {
            return userColors.get(uuid);
        } else {
            return null;
        }
    }

}
