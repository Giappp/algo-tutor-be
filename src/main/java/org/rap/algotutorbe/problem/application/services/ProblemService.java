package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.problem.infrastructure.repositories.ProblemRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProblemService {
    ProblemRepository problemRepository;
}
