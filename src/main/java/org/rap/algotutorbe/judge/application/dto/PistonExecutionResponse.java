package org.rap.algotutorbe.judge.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PistonExecutionResponse {
    private String language;
    private String version;
    private RunDetail run;
    private CompileDetail compile; // Piston đôi khi trả về thông tin compile (nếu có lỗi biên dịch)

    @Data
    public static class RunDetail {
        private String stdout;
        private String stderr;
        private String output;
        private int code;
        private String signal;
        private String message;
        private String status;

        @JsonProperty("memory")
        private Long memory; // bytes

        @JsonProperty("cpu_time")
        private Long cpuTime; // milliseconds

        @JsonProperty("wall_time")
        private Long wallTime; // milliseconds
    }

    @Data
    public static class CompileDetail {
        private String stdout;
        private String stderr;
        private String output;
        private int code;
    }
}