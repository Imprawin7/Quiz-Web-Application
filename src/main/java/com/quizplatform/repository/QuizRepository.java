package com.quizplatform.repository;

import com.quizplatform.model.Category;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStatusOrderByCreatedAtDesc(QuizStatus status);
    List<Quiz> findAllByOrderByCreatedAtDesc();
    List<Quiz> findByCategoryOrderByCreatedAtDesc(Category category);
}