package org.rap.algotutorbe.ai.services;

import org.rap.algotutorbe.ai.enums.AiChatMode;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AiResponseGuardrailService {

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("```([a-zA-Z0-9_+#.-]*)\\s*\\R([\\s\\S]*?)```");
    private static final Pattern FULL_CODE_SIGNAL_PATTERN = Pattern.compile(
            "(?is)(class\\s+solution|public\\s+class|public\\s+static\\s+void\\s+main|#include\\s*<|def\\s+solve\\s*\\(|function\\s+solve\\s*\\(|import\\s+java\\.|using\\s+namespace\\s+std|mã nguồn hoàn chỉnh|ma nguon hoan chinh|complete\\s+solution)");

    public String enforceLessonDisclosurePolicy(String responseText, AiChatMode mode, boolean lessonCompleted) {
        if (responseText == null || mode == AiChatMode.SOLUTION) {
            return responseText;
        }

        if (!containsUnsafeFullSolution(responseText, mode, lessonCompleted)) {
            return responseText;
        }

        return buildSafeReplacement(mode, lessonCompleted);
    }

    public boolean shouldBufferStreamingResponse(AiChatMode mode) {
        return mode != AiChatMode.SOLUTION;
    }

    private boolean containsUnsafeFullSolution(String responseText, AiChatMode mode, boolean lessonCompleted) {
        String normalized = responseText.toLowerCase(Locale.ROOT);

        if (FULL_CODE_SIGNAL_PATTERN.matcher(normalized).find()) {
            return true;
        }

        var matcher = CODE_FENCE_PATTERN.matcher(responseText);
        while (matcher.find()) {
            String language = matcher.group(1) == null ? "" : matcher.group(1).trim();
            String codeBody = matcher.group(2) == null ? "" : matcher.group(2).trim();

            if (mode == AiChatMode.HINT) {
                return true;
            }

            if (looksLikeCompleteCodeBlock(language, codeBody)) {
                return true;
            }

            if (!lessonCompleted && codeBody.lines().count() > 6) {
                return true;
            }
        }

        return false;
    }

    private boolean looksLikeCompleteCodeBlock(String language, String codeBody) {
        String normalized = codeBody.toLowerCase(Locale.ROOT);

        if (!language.isBlank() && codeBody.lines().count() > 8) {
            return true;
        }

        return normalized.contains("return ")
                && (normalized.contains("for (")
                || normalized.contains("while (")
                || normalized.contains("def ")
                || normalized.contains("function ")
                || normalized.contains("class "));
    }

    private String buildSafeReplacement(AiChatMode mode, boolean lessonCompleted) {
        if (mode == AiChatMode.HINT) {
            return """
                    Mình sẽ giữ đúng vai trò gợi ý từng bước nhé.
                    
                    **Gợi ý:** hãy tách bài toán thành trạng thái hiện tại, điều kiện chuyển sang bước tiếp theo, và trường hợp biên nhỏ nhất. Thử dry-run bằng một input rất nhỏ trước, rồi tự viết phần cập nhật trạng thái.
                    
                    Bạn muốn mình soi tiếp phần ý tưởng hay phần edge case?
                    """;
        }

        String progressNote = lessonCompleted
                ? "Dù bài đã hoàn thành, mode hiện tại vẫn không nên trả toàn bộ lời giải."
                : "Vì bài chưa hoàn thành, mình sẽ không đưa toàn bộ lời giải trong mode này.";

        return """
                %s
                
                Mình có thể giải thích **ý tưởng**, chỉ ra **điểm sai**, hoặc đưa **mã giả ngắn** để bạn tự hoàn thiện. Nếu bạn thật sự muốn xem lời giải đầy đủ, hãy chuyển sang chế độ `SOLUTION`.
                """.formatted(progressNote);
    }
}
