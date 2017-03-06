package com.example.linda.giffychat.Entity;

import android.content.SharedPreferences;
import android.net.Uri;

import java.util.Date;

import static com.example.linda.giffychat.Entity.MessageViewType.GIFMESSAGE;
import static com.example.linda.giffychat.Entity.MessageViewType.TEXTMESSAGE;

public class ChatMessage {

    private String messageData;

    private String messageUser;
    private String messageUserID;
    private User user;
    /* I'm gonna remove the strings when I delete the old messages that need the strings */

    private long messageTime;
    private boolean gif;
    private int gifOrientation;     // 0 = unknown, 1 = portrait, 2 = landscape
    private String absolutePath;
    private String thumbnailBase64;

    /* A normal chat message, messageData can be text or url to gif */
    public ChatMessage(String messageData, String messageUser, String messageUserID, User user, boolean gif, int gifOrientation, String thumbnailBase64) {
        this.messageData = messageData;
        this.messageUser = messageUser;
        this.messageUserID = messageUserID;
        this.user = user;
        this.gif = gif;
        this.gifOrientation = gifOrientation;
        this.thumbnailBase64 = thumbnailBase64;
        // Initialize to current time
        messageTime = new Date().getTime();
    }

    public ChatMessage() {}

    public String getMessageData() {
        return messageData;
    }

    public String getMessageUser() {
        return messageUser;
    }

    public String getMessageUserID() { return messageUserID; }

    public User getUser() { return user; }

    public long getMessageTime() {
        return messageTime;
    }

    public boolean getGif() { return gif; }

    public int getGifOrientation() { return gifOrientation; }

    public String getThumbnailBase64() { return thumbnailBase64; }

    public String getAbsolutePath() { return absolutePath; }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public void setMessageUserID(String messageUserID) { this.messageUserID = messageUserID; }

    public void setMessageData(String messageData) {
        this.messageData = messageData;
    }

    public void setUser(User user) { this.user = user; }

    public void gif(boolean gif) { this.gif = gif; }

    public void setGifOrientation(int gifOrientation) { this.gifOrientation = gifOrientation; }

    public void setThumbnailBase64(String thumbnailBase64) { this.thumbnailBase64 = thumbnailBase64; }

    public void setAbsolutePath(String absolutePath) { this.absolutePath = absolutePath; }


    public MessageViewType getType() {
        if(gif) return GIFMESSAGE;
        else return TEXTMESSAGE;
    }
}
