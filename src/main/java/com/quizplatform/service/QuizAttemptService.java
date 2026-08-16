package com.quizplatform.service;

import com.quizplatform.model.*;
import com.quizplatform.repository.QuizAttemptRepository;
import com.quizplatform.repository.StudentAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository attemptRepository;
    private final StudentAnswerRepository answerRepository;
    private final QuestionService questionService;

    public static class MaxAttemptsExceededException extends RuntimeException {
        public MaxAttemptsExceededException(String message) { super(message); }
    }

    @Transactional
    public QuizAttempt startAttempt(Quiz quiz, User student) {

        Optional<QuizAttempt> existing =
                attemptRepository.findByStudentAndQuizAndStatus(
                        student,
                        quiz,
                        AttemptStatus.IN_PROGRESS
                );

        if (existing.isPresent()) {

            QuizAttempt attempt = existing.get();

            if (isExpired(attempt)) {
                autoSubmitExpired(attempt);
                return startAttempt(quiz, student);
            }

            return attempt;
        }

        long completedAttempts = getCompletedAttemptCount(quiz, student);
        if (completedAttempts >= quiz.getMaxAttempts()) {
            throw new MaxAttemptsExceededException(
                    "You have used all " + quiz.getMaxAttempts() + " allowed attempt(s) for this quiz."
            );
        }

        QuizAttempt attempt = new QuizAttempt();

        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setStartTime(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setTotalQuestions(quiz.getQuestionCount());
        attempt.setTotalMarks(quiz.getTotalMarks());

        return attemptRepository.save(attempt);
    }

    public long getCompletedAttemptCount(Quiz quiz, User student) {
        return attemptRepository.findByStudentOrderByStartTimeDesc(student).stream()
                .filter(a -> a.getQuiz().getId().equals(quiz.getId()))
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .count();
    }

    public boolean isExpired(QuizAttempt attempt) {

        long allowedSeconds =
                attempt.getQuiz().getDurationMinutes() * 60L;

        long elapsedSeconds =
                ChronoUnit.SECONDS.between(
                        attempt.getStartTime(),
                        LocalDateTime.now()
                );

        return elapsedSeconds > allowedSeconds;
    }

    public long getRemainingSeconds(QuizAttempt attempt) {

        long allowedSeconds =
                attempt.getQuiz().getDurationMinutes() * 60L;

        long elapsedSeconds =
                ChronoUnit.SECONDS.between(
                        attempt.getStartTime(),
                        LocalDateTime.now()
                );

        return Math.max(0, allowedSeconds - elapsedSeconds);
    }

    @Transactional(readOnly = true)
    public QuizAttempt getById(Long id) {

        return attemptRepository.findWithAnswersById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Attempt not found: " + id
                        )
                );
    }

    @Transactional
    public QuizAttempt submitAttempt(
            QuizAttempt attempt,
            Map<Long, String> answersMap) {

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }

        boolean expired = isExpired(attempt);

        List<Question> questions =
                questionService.findByQuiz(attempt.getQuiz());

        int score = 0;
        int correctCount = 0;
        int incorrectCount = 0;
        int unansweredCount = 0;
        int totalMarks = 0;

        for (Question question : questions) {

            totalMarks += question.getMarks();

            String selected =
                    answersMap == null
                            ? null
                            : answersMap.get(question.getId());

            StudentAnswer studentAnswer = new StudentAnswer();

            studentAnswer.setAttempt(attempt);
            studentAnswer.setQuestion(question);
            studentAnswer.setSelectedOption(selected);

            boolean isCorrect =
                    selected != null
                            && selected.equalsIgnoreCase(
                                    question.getCorrectOption()
                            );

            studentAnswer.setCorrect(isCorrect);

            if (isCorrect) {

                studentAnswer.setMarksAwarded(
                        question.getMarks()
                );

                score += question.getMarks();
                correctCount++;

            } else {

                studentAnswer.setMarksAwarded(0);

                if (selected == null) {
                    unansweredCount++;
                } else {
                    incorrectCount++;
                }
            }

            attempt.getAnswers().add(studentAnswer);
        }

        attempt.setScore(score);
        attempt.setCorrectAnswers(correctCount);
        attempt.setIncorrectAnswers(incorrectCount);
        attempt.setUnansweredQuestions(unansweredCount);
        attempt.setTotalQuestions(questions.size());
        attempt.setTotalMarks(totalMarks);
        attempt.setEndTime(LocalDateTime.now());

        attempt.setStatus(
                expired
                        ? AttemptStatus.TIME_EXPIRED
                        : AttemptStatus.SUBMITTED
        );

        return attemptRepository.save(attempt);
    }

    @Transactional
    public void autoSubmitExpired(QuizAttempt attempt) {

        submitAttempt(attempt, Map.of());
    }

    public List<QuizAttempt> findByStudent(User student) {

        return attemptRepository
                .findByStudentOrderByStartTimeDesc(student);
    }

    public List<QuizAttempt> findLeaderboard(Quiz quiz) {

        return attemptRepository
                .findByQuizAndStatusOrderByScoreDescEndTimeAsc(
                        quiz,
                        AttemptStatus.SUBMITTED
                );
    }

    public List<QuizAttempt> findAllAttempts() {

        return attemptRepository
                .findAllByOrderByStartTimeDesc();
    }

    public List<StudentAnswer> getAnswers(QuizAttempt attempt) {

        return answerRepository.findByAttempt(attempt);
    }

    public long countAttemptsForQuiz(Quiz quiz) {

        return attemptRepository.countByQuiz(quiz);
    }
}