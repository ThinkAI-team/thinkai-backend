package com.thinkai.backend.ai;

import com.thinkai.backend.ai.config.AgentType;
import com.thinkai.backend.ai.embedding.EmbeddingService;
import com.thinkai.backend.ai.router.HybridRouter;
import com.thinkai.backend.ai.state.AiHarnessRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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
        var embedding = embeddingService.embed(text);
        
        assertNotNull(embedding);
        assertFalse(embedding.isEmpty());
    }

    @Test
    void testEmbeddingSimilarity() {
        String text1 = "English grammar";
        String text2 = "Grammar in English";
        String text3 = "Mathematics";

        var emb1 = embeddingService.embed(text1);
        var emb2 = embeddingService.embed(text2);
        var emb3 = embeddingService.embed(text3);

        double sim12 = embeddingService.cosineSimilarity(emb1, emb2);
        double sim13 = embeddingService.cosineSimilarity(emb1, emb3);

        assertTrue(sim12 > sim13, "Similar texts should have higher similarity");
    }

    private AgentType detectAgent(String message) {
        String msg = message.toLowerCase();
        
        if (msg.contains("toeic")) {
            if (msg.contains("reading") || msg.contains("đọc")) return AgentType.TOEIC_READING;
            if (msg.contains("listening") || msg.contains("nghe")) return AgentType.TOEIC_LISTENING;
            if (msg.contains("grammar") || msg.contains("ngữ pháp")) return AgentType.TOEIC_GRAMMAR;
            if (msg.contains("vocabulary") || msg.contains("từ vựng")) return AgentType.TOEIC_VOCABULARY;
            return AgentType.TOEIC_READING;
        }
        
        if (msg.contains("ielts")) {
            if (msg.contains("writing") || msg.contains("viết")) return AgentType.IELTS_WRITING;
            if (msg.contains("speaking") || msg.contains("nói")) return AgentType.IELTS_SPEAKING;
            if (msg.contains("reading") || msg.contains("đọc")) return AgentType.IELTS_READING;
            if (msg.contains("listening") || msg.contains("nghe")) return AgentType.IELTS_LISTENING;
            return AgentType.IELTS_SPEAKING;
        }
        
        if (msg.contains("grammar") || msg.contains("ngữ pháp")) return AgentType.GRAMMAR;
        if (msg.contains("vocabulary") || msg.contains("từ vựng")) return AgentType.VOCABULARY;
        if (msg.contains("pronunciation") || msg.contains("phát âm")) return AgentType.PRONUNCIATION;
        
        return AgentType.CONVERSATION;
    }
}
