package com.Quizvera.repository;

import com.Quizvera.model.QuizAttempt;
import com.Quizvera.model.StudentAnswer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {

    @EntityGraph(attributePaths = "question")
    List<StudentAnswer> findByAttempt(QuizAttempt attempt);
}