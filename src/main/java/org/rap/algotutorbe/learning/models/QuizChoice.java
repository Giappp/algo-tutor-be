package org.rap.algotutorbe.learning.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseEntity;

@Entity
@Table(name = "quiz_choices") // Đổi tên bảng cho hợp lý
@Getter
@Setter
@NoArgsConstructor
public class QuizChoice extends BaseEntity {
    // Giả sử BaseEntity đã có sẵn trường ID

    @Column(name = "choice_text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;
}

