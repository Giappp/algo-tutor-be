package org.rap.algotutorbe.iam.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.rap.algotutorbe.common.domain.BaseUuidEntity;
import org.rap.algotutorbe.learning.models.Enrollment;
import org.rap.algotutorbe.learning.models.QuizAttempt;
import org.rap.algotutorbe.submission.entities.Submission;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User extends BaseUuidEntity {
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    List<RefreshToken> sessions = new ArrayList<>();
    @Column(name = "username", nullable = false, unique = true)
    private String userName;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash")
    private String passwordHashed;
    private Integer totalSolved;

    private boolean isEnabled;
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Submission> submissions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuizAttempt> quizAttempts = new ArrayList<>();

}
