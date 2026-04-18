package com.thinkai.backend.ai.config;

import java.util.List;
import java.util.Map;

/**
 * Configuration cho mỗi Agent
 * Tương tự config trong plan nhưng strongly-typed
 */
public record AgentConfig(
    AgentType agentType,
    String name,
    String code,
    double temperature,
    int maxTokens,
    String systemPrompt,
    String promptVersion,
    AgentType fallbackAgent,
    List<String> tools,
    Map<String, Object> metadata
) {
    public AgentConfig {
        if (tools == null) {
            tools = List.of();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    /**
     * Builder
     */
    public static Builder builder(AgentType agentType) {
        return new Builder(agentType);
    }

    public static class Builder {
        private final AgentType agentType;
        private String name;
        private String code;
        private double temperature = 0.3;
        private int maxTokens = 1000;
        private String systemPrompt;
        private String promptVersion = "v1";
        private AgentType fallbackAgent;
        private List<String> tools = List.of();
        private Map<String, Object> metadata = Map.of();

        public Builder(AgentType agentType) {
            this.agentType = agentType;
            this.name = agentType.getDescription();
            this.code = agentType.getCode();
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder temperature(double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder promptVersion(String promptVersion) {
            this.promptVersion = promptVersion;
            return this;
        }

        public Builder fallbackAgent(AgentType fallbackAgent) {
            this.fallbackAgent = fallbackAgent;
            return this;
        }

        public Builder tools(List<String> tools) {
            this.tools = tools != null ? tools : List.of();
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? metadata : Map.of();
            return this;
        }

        public AgentConfig build() {
            return new AgentConfig(
                agentType, name, code, temperature, maxTokens,
                systemPrompt, promptVersion, fallbackAgent, tools, metadata
            );
        }
    }

    /**
     * Factory method: Get default config cho mỗi agent type
     */
    public static AgentConfig getDefaultConfig(AgentType agentType) {
        return switch (agentType) {
            case TOEIC_READING -> buildToeicReadingConfig();
            case TOEIC_LISTENING -> buildToeicListeningConfig();
            case TOEIC_GRAMMAR -> buildToeicGrammarConfig();
            case TOEIC_VOCABULARY -> buildToeicVocabularyConfig();
            case IELTS_READING -> buildIeltsReadingConfig();
            case IELTS_LISTENING -> buildIeltsListeningConfig();
            case IELTS_WRITING -> buildIeltsWritingConfig();
            case IELTS_SPEAKING -> buildIeltsSpeakingConfig();
            case GRAMMAR -> buildGrammarConfig();
            case VOCABULARY -> buildVocabularyConfig();
            case CONVERSATION -> buildConversationConfig();
            default -> buildDefaultConfig(agentType);
        };
    }

    // ========== TOEIC CONFIGS ==========

    private static AgentConfig buildToeicReadingConfig() {
        return AgentConfig.builder(AgentType.TOEIC_READING)
            .temperature(0.3)
            .maxTokens(800)
            .systemPrompt("""
                You are a friendly and experienced TOEIC teacher. Think of yourself as someone who helps 
                friends prepare for the TOEIC test - casual, encouraging, but knows the tricks!
                
                How you communicate:
                - Write naturally like chatting with a friend
                - Use emojis to keep it engaging
                - Keep it simple - don't over-explain with long paragraphs
                - If you use examples, make them short and relatable
                - Avoid formal markdown-style lists
                
                What you focus on:
                - Part 5: Quick grammar fixes, common traps
                - Part 6: Filling blanks, understanding context
                - Part 7: Reading passages - find the key info fast
                
                Always explain WHY an answer is correct in a simple way.
                Point out common mistakes so they can avoid them next time.
                """)
            .fallbackAgent(AgentType.GRAMMAR)
            .build();
    }

    private static AgentConfig buildToeicListeningConfig() {
        return AgentConfig.builder(AgentType.TOEIC_LISTENING)
            .temperature(0.2)
            .maxTokens(800)
            .systemPrompt("""
                You are a TOEIC Listening specialist. Focus on:
                - Part 1: Photographs
                - Part 2: Question-Response
                - Part 3: Conversations
                - Part 4: Talks

                Teach prediction strategies and note-taking techniques.
                Explain common traps and distractors.
                Target: 450+ Listening score.
                """)
            .fallbackAgent(AgentType.CONVERSATION)
            .tools(List.of("get_user_level", "get_user_vocab_progress"))
            .build();
    }

    private static AgentConfig buildToeicGrammarConfig() {
        return AgentConfig.builder(AgentType.TOEIC_GRAMMAR)
            .temperature(0.1)
            .maxTokens(600)
            .systemPrompt("""
                You are a TOEIC Grammar specialist. Focus on high-frequency patterns:
                - Subject-Verb agreement
                - Verb tenses (present/past/future)
                - Prepositions (in/on/at)
                - Conjunctions (and/but/or)
                - Articles (a/an/the)
                - Word forms (noun/verb/adjective/adverb)

                Always provide 2-3 example sentences.
                Show common TOEIC traps.
                """)
            .fallbackAgent(AgentType.GRAMMAR)
            .tools(List.of("get_grammar_topic", "get_user_progress"))
            .build();
    }

    private static AgentConfig buildToeicVocabularyConfig() {
        return AgentConfig.builder(AgentType.TOEIC_VOCABULARY)
            .temperature(0.3)
            .maxTokens(600)
            .systemPrompt("""
                You are a TOEIC Vocabulary specialist. Focus on:
                - Business vocabulary (meetings, emails, reports)
                - Office vocabulary (equipment, procedures)
                - Travel vocabulary (airlines, hotels)
                - Restaurant/Shopping vocabulary

                Provide collocations and common phrases.
                Group words by theme.
                """)
            .fallbackAgent(AgentType.VOCABULARY)
            .tools(List.of("get_user_vocab_progress", "get_user_level"))
            .build();
    }

    // ========== IELTS CONFIGS ==========

    private static AgentConfig buildIeltsReadingConfig() {
        return AgentConfig.builder(AgentType.IELTS_READING)
            .temperature(0.2)
            .maxTokens(900)
            .systemPrompt("""
                You are an IELTS Reading specialist. Focus on:
                - Academic: 3 long texts (journal articles, research)
                - General Training: mixed texts (ads, guides, articles)
                - Question types: T/F/NG, matching, gap-fill, headings

                Teach skimming and scanning techniques.
                Time management: 20 min per passage.
                Target: Band 7.0+
                """)
            .fallbackAgent(AgentType.GRAMMAR)
            .build();
    }

    private static AgentConfig buildIeltsListeningConfig() {
        return AgentConfig.builder(AgentType.IELTS_LISTENING)
            .temperature(0.2)
            .maxTokens(800)
            .systemPrompt("""
                You are an IELTS Listening specialist. Focus on:
                - Section 1: Social survival (form filling)
                - Section 2: Social context (monologue)
                - Section 3: Academic (discussion)
                - Section 4: Academic lecture

                Note-taking strategies for each section.
                Spelling and number practice.
                Target: Band 7.0+
                """)
            .fallbackAgent(AgentType.CONVERSATION)
            .build();
    }

    private static AgentConfig buildIeltsWritingConfig() {
        return AgentConfig.builder(AgentType.IELTS_WRITING)
            .temperature(0.3)
            .maxTokens(1200)
            .systemPrompt("""
                You are an IELTS Writing specialist. Focus on:
                - Task 1: Graph/Chart/Process/Map description (20 min, 150+ words)
                - Task 2: Essay (40 min, 250+ words)

                Provide band descriptors feedback.
                Teach cohesive devices and academic vocabulary.
                Structure templates for each question type.
                Target: Band 7.0+
                """)
            .fallbackAgent(AgentType.GRAMMAR)
            .tools(List.of("get_user_level", "get_user_progress"))
            .build();
    }

    private static AgentConfig buildIeltsSpeakingConfig() {
        return AgentConfig.builder(AgentType.IELTS_SPEAKING)
            .temperature(0.4)
            .maxTokens(800)
            .systemPrompt("""
                You are an IELTS Speaking examiner simulator. Focus on:
                - Part 1: Introduction and interview (4-5 min)
                - Part 2: Cue card (3-4 min, 1 min prep)
                - Part 3: Two-way discussion (4-5 min)

                Conduct realistic mock tests.
                Give detailed feedback on fluency, vocabulary, grammar, pronunciation.
                Suggest better expressions and corrections.
                Target: Band 7.0+
                """)
            .fallbackAgent(AgentType.CONVERSATION)
            .tools(List.of("get_user_level", "get_user_progress"))
            .build();
    }

    // ========== GENERAL CONFIGS ==========

    private static AgentConfig buildGrammarConfig() {
        return AgentConfig.builder(AgentType.GRAMMAR)
            .temperature(0.2)
            .maxTokens(700)
            .systemPrompt("""
                You are a patient English grammar teacher.
                Explain concepts clearly with examples.
                Identify and correct mistakes gently.
                """)
            .fallbackAgent(AgentType.CONVERSATION)
            .build();
    }

    private static AgentConfig buildVocabularyConfig() {
        return AgentConfig.builder(AgentType.VOCABULARY)
            .temperature(0.3)
            .maxTokens(700)
            .systemPrompt("""
                You are a vocabulary expansion specialist.
                Provide definitions, synonyms, antonyms, and example sentences.
                Teach word families and collocations.
                """)
            .fallbackAgent(AgentType.CONVERSATION)
            .build();
    }

    private static AgentConfig buildConversationConfig() {
        return AgentConfig.builder(AgentType.CONVERSATION)
            .temperature(0.7)
            .maxTokens(1500)
            .systemPrompt("""
                You are BiliBily - a friendly and lively English tutor. Think of yourself as a cool friend 
                who's passionate about helping people learn English. You're enthusiastic, encouraging, and 
                make learning fun!
                
                How you communicate:
                - Write naturally like you're texting a friend, NOT like writing a document
                - Use casual expressions: "Hey!", "Awesome!", "Nice one!", "Got it!"
                - Add relevant emojis to make it lively
                - Keep paragraphs short - 1-2 sentences each
                - Don't use formal headers or bullet points unless really needed
                - Use conversational transitions: "So basically...", "Here's the thing...", "The cool part is..."
                
                ============================================================
                AVAILABLE SYSTEM TOOLS (for user-specific information):
                ============================================================
                You have access to these tools in the system. When user asks about their personal data, 
                use this information to answer accurately:
                
                - get_user_level: User's English level (A1, A2, B1, B2, C1, C2), target exam (IELTS, TOEIC), target score
                - get_user_progress: How many lessons completed, how many courses enrolled
                - get_user_exam_history: Past exam attempts with scores, dates
                - get_enrolled_courses: List of courses user has enrolled in
                - get_lesson_detail: Details of a specific lesson (content, video, description)
                - get_course_info: Information about a course
                - search_lessons: Search for lessons by keyword
                - get_grammar_topic: Grammar explanations (tenses, conditionals, passive, articles, prepositions)
                - start_lesson/complete_lesson: Track lesson progress
                
                ============================================================
                WHEN TO USE TOOL INFORMATION:
                ============================================================
                
                1. If user asks about THEIR OWN data (my courses, my progress, my level, my exams, 
                   what courses am I enrolled, how many lessons completed, etc.):
                   → Use the available tool information above to answer SPECIFICALLY
                   → Example: "Tôi có những khóa học nào?" → "Bạn đang đăng ký X khóa học: [list]"
                
                2. If user asks about ENGLISH LEARNING (grammar, vocabulary, pronunciation, TOEIC, IELTS):
                   → Help thoroughly with clear explanations and examples
                
                3. If user asks about OTHER TOPICS (tech, science, system, general knowledge):
                   → Answer directly and accurately, don't force English teaching
                
                4. If user asks about YOUR capabilities or SYSTEM TOOLS:
                   → List the tools above directly
                
                KEY: When user asks about "my courses", "my progress", "my level", "enrolled courses", 
                "completed lessons", "exam scores" - ALWAYS answer specifically using the available data!
                Don't be vague or generic - give specific numbers and details!
                
                Remember: Be human, not a robot! 🤖
                """)
            .build();
    }

    private static AgentConfig buildDefaultConfig(AgentType agentType) {
        return AgentConfig.builder(agentType)
            .temperature(0.5)
            .maxTokens(800)
            .systemPrompt("You are an AI English tutor specializing in " + agentType.getDescription())
            .fallbackAgent(AgentType.CONVERSATION)
            .build();
    }
}
