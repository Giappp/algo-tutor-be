package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.ai.entity.AIConversation;
import org.rap.algotutorbe.ai.enums.AiGeneralChatIntent;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.List;

@Service
public class AiGeneralChatIntentClassifier {

    private static final List<String> STRONG_ROADMAP_PHRASES = List.of(
            "lộ trình",
            "lo trinh",
            "roadmap",
            "learning path",
            "khóa học",
            "khoa hoc",
            "course",
            "nên học gì",
            "nen hoc gi",
            "học gì tiếp",
            "hoc gi tiep",
            "bắt đầu học",
            "bat dau hoc",
            "định hướng học",
            "dinh huong hoc");

    private static final List<String> ROADMAP_MODIFIERS = List.of(
            "tư vấn",
            "tu van",
            "gợi ý",
            "goi y",
            "recommend",
            "chọn",
            "chon",
            "bắt đầu",
            "bat dau",
            "định hướng",
            "dinh huong");

    private static final List<String> LEARNING_TERMS = List.of(
            "học",
            "hoc",
            "frontend",
            "backend",
            "fullstack",
            "java",
            "python",
            "javascript",
            "dsa",
            "data structure",
            "algorithm",
            "giải thuật",
            "giai thuat",
            "cấu trúc dữ liệu",
            "cau truc du lieu");

    private static final List<String> CODING_TERMS = List.of(
            "code",
            "debug",
            "bug",
            "lỗi",
            "loi",
            "error",
            "exception",
            "compile",
            "runtime",
            "big-o",
            "độ phức tạp",
            "do phuc tap",
            "thuật toán",
            "thuat toan",
            "test case",
            "wrong answer");

    private static final List<String> PLATFORM_TERMS = List.of(
            "algotutor",
            "nền tảng",
            "nen tang",
            "tài khoản",
            "tai khoan",
            "đăng ký",
            "dang ky",
            "đăng nhập",
            "dang nhap",
            "bài học",
            "bai hoc",
            "lesson",
            "quiz",
            "submission",
            "nộp bài",
            "nop bai");

    public AiGeneralChatIntent classify(String message, AIConversation conversation) {
        String normalizedMessage = normalize(message);
        String normalizedTitle = conversation == null ? "" : normalize(conversation.getTitle());

        if (isRoadmapAdvisory(normalizedMessage)) {
            return AiGeneralChatIntent.ROADMAP_ADVISORY;
        }

        if (isRoadmapContinuation(normalizedMessage, normalizedTitle)) {
            return AiGeneralChatIntent.ROADMAP_ADVISORY;
        }

        if (containsAny(normalizedMessage, CODING_TERMS)) {
            return AiGeneralChatIntent.CODING_HELP;
        }

        if (containsAny(normalizedMessage, PLATFORM_TERMS)) {
            return AiGeneralChatIntent.PLATFORM_HELP;
        }

        return AiGeneralChatIntent.GENERAL;
    }

    private boolean isRoadmapAdvisory(String normalizedMessage) {
        if (containsAny(normalizedMessage, STRONG_ROADMAP_PHRASES)) {
            return true;
        }

        return containsAny(normalizedMessage, ROADMAP_MODIFIERS)
                && containsAny(normalizedMessage, LEARNING_TERMS);
    }

    private boolean isRoadmapContinuation(String normalizedMessage, String normalizedTitle) {
        if (!isRoadmapAdvisory(normalizedTitle)) {
            return false;
        }

        return normalizedMessage.length() <= 120
                && !containsAny(normalizedMessage, CODING_TERMS)
                && !containsAny(normalizedMessage, PLATFORM_TERMS);
    }

    private boolean containsAny(String value, List<String> candidates) {
        return candidates.stream().anyMatch(value::contains);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String lower = value.toLowerCase();
        String withoutMarks = Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return lower + " " + withoutMarks;
    }
}
