package org.rap.algotutorbe.common.services;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class SlugGenerator {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");
    private static final Pattern MULTIPLE_DASHES = Pattern.compile("-{2,}");

    /**
     * Tạo slug từ một chuỗi bất kỳ. Hỗ trợ tốt tiếng Việt.
     * Ví dụ: "Bài Tập Thuật Toán: Two Sum 101" -> "bai-tap-thuat-toan-two-sum-101"
     */
    public String generateFrom(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        // 1. Xử lý ký tự đặc thù tiếng Việt (đ, Đ) trước khi normalize
        String processedInput = input.replace("đ", "d").replace("Đ", "D");

        // 2. Thay thế khoảng trắng bằng dấu gạch ngang
        String noWhiteSpace = WHITESPACE.matcher(processedInput).replaceAll("-");

        // 3. Normalize chuỗi về dạng NFD để tách dấu ra khỏi ký tự (Ví dụ: 'á' -> 'a' + '´')
        String normalized = Normalizer.normalize(noWhiteSpace, Normalizer.Form.NFD);

        // 4. Bỏ các dấu (diacritical marks)
        String noAccents = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");

        // 5. Chuyển về chữ thường và loại bỏ các ký tự không phải là chữ cái, số, hoặc dấu gạch ngang
        String slug = NONLATIN.matcher(noAccents).replaceAll("").toLowerCase(Locale.ENGLISH);

        // 6. Xóa các dấu gạch ngang thừa (nhiều dấu gạch ngang liên tiếp)
        slug = MULTIPLE_DASHES.matcher(slug).replaceAll("-");

        // 7. Xóa dấu gạch ngang ở đầu và cuối chuỗi (nếu có)
        slug = EDGES_DASHES.matcher(slug).replaceAll("");

        return slug;
    }
}