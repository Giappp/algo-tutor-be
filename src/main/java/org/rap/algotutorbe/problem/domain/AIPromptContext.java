package org.rap.algotutorbe.problem.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "ai_prompt_contexts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AIPromptContext extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "problem_id")
    private Problem problem;

    @Column(columnDefinition = "TEXT")
    private String algorithmicConcept; // Khái niệm thuật toán liên quan đến bài toán

    @Column(columnDefinition = "TEXT")
    private String predefinedHints; // Các hint chuẩn bị sẵn để AI dùng làm cơ sở sinh câu trả lời

    @Column(columnDefinition = "TEXT")
    private String edgeCasesToRemind; // Các trường hợp góc cần nhắc nhở user (mảng rỗng, số âm...)
}
