package com.quizplatform.repository;

import com.quizplatform.model.AttemptStatus;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudent(User student);

    List<QuizAttempt> findByStudentOrderByStartTimeDesc(User student);

    Optional<QuizAttempt> findByStudentAndQuizAndStatus(
            User student,
            Quiz quiz,
            AttemptStatus status
    );

    @EntityGraph(attributePaths = {
            "student",
            "quiz",
            "quiz.category",
            "answers",
            "answers.question"
    })
    Optional<QuizAttempt> findWithAnswersById(Long id);

    List<QuizAttempt> findByQuizAndStatusOrderByScoreDescEndTimeAsc(
            Quiz quiz,
            AttemptStatus status
    );

    List<QuizAttempt> findAllByOrderByStartTimeDesc();

    long countByQuiz(Quiz quiz);
}