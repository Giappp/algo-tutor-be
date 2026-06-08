package org.rap.algotutorbe.learning.dto.testcase;

public record TestCasePresignResponse(String fileName,
                                      TestCaseFileType fileType,
                                      String uploadUrl,   // URL cho Next.js PUT file lên
                                      String downloadUrl,  // URL lưu vào DB sau này
                                      String fileKey
) {
}
