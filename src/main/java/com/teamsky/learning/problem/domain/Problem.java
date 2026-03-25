package com.teamsky.learning.problem.domain;

import com.teamsky.learning.chapter.domain.Chapter;
import com.teamsky.learning.shared.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "problems")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Problem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProblemType problemType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(columnDefinition = "TEXT")
    private String hint;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("choiceNumber ASC")
    private List<Choice> choices = new ArrayList<>();

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @Builder
    public Problem(Chapter chapter, String content, ProblemType problemType,
                   Difficulty difficulty, String explanation, String hint) {
        this.chapter = chapter;
        this.content = content;
        this.problemType = problemType;
        this.difficulty = difficulty;
        this.explanation = explanation;
        this.hint = hint;
    }

    public void addChoice(Choice choice) {
        this.choices.add(choice);
        choice.setProblem(this);
    }

    public void addAnswer(Answer answer) {
        this.answers.add(answer);
        answer.setProblem(this);
    }

    public boolean isMultipleChoice() {
        return this.problemType == ProblemType.MULTIPLE_CHOICE;
    }

    public boolean isSubjective() {
        return this.problemType == ProblemType.SUBJECTIVE;
    }
}

