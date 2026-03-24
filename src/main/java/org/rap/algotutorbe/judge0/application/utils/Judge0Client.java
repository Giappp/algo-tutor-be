package org.rap.algotutorbe.judge0.application.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge0.application.dto.Judge0BatchSubmissionRequest;
import org.rap.algotutorbe.judge0.application.dto.Judge0TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class Judge0Client {

    private final RestTemplate restTemplate;

    @Value("${judge0.api.url}")
    private String judge0ApiUrl;

    public List<Judge0TokenResponse> submitBatch(Judge0BatchSubmissionRequest batchRequest) {
        String url = judge0ApiUrl + "/submissions/batch?base64_encoded=true";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            // 1. Sinh chuỗi JSON chuẩn từ ObjectMapper
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(batchRequest);

            log.info("Chuỗi JSON sẽ được gửi đi: {}", jsonPayload);

            // 2. GỬI TRỰC TIẾP CHUỖI STRING NÀY (Đây là điểm thay đổi cốt lõi)
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            // 3. Gọi API
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity, // Truyền entity chứa String vào đây
                    new ParameterizedTypeReference<List<Judge0TokenResponse>>() {
                    }
            );
            return response.getBody();

        } catch (Exception e) {
            log.error("Lỗi khi gọi Judge0 Batch API: {}", e.getMessage());
            throw new RuntimeException("Không thể kết nối tới Judge0", e);
        }
    }
}
