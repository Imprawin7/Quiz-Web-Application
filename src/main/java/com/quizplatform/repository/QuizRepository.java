package com.Quizvera.repository;

import com.Quizvera.model.Category;
import com.Quizvera.model.Quiz;
import com.Quizvera.model.QuizStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByStatusOrderByCreatedAtDesc(QuizStatus status);
    List<Quiz> findAllByOrderByCreatedAtDesc();
    List<Quiz> findByCategoryOrderByCreatedAtDesc(Category category);
}