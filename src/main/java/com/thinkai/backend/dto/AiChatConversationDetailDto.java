package com.thinkai.backend.dto;

import com.thinkai.backend.entity.AiChatLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatConversationDetailDto {
    private String conversationId;
    private String title;
    private List<AiChatLog> messages;
}
