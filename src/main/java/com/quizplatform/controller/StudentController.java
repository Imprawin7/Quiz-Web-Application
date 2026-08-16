package com.quizplatform.controller;

import com.quizplatform.model.AttemptStatus;
import com.quizplatform.model.Category;
import com.quizplatform.model.Difficulty;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.User;
import com.quizplatform.repository.CategoryRepository;
import com.quizplatform.service.AnalyticsService;
import com.quizplatform.service.QuizAttemptService;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final QuizService quizService;
    private final QuizAttemptService quizAttemptService;
    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;

    private User currentUser(Authentication auth) {
        return userService.getByUsername(auth.getName());
    }

    // =========================================================
    // STUDENT DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {

        User student = currentUser(auth);

        List<Quiz> availableQuizzes =
                quizService.findActiveForStudents();

        List<QuizAttempt> recentAttempts =
                quizAttemptService.findByStudent(student)
                        .stream()
                        .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                        .limit(5)
                        .toList();

        model.addAttribute("student", student);
        model.addAttribute("availableQuizzes", availableQuizzes);
        model.addAttribute("recentAttempts", recentAttempts);
        model.addAttribute(
                "stats",
                analyticsService.getStudentStats(student)
        );

        return "student/dashboard";
    }


    // =========================================================
    // AVAILABLE QUIZZES
    // =========================================================

    @GetMapping("/quizzes")
    public String browseQuizzes(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Difficulty difficulty,
            @RequestParam(required = false) Integer maxDuration,
            @RequestParam(
                    required = false,
                    defaultValue = "newest"
            ) String sortBy,
            Model model) {

        List<Quiz> quizzes =
                quizService.searchActiveForStudents(
                        search,
                        categoryId,
                        difficulty,
                        maxDuration
                );

        if ("popularity".equals(sortBy)) {

            quizzes = quizzes.stream()
                    .sorted(
                            Comparator.comparingLong(
                                    (Quiz q) ->
                                            quizAttemptService
                                                    .countAttemptsForQuiz(q)
                            ).reversed()
                    )
                    .toList();
        }

        // "newest" is already the natural order returned
        // by the repository.

        List<Category> categories =
                categoryRepository.findAll();

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("categories", categories);
        model.addAttribute(
                "difficulties",
                Difficulty.values()
        );
        model.addAttribute("search", search);
        model.addAttribute(
                "selectedCategoryId",
                categoryId
        );
        model.addAttribute(
                "selectedDifficulty",
                difficulty
        );
        model.addAttribute(
                "selectedMaxDuration",
                maxDuration
        );
        model.addAttribute(
                "selectedSortBy",
                sortBy
        );

        return "student/quizzes";
    }


    // =========================================================
    // STUDENT HISTORY
    // =========================================================

    @GetMapping("/history")
    public String history(
            Model model,
            Authentication auth) {

        User student = currentUser(auth);

        List<QuizAttempt> attempts =
                quizAttemptService.findByStudent(student)
                        .stream()
                        .filter(
                                a -> a.getStatus()
                                        != AttemptStatus.IN_PROGRESS
                        )
                        .toList();

        model.addAttribute("attempts", attempts);

        return "student/history";
    }


    // =========================================================
    // STUDENT PROFILE
    // =========================================================

    @GetMapping("/profile")
    public String profile(
            Model model,
            Authentication auth) {

        User student = currentUser(auth);

        model.addAttribute("student", student);

        return "student/profile";
    }
}