package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.ai.dto.AiQuickAction;
import org.rap.algotutorbe.ai.enums.AiChatIntent;
import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.rap.algotutorbe.learning.enums.LessonType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SuggestionGenerator {

    public List<AiQuickAction> generate(
            AiChatMode currentMode,
            LessonType lessonType,
            boolean hasCode,
            boolean hasError,
            boolean canAskHint
    ) {
        List<AiQuickAction> actions = new ArrayList<>();

        if (lessonType == LessonType.CODING) {
            // General coding suggestions
            if (currentMode == AiChatMode.HINT) {
                if (canAskHint) {
                    actions.add(new AiQuickAction("Gợi ý tiếp theo", AiChatIntent.NEXT_HINT, "HINT", "Cho tôi xin gợi ý tiếp theo nhé."));
                }
                actions.add(new AiQuickAction("Giải thích đề bài", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Hãy giải thích chi tiết đề bài này cho tôi."));
                if (hasCode) {
                    actions.add(new AiQuickAction("Kiểm tra lỗi code", AiChatIntent.DEBUG_CODE, "DEBUG", "Hãy kiểm tra lỗi trong đoạn code này."));
                }
            } else if (currentMode == AiChatMode.DEBUG) {
                actions.add(new AiQuickAction("Đánh giá code", AiChatIntent.REVIEW_CODE, "REVIEW", "Đánh giá chất lượng và tối ưu code này giúp tôi."));
                actions.add(new AiQuickAction("Phân tích độ phức tạp", AiChatIntent.ANALYZE_COMPLEXITY, "COMPLEXITY", "Phân tích độ phức tạp thời gian và không gian của code này."));
                if (canAskHint) {
                    actions.add(new AiQuickAction("Cho tôi gợi ý", AiChatIntent.GIVE_HINT, "HINT", "Cho tôi xin một gợi ý để sửa code."));
                }
            } else if (currentMode == AiChatMode.REVIEW) {
                actions.add(new AiQuickAction("Phân tích độ phức tạp", AiChatIntent.ANALYZE_COMPLEXITY, "COMPLEXITY", "Phân tích độ phức tạp Big-O của code."));
                actions.add(new AiQuickAction("Giải thích hướng giải tối ưu", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích phương pháp tối ưu hơn cho bài này."));
            } else if (currentMode == AiChatMode.COMPLEXITY) {
                actions.add(new AiQuickAction("Tối ưu hóa code", AiChatIntent.REVIEW_CODE, "REVIEW", "Hãy review và đề xuất tối ưu code này."));
                actions.add(new AiQuickAction("Gợi ý bước tiếp theo", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Bước tiếp theo tôi cần làm gì để cải thiện code?"));
            } else if (currentMode == AiChatMode.EXPLAIN) {
                if (canAskHint) {
                    actions.add(new AiQuickAction("Cho tôi gợi ý", AiChatIntent.GIVE_HINT, "HINT", "Gợi ý cho tôi hướng bắt đầu code."));
                }
                actions.add(new AiQuickAction("Gợi ý bước tiếp theo", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Tôi cần làm gì tiếp theo để giải quyết bài này?"));
            } else if (currentMode == AiChatMode.NEXT_STEP) {
                if (canAskHint) {
                    actions.add(new AiQuickAction("Cho tôi gợi ý", AiChatIntent.GIVE_HINT, "HINT", "Cho tôi gợi ý chi tiết hơn về bước này."));
                }
                actions.add(new AiQuickAction("Giải thích lý thuyết", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích rõ hơn về thuật toán cần dùng."));
            } else { // SOLUTION or other
                actions.add(new AiQuickAction("Phân tích độ phức tạp", AiChatIntent.ANALYZE_COMPLEXITY, "COMPLEXITY", "Phân tích độ phức tạp của giải pháp này."));
                actions.add(new AiQuickAction("Đánh giá code", AiChatIntent.REVIEW_CODE, "REVIEW", "Đánh giá chất lượng của giải pháp."));
            }
        } else {
            // THEORY or QUIZ suggestions
            if (currentMode == AiChatMode.EXPLAIN) {
                actions.add(new AiQuickAction("Cho tôi ví dụ thực tế", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Hãy cho tôi một ví dụ thực tế minh họa cho kiến thức này."));
                actions.add(new AiQuickAction("Tôi cần làm gì tiếp theo?", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Tôi nên học hoặc làm gì tiếp theo?"));
            } else {
                actions.add(new AiQuickAction("Giải thích lý thuyết", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích lại lý thuyết của bài học này giúp tôi."));
                actions.add(new AiQuickAction("Gợi ý câu hỏi ôn tập", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Gợi ý cho tôi một câu hỏi ôn tập về bài học."));
            }
        }

        // Limit to 2 to 4 actions
        if (actions.size() > 4) {
            return actions.subList(0, 4);
        }
        if (actions.size() < 2) {
            if (actions.isEmpty()) {
                actions.add(new AiQuickAction("Giải thích lý thuyết", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích lại bài học này."));
            }
            if (actions.size() == 1) {
                actions.add(new AiQuickAction("Gợi ý bước tiếp theo", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Tôi cần làm gì tiếp theo?"));
            }
        }

        return actions;
    }
}
