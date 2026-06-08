package org.rap.algotutorbe.learning.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class QuizChoice implements Serializable {
    private String id;
    private String text;
    private Boolean isCorrect;
    private String explanation;
}

