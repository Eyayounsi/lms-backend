package com.elearning.ProjetPfe.service.learning;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.entity.learning.AttemptAnswer;
import com.elearning.ProjetPfe.entity.learning.Choice;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.learning.Question;
import com.elearning.ProjetPfe.entity.learning.QuestionType;
import com.elearning.ProjetPfe.entity.learning.Quiz;
import com.elearning.ProjetPfe.entity.learning.QuizAttempt;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.learning.ChoiceRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.course.LessonRepository;
import com.elearning.ProjetPfe.repository.learning.QuestionRepository;
import com.elearning.ProjetPfe.repository.learning.QuizAttemptRepository;
import com.elearning.ProjetPfe.repository.learning.QuizRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class QuizService {

    @Autowired private QuizRepository quizRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private ChoiceRepository choiceRepository;
    @Autowired private QuizAttemptRepository attemptRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @PersistenceContext private EntityManager entityManager;

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — CRUD Quiz
    // ═══════════════════════════════════════════════════════════════════════

    /** Lister tous les quizzes de l'instructor (cours + leçons) */
    public List<Map<String, Object>> getInstructorQuizzes(User instructor) {
        List<Quiz> fromCourse = quizRepository.findByCourseInstructorIdOrderByCreatedAtDesc(instructor.getId());
        List<Quiz> fromLesson = quizRepository.findByLessonSectionCourseInstructorIdOrderByCreatedAtDesc(instructor.getId());
        List<Quiz> all = new ArrayList<>(fromCourse);
        fromLesson.stream()
            .filter(q -> fromCourse.stream().noneMatch(c -> c.getId().equals(q.getId())))
            .forEach(all::add);
        all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return all.stream().map(this::mapQuizToDto).collect(Collectors.toList());
    }

    /** Obtenir (ou vérifier) le quiz d'une leçon */
    public Map<String, Object> getQuizByLesson(Long lessonId, User instructor) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Leçon introuvable"));
        if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }
        return quizRepository.findFirstByLessonId(lessonId)
                .map(this::mapQuizToDto)
                .orElse(null);
    }

    /** Lister les quizzes d'un cours spécifique */
    public List<Map<String, Object>> getQuizzesByCourse(Long courseId, User instructor) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));
        if (!course.getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }
        return quizRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream().map(this::mapQuizToDto).collect(Collectors.toList());
    }

    /** Créer un quiz (optionnellement lié à une leçon + questions inline) */
    @Transactional
    @SuppressWarnings("unchecked")
    public Map<String, Object> createQuiz(Map<String, Object> data, User instructor) {
        // ── Résoudre cours / leçon ──────────────────────────────────────
        Course course;
        Lesson lesson = null;

        if (data.containsKey("lessonId") && data.get("lessonId") != null) {
            Long lessonId = Long.valueOf(data.get("lessonId").toString());
            lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new RuntimeException("Leçon introuvable"));
            if (!lesson.getSection().getCourse().getInstructor().getId().equals(instructor.getId())) {
                throw new RuntimeException("Accès non autorisé");
            }
            if (quizRepository.existsByLessonId(lessonId)) {
                throw new RuntimeException("Cette leçon possède déjà un quiz");
            }
            course = lesson.getSection().getCourse();
        } else {
            Long courseId = Long.valueOf(data.get("courseId").toString());
            course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Cours introuvable"));
            if (!course.getInstructor().getId().equals(instructor.getId())) {
                throw new RuntimeException("Accès non autorisé");
            }
        }

        // ── Valider les questions inline ────────────────────────────────
        List<Map<String, Object>> questionsData = null;
        if (data.containsKey("questions") && data.get("questions") != null) {
            questionsData = (List<Map<String, Object>>) data.get("questions");
            if (questionsData.isEmpty()) {
                throw new RuntimeException("Le quiz doit contenir au moins une question");
            }
            for (Map<String, Object> qData : questionsData) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) qData.get("choices");
                if (choices == null || choices.isEmpty()) {
                    throw new RuntimeException("Chaque question doit avoir au moins un choix");
                }
                boolean hasCorrect = choices.stream()
                        .anyMatch(c -> Boolean.TRUE.equals(c.get("isCorrect")) ||
                                       "true".equalsIgnoreCase(String.valueOf(c.get("isCorrect"))));
                if (!hasCorrect) {
                    throw new RuntimeException("Chaque question doit avoir au moins une réponse correcte");
                }
            }
        }

        // ── Créer le quiz ───────────────────────────────────────────────
        Quiz quiz = new Quiz();
        quiz.setTitle((String) data.get("title"));
        quiz.setTotalMarks(Integer.parseInt(data.get("totalMarks").toString()));
        quiz.setPassMark(Integer.parseInt(data.get("passMark").toString()));
        quiz.setDurationMinutes(Integer.parseInt(data.get("durationMinutes").toString()));
        quiz.setCourse(course);
        if (lesson != null) quiz.setLesson(lesson);

        quiz = quizRepository.save(quiz);

        // ── Sauvegarder les questions inline ────────────────────────────
        if (questionsData != null) {
            int order = 0;
            int totalMarks = quiz.getTotalMarks() > 0 ? quiz.getTotalMarks() : 10;
            int autoMarksPerQuestion = (int) Math.ceil((double) totalMarks / questionsData.size());
            for (Map<String, Object> qData : questionsData) {
                Question q = new Question();
                q.setQuestionText((String) qData.get("questionText"));
                q.setQuestionType(QuestionType.MULTIPLE_CHOICE);
                q.setOrderIndex(order++);
                q.setMarks(qData.containsKey("marks") ? Integer.parseInt(qData.get("marks").toString()) : autoMarksPerQuestion);
                q.setQuiz(quiz);
                q = questionRepository.save(q);

                List<Map<String, Object>> choices = (List<Map<String, Object>>) qData.get("choices");
                for (Map<String, Object> cData : choices) {
                    Choice c = new Choice();
                    c.setText((String) cData.get("text"));
                    c.setCorrect(Boolean.TRUE.equals(cData.get("isCorrect")) ||
                                 "true".equalsIgnoreCase(String.valueOf(cData.get("isCorrect"))));
                    c.setQuestion(q);
                    choiceRepository.save(c);
                }
            }
        }

        return mapQuizToDto(quizRepository.findById(quiz.getId()).orElse(quiz));
    }

    /** Modifier un quiz */
    @Transactional
    public Map<String, Object> updateQuiz(Long quizId, Map<String, Object> data, User instructor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));
        User owner = quiz.getCourse() != null
                ? quiz.getCourse().getInstructor()
                : quiz.getLesson().getSection().getCourse().getInstructor();
        if (!owner.getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        if (data.containsKey("title")) quiz.setTitle((String) data.get("title"));
        if (data.containsKey("totalMarks")) quiz.setTotalMarks(Integer.parseInt(data.get("totalMarks").toString()));
        if (data.containsKey("passMark")) quiz.setPassMark(Integer.parseInt(data.get("passMark").toString()));
        if (data.containsKey("durationMinutes")) quiz.setDurationMinutes(Integer.parseInt(data.get("durationMinutes").toString()));

        quiz = quizRepository.save(quiz);
        return mapQuizToDto(quiz);
    }

    /** Supprimer un quiz */
    @Transactional
    public void deleteQuiz(Long quizId, User instructor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));
        User owner = quiz.getCourse() != null
                ? quiz.getCourse().getInstructor()
                : quiz.getLesson().getSection().getCourse().getInstructor();
        if (!owner.getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }
        quizRepository.delete(quiz);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — CRUD Questions
    // ═══════════════════════════════════════════════════════════════════════

    /** Lister les questions d'un quiz */
    public List<Map<String, Object>> getQuestions(Long quizId, User instructor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));
        if (!quiz.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }
        return questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId)
                .stream().map(this::mapQuestionToDto).collect(Collectors.toList());
    }

    /** Ajouter une question */
    @Transactional
    public Map<String, Object> addQuestion(Long quizId, Map<String, Object> data, User instructor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));
        if (!quiz.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        long existingCount = questionRepository.countByQuizId(quizId);

        Question question = new Question();
        question.setQuestionText((String) data.get("questionText"));
        question.setQuestionType(QuestionType.valueOf((String) data.get("questionType")));
        question.setOrderIndex((int) existingCount);
        // Auto-calculate marks = ceil(totalMarks / totalQuestions) when not explicitly provided
        int autoMarks;
        if (data.containsKey("marks") && data.get("marks") != null) {
            autoMarks = Integer.parseInt(data.get("marks").toString());
        } else {
            int totalMarks = quiz.getTotalMarks() > 0 ? quiz.getTotalMarks() : 10;
            autoMarks = (int) Math.ceil((double) totalMarks / (existingCount + 1));
            // Redistribute marks evenly across existing questions too
            List<Question> existingQs = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
            for (Question eq : existingQs) {
                eq.setMarks(autoMarks);
                questionRepository.save(eq);
            }
        }
        question.setMarks(autoMarks);
        question.setQuiz(quiz);
        question = questionRepository.save(question);

        // Ajouter les choix
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
        if (choices != null) {
            for (Map<String, Object> choiceData : choices) {
                Choice choice = new Choice();
                choice.setText((String) choiceData.get("text"));
                choice.setCorrect(Boolean.TRUE.equals(choiceData.get("isCorrect")));
                choice.setQuestion(question);
                choiceRepository.save(choice);
            }
        }

        // Recharger pour obtenir les choix
        question = questionRepository.findById(question.getId()).orElseThrow();
        return mapQuestionToDto(question);
    }

    /** Modifier une question */
    @Transactional
    public Map<String, Object> updateQuestion(Long questionId, Map<String, Object> data, User instructor) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question introuvable"));
        if (!question.getQuiz().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        if (data.containsKey("questionText")) question.setQuestionText((String) data.get("questionText"));
        if (data.containsKey("questionType")) question.setQuestionType(QuestionType.valueOf((String) data.get("questionType")));
        if (data.containsKey("marks")) question.setMarks(Integer.parseInt(data.get("marks").toString()));

        // Remplacer les choix si fournis
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) data.get("choices");
        if (choices != null) {
            List<Choice> existingChoices = new ArrayList<>(question.getChoices());

            // Update existing choices or create new ones
            for (int i = 0; i < choices.size(); i++) {
                Map<String, Object> choiceData = choices.get(i);
                if (i < existingChoices.size()) {
                    // Update existing choice in place (preserves FK references in attempt_answers)
                    Choice existing = existingChoices.get(i);
                    existing.setText((String) choiceData.get("text"));
                    existing.setCorrect(Boolean.TRUE.equals(choiceData.get("isCorrect")));
                    choiceRepository.save(existing);
                } else {
                    // Create new choice
                    Choice newChoice = new Choice();
                    newChoice.setText((String) choiceData.get("text"));
                    newChoice.setCorrect(Boolean.TRUE.equals(choiceData.get("isCorrect")));
                    newChoice.setQuestion(question);
                    choiceRepository.save(newChoice);
                }
            }

            // Remove extra old choices if new list is shorter
            if (choices.size() < existingChoices.size()) {
                for (int i = choices.size(); i < existingChoices.size(); i++) {
                    Choice toRemove = existingChoices.get(i);
                    // Nullify FK references in attempt_answers before deleting
                    entityManager.createQuery(
                        "UPDATE AttemptAnswer aa SET aa.selectedChoice = null WHERE aa.selectedChoice.id = :cid")
                        .setParameter("cid", toRemove.getId())
                        .executeUpdate();
                    question.getChoices().remove(toRemove);
                    choiceRepository.delete(toRemove);
                }
            }
        }

        question = questionRepository.findById(question.getId()).orElseThrow();
        return mapQuestionToDto(question);
    }

    /** Supprimer une question */
    @Transactional
    public void deleteQuestion(Long questionId, User instructor) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question introuvable"));
        if (!question.getQuiz().getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }
        // Nullify FK references in attempt_answers before deleting choices
        for (Choice c : question.getChoices()) {
            entityManager.createQuery(
                "UPDATE AttemptAnswer aa SET aa.selectedChoice = null WHERE aa.selectedChoice.id = :cid")
                .setParameter("cid", c.getId())
                .executeUpdate();
        }
        // Nullify FK references for the question itself
        entityManager.createQuery(
            "DELETE FROM AttemptAnswer aa WHERE aa.question.id = :qid")
            .setParameter("qid", questionId)
            .executeUpdate();
        
        Long quizId = question.getQuiz().getId();
        questionRepository.delete(question);
        
        // Redistribute marks evenly among remaining questions
        Quiz quiz = question.getQuiz();
        List<Question> remaining = questionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);
        if (!remaining.isEmpty() && quiz.getTotalMarks() > 0) {
            int autoMarks = (int) Math.ceil((double) quiz.getTotalMarks() / remaining.size());
            for (Question rq : remaining) {
                rq.setMarks(autoMarks);
                questionRepository.save(rq);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — Résultats
    // ═══════════════════════════════════════════════════════════════════════

    /** Voir les résultats d'un quiz (pour l'instructor) */
    public Map<String, Object> getQuizResults(Long quizId, User instructor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));
        if (!quiz.getCourse().getInstructor().getId().equals(instructor.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        List<QuizAttempt> attempts = attemptRepository.findByQuizIdOrderByFinishedAtDesc(quizId);

        List<Map<String, Object>> results = attempts.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", a.getStudent().getId());
            m.put("studentName", a.getStudent().getFullName());
            m.put("studentAvatar", a.getStudent().getAvatarPath());
            m.put("score", a.getScore());
            m.put("earnedMarks", a.getEarnedMarks());
            m.put("passed", a.isPassed());
            m.put("attempts", attemptRepository.countByStudentIdAndQuizId(a.getStudent().getId(), quizId));
            m.put("finishedAt", a.getFinishedAt());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("quizId", quiz.getId());
        dto.put("quizTitle", quiz.getTitle());
        dto.put("courseName", quiz.getCourse().getTitle());
        dto.put("totalParticipants", attempts.stream().map(a -> a.getStudent().getId()).distinct().count());
        dto.put("results", results);
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT — Passer un quiz
    // ═══════════════════════════════════════════════════════════════════════

    /** Lister les quizzes disponibles pour un cours (pour l'étudiant inscrit) */
    public List<Map<String, Object>> getStudentQuizzes(Long courseId, User student) {
        // Vérifier l'inscription
        enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID)
                .orElseThrow(() -> new RuntimeException("Vous n'êtes pas inscrit à ce cours"));

        return quizRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream().map(q -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", q.getId());
                    m.put("title", q.getTitle());
                    m.put("numberOfQuestions", q.getQuestions().size());
                    m.put("totalMarks", q.getTotalMarks());
                    m.put("passMark", q.getPassMark());
                    m.put("durationMinutes", q.getDurationMinutes());
                    m.put("attempts", attemptRepository.countByStudentIdAndQuizId(student.getId(), q.getId()));
                    return m;
                }).collect(Collectors.toList());
    }

    /** Lister tous les quizzes de toutes les inscriptions d'un étudiant */
    public List<Map<String, Object>> getAllStudentQuizzes(User student) {
        List<Enrollment> enrollments =
                enrollmentRepository.findByStudentIdAndPaymentStatus(student.getId(), PaymentStatus.PAID);

        List<Map<String, Object>> all = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            List<Quiz> quizzes = quizRepository.findByCourseIdOrderByCreatedAtDesc(enrollment.getCourse().getId());
            for (Quiz q : quizzes) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", q.getId());
                m.put("title", q.getTitle());
                m.put("courseName", q.getCourse().getTitle());
                m.put("courseId", q.getCourse().getId());
                m.put("numberOfQuestions", q.getQuestions().size());
                m.put("totalMarks", q.getTotalMarks());
                m.put("passMark", q.getPassMark());
                m.put("durationMinutes", q.getDurationMinutes());
                m.put("attempts", attemptRepository.countByStudentIdAndQuizId(student.getId(), q.getId()));
                all.add(m);
            }
        }
        return all;
    }

    /** Récupérer les questions d'un quiz pour le passer (sans les bonnes réponses) */
    public Map<String, Object> startQuiz(Long quizId, User student) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));

        // Vérifier l'inscription
        enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), quiz.getCourse().getId(), PaymentStatus.PAID)
                .orElseThrow(() -> new RuntimeException("Vous n'êtes pas inscrit à ce cours"));

        List<Map<String, Object>> questions = quiz.getQuestions().stream().map(q -> {
            Map<String, Object> qm = new LinkedHashMap<>();
            qm.put("id", q.getId());
            qm.put("questionText", q.getQuestionText());
            qm.put("questionType", q.getQuestionType().name());
            qm.put("marks", q.getMarks());
            qm.put("choices", q.getChoices().stream().map(c -> {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", c.getId());
                cm.put("text", c.getText());
                // NE PAS inclure isCorrect !
                return cm;
            }).collect(Collectors.toList()));
            return qm;
        }).collect(Collectors.toList());

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("quizId", quiz.getId());
        dto.put("title", quiz.getTitle());
        dto.put("courseName", quiz.getCourse().getTitle());
        dto.put("totalMarks", quiz.getTotalMarks());
        dto.put("passMark", quiz.getPassMark());
        dto.put("durationMinutes", quiz.getDurationMinutes());
        dto.put("questions", questions);
        return dto;
    }

    /** Soumettre les réponses et obtenir le résultat */
    @Transactional
    public Map<String, Object> submitQuiz(Long quizId, Map<String, Object> data, User student) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz introuvable"));

        enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), quiz.getCourse().getId(), PaymentStatus.PAID)
                .orElseThrow(() -> new RuntimeException("Vous n'êtes pas inscrit à ce cours"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> answersData = (List<Map<String, Object>>) data.get("answers");
        LocalDateTime startedAt = data.containsKey("startedAt")
                ? LocalDateTime.parse(data.get("startedAt").toString())
                : LocalDateTime.now().minusMinutes(quiz.getDurationMinutes());

        QuizAttempt attempt = new QuizAttempt();
        attempt.setStudent(student);
        attempt.setQuiz(quiz);
        attempt.setStartedAt(startedAt);
        attempt.setFinishedAt(LocalDateTime.now());

        int correctCount = 0;
        int earnedMarks = 0;
        List<AttemptAnswer> attemptAnswers = new ArrayList<>();

        for (Map<String, Object> answerData : answersData) {
            Long questionId = Long.valueOf(answerData.get("questionId").toString());
            Long selectedChoiceId = answerData.get("selectedChoiceId") != null
                    ? Long.valueOf(answerData.get("selectedChoiceId").toString()) : null;

            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question introuvable: " + questionId));

            Choice selectedChoice = selectedChoiceId != null
                    ? choiceRepository.findById(selectedChoiceId).orElse(null) : null;

            boolean isCorrect = selectedChoice != null && selectedChoice.isCorrect();
            if (isCorrect) {
                correctCount++;
                earnedMarks += question.getMarks();
            }

            AttemptAnswer aa = new AttemptAnswer();
            aa.setAttempt(attempt);
            aa.setQuestion(question);
            aa.setSelectedChoice(selectedChoice);
            aa.setCorrect(isCorrect);
            attemptAnswers.add(aa);
        }

        int totalQuestions = quiz.getQuestions().size();
        int incorrectCount = totalQuestions - correctCount;
        int scorePercent = totalQuestions > 0 ? (correctCount * 100) / totalQuestions : 0;
        boolean passed = earnedMarks >= quiz.getPassMark();

        attempt.setScore(scorePercent);
        attempt.setEarnedMarks(earnedMarks);
        attempt.setCorrectAnswers(correctCount);
        attempt.setIncorrectAnswers(incorrectCount);
        attempt.setPassed(passed);
        attempt.setAnswers(attemptAnswers);

        attemptRepository.save(attempt);

        // Préparer le résultat
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attemptId", attempt.getId());
        result.put("passed", passed);
        result.put("scorePercent", scorePercent);
        result.put("earnedMarks", earnedMarks);
        result.put("totalMarks", quiz.getTotalMarks());
        result.put("correctCount", correctCount);
        result.put("incorrectCount", incorrectCount);
        result.put("totalQuestions", totalQuestions);
        return result;
    }

    /** Détail d'une tentative (pour l'étudiant ou l'instructor) */
    public Map<String, Object> getAttemptDetail(Long attemptId, User user) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Tentative introuvable"));

        boolean isStudent = attempt.getStudent().getId().equals(user.getId());
        boolean isInstructor = attempt.getQuiz().getCourse().getInstructor().getId().equals(user.getId());
        if (!isStudent && !isInstructor) {
            throw new RuntimeException("Accès non autorisé");
        }

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("attemptId", attempt.getId());
        dto.put("quizTitle", attempt.getQuiz().getTitle());
        dto.put("courseName", attempt.getQuiz().getCourse().getTitle());
        dto.put("studentName", attempt.getStudent().getFullName());
        dto.put("score", attempt.getScore());
        dto.put("earnedMarks", attempt.getEarnedMarks());
        dto.put("totalMarks", attempt.getQuiz().getTotalMarks());
        dto.put("passMark", attempt.getQuiz().getPassMark());
        dto.put("correctAnswers", attempt.getCorrectAnswers());
        dto.put("incorrectAnswers", attempt.getIncorrectAnswers());
        dto.put("passed", attempt.isPassed());
        dto.put("startedAt", attempt.getStartedAt());
        dto.put("finishedAt", attempt.getFinishedAt());
        dto.put("durationMinutes", attempt.getQuiz().getDurationMinutes());

        // Détail par question
        List<Map<String, Object>> questionOverview = attempt.getAnswers().stream().map(aa -> {
            Map<String, Object> qo = new LinkedHashMap<>();
            qo.put("questionId", aa.getQuestion().getId());
            qo.put("questionText", aa.getQuestion().getQuestionText());
            qo.put("questionType", aa.getQuestion().getQuestionType().name());
            qo.put("givenAnswer", aa.getSelectedChoice() != null ? aa.getSelectedChoice().getText() : "—");
            // Trouver la bonne réponse
            String correctAnswer = aa.getQuestion().getChoices().stream()
                    .filter(Choice::isCorrect).map(Choice::getText).findFirst().orElse("—");
            qo.put("correctAnswer", correctAnswer);
            qo.put("isCorrect", aa.isCorrect());
            return qo;
        }).collect(Collectors.toList());

        dto.put("questionOverview", questionOverview);
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Private mappers
    // ═══════════════════════════════════════════════════════════════════════

    private Map<String, Object> mapQuizToDto(Quiz quiz) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", quiz.getId());
        // Course (peut être null si quiz lié uniquement à une leçon)
        Course c = quiz.getCourse();
        if (c == null && quiz.getLesson() != null) {
            c = quiz.getLesson().getSection().getCourse();
        }
        m.put("courseId", c != null ? c.getId() : null);
        m.put("courseName", c != null ? c.getTitle() : null);
        // Lesson
        if (quiz.getLesson() != null) {
            m.put("lessonId", quiz.getLesson().getId());
            m.put("lessonTitle", quiz.getLesson().getTitle());
        } else {
            m.put("lessonId", null);
            m.put("lessonTitle", null);
        }
        m.put("title", quiz.getTitle());
        m.put("numberOfQuestions", quiz.getQuestions().size());
        m.put("totalMarks", quiz.getTotalMarks());
        m.put("passMark", quiz.getPassMark());
        m.put("durationMinutes", quiz.getDurationMinutes());
        m.put("createdAt", quiz.getCreatedAt());
        return m;
    }

    private Map<String, Object> mapQuestionToDto(Question q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("questionText", q.getQuestionText());
        m.put("questionType", q.getQuestionType().name());
        m.put("orderIndex", q.getOrderIndex());
        m.put("marks", q.getMarks());
        m.put("choices", q.getChoices().stream().map(c -> {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.getId());
            cm.put("text", c.getText());
            cm.put("isCorrect", c.isCorrect());
            return cm;
        }).collect(Collectors.toList()));
        return m;
    }
}
