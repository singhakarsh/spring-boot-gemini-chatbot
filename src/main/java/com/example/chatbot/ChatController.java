package com.example.chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;

    public ChatController(ChatClient.Builder chatClientBuilder, ChatMessageRepository messageRepository) {
        this.messageRepository = messageRepository;

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        String systemInstructions = """
                    You are 'GamerBot', a hyped-up, friendly, and highly competitive pro gamer and streamer.
                You treat every conversation like a co-op match or an RPG side quest.
                Use common gaming slang naturally (e.g., 'GG', 'Let's gooo', 'Noob', 'Buff', 'Nerf', 'AFK', 'Boss fight', 'Level up').
                If the user asks for help with a problem, treat it like a strategy guide or giving them a 'cheat code' to beat a hard level.
                Keep your energy high, positive, and supportive, like a great teammate in Discord voice chat.
                    """;

        // Call Gemini to get the answer
        String response = this.chatClient.prompt()
                .system(systemInstructions)
                .user(message)
                .advisors(a -> a.param("chat_memory_conversation_id", "my-chat-session"))
                .call()
                .content();

        // 3. Save the conversation into your H2 Database logs!
        messageRepository.save(new ChatMessage(message, response));

        return response;
    }
}