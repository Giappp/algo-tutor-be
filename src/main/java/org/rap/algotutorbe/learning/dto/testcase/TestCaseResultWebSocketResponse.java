package org.rap.algotutorbe.learning.dto.testcase;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCaseResultWebSocketResponse {
    private UUID submissionId;
    private String type;
    private Long testCaseId;
    private String status;       // AC, WA, RTE, TLE
    private Integer timeMs;
    private Integer memoryKb;
    private String stdout;
    private String stderr;
    private Integer sortOrder;   // Thứ tự của testcase để FE hiển thị đúng vị trí
    private Integer passed;
    private Integer total;
    private Integer maxTimeMs;
    private Integer maxMemoryKb;
    private String compilationError;
    private Boolean progressUpdated;
    private Boolean isCompleted; // Đã là testcase cuối cùng chưa (để FE tắt trạng thái loading)
}
