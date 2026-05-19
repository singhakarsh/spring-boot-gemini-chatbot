package com.example.chatbot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@Entity // This tells Spring to turn this class into a database table!
public class ChatMessage {

    @Id // Sets this as the primary key column
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Automatically counts up (1, 2, 3...)
    private Long id;

    @Column(columnDefinition = "TEXT") // Allows messages to be longer than 255 characters
    private String userMessage;

    @Column(columnDefinition = "TEXT") // Allows long AI paragraphs to be saved securely
    private String botResponse;

    private LocalDateTime timestamp;

    // No-argument constructor (Required by JPA)
    public ChatMessage() {
    }

    // Convenience constructor
    public ChatMessage(String userMessage, String botResponse) {
        this.userMessage = userMessage;
        this.botResponse = botResponse;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters so Spring can read/write the data
    public Long getId() {
        return id;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getBotResponse() {
        return botResponse;
    }

    public void setBotResponse(String botResponse) {
        this.botResponse = botResponse;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}