package com.example.chatbot;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
// JpaRepository needs to know: What entity are we managing? (ChatMessage) and
// what type is its ID? (Long)
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // You get standard database operations like .save(), .findAll(), .deleteById()
    // for free!
}