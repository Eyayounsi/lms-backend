package com.elearning.ProjetPfe.service.engagement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.engagement.DetectionRemark;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.engagement.DetectionRemarkRepository;
import com.elearning.ProjetPfe.repository.auth.UserRepository;

@Service
public class DetectionRemarkService {

    private final DetectionRemarkRepository remarkRepo;
    private final UserRepository userRepo;
    private final CourseRepository courseRepo;

    public DetectionRemarkService(DetectionRemarkRepository remarkRepo,
                                  UserRepository userRepo,
                                  CourseRepository courseRepo) {
        this.remarkRepo = remarkRepo;
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
    }

    /**
     * Save a detection remark from the CV service.
     */
    public DetectionRemark saveRemark(Long studentId, Long courseId, Long lessonId,
                                      String remarkType, String remarkMessage, Integer attentionScore) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Course course = courseRepo.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        DetectionRemark remark = new DetectionRemark();
        remark.setStudent(student);
        remark.setCourse(course);
        remark.setLessonId(lessonId);
        remark.setRemarkType(remarkType);
        remark.setRemarkMessage(remarkMessage);
        remark.setAttentionScore(attentionScore);
        return remarkRepo.save(remark);
    }

    /**
     * Get all remarks for a specific student in a specific course.
     */
    public List<Map<String, Object>> getRemarksByStudentAndCourse(Long studentId, Long courseId) {
        return remarkRepo.findByStudentIdAndCourseIdOrderByDetectedAtDesc(studentId, courseId)
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    /**
     * Get all remarks for a course (for instructor view).
     */
    public List<Map<String, Object>> getRemarksByCourse(Long courseId) {
        return remarkRepo.findByCourseIdOrderByDetectedAtDesc(courseId)
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    /**
     * Get all remarks for a student (for admin view).
     */
    public List<Map<String, Object>> getRemarksByStudent(Long studentId) {
        return remarkRepo.findByStudentIdOrderByDetectedAtDesc(studentId)
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    /**
     * Get summary for all students across all courses (admin overview).
     */
    public List<Map<String, Object>> getAllRemarksSummary() {
        List<DetectionRemark> all = remarkRepo.findAll();

        // Group by student+course
        Map<String, List<DetectionRemark>> grouped = all.stream()
                .collect(Collectors.groupingBy(r -> r.getStudent().getId() + "-" + r.getCourse().getId()));

        List<Map<String, Object>> summaries = new ArrayList<>();
        for (Map.Entry<String, List<DetectionRemark>> entry : grouped.entrySet()) {
            List<DetectionRemark> remarks = entry.getValue();
            if (remarks.isEmpty()) continue;
            DetectionRemark first = remarks.get(0);

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("studentId", first.getStudent().getId());
            summary.put("studentName", first.getStudent().getFullName());
            summary.put("studentEmail", first.getStudent().getEmail());
            summary.put("courseId", first.getCourse().getId());
            summary.put("courseTitle", first.getCourse().getTitle());
            summary.put("totalRemarks", remarks.size());

            // Count by type
            Map<String, Long> typeCounts = remarks.stream()
                    .collect(Collectors.groupingBy(DetectionRemark::getRemarkType, Collectors.counting()));
            summary.put("typeCounts", typeCounts);

            // Average attention score
            OptionalDouble avgScore = remarks.stream()
                    .filter(r -> r.getAttentionScore() != null)
                    .mapToInt(DetectionRemark::getAttentionScore)
                    .average();
            summary.put("avgAttentionScore", avgScore.isPresent() ? (int) avgScore.getAsDouble() : null);

            // Last detection date
            summary.put("lastDetectedAt", remarks.get(0).getDetectedAt());

            summaries.add(summary);
        }

        // Sort by total remarks descending
        summaries.sort((a, b) -> Integer.compare((int) b.get("totalRemarks"), (int) a.get("totalRemarks")));
        return summaries;
    }

    private Map<String, Object> toMap(DetectionRemark r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("studentId", r.getStudent().getId());
        map.put("studentName", r.getStudent().getFullName());
        map.put("courseId", r.getCourse().getId());
        map.put("courseTitle", r.getCourse().getTitle());
        map.put("lessonId", r.getLessonId());
        map.put("remarkType", r.getRemarkType());
        map.put("remarkMessage", r.getRemarkMessage());
        map.put("attentionScore", r.getAttentionScore());
        map.put("detectedAt", r.getDetectedAt());
        return map;
    }
}
