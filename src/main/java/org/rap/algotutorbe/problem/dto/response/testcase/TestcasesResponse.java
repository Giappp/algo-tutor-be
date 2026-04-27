package org.rap.algotutorbe.problem.dto.response.testcase;

import java.util.List;

public record TestcasesResponse(
        Long problemId,
        SummaryInfo summary,
        List<SuccessItem> successItems,
        List<FailedItem> failedItems
) {
}