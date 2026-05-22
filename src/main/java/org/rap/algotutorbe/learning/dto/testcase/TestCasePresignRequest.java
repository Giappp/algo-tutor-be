package org.rap.algotutorbe.learning.dto.testcase;

import java.util.List;

public record TestCasePresignRequest(List<FileInfo> files) {
    public record FileInfo(String fileName, String fileType) {
    }
}
