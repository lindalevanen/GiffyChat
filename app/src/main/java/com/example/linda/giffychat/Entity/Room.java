package com.example.linda.giffychat.Entity;

import java.util.List;
import java.util.Set;

public class Room {

    private String id;
    private String title;
    private List<String> members;
    private String base64RoomImage;

    public Room() {}

    public Room(String id, String title, List<String> members, String base64RoomImage) {
        this.id = id;
        this.title = title;
        this.members = members;
        this.base64RoomImage = base64RoomImage;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void setBase42RoomImage(String base64RoomImage) { this.base64RoomImage = base64RoomImage; }

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public List<String> getMembers() {
        return members;
    }

    public String getBase64RoomImage() { return base64RoomImage; }
}
