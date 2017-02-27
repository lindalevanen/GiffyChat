package com.example.linda.giffychat;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Linda on 27/02/17.
 */

public class Constants {

    public static HashMap<String, String> userColors;

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
