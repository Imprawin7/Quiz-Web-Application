# Quizvera
A complete, production-style **Java Spring Boot** web application for creating, taking, and
grading online quizzes — with separate Admin and Student experiences, a live countdown timer,
automatic scoring, performance analytics, and leaderboards.

## Tech Stack

| Layer          | Technology                                              |
|----------------|----------------------------------------------------------|
| Language       | Java 17                                                   |
| Framework      | Spring Boot 3.2 (Web, Security, Data JPA, Validation)     |
| View layer     | Thymeleaf + Bootstrap 5 (responsive, mobile-friendly)     |
| Database       | H2 (file-based, zero setup) — MySQL config included too  |
| Auth           | Spring Security, BCrypt password hashing, role-based ACL |
| Build tool     | Maven                                                     |

## Features implemented

- **Secure authentication** — BCrypt-hashed passwords, Spring Security form login, CSRF protection.
- **Admin & Student roles** — enforced at the URL level (`/admin/**` vs `/student/**`).
- **Admin**: create/edit/activate/deactivate/delete quizzes, add/edit/delete multiple-choice
  questions per quiz, view per-quiz analytics and a full leaderboard of every attempt.
- **Student**: register an account, browse active quizzes, take a quiz with a **live countdown
  timer** that **auto-submits** when time runs out, view detailed results with answer review,
  see quiz history, and check the leaderboard.
- **Automatic scoring** — every submission is scored server-side against the stored correct
  answers the instant it's submitted (or the instant the timer expires).
- **Quiz attempts & results storage** — every attempt (in-progress, submitted, or auto-expired)
  and every individual answer is persisted for full auditability.
- **Performance analytics** — average score, average %, highest/lowest % per quiz (admin), and
  average/best % across all attempts (student).
- **Leaderboard** — ranked by score per quiz, with medal styling for the top 3.
- **Responsive UI** — Bootstrap 5 grid, works on mobile/tablet/desktop.

## Getting started

### Prerequisites
- JDK 17+
- Maven 3.8+ (or use the Maven wrapper if you add one)

### Run it

```bash
cd online-quiz-platform
mvn spring-boot:run
```

The app starts on **https://quiz-web-application-kl7g.onrender.com**.

### Default admin login

On first startup, a default admin account is created automatically:

```

```

Change these via `application.properties` (`app.admin.username` / `app.admin.password`) before
first run in a real deployment, or update the admin's password directly afterward.

### Student accounts

Students self-register at `/register`.

### Database

By default the app uses an embedded **H2** database persisted to `./data/quizdb` (file-based, so
your data survives restarts). Browse it at `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:file:./data/quizdb`, user `sa`, empty password).

To use **MySQL** instead: open `src/main/resources/application.properties`, comment out the H2
block, and uncomment/configure the MySQL block (set your DB name, username, password). Then add
your MySQL server details and the app will auto-create the schema (`ddl-auto=update`).

## Project structure

```
src/main/java/com/Quizvera/
├── QuizveraApplication.java     # entry point
├── config/
│   ├── SecurityConfig.java          # auth rules, password encoder, login/logout
│   └── DataInitializer.java         # seeds the default admin account
├── model/                           # JPA entities: User, Quiz, Question, QuizAttempt, StudentAnswer
├── repository/                      # Spring Data JPA repositories
├── service/                         # business logic (scoring, timer checks, analytics)
└── controller/                      # MVC controllers (Auth, Admin, Student, QuizAttempt)

src/main/resources/
├── application.properties
├── static/css/style.css             # custom styling on top of Bootstrap
└── templates/                       # Thymeleaf views (admin/, student/, shared fragments)
```

## How the timer & auto-scoring work

1. Starting a quiz creates a `QuizAttempt` row with `startTime = now()`. If the student refreshes
   the page or navigates away and comes back, the **same** in-progress attempt is resumed (the
   timer is always calculated from the original `startTime`, not the browser), so it can't be
   reset by refreshing.
2. The exam page (`student/attempt.html`) receives the server-calculated `remainingSeconds` and
   runs a JavaScript countdown from there. When it hits zero, the form **auto-submits**.
3. On submit (manual or auto), `QuizAttemptService.submitAttempt()` re-checks server-side whether
   the deadline has actually passed (never trusts the client), grades every question against its
   stored `correctOption`, sums the marks, and marks the attempt `SUBMITTED` or `TIME_EXPIRED`.

## Extending the project

Ideas for further work:
- Add question types beyond single-correct-answer MCQ (multi-select, true/false, short answer).
- Add quiz categories/tags filtering on the student browse page.
- Export analytics/leaderboards to CSV or PDF.
- Add email notifications on quiz completion.
- Add pagination for large question banks / attempt lists.
