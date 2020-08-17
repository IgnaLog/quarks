package com.quarks.android.Items;

public class MessageItem {

    private String messageId;
    private String message;
    private String senderMessageId;
    private int messageChannel;
    private String messageTime;
    private String date;
    private int pendingMessages;
    private int status;

    public MessageItem(String messageId, String message, String senderMessageId, int messageChannel, String messageTime, String date, int pendingMessages, int status) {
        this.messageId = messageId;
        this.message = message;
        this.senderMessageId = senderMessageId;
        this.messageChannel = messageChannel;
        this.messageTime = messageTime;
        this.date = date;
        this.pendingMessages = pendingMessages;
        this.status = status;
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

    public String getSenderMessageId() {
        return senderMessageId;
    }

    public void setSenderMessageId(String senderMessageId) {
        this.senderMessageId = senderMessageId;
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

    public int getPendingMessages() {
        return pendingMessages;
    }

    public void setPendingMessages(int pendingMessages) {
        this.pendingMessages = pendingMessages;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
