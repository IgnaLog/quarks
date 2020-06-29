package com.quarks.android.Items;

public class ConversationItem {
    private String urlPhoto;
    private String filename;
    private String username;
    private String userId;
    private String lastMessage;
    private String time;

    public ConversationItem(String urlPhoto, String filename, String username, String userId, String lastMessage, String time) {
        this.urlPhoto = urlPhoto;
        this.filename = filename;
        this.username = username;
        this.userId = userId;
        this.lastMessage = lastMessage;
        this.time = time;
    }

    public String getUrlPhoto() {
        return urlPhoto;
    }

    public void setUrlPhoto(String urlPhoto) {
        this.urlPhoto = urlPhoto;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String geTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
