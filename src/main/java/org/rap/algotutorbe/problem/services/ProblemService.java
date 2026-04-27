package org.rap.algotutorbe.problem.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.problem.dto.response.ProblemSummaryResponse;
import org.rap.algotutorbe.problem.mapper.ProblemMapper;
import org.rap.algotutorbe.problem.repositories.ProblemRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProblemService extends BaseService {
    private final ProblemRepository problemRepository;
    private final ProblemMapper mapper;

    @Transactional(readOnly = true)
    public ApiResponse<List<ProblemSummaryResponse>> listPublished(
            String difficulty,
            String status,
            String tag,
            String search,
            Pageable pageable
    ) {
        return null;
    }
}
