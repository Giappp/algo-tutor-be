package org.rap.algotutorbe.problem.repositories;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.springframework.data.jpa.domain.Specification;

public final class ProblemSpecifications {
    private ProblemSpecifications() {
    }

    public static Specification<Problem> hasDifficulty(Difficulty difficulty) {
        return (root, query, cb) -> cb.equal(root.get("difficulty"), difficulty);
    }

    public static Specification<Problem> search(String search) {
        String q = search == null ? null : search.trim();
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("statement")), like)
        );
    }

    public static Specification<Problem> hasTagName(String tagName) {
        String t = tagName == null ? null : tagName.trim();
        if (t == null || t.isBlank()) return null;
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Problem, Tag> tags = root.join("tags", JoinType.LEFT);
            return cb.equal(cb.lower(tags.get("name")), t.toLowerCase());
        };
    }
}

