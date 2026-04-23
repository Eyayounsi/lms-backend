package com.elearning.ProjetPfe.service.communication;

import com.elearning.ProjetPfe.repository.learning.QuestionRepository;
import com.elearning.ProjetPfe.entity.learning.Question;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.communication.CourseAnswer;
import com.elearning.ProjetPfe.entity.communication.CourseQuestion;
import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.communication.CourseAnswerRepository;
import com.elearning.ProjetPfe.repository.communication.CourseQuestionRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.course.LessonRepository;

@Service
public class QaService {

    @Autowired private CourseQuestionRepository questionRepository;
    @Autowired private CourseAnswerRepository answerRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  Questions
    // ═══════════════════════════════════════════════════════════════════════

    /** Poser une question */
    @Transactional
    public Map<String, Object> askQuestion(Map<String, Object> data, User student) {
        Long courseId = Long.valueOf(data.get("courseId").toString());
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours introuvable"));

        // Étudiant doit être inscrit (ou instructor du cours)
        boolean isInstructor = course.getInstructor().getId().equals(student.getId());
        if (!isInstructor) {
            enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                    student.getId(), courseId, PaymentStatus.PAID)
                    .orElseThrow(() -> new RuntimeException("Vous n'êtes pas inscrit à ce cours"));
        }

        CourseQuestion question = new CourseQuestion();
        question.setTitle((String) data.get("title"));
        question.setBody((String) data.get("body"));
        question.setStudent(student);
        question.setCourse(course);

        if (data.containsKey("lessonId") && data.get("lessonId") != null) {
            Long lessonId = Long.valueOf(data.get("lessonId").toString());
            Lesson lesson = lessonRepository.findById(lessonId)
                    .orElseThrow(() -> new RuntimeException("Leçon introuvable"));
            question.setLesson(lesson);
        }

        question = questionRepository.save(question);
        return mapQuestionToDto(question);
    }

    /** Lister les questions d'un cours */
    public List<Map<String, Object>> getQuestionsByCourse(Long courseId) {
        return questionRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream().map(this::mapQuestionToDto).collect(Collectors.toList());
    }

    /** Lister les questions d'une leçon */
    public List<Map<String, Object>> getQuestionsByLesson(Long lessonId) {
        return questionRepository.findByLessonIdOrderByCreatedAtDesc(lessonId)
                .stream().map(this::mapQuestionToDto).collect(Collectors.toList());
    }

    /** Toutes les questions posées par l'étudiant connecté (toutes ses inscriptions) */
    public List<Map<String, Object>> getMyQuestions(User user) {
        return questionRepository.findByStudentIdOrderByCreatedAtDesc(user.getId())
                .stream().map(this::mapQuestionToDto).collect(Collectors.toList());
    }

    /** Détail d'une question avec réponses */
    public Map<String, Object> getQuestionDetail(Long questionId) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question introuvable"));
        return mapQuestionToDto(question);
    }

    /** Supprimer sa propre question */
    @Transactional
    public void deleteQuestion(Long questionId, User user) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question introuvable"));
        boolean isOwner = question.getStudent().getId().equals(user.getId());
        boolean isInstructor = question.getCourse().getInstructor().getId().equals(user.getId());
        if (!isOwner && !isInstructor) {
            throw new RuntimeException("Accès non autorisé");
        }
        questionRepository.delete(question);
    }

    /** Modifier sa propre question */
    @Transactional
    public Map<String, Object> updateQuestion(Long questionId, Map<String, Object> data, User user) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question introuvable"));
        if (!question.getStudent().getId().equals(user.getId())) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres questions");
        }
        if (data.containsKey("title")) question.setTitle((String) data.get("title"));
        if (data.containsKey("body"))  question.setBody((String) data.get("body"));
        question = questionRepository.save(question);
        return mapQuestionToDto(question);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Réponses
    // ═══════════════════════════════════════════════════════════════════════

    /** Répondre à une question */
    @Transactional
    public Map<String, Object> answerQuestion(Long questionId, Map<String, Object> data, User author) {
        CourseQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question introuvable"));

        boolean isInstructor = question.getCourse().getInstructor().getId().equals(author.getId());

        CourseAnswer answer = new CourseAnswer();
        answer.setBody((String) data.get("body"));
        answer.setAuthor(author);
        answer.setQuestion(question);
        answer.setInstructorAnswer(isInstructor);

        answer = answerRepository.save(answer);
        return mapAnswerToDto(answer);
    }

    /** Supprimer sa propre réponse */
    @Transactional
    public void deleteAnswer(Long answerId, User user) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Réponse introuvable"));
        boolean isOwner = answer.getAuthor().getId().equals(user.getId());
        boolean isInstructor = answer.getQuestion().getCourse().getInstructor().getId().equals(user.getId());
        if (!isOwner && !isInstructor) {
            throw new RuntimeException("Accès non autorisé");
        }
        answerRepository.delete(answer);
    }

    /** Modifier sa propre réponse */
    @Transactional
    public Map<String, Object> updateAnswer(Long answerId, Map<String, Object> data, User user) {
        CourseAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Réponse introuvable"));
        if (!answer.getAuthor().getId().equals(user.getId())) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres réponses");
        }
        if (data.containsKey("body")) answer.setBody((String) data.get("body"));
        answer = answerRepository.save(answer);
        return mapAnswerToDto(answer);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Mappers
    // ═══════════════════════════════════════════════════════════════════════

    private Map<String, Object> mapQuestionToDto(CourseQuestion q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.getId());
        m.put("title", q.getTitle());
        m.put("body", q.getBody());
        m.put("courseId", q.getCourse().getId());
        m.put("courseName", q.getCourse().getTitle());
        m.put("lessonId", q.getLesson() != null ? q.getLesson().getId() : null);
        m.put("lessonTitle", q.getLesson() != null ? q.getLesson().getTitle() : null);
        m.put("authorId", q.getStudent().getId());
        m.put("authorName", q.getStudent().getFullName());
        m.put("authorAvatar", q.getStudent().getAvatarPath());
        m.put("answerCount", q.getAnswers() != null ? q.getAnswers().size() : 0);
        m.put("answers", q.getAnswers() != null
                ? q.getAnswers().stream().map(this::mapAnswerToDto).collect(Collectors.toList())
                : new ArrayList<>());
        m.put("createdAt", q.getCreatedAt());
        m.put("updatedAt", q.getUpdatedAt());
        return m;
    }

    private Map<String, Object> mapAnswerToDto(CourseAnswer a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("body", a.getBody());
        m.put("authorId", a.getAuthor().getId());
        m.put("authorName", a.getAuthor().getFullName());
        m.put("authorAvatar", a.getAuthor().getAvatarPath());
        m.put("instructorAnswer", a.isInstructorAnswer());
        m.put("createdAt", a.getCreatedAt());
        return m;
    }
}
