package com.example.trialnotepad;

import com.google.firebase.Timestamp;

public class NoteModel {
    String title;
    String lowercaseTitle;
    String content;
    Timestamp timestamp;

    public NoteModel() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.lowercaseTitle = title.toLowerCase();
    }

    public String getLowercaseTitle() {
        return lowercaseTitle;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}
