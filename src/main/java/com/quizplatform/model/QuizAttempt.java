package com.quizplatform.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    private User student;

    @Column(nullable = false)
    private LocalDateTime startTime = LocalDateTime.now();

    private LocalDateTime endTime;

    private Integer score = 0;

    private Integer totalMarks = 0;

    private Integer correctAnswers = 0;

    private Integer incorrectAnswers = 0;

    private Integer unansweredQuestions = 0;

    private Integer totalQuestions = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @OneToMany(
            mappedBy = "attempt",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<StudentAnswer> answers = new ArrayList<>();

    public double getPercentage() {
        if (totalMarks == null || totalMarks == 0) return 0.0;
        return Math.round((score * 10000.0) / totalMarks) / 100.0;
    }

    /** Seconds between start and end. 0 if the attempt hasn't finished yet. */
    public long getTimeTakenSeconds() {
        if (endTime == null) return 0;
        return ChronoUnit.SECONDS.between(startTime, endTime);
    }

    /** Time taken formatted as "MM:SS" for display, e.g. "14:21". */
    public String getTimeTakenFormatted() {
        long totalSeconds = getTimeTakenSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public boolean isPassed() {
        if (quiz == null) return false;
        return getPercentage() >= quiz.getPassingPercentage();
    }
}