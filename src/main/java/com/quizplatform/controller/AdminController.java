package com.quizplatform.controller;
import com.quizplatform.model.AttemptStatus;
import com.quizplatform.model.Category;
import com.quizplatform.model.Difficulty;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.model.User;
import com.quizplatform.repository.CategoryRepository;
import com.quizplatform.service.AnalyticsService;
import com.quizplatform.service.QuestionService;
import com.quizplatform.service.QuizAttemptService;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final QuizService quizService;
    private final QuestionService questionService;
    private final QuizAttemptService quizAttemptService;
    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final CategoryRepository categoryRepository;

    private User currentUser(Authentication auth) {
        return userService.getByUsername(auth.getName());
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication auth) {

        List<Quiz> quizzes = quizService.findAllForAdmin();
        List<User> students = userService.listStudents();
        List<QuizAttempt> allAttempts = quizAttemptService.findAllAttempts();

        AnalyticsService.DashboardStats stats =
                analyticsService.getDashboardStats(quizzes, students, allAttempts);

        model.addAttribute("quizzes", quizzes);
        model.addAttribute("stats", stats);
        model.addAttribute("admin", currentUser(auth));

        return "admin/dashboard";
    }

    // ---------------- Quiz management ----------------

    @GetMapping("/quizzes")
    public String listQuizzes(Model model) {

        model.addAttribute("quizzes", quizService.findAllForAdmin());

        return "admin/quizzes";
    }

    @GetMapping("/quizzes/new")
    public String newQuizForm(Model model) {

        model.addAttribute("quiz", new Quiz());

        List<Category> categories = categoryRepository.findAll();

        model.addAttribute("categories", categories);
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("statuses", QuizStatus.values());
        model.addAttribute("isEdit", false);

        return "admin/quiz-form";
    }

    @PostMapping("/quizzes")
    public String createQuiz(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Long categoryId,
            @RequestParam Difficulty difficulty,
            @RequestParam Integer durationMinutes,
            @RequestParam QuizStatus status,
            @RequestParam Integer passingPercentage,
            @RequestParam Integer maxAttempts,
            @RequestParam(required = false) String thumbnailUrl,
            Authentication auth) {

        quizService.createQuiz(
                title,
                description,
                categoryId,
                difficulty,
                durationMinutes,
                status,
                passingPercentage,
                maxAttempts,
                thumbnailUrl,
                currentUser(auth)
        );

        return "redirect:/admin/quizzes";
    }

    @GetMapping("/quizzes/{id}/edit")
    public String editQuizForm(
            @PathVariable Long id,
            Model model) {

        model.addAttribute("quiz", quizService.getById(id));
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("statuses", QuizStatus.values());
        model.addAttribute("isEdit", true);

        return "admin/quiz-form";
    }

    @PostMapping("/quizzes/{id}")
    public String updateQuiz(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Long categoryId,
            @RequestParam Difficulty difficulty,
            @RequestParam Integer durationMinutes,
            @RequestParam QuizStatus status,
            @RequestParam Integer passingPercentage,
            @RequestParam Integer maxAttempts,
            @RequestParam(required = false) String thumbnailUrl) {

        quizService.updateQuiz(
                id,
                title,
                description,
                categoryId,
                difficulty,
                durationMinutes,
                status,
                passingPercentage,
                maxAttempts,
                thumbnailUrl
        );

        return "redirect:/admin/quizzes";
    }

    @PostMapping("/quizzes/{id}/delete")
    public String deleteQuiz(@PathVariable Long id) {

        quizService.deleteQuiz(id);

        return "redirect:/admin/quizzes";
    }

    @PostMapping("/quizzes/{id}/toggle")
    public String toggleQuiz(@PathVariable Long id) {

        quizService.toggleActive(id);

        return "redirect:/admin/quizzes";
    }

    // ---------------- Question management ----------------

    @GetMapping("/quizzes/{quizId}/questions")
    public String listQuestions(
            @PathVariable Long quizId,
            Model model) {

        Quiz quiz = quizService.getById(quizId);

        model.addAttribute("quiz", quiz);
        model.addAttribute(
                "questions",
                questionService.findByQuiz(quiz)
        );

        return "admin/questions";
    }

    @GetMapping("/quizzes/{quizId}/questions/new")
    public String newQuestionForm(
            @PathVariable Long quizId,
            Model model) {

        model.addAttribute(
                "quiz",
                quizService.getById(quizId)
        );

        model.addAttribute("question", new Question());
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("isEdit", false);

        return "admin/question-form";
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public String createQuestion(
            @PathVariable Long quizId,
            @RequestParam String questionText,
            @RequestParam String optionA,
            @RequestParam String optionB,
            @RequestParam String optionC,
            @RequestParam String optionD,
            @RequestParam String correctOption,
            @RequestParam(required = false) String explanation,
            @RequestParam Difficulty difficulty,
            @RequestParam Integer marks) {

        Quiz quiz = quizService.getById(quizId);

        questionService.createQuestion(
                quiz,
                questionText,
                optionA,
                optionB,
                optionC,
                optionD,
                correctOption,
                explanation,
                difficulty,
                marks
        );

        return "redirect:/admin/quizzes/" + quizId + "/questions";
    }

    @GetMapping("/quizzes/{quizId}/questions/{qid}/edit")
    public String editQuestionForm(
            @PathVariable Long quizId,
            @PathVariable Long qid,
            Model model) {

        model.addAttribute(
                "quiz",
                quizService.getById(quizId)
        );

        model.addAttribute(
                "question",
                questionService.getById(qid)
        );

        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("isEdit", true);

        return "admin/question-form";
    }

    @PostMapping("/quizzes/{quizId}/questions/{qid}")
    public String updateQuestion(
            @PathVariable Long quizId,
            @PathVariable Long qid,
            @RequestParam String questionText,
            @RequestParam String optionA,
            @RequestParam String optionB,
            @RequestParam String optionC,
            @RequestParam String optionD,
            @RequestParam String correctOption,
            @RequestParam(required = false) String explanation,
            @RequestParam Difficulty difficulty,
            @RequestParam Integer marks) {

        questionService.updateQuestion(
                qid,
                questionText,
                optionA,
                optionB,
                optionC,
                optionD,
                correctOption,
                explanation,
                difficulty,
                marks
        );

        return "redirect:/admin/quizzes/" + quizId + "/questions";
    }

    @PostMapping("/quizzes/{quizId}/questions/{qid}/delete")
    public String deleteQuestion(
            @PathVariable Long quizId,
            @PathVariable Long qid) {

        questionService.deleteQuestion(qid);

        return "redirect:/admin/quizzes/" + quizId + "/questions";
    }

    // ---------------- Category management ----------------

    @GetMapping("/categories")
    public String listCategories(Model model) {

        model.addAttribute("categories", categoryRepository.findAll());

        return "admin/categories";
    }

    @PostMapping("/categories")
    public String createCategory(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            Model model) {

        boolean duplicate = categoryRepository.findAll().stream()
                .anyMatch(c -> c.getName().equalsIgnoreCase(name.trim()));

        if (duplicate) {
            model.addAttribute("error", "A category named \"" + name + "\" already exists.");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/categories";
        }

        Category category = new Category();
        category.setName(name.trim());
        category.setDescription(description);
        categoryRepository.save(category);

        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            Model model) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        boolean duplicate = categoryRepository.findAll().stream()
                .anyMatch(c -> !c.getId().equals(id) && c.getName().equalsIgnoreCase(name.trim()));

        if (duplicate) {
            model.addAttribute("error", "A category named \"" + name + "\" already exists.");
            model.addAttribute("categories", categoryRepository.findAll());
            return "admin/categories";
        }

        category.setName(name.trim());
        category.setDescription(description);
        categoryRepository.save(category);

        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id) {

        categoryRepository.deleteById(id);

        return "redirect:/admin/categories";
    }

    /** Module 8: view every quiz filed under a given category. */
    @GetMapping("/categories/{id}/quizzes")
    public String categoryQuizzes(@PathVariable Long id, Model model) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        model.addAttribute("category", category);
        model.addAttribute("quizzes", quizService.findByCategory(category));

        return "admin/category-quizzes";
    }

    // ---------------- Student / user management ----------------

    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String search, Model model) {

        List<User> students = userService.searchStudents(search);

        Map<Long, AnalyticsService.StudentStats> statsMap = new HashMap<>();
        for (User student : students) {
            statsMap.put(student.getId(), analyticsService.getStudentStats(student));
        }

        model.addAttribute("students", students);
        model.addAttribute("statsMap", statsMap);
        model.addAttribute("search", search);

        return "admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(
            @PathVariable Long id,
            Model model) {

        User student = userService.getById(id);

        model.addAttribute("student", student);
        model.addAttribute(
                "attempts",
                quizAttemptService.findByStudent(student).stream()
                        .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                        .toList()
        );
        model.addAttribute("stats", analyticsService.getStudentStats(student));

        return "admin/user-detail";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id) {

        userService.toggleEnabled(id);

        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {

        userService.deleteAccount(id);

        return "redirect:/admin/users";
    }

    // ---------------- Analytics ----------------

    @GetMapping("/analytics")
    public String analytics(Model model) {

        List<Quiz> quizzes = quizService.findAllForAdmin();

        List<AnalyticsService.QuizStats> statsList =
                quizzes.stream()
                        .map(analyticsService::getQuizStats)
                        .toList();

        model.addAttribute("statsList", statsList);

        return "admin/analytics";
    }

    @GetMapping("/quizzes/{id}/attempts")
    public String quizAttempts(
            @PathVariable Long id,
            Model model) {

        Quiz quiz = quizService.getById(id);

        model.addAttribute("quiz", quiz);
        model.addAttribute(
                "attempts",
                quizAttemptService.findLeaderboard(quiz)
        );
        model.addAttribute(
                "stats",
                analyticsService.getQuizStats(quiz)
        );

        return "admin/quiz-attempts";
    }

    /** Platform-wide view of every attempt across every quiz. */
    @GetMapping("/attempts")
    public String allAttempts(Model model) {

        model.addAttribute("attempts", quizAttemptService.findAllAttempts());

        return "admin/all-attempts";
    }
}