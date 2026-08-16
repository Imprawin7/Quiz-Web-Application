package com.quizplatform.repository;

import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.StudentAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    @EntityGraph(attributePaths = "question")
    List<StudentAnswer> findByAttempt(QuizAttempt attempt);
}