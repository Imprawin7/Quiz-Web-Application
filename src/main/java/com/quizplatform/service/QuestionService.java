package com.quizplatform.service;

import com.quizplatform.model.Difficulty;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    public List<Question> findByQuiz(Quiz quiz) {
        return questionRepository.findByQuiz(quiz);
    }

    public Question getById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found: " + id));
    }

    public Question createQuestion(Quiz quiz, String text, String a, String b, String c, String d,
                                    String correctOption, String explanation, Difficulty difficulty, Integer marks) {
        Question q = new Question();
        q.setQuiz(quiz);
        q.setQuestionText(text);
        q.setOptionA(a);
        q.setOptionB(b);
        q.setOptionC(c);
        q.setOptionD(d);
        q.setCorrectOption(correctOption.toUpperCase());
        q.setExplanation((explanation == null || explanation.isBlank()) ? null : explanation.trim());
        q.setDifficulty(difficulty == null ? Difficulty.MEDIUM : difficulty);
        q.setMarks(marks == null ? 1 : marks);
        return questionRepository.save(q);
    }

    public Question updateQuestion(Long id, String text, String a, String b, String c, String d,
                                    String correctOption, String explanation, Difficulty difficulty, Integer marks) {
        Question q = getById(id);
        q.setQuestionText(text);
        q.setOptionA(a);
        q.setOptionB(b);
        q.setOptionC(c);
        q.setOptionD(d);
        q.setCorrectOption(correctOption.toUpperCase());
        q.setExplanation((explanation == null || explanation.isBlank()) ? null : explanation.trim());
        q.setDifficulty(difficulty == null ? Difficulty.MEDIUM : difficulty);
        q.setMarks(marks == null ? 1 : marks);
        return questionRepository.save(q);
    }

    public void deleteQuestion(Long id) {
        questionRepository.deleteById(id);
    }
}