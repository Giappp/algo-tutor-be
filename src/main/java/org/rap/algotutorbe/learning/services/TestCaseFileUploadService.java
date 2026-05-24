package org.rap.algotutorbe.learning.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.PresignedUploadTarget;
import org.rap.algotutorbe.common.services.S3Service;
import org.rap.algotutorbe.learning.dto.testcase.TestCasePresignResponse;
import org.rap.algotutorbe.learning.dto.testcase.TestCasePresignedFileRequest;
import org.rap.algotutorbe.learning.dto.testcase.TestCasePresignedUploadRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestCaseFileUploadService {

    private final S3Service s3Service;

    public List<TestCasePresignResponse> createPresignedUploadUrls(
            Long problemId,
            TestCasePresignedUploadRequest request
    ) {
        String testCaseUuid = UUID.randomUUID().toString();

        return request.files()
                .stream()
                .map(file -> createUploadResponse(problemId, testCaseUuid, file))
                .toList();
    }

    private TestCasePresignResponse createUploadResponse(
            Long problemId,
            String testCaseUuid,
            TestCasePresignedFileRequest file
    ) {
        PresignedUploadTarget target = s3Service.createPresignedUploadTarget(
                problemId,
                testCaseUuid,
                file.fileName()
        );

        return new TestCasePresignResponse(
                file.fileName(),
                file.fileType(),
                target.uploadUrl(),
                target.fileUrl(),
                target.objectKey()
        );
    }
}