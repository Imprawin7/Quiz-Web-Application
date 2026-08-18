package com.Quizvera.controller;

import com.Quizvera.model.*;
import com.Quizvera.repository.CategoryRepository;
import com.Quizvera.service.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizService quizService;
    private final QuestionService questionService;
    private final QuizAttemptService quizAttemptService;
    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final CategoryRepository categoryRepository;

    private User currentUser(Authentication auth) {
        return userService.getByUsername(auth.getName());
    }

    /** Landing page before starting — shows quiz info, rules, and attempts used so far. */
    @GetMapping("/quiz/{id}/start")
    public String startPage(@PathVariable Long id, Model model, Authentication auth,
                             @RequestParam(required = false) String error) {
        Quiz quiz = quizService.getById(id);
        User student = currentUser(auth);
        long attemptsUsed = quizAttemptService.getCompletedAttemptCount(quiz, student);

        model.addAttribute("quiz", quiz);
        model.addAttribute("attemptsUsed", attemptsUsed);
        model.addAttribute("attemptsRemaining", Math.max(0, quiz.getMaxAttempts() - attemptsUsed));
        model.addAttribute("maxAttemptsReached", attemptsUsed >= quiz.getMaxAttempts());
        if (error != null) {
            model.addAttribute("error", error);
        }
        return "student/quiz-start";
    }

    /** Actually begins (or resumes) the attempt and redirects into the exam view. */
    @PostMapping("/quiz/{id}/begin")
    public String begin(@PathVariable Long id, Authentication auth) {
        Quiz quiz = quizService.getById(id);
        try {
            QuizAttempt attempt = quizAttemptService.startAttempt(quiz, currentUser(auth));
            return "redirect:/quiz/attempt/" + attempt.getId();
        } catch (QuizAttemptService.MaxAttemptsExceededException e) {
            String message = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/quiz/" + id + "/start?error=" + message;
        }
    }

    /** The live exam page with countdown timer. */
    @GetMapping("/quiz/attempt/{attemptId}")
    public String takeQuiz(@PathVariable Long attemptId, Model model, Authentication auth) {
        QuizAttempt attempt = quizAttemptService.getById(attemptId);
        guardOwnership(attempt, auth);

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return "redirect:/quiz/result/" + attempt.getId();
        }

        if (quizAttemptService.isExpired(attempt)) {
            quizAttemptService.autoSubmitExpired(attempt);
            return "redirect:/quiz/result/" + attempt.getId();
        }

        List<Question> questions = questionService.findByQuiz(attempt.getQuiz());
        model.addAttribute("attempt", attempt);
        model.addAttribute("quiz", attempt.getQuiz());
        model.addAttribute("questions", questions);
        model.addAttribute("remainingSeconds", quizAttemptService.getRemainingSeconds(attempt));
        return "student/attempt";
    }

    /** Handles both manual submission and timer-expired auto-submission. */
    @PostMapping("/quiz/attempt/{attemptId}/submit")
    public String submit(@PathVariable Long attemptId, Authentication auth, HttpServletRequest request) {
        QuizAttempt attempt = quizAttemptService.getById(attemptId);
        guardOwnership(attempt, auth);

        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            Map<Long, String> answers = new HashMap<>();
            List<Question> questions = questionService.findByQuiz(attempt.getQuiz());
            for (Question q : questions) {
                String selected = request.getParameter("q_" + q.getId());
                if (selected != null && !selected.isBlank()) {
                    answers.put(q.getId(), selected);
                }
            }
            quizAttemptService.submitAttempt(attempt, answers);
        }
        return "redirect:/quiz/result/" + attempt.getId();
    }

    @GetMapping("/quiz/result/{attemptId}")
    public String result(@PathVariable Long attemptId, Model model, Authentication auth) {
        QuizAttempt attempt = quizAttemptService.getById(attemptId);
        guardOwnership(attempt, auth);

        model.addAttribute("attempt", attempt);
        model.addAttribute("answers", quizAttemptService.getAnswers(attempt));
        return "student/result";
    }

    /** Per-quiz leaderboard (ranked by score on that one quiz). */
    @GetMapping("/leaderboard/{quizId}")
    public String leaderboard(@PathVariable Long quizId, Model model) {
        Quiz quiz = quizService.getById(quizId);
        model.addAttribute("quiz", quiz);
        model.addAttribute("attempts", quizAttemptService.findLeaderboard(quiz));
        return "leaderboard";
    }

    /**
     * Main leaderboard: Overall or Category-wise, All-time / Weekly / Monthly,
     * ranked by Highest Score, Average Score, or Quizzes Completed.
     */
    @GetMapping("/leaderboard")
    public String leaderboardIndex(@RequestParam(required = false) Long categoryId,
                                    @RequestParam(required = false, defaultValue = "all") String period,
                                    @RequestParam(required = false, defaultValue = "highest") String rankBy,
                                    Model model) {

        List<QuizAttempt> allAttempts = quizAttemptService.findAllAttempts();
        List<AnalyticsService.LeaderboardEntry> leaderboard =
                analyticsService.getLeaderboard(allAttempts, categoryId, period, rankBy);

        model.addAttribute("leaderboard", leaderboard);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("selectedRankBy", rankBy);
        return "leaderboard-index";
    }

    /** List of quizzes to browse per-quiz leaderboards from (the old /leaderboard behaviour). */
    @GetMapping("/leaderboard/by-quiz")
    public String leaderboardByQuiz(Model model) {
        model.addAttribute("quizzes", quizService.findActiveForStudents());
        return "leaderboard-by-quiz";
    }

    private void guardOwnership(QuizAttempt attempt, Authentication auth) {
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && !attempt.getStudent().getUsername().equals(currentUsername)) {
            throw new org.springframework.security.access.AccessDeniedException("Not your quiz attempt");
        }
    }
}