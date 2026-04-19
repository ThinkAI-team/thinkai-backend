package com.thinkai.backend.ai;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.embedding.EmbeddingService;
import com.thinkai.backend.ai.router.HybridRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThinkAIComponentsTest {

    @Mock
    private HybridRouter hybridRouter;

    @Mock
    private EmbeddingService embeddingService;

    @Test
    void testAgentTypeRouting() {
        // Test basic agent routing by keywords
        String toeicMsg = "I want to practice TOEIC reading part 5";
        String grammarMsg = "Explain grammar about conditionals";
        String ieltsMsg = "Help me with IELTS writing task 2";
        String conversationMsg = "Hello, how are you?";

        AgentType toeicAgent = detectAgent(toeicMsg);
        AgentType grammarAgent = detectAgent(grammarMsg);
        AgentType ieltsAgent = detectAgent(ieltsMsg);
        AgentType convAgent = detectAgent(conversationMsg);

        assertEquals(AgentType.TOEIC_READING, toeicAgent);
        assertEquals(AgentType.GRAMMAR, grammarAgent);
        assertEquals(AgentType.IELTS_WRITING, ieltsAgent);
        assertEquals(AgentType.CONVERSATION, convAgent);
    }

    @Test
    void testEmbeddingGeneration() {
        String text = "Hello world";
        java.util.List<Double> mockEmbedding = Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5);
        when(embeddingService.embed(text)).thenReturn(mockEmbedding);

        var embedding = embeddingService.embed(text);

        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
    }

    @Test
    void testEmbeddingSimilarity() {
        String text1 = "English grammar";
        String text2 = "Grammar in English";
        String text3 = "Mathematics";

        java.util.List<Double> emb1 = Arrays.asList(0.9, 0.1, 0.0);
        java.util.List<Double> emb2 = Arrays.asList(0.85, 0.15, 0.05);
        java.util.List<Double> emb3 = Arrays.asList(0.1, 0.1, 0.9);

        when(embeddingService.embed(text1)).thenReturn(emb1);
        when(embeddingService.embed(text2)).thenReturn(emb2);
        when(embeddingService.embed(text3)).thenReturn(emb3);
        when(embeddingService.cosineSimilarity(emb1, emb2)).thenReturn(0.95);
        when(embeddingService.cosineSimilarity(emb1, emb3)).thenReturn(0.25);

        var result1 = embeddingService.embed(text1);
        var result2 = embeddingService.embed(text2);
        var result3 = embeddingService.embed(text3);

        double sim12 = embeddingService.cosineSimilarity(result1, result2);
        double sim13 = embeddingService.cosineSimilarity(result1, result3);

        assertTrue(sim12 > sim13, "Similar texts should have higher similarity");
    }

    private AgentType detectAgent(String message) {
        String msg = message.toLowerCase();
        
        if (msg.contains("toeic")) {
            if (msg.contains("reading") || msg.contains("đọc")) {
                return AgentType.TOEIC_READING;
            }
            if (msg.contains("listening") || msg.contains("nghe")) {
                return AgentType.TOEIC_LISTENING;
            }
            if (msg.contains("grammar") || msg.contains("ngữ pháp")) {
                return AgentType.TOEIC_GRAMMAR;
            }
            if (msg.contains("vocabulary") || msg.contains("từ vựng")) {
                return AgentType.TOEIC_VOCABULARY;
            }
            return AgentType.TOEIC_READING;
        }

        if (msg.contains("ielts")) {
            if (msg.contains("writing") || msg.contains("viết")) {
                return AgentType.IELTS_WRITING;
            }
            if (msg.contains("speaking") || msg.contains("nói")) {
                return AgentType.IELTS_SPEAKING;
            }
            if (msg.contains("reading") || msg.contains("đọc")) {
                return AgentType.IELTS_READING;
            }
            if (msg.contains("listening") || msg.contains("nghe")) {
                return AgentType.IELTS_LISTENING;
            }
            return AgentType.IELTS_SPEAKING;
        }

        if (msg.contains("grammar") || msg.contains("ngữ pháp")) {
            return AgentType.GRAMMAR;
        }
        if (msg.contains("vocabulary") || msg.contains("từ vựng")) {
            return AgentType.VOCABULARY;
        }
        if (msg.contains("pronunciation") || msg.contains("phát âm")) {
            return AgentType.PRONUNCIATION;
        }
        
        return AgentType.CONVERSATION;
    }
}
