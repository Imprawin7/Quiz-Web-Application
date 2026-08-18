package com.Quizvera.service;

import com.Quizvera.model.AttemptStatus;
import com.Quizvera.model.Quiz;
import com.Quizvera.model.QuizAttempt;
import com.Quizvera.model.QuizStatus;
import com.Quizvera.model.User;
import com.Quizvera.repository.QuizAttemptRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final QuizAttemptRepository attemptRepository;

    // =========================================================
    // QUIZ STATISTICS
    // =========================================================

    @Getter
    @Setter
    public static class QuizStats {

        private Quiz quiz;
        private long totalAttempts;
        private double averageScore;
        private double averagePercentage;
        private double highestPercentage;
        private double lowestPercentage;
    }

    // =========================================================
    // STUDENT STATISTICS
    // =========================================================

    @Getter
    @Setter
    public static class StudentStats {

        private long totalAttempts;
        private double averagePercentage;
        private double bestPercentage;
        private long quizzesTaken;
        private long totalPassed;
        private long totalFailed;
        private long totalQuestionsAnswered;
    }

    // =========================================================
    // ADMIN DASHBOARD STATISTICS
    // =========================================================

    @Getter
    @Setter
    public static class DashboardStats {

        private long totalStudents;
        private long totalQuizzes;
        private long publishedQuizzes;
        private long draftQuizzes;
        private long totalQuestions;
        private long totalAttempts;
        private double averageScore;
        private long totalPassed;
        private long totalFailed;

        private Map<String, Long> attemptsOverTime;
        private Map<String, Long> registrationsOverTime;
        private Map<String, Double> avgScorePerQuiz;
        private Map<String, Long> popularQuizzes;
        private Map<String, Long> popularCategories;
    }

    // =========================================================
    // LEADERBOARD ENTRY
    // =========================================================

    @Getter
    @Setter
    public static class LeaderboardEntry {

        private User student;
        private double highestPercentage;
        private double averagePercentage;
        private long quizzesCompleted;
    }

    // =========================================================
    // LEADERBOARD
    // =========================================================

    /**
     * Creates a ranked leaderboard from completed quiz attempts.
     *
     * categoryId:
     *     null = Overall leaderboard
     *     otherwise = only quizzes from that category
     *
     * period:
     *     weekly  = last 7 days
     *     monthly = last 30 days
     *     anything else = All Time
     *
     * rankBy:
     *     average   = Average Score
     *     completed = Quizzes Completed
     *     anything else = Highest Score
     */
    public List<LeaderboardEntry> getLeaderboard(
            List<QuizAttempt> allAttempts,
            Long categoryId,
            String period,
            String rankBy) {

        // ---------------------------------------------------------
        // Determine time period
        // ---------------------------------------------------------

        LocalDateTime cutoff = switch (period == null ? "" : period) {

            case "weekly" ->
                    LocalDateTime.now().minusDays(7);

            case "monthly" ->
                    LocalDateTime.now().minusDays(30);

            default ->
                    null;
        };

        // ---------------------------------------------------------
        // Filter attempts
        // ---------------------------------------------------------

        List<QuizAttempt> filtered = allAttempts.stream()

                // Ignore attempts that are still running
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)

                // Category filter
                .filter(a ->
                        categoryId == null
                                || (
                                    a.getQuiz().getCategory() != null
                                    && a.getQuiz()
                                        .getCategory()
                                        .getId()
                                        .equals(categoryId)
                                )
                )

                // Period filter
                .filter(a ->
                        cutoff == null
                                || (
                                    a.getStartTime() != null
                                    && a.getStartTime().isAfter(cutoff)
                                )
                )

                .toList();

        // ---------------------------------------------------------
        // Group attempts by student
        // ---------------------------------------------------------

        Map<Long, List<QuizAttempt>> byStudent =
                filtered.stream()
                        .collect(
                                Collectors.groupingBy(
                                        a -> a.getStudent().getId()
                                )
                        );

        // ---------------------------------------------------------
        // Build leaderboard entries
        // ---------------------------------------------------------

        List<LeaderboardEntry> entries = new ArrayList<>();

        for (List<QuizAttempt> studentAttempts : byStudent.values()) {

            if (studentAttempts.isEmpty()) {
                continue;
            }

            LeaderboardEntry entry = new LeaderboardEntry();

            // Student
            entry.setStudent(studentAttempts.get(0).getStudent());

            // Highest score
            double highest = studentAttempts.stream()
                    .mapToDouble(QuizAttempt::getPercentage)
                    .max()
                    .orElse(0);

            // Average score
            double average = studentAttempts.stream()
                    .mapToDouble(QuizAttempt::getPercentage)
                    .average()
                    .orElse(0);

            // Number of unique quizzes completed
            long completed = studentAttempts.stream()
                    .map(a -> a.getQuiz().getId())
                    .distinct()
                    .count();

            entry.setHighestPercentage(highest);

            // Round average to 2 decimal places
            entry.setAveragePercentage(
                    Math.round(average * 100) / 100.0
            );

            entry.setQuizzesCompleted(completed);

            entries.add(entry);
        }

        // =========================================================
        // RANKING RULES
        // =========================================================

        Comparator<LeaderboardEntry> comparator;

        String selectedRank = rankBy == null
                ? ""
                : rankBy.trim().toLowerCase();

        switch (selectedRank) {

            // -----------------------------------------------------
            // AVERAGE SCORE
            // -----------------------------------------------------
            //
            // Example:
            //
            // Sarina:
            // Highest = 100%
            // Average = 66%
            //
            // Prabhat:
            // Highest = 100%
            // Average = 80%
            //
            // RESULT:
            // Prabhat #1
            // Sarina  #2
            // -----------------------------------------------------

            case "average":

                comparator = Comparator
                        .comparingDouble(
                                LeaderboardEntry::getAveragePercentage
                        )
                        .reversed()

                        // If average is equal,
                        // higher highest score wins
                        .thenComparing(
                                Comparator
                                        .comparingDouble(
                                                LeaderboardEntry::getHighestPercentage
                                        )
                                        .reversed()
                        )

                        // If still equal,
                        // more completed quizzes wins
                        .thenComparing(
                                Comparator
                                        .comparingLong(
                                                LeaderboardEntry::getQuizzesCompleted
                                        )
                                        .reversed()
                        )

                        // Final tie-breaker:
                        // alphabetical username
                        .thenComparing(
                                entry -> entry.getStudent().getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        );

                break;

            // -----------------------------------------------------
            // QUIZZES COMPLETED
            // -----------------------------------------------------

            case "completed":

                comparator = Comparator
                        .comparingLong(
                                LeaderboardEntry::getQuizzesCompleted
                        )
                        .reversed()

                        // If completed count is equal,
                        // higher average wins
                        .thenComparing(
                                Comparator
                                        .comparingDouble(
                                                LeaderboardEntry::getAveragePercentage
                                        )
                                        .reversed()
                        )

                        // If still equal,
                        // higher highest score wins
                        .thenComparing(
                                Comparator
                                        .comparingDouble(
                                                LeaderboardEntry::getHighestPercentage
                                        )
                                        .reversed()
                        )

                        // Final tie-breaker
                        .thenComparing(
                                entry -> entry.getStudent().getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        );

                break;

            // -----------------------------------------------------
            // HIGHEST SCORE
            // -----------------------------------------------------

            default:

                comparator = Comparator
                        .comparingDouble(
                                LeaderboardEntry::getHighestPercentage
                        )
                        .reversed()

                        // If highest score is equal,
                        // higher average wins
                        .thenComparing(
                                Comparator
                                        .comparingDouble(
                                                LeaderboardEntry::getAveragePercentage
                                        )
                                        .reversed()
                        )

                        // If still equal,
                        // more completed quizzes wins
                        .thenComparing(
                                Comparator
                                        .comparingLong(
                                                LeaderboardEntry::getQuizzesCompleted
                                        )
                                        .reversed()
                        )

                        // Final tie-breaker
                        .thenComparing(
                                entry -> entry.getStudent().getUsername(),
                                String.CASE_INSENSITIVE_ORDER
                        );

                break;
        }

        // ---------------------------------------------------------
        // Return sorted leaderboard
        // ---------------------------------------------------------

        return entries.stream()
                .sorted(comparator)
                .toList();
    }

    // =========================================================
    // QUIZ STATISTICS
    // =========================================================

    public QuizStats getQuizStats(Quiz quiz) {

        List<QuizAttempt> submitted =
                attemptRepository
                        .findByQuizAndStatusOrderByScoreDescEndTimeAsc(
                                quiz,
                                AttemptStatus.SUBMITTED
                        );

        submitted.addAll(
                attemptRepository
                        .findByQuizAndStatusOrderByScoreDescEndTimeAsc(
                                quiz,
                                AttemptStatus.TIME_EXPIRED
                        )
        );

        QuizStats stats = new QuizStats();

        stats.setQuiz(quiz);
        stats.setTotalAttempts(submitted.size());

        if (submitted.isEmpty()) {

            stats.setAverageScore(0);
            stats.setAveragePercentage(0);
            stats.setHighestPercentage(0);
            stats.setLowestPercentage(0);

            return stats;
        }

        double avgScore = submitted.stream()
                .mapToInt(QuizAttempt::getScore)
                .average()
                .orElse(0);

        double avgPercentage = submitted.stream()
                .mapToDouble(QuizAttempt::getPercentage)
                .average()
                .orElse(0);

        double highestPercentage = submitted.stream()
                .mapToDouble(QuizAttempt::getPercentage)
                .max()
                .orElse(0);

        double lowestPercentage = submitted.stream()
                .mapToDouble(QuizAttempt::getPercentage)
                .min()
                .orElse(0);

        stats.setAverageScore(
                Math.round(avgScore * 100) / 100.0
        );

        stats.setAveragePercentage(
                Math.round(avgPercentage * 100) / 100.0
        );

        stats.setHighestPercentage(highestPercentage);
        stats.setLowestPercentage(lowestPercentage);

        return stats;
    }

    // =========================================================
    // STUDENT STATISTICS
    // =========================================================

    public StudentStats getStudentStats(User student) {

        List<QuizAttempt> attempts =
                attemptRepository
                        .findByStudentOrderByStartTimeDesc(student)
                        .stream()
                        .filter(a ->
                                a.getStatus() != AttemptStatus.IN_PROGRESS
                        )
                        .toList();

        StudentStats stats = new StudentStats();

        stats.setTotalAttempts(attempts.size());

        stats.setQuizzesTaken(
                attempts.stream()
                        .map(a -> a.getQuiz().getId())
                        .distinct()
                        .count()
        );

        if (attempts.isEmpty()) {

            stats.setAveragePercentage(0);
            stats.setBestPercentage(0);
            stats.setTotalPassed(0);
            stats.setTotalFailed(0);
            stats.setTotalQuestionsAnswered(0);

            return stats;
        }

        double averagePercentage = attempts.stream()
                .mapToDouble(QuizAttempt::getPercentage)
                .average()
                .orElse(0);

        double bestPercentage = attempts.stream()
                .mapToDouble(QuizAttempt::getPercentage)
                .max()
                .orElse(0);

        long passed = attempts.stream()
                .filter(QuizAttempt::isPassed)
                .count();

        long questionsAnswered = attempts.stream()
                .mapToLong(a ->
                        (a.getCorrectAnswers() == null
                                ? 0
                                : a.getCorrectAnswers())
                        +
                        (a.getIncorrectAnswers() == null
                                ? 0
                                : a.getIncorrectAnswers())
                )
                .sum();

        stats.setAveragePercentage(
                Math.round(averagePercentage * 100) / 100.0
        );

        stats.setBestPercentage(bestPercentage);
        stats.setTotalPassed(passed);
        stats.setTotalFailed(attempts.size() - passed);
        stats.setTotalQuestionsAnswered(questionsAnswered);

        return stats;
    }

    // =========================================================
    // ADMIN DASHBOARD STATISTICS
    // =========================================================

    public DashboardStats getDashboardStats(
            List<Quiz> allQuizzes,
            List<User> allStudents,
            List<QuizAttempt> allAttempts) {

        DashboardStats stats = new DashboardStats();

        // ---------------------------------------------------------
        // Basic counts
        // ---------------------------------------------------------

        stats.setTotalStudents(allStudents.size());
        stats.setTotalQuizzes(allQuizzes.size());

        long published = allQuizzes.stream()
                .filter(q -> q.getStatus() == QuizStatus.PUBLISHED)
                .count();

        stats.setPublishedQuizzes(published);

        stats.setDraftQuizzes(
                allQuizzes.size() - published
        );

        // ---------------------------------------------------------
        // Questions
        // ---------------------------------------------------------

        stats.setTotalQuestions(
                allQuizzes.stream()
                        .mapToInt(Quiz::getQuestionCount)
                        .sum()
        );

        // ---------------------------------------------------------
        // Completed attempts
        // ---------------------------------------------------------

        List<QuizAttempt> completed =
                allAttempts.stream()
                        .filter(a ->
                                a.getStatus() != AttemptStatus.IN_PROGRESS
                        )
                        .toList();

        stats.setTotalAttempts(completed.size());

        double averageScore = completed.stream()
                .mapToDouble(QuizAttempt::getPercentage)
                .average()
                .orElse(0);

        stats.setAverageScore(
                Math.round(averageScore * 100) / 100.0
        );

        long passed = completed.stream()
                .filter(QuizAttempt::isPassed)
                .count();

        stats.setTotalPassed(passed);
        stats.setTotalFailed(completed.size() - passed);

        // ---------------------------------------------------------
        // Date information
        // ---------------------------------------------------------

        DateTimeFormatter dayLabel =
                DateTimeFormatter.ofPattern("MMM d");

        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(13);

        // ---------------------------------------------------------
        // Attempts over time
        // ---------------------------------------------------------

        Map<String, Long> attemptsOverTime =
                new LinkedHashMap<>();

        for (int i = 13; i >= 0; i--) {

            attemptsOverTime.put(
                    today.minusDays(i).format(dayLabel),
                    0L
            );
        }

        for (QuizAttempt attempt : completed) {

            if (attempt.getStartTime() == null) {
                continue;
            }

            LocalDate day =
                    attempt.getStartTime().toLocalDate();

            if (!day.isBefore(windowStart)) {

                attemptsOverTime.merge(
                        day.format(dayLabel),
                        1L,
                        Long::sum
                );
            }
        }

        stats.setAttemptsOverTime(attemptsOverTime);

        // ---------------------------------------------------------
        // Student registrations over time
        // ---------------------------------------------------------

        Map<String, Long> registrationsOverTime =
                new LinkedHashMap<>();

        for (int i = 13; i >= 0; i--) {

            registrationsOverTime.put(
                    today.minusDays(i).format(dayLabel),
                    0L
            );
        }

        for (User user : allStudents) {

            if (user.getCreatedAt() == null) {
                continue;
            }

            LocalDate day =
                    user.getCreatedAt().toLocalDate();

            if (!day.isBefore(windowStart)) {

                registrationsOverTime.merge(
                        day.format(dayLabel),
                        1L,
                        Long::sum
                );
            }
        }

        stats.setRegistrationsOverTime(
                registrationsOverTime
        );

        // ---------------------------------------------------------
        // Average score per quiz
        // ---------------------------------------------------------

        Map<String, Double> avgScorePerQuiz =
                new LinkedHashMap<>();

        for (Quiz quiz : allQuizzes) {

            List<QuizAttempt> forQuiz =
                    completed.stream()
                            .filter(a ->
                                    a.getQuiz()
                                            .getId()
                                            .equals(quiz.getId())
                            )
                            .toList();

            if (!forQuiz.isEmpty()) {

                double avg =
                        forQuiz.stream()
                                .mapToDouble(
                                        QuizAttempt::getPercentage
                                )
                                .average()
                                .orElse(0);

                avgScorePerQuiz.put(
                        quiz.getTitle(),
                        Math.round(avg * 100) / 100.0
                );
            }
        }

        stats.setAvgScorePerQuiz(avgScorePerQuiz);

        // ---------------------------------------------------------
        // Popular quizzes
        // ---------------------------------------------------------

        Map<String, Long> popularQuizzes =
                completed.stream()
                        .collect(
                                Collectors.groupingBy(
                                        a -> a.getQuiz().getTitle(),
                                        Collectors.counting()
                                )
                        )
                        .entrySet()
                        .stream()
                        .sorted(
                                Map.Entry
                                        .<String, Long>
                                        comparingByValue()
                                        .reversed()
                        )
                        .limit(5)
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                )
                        );

        stats.setPopularQuizzes(popularQuizzes);

        // ---------------------------------------------------------
        // Popular categories
        // ---------------------------------------------------------

        Map<String, Long> popularCategories =
                completed.stream()
                        .collect(
                                Collectors.groupingBy(
                                        a -> a.getQuiz().getCategory() != null
                                                ? a.getQuiz()
                                                        .getCategory()
                                                        .getName()
                                                : "Uncategorized",
                                        Collectors.counting()
                                )
                        )
                        .entrySet()
                        .stream()
                        .sorted(
                                Map.Entry
                                        .<String, Long>
                                        comparingByValue()
                                        .reversed()
                        )
                        .limit(5)
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        (a, b) -> a,
                                        LinkedHashMap::new
                                )
                        );

        stats.setPopularCategories(popularCategories);

        return stats;
    }
}