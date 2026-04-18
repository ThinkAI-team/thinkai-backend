package com.thinkai.backend.ai.critic;

import com.thinkai.backend.ai.config.AgentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class QualityScorer {

    public QualityScore score(String response, AgentType agent) {
        int totalScore = 0;
        List<String> criteria = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        int lengthScore = calculateLengthScore(response.length());
        totalScore += lengthScore;
        criteria.add("Length: " + lengthScore + "/25");
        if (lengthScore < 15) {
            failures.add("Response too short or too long");
        }

        int grammarScore = calculateGrammarScore(response);
        totalScore += grammarScore;
        criteria.add("Grammar: " + grammarScore + "/25");
        if (grammarScore < 15) {
            failures.add("Potential grammar issues");
        }

        int relevanceScore = calculateRelevanceScore(response, agent);
        totalScore += relevanceScore;
        criteria.add("Relevance: " + relevanceScore + "/25");
        if (relevanceScore < 15) {
            failures.add("Response may not match user intent");
        }

        int completenessScore = calculateCompletenessScore(response, agent);
        totalScore += completenessScore;
        criteria.add("Completeness: " + completenessScore + "/25");
        if (completenessScore < 15) {
            failures.add("Response may be incomplete");
        }

        return new QualityScore(totalScore, criteria, failures);
    }

    private int calculateLengthScore(int length) {
        if (length < 20) return 5;
        if (length < 50) return 10;
        if (length < 150) return 20;
        if (length < 500) return 25;
        if (length < 1000) return 20;
        return 15;
    }

    private int calculateGrammarScore(String response) {
        int score = 20;
        
        if (response.contains("..")) score -= 5;
        if (response.contains("  ")) score -= 3;
        if (!response.contains(".") && !response.contains("?") && !response.contains("!")) {
            score -= 5;
        }
        
        return Math.max(5, score);
    }

    private int calculateRelevanceScore(String response, AgentType agent) {
        int score = 20;
        
        if (agent == AgentType.CONVERSATION) {
            if (response.length() > 100) score = 25;
        } else if (agent.isToeicAgent() || agent.isIeltsAgent()) {
            if (response.toLowerCase().contains("answer") || 
                response.toLowerCase().contains("explanation")) {
                score = 25;
            }
        } else if (agent == AgentType.GRAMMAR || agent == AgentType.VOCABULARY) {
            if (response.toLowerCase().contains("example")) {
                score = 25;
            }
        }
        
        return score;
    }

    private int calculateCompletenessScore(String response, AgentType agent) {
        int score = 20;
        
        if (agent == AgentType.IELTS_WRITING || agent == AgentType.IELTS_SPEAKING) {
            if (!response.toLowerCase().contains("however") && 
                !response.toLowerCase().contains("therefore") &&
                !response.toLowerCase().contains("additionally")) {
                score -= 5;
            }
        }
        
        if (response.endsWith("...") || response.endsWith("…")) {
            score -= 10;
        }
        
        return Math.max(5, score);
    }

    public record QualityScore(
        int totalScore,
        List<String> criteria,
        List<String> failures
    ) {}
}
