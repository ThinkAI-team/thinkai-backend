package com.thinkai.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatConversationDto {
    private String conversationId;
    private String title;
    private String lastMessagePreview;
    private LocalDateTime lastMessageAt;
    private int messageCount;
}
