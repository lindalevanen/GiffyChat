package com.example.linda.giffychat.Entity;

import java.util.List;
import java.util.Set;

public class One2OneChat {

    private String id;
    private User member1, member2;
    private int messageCount;

    public One2OneChat() {}

    public One2OneChat(String id, User member1, User member2) {
        this.id = id;
        this.member1 = member1;
        this.member2 = member2;
        this.messageCount = 0;
    }

    public void setId(String id) { this.id = id; }

    public void setMember1(User member1) { this.member1 = member1; }

    public void setMember2(User member2) { this.member2 = member2; }

    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public String getId() { return id; }

    public User getMember1() { return member1; }

    public User getMember2() { return member2; }

    public int getMessageCount() { return messageCount; }
}
