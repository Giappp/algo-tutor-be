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
                    actions.add(new AiQuickAction("Gợi ý tiếp theo", AiChatIntent.NEXT_HINT, "HINT", "Cho tôi gợi ý tiếp theo."));
                }
                actions.add(new AiQuickAction("Giải thích đề bài", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích ngắn gọn đề bài."));
                if (hasCode) {
                    actions.add(new AiQuickAction("Kiểm tra lỗi code", AiChatIntent.DEBUG_CODE, "DEBUG", "Chỉ ra lỗi quan trọng nhất trong code."));
                }
            } else if (currentMode == AiChatMode.DEBUG) {
                actions.add(new AiQuickAction("Đánh giá code", AiChatIntent.REVIEW_CODE, "REVIEW", "Đánh giá ngắn gọn code này."));
                actions.add(new AiQuickAction("Phân tích độ phức tạp", AiChatIntent.ANALYZE_COMPLEXITY, "COMPLEXITY", "Phân tích Big-O của code."));
                if (canAskHint) {
                    actions.add(new AiQuickAction("Cho tôi gợi ý", AiChatIntent.GIVE_HINT, "HINT", "Gợi ý một cách sửa code."));
                }
            } else if (currentMode == AiChatMode.REVIEW) {
                actions.add(new AiQuickAction("Phân tích độ phức tạp", AiChatIntent.ANALYZE_COMPLEXITY, "COMPLEXITY", "Phân tích Big-O của code."));
                actions.add(new AiQuickAction("Giải thích hướng tối ưu", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích ngắn gọn hướng tối ưu."));
            } else if (currentMode == AiChatMode.COMPLEXITY) {
                actions.add(new AiQuickAction("Tối ưu code", AiChatIntent.REVIEW_CODE, "REVIEW", "Đề xuất một cách tối ưu code."));
                actions.add(new AiQuickAction("Bước tiếp theo", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Tôi nên cải thiện gì tiếp theo?"));
            } else if (currentMode == AiChatMode.EXPLAIN) {
                if (canAskHint) {
                    actions.add(new AiQuickAction("Cho tôi gợi ý", AiChatIntent.GIVE_HINT, "HINT", "Gợi ý hướng bắt đầu."));
                }
                actions.add(new AiQuickAction("Bước tiếp theo", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Tôi cần làm gì tiếp theo?"));
            } else if (currentMode == AiChatMode.NEXT_STEP) {
                if (canAskHint) {
                    actions.add(new AiQuickAction("Cho tôi gợi ý", AiChatIntent.GIVE_HINT, "HINT", "Gợi ý rõ hơn về bước này."));
                }
                actions.add(new AiQuickAction("Giải thích lý thuyết", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích ngắn gọn thuật toán cần dùng."));
            } else { // SOLUTION or other
                actions.add(new AiQuickAction("Phân tích độ phức tạp", AiChatIntent.ANALYZE_COMPLEXITY, "COMPLEXITY", "Phân tích Big-O của giải pháp."));
                actions.add(new AiQuickAction("Đánh giá code", AiChatIntent.REVIEW_CODE, "REVIEW", "Đánh giá ngắn gọn giải pháp."));
            }
        } else {
            // THEORY or QUIZ suggestions
            if (currentMode == AiChatMode.EXPLAIN) {
                actions.add(new AiQuickAction("Ví dụ thực tế", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Cho tôi một ví dụ thực tế ngắn."));
                actions.add(new AiQuickAction("Bước tiếp theo", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Tôi nên học gì tiếp theo?"));
            } else {
                actions.add(new AiQuickAction("Giải thích lý thuyết", AiChatIntent.EXPLAIN_PROBLEM, "EXPLAIN", "Giải thích ngắn gọn bài học."));
                actions.add(new AiQuickAction("Câu hỏi ôn tập", AiChatIntent.SUGGEST_NEXT_STEP, "NEXT_STEP", "Cho tôi một câu hỏi ôn tập."));
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
