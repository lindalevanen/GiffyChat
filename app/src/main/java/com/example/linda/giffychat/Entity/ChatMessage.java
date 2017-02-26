package com.example.linda.giffychat.Entity;

import android.net.Uri;

import java.util.Date;

import static com.example.linda.giffychat.Entity.MessageViewType.GIFMESSAGE;
import static com.example.linda.giffychat.Entity.MessageViewType.TEXTMESSAGE;

public class ChatMessage {

    private String messageData;
    private String messageUser;
    private long messageTime;
    private boolean gif;
    private int gifOrientation;     // 0 = unknown, 1 = portrait, 2 = landscape
    private String absolutePath;

    /* A normal chat message, messageData can be text or url to gif */
    public ChatMessage(String messageData, String messageUser, boolean gif, int gifOrientation) {
        this.messageData = messageData;
        this.messageUser = messageUser;
        this.gif = gif;
        this.gifOrientation = gifOrientation;
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

    public long getMessageTime() {
        return messageTime;
    }

    public boolean getGif() { return gif; }

    public int getGifOrientation() { return gifOrientation; }

    public String getAbsolutePath() { return absolutePath; }

    public void setMessageTime(long messageTime) {
        this.messageTime = messageTime;
    }

    public void setMessageUser(String messageUser) {
        this.messageUser = messageUser;
    }

    public void setMessageData(String messageData) {
        this.messageData = messageData;
    }

    public void gif(boolean gif) { this.gif = gif; }

    public void setGifOrientation(int gifOrientation) { this.gifOrientation = gifOrientation; }

    public void setAbsolutePath(String absolutePath) { this.absolutePath = absolutePath; }


    public MessageViewType getType() {
        if(gif) return GIFMESSAGE;
        else return TEXTMESSAGE;
    }
}
