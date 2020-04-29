package com.quarks.android.Items;

public class MessageItem {

    private String messageId;
    private String message;
    private int messageChannel;
    private String messageTime;
    private String date;

    public MessageItem(String messageId, String message, int messageChannel, String messageTime, String date) {
        this.messageId = messageId;
        this.message = message;
        this.messageChannel = messageChannel;
        this.messageTime = messageTime;
        this.date = date;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getMessageChannel() {
        return messageChannel;
    }

    public void setMessageChannel(int messageChannel) {
        this.messageChannel = messageChannel;
    }

    public String getMessageTime() {
        return messageTime;
    }

    public void setMessageTime(String messageTime) {
        this.messageTime = messageTime;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
