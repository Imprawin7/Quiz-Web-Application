package com.quizplatform.repository;

import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuiz(Quiz quiz);
    long countByQuiz(Quiz quiz);
}
