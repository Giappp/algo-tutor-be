package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.AiGeneralChatIntent;

import static org.assertj.core.api.Assertions.assertThat;

class AiGeneralChatIntentClassifierTest {

    private final AiGeneralChatIntentClassifier classifier = new AiGeneralChatIntentClassifier();

    @Test
    void classify_shouldDetectRoadmapAdvisoryWithStrongPhrase() {
        AiGeneralChatIntent intent = classifier.classify("Tư vấn cho tôi lộ trình học DSA", null);

        assertThat(intent).isEqualTo(AiGeneralChatIntent.ROADMAP_ADVISORY);
    }

    @Test
    void classify_shouldDetectRoadmapAdvisoryWithModifierAndLearningTerm() {
        AiGeneralChatIntent intent = classifier.classify("Tôi nên bắt đầu học backend như thế nào?", null);

        assertThat(intent).isEqualTo(AiGeneralChatIntent.ROADMAP_ADVISORY);
    }

    @Test
    void classify_shouldKeepRoadmapContextForShortFollowUp() {
        AIConversation conversation = new AIConversation();
        conversation.setTitle("Tư vấn lộ trình học Java");

        AiGeneralChatIntent intent = classifier.classify("còn Python thì sao?", conversation);

        assertThat(intent).isEqualTo(AiGeneralChatIntent.ROADMAP_ADVISORY);
    }

    @Test
    void classify_shouldPreferCodingHelpWhenAskingDebugQuestion() {
        AIConversation conversation = new AIConversation();
        conversation.setTitle("Tư vấn lộ trình học Java");

        AiGeneralChatIntent intent = classifier.classify("Code của tôi bị wrong answer ở test case 3", conversation);

        assertThat(intent).isEqualTo(AiGeneralChatIntent.CODING_HELP);
    }

    @Test
    void classify_shouldDetectPlatformHelp() {
        AiGeneralChatIntent intent = classifier.classify("Làm sao để nộp bài trên AlgoTutor?", null);

        assertThat(intent).isEqualTo(AiGeneralChatIntent.PLATFORM_HELP);
    }

    @Test
    void classify_shouldDefaultToGeneralForSmallTalk() {
        AiGeneralChatIntent intent = classifier.classify("Chào bạn, hôm nay học gì vui không?", null);

        assertThat(intent).isEqualTo(AiGeneralChatIntent.GENERAL);
    }
}
