package org.rap.algotutorbe.problem.dto.request;

import org.rap.algotutorbe.problem.domain.enums.UserProblemStatus;

public record UpdateProblemProgressRequest(
        UserProblemStatus status,
        String notes,
        Boolean bookmarked
) {
}
