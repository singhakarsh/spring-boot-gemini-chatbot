package com.example.chatbot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import org.springframework.http.MediaType;
import java.util.List;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final ChatMessageRepository messageRepository;

    // We extract the prompt cleanly as a global class constant here
    private static final String GAMER_BOT_PROMPT = """
            You are 'GamerBot', a hyped-up, friendly, and highly competitive pro gamer and streamer.
            You treat every conversation like a co-op match or an RPG side quest.
            Use common gaming slang naturally (e.g., 'GG', 'Let's gooo', 'Noob', 'Buff', 'Nerf', 'AFK', 'Boss fight', 'Level up').
            If the user asks for help with a problem, treat it like a strategy guide or giving them a 'cheat code' to beat a hard level.
            Keep your energy high, positive, and supportive, like a great teammate in Discord voice chat.
            """;

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
        String response = this.chatClient.prompt()
                .system(GAMER_BOT_PROMPT)
                .user(message)
                .advisors(a -> a.param("chat_memory_conversation_id", "my-chat-session"))
                .call()
                .content();

        messageRepository.save(new ChatMessage(message, response));

        return response;
    }

    @GetMapping("/api/history")
    public List<ChatMessage> getChatHistory() {
        return messageRepository.findAll();
    }

    @GetMapping("/api/analytics")
    public java.util.Map<String, Object> getDashboardAnalytics() {
        List<ChatMessage> allChats = messageRepository.findAll();

        int totalMessages = allChats.size();
        long totalUserChars = 0;
        long totalBotChars = 0;

        // Group messages by date string (e.g., "YYYY-MM-DD") to map activity trends
        java.util.Map<String, Integer> activityMap = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (ChatMessage chat : allChats) {
            // Calculate average character counts for text distribution metrics
            if (chat.getUserMessage() != null)
                totalUserChars += chat.getUserMessage().length();
            if (chat.getBotResponse() != null)
                totalBotChars += chat.getBotResponse().length();

            // Map timestamps to dates safely
            if (chat.getTimestamp() != null) {
                // If it's a LocalDateTime, we can format it directly!
                String dateStr = chat.getTimestamp().format(formatter);
                activityMap.put(dateStr, activityMap.getOrDefault(dateStr, 0) + 1);
            }
        }
        long avgUserLength = totalMessages == 0 ? 0 : totalUserChars / totalMessages;
        long avgBotLength = totalMessages == 0 ? 0 : totalBotChars / totalMessages;

        // Package metrics cleanly into a JSON-compatible map payload
        java.util.Map<String, Object> analyticsData = new java.util.HashMap<>();
        analyticsData.put("totalQuests", totalMessages);
        analyticsData.put("avgUserLength", avgUserLength);
        analyticsData.put("avgBotLength", avgBotLength);
        analyticsData.put("timelineLabels", activityMap.keySet());
        analyticsData.put("timelineValues", activityMap.values());

        return analyticsData;
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/api/history/clear")
    public String clearChatHistory() {
        messageRepository.deleteAll();
        return "Database wiped successfully!";
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam(value = "message", defaultValue = "Hello") String message) {

        // Create a dynamic string builder buffer to collect tokens as they pass by
        StringBuilder fullResponseBuffer = new StringBuilder();

        return this.chatClient.prompt()
                .system(GAMER_BOT_PROMPT)
                .user(message)
                .advisors(a -> a.param("chat_memory_conversation_id", "my-chat-session"))
                .stream()
                .content()
                // Hook 1: Every time a text token streams past, append it to our buffer
                .doOnNext(fullResponseBuffer::append)
                // Hook 2: The exact millisecond the stream completes successfully, commit it to
                // SQL!
                .doOnComplete(() -> {
                    String completeResponse = fullResponseBuffer.toString();
                    if (!completeResponse.isEmpty()) {
                        messageRepository.save(new ChatMessage(message, completeResponse));
                    }
                });
    }
}