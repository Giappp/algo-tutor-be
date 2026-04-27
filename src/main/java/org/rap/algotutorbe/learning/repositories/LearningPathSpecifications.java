package org.rap.algotutorbe.learning.repositories;

import org.rap.algotutorbe.learning.models.LearningPath;
import org.rap.algotutorbe.learning.models.Level;
import org.springframework.data.jpa.domain.Specification;

public final class LearningPathSpecifications {

    private LearningPathSpecifications() {}

    public static Specification<LearningPath> isActive() {
        return (root, query, cb) -> cb.equal(root.get("deleted"), false);
    }

    public static Specification<LearningPath> hasLevel(Level level) {
        return (root, query, cb) -> cb.equal(root.get("level"), level);
    }

    public static Specification<LearningPath> searchByName(String search) {
        String q = search == null ? null : search.trim();
        if (q == null || q.isBlank()) return null;
        String like = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), like);
    }
}
