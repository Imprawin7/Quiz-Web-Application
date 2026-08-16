package com.quizplatform.service;

import com.quizplatform.model.Category;
import com.quizplatform.model.Difficulty;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.model.User;
import com.quizplatform.repository.CategoryRepository;
import com.quizplatform.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final CategoryRepository categoryRepository;

    public List<Quiz> findAllForAdmin() {
        return quizRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Only PUBLISHED quizzes are available to students.
     */
    public List<Quiz> findActiveForStudents() {
        return quizRepository.findByStatusOrderByCreatedAtDesc(
                QuizStatus.PUBLISHED
        );
    }

    /**
     * Search and filter published quizzes.
     *
     * Supports:
     * - Keyword search
     * - Category filter
     * - Difficulty filter
     * - Maximum duration filter
     */
    public List<Quiz> searchActiveForStudents(
            String keyword,
            Long categoryId,
            Difficulty difficulty,
            Integer maxDurationMinutes) {

        return findActiveForStudents().stream()

                .filter(q ->
                        keyword == null ||
                        keyword.isBlank() ||
                        q.getTitle()
                                .toLowerCase()
                                .contains(keyword.toLowerCase()) ||
                        (q.getDescription() != null &&
                         q.getDescription()
                                .toLowerCase()
                                .contains(keyword.toLowerCase()))
                )

                .filter(q ->
                        categoryId == null ||
                        (q.getCategory() != null &&
                         q.getCategory().getId().equals(categoryId))
                )

                .filter(q ->
                        difficulty == null ||
                        q.getDifficulty() == difficulty
                )

                .filter(q ->
                        maxDurationMinutes == null ||
                        q.getDurationMinutes() <= maxDurationMinutes
                )

                .toList();
    }

    /**
     * Find quizzes belonging to a category.
     */
    public List<Quiz> findByCategory(Category category) {
        return quizRepository.findByCategoryOrderByCreatedAtDesc(category);
    }

    public Quiz getById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Quiz not found: " + id
                        ));
    }

    /**
     * Create a new quiz.
     */
    public Quiz createQuiz(
            String title,
            String description,
            Long categoryId,
            Difficulty difficulty,
            Integer durationMinutes,
            QuizStatus status,
            Integer passingPercentage,
            Integer maxAttempts,
            String thumbnailUrl,
            User createdBy) {

        Category category = null;

        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Category not found: " + categoryId
                            ));
        }

        Quiz quiz = new Quiz();

        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setCategory(category);

        quiz.setDifficulty(
                difficulty == null
                        ? Difficulty.MEDIUM
                        : difficulty
        );

        quiz.setDurationMinutes(durationMinutes);

        quiz.setStatus(
                status == null
                        ? QuizStatus.DRAFT
                        : status
        );

        quiz.setPassingPercentage(
                passingPercentage == null
                        ? 40
                        : passingPercentage
        );

        quiz.setMaxAttempts(
                maxAttempts == null
                        ? 1
                        : maxAttempts
        );

        quiz.setThumbnailUrl(
                thumbnailUrl == null || thumbnailUrl.isBlank()
                        ? null
                        : thumbnailUrl.trim()
        );

        quiz.setCreatedBy(createdBy);

        return quizRepository.save(quiz);
    }

    /**
     * Update an existing quiz.
     */
    public Quiz updateQuiz(
            Long id,
            String title,
            String description,
            Long categoryId,
            Difficulty difficulty,
            Integer durationMinutes,
            QuizStatus status,
            Integer passingPercentage,
            Integer maxAttempts,
            String thumbnailUrl) {

        Category category = null;

        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Category not found: " + categoryId
                            ));
        }

        Quiz quiz = getById(id);

        quiz.setTitle(title);
        quiz.setDescription(description);
        quiz.setCategory(category);

        quiz.setDifficulty(
                difficulty == null
                        ? Difficulty.MEDIUM
                        : difficulty
        );

        quiz.setDurationMinutes(durationMinutes);

        if (status != null) {
            quiz.setStatus(status);
        }

        if (passingPercentage != null) {
            quiz.setPassingPercentage(passingPercentage);
        }

        if (maxAttempts != null) {
            quiz.setMaxAttempts(maxAttempts);
        }

        quiz.setThumbnailUrl(
                thumbnailUrl == null || thumbnailUrl.isBlank()
                        ? null
                        : thumbnailUrl.trim()
        );

        return quizRepository.save(quiz);
    }

    public void deleteQuiz(Long id) {
        quizRepository.deleteById(id);
    }

    /**
     * Publish / unpublish quiz.
     *
     * DRAFT -> PUBLISHED
     * PUBLISHED -> UNPUBLISHED
     * UNPUBLISHED -> PUBLISHED
     */
    public void toggleActive(Long id) {

        Quiz quiz = getById(id);

        if (quiz.getStatus() == QuizStatus.PUBLISHED) {
            quiz.setStatus(QuizStatus.UNPUBLISHED);
        } else {
            quiz.setStatus(QuizStatus.PUBLISHED);
        }

        quizRepository.save(quiz);
    }
}