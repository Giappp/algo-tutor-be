package org.rap.algotutorbe.learning.dto.testcase;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TestCaseResultWebSocketResponse {
    private UUID submissionId;
    private String type;
    private Long testCaseId;
    private String status;       // AC, WA, RTE, TLE
    private Integer runTimeMs;   // Thời gian chạy thực tế
    private Integer sortOrder;   // Thứ tự của testcase để FE hiển thị đúng vị trí
    private Boolean isCompleted; // Đã là testcase cuối cùng chưa (để FE tắt trạng thái loading)
}